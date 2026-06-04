package com.bankguard.transaction.repository;

import com.bankguard.transaction.entity.TransactionEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByTransactionRef(String transactionRef);

    long countByCreatedAtBetween(Instant fromInclusive, Instant toExclusive);

    @Query("""
            select distinct t
            from TransactionEntity t
            left join fetch t.sourceAccount
            left join fetch t.riskFactors
            where t.transactionRef = :transactionRef
            """)
    Optional<TransactionEntity> findDetailByTransactionRef(@Param("transactionRef") String transactionRef);
}
