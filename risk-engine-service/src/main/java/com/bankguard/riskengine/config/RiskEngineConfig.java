package com.bankguard.riskengine.config;

import com.bankguard.common.event.EventEnvelopeFactory;
import com.bankguard.common.exception.InvalidKafkaEventException;
import com.bankguard.riskengine.kafka.TransactionCreatedDeadLetterPublisher;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class RiskEngineConfig {
    private static final int MAX_DELIVERY_ATTEMPTS = 3;
    private static final int MAX_RETRIES = MAX_DELIVERY_ATTEMPTS - 1;
    private static final long INITIAL_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 10_000L;

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    EventEnvelopeFactory eventEnvelopeFactory() {
        return EventEnvelopeFactory.system();
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    DefaultErrorHandler riskEngineKafkaErrorHandler(TransactionCreatedDeadLetterPublisher deadLetterPublisher) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
        backOff.setInitialInterval(INITIAL_BACKOFF_MS);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(MAX_BACKOFF_MS);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> deadLetterPublisher.publish(record, exception, MAX_DELIVERY_ATTEMPTS),
                backOff
        );
        errorHandler.addNotRetryableExceptions(InvalidKafkaEventException.class);
        return errorHandler;
    }
}
