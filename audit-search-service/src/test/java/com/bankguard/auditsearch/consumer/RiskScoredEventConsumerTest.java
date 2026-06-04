package com.bankguard.auditsearch.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bankguard.auditsearch.service.AuditIndexingProcessor;
import com.bankguard.auditsearch.service.AuditDocumentMapperTestPayload;
import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import com.bankguard.common.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class RiskScoredEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldProcessValidEventAndAcknowledge() throws JsonProcessingException {
        AuditIndexingProcessor processor = mock(AuditIndexingProcessor.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        RiskScoredEventConsumer consumer = new RiskScoredEventConsumer(processor);
        EventEnvelope<TransactionRiskScoredPayload> envelope = envelope(EventType.TRANSACTION_RISK_SCORED.name());
        String serializedPayload = objectMapper.writeValueAsString(envelope);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                KafkaTopics.TRANSACTION_RISK_SCORED,
                0,
                1L,
                "TRX-20260604-000001",
                serializedPayload
        );

        consumer.consume(record, acknowledgment);

        verify(processor).process(envelope, serializedPayload);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRejectInvalidEventBeforeProcessing() throws JsonProcessingException {
        AuditIndexingProcessor processor = mock(AuditIndexingProcessor.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        RiskScoredEventConsumer consumer = new RiskScoredEventConsumer(processor);
        String serializedPayload = objectMapper.writeValueAsString(envelope("WRONG_EVENT"));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                KafkaTopics.TRANSACTION_RISK_SCORED,
                0,
                1L,
                "TRX-20260604-000001",
                serializedPayload
        );

        assertThatThrownBy(() -> consumer.consume(record, acknowledgment))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("TRANSACTION_RISK_SCORED");
        verifyNoInteractions(processor);
        verify(acknowledgment, never()).acknowledge();
    }

    private EventEnvelope<TransactionRiskScoredPayload> envelope(String eventType) {
        return new EventEnvelope<>(
                "11111111-1111-1111-1111-111111111111",
                eventType,
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
