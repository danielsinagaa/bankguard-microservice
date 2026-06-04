package com.bankguard.transaction.kafka;

import com.bankguard.common.event.EventEnvelope;
import com.bankguard.transaction.dto.event.TransactionCreatedPayload;

public record PublishedTransactionEvent(
        EventEnvelope<TransactionCreatedPayload> envelope,
        String serializedPayload
) {
}
