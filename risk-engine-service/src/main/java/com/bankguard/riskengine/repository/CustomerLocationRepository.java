package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.CustomerLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerLocationRepository extends JpaRepository<CustomerLocationEntity, Long> {
    boolean existsByCustomerIdAndLocation(Long customerId, String location);
}
