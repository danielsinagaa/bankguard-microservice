package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import com.bankguard.riskengine.entity.AccountEntity;
import com.bankguard.riskengine.entity.CustomerEntity;
import com.bankguard.riskengine.entity.TransactionEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class RiskRuleTestSupport {
    private static final Instant CREATED_AT = Instant.parse("2026-06-04T10:00:00Z");

    private RiskRuleTestSupport() {
    }

    static TransactionEntity transaction(BigDecimal amount, String deviceId, String location) {
        CustomerEntity customer = new CustomerEntity("CIF001", "Daniel Sinaga", "ACTIVE");
        AccountEntity sourceAccount = new AccountEntity(customer, "1234567890", "ACTIVE");
        return new TransactionEntity(
                "TRX-20260604-000001",
                sourceAccount,
                "9876543210",
                amount,
                "IDR",
                "MOBILE_BANKING",
                deviceId,
                "36.77.88.12",
                location,
                "PENDING_RISK_CHECK",
                CREATED_AT
        );
    }

    static RuleConfig config(String ruleCode, int score, String thresholdValue) {
        return new RuleConfig(
                ruleCode,
                score,
                thresholdValue == null ? null : new BigDecimal(thresholdValue),
                true
        );
    }

    static TransactionContext context(TransactionEntity transaction, RuleConfig... configs) {
        return context(transaction, "LOW", true, true, true, false, 1, BigDecimal.ZERO, configs);
    }

    static TransactionContext context(
            TransactionEntity transaction,
            String customerRiskLevel,
            boolean trustedDevice,
            boolean knownDevice,
            boolean knownLocation,
            boolean blacklistedDestination,
            long velocityCount10m,
            BigDecimal velocityAmount10m,
            RuleConfig... configs
    ) {
        Map<String, RuleConfig> configMap = Arrays.stream(configs)
                .collect(Collectors.toMap(RuleConfig::ruleCode, Function.identity()));
        return new TransactionContext(
                transaction,
                customerRiskLevel,
                trustedDevice,
                knownDevice,
                knownLocation,
                blacklistedDestination,
                velocityCount10m,
                velocityAmount10m,
                configMap
        );
    }
}
