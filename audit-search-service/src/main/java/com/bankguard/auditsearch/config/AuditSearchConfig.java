package com.bankguard.auditsearch.config;

import com.bankguard.auditsearch.document.AuditDocument;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Configuration
public class AuditSearchConfig {
    private static final Logger log = LoggerFactory.getLogger(AuditSearchConfig.class);

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
}
