package com.bankguard.riskengine.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.event.DeadLetterEvent;
import com.bankguard.riskengine.entity.KafkaEventLogEntity;
import com.bankguard.riskengine.repository.KafkaEventLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class TransactionCreatedDeadLetterPublisherTest {

    @Test
    void shouldPublishTransactionCreatedDlqPayloadAndLogStatus() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaEventLogRepository eventLogRepository = mock(KafkaEventLogRepository.class);
        TransactionCreatedDeadLetterPublisher publisher = new TransactionCreatedDeadLetterPublisher(
                kafkaTemplate,
                eventLogRepository,
                Clock.fixed(Instant.parse("2026-06-04T10:00:10Z"), ZoneOffset.UTC)
        );
        when(kafkaTemplate.send(eq(KafkaTopics.TRANSACTION_CREATED_DLQ), eq("TRX-20260604-000001"), any(DeadLetterEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                KafkaTopics.TRANSACTION_CREATED,
                0,
                10L,
                "TRX-20260604-000001",
                """
                        {
                          "eventId": "11111111-1111-1111-1111-111111111111",
                          "eventType": "TRANSACTION_CREATED",
                          "aggregateId": "TRX-20260604-000001",
                          "payload": {}
                        }
                        """
        );

        publisher.publish(record, new IllegalStateException("database unavailable"), 3);

        ArgumentCaptor<DeadLetterEvent> dlqCaptor = ArgumentCaptor.forClass(DeadLetterEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.TRANSACTION_CREATED_DLQ), eq("TRX-20260604-000001"), dlqCaptor.capture());
        assertThat(dlqCaptor.getValue().originalTopic()).isEqualTo(KafkaTopics.TRANSACTION_CREATED);
        assertThat(dlqCaptor.getValue().originalKey()).isEqualTo("TRX-20260604-000001");
        assertThat(dlqCaptor.getValue().error().failedService()).isEqualTo("risk-engine-service");
        assertThat(dlqCaptor.getValue().error().attempt()).isEqualTo(3);

        ArgumentCaptor<KafkaEventLogEntity> logCaptor = ArgumentCaptor.forClass(KafkaEventLogEntity.class);
        verify(eventLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getEventId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("DLQ");
    }
}
