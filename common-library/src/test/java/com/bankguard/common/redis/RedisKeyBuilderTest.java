package com.bankguard.common.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RedisKeyBuilderTest {

    @Test
    void shouldBuildReferenceCacheKeys() {
        assertThat(RedisKeyBuilder.blacklistAccount("9876543210"))
                .isEqualTo("blacklist:account:9876543210");
        assertThat(RedisKeyBuilder.blacklistAccountNegative("9876543210"))
                .isEqualTo("blacklist:account:9876543210:negative");
        assertThat(RedisKeyBuilder.customerRiskProfile(1001L))
                .isEqualTo("customer:risk-profile:1001");
        assertThat(RedisKeyBuilder.trustedDevice(1001L, "IPHONE-15-DEVICE-001"))
                .isEqualTo("customer:trusted-device:1001:IPHONE-15-DEVICE-001");
        assertThat(RedisKeyBuilder.riskRuleConfig("HIGH_AMOUNT"))
                .isEqualTo("risk-rule:config:HIGH_AMOUNT");
    }

    @Test
    void shouldBuildVelocityCounterKeys() {
        assertThat(RedisKeyBuilder.velocityCount("1234567890"))
                .isEqualTo("risk:velocity:count:1234567890:10m");
        assertThat(RedisKeyBuilder.velocityAmount("1234567890"))
                .isEqualTo("risk:velocity:amount:1234567890:10m");
    }

    @Test
    void shouldRejectBlankKeyParts() {
        assertThatThrownBy(() -> RedisKeyBuilder.blacklistAccount(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountNumber");
    }
}
