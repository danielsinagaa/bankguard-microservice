package com.bankguard.riskengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blacklisted_accounts")
public class BlacklistedAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false)
    private boolean active;

    protected BlacklistedAccountEntity() {
    }
}
