package com.recruitshield;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.dto.AiPredictionResponse;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.repository.OfferRepository;
import com.recruitshield.repository.RiskReportRepository;
import com.recruitshield.service.AiServiceClient;
import com.recruitshield.service.AnalysisService;
import com.recruitshield.service.RecruiterVerificationService;
import com.recruitshield.service.UrlVerificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 11 - Priority 8: Frontend API Contract & DTO Field Alignment Integration Tests.
 *
 * Verifies end-to-end integration and API contract adherence between the Spring Boot backend
 * and the React frontend (services/api.js, CheckOffer.jsx, RiskReport.jsx):
 * 1. Full contract adherence across all three risk tiers (HIGHLY_SUSPICIOUS, NEEDS_VERIFICATION, LIKELY_GENUINE).
 * 2. Guaranteed field equivalence: 'score' and 'riskScore' both present and numerically identical.
 * 3. Backward-compatible alias matching: 'redFlags' == 'reasons', 'recommendations[0]' == 'recommendation'.
 * 4. Structured forensic metadata verification: nested 'urlVerification' and 'recruiterVerification'.
 * 5. Input payload tolerance: aliases ('text' vs 'offerText') and minimal payloads.
 * 6. Error handling contract: structured JSON { error, message, details } consumed by frontend error handlers.
 * 7. Browser CORS contract: preflight and actual POST requests honoring frontend origin (http://localhost:5173).
 * 8. Hybrid AI metadata contract: transparent combination when AI is available vs fallback when unavailable.
 */
@SpringBootTest
class FrontendContractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private RiskReportRepository riskReportRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        try {
            riskReportRepository.deleteAll();
            offerRepository.deleteAll();
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("1. Contract: Highly Suspicious offer returns complete DTO structure expected by api.js and RiskReport.jsx")
    void testFrontendContractHighlySuspicious() throws Exception {
        String payload = """
            {
              "offerText": "URGENT HIRING: Direct selection without any interview. Pay Rs 2,500 security deposit for laptop and training. Work from home.",
              "companyName": "Apex Cyber Tech",
              "companyWebsite": "http://apex-jobs.xyz",
              "recruiterEmail": "hr.apexcyber@gmail.com",
              "receivedVia": "WhatsApp"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                // Numeric score and riskScore must both exist and be >= 70
                .andExpect(jsonPath("$.riskScore", greaterThanOrEqualTo(70)))
                .andExpect(jsonPath("$.score", greaterThanOrEqualTo(70)))
                // Status and RiskLevel must match frontend color tiering
                .andExpect(jsonPath("$.status", is("HIGHLY_SUSPICIOUS")))
                .andExpect(jsonPath("$.riskLevel", is("HIGH")))
                // Red flags and reasons must both exist as non-empty arrays
                .andExpect(jsonPath("$.redFlags", not(empty())))
                .andExpect(jsonPath("$.reasons", not(empty())))
                // Positive signals and recommendations
                .andExpect(jsonPath("$.positiveSignals", notNullValue()))
                .andExpect(jsonPath("$.recommendations", not(empty())))
                .andExpect(jsonPath("$.recommendation", notNullValue()))
                .andExpect(jsonPath("$.analysisSummary", notNullValue()))
                // Rule-based score and AI metadata
                .andExpect(jsonPath("$.ruleBasedScore", greaterThanOrEqualTo(70)))
                .andExpect(jsonPath("$.aiAnalysisAvailable", is(false)))
                // Nested URL forensic metadata
                .andExpect(jsonPath("$.urlVerification.provided", is(true)))
                .andExpect(jsonPath("$.urlVerification.valid", is(true)))
                .andExpect(jsonPath("$.urlVerification.https", is(false)))
                .andExpect(jsonPath("$.urlVerification.riskyTld", is(true)))
                .andExpect(jsonPath("$.urlVerification.riskIndicators", not(empty())))
                // Nested Recruiter forensic metadata
                .andExpect(jsonPath("$.recruiterVerification.emailProvided", is(true)))
                .andExpect(jsonPath("$.recruiterVerification.emailValid", is(true)))
                .andExpect(jsonPath("$.recruiterVerification.publicFreemail", is(true)))
                .andExpect(jsonPath("$.recruiterVerification.informalChannel", is(true)))
                .andExpect(jsonPath("$.recruiterVerification.riskIndicators", not(empty())))
                .andReturn();

        // Verify score strictly equals riskScore, and recommendation equals recommendations[0]
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(root.get("riskScore").asInt(), root.get("score").asInt(),
                "Frontend contract requires 'score' and 'riskScore' to be numerically identical");
        assertEquals(root.get("recommendations").get(0).asText(), root.get("recommendation").asText(),
                "Frontend contract requires single 'recommendation' to match first element of 'recommendations'");
        assertEquals(root.get("redFlags").size(), root.get("reasons").size(),
                "Frontend contract requires 'redFlags' and 'reasons' to have identical size");
    }

    @Test
    @DisplayName("2. Contract: Needs Verification offer returns complete MEDIUM risk tier structure")
    void testFrontendContractNeedsVerification() throws Exception {
        String payload = """
            {
              "offerText": "Direct selection without interview for Software Analyst position. Please confirm your acceptance.",
              "companyName": "Tech Dynamics",
              "companyWebsite": "https://techdynamics.com",
              "recruiterEmail": "hr@techdynamics.com",
              "receivedVia": "Email"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore", allOf(greaterThanOrEqualTo(30), lessThan(60))))
                .andExpect(jsonPath("$.score", allOf(greaterThanOrEqualTo(30), lessThan(60))))
                .andExpect(jsonPath("$.status", is("NEEDS_VERIFICATION")))
                .andExpect(jsonPath("$.riskLevel", is("MEDIUM")))
                .andExpect(jsonPath("$.redFlags", not(empty())))
                .andExpect(jsonPath("$.positiveSignals", notNullValue()))
                .andExpect(jsonPath("$.recommendations", not(empty())))
                .andExpect(jsonPath("$.analysisSummary", notNullValue()))
                .andExpect(jsonPath("$.urlVerification.provided", is(true)))
                .andExpect(jsonPath("$.urlVerification.https", is(true)))
                .andExpect(jsonPath("$.recruiterVerification.domainMatch", is(true)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(root.get("riskScore").asInt(), root.get("score").asInt());
    }

    @Test
    @DisplayName("3. Contract: Likely Genuine offer returns LOW risk tier with positive signals")
    void testFrontendContractLikelyGenuine() throws Exception {
        String payload = """
            {
              "offerText": "We are delighted to extend an offer for Associate Software Engineer at Infosys. Following your technical interview round and coding assessment, your CTC will be 6.5 LPA.",
              "companyName": "Infosys",
              "companyWebsite": "https://infosys.com",
              "recruiterEmail": "talent@infosys.com",
              "receivedVia": "Email"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore", lessThan(40)))
                .andExpect(jsonPath("$.score", lessThan(40)))
                .andExpect(jsonPath("$.status", is("LIKELY_GENUINE")))
                .andExpect(jsonPath("$.riskLevel", is("LOW")))
                .andExpect(jsonPath("$.positiveSignals", not(empty())))
                .andExpect(jsonPath("$.recommendations", not(empty())))
                .andExpect(jsonPath("$.recommendation", notNullValue()))
                .andExpect(jsonPath("$.analysisSummary", notNullValue()))
                .andExpect(jsonPath("$.recruiterVerification.domainMatch", is(true)))
                .andExpect(jsonPath("$.recruiterVerification.publicFreemail", is(false)))
                .andExpect(jsonPath("$.urlVerification.https", is(true)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(root.get("riskScore").asInt(), root.get("score").asInt());
        assertTrue(root.get("positiveSignals").size() >= 3,
                "Legitimate offer must contain multiple positive authenticity signals");
    }

    @Test
    @DisplayName("4. Contract: Legacy /api/verify endpoint maintains full JSON contract with 'score' and 'reasons'")
    void testFrontendContractLegacyVerifyEndpoint() throws Exception {
        String payload = """
            {
              "text": "Formal job offer for Frontend Developer with two technical interview rounds."
            }
            """;

        mockMvc.perform(post("/api/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", notNullValue()))
                .andExpect(jsonPath("$.riskScore", notNullValue()))
                .andExpect(jsonPath("$.status", is("LIKELY_GENUINE")))
                .andExpect(jsonPath("$.riskLevel", is("LOW")))
                .andExpect(jsonPath("$.reasons", not(empty())))
                .andExpect(jsonPath("$.redFlags", not(empty())))
                .andExpect(jsonPath("$.recommendations", not(empty())));
    }

    @Test
    @DisplayName("5. Contract: Input alias tolerance - 'text' fallback when 'offerText' is omitted")
    void testFrontendContractInputAliasFallback() throws Exception {
        String payload = """
            {
              "text": "Direct joining without interview. Pay Rs 1,000 registration fee immediately.",
              "receivedVia": "Telegram"
            }
            """;

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("HIGHLY_SUSPICIOUS")))
                .andExpect(jsonPath("$.riskScore", greaterThanOrEqualTo(70)));
    }

    @Test
    @DisplayName("6. Contract: Minimal payload with only offerText succeeds without null pointer errors")
    void testFrontendContractMinimalPayload() throws Exception {
        String payload = """
            {
              "offerText": "Standard junior analyst offer letter."
            }
            """;

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore", notNullValue()))
                .andExpect(jsonPath("$.score", notNullValue()))
                .andExpect(jsonPath("$.urlVerification.provided", is(false)))
                .andExpect(jsonPath("$.recruiterVerification.emailProvided", is(false)));
    }

    @Test
    @DisplayName("7. Contract: 400 Bad Request returns structured { error, message, details } consumed by api.js error handler")
    void testFrontendContractValidationErrorFormat() throws Exception {
        String blankPayload = """
            {
              "offerText": "   "
            }
            """;

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("cannot be empty or blank")))
                .andExpect(jsonPath("$.details", not(empty())))
                .andExpect(jsonPath("$.details[0]", containsString("cannot be empty or blank")));
    }

    @Test
    @DisplayName("8. Contract: Malformed JSON returns structured { error: MALFORMED_REQUEST, message, details }")
    void testFrontendContractMalformedJsonFormat() throws Exception {
        String malformedJson = "{ \"offerText\": \"truncated syntax without quote";

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request body.")))
                .andExpect(jsonPath("$.details", not(empty())));
    }

    @Test
    @DisplayName("9. Contract: CORS preflight and actual POST requests include expected frontend headers")
    void testFrontendContractCorsHeaders() throws Exception {
        // Preflight OPTIONS
        mockMvc.perform(options("/api/analyze-offer")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));

        // Actual POST
        String payload = """
            {
              "offerText": "Software engineer offer with standard technical interview."
            }
            """;

        mockMvc.perform(post("/api/analyze-offer")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("10. Contract: Hybrid AI formula produces expected combined score when AI client is active")
    void testFrontendContractHybridAiFormula() {
        AiServiceClient mockAiClient = mock(AiServiceClient.class);
        when(mockAiClient.predict(anyString())).thenReturn(Optional.of(
                AiPredictionResponse.builder()
                        .riskProbability(0.90)
                        .classification("SUSPICIOUS")
                        .model("tfidf-logistic-regression")
                        .build()
        ));

        AnalysisService analysisService = new AnalysisService(
                null, null,
                new UrlVerificationService(),
                new RecruiterVerificationService(),
                mockAiClient
        );

        VerifyRequest request = VerifyRequest.builder()
                .offerText("We would like to offer you the position. Interview discussions routed via WhatsApp.")
                .receivedVia("WhatsApp")
                .build();

        VerifyResponse response = analysisService.analyzeOffer(request);

        assertNotNull(response);
        assertTrue(response.isAiAnalysisAvailable());
        assertEquals(0.90, response.getAiRiskProbability());
        assertEquals("SUSPICIOUS", response.getAiClassification());
        // ruleBasedScore has WhatsApp (+20) = 20.
        // aiScore = 0.90 * 100 = 90.
        // combined = round(0.70 * 20 + 0.30 * 90) = round(14 + 27) = 41.
        assertEquals(20, response.getRuleBasedScore());
        assertEquals(41, response.getRiskScore());
        assertEquals(41, response.getScore());
        assertEquals("NEEDS_VERIFICATION", response.getStatus());
        assertEquals("MEDIUM", response.getRiskLevel());
    }
}
