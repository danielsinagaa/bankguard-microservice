package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTopRiskyCustomerReportResponse(
        LocalDate transactionDate,
        int riskRank,
        String customerCif,
        String customerName,
        long totalTransactions,
        BigDecimal totalAmount,
        BigDecimal averageRiskScore,
        int highestRiskScore
) {
}
