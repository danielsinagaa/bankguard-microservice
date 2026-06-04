package com.bankguard.auditsearch.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.auditsearch.entity.KafkaEventLogEntity;
import com.bankguard.auditsearch.repository.KafkaEventLogRepository;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.event.DeadLetterEvent;
import com.bankguard.common.exception.InvalidKafkaEventException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class RiskScoredDeadLetterPublisherTest {

    @Test
    void shouldPublishRiskScoredDlqPayloadAndLogStatus() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaEventLogRepository eventLogRepository = mock(KafkaEventLogRepository.class);
        RiskScoredDeadLetterPublisher publisher = new RiskScoredDeadLetterPublisher(
                kafkaTemplate,
                eventLogRepository,
                Clock.fixed(Instant.parse("2026-06-04T10:00:10Z"), ZoneOffset.UTC)
        );
        when(kafkaTemplate.send(eq(KafkaTopics.TRANSACTION_RISK_SCORED_DLQ), eq("TRX-20260604-000001"), any(DeadLetterEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                KafkaTopics.TRANSACTION_RISK_SCORED,
                0,
                10L,
                "TRX-20260604-000001",
                """
                        {
                          "eventId": "22222222-2222-2222-2222-222222222222",
                          "eventType": "TRANSACTION_RISK_SCORED",
                          "aggregateId": "TRX-20260604-000001",
                          "payload": {}
                        }
                        """
        );

        publisher.publish(record, new InvalidKafkaEventException("Decision is invalid"), 3);

        ArgumentCaptor<DeadLetterEvent> dlqCaptor = ArgumentCaptor.forClass(DeadLetterEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.TRANSACTION_RISK_SCORED_DLQ), eq("TRX-20260604-000001"), dlqCaptor.capture());
        assertThat(dlqCaptor.getValue().originalTopic()).isEqualTo(KafkaTopics.TRANSACTION_RISK_SCORED);
        assertThat(dlqCaptor.getValue().error().errorType()).isEqualTo("VALIDATION_ERROR");
        assertThat(dlqCaptor.getValue().error().failedService()).isEqualTo("audit-search-service");

        ArgumentCaptor<KafkaEventLogEntity> logCaptor = ArgumentCaptor.forClass(KafkaEventLogEntity.class);
        verify(eventLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getEventId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("DLQ");
    }
}
