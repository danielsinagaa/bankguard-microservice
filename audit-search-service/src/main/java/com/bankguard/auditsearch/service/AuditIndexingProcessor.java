package com.bankguard.auditsearch.service;

import com.bankguard.auditsearch.entity.KafkaEventLogEntity;
import com.bankguard.auditsearch.mapper.AuditDocumentMapper;
import com.bankguard.auditsearch.repository.AuditDocumentRepository;
import com.bankguard.auditsearch.repository.KafkaEventLogRepository;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditIndexingProcessor {
    public static final String CONSUMER_GROUP = "audit-search-service-group";

    private final KafkaEventLogRepository kafkaEventLogRepository;
    private final AuditDocumentRepository auditDocumentRepository;
    private final AuditDocumentMapper auditDocumentMapper;
    private final Clock clock;

    public AuditIndexingProcessor(
            KafkaEventLogRepository kafkaEventLogRepository,
            AuditDocumentRepository auditDocumentRepository,
            AuditDocumentMapper auditDocumentMapper,
            Clock clock
    ) {
        this.kafkaEventLogRepository = kafkaEventLogRepository;
        this.auditDocumentRepository = auditDocumentRepository;
        this.auditDocumentMapper = auditDocumentMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(EventEnvelope<TransactionRiskScoredPayload> envelope, String serializedPayload) {
        if (kafkaEventLogRepository.existsByEventIdAndConsumerGroup(envelope.eventId(), CONSUMER_GROUP)) {
            return;
        }

        auditDocumentRepository.save(auditDocumentMapper.toDocument(envelope.payload()));
        Instant processedAt = Instant.now(clock);
        kafkaEventLogRepository.save(new KafkaEventLogEntity(
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateId(),
                KafkaTopics.TRANSACTION_RISK_SCORED,
                CONSUMER_GROUP,
                serializedPayload,
                "CONSUMED",
                processedAt,
                processedAt
        ));
    }
}
