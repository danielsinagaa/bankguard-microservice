package com.bankguard.riskengine.cache;

import com.bankguard.common.redis.RedisKeyBuilder;
import java.math.BigDecimal;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class VelocityCounterService {
    private static final Logger log = LoggerFactory.getLogger(VelocityCounterService.class);
    private static final Duration WINDOW_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public VelocityCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public VelocitySnapshot incrementAndGet(String sourceAccountNumber, BigDecimal amount) {
        String countKey = RedisKeyBuilder.velocityCount(sourceAccountNumber);
        String amountKey = RedisKeyBuilder.velocityAmount(sourceAccountNumber);
        try {
            Long count = redisTemplate.opsForValue().increment(countKey);
            double amountIncrement = amount.doubleValue();
            Double amountTotal = redisTemplate.opsForValue().increment(amountKey, amountIncrement);
            if (count != null && count == 1L) {
                redisTemplate.expire(countKey, WINDOW_TTL);
            }
            if (amountTotal != null && BigDecimal.valueOf(amountTotal).compareTo(BigDecimal.valueOf(amountIncrement)) == 0) {
                redisTemplate.expire(amountKey, WINDOW_TTL);
            }
            return new VelocitySnapshot(count == null ? 0 : count, amountTotal == null ? BigDecimal.ZERO : BigDecimal.valueOf(amountTotal));
        } catch (RuntimeException ex) {
            log.warn("Redis velocity counter unavailable, sourceAccountNumber={}, fallback=count_zero_amount_zero, error={}", sourceAccountNumber, ex.getMessage());
            return new VelocitySnapshot(0, BigDecimal.ZERO);
        }
    }
}
