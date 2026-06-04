package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.BlacklistedAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedAccountRepository extends JpaRepository<BlacklistedAccountEntity, Long> {
    boolean existsByAccountNumberAndActiveTrue(String accountNumber);
}
