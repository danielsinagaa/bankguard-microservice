package com.bankguard.auditsearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditSearchServiceApplicationTests {

    @Test
    void shouldExposeApplicationClass() {
        assertThat(AuditSearchServiceApplication.class).isNotNull();
    }
}
