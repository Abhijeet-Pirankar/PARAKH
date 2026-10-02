package com.recruitshield;

import com.recruitshield.dto.UrlVerificationResult;
import com.recruitshield.service.UrlVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlVerificationServiceTest {

    private UrlVerificationService urlVerificationService;

    @BeforeEach
    void setUp() {
        urlVerificationService = new UrlVerificationService();
    }

    @Test
    @DisplayName("1. Valid HTTPS URL: should identify secure protocol and standard domain")
    void testValidHttpsUrl() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("https://careers.acme-corp.com");

        assertTrue(result.isProvided());
        assertTrue(result.isValid());
        assertTrue(result.isHttps());
        assertEquals("careers.acme-corp.com", result.getDomain());
        assertFalse(result.isIpAddress());
        assertFalse(result.isUrlShortener());
        assertFalse(result.isRiskyTld());
        assertTrue(result.getPositiveIndicators().stream().anyMatch(s -> s.contains("HTTPS")));
        assertTrue(result.getPositiveIndicators().stream().anyMatch(s -> s.contains("standard")));
    }

    @Test
    @DisplayName("2. HTTP URL: should flag unencrypted transport as risk indicator")
    void testHttpUrl() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("http://example-corp.com");

        assertTrue(result.isProvided());
        assertTrue(result.isValid());
        assertFalse(result.isHttps());
        assertEquals("example-corp.com", result.getDomain());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("insecure http")));
    }

    @Test
    @DisplayName("3. Invalid URL: should flag malformed URL syntax")
    void testInvalidUrl() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("http://not a valid url syntax %%");

        assertTrue(result.isProvided());
        assertFalse(result.isValid());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("invalid") || s.toLowerCase().contains("malformed")));
    }

    @Test
    @DisplayName("4. IP-address URL: should detect raw IP address instead of domain")
    void testIpAddressUrl() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("http://192.168.1.50:8080/careers");

        assertTrue(result.isProvided());
        assertTrue(result.isIpAddress());
        assertTrue(result.isPrivateOrLocal());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("raw ip address")));
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("private network")));
    }

    @Test
    @DisplayName("5. URL with path and query: extracts clean domain while preserving original")
    void testUrlWithPathAndQuery() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("https://techcorp.com/jobs/apply?req_id=9872&source=direct#form");

        assertTrue(result.isValid());
        assertTrue(result.isHttps());
        assertEquals("techcorp.com", result.getDomain());
        assertEquals("https://techcorp.com/jobs/apply?req_id=9872&source=direct#form", result.getOriginalUrl());
    }

    @Test
    @DisplayName("6. URL with www: strips leading www prefix cleanly")
    void testUrlWithWww() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("https://www.globaltech.com");

        assertTrue(result.isValid());
        assertEquals("globaltech.com", result.getDomain());
    }

    @Test
    @DisplayName("7. URL with subdomain: handles normal and excessive subdomain hierarchies")
    void testUrlWithSubdomain() {
        // Normal subdomain
        UrlVerificationResult normalResult = urlVerificationService.verifyUrl("https://careers.emea.globalcorp.com");
        assertTrue(normalResult.isValid());
        assertFalse(normalResult.isExcessiveSubdomains());

        // Excessive subdomains
        UrlVerificationResult excessiveResult = urlVerificationService.verifyUrl("https://a.b.c.d.suspicious-portal.com");
        assertTrue(excessiveResult.isValid());
        assertTrue(excessiveResult.isExcessiveSubdomains());
        assertTrue(excessiveResult.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("subdomain")));
    }

    @Test
    @DisplayName("8. URL-shortener pattern: flags link shorteners appropriately")
    void testUrlShortener() {
        UrlVerificationResult bitlyResult = urlVerificationService.verifyUrl("https://bit.ly/fast-hiring-offer");
        assertTrue(bitlyResult.isUrlShortener());
        assertTrue(bitlyResult.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("shortening")));

        UrlVerificationResult tinyurlResult = urlVerificationService.verifyUrl("http://tinyurl.com/job-apply");
        assertTrue(tinyurlResult.isUrlShortener());

        UrlVerificationResult telegramResult = urlVerificationService.verifyUrl("https://t.me/recruiter_chat");
        assertTrue(telegramResult.isUrlShortener());
    }

    @Test
    @DisplayName("9. Missing URL: gracefully handles null or blank input without error")
    void testMissingUrl() {
        UrlVerificationResult nullResult = urlVerificationService.verifyUrl(null);
        assertFalse(nullResult.isProvided());
        assertFalse(nullResult.isValid());
        assertEquals("", nullResult.getDomain());

        UrlVerificationResult blankResult = urlVerificationService.verifyUrl("    ");
        assertFalse(blankResult.isProvided());
        assertFalse(blankResult.isValid());
    }

    @Test
    @DisplayName("10. Risky TLD and suspicious keywords detection")
    void testRiskyTldAndKeywords() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("https://careers-login-portal.xyz/pay");
        assertTrue(result.isRiskyTld());
        assertTrue(result.isSuspiciousKeywords());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("top-level domain")));
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("credential theft") || s.toLowerCase().contains("phishing")));
    }

    @Test
    @DisplayName("11. URL obfuscation: flags userinfo trick, double slashes, and hex percent-encoding")
    void testUrlObfuscationPattern() {
        // Userinfo trick: http://trusted.com@evil-site.com
        UrlVerificationResult userinfoResult = urlVerificationService.verifyUrl("https://microsoft.com@evil-phishing.xyz/login");
        assertTrue(userinfoResult.getRiskIndicators().stream()
                .anyMatch(s -> s.toLowerCase().contains("suspicious character patterns") || s.toLowerCase().contains("obfuscated")),
                "Should detect userinfo obfuscation");

        // Double slash in path
        UrlVerificationResult doubleSlashResult = urlVerificationService.verifyUrl("https://example.com//evil.com/apply");
        assertTrue(doubleSlashResult.getRiskIndicators().stream()
                .anyMatch(s -> s.toLowerCase().contains("suspicious character patterns") || s.toLowerCase().contains("obfuscated")),
                "Should detect double slash obfuscation");

        // Hex percent encoding
        UrlVerificationResult hexResult = urlVerificationService.verifyUrl("https://example.com/%2f%2fcareers");
        assertTrue(hexResult.getRiskIndicators().stream()
                .anyMatch(s -> s.toLowerCase().contains("suspicious character patterns") || s.toLowerCase().contains("obfuscated")),
                "Should detect hex percent-encoding pattern");
    }

    @Test
    @DisplayName("12. Unconventional protocol schemes: flags ftp, file, and websocket URI schemes")
    void testUnconventionalProtocolSchemes() {
        UrlVerificationResult ftpResult = urlVerificationService.verifyUrl("ftp://ftp.acme.com/jobs/spec.pdf");
        assertFalse(ftpResult.isHttps());
        assertTrue(ftpResult.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("unconventional protocol scheme")));

        UrlVerificationResult fileResult = urlVerificationService.verifyUrl("file://corporate-intranet/jobs/offer.pdf");
        assertFalse(fileResult.isHttps());
        assertTrue(fileResult.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("unconventional protocol scheme")));

        UrlVerificationResult wsResult = urlVerificationService.verifyUrl("ws://socket.company.com/feed");
        assertFalse(wsResult.isHttps());
        assertTrue(wsResult.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("unconventional protocol scheme")));
    }

    @Test
    @DisplayName("13. Whitespace and case normalization: normalizes mixed case and untrimmed URLs")
    void testWhitespaceAndCaseNormalization() {
        UrlVerificationResult result = urlVerificationService.verifyUrl("   HTTPS://WWW.CAREERS.GOOGLE.COM/JOBS/REQ123/   ");

        assertTrue(result.isProvided());
        assertTrue(result.isValid());
        assertTrue(result.isHttps());
        assertEquals("careers.google.com", result.getDomain());
        assertEquals("https://www.careers.google.com/jobs/req123/", result.getOriginalUrl().toLowerCase());
    }

    @Test
    @DisplayName("14. Local and private network ranges: identifies loopback, private intranet, and internal hosts")
    void testLocalAndPrivateNetworkRanges() {
        String[] localUrls = {
                "http://localhost:8080/careers",
                "https://127.0.0.1:443/login",
                "http://10.0.0.1/apply",
                "http://192.168.1.1/jobs",
                "http://172.16.0.10:9000/portal",
                "http://dev.corp.internal/apply",
                "http://portal.company.local/job"
        };

        for (String url : localUrls) {
            UrlVerificationResult result = urlVerificationService.verifyUrl(url);
            assertTrue(result.isPrivateOrLocal(), "Should identify private/local host: " + url);
            assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("local or private network")),
                    "Should flag private network indicator for: " + url);
        }
    }

    @Test
    @DisplayName("15. Strict offline guarantee: executes without network latency or socket connections")
    void testStrictOfflineGuarantee() {
        long startTime = System.currentTimeMillis();

        // Verify across multiple complex URLs
        for (int i = 0; i < 50; i++) {
            UrlVerificationResult result = urlVerificationService.verifyUrl("https://careers.company" + i + ".org/apply?ref=board#top");
            assertTrue(result.isValid());
            assertNotNull(result.getDomain());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        // 50 offline evaluations should take well under 500ms (typically < 30ms)
        assertTrue(elapsed < 500, "Offline verification must be fast and non-blocking, took: " + elapsed + "ms");
    }
}
