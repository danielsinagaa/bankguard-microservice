CREATE TABLE kafka_event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    topic_name VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100),
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,

    CONSTRAINT uk_kafka_event_logs_event_consumer UNIQUE (event_id, consumer_group),
    CONSTRAINT chk_kafka_event_logs_status CHECK (
        status IN ('PUBLISHED', 'CONSUMED', 'FAILED', 'DLQ', 'IGNORED')
    ),
    CONSTRAINT chk_kafka_event_logs_retry_count_non_negative CHECK (retry_count >= 0)
);

CREATE INDEX idx_kafka_event_logs_event_id ON kafka_event_logs(event_id);
CREATE INDEX idx_kafka_event_logs_aggregate_id ON kafka_event_logs(aggregate_id);
CREATE INDEX idx_kafka_event_logs_topic_status ON kafka_event_logs(topic_name, status);
CREATE INDEX idx_kafka_event_logs_created_at ON kafka_event_logs(created_at);
