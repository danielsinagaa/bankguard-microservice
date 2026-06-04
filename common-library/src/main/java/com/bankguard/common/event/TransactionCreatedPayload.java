package com.bankguard.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionCreatedPayload(
        String transactionRef,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String channel,
        String deviceId,
        String ipAddress,
        String location,
        Instant createdAt
) {
}
