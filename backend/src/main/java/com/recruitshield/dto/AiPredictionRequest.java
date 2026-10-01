package com.recruitshield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPredictionRequest {

    /**
     * Offer text sent to Python AI microservice for classification.
     */
    private String offerText;
}
