package com.bankguard.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionRiskScoredPayload(
        String transactionRef,
        String customerCif,
        String customerName,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String channel,
        String deviceId,
        String ipAddress,
        String location,
        int riskScore,
        String decision,
        List<RiskFactorPayload> riskFactors,
        Instant createdAt,
        Instant scoredAt
) {
    public TransactionRiskScoredPayload {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }
}
