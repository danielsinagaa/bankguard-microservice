package com.bankguard.auditsearch.kafka;

import com.bankguard.auditsearch.entity.KafkaEventLogEntity;
import com.bankguard.auditsearch.repository.KafkaEventLogRepository;
import com.bankguard.auditsearch.service.AuditIndexingProcessor;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.constant.KafkaTopics;
import com.bankguard.common.event.DeadLetterError;
import com.bankguard.common.event.DeadLetterEvent;
import com.bankguard.common.exception.ApiException;
import com.bankguard.common.exception.InvalidKafkaEventException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RiskScoredDeadLetterPublisher {
    private static final Logger log = LoggerFactory.getLogger(RiskScoredDeadLetterPublisher.class);
    private static final String FAILED_SERVICE = "audit-search-service";
    private static final String UNKNOWN_EVENT_TYPE = "UNKNOWN";
    private static final String UNKNOWN_AGGREGATE_ID = "UNKNOWN";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaEventLogRepository kafkaEventLogRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RiskScoredDeadLetterPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaEventLogRepository kafkaEventLogRepository,
            Clock clock
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEventLogRepository = kafkaEventLogRepository;
        this.clock = clock;
    }

    @Transactional
    public void publish(ConsumerRecord<?, ?> record, Exception exception, int attempt) {
        Instant failedAt = Instant.now(clock);
        OriginalEvent originalEvent = originalEvent(record);
        DeadLetterEvent deadLetterEvent = new DeadLetterEvent(
                UUID.randomUUID().toString(),
                record.topic(),
                originalKey(record),
                originalEvent.value(),
                new DeadLetterError(
                        errorType(exception),
                        rootMessage(exception),
                        null,
                        FAILED_SERVICE,
                        failedAt,
                        attempt
                )
        );

        try {
            kafkaTemplate.send(KafkaTopics.TRANSACTION_RISK_SCORED_DLQ, originalKey(record), deadLetterEvent)
                    .get(10, TimeUnit.SECONDS);
            kafkaEventLogRepository.save(new KafkaEventLogEntity(
                    originalEvent.eventId(deadLetterEvent.dlqEventId()),
                    originalEvent.eventTypeOrUnknown(),
                    originalEvent.aggregateIdOr(originalKey(record)),
                    record.topic(),
                    AuditIndexingProcessor.CONSUMER_GROUP,
                    originalEvent.logPayload(),
                    "DLQ",
                    rootMessage(exception),
                    attempt,
                    failedAt,
                    failedAt
            ));
            log.warn(
                    "Kafka event sent to DLQ, topic={}, key={}, dlqTopic={}, consumerGroup={}, errorType={}, error={}",
                    record.topic(),
                    originalKey(record),
                    KafkaTopics.TRANSACTION_RISK_SCORED_DLQ,
                    AuditIndexingProcessor.CONSUMER_GROUP,
                    deadLetterEvent.error().errorType(),
                    deadLetterEvent.error().errorMessage()
            );
        } catch (Exception sendException) {
            throw new IllegalStateException("Failed to publish transaction.risk-scored DLQ event", sendException);
        }
    }

    private OriginalEvent originalEvent(ConsumerRecord<?, ?> record) {
        Object rawValue = record.value();
        if (rawValue == null) {
            return new OriginalEvent(Map.of(), "{}", null, null, null);
        }

        String payload = rawValue.toString();
        try {
            JsonNode json = objectMapper.readTree(payload);
            return new OriginalEvent(
                    json,
                    payload,
                    text(json, "eventId"),
                    text(json, "eventType"),
                    text(json, "aggregateId")
            );
        } catch (Exception ex) {
            Map<String, Object> wrapper = Map.of("rawPayload", payload);
            try {
                return new OriginalEvent(wrapper, objectMapper.writeValueAsString(wrapper), null, null, null);
            } catch (Exception serializationException) {
                return new OriginalEvent(Map.of(), "{}", null, null, null);
            }
        }
    }

    private static String originalKey(ConsumerRecord<?, ?> record) {
        return record.key() == null ? UNKNOWN_AGGREGATE_ID : record.key().toString();
    }

    private static String text(JsonNode json, String fieldName) {
        JsonNode value = json.get(fieldName);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static String errorType(Throwable throwable) {
        Throwable root = rootCause(throwable);
        if (root instanceof InvalidKafkaEventException) {
            return "VALIDATION_ERROR";
        }
        if (root instanceof ApiException apiException && apiException.getErrorCode() == ErrorCode.DEPENDENCY_UNAVAILABLE) {
            return "DEPENDENCY_UNAVAILABLE";
        }
        String className = root.getClass().getName().toLowerCase();
        if (className.contains("elasticsearch") || className.contains("kafka") || className.contains("timeout")) {
            return "DEPENDENCY_UNAVAILABLE";
        }
        return "PROCESSING_ERROR";
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private record OriginalEvent(
            Object value,
            String logPayload,
            String eventId,
            String eventType,
            String aggregateId
    ) {
        String eventId(String fallback) {
            return eventId == null ? fallback : eventId;
        }

        String eventTypeOrUnknown() {
            return eventType == null ? UNKNOWN_EVENT_TYPE : eventType;
        }

        String aggregateIdOr(String fallback) {
            if (aggregateId != null) {
                return aggregateId;
            }
            return fallback == null || fallback.isBlank() ? UNKNOWN_AGGREGATE_ID : fallback;
        }
    }
}
