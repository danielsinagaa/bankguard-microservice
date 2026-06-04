package com.bankguard.common.api;

public record ErrorDetail(
        String field,
        String message
) {
}
