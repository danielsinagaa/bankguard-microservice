package com.bankguard.transaction.repository;

import com.bankguard.transaction.entity.AccountEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    @EntityGraph(attributePaths = "customer")
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
}
