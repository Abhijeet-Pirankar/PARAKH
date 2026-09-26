package com.recruitshield.service;

import com.recruitshield.dto.UrlVerificationResult;
import com.recruitshield.util.DomainNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Static Heuristic URL Verification Service.
 *
 * SAFETY GUARANTEE:
 * This service performs strictly offline lexical and syntactic verification of submitted URLs.
 * It NEVER initiates outgoing network requests, follows HTTP redirects, connects to sockets,
 * or performs DNS name resolution.
 */
@Slf4j
@Service
public class UrlVerificationService {

    private static final Pattern URL_SHORTENER_PATTERN = Pattern.compile(
            "(bit\\.ly|tinyurl\\.com|t\\.me|forms\\.gle|cutt\\.ly|is\\.gd|rb\\.gy|shorturl\\.at)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RISKY_TLD_PATTERN = Pattern.compile(
            "\\.(xyz|top|tk|ml|ga|cf|gq|work|click|buzz|loan|fit)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SUSPICIOUS_DOMAIN_KEYWORDS = Pattern.compile(
            "(careers?-login|job-verify|payment-portal|recruitment-fee|secure-hiring|login-verify|candidate-pay|verify-credential)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SUSPICIOUS_OBFUSCATION_PATTERN = Pattern.compile(
            "(@|%[0-9a-fA-F]{2}%[0-9a-fA-F]{2}|//.*/|\\.\\.)"
    );

    /**
     * Statically inspects a company website or job application URL.
     *
     * @param rawUrl Untrusted URL string submitted in offer analysis
     * @return Structured UrlVerificationResult with explainable heuristic indicators
     */
    public UrlVerificationResult verifyUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return UrlVerificationResult.builder()
                    .provided(false)
                    .valid(false)
                    .https(false)
                    .originalUrl(null)
                    .domain("")
                    .riskIndicators(new ArrayList<>())
                    .positiveIndicators(new ArrayList<>())
                    .build();
        }

        String trimmed = rawUrl.trim();
        List<String> riskIndicators = new ArrayList<>();
        List<String> positiveIndicators = new ArrayList<>();

        boolean isHttps = trimmed.toLowerCase(Locale.ROOT).startsWith("https://");
        boolean isHttp = trimmed.toLowerCase(Locale.ROOT).startsWith("http://");

        if (isHttp) {
            riskIndicators.add("Company website uses insecure HTTP instead of encrypted HTTPS.");
        } else if (!isHttps && trimmed.contains("://")) {
            riskIndicators.add("Company website uses an unconventional protocol scheme.");
        }

        // Check for suspicious obfuscation or userinfo tricks (e.g., http://trusted.com@evil.com)
        if (SUSPICIOUS_OBFUSCATION_PATTERN.matcher(trimmed).find()) {
            riskIndicators.add("Company website contains suspicious character patterns or obfuscated encoding.");
        }

        // Validate syntax
        boolean validSyntax = true;
        String host = "";
        try {
            String parseable = trimmed;
            if (!parseable.contains("://")) {
                parseable = "https://" + parseable;
            }
            URI uri = new URI(parseable);
            host = DomainNormalizer.normalizeHost(trimmed);
            if (host.isEmpty() || host.contains(" ") || host.contains("/") || host.contains("?")) {
                validSyntax = false;
            }
        } catch (Exception e) {
            validSyntax = false;
            host = DomainNormalizer.normalizeHost(trimmed);
        }

        if (!validSyntax || host.isEmpty()) {
            riskIndicators.add("Company website has an invalid or malformed URL syntax.");
            return UrlVerificationResult.builder()
                    .provided(true)
                    .valid(false)
                    .https(isHttps)
                    .originalUrl(trimmed)
                    .domain(host)
                    .riskIndicators(riskIndicators)
                    .positiveIndicators(positiveIndicators)
                    .build();
        }

        // IP Address analysis
        boolean isIp = DomainNormalizer.isIpAddress(host);
        if (isIp) {
            riskIndicators.add("Company website uses a raw IP address instead of a domain name.");
        }

        // Local / private address analysis
        boolean isLocal = DomainNormalizer.isPrivateOrLocal(host);
        if (isLocal) {
            riskIndicators.add("Company website points to a local or private network address.");
        }

        // URL shortener analysis
        boolean isShortener = URL_SHORTENER_PATTERN.matcher(host).find() || URL_SHORTENER_PATTERN.matcher(trimmed).find();
        if (isShortener) {
            riskIndicators.add("Company website uses a link-shortening or URL redirection service.");
        }

        // High-risk TLD analysis
        boolean isRiskyTld = RISKY_TLD_PATTERN.matcher(host).find();
        if (isRiskyTld) {
            riskIndicators.add("Company website uses an untrusted or high-risk generic top-level domain.");
        }

        // Excessive subdomain analysis (e.g. sub1.sub2.sub3.example.com)
        boolean hasExcessiveSubdomains = checkExcessiveSubdomains(host);
        if (hasExcessiveSubdomains) {
            riskIndicators.add("Company website contains an unusually high number of subdomain levels.");
        }

        // Suspicious keywords analysis
        boolean hasSuspiciousKeywords = SUSPICIOUS_DOMAIN_KEYWORDS.matcher(host).find()
                || SUSPICIOUS_DOMAIN_KEYWORDS.matcher(trimmed).find();
        if (hasSuspiciousKeywords) {
            riskIndicators.add("Company website domain includes keywords commonly associated with credential theft or phishing.");
        }

        // Positive authenticity signals
        if (isHttps) {
            positiveIndicators.add("Company website uses HTTPS protocol.");
        }
        if (validSyntax && !isIp && !isLocal && !isShortener && !isRiskyTld) {
            positiveIndicators.add("Company website uses a standard, established domain structure.");
        }

        return UrlVerificationResult.builder()
                .provided(true)
                .valid(true)
                .https(isHttps)
                .originalUrl(trimmed)
                .domain(host)
                .ipAddress(isIp)
                .privateOrLocal(isLocal)
                .urlShortener(isShortener)
                .riskyTld(isRiskyTld)
                .excessiveSubdomains(hasExcessiveSubdomains)
                .suspiciousKeywords(hasSuspiciousKeywords)
                .riskIndicators(riskIndicators)
                .positiveIndicators(positiveIndicators)
                .build();
    }

    private boolean checkExcessiveSubdomains(String host) {
        if (host == null || host.isEmpty() || DomainNormalizer.isIpAddress(host)) {
            return false;
        }
        String rootDomain = DomainNormalizer.extractRootDomain(host);
        if (rootDomain.isEmpty() || rootDomain.equalsIgnoreCase(host)) {
            return false;
        }
        // Count subdomains preceding root domain
        if (host.endsWith("." + rootDomain)) {
            String prefix = host.substring(0, host.length() - rootDomain.length() - 1);
            String[] subLevels = prefix.split("\\.");
            return subLevels.length >= 3;
        }
        return false;
    }
}
