package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;

public record TransactionVelocityReportResponse(
        String sourceAccountNumber,
        long transactionCount,
        BigDecimal totalAmount,
        int maxRiskScore
) {
}
