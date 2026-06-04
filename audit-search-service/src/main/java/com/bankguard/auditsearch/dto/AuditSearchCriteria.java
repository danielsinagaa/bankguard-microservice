package com.bankguard.auditsearch.dto;

import java.time.LocalDate;

public record AuditSearchCriteria(
        String keyword,
        String transactionRef,
        String customerCif,
        String decision,
        Integer minimumRiskScore,
        String riskFactor,
        String channel,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
}
