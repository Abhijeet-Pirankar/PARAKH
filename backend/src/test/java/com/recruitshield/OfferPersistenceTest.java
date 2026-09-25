package com.recruitshield;

import com.recruitshield.entity.Offer;
import com.recruitshield.entity.RiskReport;
import com.recruitshield.repository.OfferRepository;
import com.recruitshield.repository.RiskReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OfferPersistenceTest {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private RiskReportRepository riskReportRepository;

    @Test
    @DisplayName("Should successfully persist and retrieve Offer entity")
    void testOfferPersistence() {
        Offer offer = Offer.builder()
                .offerText("Senior Full Stack Engineer position at Innovatech Labs.")
                .companyName("Innovatech Labs")
                .companyWebsite("https://innovatech.io")
                .recruiterEmail("careers@innovatech.io")
                .receivedVia("LinkedIn")
                .createdAt(LocalDateTime.now())
                .build();

        Offer saved = offerRepository.save(offer);
        assertNotNull(saved.getId(), "Offer ID should be generated");

        Optional<Offer> fetched = offerRepository.findById(saved.getId());
        assertTrue(fetched.isPresent(), "Offer should be found in DB");
        assertEquals("Innovatech Labs", fetched.get().getCompanyName());
        assertEquals("https://innovatech.io", fetched.get().getCompanyWebsite());
        assertEquals("careers@innovatech.io", fetched.get().getRecruiterEmail());
        assertEquals("LinkedIn", fetched.get().getReceivedVia());
        assertEquals("Senior Full Stack Engineer position at Innovatech Labs.", fetched.get().getOfferText());
        assertNotNull(fetched.get().getCreatedAt());
    }

    @Test
    @DisplayName("Should successfully persist and retrieve RiskReport entity linked to Offer")
    void testRiskReportPersistence() {
        Offer offer = Offer.builder()
                .offerText("Suspicious offer demanding Rs 5000 fee.")
                .companyName("Fake Company")
                .recruiterEmail("scam@gmail.com")
                .receivedVia("WhatsApp")
                .createdAt(LocalDateTime.now())
                .build();

        Offer savedOffer = offerRepository.save(offer);

        RiskReport report = RiskReport.builder()
                .offer(savedOffer)
                .riskScore(85)
                .status("HIGHLY_SUSPICIOUS")
                .riskLevel("HIGH")
                .summary("Offer contains multiple severe indicators.")
                .redFlags(List.of("Demands advance payment", "Public webmail used"))
                .positiveSignals(List.of())
                .recommendations(List.of("Do not transfer funds"))
                .createdAt(LocalDateTime.now())
                .build();

        RiskReport savedReport = riskReportRepository.save(report);
        assertNotNull(savedReport.getId(), "RiskReport ID should be generated");

        Optional<RiskReport> fetched = riskReportRepository.findByOfferId(savedOffer.getId());
        assertTrue(fetched.isPresent(), "RiskReport should be found by offer ID");
        assertEquals(85, fetched.get().getRiskScore());
        assertEquals("HIGHLY_SUSPICIOUS", fetched.get().getStatus());
        assertEquals("HIGH", fetched.get().getRiskLevel());
        assertEquals(2, fetched.get().getRedFlags().size());
        assertTrue(fetched.get().getRedFlags().contains("Demands advance payment"));
        assertTrue(fetched.get().getRecommendations().contains("Do not transfer funds"));
        assertEquals(savedOffer.getId(), fetched.get().getOffer().getId());
    }
}
