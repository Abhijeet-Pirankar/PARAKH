package com.recruitshield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured forensic verification result for recruiter identity and communication channels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterVerificationResult {

    /**
     * Whether a recruiter contact email was provided.
     */
    private boolean emailProvided;

    /**
     * Whether the recruiter email conforms to standard syntactic formatting.
     */
    private boolean emailValid;

    /**
     * The raw recruiter email address submitted.
     */
    private String email;

    /**
     * Extracted and normalized domain from recruiter email.
     */
    private String emailDomain;

    /**
     * Extracted and normalized domain from company website.
     */
    private String companyDomain;

    /**
     * Whether the recruiter email domain matches the stated company website domain.
     * Evaluated only when both domains are present. Null if comparison was not possible.
     */
    private Boolean domainMatch;

    /**
     * Whether the recruiter email originates from a public free email provider (e.g. Gmail, Yahoo).
     */
    private boolean publicFreemail;

    /**
     * Whether the recruiter email originates from a known disposable or anonymous mailbox provider.
     */
    private boolean disposableEmail;

    /**
     * Whether the email address displays randomized, automated, or spoofing patterns.
     */
    private boolean suspiciousPattern;

    /**
     * Stated or detected communication medium (e.g., WhatsApp, Telegram, Email, LinkedIn).
     */
    private String communicationChannel;

    /**
     * Whether communication relies on unmonitored or informal chat conduits.
     */
    private boolean informalChannel;

    /**
     * Specific explainable risk signals detected for this recruiter.
     */
    @Builder.Default
    private List<String> riskIndicators = new ArrayList<>();

    /**
     * Specific positive authenticity signals observed for this recruiter.
     */
    @Builder.Default
    private List<String> positiveIndicators = new ArrayList<>();
}
