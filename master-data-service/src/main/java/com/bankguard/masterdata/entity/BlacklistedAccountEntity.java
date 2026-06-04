package com.bankguard.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "blacklisted_accounts")
public class BlacklistedAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected BlacklistedAccountEntity() {
    }

    public BlacklistedAccountEntity(String accountNumber, String reason, String createdBy, Instant createdAt) {
        this.accountNumber = accountNumber;
        this.reason = reason;
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = createdAt;
    }

    public void update(String reason, boolean active, Instant updatedAt) {
        this.reason = reason;
        this.active = active;
        this.updatedAt = updatedAt;
    }

    public void deactivate(Instant updatedAt) {
        this.active = false;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getReason() {
        return reason;
    }

    public boolean isActive() {
        return active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
