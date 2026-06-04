package com.bankguard.common.constant;

public final class KafkaTopics {
    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String TRANSACTION_RISK_SCORED = "transaction.risk-scored";
    public static final String TRANSACTION_CREATED_DLQ = "transaction.created.dlq";
    public static final String TRANSACTION_RISK_SCORED_DLQ = "transaction.risk-scored.dlq";

    private KafkaTopics() {
    }
}
