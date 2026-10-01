package com.recruitshield;

import com.recruitshield.dto.AiPredictionResponse;
import com.recruitshield.service.AiServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiServiceClientTest {

    @Test
    @DisplayName("AiServiceClient successfully posts offerText and parses JSON response")
    void testSuccessfulPredict() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String responseJson = """
                {
                  "riskProbability": 0.88,
                  "classification": "SUSPICIOUS",
                  "model": "tfidf-logistic-regression"
                }
                """;

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offerText").value("Pay Rs 2000 registration fee"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict("Pay Rs 2000 registration fee");

        server.verify();
        assertTrue(result.isPresent());
        assertEquals(0.88, result.get().getRiskProbability());
        assertEquals("SUSPICIOUS", result.get().getClassification());
        assertEquals("tfidf-logistic-regression", result.get().getModel());
    }

    @Test
    @DisplayName("AiServiceClient returns empty Optional on HTTP 500 error without throwing")
    void testServerErrorFallback() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict("Some offer text");

        server.verify();
        assertTrue(result.isEmpty(), "Should return empty Optional on server error");
    }

    @Test
    @DisplayName("AiServiceClient returns empty Optional when service is disabled")
    void testServiceDisabled() {
        RestClient restClient = RestClient.create();
        AiServiceClient client = new AiServiceClient(restClient, "http://localhost:8000", false);

        Optional<AiPredictionResponse> result = client.predict("Some offer text");

        assertTrue(result.isEmpty(), "Disabled client should immediately return empty Optional");
    }

    @Test
    @DisplayName("AiServiceClient returns empty Optional on blank text")
    void testBlankText() {
        RestClient restClient = RestClient.create();
        AiServiceClient client = new AiServiceClient(restClient, "http://localhost:8000", true);

        assertTrue(client.predict("").isEmpty());
        assertTrue(client.predict(null).isEmpty());
        assertTrue(client.predict("   ").isEmpty());
    }
}
