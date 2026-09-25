package com.recruitshield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRequest {

    /**
     * Primary offer communication text (job description, offer letter, email body, chat transcript).
     */
    private String offerText;

    /**
     * Legacy field retained for backward compatibility.
     */
    private String text;

    /**
     * Stated company or organization name.
     */
    private String companyName;

    /**
     * Company official website or career portal URL.
     */
    private String companyWebsite;

    /**
     * Recruiter or sender email address.
     */
    private String recruiterEmail;

    /**
     * Communication medium (e.g., WhatsApp, Telegram, Email, LinkedIn, SMS).
     */
    private String receivedVia;

    /**
     * Returns the offer text from either offerText or text (legacy).
     */
    public String getEffectiveOfferText() {
        if (offerText != null && !offerText.trim().isEmpty()) {
            return offerText.trim();
        }
        if (text != null && !text.trim().isEmpty()) {
            return text.trim();
        }
        return "";
    }
}
