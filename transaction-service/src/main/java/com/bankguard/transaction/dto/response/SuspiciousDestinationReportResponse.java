package com.bankguard.transaction.dto.response;

import java.math.BigDecimal;

public record SuspiciousDestinationReportResponse(
        String destinationAccountNumber,
        long totalReceived,
        long uniqueSenders,
        BigDecimal totalAmount,
        BigDecimal averageRiskScore
) {
}
