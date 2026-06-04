package com.bankguard.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeFactoryTest {

    @Test
    void shouldBuildTransactionEventEnvelopeWithRequiredFields() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC);
        UUID fixedUuid = UUID.fromString("7a7f0e9b-3dd6-4f22-b23a-44e42c25a001");
        EventEnvelopeFactory factory = new EventEnvelopeFactory(fixedClock, () -> fixedUuid);
        Map<String, String> payload = Map.of("transactionRef", "TRX-20260604-000001");

        EventEnvelope<Map<String, String>> envelope = factory.transactionEvent(
                "TRANSACTION_CREATED",
                "TRX-20260604-000001",
                "transaction-service",
                "req-001",
                "corr-001",
                payload
        );

        assertThat(envelope.eventId()).isEqualTo(fixedUuid.toString());
        assertThat(envelope.eventType()).isEqualTo("TRANSACTION_CREATED");
        assertThat(envelope.eventVersion()).isEqualTo("1.0");
        assertThat(envelope.aggregateType()).isEqualTo("TRANSACTION");
        assertThat(envelope.aggregateId()).isEqualTo("TRX-20260604-000001");
        assertThat(envelope.occurredAt()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
        assertThat(envelope.producer()).isEqualTo("transaction-service");
        assertThat(envelope.requestId()).isEqualTo("req-001");
        assertThat(envelope.correlationId()).isEqualTo("corr-001");
        assertThat(envelope.payload()).isEqualTo(payload);
    }
}
