package com.bankguard.riskengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "risk_rule_configs")
public class RiskRuleConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(nullable = false)
    private int score;

    @Column(name = "threshold_value", precision = 19, scale = 2)
    private BigDecimal thresholdValue;

    @Column(nullable = false)
    private boolean active;

    protected RiskRuleConfigEntity() {
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public int getScore() {
        return score;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public boolean isActive() {
        return active;
    }
}
