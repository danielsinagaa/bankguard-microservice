package com.bankguard.masterdata.dto;

import java.time.Instant;

public record BlacklistedAccountResponse(
        Long id,
        String accountNumber,
        String reason,
        boolean active,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
