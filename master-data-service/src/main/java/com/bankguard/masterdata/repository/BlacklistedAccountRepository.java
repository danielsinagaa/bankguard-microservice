package com.bankguard.masterdata.repository;

import com.bankguard.masterdata.entity.BlacklistedAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlacklistedAccountRepository extends JpaRepository<BlacklistedAccountEntity, Long> {
    boolean existsByAccountNumber(String accountNumber);

    @Query("""
            select account
            from BlacklistedAccountEntity account
            where (:active is null or account.active = :active)
              and (:accountNumber is null or lower(account.accountNumber) like lower(concat('%', :accountNumber, '%')))
            """)
    Page<BlacklistedAccountEntity> search(
            @Param("active") Boolean active,
            @Param("accountNumber") String accountNumber,
            Pageable pageable
    );
}
