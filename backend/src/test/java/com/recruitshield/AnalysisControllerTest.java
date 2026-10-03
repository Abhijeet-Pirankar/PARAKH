package com.recruitshield;

import com.recruitshield.controller.AnalysisController;
import com.recruitshield.exception.GlobalExceptionHandler;
import com.recruitshield.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalysisControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnalysisService analysisService = new AnalysisService();
        AnalysisController controller = new AnalysisController(analysisService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/analyze-offer returns 200 and explainable risk report for valid offer")
    void testAnalyzeOfferEndpointSuccess() throws Exception {
        String jsonPayload = """
            {
              "offerText": "Urgent job offer! Direct selection without interview. Pay Rs 2000 registration fee.",
              "companyName": "Tech Global",
              "recruiterEmail": "hr@gmail.com",
              "receivedVia": "WhatsApp"
            }
            """;

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore", greaterThanOrEqualTo(60)))
                .andExpect(jsonPath("$.status", is("HIGHLY_SUSPICIOUS")))
                .andExpect(jsonPath("$.riskLevel", is("HIGH")))
                .andExpect(jsonPath("$.redFlags", not(empty())))
                .andExpect(jsonPath("$.positiveSignals", notNullValue()))
                .andExpect(jsonPath("$.recommendations", not(empty())))
                .andExpect(jsonPath("$.analysisSummary", notNullValue()))
                .andExpect(jsonPath("$.urlVerification", notNullValue()))
                .andExpect(jsonPath("$.recruiterVerification", notNullValue()))
                .andExpect(jsonPath("$.recruiterVerification.publicFreemail", is(true)));
    }

    @Test
    @DisplayName("POST /api/analyze-offer returns 400 Bad Request when offerText is blank")
    void testAnalyzeOfferEndpointEmptyOffer() throws Exception {
        String jsonPayload = """
            {
              "offerText": "   "
            }
            """;

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("cannot be empty or blank")));
    }

    @Test
    @DisplayName("POST /api/analyze-offer returns 400 Bad Request when recruiter email is invalid")
    void testAnalyzeOfferEndpointInvalidEmail() throws Exception {
        String jsonPayload = """
            {
              "offerText": "Valid offer text for junior analyst position.",
              "recruiterEmail": "not-a-valid-email"
            }
            """;

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Invalid recruiter email")));
    }

    @Test
    @DisplayName("POST /api/verify maintains backward compatibility with legacy endpoint")
    void testLegacyVerifyEndpoint() throws Exception {
        String jsonPayload = """
            {
              "text": "Standard engineering internship offer letter with technical interview."
            }
            """;

        mockMvc.perform(post("/api/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", notNullValue()))
                .andExpect(jsonPath("$.status", is("LIKELY_GENUINE")));
    }

    @Test
    @DisplayName("1. Malformed JSON: Syntactically invalid JSON returns HTTP 400 MALFORMED_REQUEST without leaking internals")
    void testMalformedJsonPayload() throws Exception {
        String invalidJson = "{ \"offerText\": \"broken json without closing quote or brace ";

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request body.")))
                .andExpect(jsonPath("$.details", not(empty())))
                // Verify stack traces, internal parser classes, and paths are NOT exposed
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("JsonParseException"))))
                .andExpect(jsonPath("$.message", not(containsString("com.fasterxml.jackson"))));
    }

    @Test
    @DisplayName("2a. Empty request body: Empty HTTP body returns HTTP 400 BAD_REQUEST")
    void testEmptyHttpRequestBody() throws Exception {
        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("cannot be null")));
    }

    @Test
    @DisplayName("2b. Empty JSON body: Payload {} without offerText returns HTTP 400 BAD_REQUEST")
    void testEmptyJsonObjectBody() throws Exception {
        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("cannot be empty or blank")));
    }

    @Test
    @DisplayName("3. Oversized offerText: Payload exceeding 50,000 characters returns HTTP 400 BAD_REQUEST")
    void testOversizedOfferText() throws Exception {
        String oversizedText = "A".repeat(50001);
        String jsonPayload = """
            {
              "offerText": "%s"
            }
            """.formatted(oversizedText);

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("exceeds maximum supported size")));
    }

    @Test
    @DisplayName("4. Unsupported HTTP method: GET /api/analyze-offer returns appropriate 4xx client error")
    void testUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(get("/api/analyze-offer"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("5. General exception sanitization: Unhandled server error returns HTTP 500 without leaking stack trace or internals")
    void testGeneralExceptionSanitization() throws Exception {
        AnalysisService failingService = mock(AnalysisService.class);
        when(failingService.analyzeOffer(any())).thenThrow(new RuntimeException("Simulated unexpected internal database failure or null pointer"));
        AnalysisController failingController = new AnalysisController(failingService);
        MockMvc customMockMvc = MockMvcBuilders.standaloneSetup(failingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String jsonPayload = """
            {
              "offerText": "Valid standard offer text for senior software engineer."
            }
            """;

        customMockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred.")))
                .andExpect(jsonPath("$.details", contains("Please try again later.")))
                // Verify stack traces, Java class names, file paths, and internal messages are not exposed
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("RuntimeException"))))
                .andExpect(jsonPath("$.message", not(containsString("Simulated unexpected internal database failure"))))
                .andExpect(jsonPath("$.details[0]", not(containsString("com.recruitshield"))));
    }

    @Test
    @DisplayName("6. Oversized companyName: Payload exceeding 255 characters returns HTTP 400 BAD_REQUEST")
    void testOversizedCompanyName() throws Exception {
        String oversizedName = "C".repeat(256);
        String jsonPayload = """
            {
              "offerText": "Valid offer text.",
              "companyName": "%s"
            }
            """.formatted(oversizedName);

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Company name cannot exceed 255 characters")));
    }

    @Test
    @DisplayName("7. Oversized companyWebsite: Payload exceeding 2048 characters returns HTTP 400 BAD_REQUEST")
    void testOversizedCompanyWebsite() throws Exception {
        String oversizedUrl = "https://example.com/" + "a".repeat(2040);
        String jsonPayload = """
            {
              "offerText": "Valid offer text.",
              "companyWebsite": "%s"
            }
            """.formatted(oversizedUrl);

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Company website URL cannot exceed 2,048 characters")));
    }

    @Test
    @DisplayName("8. Unsupported media type: Request with text/plain returns HTTP 415 UNSUPPORTED_MEDIA_TYPE")
    void testUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Raw text payload instead of json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error", is("UNSUPPORTED_MEDIA_TYPE")))
                .andExpect(jsonPath("$.message", containsString("Unsupported Content-Type")));
    }
}
