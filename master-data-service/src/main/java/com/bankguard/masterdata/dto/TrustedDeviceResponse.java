package com.bankguard.masterdata.dto;

import java.time.Instant;

public record TrustedDeviceResponse(
        Long customerId,
        String deviceId,
        String deviceName,
        boolean trusted,
        Instant firstSeenAt,
        Instant lastUsedAt
) {
}
