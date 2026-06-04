package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record HighRiskTransactionReportResponse(
        String transactionRef,
        String customerCif,
        String customerName,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        int riskScore,
        String decision,
        String status,
        Instant createdAt
) {
}
