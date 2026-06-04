package com.bankguard.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountMaskingUtilTest {

    @Test
    void shouldMaskAccountNumberWhenLengthIsAtLeastSeven() {
        assertThat(AccountMaskingUtil.mask("1234567890")).isEqualTo("123****890");
    }

    @Test
    void shouldMaskShortAccountNumberExceptLastTwoCharacters() {
        assertThat(AccountMaskingUtil.mask("123456")).isEqualTo("****56");
    }

    @Test
    void shouldReturnShortAccountNumberWhenLengthIsTwoOrLess() {
        assertThat(AccountMaskingUtil.mask("12")).isEqualTo("12");
    }

    @Test
    void shouldReturnNullWhenAccountNumberIsNull() {
        assertThat(AccountMaskingUtil.mask(null)).isNull();
    }

    @Test
    void shouldReturnEmptyStringWhenAccountNumberIsBlank() {
        assertThat(AccountMaskingUtil.mask("   ")).isEmpty();
    }
}
