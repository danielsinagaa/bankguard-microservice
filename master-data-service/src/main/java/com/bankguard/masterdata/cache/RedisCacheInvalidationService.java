package com.bankguard.masterdata.cache;

import com.bankguard.common.redis.RedisKeyBuilder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheInvalidationService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidationService.class);

    private final StringRedisTemplate redisTemplate;

    public RedisCacheInvalidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void invalidateBlacklistedAccount(String accountNumber) {
        deleteKeys(List.of(
                RedisKeyBuilder.blacklistAccount(accountNumber),
                RedisKeyBuilder.blacklistAccountNegative(accountNumber)
        ));
    }

    public void invalidateTrustedDevice(Long customerId, String deviceId) {
        deleteKeys(List.of(RedisKeyBuilder.trustedDevice(customerId, deviceId)));
    }

    public void invalidateCustomerRiskProfile(Long customerId) {
        deleteKeys(List.of(RedisKeyBuilder.customerRiskProfile(customerId)));
    }

    public void invalidateRiskRuleConfig(String ruleCode) {
        deleteKeys(List.of(RedisKeyBuilder.riskRuleConfig(ruleCode)));
    }

    private void deleteKeys(List<String> keys) {
        try {
            redisTemplate.delete(keys);
        } catch (RuntimeException ex) {
            log.warn("Redis cache invalidation failed for keys={}, error={}", keys, ex.getMessage());
        }
    }
}
