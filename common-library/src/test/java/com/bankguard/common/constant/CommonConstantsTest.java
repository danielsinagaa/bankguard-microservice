package com.bankguard.common.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommonConstantsTest {

    @Test
    void shouldExposeKafkaTopicNamesFromContract() {
        assertThat(KafkaTopics.TRANSACTION_CREATED).isEqualTo("transaction.created");
        assertThat(KafkaTopics.TRANSACTION_RISK_SCORED).isEqualTo("transaction.risk-scored");
        assertThat(KafkaTopics.TRANSACTION_CREATED_DLQ).isEqualTo("transaction.created.dlq");
        assertThat(KafkaTopics.TRANSACTION_RISK_SCORED_DLQ).isEqualTo("transaction.risk-scored.dlq");
    }

    @Test
    void shouldExposeProducerNamesFromContract() {
        assertThat(ProducerName.TRANSACTION_SERVICE).isEqualTo("transaction-service");
        assertThat(ProducerName.RISK_ENGINE_SERVICE).isEqualTo("risk-engine-service");
        assertThat(ProducerName.AUDIT_SEARCH_SERVICE).isEqualTo("audit-search-service");
        assertThat(ProducerName.MASTER_DATA_SERVICE).isEqualTo("master-data-service");
    }

    @Test
    void shouldExposeRequiredEnumValues() {
        assertThat(TransactionStatus.values())
                .extracting(Enum::name)
                .containsExactly("PENDING_RISK_CHECK", "APPROVED", "REVIEW", "BLOCKED", "FAILED");
        assertThat(RiskDecision.values())
                .extracting(Enum::name)
                .containsExactly("APPROVED", "REVIEW", "BLOCKED");
        assertThat(RoleCode.values())
                .extracting(Enum::name)
                .containsExactly("ROLE_ADMIN", "ROLE_BACKOFFICE", "ROLE_FRAUD_ANALYST", "ROLE_SYSTEM");
        assertThat(Channel.values())
                .extracting(Enum::name)
                .containsExactly("MOBILE_BANKING", "INTERNET_BANKING", "ATM", "BRANCH", "BACK_OFFICE");
    }
}
