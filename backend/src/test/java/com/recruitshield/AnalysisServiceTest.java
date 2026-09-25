package com.recruitshield;

import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisServiceTest {

    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService();
    }

    @Test
    @DisplayName("Should detect highly suspicious offer with payment, urgency, and interview bypass")
    void testSuspiciousOffer() {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Urgent hiring! Direct joining without interview. You must pay Rs 2500 registration fee. Limited spots available.")
                .recruiterEmail("hr-recruiter@gmail.com")
                .receivedVia("WhatsApp")
                .build();

        VerifyResponse response = analysisService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.getRiskScore() >= 60, "Expected high risk score for multi-flag offer");
        assertEquals("HIGHLY_SUSPICIOUS", response.getStatus());
        assertEquals("HIGH", response.getRiskLevel());
        assertFalse(response.getRedFlags().isEmpty());
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.toLowerCase().contains("advance payment") || f.toLowerCase().contains("registration fee")));
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.toLowerCase().contains("interview")));
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.toLowerCase().contains("urgency")));
        assertFalse(response.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Should classify normal corporate recruitment as likely genuine")
    void testNormalLookingOffer() {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("We are pleased to offer you the Associate Software Engineer role following your completion of 3 technical interview rounds and coding assessment.")
                .companyName("Acme Technologies")
                .companyWebsite("https://acmetech.com/careers")
                .recruiterEmail("talent@acmetech.com")
                .receivedVia("Email")
                .build();

        VerifyResponse response = analysisService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.getRiskScore() < 30, "Expected low risk score for standard corporate offer");
        assertEquals("LIKELY_GENUINE", response.getStatus());
        assertEquals("LOW", response.getRiskLevel());
        assertFalse(response.getPositiveSignals().isEmpty(), "Expected positive signals to be populated");
        assertTrue(response.getPositiveSignals().stream().anyMatch(s -> s.contains("No upfront payment")));
    }

    @Test
    @DisplayName("Should identify upfront registration/security fee demands")
    void testPaymentRequest() {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Please transfer a refundable security deposit of 5000 to our UPI id for your laptop allocation.")
                .build();

        VerifyResponse response = analysisService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.getRiskScore() >= 40);
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.toLowerCase().contains("payment") || f.toLowerCase().contains("security deposit")));
    }

    @Test
    @DisplayName("Should flag public freemail domain for recruiter email")
    void testSuspiciousEmail() {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Congratulations on selection. Please review your appointment letter.")
                .recruiterEmail("recruiter.infosys@yahoo.com")
                .companyWebsite("https://infosys.com")
                .build();

        VerifyResponse response = analysisService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.toLowerCase().contains("freemail") || f.toLowerCase().contains("public")));
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.toLowerCase().contains("match")));
    }

    @Test
    @DisplayName("Should flag suspicious shortened URLs and untrusted domains")
    void testSuspiciousUrl() {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Fill your onboarding information at https://bit.ly/job-onboard-form to proceed.")
                .build();

        VerifyResponse response = analysisService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.toLowerCase().contains("shortened") || f.toLowerCase().contains("url")));
    }

    @Test
    @DisplayName("Should reject empty or blank offer text")
    void testEmptyOffer() {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("   ")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> analysisService.analyzeOffer(request));
        assertTrue(ex.getMessage().contains("cannot be empty or blank"));
    }

    @Test
    @DisplayName("Should reject malformed recruiter email format")
    void testInvalidEmail() {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Valid job offer text for software role.")
                .recruiterEmail("invalid-email-format")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> analysisService.analyzeOffer(request));
        assertTrue(ex.getMessage().contains("Invalid recruiter email format"));
    }
}
