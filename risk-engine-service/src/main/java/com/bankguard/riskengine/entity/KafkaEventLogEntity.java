package com.bankguard.riskengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kafka_event_logs")
public class KafkaEventLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "topic_name", nullable = false, length = 100)
    private String topicName;

    @Column(name = "consumer_group", length = 100)
    private String consumerGroup;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected KafkaEventLogEntity() {
    }

    public KafkaEventLogEntity(String eventId, String eventType, String aggregateId, String topicName, String consumerGroup, String payload, String status, Instant createdAt, Instant processedAt) {
        this(eventId, eventType, aggregateId, topicName, consumerGroup, payload, status, null, 0, createdAt, processedAt);
    }

    public KafkaEventLogEntity(
            String eventId,
            String eventType,
            String aggregateId,
            String topicName,
            String consumerGroup,
            String payload,
            String status,
            String errorMessage,
            int retryCount,
            Instant createdAt,
            Instant processedAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
        this.payload = payload;
        this.status = status;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }
}
