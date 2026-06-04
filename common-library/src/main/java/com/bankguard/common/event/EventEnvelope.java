package com.bankguard.common.event;

import java.time.Instant;

public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String eventVersion,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String producer,
        String requestId,
        String correlationId,
        T payload
) {
    public static final String DEFAULT_EVENT_VERSION = "1.0";
    public static final String TRANSACTION_AGGREGATE_TYPE = "TRANSACTION";
}
