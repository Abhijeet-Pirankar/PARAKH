package com.recruitshield;

import com.recruitshield.dto.AiPredictionResponse;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.service.AiServiceClient;
import com.recruitshield.service.AnalysisService;
import com.recruitshield.service.RecruiterVerificationService;
import com.recruitshield.service.UrlVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisIntegrationTest {

    @Mock
    private AiServiceClient mockAiClient;

    private final UrlVerificationService urlService = new UrlVerificationService();
    private final RecruiterVerificationService recruiterService = new RecruiterVerificationService();

    @Test
    @DisplayName("1. Legitimate offer: Combined score remains low and positive signals populated")
    void testLegitimateOffer() {
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(0.05)
                        .classification("LEGITIMATE")
                        .model("tfidf-logistic-regression")
                        .build()
        ));

        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("We are delighted to extend an offer for Associate Software Engineer at Infosys. Following your technical interview, your CTC will be 6.5 LPA.")
                .companyName("Infosys")
                .companyWebsite("https://infosys.com")
                .recruiterEmail("talent@infosys.com")
                .receivedVia("Email")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.isAiAnalysisAvailable(), "AI analysis should be available");
        assertEquals(0.05, response.getAiRiskProbability());
        assertEquals("LEGITIMATE", response.getAiClassification());
        assertEquals("LIKELY_GENUINE", response.getStatus());
        assertEquals("LOW", response.getRiskLevel());
        assertTrue(response.getRiskScore() < 30);
        assertTrue(response.getPositiveSignals().stream().anyMatch(s -> s.contains("AI model classified offer text as legitimate")));
    }

    @Test
    @DisplayName("2. Suspicious offer: AI elevates risk and flags suspicious narrative")
    void testSuspiciousOffer() {
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(0.92)
                        .classification("SUSPICIOUS")
                        .model("tfidf-logistic-regression")
                        .build()
        ));

        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Direct joining without interview! High salary 80,000 monthly with company laptop. Contact immediately on WhatsApp.")
                .recruiterEmail("hr@gmail.com")
                .receivedVia("WhatsApp")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.isAiAnalysisAvailable());
        assertEquals(0.92, response.getAiRiskProbability());
        assertEquals("SUSPICIOUS", response.getAiClassification());
        assertEquals("HIGHLY_SUSPICIOUS", response.getStatus());
        assertEquals("HIGH", response.getRiskLevel());
        assertTrue(response.getRiskScore() >= 60);
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.contains("AI model flagged offer text as suspicious")));
    }

    @Test
    @DisplayName("3. Fee/payment scam: Rule engine detects fee demand and AI contributes to composite risk")
    void testFeePaymentScam() {
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(0.95)
                        .classification("SUSPICIOUS")
                        .model("tfidf-logistic-regression")
                        .build()
        ));

        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Congratulations! Selected for Data Analyst job. Please pay Rs 2500 refundable security deposit to our UPI id.")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.isAiAnalysisAvailable());
        assertEquals(40, response.getRuleBasedScore());
        // Combined score: round(0.70 * 40 + 0.30 * 95) = round(28 + 28.5) = round(56.5) = 57 (or 57)
        int expectedScore = (int) Math.round((0.70 * 40) + (0.30 * 95));
        assertEquals(expectedScore, response.getRiskScore());
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.contains("Advance payment") || f.contains("registration fee")));
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.contains("AI model flagged")));
    }

    @Test
    @DisplayName("4. Phishing-style offer: Credential theft and OTP phishing flagged by rules and AI")
    void testPhishingOffer() {
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(0.98)
                        .classification("SUSPICIOUS")
                        .model("tfidf-logistic-regression")
                        .build()
        ));

        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("To verify your offer letter, please share the OTP sent to your phone and provide your net banking password.")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.isAiAnalysisAvailable());
        assertTrue(response.getRedFlags().stream().anyMatch(f -> f.contains("sensitive financial credentials") || f.contains("OTP")));
        assertTrue(response.getRiskScore() >= 50);
    }

    @Test
    @DisplayName("5. Empty input: Blank or whitespace offer text rejected with 400 validation error")
    void testEmptyInput() {
        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest emptyRequest = VerifyRequest.builder().offerText("").build();
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> service.analyzeOffer(emptyRequest));
        assertTrue(ex1.getMessage().contains("cannot be empty or blank"));

        VerifyRequest whitespaceRequest = VerifyRequest.builder().offerText("    \n\t   ").build();
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> service.analyzeOffer(whitespaceRequest));
        assertTrue(ex2.getMessage().contains("cannot be empty or blank"));
    }

    @Test
    @DisplayName("6. Malformed request: Invalid email format and oversized payload rejected")
    void testMalformedRequest() {
        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        // Invalid email
        VerifyRequest invalidEmailRequest = VerifyRequest.builder()
                .offerText("Standard software engineer offer text.")
                .recruiterEmail("not-a-valid-email")
                .build();
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> service.analyzeOffer(invalidEmailRequest));
        assertTrue(ex1.getMessage().contains("Invalid recruiter email format"));

        // Oversized input (> 50,000 characters)
        String hugeText = "A".repeat(50001);
        VerifyRequest oversizedRequest = VerifyRequest.builder().offerText(hugeText).build();
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> service.analyzeOffer(oversizedRequest));
        assertTrue(ex2.getMessage().contains("exceeds maximum supported size"));
    }

    @Test
    @DisplayName("7. Python AI service unavailable: Fallback to rule engine, aiAnalysisAvailable is false")
    void testPythonAiUnavailable() {
        // AI client returns empty (simulating timeout or connection refused)
        when(mockAiClient.predict(anyString())).thenReturn(Optional.empty());

        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Urgent hiring! Direct selection without interview. Pay Rs 2500 registration fee.")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertFalse(response.isAiAnalysisAvailable(), "AI analysis should be marked unavailable");
        assertNull(response.getAiRiskProbability(), "AI risk probability should be null when offline");
        assertNull(response.getAiClassification(), "AI classification should be null when offline");
        assertEquals(response.getRuleBasedScore(), response.getRiskScore(), "Final score must equal rule-based score on fallback");
        assertTrue(response.getRiskScore() >= 60);
    }

    @Test
    @DisplayName("8. Invalid/unexpected AI response: Missing riskProbability falls back smoothly")
    void testInvalidAiResponse() {
        // AI client returns object with null probability
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(null)
                        .classification(null)
                        .model("corrupted")
                        .build()
        ));

        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Please review your software engineering offer letter.")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertFalse(response.isAiAnalysisAvailable(), "Should fallback to rule-based when response is invalid");
        assertNull(response.getAiRiskProbability());
        assertEquals(response.getRuleBasedScore(), response.getRiskScore());
    }

    @Test
    @DisplayName("9. Rule engine working without AI: null AiServiceClient uses pure heuristics")
    void testRuleEngineWithoutAi() {
        // AnalysisService initialized with null AiServiceClient
        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, null);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Direct joining without interview! Earn 50000 per day. Pay Rs 3000 advance payment.")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertFalse(response.isAiAnalysisAvailable());
        assertNull(response.getAiRiskProbability());
        assertEquals(response.getRuleBasedScore(), response.getRiskScore());
        assertTrue(response.getRiskScore() >= 80);
        assertEquals("HIGHLY_SUSPICIOUS", response.getStatus());
    }

    @Test
    @DisplayName("10. Complete Java -> Python -> Java analysis flow: Correct combination formula applied")
    void testCompleteJavaPythonJavaFlow() {
        // Simulates realistic prediction: 0.80 risk probability
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(0.80)
                        .classification("SUSPICIOUS")
                        .model("tfidf-logistic-regression")
                        .build()
        ));

        AnalysisService service = new AnalysisService(null, null, urlService, recruiterService, mockAiClient);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Immediate joining! Direct selection without interview. Limited spots.")
                .build();

        VerifyResponse response = service.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.isAiAnalysisAvailable());
        assertEquals(0.80, response.getAiRiskProbability());
        assertEquals("SUSPICIOUS", response.getAiClassification());

        // Rule-based score: instant joining (+30) + urgency (+15) = 45
        assertEquals(45, response.getRuleBasedScore());

        // Exact formula: finalRiskScore = round(0.70 * 45 + 0.30 * 80) = round(31.5 + 24.0) = round(55.5) = 56
        int expectedFinal = (int) Math.round((0.70 * 45) + (0.30 * 80));
        assertEquals(expectedFinal, response.getRiskScore());
        assertEquals("NEEDS_VERIFICATION", response.getStatus());
        assertTrue(response.getAnalysisSummary().contains("AI ML signals"));
    }
}
