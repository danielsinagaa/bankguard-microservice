package com.bankguard.riskengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    protected CustomerRiskProfileEntity() {
    }

    public String getRiskLevel() {
        return riskLevel;
    }
}
