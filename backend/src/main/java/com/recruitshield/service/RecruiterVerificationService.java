package com.recruitshield.service;

import com.recruitshield.dto.RecruiterVerificationResult;
import com.recruitshield.util.DomainNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static Heuristic Recruiter Verification Service.
 * Evaluates recruiter email syntax, domain correlation, and communication avenues.
 *
 * SAFETY GUARANTEE:
 * This service operates purely on static string parsing and heuristics.
 * It NEVER performs MX record lookups, SMTP handshakes, or network probing.
 */
@Slf4j
@Service
public class RecruiterVerificationService {

    private static final Pattern EMAIL_SYNTAX_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern EXTRACT_EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );

    private static final Pattern FREEMAIL_PATTERN = Pattern.compile(
            "@(gmail|yahoo|hotmail|outlook|zoho|rediffmail|protonmail|live|aol|icloud|mail)\\.com",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DISPOSABLE_EMAIL_PATTERN = Pattern.compile(
            "@(tempmail|throwaway|guerrillamail|mailinator|yopmail|10minutemail|trashmail|dispostable)\\.",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RANDOMIZED_USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z._-]*[0-9]{5,}@"
    );

    /**
     * Statically inspects recruiter credentials and communications.
     *
     * @param recruiterEmail Submitted recruiter email address
     * @param companyWebsite Submitted company website URL
     * @param receivedVia Stated communication medium
     * @param offerText Offer narrative text
     * @return Structured RecruiterVerificationResult with explainable heuristic indicators
     */
    public RecruiterVerificationResult verifyRecruiter(
            String recruiterEmail,
            String companyWebsite,
            String receivedVia,
            String offerText) {

        List<String> riskIndicators = new ArrayList<>();
        List<String> positiveIndicators = new ArrayList<>();

        String rawEmail = recruiterEmail != null ? recruiterEmail.trim() : "";
        String rawWebsite = companyWebsite != null ? companyWebsite.trim() : "";
        String channel = receivedVia != null ? receivedVia.trim() : "";
        String text = offerText != null ? offerText.toLowerCase(Locale.ROOT) : "";

        // If email field is empty, attempt heuristic extraction from offer text
        boolean emailProvided = !rawEmail.isEmpty();
        String effectiveEmail = rawEmail;
        if (!emailProvided && offerText != null) {
            Matcher matcher = EXTRACT_EMAIL_PATTERN.matcher(offerText);
            if (matcher.find()) {
                effectiveEmail = matcher.group();
            }
        }

        boolean emailValid = false;
        String emailDomain = "";
        boolean isFreemail = false;
        boolean isDisposable = false;
        boolean hasSuspiciousPattern = false;

        if (effectiveEmail.isEmpty()) {
            riskIndicators.add("Recruiter email was not provided.");
        } else {
            emailValid = EMAIL_SYNTAX_PATTERN.matcher(effectiveEmail).matches();
            if (!emailValid) {
                riskIndicators.add("Recruiter email format is syntactically invalid.");
            } else {
                emailDomain = DomainNormalizer.extractDomainFromEmail(effectiveEmail);

                // Check free webmail provider
                if (FREEMAIL_PATTERN.matcher(effectiveEmail).find()) {
                    isFreemail = true;
                    riskIndicators.add("Recruiter uses a public/free email domain.");
                }

                // Check disposable mailbox provider
                if (DISPOSABLE_EMAIL_PATTERN.matcher(effectiveEmail).find()) {
                    isDisposable = true;
                    riskIndicators.add("Recruiter uses a disposable or anonymous email address.");
                }

                // Check randomized username pattern
                if (RANDOMIZED_USERNAME_PATTERN.matcher(effectiveEmail).find()) {
                    hasSuspiciousPattern = true;
                    riskIndicators.add("Recruiter email address exhibits automated or randomized naming patterns.");
                }

                // Positive signal for corporate email
                if (!isFreemail && !isDisposable && !emailDomain.isEmpty()) {
                    positiveIndicators.add("Recruiter contacted from an enterprise corporate email domain.");
                }
            }
        }

        // Domain comparison: Only evaluate when BOTH domains are actually present
        String companyDomain = DomainNormalizer.normalizeHost(rawWebsite);
        Boolean domainMatch = null;

        if (!emailDomain.isEmpty() && !companyDomain.isEmpty() && !isFreemail && !isDisposable) {
            boolean matches = DomainNormalizer.isDomainMatch(emailDomain, companyDomain);
            domainMatch = matches;
            if (matches) {
                positiveIndicators.add("Recruiter email domain matches the provided company website domain.");
            } else {
                riskIndicators.add("Recruiter email domain does not match the provided company website domain.");
            }
        } else if (isFreemail && !companyDomain.isEmpty()) {
            // Freemail with a company website is also a domain mismatch signal
            domainMatch = false;
            riskIndicators.add("Recruiter email domain does not match the provided company website domain.");
        }

        // Communication channel analysis
        boolean informalChannel = false;
        if (channel.equalsIgnoreCase("WhatsApp") || channel.equalsIgnoreCase("Telegram")
                || text.contains("whatsapp") || text.contains("telegram") || text.contains("t.me/")) {
            informalChannel = true;
            riskIndicators.add("Recruitment communication uses an informal channel.");
        } else if (channel.equalsIgnoreCase("LinkedIn") || channel.equalsIgnoreCase("Email")
                || channel.equalsIgnoreCase("Company Portal")) {
            positiveIndicators.add("Recruitment communication uses an official professional channel.");
        }

        return RecruiterVerificationResult.builder()
                .emailProvided(emailProvided || !effectiveEmail.isEmpty())
                .emailValid(emailValid)
                .email(effectiveEmail.isEmpty() ? null : effectiveEmail)
                .emailDomain(emailDomain.isEmpty() ? null : emailDomain)
                .companyDomain(companyDomain.isEmpty() ? null : companyDomain)
                .domainMatch(domainMatch)
                .publicFreemail(isFreemail)
                .disposableEmail(isDisposable)
                .suspiciousPattern(hasSuspiciousPattern)
                .communicationChannel(channel.isEmpty() ? null : channel)
                .informalChannel(informalChannel)
                .riskIndicators(riskIndicators)
                .positiveIndicators(positiveIndicators)
                .build();
    }
}
