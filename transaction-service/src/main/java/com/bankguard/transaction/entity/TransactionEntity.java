package com.bankguard.transaction.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 50)
    private String transactionRef;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

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

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TransactionRiskFactorEntity> riskFactors = new ArrayList<>();

    protected TransactionEntity() {
    }

    public TransactionEntity(
            String transactionRef,
            String idempotencyKey,
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
        this.idempotencyKey = idempotencyKey;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<TransactionRiskFactorEntity> getRiskFactors() {
        return riskFactors;
    }
}
