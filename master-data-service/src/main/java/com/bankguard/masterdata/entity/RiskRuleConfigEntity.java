package com.bankguard.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "risk_rule_configs")
public class RiskRuleConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false)
    private int score;

    @Column(name = "threshold_value", precision = 19, scale = 2)
    private BigDecimal thresholdValue;

    @Column(nullable = false)
    private boolean active;

    @Column
    private String description;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected RiskRuleConfigEntity() {
    }

    public RiskRuleConfigEntity(String ruleCode, String ruleName, int score, BigDecimal thresholdValue, boolean active, String description) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.score = score;
        this.thresholdValue = thresholdValue;
        this.active = active;
        this.description = description;
    }

    public void update(int score, BigDecimal thresholdValue, boolean active, String description, Instant updatedAt) {
        this.score = score;
        this.thresholdValue = thresholdValue;
        this.active = active;
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
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

    public String getDescription() {
        return description;
    }
}
