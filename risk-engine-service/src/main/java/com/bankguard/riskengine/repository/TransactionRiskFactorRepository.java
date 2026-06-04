package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.TransactionRiskFactorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRiskFactorRepository extends JpaRepository<TransactionRiskFactorEntity, Long> {
}
