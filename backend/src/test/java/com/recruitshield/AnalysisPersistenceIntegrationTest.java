package com.recruitshield;

import com.recruitshield.dto.AiPredictionResponse;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.entity.Offer;
import com.recruitshield.entity.RiskReport;
import com.recruitshield.repository.OfferRepository;
import com.recruitshield.repository.RiskReportRepository;
import com.recruitshield.service.AiServiceClient;
import com.recruitshield.service.AnalysisService;
import com.recruitshield.service.RecruiterVerificationService;
import com.recruitshield.service.UrlVerificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class AnalysisPersistenceIntegrationTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private RiskReportRepository riskReportRepository;

    private final List<Long> trackedOfferIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long offerId : trackedOfferIds) {
            try {
                if (offerRepository.existsById(offerId)) {
                    offerRepository.deleteById(offerId);
                }
            } catch (Exception ignored) {
            }
        }
        trackedOfferIds.clear();
    }

    @Test
    @DisplayName("1. Complete flow: POST analyze-offer persists Offer and RiskReport and establishes bidirectional relationship")
    void testCompleteAnalyzeOfferFlow() {
        String uniqueCompanyName = "CloudCore Technologies " + System.currentTimeMillis();
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Urgent selection! Pay Rs 3500 refundable security deposit for company hardware.")
                .companyName(uniqueCompanyName)
                .companyWebsite("https://cloudcore-fake.top")
                .recruiterEmail("hr-cloudcore@gmail.com")
                .receivedVia("WhatsApp")
                .build();

        long initialOfferCount = offerRepository.count();
        long initialReportCount = riskReportRepository.count();

        VerifyResponse response = analysisService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.getRiskScore() >= 60);
        assertEquals("HIGHLY_SUSPICIOUS", response.getStatus());
        assertFalse(response.getRedFlags().isEmpty());

        assertEquals(initialOfferCount + 1, offerRepository.count(), "Offer should be persisted in MySQL");
        assertEquals(initialReportCount + 1, riskReportRepository.count(), "RiskReport should be persisted in MySQL");

        // Verify the persisted record matches the analyzed offer
        Offer savedOffer = offerRepository.findAll().stream()
                .filter(o -> uniqueCompanyName.equals(o.getCompanyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedOffer, "Persisted offer must be found");
        trackedOfferIds.add(savedOffer.getId());
        assertEquals("WhatsApp", savedOffer.getReceivedVia());

        Optional<RiskReport> savedReport = riskReportRepository.findByOfferId(savedOffer.getId());
        assertTrue(savedReport.isPresent(), "RiskReport associated with Offer must exist");
        assertEquals(response.getRiskScore(), savedReport.get().getRiskScore());
        assertEquals(response.getStatus(), savedReport.get().getStatus());
        assertEquals(response.getRiskLevel(), savedReport.get().getRiskLevel());
        assertFalse(savedReport.get().getRedFlags().isEmpty());

        // Verify relationship integrity
        assertEquals(savedOffer.getId(), savedReport.get().getOffer().getId(), "RiskReport must reference parent Offer ID");
    }

    @Test
    @DisplayName("2. Risk report fields: Verify persisted entity fields and analysis response integrity")
    void testRiskReportPersistedFieldsComprehensive() {
        String uniqueCompany = "DataVantage Corp " + System.currentTimeMillis();
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Congratulations! Selected for Lead Analyst role. Pay Rs 2000 registration fee. Contact hr@gmail.com.")
                .companyName(uniqueCompany)
                .recruiterEmail("hr.recruiter@gmail.com")
                .receivedVia("WhatsApp")
                .build();

        VerifyResponse response = analysisService.analyzeOffer(request);
        assertNotNull(response);

        Offer savedOffer = offerRepository.findAll().stream()
                .filter(o -> uniqueCompany.equals(o.getCompanyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedOffer, "Offer must be saved");
        trackedOfferIds.add(savedOffer.getId());

        RiskReport savedReport = riskReportRepository.findByOfferId(savedOffer.getId())
                .orElseThrow(() -> new AssertionError("RiskReport not found for offer"));

        // Persisted entity assertions
        assertEquals(response.getRiskScore(), savedReport.getRiskScore(), "Persisted riskScore must match response");
        assertEquals(response.getStatus(), savedReport.getStatus(), "Persisted status must match response");
        assertEquals(response.getRiskLevel(), savedReport.getRiskLevel(), "Persisted riskLevel must match response");
        assertEquals(response.getAnalysisSummary(), savedReport.getSummary(), "Persisted summary must match executive summary");
        assertEquals(response.getRedFlags().size(), savedReport.getRedFlags().size(), "Persisted redFlags count must match");
        assertEquals(response.getPositiveSignals().size(), savedReport.getPositiveSignals().size(), "Persisted positiveSignals count must match");
        assertEquals(response.getRecommendations().size(), savedReport.getRecommendations().size(), "Persisted recommendations count must match");
        assertNotNull(savedReport.getCreatedAt(), "CreatedAt timestamp must be generated");

        // Associated response contract assertions
        assertTrue(response.getRuleBasedScore() >= 60, "Rule-based score must be computed");
        assertNotNull(response.getRecommendations(), "Recommendations must be available");
    }

    @Test
    @DisplayName("3. Multiple analyses: Successive analyses create distinct non-overlapping records")
    void testMultipleAnalysesDoNotOverwriteEachOther() {
        String companyA = "AlphaTech " + System.currentTimeMillis();
        VerifyRequest requestA = VerifyRequest.builder()
                .offerText("Software Engineer offer at AlphaTech following 3 interview rounds.")
                .companyName(companyA)
                .companyWebsite("https://alphatech.com")
                .recruiterEmail("hr@alphatech.com")
                .receivedVia("LinkedIn")
                .build();

        String companyB = "BetaExtortion " + System.currentTimeMillis();
        VerifyRequest requestB = VerifyRequest.builder()
                .offerText("Direct joining! Pay Rs 5000 deposit immediately. Limited spots.")
                .companyName(companyB)
                .recruiterEmail("scam@gmail.com")
                .receivedVia("WhatsApp")
                .build();

        VerifyResponse responseA = analysisService.analyzeOffer(requestA);
        VerifyResponse responseB = analysisService.analyzeOffer(requestB);

        Offer offerA = offerRepository.findAll().stream()
                .filter(o -> companyA.equals(o.getCompanyName()))
                .findFirst()
                .orElse(null);
        Offer offerB = offerRepository.findAll().stream()
                .filter(o -> companyB.equals(o.getCompanyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(offerA, "Offer A must be persisted");
        assertNotNull(offerB, "Offer B must be persisted");
        trackedOfferIds.add(offerA.getId());
        trackedOfferIds.add(offerB.getId());

        assertNotEquals(offerA.getId(), offerB.getId(), "Each offer must have a unique primary key");

        RiskReport reportA = riskReportRepository.findByOfferId(offerA.getId()).orElse(null);
        RiskReport reportB = riskReportRepository.findByOfferId(offerB.getId()).orElse(null);

        assertNotNull(reportA, "Report A must exist");
        assertNotNull(reportB, "Report B must exist");
        assertNotEquals(reportA.getId(), reportB.getId(), "Each report must have a unique primary key");

        // Verify data separation
        assertEquals(responseA.getRiskScore(), reportA.getRiskScore());
        assertEquals(responseB.getRiskScore(), reportB.getRiskScore());
        assertTrue(reportA.getRiskScore() < reportB.getRiskScore(), "Offer A (genuine) must have lower score than Offer B (scam)");
        assertEquals(offerA.getId(), reportA.getOffer().getId());
        assertEquals(offerB.getId(), reportB.getOffer().getId());
    }

    @Test
    @DisplayName("4. Database isolation: Cascade deletion cleans up Offer and associated RiskReport cleanly")
    void testDatabaseIsolationAndCascadeCleanup() {
        String company = "TempIsolation Corp " + System.currentTimeMillis();
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Offer to be analyzed and subsequently cleaned up.")
                .companyName(company)
                .build();

        analysisService.analyzeOffer(request);

        Offer savedOffer = offerRepository.findAll().stream()
                .filter(o -> company.equals(o.getCompanyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedOffer, "Offer must be saved");
        Long offerId = savedOffer.getId();

        Optional<RiskReport> savedReport = riskReportRepository.findByOfferId(offerId);
        assertTrue(savedReport.isPresent(), "RiskReport must exist before cleanup");
        Long reportId = savedReport.get().getId();

        // Perform cascade delete
        offerRepository.deleteById(offerId);

        // Verify both offer and report are gone
        assertFalse(offerRepository.existsById(offerId), "Offer must be deleted");
        assertFalse(riskReportRepository.existsById(reportId), "RiskReport must be cascaded and deleted");
    }

    @Test
    @DisplayName("5. Persistence with AI available: Simulated AI prediction contributes to score and is reflected in persisted summary")
    void testPersistenceWithAiAvailable() {
        AiServiceClient mockAiClient = mock(AiServiceClient.class);
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(0.85)
                        .classification("SUSPICIOUS")
                        .model("tfidf-logistic-regression")
                        .build()
        ));

        AnalysisService customService = new AnalysisService(
                offerRepository,
                riskReportRepository,
                new UrlVerificationService(),
                new RecruiterVerificationService(),
                mockAiClient
        );

        String uniqueCompany = "AiEnabled Enterprise " + System.currentTimeMillis();
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Direct selection! Pay Rs 1500 application fee.")
                .companyName(uniqueCompany)
                .build();

        VerifyResponse response = customService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.isAiAnalysisAvailable(), "AI analysis should be flagged as available");
        assertEquals(0.85, response.getAiRiskProbability());
        assertEquals("SUSPICIOUS", response.getAiClassification());

        Offer savedOffer = offerRepository.findAll().stream()
                .filter(o -> uniqueCompany.equals(o.getCompanyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedOffer, "Offer must be persisted");
        trackedOfferIds.add(savedOffer.getId());

        RiskReport savedReport = riskReportRepository.findByOfferId(savedOffer.getId())
                .orElseThrow(() -> new AssertionError("RiskReport must be persisted"));

        assertEquals(response.getRiskScore(), savedReport.getRiskScore(), "Persisted riskScore must match composite final score");
        assertTrue(savedReport.getSummary().contains("AI ML signals"), "Persisted summary must indicate AI signals incorporated");
    }

    @Test
    @DisplayName("6. Persistence with AI unavailable: Offer and rule-based report persisted cleanly with null/false AI fields")
    void testPersistenceWithAiUnavailable() {
        // AnalysisService with null AI client (service disabled/offline)
        AnalysisService offlineService = new AnalysisService(
                offerRepository,
                riskReportRepository,
                new UrlVerificationService(),
                new RecruiterVerificationService(),
                null
        );

        String uniqueCompany = "OfflineAi Corp " + System.currentTimeMillis();
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Immediate joining without interview! Pay Rs 2500 training fee.")
                .companyName(uniqueCompany)
                .build();

        VerifyResponse response = offlineService.analyzeOffer(request);

        assertNotNull(response);
        assertFalse(response.isAiAnalysisAvailable(), "AI analysis must be marked unavailable");
        assertNull(response.getAiRiskProbability(), "AI risk probability must be null when AI is unavailable");
        assertNull(response.getAiClassification(), "AI classification must be null when AI is unavailable");
        assertEquals(response.getRuleBasedScore(), response.getRiskScore(), "Risk score must equal rule-based score when AI offline");

        Offer savedOffer = offerRepository.findAll().stream()
                .filter(o -> uniqueCompany.equals(o.getCompanyName()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedOffer, "Offer must be persisted");
        trackedOfferIds.add(savedOffer.getId());

        RiskReport savedReport = riskReportRepository.findByOfferId(savedOffer.getId())
                .orElseThrow(() -> new AssertionError("RiskReport must be persisted"));

        assertEquals(response.getRiskScore(), savedReport.getRiskScore());
        assertFalse(savedReport.getSummary().contains("AI ML signals"), "Persisted summary must NOT claim AI signals when AI offline");
    }

    @Test
    @DisplayName("7. Invalid request: Blank offer rejected before any MySQL persistence occurs")
    void testInvalidRequestNoPersistence() {
        long initialOfferCount = offerRepository.count();
        long initialReportCount = riskReportRepository.count();

        VerifyRequest invalidRequest = VerifyRequest.builder()
                .offerText("   ")
                .recruiterEmail("invalid-email")
                .build();

        assertThrows(IllegalArgumentException.class, () -> analysisService.analyzeOffer(invalidRequest));

        assertEquals(initialOfferCount, offerRepository.count(), "No offer should be saved for invalid request");
        assertEquals(initialReportCount, riskReportRepository.count(), "No report should be saved for invalid request");
    }

    @Test
    @DisplayName("8. Database error handling: Analysis still succeeds and returns response if MySQL save fails")
    void testDatabaseErrorHandlingGraceful() {
        OfferRepository mockOfferRepo = Mockito.mock(OfferRepository.class);
        RiskReportRepository mockReportRepo = Mockito.mock(RiskReportRepository.class);

        when(mockOfferRepo.save(any(Offer.class))).thenThrow(new DataRetrievalFailureException("Simulated DB connection failure"));

        AnalysisService resilientService = new AnalysisService(mockOfferRepo, mockReportRepo);

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Direct joining! Pay Rs 1000 application fee.")
                .companyName("Resilient Test Inc")
                .recruiterEmail("hr@gmail.com")
                .receivedVia("WhatsApp")
                .build();

        // Must not throw exception; returns calculated response
        VerifyResponse response = assertDoesNotThrow(() -> resilientService.analyzeOffer(request));

        assertNotNull(response);
        assertTrue(response.getRiskScore() >= 40);
        assertEquals("HIGHLY_SUSPICIOUS", response.getStatus());
        verify(mockOfferRepo, times(1)).save(any(Offer.class));
    }
}
