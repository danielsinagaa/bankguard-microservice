package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.CustomerRiskProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRiskProfileRepository extends JpaRepository<CustomerRiskProfileEntity, Long> {
    Optional<CustomerRiskProfileEntity> findByCustomerId(Long customerId);
}
