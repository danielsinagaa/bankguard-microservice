package com.bankguard.common.api;

import java.time.Instant;
import java.util.List;

public record CommonErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<ErrorDetail> details,
        String path,
        String requestId
) {
    public CommonErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static CommonErrorResponse of(
            int status,
            String error,
            String message,
            List<ErrorDetail> details,
            String path,
            String requestId
    ) {
        return new CommonErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                details,
                path,
                requestId
        );
    }
}
