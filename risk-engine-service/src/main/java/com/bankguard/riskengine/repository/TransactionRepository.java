package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.TransactionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    @Query("""
            select t
            from TransactionEntity t
            join fetch t.sourceAccount a
            join fetch a.customer
            where t.transactionRef = :transactionRef
            """)
    Optional<TransactionEntity> findContextByTransactionRef(@Param("transactionRef") String transactionRef);
}
