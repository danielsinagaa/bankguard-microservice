package com.bankguard.riskengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", nullable = false, length = 50)
    private String transactionRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private AccountEntity sourceAccount;

    @Column(name = "destination_account_number", nullable = false, length = 30)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 100)
    private String location;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "risk_decision", length = 30)
    private String riskDecision;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected TransactionEntity() {
    }

    public TransactionEntity(
            String transactionRef,
            AccountEntity sourceAccount,
            String destinationAccountNumber,
            BigDecimal amount,
            String currency,
            String channel,
            String deviceId,
            String ipAddress,
            String location,
            String status,
            Instant createdAt
    ) {
        this.transactionRef = transactionRef;
        this.sourceAccount = sourceAccount;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.channel = channel;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.location = location;
        this.status = status;
        this.riskScore = 0;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public AccountEntity getSourceAccount() {
        return sourceAccount;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getChannel() {
        return channel;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getLocation() {
        return location;
    }

    public String getStatus() {
        return status;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskDecision() {
        return riskDecision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isFinalDecision() {
        return "APPROVED".equals(status) || "REVIEW".equals(status) || "BLOCKED".equals(status);
    }

    public void applyRiskResult(int riskScore, String decision, Instant updatedAt) {
        this.riskScore = riskScore;
        this.riskDecision = decision;
        this.status = decision;
        this.updatedAt = updatedAt;
    }
}
