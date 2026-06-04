package com.bankguard.transaction.kafka;

import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionCreatedPayload;

public record PublishedTransactionEvent(
        EventEnvelope<TransactionCreatedPayload> envelope,
        String serializedPayload
) {
}
