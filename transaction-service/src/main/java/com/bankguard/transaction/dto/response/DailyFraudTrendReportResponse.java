package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyFraudTrendReportResponse(
        LocalDate transactionDate,
        long totalTransactions,
        long approvedCount,
        long reviewCount,
        long blockedCount,
        BigDecimal averageRiskScore,
        BigDecimal totalAmount
) {
}
