package com.bankguard.riskengine.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionCreatedPayload;
import com.bankguard.common.exception.ApiException;
import com.bankguard.riskengine.service.RiskEngineProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class TransactionCreatedConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldProcessValidTransactionCreatedEventAndAcknowledge() throws JsonProcessingException {
        RiskEngineProcessor processor = mock(RiskEngineProcessor.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        TransactionCreatedConsumer consumer = new TransactionCreatedConsumer(processor);
        EventEnvelope<TransactionCreatedPayload> envelope = validEnvelope(EventType.TRANSACTION_CREATED.name());
        String serializedPayload = objectMapper.writeValueAsString(envelope);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                KafkaTopics.TRANSACTION_CREATED,
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
    void shouldRejectInvalidEnvelopeBeforeProcessing() throws JsonProcessingException {
        RiskEngineProcessor processor = mock(RiskEngineProcessor.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        TransactionCreatedConsumer consumer = new TransactionCreatedConsumer(processor);
        String serializedPayload = objectMapper.writeValueAsString(validEnvelope("WRONG_EVENT"));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                KafkaTopics.TRANSACTION_CREATED,
                0,
                1L,
                "TRX-20260604-000001",
                serializedPayload
        );

        assertThatThrownBy(() -> consumer.consume(record, acknowledgment))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("TRANSACTION_CREATED");
        verifyNoInteractions(processor);
        verify(acknowledgment, never()).acknowledge();
    }

    private EventEnvelope<TransactionCreatedPayload> validEnvelope(String eventType) {
        TransactionCreatedPayload payload = new TransactionCreatedPayload(
                "TRX-20260604-000001",
                "1234567890",
                "9876543210",
                new BigDecimal("25000000"),
                "IDR",
                "MOBILE_BANKING",
                "DEVICE-1",
                "36.77.88.12",
                "Jakarta",
                Instant.parse("2026-06-04T10:00:00Z")
        );
        return new EventEnvelope<>(
                "11111111-1111-1111-1111-111111111111",
                eventType,
                EventEnvelope.DEFAULT_EVENT_VERSION,
                EventEnvelope.TRANSACTION_AGGREGATE_TYPE,
                "TRX-20260604-000001",
                Instant.parse("2026-06-04T10:00:01Z"),
                ProducerName.TRANSACTION_SERVICE,
                "req-001",
                "req-001",
                payload
        );
    }
}
