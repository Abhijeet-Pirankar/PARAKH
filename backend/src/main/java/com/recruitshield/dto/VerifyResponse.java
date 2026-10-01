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
     * Pure rule-based heuristic risk score before AI contribution.
     */
    private int ruleBasedScore;

    /**
     * ML risk probability predicted by Python AI service (0.0 to 1.0). Null if unavailable.
     */
    private Double aiRiskProbability;

    /**
     * ML classification: 'SUSPICIOUS' or 'LEGITIMATE'. Null if unavailable.
     */
    private String aiClassification;

    /**
     * Indicates whether Python AI service was available and contributed to the assessment.
     */
    private boolean aiAnalysisAvailable;

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

    /**
     * Structured forensic analysis of company URL / website.
     */
    private UrlVerificationResult urlVerification;

    /**
     * Structured forensic analysis of recruiter email and communication channel.
     */
    private RecruiterVerificationResult recruiterVerification;
}
