package com.bankguard.riskengine.kafka;

import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionRiskScoredPayload;

public record PublishedRiskScoredEvent(
        EventEnvelope<TransactionRiskScoredPayload> envelope,
        String serializedPayload
) {
}
