package com.recruitshield;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.recruitshield.dto.AiPredictionResponse;
import com.recruitshield.service.AiServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiServiceClientTest {

    @Test
    @DisplayName("1. Successful predict: AiServiceClient posts offerText and parses JSON response")
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
    @DisplayName("2. HTTP 500 error: AiServiceClient returns empty Optional without throwing")
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
    @DisplayName("3. HTTP 422 Unprocessable Entity: returns empty Optional gracefully on validation error")
    void testValidationFailureFallback422() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String errorJson = """
                {
                  "error": "VALIDATION_ERROR",
                  "message": "Invalid input provided.",
                  "details": ["offerText: String should have at most 50000 characters"]
                }
                """;

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorJson));

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict("A".repeat(50001));

        server.verify();
        assertTrue(result.isEmpty(), "Should return empty Optional on HTTP 422 error");
    }

    @Test
    @DisplayName("4. HTTP 503 Service Unavailable: returns empty Optional when Python model is uninitialized")
    void testServiceUnavailableFallback503() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String serviceUnavailableJson = "{\"detail\": \"AI model is currently not available.\"}";

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(serviceUnavailableJson));

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict("Standard offer text");

        server.verify();
        assertTrue(result.isEmpty(), "Should return empty Optional on HTTP 503");
    }

    @Test
    @DisplayName("5. Malformed non-JSON response: HTML error page handled safely without crash")
    void testMalformedHtmlResponseFallback() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("<html><body>502 Bad Gateway - Nginx</body></html>", MediaType.TEXT_HTML));

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict("Some offer text");

        server.verify();
        assertTrue(result.isEmpty(), "Should return empty Optional when Python/proxy returns HTML");
    }

    @Test
    @DisplayName("6. Missing required field in JSON response: returns empty Optional gracefully")
    void testMissingFieldInResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Valid JSON but missing riskProbability
        String incompleteJson = "{\"classification\": \"SUSPICIOUS\", \"model\": \"tfidf\"}";

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(incompleteJson, MediaType.APPLICATION_JSON));

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict("Some offer text");

        server.verify();
        assertTrue(result.isEmpty(), "Should return empty Optional when riskProbability is null");
    }

    @Test
    @DisplayName("7. Read/Socket Timeout: handles timeout exception and falls back to empty Optional")
    void testSocketTimeoutHandling() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out after 3000ms");
                });

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict("Some offer text");

        server.verify();
        assertTrue(result.isEmpty(), "Should return empty Optional on socket timeout without throwing");
    }

    @Test
    @DisplayName("8. Connection failure: unreachable service port returns empty Optional without throwing")
    void testConnectionFailureGraceful() {
        // Points to an unallocated local port with a short 300ms timeout
        AiServiceClient client = new AiServiceClient("http://127.0.0.1:59999", true, 300);

        Optional<AiPredictionResponse> result = client.predict("Sample offer text");

        assertTrue(result.isEmpty(), "Unreachable service must return empty Optional without crashing");
    }

    @Test
    @DisplayName("9. Service disabled: immediately returns empty Optional without HTTP call")
    void testServiceDisabled() {
        RestClient restClient = RestClient.create();
        AiServiceClient client = new AiServiceClient(restClient, "http://localhost:8000", false);

        Optional<AiPredictionResponse> result = client.predict("Some offer text");

        assertTrue(result.isEmpty(), "Disabled client should immediately return empty Optional");
    }

    @Test
    @DisplayName("10. Blank text: returns empty Optional without HTTP call")
    void testBlankText() {
        RestClient restClient = RestClient.create();
        AiServiceClient client = new AiServiceClient(restClient, "http://localhost:8000", true);

        assertTrue(client.predict("").isEmpty());
        assertTrue(client.predict(null).isEmpty());
        assertTrue(client.predict("   ").isEmpty());
    }

    @Test
    @DisplayName("11. Privacy & Logging: Sensitive offer text is NOT logged; only text length is recorded")
    void testPrivacyNoSensitiveOfferTextInLogs() {
        Logger clientLogger = (Logger) LoggerFactory.getLogger(AiServiceClient.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        clientLogger.addAppender(listAppender);

        String sensitiveToken = "CONFIDENTIAL_APPLICANT_PAN_ABCDE1234F_SALARY_9999999";
        String offer = "Offer for " + sensitiveToken + " with joining date next month.";

        // Invoke with unreachable endpoint to trigger info and warn logging
        AiServiceClient client = new AiServiceClient("http://127.0.0.1:59998", true, 200);
        client.predict(offer);

        try {
            boolean foundLengthLog = false;
            for (ILoggingEvent event : listAppender.list) {
                String message = event.getFormattedMessage();
                // Critical privacy check: Sensitive candidate content must NEVER appear in logs
                assertFalse(message.contains(sensitiveToken),
                        "Log message must not contain sensitive offer text: " + message);
                assertFalse(message.contains("ABCDE1234F"),
                        "Log message must not leak sensitive identifiers: " + message);

                if (message.contains("text length: " + offer.length() + " characters")) {
                    foundLengthLog = true;
                }
            }
            assertTrue(foundLengthLog, "Log should record text length for observability without leaking content");
        } finally {
            clientLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("12. Input handling: Untrusted offer text is transmitted strictly as request data")
    void testUntrustedInputTransmittedAsData() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String untrustedText = "<script>alert('xss');</script>; DROP TABLE offers; https://evil.xyz/payload";

        server.expect(requestTo("http://localhost:8000/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offerText").value(untrustedText))
                .andRespond(withSuccess("""
                        {
                          "riskProbability": 0.75,
                          "classification": "SUSPICIOUS",
                          "model": "tfidf-logistic-regression"
                        }
                        """, MediaType.APPLICATION_JSON));

        AiServiceClient client = new AiServiceClient(builder.build(), "http://localhost:8000", true);

        Optional<AiPredictionResponse> result = client.predict(untrustedText);

        server.verify();
        assertTrue(result.isPresent());
        assertEquals("SUSPICIOUS", result.get().getClassification());
    }
}
