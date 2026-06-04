package com.bankguard.riskengine.cache;

import com.bankguard.common.constant.RedisKeys;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VelocityCounterService {
    private static final Duration WINDOW_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public VelocityCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public VelocitySnapshot incrementAndGet(String sourceAccountNumber, BigDecimal amount) {
        String countKey = RedisKeys.VELOCITY_COUNT_PREFIX + ":" + sourceAccountNumber + ":10m";
        String amountKey = RedisKeys.VELOCITY_AMOUNT_PREFIX + ":" + sourceAccountNumber + ":10m";
        try {
            Long count = redisTemplate.opsForValue().increment(countKey);
            Long amountTotal = redisTemplate.opsForValue().increment(amountKey, amount.longValue());
            if (count != null && count == 1L) {
                redisTemplate.expire(countKey, WINDOW_TTL);
            }
            if (amountTotal != null && amountTotal.equals(amount.longValue())) {
                redisTemplate.expire(amountKey, WINDOW_TTL);
            }
            return new VelocitySnapshot(count == null ? 0 : count, BigDecimal.valueOf(amountTotal == null ? 0 : amountTotal));
        } catch (RuntimeException ex) {
            return new VelocitySnapshot(0, BigDecimal.ZERO);
        }
    }
}
