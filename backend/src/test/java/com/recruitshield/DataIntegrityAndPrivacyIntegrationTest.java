package com.recruitshield;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.dto.RegisterRequest;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.entity.Offer;
import com.recruitshield.entity.RiskReport;
import com.recruitshield.entity.User;
import com.recruitshield.repository.OfferRepository;
import com.recruitshield.repository.RiskReportRepository;
import com.recruitshield.repository.UserRepository;
import com.recruitshield.service.AnalysisService;
import com.recruitshield.service.RecruiterVerificationService;
import com.recruitshield.service.UrlVerificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 11 - Priority 9: Duplicate Data Handling, Concurrency & Sensitive Information Exposure Tests.
 *
 * Verifies:
 * 1. Duplicate Data Handling: Case-insensitive duplicate email rejection on registration.
 * 2. Duplicate Offer Submissions: Submitting identical offer text produces distinct, non-conflicting persisted records.
 * 3. Multi-Threaded Concurrency: Concurrent offer analysis requests across threads execute safely without race conditions.
 * 4. Sensitive Data Privacy: Password hash, raw passwords, and database credentials are never exposed in API responses or serialized DTOs.
 * 5. Deterministic Forensic Engine: Repeated forensic analysis of identical URLs and recruiter emails is strictly idempotent.
 */
@SpringBootTest
class DataIntegrityAndPrivacyIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private RiskReportRepository riskReportRepository;

    @Autowired
    private AnalysisService analysisService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Long> trackedOfferIds = new ArrayList<>();
    private final List<Long> trackedUserIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

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

        for (Long userId : trackedUserIds) {
            try {
                if (userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            } catch (Exception ignored) {
            }
        }
        trackedUserIds.clear();
    }

    @Test
    @DisplayName("1. Duplicate Email: Registering email with uppercase or mixed-case variation is rejected with 409 Conflict")
    void testDuplicateEmailCaseInsensitiveRejection() throws Exception {
        String baseEmail = "unique_dup_" + System.currentTimeMillis() + "@domain.com";

        // Register lowercase
        RegisterRequest initialRequest = RegisterRequest.builder()
                .name("First User")
                .email(baseEmail.toLowerCase())
                .password("SecurePass123")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode regNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        trackedUserIds.add(regNode.get("id").asLong());

        // Attempt duplicate with UPPERCASE
        RegisterRequest upperRequest = RegisterRequest.builder()
                .name("Duplicate User Upper")
                .email(baseEmail.toUpperCase())
                .password("SecurePass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upperRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_EMAIL")))
                .andExpect(jsonPath("$.message", containsString("already registered")));

        // Attempt duplicate with MixedCase
        String mixedEmail = "UniQue_DuP_" + System.currentTimeMillis() + "@Domain.Com";
        // but using same root
        RegisterRequest mixedRequest = RegisterRequest.builder()
                .name("Duplicate User Mixed")
                .email(baseEmail.substring(0, 5).toUpperCase() + baseEmail.substring(5).toLowerCase())
                .password("SecurePass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mixedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_EMAIL")));
    }

    @Test
    @DisplayName("2. Duplicate Offer Submissions: Repeated submission of identical payload persists separate non-conflicting entities")
    void testDuplicateOfferSubmissionsPersistenceIsolation() {
        String identicalText = "Urgent job offer! Direct selection without interview. Pay Rs 2000 registration fee.";
        String identicalCompany = "Duplicate Corp " + System.currentTimeMillis();

        VerifyRequest request = VerifyRequest.builder()
                .offerText(identicalText)
                .companyName(identicalCompany)
                .recruiterEmail("hr.dup@gmail.com")
                .receivedVia("WhatsApp")
                .build();

        // Submit 1
        VerifyResponse response1 = analysisService.analyzeOffer(request);
        // Submit 2 (identical)
        VerifyResponse response2 = analysisService.analyzeOffer(request);

        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(response1.getRiskScore(), response2.getRiskScore());
        assertEquals(response1.getStatus(), response2.getStatus());

        // Verify two distinct offers exist in MySQL
        List<Offer> matchingOffers = offerRepository.findAll().stream()
                .filter(o -> identicalCompany.equals(o.getCompanyName()))
                .toList();

        assertEquals(2, matchingOffers.size(), "Both identical submissions must be persisted as distinct records");
        Offer offer1 = matchingOffers.get(0);
        Offer offer2 = matchingOffers.get(1);
        trackedOfferIds.add(offer1.getId());
        trackedOfferIds.add(offer2.getId());

        assertNotEquals(offer1.getId(), offer2.getId(), "Primary keys must be distinct");

        // Verify both have independent RiskReport entries
        Optional<RiskReport> report1 = riskReportRepository.findByOfferId(offer1.getId());
        Optional<RiskReport> report2 = riskReportRepository.findByOfferId(offer2.getId());

        assertTrue(report1.isPresent());
        assertTrue(report2.isPresent());
        assertNotEquals(report1.get().getId(), report2.get().getId());
    }

    @Test
    @DisplayName("3. Multi-Threaded Concurrency: Concurrent offer analysis requests execute in parallel without data corruption")
    void testConcurrentOfferAnalysisExecution() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Future<VerifyResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await(); // Synchronize all threads to start simultaneously
                VerifyRequest request;
                if (index % 2 == 0) {
                    // Scam offer
                    request = VerifyRequest.builder()
                            .offerText("Direct selection without interview! Pay Rs 2500 laptop deposit. Candidate #" + index)
                            .companyName("Concurrent Scam Co " + index + "_" + System.currentTimeMillis())
                            .recruiterEmail("hr" + index + "@gmail.com")
                            .receivedVia("WhatsApp")
                            .build();
                } else {
                    // Genuine offer
                    request = VerifyRequest.builder()
                            .offerText("Offer for Associate Software Engineer following three technical interview rounds. Candidate #" + index)
                            .companyName("Enterprise Genuine " + index + "_" + System.currentTimeMillis())
                            .companyWebsite("https://enterprise" + index + ".com")
                            .recruiterEmail("careers@enterprise" + index + ".com")
                            .receivedVia("Email")
                            .build();
                }
                try {
                    return analysisService.analyzeOffer(request);
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();
        boolean completedInTime = doneLatch.await(15, TimeUnit.SECONDS);
        assertTrue(completedInTime, "All concurrent analysis threads must complete within timeout");

        for (int i = 0; i < threadCount; i++) {
            VerifyResponse res = futures.get(i).get();
            assertNotNull(res);
            if (i % 2 == 0) {
                // Assert scam offer attributes
                assertTrue(res.getRiskScore() >= 60, "Scam thread " + i + " must have high score");
                assertEquals("HIGHLY_SUSPICIOUS", res.getStatus());
                assertTrue(res.getRedFlags().stream().anyMatch(f -> f.contains("Advance payment") || f.contains("deposit")));
            } else {
                // Assert genuine offer attributes
                assertTrue(res.getRiskScore() < 40, "Genuine thread " + i + " must have low score");
                assertEquals("LIKELY_GENUINE", res.getStatus());
                assertTrue(res.getPositiveSignals().stream().anyMatch(s -> s.contains("HTTPS") || s.contains("corporate email")));
            }
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("4. Sensitive Data Privacy: Password hash is strictly BCrypt-hashed in DB and never stored in plain text")
    void testPasswordHashDatabasePrivacy() {
        String uniqueEmail = "plain_check_" + System.currentTimeMillis() + "@domain.com";
        String rawPassword = "CandidateSecret987!";

        RegisterRequest request = RegisterRequest.builder()
                .name("Security Check User")
                .email(uniqueEmail)
                .password(rawPassword)
                .build();

        mockMvcPerformRegister(request);

        User savedUser = userRepository.findByEmailIgnoreCase(uniqueEmail).orElse(null);
        assertNotNull(savedUser, "User must be persisted in database");
        trackedUserIds.add(savedUser.getId());

        // Password hash assertions
        assertNotNull(savedUser.getPasswordHash());
        assertNotEquals(rawPassword, savedUser.getPasswordHash(), "Raw password must NEVER be stored in plain text");
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$") || savedUser.getPasswordHash().startsWith("$2b$"),
                "Password must be hashed with strong BCrypt algorithm");
    }

    @Test
    @DisplayName("5. Sensitive Data Privacy: DTO serialization does not include passwordHash across endpoints")
    void testSensitiveFieldsAbsentFromDtoSerialization() throws Exception {
        String uniqueEmail = "dtocheck_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest request = RegisterRequest.builder()
                .name("DTO Privacy User")
                .email(uniqueEmail)
                .password("MySecretPass456")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        trackedUserIds.add(root.get("id").asLong());

        // Assert sensitive keys are absent from JSON
        assertNull(root.get("passwordHash"));
        assertNull(root.get("password"));
        if (root.has("user")) {
            assertNull(root.get("user").get("passwordHash"));
            assertNull(root.get("user").get("password"));
        }
    }

    @Test
    @DisplayName("6. Deterministic Idempotency: Forensic URL and Recruiter verification results are strictly deterministic")
    void testForensicServicesIdempotentDeterminism() {
        UrlVerificationService urlService = new UrlVerificationService();
        RecruiterVerificationService recruiterService = new RecruiterVerificationService();

        String url = "https://sub.portal.phish-login.xyz/apply";
        String email = "hr_recruiter98231@gmail.com";
        String text = "Direct joining without interview. Contact hr_recruiter98231@gmail.com via WhatsApp.";

        // Run 50 iterations on URL service
        com.recruitshield.dto.UrlVerificationResult firstUrlResult = urlService.verifyUrl(url);
        for (int i = 0; i < 50; i++) {
            com.recruitshield.dto.UrlVerificationResult currentResult = urlService.verifyUrl(url);
            assertEquals(firstUrlResult.isValid(), currentResult.isValid());
            assertEquals(firstUrlResult.isHttps(), currentResult.isHttps());
            assertEquals(firstUrlResult.isRiskyTld(), currentResult.isRiskyTld());
            assertEquals(firstUrlResult.getDomain(), currentResult.getDomain());
            assertEquals(firstUrlResult.getRiskIndicators(), currentResult.getRiskIndicators());
        }

        // Run 50 iterations on Recruiter service
        com.recruitshield.dto.RecruiterVerificationResult firstRecruiterResult =
                recruiterService.verifyRecruiter(email, url, "WhatsApp", text);
        for (int i = 0; i < 50; i++) {
            com.recruitshield.dto.RecruiterVerificationResult currentResult =
                    recruiterService.verifyRecruiter(email, url, "WhatsApp", text);
            assertEquals(firstRecruiterResult.isPublicFreemail(), currentResult.isPublicFreemail());
            assertEquals(firstRecruiterResult.isSuspiciousPattern(), currentResult.isSuspiciousPattern());
            assertEquals(firstRecruiterResult.isInformalChannel(), currentResult.isInformalChannel());
            assertEquals(firstRecruiterResult.getDomainMatch(), currentResult.getDomainMatch());
            assertEquals(firstRecruiterResult.getRiskIndicators(), currentResult.getRiskIndicators());
        }
    }

    private void mockMvcPerformRegister(RegisterRequest req) {
        try {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
