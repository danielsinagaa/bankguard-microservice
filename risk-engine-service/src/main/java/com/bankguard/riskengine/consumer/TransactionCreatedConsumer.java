package com.bankguard.riskengine.consumer;

import com.bankguard.common.constant.Channel;
import com.bankguard.common.constant.EventType;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.constant.ProducerName;
import com.bankguard.common.event.EventEnvelope;
import com.bankguard.common.event.TransactionCreatedPayload;
import com.bankguard.common.exception.InvalidKafkaEventException;
import com.bankguard.riskengine.service.RiskEngineProcessor;
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
public class TransactionCreatedConsumer {
    private static final Pattern TRANSACTION_REF_PATTERN = Pattern.compile("TRX-\\d{8}-\\d{6}");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RiskEngineProcessor riskEngineProcessor;

    public TransactionCreatedConsumer(RiskEngineProcessor riskEngineProcessor) {
        this.riskEngineProcessor = riskEngineProcessor;
    }

    @KafkaListener(topics = KafkaTopics.TRANSACTION_CREATED, groupId = RiskEngineProcessor.CONSUMER_GROUP)
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        EventEnvelope<TransactionCreatedPayload> envelope = deserialize(record.value());
        validate(record.key(), envelope);
        riskEngineProcessor.process(envelope, record.value());
        acknowledgment.acknowledge();
    }

    private EventEnvelope<TransactionCreatedPayload> deserialize(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new InvalidKafkaEventException("Malformed transaction.created event");
        }
    }

    private void validate(String key, EventEnvelope<TransactionCreatedPayload> envelope) {
        require(envelope != null, "Event envelope is required");
        requireUuid(envelope.eventId());
        require(EventType.TRANSACTION_CREATED.name().equals(envelope.eventType()), "Event type must be TRANSACTION_CREATED");
        require(EventEnvelope.DEFAULT_EVENT_VERSION.equals(envelope.eventVersion()), "Event version is unsupported");
        require(EventEnvelope.TRANSACTION_AGGREGATE_TYPE.equals(envelope.aggregateType()), "Aggregate type must be TRANSACTION");
        require(ProducerName.TRANSACTION_SERVICE.equals(envelope.producer()), "Producer must be transaction-service");
        require(envelope.payload() != null, "Event payload is required");

        TransactionCreatedPayload payload = envelope.payload();
        requireTransactionRef(key, "Kafka key");
        requireTransactionRef(envelope.aggregateId(), "Aggregate id");
        requireTransactionRef(payload.transactionRef(), "Payload transaction ref");
        require(key.equals(envelope.aggregateId()), "Kafka key must equal aggregate id");
        require(key.equals(payload.transactionRef()), "Kafka key must equal payload transaction ref");
        requireNotBlank(payload.sourceAccountNumber(), "Source account number is required");
        requireNotBlank(payload.destinationAccountNumber(), "Destination account number is required");
        require(payload.amount() != null && payload.amount().compareTo(BigDecimal.ZERO) > 0, "Transaction amount must be greater than zero");
        require("IDR".equals(payload.currency()), "Currency must be IDR");
        requireValidChannel(payload.channel());
        require(payload.createdAt() != null, "Transaction createdAt is required");
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

    private void requireValidChannel(String channel) {
        requireNotBlank(channel, "Channel is required");
        try {
            Channel.valueOf(channel);
        } catch (RuntimeException ex) {
            throw validationError("Channel is invalid");
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

    private InvalidKafkaEventException validationError(String message) {
        return new InvalidKafkaEventException(message);
    }
}
