package com.recruitshield;

import com.recruitshield.dto.RecruiterVerificationResult;
import com.recruitshield.service.RecruiterVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecruiterVerificationServiceTest {

    private RecruiterVerificationService recruiterVerificationService;

    @BeforeEach
    void setUp() {
        recruiterVerificationService = new RecruiterVerificationService();
    }

    @Test
    @DisplayName("1. Corporate email matching company domain: confirms domain correlation and corporate signal")
    void testCorporateEmailMatchingDomain() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "talent.acquisition@infosys.com",
                "https://www.infosys.com/careers",
                "Email",
                "Official offer letter for Systems Engineer."
        );

        assertTrue(result.isEmailProvided());
        assertTrue(result.isEmailValid());
        assertEquals("infosys.com", result.getEmailDomain());
        assertEquals("infosys.com", result.getCompanyDomain());
        assertEquals(Boolean.TRUE, result.getDomainMatch());
        assertFalse(result.isPublicFreemail());
        assertFalse(result.isDisposableEmail());
        assertFalse(result.isInformalChannel());
        assertTrue(result.getPositiveIndicators().stream().anyMatch(s -> s.toLowerCase().contains("matches")));
        assertTrue(result.getPositiveIndicators().stream().anyMatch(s -> s.toLowerCase().contains("corporate email domain")));
    }

    @Test
    @DisplayName("2. Gmail recruiter: identifies public webmail provider")
    void testGmailRecruiter() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "google.recruiter.hr@gmail.com",
                "https://careers.google.com",
                "Email",
                "Congratulations on your Google selection."
        );

        assertTrue(result.isEmailProvided());
        assertTrue(result.isEmailValid());
        assertTrue(result.isPublicFreemail());
        assertEquals(Boolean.FALSE, result.getDomainMatch());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("public/free email domain")));
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("does not match")));
    }

    @Test
    @DisplayName("3. Yahoo recruiter: identifies public webmail provider and flags warning")
    void testYahooRecruiter() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "infosys_careers_hr@yahoo.com",
                "https://infosys.com",
                "Email",
                "Offer of appointment."
        );

        assertTrue(result.isEmailProvided());
        assertTrue(result.isEmailValid());
        assertTrue(result.isPublicFreemail());
        assertEquals(Boolean.FALSE, result.getDomainMatch());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("public/free email domain")));
    }

    @Test
    @DisplayName("4. Domain mismatch: detects mismatch when corporate email domain differs from website domain")
    void testDomainMismatch() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "hr@tech-recruitment-agency.com",
                "https://microsoft.com",
                "Email",
                "Direct client placement offer."
        );

        assertTrue(result.isEmailProvided());
        assertTrue(result.isEmailValid());
        assertFalse(result.isPublicFreemail());
        assertEquals(Boolean.FALSE, result.getDomainMatch());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("does not match")));
    }

    @Test
    @DisplayName("5. Missing recruiter email: handled gracefully without crashing or false mismatch warning")
    void testMissingRecruiterEmail() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                null,
                "https://acmecorp.com",
                "Company Portal",
                "Direct job vacancy announcement on job board."
        );

        assertFalse(result.isEmailProvided());
        assertFalse(result.isEmailValid());
        assertNull(result.getDomainMatch()); // Must not claim mismatch when email is absent
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("email was not provided")));
    }

    @Test
    @DisplayName("6. Invalid email format: flags syntactically malformed email addresses")
    void testInvalidEmailFormat() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "not-an-email-at-all",
                "https://company.com",
                "Email",
                "Job offer text."
        );

        assertTrue(result.isEmailProvided());
        assertFalse(result.isEmailValid());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("invalid")));
    }

    @Test
    @DisplayName("7. WhatsApp/Telegram communication: flags unmonitored chat channels as risk indicators")
    void testInformalCommunicationChannel() {
        // WhatsApp
        RecruiterVerificationResult waResult = recruiterVerificationService.verifyRecruiter(
                "recruiter@acme.com",
                "https://acme.com",
                "WhatsApp",
                "Contact me on WhatsApp to finalize hiring."
        );
        assertTrue(waResult.isInformalChannel());
        assertTrue(waResult.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("informal channel")));

        // Telegram
        RecruiterVerificationResult tgResult = recruiterVerificationService.verifyRecruiter(
                "recruiter@acme.com",
                "https://acme.com",
                "Telegram",
                "Join telegram group for task briefings."
        );
        assertTrue(tgResult.isInformalChannel());
        assertTrue(tgResult.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("informal channel")));
    }

    @Test
    @DisplayName("8. Disposable email domains: detects temporary/anonymous mail providers")
    void testDisposableEmailProvider() {
        String[] disposableEmails = {
                "scam_hr@tempmail.com",
                "recruiter123@mailinator.com",
                "jobs@throwaway.net",
                "career@yopmail.com",
                "applicant@10minutemail.com",
                "hr@guerrillamail.com"
        };

        for (String email : disposableEmails) {
            RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                    email,
                    "https://company.com",
                    "Email",
                    "Offer letter."
            );
            assertTrue(result.isDisposableEmail(), "Should detect disposable email: " + email);
            assertFalse(result.isPublicFreemail(), "Disposable email should not be classified as public freemail: " + email);
            assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("disposable or anonymous")),
                    "Should contain disposable email risk indicator for: " + email);
        }
    }

    @Test
    @DisplayName("9. Randomized recruiter usernames: detects automated numbering patterns")
    void testRandomizedRecruiterUsername() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "recruiter981723@corporate-consulting.com",
                "https://corporate-consulting.com",
                "Email",
                "Offer of appointment."
        );

        assertTrue(result.isSuspiciousPattern(), "Should detect randomized/automated naming pattern");
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("randomized naming patterns")));

        // Normal username with short number should NOT trigger suspicious pattern
        RecruiterVerificationResult normalResult = recruiterVerificationService.verifyRecruiter(
                "john.doe2@corporate-consulting.com",
                "https://corporate-consulting.com",
                "Email",
                "Offer of appointment."
        );
        assertFalse(normalResult.isSuspiciousPattern(), "Standard username with small digit should not be flagged");
    }

    @Test
    @DisplayName("10. In-text email extraction fallback: extracts recruiter email when parameter is omitted")
    void testInTextEmailExtractionFallback() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                null,
                "https://innovatech.io",
                "Email",
                "We are hiring! Please send your signed offer to careers@innovatech.io within 48 hours."
        );

        assertTrue(result.isEmailProvided(), "Should detect email provided from text fallback");
        assertTrue(result.isEmailValid());
        assertEquals("careers@innovatech.io", result.getEmail());
        assertEquals("innovatech.io", result.getEmailDomain());
        assertEquals("innovatech.io", result.getCompanyDomain());
        assertEquals(Boolean.TRUE, result.getDomainMatch());
        assertFalse(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("email was not provided")),
                "Must not claim email was not provided when found in text");
    }

    @Test
    @DisplayName("11. Whitespace and case insensitivity: normalizes mixed casing and padding")
    void testWhitespaceAndCaseNormalization() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "   TALENT.LEAD@MICROSOFT.COM   ",
                "   HTTPS://WWW.MICROSOFT.COM/CAREERS   ",
                "   EMAIL   ",
                "Official offer of employment."
        );

        assertTrue(result.isEmailProvided());
        assertTrue(result.isEmailValid());
        assertEquals("microsoft.com", result.getEmailDomain());
        assertEquals("microsoft.com", result.getCompanyDomain());
        assertEquals(Boolean.TRUE, result.getDomainMatch());
        assertFalse(result.isInformalChannel());
        assertTrue(result.getPositiveIndicators().stream().anyMatch(s -> s.toLowerCase().contains("official professional channel")));
    }

    @Test
    @DisplayName("12. Freemail with company website: flags both public freemail and domain mismatch")
    void testFreemailWithCompanyWebsiteMismatch() {
        RecruiterVerificationResult result = recruiterVerificationService.verifyRecruiter(
                "amazon.campus.recruiter@gmail.com",
                "https://www.amazon.jobs",
                "Email",
                "Welcome to Amazon!"
        );

        assertTrue(result.isPublicFreemail());
        assertEquals("gmail.com", result.getEmailDomain());
        assertEquals("amazon.jobs", result.getCompanyDomain());
        assertEquals(Boolean.FALSE, result.getDomainMatch());
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("public/free email")));
        assertTrue(result.getRiskIndicators().stream().anyMatch(s -> s.toLowerCase().contains("does not match")));
    }
}
