package com.recruitshield.service;

import com.recruitshield.dto.AiPredictionRequest;
import com.recruitshield.dto.AiPredictionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * HTTP Client connecting the Java Spring Boot backend to the Python FastAPI AI service.
 * Supports configurable timeouts, automatic circuit-breaking / graceful fallback if offline.
 */
@Slf4j
@Service
public class AiServiceClient {

    private final RestClient restClient;
    private final String aiServiceUrl;
    private final boolean aiServiceEnabled;

    @Autowired
    public AiServiceClient(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai.service.enabled:true}") boolean aiServiceEnabled,
            @Value("${ai.service.timeout-ms:3000}") int timeoutMs) {
        this.aiServiceUrl = aiServiceUrl;
        this.aiServiceEnabled = aiServiceEnabled;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AiServiceClient(RestClient restClient, String aiServiceUrl, boolean aiServiceEnabled) {
        this.restClient = restClient;
        this.aiServiceUrl = aiServiceUrl;
        this.aiServiceEnabled = aiServiceEnabled;
    }

    public AiServiceClient() {
        this("http://localhost:8000", true, 3000);
    }

    /**
     * Sends the offer text to the Python AI service for machine-learning inference.
     *
     * @param offerText Raw offer text (untrusted candidate input)
     * @return Optional containing AiPredictionResponse if successful, empty Optional if unavailable
     */
    public Optional<AiPredictionResponse> predict(String offerText) {
        if (!aiServiceEnabled) {
            log.debug("Python AI service integration is disabled by configuration.");
            return Optional.empty();
        }

        if (offerText == null || offerText.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Security: Log only the text length to protect candidate privacy and avoid sensitive data leakage
            log.info("Sending prediction request to AI service at {} (text length: {} characters)",
                    aiServiceUrl, offerText.length());

            AiPredictionResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AiPredictionRequest(offerText))
                    .retrieve()
                    .body(AiPredictionResponse.class);

            if (response != null && response.getRiskProbability() != null) {
                log.info("Received AI prediction: probability={}, classification={}, model={}",
                        response.getRiskProbability(), response.getClassification(), response.getModel());
                return Optional.of(response);
            } else {
                log.warn("AI service returned empty or invalid response body.");
                return Optional.empty();
            }
        } catch (Exception ex) {
            // Graceful fallback: Do not rethrow or leak internal network/Python errors to candidate
            log.warn("Python AI service unavailable or returned error: {}. Falling back to rule-based analysis.",
                    ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean isAiServiceEnabled() {
        return aiServiceEnabled;
    }

    public String getAiServiceUrl() {
        return aiServiceUrl;
    }
}
