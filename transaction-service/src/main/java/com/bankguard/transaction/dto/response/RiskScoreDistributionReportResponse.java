package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;

public record RiskScoreDistributionReportResponse(
        String riskBucket,
        long totalTransactions,
        int minimumScore,
        int maximumScore,
        BigDecimal averageScore
) {
}
