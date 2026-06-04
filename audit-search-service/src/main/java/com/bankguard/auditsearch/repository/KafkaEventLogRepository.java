package com.bankguard.auditsearch.repository;

import com.bankguard.auditsearch.entity.KafkaEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaEventLogRepository extends JpaRepository<KafkaEventLogEntity, Long> {
    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}
