package com.bankguard.transaction.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.EventEnvelopeFactory;
import com.bankguard.common.event.TransactionCreatedPayload;
import com.bankguard.transaction.entity.AccountEntity;
import com.bankguard.transaction.entity.CustomerEntity;
import com.bankguard.transaction.entity.TransactionEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

class TransactionCreatedEventPublisherTest {

    @Test
    void shouldPublishTransactionCreatedWithTransactionRefAsKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, EventEnvelope<TransactionCreatedPayload>> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq(KafkaTopics.TRANSACTION_CREATED), eq("TRX-20260604-000001"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        EventEnvelopeFactory envelopeFactory = new EventEnvelopeFactory(
                Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC),
                () -> java.util.UUID.fromString("7a7f0e9b-3dd6-4f22-b23a-44e42c25a001")
        );
        TransactionCreatedEventPublisher publisher = new TransactionCreatedEventPublisher(kafkaTemplate, envelopeFactory);

        PublishedTransactionEvent event = publisher.publishCreated(transaction(), "req-001");

        ArgumentCaptor<EventEnvelope<TransactionCreatedPayload>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        org.mockito.Mockito.verify(kafkaTemplate).send(eq(KafkaTopics.TRANSACTION_CREATED), eq("TRX-20260604-000001"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(EventType.TRANSACTION_CREATED.name());
        assertThat(captor.getValue().aggregateId()).isEqualTo("TRX-20260604-000001");
        assertThat(captor.getValue().requestId()).isEqualTo("req-001");
        assertThat(captor.getValue().payload().sourceAccountNumber()).isEqualTo("1234567890");
        assertThat(event.serializedPayload()).contains("TRX-20260604-000001");
    }

    private static TransactionEntity transaction() {
        CustomerEntity customer = new CustomerEntity("CIF001", "Daniel Sinaga", "daniel@example.com", "0811", "ACTIVE");
        AccountEntity sourceAccount = new AccountEntity(customer, "1234567890", "SAVINGS", "IDR", BigDecimal.TEN, "ACTIVE");
        return new TransactionEntity(
                "TRX-20260604-000001",
                "idem-001",
                sourceAccount,
                "9876543210",
                BigDecimal.valueOf(25000000),
                "IDR",
                "MOBILE_BANKING",
                "DEVICE-001",
                "36.77.88.12",
                "Jakarta",
                "PENDING_RISK_CHECK",
                Instant.parse("2026-06-04T10:00:00Z")
        );
    }
}
