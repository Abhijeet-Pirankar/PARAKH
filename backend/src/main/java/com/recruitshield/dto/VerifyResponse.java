package com.recruitshield.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {

    /**
     * Explainable risk score bounded between 0 and 100.
     */
    private int riskScore;

    /**
     * Backward-compatible alias for riskScore.
     */
    private int score;

    /**
     * Risk assessment classification: LIKELY_GENUINE, NEEDS_VERIFICATION, or HIGHLY_SUSPICIOUS.
     */
    private String status;

    /**
     * Risk tier: LOW, MEDIUM, or HIGH.
     */
    private String riskLevel;

    /**
     * List of detected heuristic warning indicators.
     */
    @Builder.Default
    private List<String> redFlags = new ArrayList<>();

    /**
     * Backward-compatible alias for redFlags.
     */
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    /**
     * List of positive authenticity signals observed in the offer.
     */
    @Builder.Default
    private List<String> positiveSignals = new ArrayList<>();

    /**
     * Actionable candidate safety recommendations.
     */
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    /**
     * Backward-compatible single recommendation summary.
     */
    private String recommendation;

    /**
     * High-level executive synthesis of the risk assessment.
     */
    private String analysisSummary;
}
