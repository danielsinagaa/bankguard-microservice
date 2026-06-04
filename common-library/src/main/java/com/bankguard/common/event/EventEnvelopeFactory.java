package com.bankguard.common.event;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class EventEnvelopeFactory {
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    public EventEnvelopeFactory(Clock clock, Supplier<UUID> uuidSupplier) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.uuidSupplier = Objects.requireNonNull(uuidSupplier, "uuidSupplier must not be null");
    }

    public static EventEnvelopeFactory system() {
        return new EventEnvelopeFactory(Clock.systemUTC(), UUID::randomUUID);
    }

    public <T> EventEnvelope<T> transactionEvent(
            String eventType,
            String aggregateId,
            String producer,
            String requestId,
            String correlationId,
            T payload
    ) {
        return new EventEnvelope<>(
                uuidSupplier.get().toString(),
                eventType,
                EventEnvelope.DEFAULT_EVENT_VERSION,
                EventEnvelope.TRANSACTION_AGGREGATE_TYPE,
                aggregateId,
                Instant.now(clock),
                producer,
                requestId,
                correlationId,
                payload
        );
    }
}
