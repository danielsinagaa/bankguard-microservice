package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.CustomerDeviceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerDeviceRepository extends JpaRepository<CustomerDeviceEntity, Long> {
    Optional<CustomerDeviceEntity> findByCustomerIdAndDeviceId(Long customerId, String deviceId);
}
