package com.bankguard.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MasterDataServiceApplicationTests {

    @Test
    void shouldExposeApplicationClass() {
        assertThat(MasterDataServiceApplication.class).isNotNull();
    }
}
