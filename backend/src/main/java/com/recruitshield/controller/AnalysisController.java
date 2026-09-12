package com.recruitshield.controller;

import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.dto.VerifyResponse;
import com.recruitshield.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verify")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend to call the API
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<VerifyResponse> verifyOffer(@RequestBody VerifyRequest request) {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        VerifyResponse response = analysisService.analyzeOffer(request);
        return ResponseEntity.ok(response);
    }
}
