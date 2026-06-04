package com.bankguard.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    void shouldCreatePageResponse() {
        PageResponse<String> response = PageResponse.of(List.of("one", "two"), 0, 20, 2);

        assertThat(response.data()).containsExactly("one", "two");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(2);
    }

    @Test
    void shouldUseEmptyDataWhenDataIsNull() {
        PageResponse<String> response = PageResponse.of(null, 1, 10, 0);

        assertThat(response.data()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.total()).isZero();
    }
}
