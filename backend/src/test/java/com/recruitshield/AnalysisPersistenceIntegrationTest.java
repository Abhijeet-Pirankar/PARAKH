package com.recruitshield;

import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.entity.Offer;
import com.recruitshield.entity.RiskReport;
import com.recruitshield.repository.OfferRepository;
import com.recruitshield.repository.RiskReportRepository;
import com.recruitshield.service.AnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class AnalysisPersistenceIntegrationTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private RiskReportRepository riskReportRepository;

    @Test
    @DisplayName("Complete flow: POST analyze-offer persists Offer and RiskReport into MySQL and returns response")
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
        assertEquals("WhatsApp", savedOffer.getReceivedVia());

        Optional<RiskReport> savedReport = riskReportRepository.findByOfferId(savedOffer.getId());
        assertTrue(savedReport.isPresent(), "RiskReport associated with Offer must exist");
        assertEquals(response.getRiskScore(), savedReport.get().getRiskScore());
        assertEquals(response.getStatus(), savedReport.get().getStatus());
        assertFalse(savedReport.get().getRedFlags().isEmpty());
    }

    @Test
    @DisplayName("Invalid request should be rejected before any persistence occurs")
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
    @DisplayName("Database error handling: Analysis still succeeds and returns response if MySQL save fails")
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
