package com.bankguard.common.util;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class CorrelationIdUtil {
    private CorrelationIdUtil() {
    }

    public static String resolveRequestId(String requestId, Supplier<UUID> uuidSupplier) {
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }

        return Objects.requireNonNull(uuidSupplier, "uuidSupplier must not be null")
                .get()
                .toString();
    }
}
