package com.bankguard.riskengine.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.EventEnvelopeFactory;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RiskScoringResult;
import com.bankguard.riskengine.entity.AccountEntity;
import com.bankguard.riskengine.entity.CustomerEntity;
import com.bankguard.riskengine.entity.TransactionEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class RiskScoredEventPublisherTest {

    @Test
    void shouldPublishRiskScoredEnvelopeUsingTransactionRefAsKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, EventEnvelope<TransactionRiskScoredPayload>> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq(KafkaTopics.TRANSACTION_RISK_SCORED), eq("TRX-20260604-000001"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        EventEnvelopeFactory factory = new EventEnvelopeFactory(
                Clock.fixed(Instant.parse("2026-06-04T10:00:03Z"), ZoneOffset.UTC),
                () -> UUID.fromString("11111111-1111-1111-1111-111111111111")
        );
        RiskScoredEventPublisher publisher = new RiskScoredEventPublisher(kafkaTemplate, factory);
        TransactionEntity transaction = transaction();
        RiskScoringResult scoringResult = new RiskScoringResult(
                72,
                "REVIEW",
                List.of(RiskFactor.triggered("HIGH_AMOUNT", "Transaction amount is above configured threshold", 25, Map.of()))
        );

        PublishedRiskScoredEvent published = publisher.publish(
                transaction,
                scoringResult,
                "req-001",
                Instant.parse("2026-06-04T10:00:03Z")
        );

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<EventEnvelope<TransactionRiskScoredPayload>> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.TRANSACTION_RISK_SCORED), eq("TRX-20260604-000001"), envelopeCaptor.capture());
        EventEnvelope<TransactionRiskScoredPayload> envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(envelope.eventType()).isEqualTo(EventType.TRANSACTION_RISK_SCORED.name());
        assertThat(envelope.producer()).isEqualTo(ProducerName.RISK_ENGINE_SERVICE);
        assertThat(envelope.aggregateId()).isEqualTo("TRX-20260604-000001");
        assertThat(envelope.payload().customerCif()).isEqualTo("CIF001");
        assertThat(envelope.payload().riskScore()).isEqualTo(72);
        assertThat(envelope.payload().decision()).isEqualTo("REVIEW");
        assertThat(envelope.payload().riskFactors()).hasSize(1);
        assertThat(published.serializedPayload()).contains("\"transactionRef\":\"TRX-20260604-000001\"");
    }

    private TransactionEntity transaction() {
        CustomerEntity customer = new CustomerEntity("CIF001", "Daniel Sinaga", "ACTIVE");
        AccountEntity sourceAccount = new AccountEntity(customer, "1234567890", "ACTIVE");
        return new TransactionEntity(
                "TRX-20260604-000001",
                sourceAccount,
                "9876543210",
                new BigDecimal("25000000"),
                "IDR",
                "MOBILE_BANKING",
                "DEVICE-1",
                "36.77.88.12",
                "Jakarta",
                "PENDING_RISK_CHECK",
                Instant.parse("2026-06-04T10:00:00Z")
        );
    }
}
