package com.bankguard.masterdata.dto;

import java.time.Instant;

public record RiskProfileResponse(
        Long customerId,
        String riskLevel,
        String riskReason,
        Instant lastReviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
