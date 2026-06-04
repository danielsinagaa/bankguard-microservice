package com.bankguard.masterdata.cache;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisCacheInvalidationServiceTest {

    @Test
    void shouldDeleteBlacklistKeysIncludingNegativeCache() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisCacheInvalidationService service = new RedisCacheInvalidationService(redisTemplate);

        service.invalidateBlacklistedAccount("9876543210");

        verify(redisTemplate).delete(List.of(
                "blacklist:account:9876543210",
                "blacklist:account:9876543210:negative"
        ));
    }

    @Test
    void shouldDeleteTrustedDeviceRiskProfileAndRuleConfigKeys() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisCacheInvalidationService service = new RedisCacheInvalidationService(redisTemplate);

        service.invalidateTrustedDevice(1001L, "IPHONE-15-DEVICE-001");
        service.invalidateCustomerRiskProfile(1001L);
        service.invalidateRiskRuleConfig("HIGH_AMOUNT");

        verify(redisTemplate).delete(List.of("customer:trusted-device:1001:IPHONE-15-DEVICE-001"));
        verify(redisTemplate).delete(List.of("customer:risk-profile:1001"));
        verify(redisTemplate).delete(List.of("risk-rule:config:HIGH_AMOUNT"));
    }

    @Test
    void shouldNotFailBusinessFlowWhenRedisInvalidationFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(redisTemplate)
                .delete(List.of("risk-rule:config:HIGH_AMOUNT"));
        RedisCacheInvalidationService service = new RedisCacheInvalidationService(redisTemplate);

        assertThatNoException().isThrownBy(() -> service.invalidateRiskRuleConfig("HIGH_AMOUNT"));
    }
}
