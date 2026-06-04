package com.bankguard.common.event;

import java.time.Instant;

public record DeadLetterError(
        String errorType,
        String errorMessage,
        String stackTrace,
        String failedService,
        Instant failedAt,
        int attempt
) {
}
