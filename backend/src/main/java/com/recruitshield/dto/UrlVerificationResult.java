package com.recruitshield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured forensic verification result for company website / URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlVerificationResult {

    /**
     * Whether a URL or company website was submitted for analysis.
     */
    private boolean provided;

    /**
     * Whether the submitted URL conforms to valid URL/URI syntax.
     */
    private boolean valid;

    /**
     * Whether the URL utilizes encrypted HTTPS transport.
     */
    private boolean https;

    /**
     * The raw, untrusted URL string as submitted.
     */
    private String originalUrl;

    /**
     * Syntactically normalized hostname or domain.
     */
    private String domain;

    /**
     * Whether an IP address literal is used instead of a standard domain.
     */
    private boolean ipAddress;

    /**
     * Whether the URL addresses a localhost, loopback, or private internal network address.
     */
    private boolean privateOrLocal;

    /**
     * Whether the URL utilizes a known link-shortener or redirection domain.
     */
    private boolean urlShortener;

    /**
     * Whether the domain uses a generic TLD frequently observed in disposable or high-risk campaigns.
     */
    private boolean riskyTld;

    /**
     * Whether the hostname contains an abnormally high number of subdomain delegations.
     */
    private boolean excessiveSubdomains;

    /**
     * Whether domain keywords indicate phishing or deceptive branding.
     */
    private boolean suspiciousKeywords;

    /**
     * Specific explainable risk signals detected for this URL.
     */
    @Builder.Default
    private List<String> riskIndicators = new ArrayList<>();

    /**
     * Specific positive authenticity signals observed for this URL.
     */
    @Builder.Default
    private List<String> positiveIndicators = new ArrayList<>();
}
