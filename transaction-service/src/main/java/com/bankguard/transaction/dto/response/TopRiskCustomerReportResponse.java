package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;

public record TopRiskCustomerReportResponse(
        String customerCif,
        String customerName,
        long totalTransactions,
        BigDecimal averageRiskScore,
        int highestRiskScore,
        BigDecimal totalAmount
) {
}
