package com.recruitshield.controller;

import com.recruitshield.dto.ErrorResponse;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * Primary PARAKH risk analysis endpoint.
     * Evaluates job and internship offer text against heuristic safety indicators.
     */
    @PostMapping(value = {"/analyze-offer", "/verify"})
    public ResponseEntity<VerifyResponse> analyzeOffer(@RequestBody(required = false) VerifyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null.");
        }
        VerifyResponse response = analysisService.analyzeOffer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles validation and bad input errors with structured JSON response.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .details(List.of(ex.getMessage()))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
