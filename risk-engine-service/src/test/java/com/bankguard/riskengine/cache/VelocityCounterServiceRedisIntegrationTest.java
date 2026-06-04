package com.bankguard.riskengine.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankguard.common.redis.RedisKeyBuilder;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class VelocityCounterServiceRedisIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private VelocityCounterService service;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(
                REDIS.getHost(),
                REDIS.getMappedPort(6379)
        ));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        service = new VelocityCounterService(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void shouldIncrementVelocityCountersAndApplyTtl() {
        VelocitySnapshot first = service.incrementAndGet("1234567890", new BigDecimal("1000000.50"));
        VelocitySnapshot second = service.incrementAndGet("1234567890", new BigDecimal("2000000.25"));

        String countKey = RedisKeyBuilder.velocityCount("1234567890");
        String amountKey = RedisKeyBuilder.velocityAmount("1234567890");
        Long countTtl = redisTemplate.getExpire(countKey);
        Long amountTtl = redisTemplate.getExpire(amountKey);

        assertThat(first.count()).isEqualTo(1);
        assertThat(second.count()).isEqualTo(2);
        assertThat(second.amount()).isEqualByComparingTo(new BigDecimal("3000000.75"));
        assertThat(countTtl).isPositive();
        assertThat(amountTtl).isPositive();
    }
}
