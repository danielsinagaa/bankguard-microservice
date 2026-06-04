package com.bankguard.auditsearch.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AuditSearchResponse(
        String transactionRef,
        String customerCif,
        String customerName,
        String sourceAccountMasked,
        String destinationAccountMasked,
        BigDecimal amount,
        String currency,
        String channel,
        String location,
        int riskScore,
        String decision,
        List<String> riskFactors,
        Instant createdAt,
        Instant scoredAt
) {
    public AuditSearchResponse {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }
}
