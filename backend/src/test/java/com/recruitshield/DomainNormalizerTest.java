package com.recruitshield;

import com.recruitshield.util.DomainNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainNormalizerTest {

    @Test
    @DisplayName("Normalizes various URL formats and schemes to clean hostnames")
    void testNormalizeHostVariousFormats() {
        assertEquals("example.com", DomainNormalizer.normalizeHost("https://www.example.com/careers"));
        assertEquals("example.com", DomainNormalizer.normalizeHost("http://example.com"));
        assertEquals("example.com", DomainNormalizer.normalizeHost("example.com"));
        assertEquals("careers.example.com", DomainNormalizer.normalizeHost("https://careers.example.com/jobs?id=123#overview"));
        assertEquals("example.com", DomainNormalizer.normalizeHost("http://www2.example.com:8080/portal/"));
        assertEquals("example.co.uk", DomainNormalizer.normalizeHost("https://www.example.co.uk/"));
    }

    @Test
    @DisplayName("Extracts registrable root domain across single and multi-part ccTLDs")
    void testExtractRootDomain() {
        assertEquals("example.com", DomainNormalizer.extractRootDomain("careers.example.com"));
        assertEquals("example.co.uk", DomainNormalizer.extractRootDomain("jobs.portal.example.co.uk"));
        assertEquals("acmetech.co.in", DomainNormalizer.extractRootDomain("https://careers.acmetech.co.in/vacancies"));
        assertEquals("google.com", DomainNormalizer.extractRootDomain("www.google.com"));
        assertEquals("startup.io", DomainNormalizer.extractRootDomain("api.staging.startup.io"));
    }

    @Test
    @DisplayName("Extracts normalized domain from email addresses")
    void testExtractDomainFromEmail() {
        assertEquals("example.com", DomainNormalizer.extractDomainFromEmail("hr@example.com"));
        assertEquals("careers.corp.com", DomainNormalizer.extractDomainFromEmail("recruiter@careers.corp.com"));
        assertEquals("gmail.com", DomainNormalizer.extractDomainFromEmail("recruiter.apex@gmail.com"));
        assertEquals("", DomainNormalizer.extractDomainFromEmail("invalid-email"));
        assertEquals("", DomainNormalizer.extractDomainFromEmail(null));
    }

    @Test
    @DisplayName("Accurately verifies heuristic domain matching between email and company website")
    void testIsDomainMatch() {
        // Direct exact match
        assertTrue(DomainNormalizer.isDomainMatch("example.com", "example.com"));
        // Subdomain to root match
        assertTrue(DomainNormalizer.isDomainMatch("talent@example.com", "https://careers.example.com"));
        assertTrue(DomainNormalizer.isDomainMatch("hr@corp.acme.com", "https://acme.com"));
        assertTrue(DomainNormalizer.isDomainMatch("recruiter@tcs.com", "https://www.tcs.com/careers"));
        // Multi-part ccTLD match
        assertTrue(DomainNormalizer.isDomainMatch("recruiter@tech.co.in", "https://careers.tech.co.in"));

        // Domain mismatch
        assertFalse(DomainNormalizer.isDomainMatch("recruiter@gmail.com", "https://microsoft.com"));
        assertFalse(DomainNormalizer.isDomainMatch("recruiter@yahoo.com", "https://infosys.com"));
        assertFalse(DomainNormalizer.isDomainMatch("hr@evil-phish.xyz", "https://google.com"));
        assertFalse(DomainNormalizer.isDomainMatch(null, "example.com"));
        assertFalse(DomainNormalizer.isDomainMatch("example.com", ""));
    }

    @Test
    @DisplayName("Detects IPv4, IPv6, and private/local addresses")
    void testIpAndLocalDetection() {
        assertTrue(DomainNormalizer.isIpAddress("192.168.1.1"));
        assertTrue(DomainNormalizer.isIpAddress("10.0.0.1"));
        assertTrue(DomainNormalizer.isIpAddress("127.0.0.1"));
        assertFalse(DomainNormalizer.isIpAddress("example.com"));

        assertTrue(DomainNormalizer.isPrivateOrLocal("localhost"));
        assertTrue(DomainNormalizer.isPrivateOrLocal("127.0.0.1"));
        assertTrue(DomainNormalizer.isPrivateOrLocal("192.168.0.10"));
        assertTrue(DomainNormalizer.isPrivateOrLocal("10.1.2.3"));
        assertTrue(DomainNormalizer.isPrivateOrLocal("dev.company.local"));
        assertFalse(DomainNormalizer.isPrivateOrLocal("google.com"));
    }
}
