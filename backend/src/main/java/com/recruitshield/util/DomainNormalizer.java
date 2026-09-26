package com.recruitshield.util;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Domain and Hostname Normalization Utility.
 * Performs safe, offline syntactic normalization of URLs, hostnames, and email domains.
 *
 * NOTE: This utility operates purely via static string and URI parsing.
 * It does NOT execute network calls, DNS queries, or socket connections.
 */
public final class DomainNormalizer {

    private DomainNormalizer() {
        // Utility class
    }

    private static final Pattern IPV4_PATTERN = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$");

    // Common two-level ccTLDs to accurately identify registrable base domains
    private static final Set<String> MULTI_PART_TLDS = new HashSet<>(Arrays.asList(
            "co.uk", "org.uk", "gov.uk", "ac.uk", "me.uk",
            "co.in", "net.in", "org.in", "gen.in", "firm.in", "ind.in", "nic.in", "ac.in", "edu.in", "res.in", "gov.in",
            "com.au", "net.au", "org.au", "edu.au", "gov.au",
            "co.nz", "net.nz", "org.nz",
            "co.za", "org.za",
            "co.jp", "ne.jp", "or.jp",
            "com.br", "org.br",
            "com.sg", "edu.sg",
            "com.my", "edu.my",
            "com.ph", "org.ph",
            "co.kr", "ne.kr",
            "com.cn", "net.cn", "org.cn",
            "com.mx", "org.mx",
            "com.ar", "org.ar",
            "com.hk", "org.hk"
    ));

    /**
     * Normalizes a raw URL or domain string into a clean hostname.
     * Strips protocol (http://, https://), credentials, port, path, query, fragment,
     * and standard leading prefixes such as "www.".
     *
     * @param input Raw URL or domain string
     * @return Normalized hostname (e.g., 'example.com'), or empty string if input is invalid
     */
    public static String normalizeHost(String input) {
        if (input == null) {
            return "";
        }
        String clean = input.trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) {
            return "";
        }

        // Strip enclosing quotes if present
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }

        // If scheme is missing, prepend https:// for standard URI parsing
        String parseable = clean;
        if (!parseable.contains("://")) {
            parseable = "https://" + parseable;
        }

        try {
            URI uri = new URI(parseable);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                clean = host;
            } else {
                clean = extractHostManually(clean);
            }
        } catch (Exception e) {
            clean = extractHostManually(clean);
        }

        // Strip leading 'www.' or 'www[0-9].' prefixes
        if (clean.startsWith("www.")) {
            clean = clean.substring(4);
        } else if (clean.matches("^www[0-9]+\\..*")) {
            clean = clean.replaceFirst("^www[0-9]+\\.", "");
        }

        // Strip trailing period if fully qualified domain name
        if (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1);
        }

        return clean;
    }

    private static String extractHostManually(String raw) {
        String s = raw;
        int schemeIdx = s.indexOf("://");
        if (schemeIdx != -1) {
            s = s.substring(schemeIdx + 3);
        }
        int authIdx = s.indexOf('@');
        if (authIdx != -1) {
            s = s.substring(authIdx + 1);
        }
        int slashIdx = s.indexOf('/');
        if (slashIdx != -1) {
            s = s.substring(0, slashIdx);
        }
        int queryIdx = s.indexOf('?');
        if (queryIdx != -1) {
            s = s.substring(0, queryIdx);
        }
        int hashIdx = s.indexOf('#');
        if (hashIdx != -1) {
            s = s.substring(0, hashIdx);
        }
        int portIdx = s.indexOf(':');
        if (portIdx != -1) {
            s = s.substring(0, portIdx);
        }
        return s.trim();
    }

    /**
     * Extracts the registrable root domain (e.g., 'careers.example.com' -> 'example.com').
     * Accurately preserves multi-part country code TLDs (e.g., 'careers.google.co.in' -> 'google.co.in').
     *
     * @param hostOrUrl Hostname or URL string
     * @return Registrable root domain
     */
    public static String extractRootDomain(String hostOrUrl) {
        String host = normalizeHost(hostOrUrl);
        if (host.isEmpty() || isIpAddress(host) || isPrivateOrLocal(host)) {
            return host;
        }

        String[] parts = host.split("\\.");
        if (parts.length <= 2) {
            return host;
        }

        // Check if domain ends with a multi-part TLD (e.g., co.uk, co.in)
        String lastTwoParts = parts[parts.length - 2] + "." + parts[parts.length - 1];
        if (MULTI_PART_TLDS.contains(lastTwoParts)) {
            if (parts.length >= 3) {
                return parts[parts.length - 3] + "." + lastTwoParts;
            }
            return host;
        }

        // Standard single TLD (e.g., example.com)
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    /**
     * Extracts the normalized domain portion from an email address.
     *
     * @param email Recruiter or sender email
     * @return Normalized host domain (e.g., 'example.com')
     */
    public static String extractDomainFromEmail(String email) {
        if (email == null) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex != -1 && atIndex < email.length() - 1) {
            return normalizeHost(email.substring(atIndex + 1));
        }
        return "";
    }

    /**
     * Heuristically determines whether two domains belong to the same organizational entity.
     * Matches if exact match, if one is a direct subdomain of the other, or if root domains match.
     *
     * @param domain1 First domain (e.g. from recruiter email)
     * @param domain2 Second domain (e.g. from company website)
     * @return true if domains match heuristically
     */
    public static boolean isDomainMatch(String domain1, String domain2) {
        if (domain1 == null || domain2 == null) {
            return false;
        }
        String d1 = normalizeHost(domain1);
        String d2 = normalizeHost(domain2);

        if (d1.isEmpty() || d2.isEmpty()) {
            return false;
        }

        if (d1.equalsIgnoreCase(d2)) {
            return true;
        }

        if (d1.endsWith("." + d2) || d2.endsWith("." + d1)) {
            return true;
        }

        String root1 = extractRootDomain(d1);
        String root2 = extractRootDomain(d2);

        return !root1.isEmpty() && root1.equalsIgnoreCase(root2);
    }

    /**
     * Determines whether a hostname is a raw IPv4 or IPv6 literal.
     */
    public static boolean isIpAddress(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(host).matches() || IPV6_PATTERN.matcher(host).matches();
    }

    /**
     * Determines whether a host points to a local, loopback, or private intranet address.
     */
    public static boolean isPrivateOrLocal(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        String clean = host.toLowerCase(Locale.ROOT);
        if (clean.equals("localhost") || clean.endsWith(".local") || clean.endsWith(".internal")
                || clean.endsWith(".test") || clean.equals("127.0.0.1") || clean.equals("0.0.0.0")
                || clean.equals("::1")) {
            return true;
        }
        if (clean.startsWith("10.") || clean.startsWith("192.168.")) {
            return true;
        }
        if (clean.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            return true;
        }
        return false;
    }
}
