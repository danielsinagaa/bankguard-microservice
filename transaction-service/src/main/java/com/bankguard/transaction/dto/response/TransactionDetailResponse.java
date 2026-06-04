package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionDetailResponse(
        String transactionRef,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String channel,
        String deviceId,
        String ipAddress,
        String location,
        String status,
        int riskScore,
        String riskDecision,
        List<RiskFactorResponse> riskFactors,
        Instant createdAt,
        Instant updatedAt
) {
    public TransactionDetailResponse {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }
}
