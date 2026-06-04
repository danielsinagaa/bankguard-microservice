package com.bankguard.masterdata.dto;

import java.math.BigDecimal;

public record RiskRuleResponse(
        String ruleCode,
        String ruleName,
        int score,
        BigDecimal thresholdValue,
        boolean active,
        String description
) {
}
