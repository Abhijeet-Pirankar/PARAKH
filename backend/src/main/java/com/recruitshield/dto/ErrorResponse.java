package com.recruitshield.dto;

import java.time.Instant;
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
public class ErrorResponse {

    private String error;
    private String message;
    @Builder.Default
    private List<String> details = new ArrayList<>();
    @Builder.Default
    private String timestamp = Instant.now().toString();
}
