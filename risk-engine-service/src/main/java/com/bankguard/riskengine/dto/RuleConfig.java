package com.bankguard.riskengine.dto;

import java.math.BigDecimal;

public record RuleConfig(
        String ruleCode,
        int score,
        BigDecimal thresholdValue,
        boolean active
) {
}
