package com.bankguard.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CommonErrorResponseTest {

    @Test
    void shouldCreateErrorResponseWithDetails() {
        ErrorDetail detail = new ErrorDetail("amount", "Amount must be greater than zero");

        CommonErrorResponse response = CommonErrorResponse.of(
                400,
                "VALIDATION_ERROR",
                "Request validation failed",
                List.of(detail),
                "/api/v1/transactions",
                "req-001"
        );

        assertThat(response.timestamp()).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.message()).isEqualTo("Request validation failed");
        assertThat(response.details()).containsExactly(detail);
        assertThat(response.path()).isEqualTo("/api/v1/transactions");
        assertThat(response.requestId()).isEqualTo("req-001");
    }

    @Test
    void shouldUseEmptyDetailsWhenDetailsAreNull() {
        CommonErrorResponse response = CommonErrorResponse.of(
                500,
                "INTERNAL_SERVER_ERROR",
                "Unexpected error",
                null,
                "/api/v1/test",
                "req-002"
        );

        assertThat(response.details()).isEmpty();
    }
}
