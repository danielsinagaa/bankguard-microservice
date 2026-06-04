package com.bankguard.riskengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiskEngineServiceApplicationTests {

    @Test
    void shouldExposeApplicationClass() {
        assertThat(RiskEngineServiceApplication.class).isNotNull();
    }
}
