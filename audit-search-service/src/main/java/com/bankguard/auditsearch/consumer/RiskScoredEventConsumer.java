package com.bankguard.auditsearch.consumer;

import com.bankguard.auditsearch.service.AuditIndexingProcessor;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.constant.RiskDecision;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import com.bankguard.common.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class RiskScoredEventConsumer {
    private static final Pattern TRANSACTION_REF_PATTERN = Pattern.compile("TRX-\\d{8}-\\d{6}");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuditIndexingProcessor auditIndexingProcessor;

    public RiskScoredEventConsumer(AuditIndexingProcessor auditIndexingProcessor) {
        this.auditIndexingProcessor = auditIndexingProcessor;
    }

    @KafkaListener(topics = KafkaTopics.TRANSACTION_RISK_SCORED, groupId = AuditIndexingProcessor.CONSUMER_GROUP)
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        EventEnvelope<TransactionRiskScoredPayload> envelope = deserialize(record.value());
        validate(record.key(), envelope);
        auditIndexingProcessor.process(envelope, record.value());
        acknowledgment.acknowledge();
    }

    private EventEnvelope<TransactionRiskScoredPayload> deserialize(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Malformed transaction.risk-scored event");
        }
    }

    private void validate(String key, EventEnvelope<TransactionRiskScoredPayload> envelope) {
        require(envelope != null, "Event envelope is required");
        requireUuid(envelope.eventId());
        require(EventType.TRANSACTION_RISK_SCORED.name().equals(envelope.eventType()), "Event type must be TRANSACTION_RISK_SCORED");
        require(EventEnvelope.DEFAULT_EVENT_VERSION.equals(envelope.eventVersion()), "Event version is unsupported");
        require(EventEnvelope.TRANSACTION_AGGREGATE_TYPE.equals(envelope.aggregateType()), "Aggregate type must be TRANSACTION");
        require(ProducerName.RISK_ENGINE_SERVICE.equals(envelope.producer()), "Producer must be risk-engine-service");
        require(envelope.payload() != null, "Event payload is required");

        TransactionRiskScoredPayload payload = envelope.payload();
        requireTransactionRef(key, "Kafka key");
        requireTransactionRef(envelope.aggregateId(), "Aggregate id");
        requireTransactionRef(payload.transactionRef(), "Payload transaction ref");
        require(key.equals(envelope.aggregateId()), "Kafka key must equal aggregate id");
        require(key.equals(payload.transactionRef()), "Kafka key must equal payload transaction ref");
        requireNotBlank(payload.customerCif(), "Customer CIF is required");
        requireNotBlank(payload.customerName(), "Customer name is required");
        requireNotBlank(payload.sourceAccountNumber(), "Source account number is required");
        requireNotBlank(payload.destinationAccountNumber(), "Destination account number is required");
        require(payload.amount() != null && payload.amount().compareTo(BigDecimal.ZERO) > 0, "Transaction amount must be greater than zero");
        require(payload.riskScore() >= 0, "Risk score must not be negative");
        requireDecision(payload.decision());
        require(payload.createdAt() != null, "Transaction createdAt is required");
        require(payload.scoredAt() != null, "Transaction scoredAt is required");
    }

    private void requireTransactionRef(String value, String fieldName) {
        requireNotBlank(value, fieldName + " is required");
        require(TRANSACTION_REF_PATTERN.matcher(value).matches(), fieldName + " has invalid format");
    }

    private void requireUuid(String value) {
        requireNotBlank(value, "Event id is required");
        try {
            UUID.fromString(value);
        } catch (RuntimeException ex) {
            throw validationError("Event id must be a valid UUID");
        }
    }

    private void requireDecision(String decision) {
        requireNotBlank(decision, "Decision is required");
        try {
            RiskDecision.valueOf(decision);
        } catch (RuntimeException ex) {
            throw validationError("Decision is invalid");
        }
    }

    private void requireNotBlank(String value, String message) {
        require(value != null && !value.isBlank(), message);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw validationError(message);
        }
    }

    private ApiException validationError(String message) {
        return new ApiException(400, ErrorCode.VALIDATION_ERROR, message);
    }
}
