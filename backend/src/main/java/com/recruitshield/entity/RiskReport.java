package com.recruitshield.entity;

import com.recruitshield.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "risk_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Convert(converter = StringListConverter.class)
    @Column(name = "red_flags", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> redFlags = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "positive_signals", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> positiveSignals = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "recommendations", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
