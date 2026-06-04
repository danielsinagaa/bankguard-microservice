package com.bankguard.masterdata.repository;

import com.bankguard.masterdata.entity.CustomerDeviceEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerDeviceRepository extends JpaRepository<CustomerDeviceEntity, Long> {
    boolean existsByCustomerIdAndDeviceId(Long customerId, String deviceId);

    Optional<CustomerDeviceEntity> findByCustomerIdAndDeviceId(Long customerId, String deviceId);

    Page<CustomerDeviceEntity> findByCustomerId(Long customerId, Pageable pageable);
}
