package com.bankguard.transaction.repository;

import com.bankguard.transaction.entity.KafkaEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaEventLogRepository extends JpaRepository<KafkaEventLogEntity, Long> {
}
