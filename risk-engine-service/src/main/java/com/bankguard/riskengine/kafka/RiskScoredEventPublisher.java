package com.bankguard.riskengine.kafka;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.EventEnvelopeFactory;
import com.bankguard.common.event.RiskFactorPayload;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import com.bankguard.common.exception.ApiException;
import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RiskScoringResult;
import com.bankguard.riskengine.entity.TransactionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskScoredEventPublisher {
    private final KafkaTemplate<String, EventEnvelope<TransactionRiskScoredPayload>> kafkaTemplate;
    private final EventEnvelopeFactory eventEnvelopeFactory;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RiskScoredEventPublisher(
            KafkaTemplate<String, EventEnvelope<TransactionRiskScoredPayload>> kafkaTemplate,
            EventEnvelopeFactory eventEnvelopeFactory
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventEnvelopeFactory = eventEnvelopeFactory;
    }

    public PublishedRiskScoredEvent publish(TransactionEntity transaction, RiskScoringResult result, String requestId, Instant scoredAt) {
        TransactionRiskScoredPayload payload = new TransactionRiskScoredPayload(
                transaction.getTransactionRef(),
                transaction.getSourceAccount().getCustomer().getCifNumber(),
                transaction.getSourceAccount().getCustomer().getFullName(),
                transaction.getSourceAccount().getAccountNumber(),
                transaction.getDestinationAccountNumber(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getChannel(),
                transaction.getDeviceId(),
                transaction.getIpAddress(),
                transaction.getLocation(),
                result.totalScore(),
                result.decision(),
                result.riskFactors().stream().map(this::toPayload).toList(),
                transaction.getCreatedAt(),
                scoredAt
        );
        EventEnvelope<TransactionRiskScoredPayload> envelope = eventEnvelopeFactory.transactionEvent(
                EventType.TRANSACTION_RISK_SCORED.name(),
                transaction.getTransactionRef(),
                ProducerName.RISK_ENGINE_SERVICE,
                requestId,
                requestId,
                payload
        );

        try {
            kafkaTemplate.send(KafkaTopics.TRANSACTION_RISK_SCORED, transaction.getTransactionRef(), envelope).get(10, TimeUnit.SECONDS);
            return new PublishedRiskScoredEvent(envelope, objectMapper.writeValueAsString(envelope));
        } catch (Exception ex) {
            throw new ApiException(500, ErrorCode.EVENT_PUBLISH_FAILED, "Failed to publish transaction.risk-scored event");
        }
    }

    private RiskFactorPayload toPayload(RiskFactor factor) {
        return new RiskFactorPayload(factor.code(), factor.description(), factor.score(), factor.metadata());
    }
}
