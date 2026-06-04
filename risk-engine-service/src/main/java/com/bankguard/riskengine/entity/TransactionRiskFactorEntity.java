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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transaction_risk_factors")
public class TransactionRiskFactorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionEntity transaction;

    @Column(name = "factor_code", nullable = false, length = 50)
    private String factorCode;

    @Column(name = "factor_description", nullable = false)
    private String factorDescription;

    @Column(nullable = false)
    private int score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TransactionRiskFactorEntity() {
    }

    public TransactionRiskFactorEntity(TransactionEntity transaction, String factorCode, String factorDescription, int score, String metadata, Instant createdAt) {
        this.transaction = transaction;
        this.factorCode = factorCode;
        this.factorDescription = factorDescription;
        this.score = score;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }
}
