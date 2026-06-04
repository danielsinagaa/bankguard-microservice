package com.bankguard.riskengine.dto;

import com.bankguard.riskengine.entity.TransactionEntity;
import java.math.BigDecimal;
import java.util.Map;

public record TransactionContext(
        TransactionEntity transaction,
        String customerRiskLevel,
        boolean trustedDevice,
        boolean knownDevice,
        boolean knownLocation,
        boolean blacklistedDestination,
        long velocityCount10m,
        BigDecimal velocityAmount10m,
        Map<String, RuleConfig> ruleConfigs
) {
    public TransactionContext {
        velocityAmount10m = velocityAmount10m == null ? BigDecimal.ZERO : velocityAmount10m;
        ruleConfigs = ruleConfigs == null ? Map.of() : Map.copyOf(ruleConfigs);
    }

    public RuleConfig config(String ruleCode) {
        return ruleConfigs.get(ruleCode);
    }
}
