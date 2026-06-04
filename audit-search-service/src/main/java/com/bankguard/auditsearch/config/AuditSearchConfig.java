package com.bankguard.auditsearch.config;

import com.bankguard.auditsearch.document.AuditDocument;
import com.bankguard.auditsearch.kafka.RiskScoredDeadLetterPublisher;
import com.bankguard.common.exception.InvalidKafkaEventException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class AuditSearchConfig {
    private static final Logger log = LoggerFactory.getLogger(AuditSearchConfig.class);
    private static final int MAX_DELIVERY_ATTEMPTS = 3;
    private static final int MAX_RETRIES = MAX_DELIVERY_ATTEMPTS - 1;
    private static final long INITIAL_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 10_000L;

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ApplicationRunner auditIndexInitializer(ElasticsearchOperations elasticsearchOperations) {
        return args -> {
            try {
                IndexOperations indexOperations = elasticsearchOperations.indexOps(AuditDocument.class);
                if (!indexOperations.exists()) {
                    indexOperations.create();
                    indexOperations.putMapping(indexOperations.createMapping(AuditDocument.class));
                }
            } catch (RuntimeException ex) {
                log.warn("Audit index initialization skipped, error={}", ex.getMessage());
            }
        };
    }

    @Bean
    DefaultErrorHandler auditSearchKafkaErrorHandler(RiskScoredDeadLetterPublisher deadLetterPublisher) {
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
