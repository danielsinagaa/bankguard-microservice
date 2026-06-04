package com.bankguard.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer_risk_profiles")
public class CustomerRiskProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "risk_reason")
    private String riskReason;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected CustomerRiskProfileEntity() {
    }

    public CustomerRiskProfileEntity(Long customerId, String riskLevel, String riskReason, Instant createdAt) {
        this.customerId = customerId;
        this.riskLevel = riskLevel;
        this.riskReason = riskReason;
        this.lastReviewedAt = createdAt;
        this.createdAt = createdAt;
    }

    public void update(String riskLevel, String riskReason, Instant reviewedAt) {
        this.riskLevel = riskLevel;
        this.riskReason = riskReason;
        this.lastReviewedAt = reviewedAt;
        this.updatedAt = reviewedAt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
