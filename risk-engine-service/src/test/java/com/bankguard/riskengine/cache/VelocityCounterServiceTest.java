package com.bankguard.riskengine.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class VelocityCounterServiceTest {

    @Test
    void shouldSetTtlWhenCounterKeysAreCreated() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("risk:velocity:count:1234567890:10m")).thenReturn(1L);
        when(valueOperations.increment("risk:velocity:amount:1234567890:10m", 25000000.0)).thenReturn(25000000.0);
        VelocityCounterService service = new VelocityCounterService(redisTemplate);

        VelocitySnapshot snapshot = service.incrementAndGet("1234567890", new BigDecimal("25000000"));

        assertThat(snapshot.count()).isEqualTo(1);
        assertThat(snapshot.amount()).isEqualByComparingTo("25000000");
        verify(redisTemplate).expire("risk:velocity:count:1234567890:10m", Duration.ofMinutes(10));
        verify(redisTemplate).expire("risk:velocity:amount:1234567890:10m", Duration.ofMinutes(10));
    }

    @Test
    void shouldNotResetTtlForExistingCounterKeys() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("risk:velocity:count:1234567890:10m")).thenReturn(2L);
        when(valueOperations.increment("risk:velocity:amount:1234567890:10m", 25000000.0)).thenReturn(50000000.0);
        VelocityCounterService service = new VelocityCounterService(redisTemplate);

        VelocitySnapshot snapshot = service.incrementAndGet("1234567890", new BigDecimal("25000000"));

        assertThat(snapshot.count()).isEqualTo(2);
        assertThat(snapshot.amount()).isEqualByComparingTo("50000000");
        verify(redisTemplate, never()).expire(any(String.class), any(Duration.class));
    }

    @Test
    void shouldReturnZeroVelocityWhenRedisFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("Redis unavailable"));
        VelocityCounterService service = new VelocityCounterService(redisTemplate);

        VelocitySnapshot snapshot = service.incrementAndGet("1234567890", new BigDecimal("25000000"));

        assertThat(snapshot.count()).isZero();
        assertThat(snapshot.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
