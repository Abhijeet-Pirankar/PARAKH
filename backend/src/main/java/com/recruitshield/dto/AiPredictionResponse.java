package com.recruitshield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPredictionResponse {

    /**
     * Estimated probability that the offer text is fraudulent/suspicious (0.0 to 1.0).
     */
    private Double riskProbability;

    /**
     * Categorical classification: 'SUSPICIOUS' or 'LEGITIMATE'.
     */
    private String classification;

    /**
     * Name of the ML model that performed inference (e.g. 'tfidf-logistic-regression').
     */
    private String model;
}
