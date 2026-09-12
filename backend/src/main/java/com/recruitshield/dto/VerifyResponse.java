package com.recruitshield.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyResponse {
    private int score;
    private String status;
    private List<String> reasons;
    private String recommendation;
}
