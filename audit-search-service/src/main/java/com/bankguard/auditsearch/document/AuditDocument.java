package com.bankguard.auditsearch.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = AuditDocument.INDEX_NAME)
public class AuditDocument {
    public static final String INDEX_NAME = "fraud-audit-events";

    @Id
    @Field(type = FieldType.Keyword)
    private String transactionRef;

    @Field(type = FieldType.Keyword)
    private String customerCif;

    @Field(type = FieldType.Text)
    private String customerName;

    @Field(type = FieldType.Keyword)
    private String sourceAccountMasked;

    @Field(type = FieldType.Keyword)
    private String destinationAccountMasked;

    @Field(type = FieldType.Double)
    private BigDecimal amount;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Keyword)
    private String channel;

    @Field(type = FieldType.Text)
    private String location;

    @Field(type = FieldType.Integer)
    private int riskScore;

    @Field(type = FieldType.Keyword)
    private String decision;

    @Field(type = FieldType.Keyword)
    private List<String> riskFactors;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant scoredAt;

    protected AuditDocument() {
    }

    public AuditDocument(
            String transactionRef,
            String customerCif,
            String customerName,
            String sourceAccountMasked,
            String destinationAccountMasked,
            BigDecimal amount,
            String currency,
            String channel,
            String location,
            int riskScore,
            String decision,
            List<String> riskFactors,
            Instant createdAt,
            Instant scoredAt
    ) {
        this.transactionRef = transactionRef;
        this.customerCif = customerCif;
        this.customerName = customerName;
        this.sourceAccountMasked = sourceAccountMasked;
        this.destinationAccountMasked = destinationAccountMasked;
        this.amount = amount;
        this.currency = currency;
        this.channel = channel;
        this.location = location;
        this.riskScore = riskScore;
        this.decision = decision;
        this.riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        this.createdAt = createdAt;
        this.scoredAt = scoredAt;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public String getCustomerCif() {
        return customerCif;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getSourceAccountMasked() {
        return sourceAccountMasked;
    }

    public String getDestinationAccountMasked() {
        return destinationAccountMasked;
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

    public String getLocation() {
        return location;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getDecision() {
        return decision;
    }

    public List<String> getRiskFactors() {
        return riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getScoredAt() {
        return scoredAt;
    }
}
