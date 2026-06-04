package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.KafkaEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaEventLogRepository extends JpaRepository<KafkaEventLogEntity, Long> {
    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}
