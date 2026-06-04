package com.bankguard.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorrelationIdUtilTest {

    @Test
    void shouldReturnExistingRequestIdWhenPresent() {
        String requestId = CorrelationIdUtil.resolveRequestId(
                "req-001",
                () -> UUID.fromString("7a7f0e9b-3dd6-4f22-b23a-44e42c25a001")
        );

        assertThat(requestId).isEqualTo("req-001");
    }

    @Test
    void shouldGenerateRequestIdWhenMissing() {
        UUID uuid = UUID.fromString("7a7f0e9b-3dd6-4f22-b23a-44e42c25a001");

        String requestId = CorrelationIdUtil.resolveRequestId(null, () -> uuid);

        assertThat(requestId).isEqualTo(uuid.toString());
    }

    @Test
    void shouldGenerateRequestIdWhenBlank() {
        UUID uuid = UUID.fromString("5b743cfb-f21c-4de4-8ac2-d97d86bc6001");

        String requestId = CorrelationIdUtil.resolveRequestId("  ", () -> uuid);

        assertThat(requestId).isEqualTo(uuid.toString());
    }
}
