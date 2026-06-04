package com.bankguard.auditsearch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.auditsearch.entity.KafkaEventLogEntity;
import com.bankguard.auditsearch.mapper.AuditDocumentMapper;
import com.bankguard.auditsearch.repository.AuditDocumentRepository;
import com.bankguard.auditsearch.repository.KafkaEventLogRepository;
import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuditIndexingProcessorTest {

    @Test
    void shouldIndexDocumentAndWriteEventLog() {
        KafkaEventLogRepository eventLogRepository = mock(KafkaEventLogRepository.class);
        AuditDocumentRepository documentRepository = mock(AuditDocumentRepository.class);
        AuditIndexingProcessor processor = new AuditIndexingProcessor(
                eventLogRepository,
                documentRepository,
                new AuditDocumentMapper(),
                Clock.fixed(Instant.parse("2026-06-04T10:00:05Z"), ZoneOffset.UTC)
        );

        processor.process(envelope(), "{\"eventId\":\"11111111-1111-1111-1111-111111111111\"}");

        verify(documentRepository).save(any());
        verify(eventLogRepository).save(any(KafkaEventLogEntity.class));
    }

    @Test
    void shouldSkipDuplicateEvent() {
        KafkaEventLogRepository eventLogRepository = mock(KafkaEventLogRepository.class);
        AuditDocumentRepository documentRepository = mock(AuditDocumentRepository.class);
        when(eventLogRepository.existsByEventIdAndConsumerGroup("11111111-1111-1111-1111-111111111111", AuditIndexingProcessor.CONSUMER_GROUP))
                .thenReturn(true);
        AuditIndexingProcessor processor = new AuditIndexingProcessor(
                eventLogRepository,
                documentRepository,
                new AuditDocumentMapper(),
                Clock.systemUTC()
        );

        processor.process(envelope(), "{}");

        verify(documentRepository, never()).save(any());
        verify(eventLogRepository, never()).save(any());
    }

    static EventEnvelope<TransactionRiskScoredPayload> envelope() {
        return new EventEnvelope<>(
                "11111111-1111-1111-1111-111111111111",
                EventType.TRANSACTION_RISK_SCORED.name(),
                EventEnvelope.DEFAULT_EVENT_VERSION,
                EventEnvelope.TRANSACTION_AGGREGATE_TYPE,
                "TRX-20260604-000001",
                Instant.parse("2026-06-04T10:00:04Z"),
                ProducerName.RISK_ENGINE_SERVICE,
                "req-001",
                "req-001",
                AuditDocumentMapperTestPayload.payload()
        );
    }
}
