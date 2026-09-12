package com.recruitshield.service;

import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    public VerifyResponse analyzeOffer(VerifyRequest request) {
        String text = request.getText().toLowerCase();
        int score = 0;
        List<String> reasons = new ArrayList<>();
        
        // Basic Rule-Based Analysis

        if (text.contains("registration fee") || text.contains("processing fee") || text.contains("pay rs") || text.contains("deposit")) {
            score += 40;
            reasons.add("Requests for payment, registration, or processing fees");
        }

        if (text.contains("whatsapp") || text.contains("telegram")) {
            score += 20;
            reasons.add("Encourages communication via unverified personal messaging apps");
        }

        if (text.contains("urgent") || text.contains("immediate hiring") || text.contains("limited spots")) {
            score += 15;
            reasons.add("Creates artificial urgency or pressure");
        }

        if (text.contains("@gmail.com") || text.contains("@yahoo.com") || text.contains("@hotmail.com")) {
            score += 25;
            reasons.add("Uses a personal or free email domain instead of a corporate domain");
        }

        if (text.contains("no interview") || text.contains("direct joining") || text.contains("without interview")) {
            score += 30;
            reasons.add("Promises direct employment without a proper interview process");
        }

        // Cap score at 100
        score = Math.min(score, 100);

        String status = determineStatus(score);
        String recommendation = determineRecommendation(score);

        return VerifyResponse.builder()
                .score(score)
                .status(status)
                .reasons(reasons.isEmpty() ? List.of("No obvious red flags detected in this text.") : reasons)
                .recommendation(recommendation)
                .build();
    }

    private String determineStatus(int score) {
        if (score < 40) return "Likely Genuine";
        if (score < 70) return "Needs Verification";
        return "Highly Suspicious";
    }

    private String determineRecommendation(int score) {
        if (score < 40) {
            return "This offer appears standard, but always verify the sender's identity and avoid sharing sensitive financial information.";
        } else if (score < 70) {
            return "Proceed with caution. Independently verify the company's existence, check their official career page, and do not pay any fees.";
        } else {
            return "Do NOT pay any fees or share personal information. Genuine companies do not ask candidates to pay for jobs. Block the sender and report the offer.";
        }
    }
}
