package com.recruitshield;

import com.recruitshield.controller.AnalysisController;
import com.recruitshield.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalysisControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnalysisService analysisService = new AnalysisService();
        AnalysisController controller = new AnalysisController(analysisService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
                .andExpect(jsonPath("$.analysisSummary", notNullValue()));
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
}
