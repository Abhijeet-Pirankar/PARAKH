package com.recruitshield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_text", columnDefinition = "LONGTEXT", nullable = false)
    private String offerText;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "recruiter_email")
    private String recruiterEmail;

    @Column(name = "received_via")
    private String receivedVia;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RiskReport riskReport;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void setRiskReport(RiskReport riskReport) {
        this.riskReport = riskReport;
        if (riskReport != null && riskReport.getOffer() != this) {
            riskReport.setOffer(this);
        }
    }
}
