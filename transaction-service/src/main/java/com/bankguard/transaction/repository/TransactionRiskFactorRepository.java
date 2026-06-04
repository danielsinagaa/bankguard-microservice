package com.bankguard.transaction.repository;

import com.bankguard.transaction.entity.TransactionRiskFactorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRiskFactorRepository extends JpaRepository<TransactionRiskFactorEntity, Long> {
}
