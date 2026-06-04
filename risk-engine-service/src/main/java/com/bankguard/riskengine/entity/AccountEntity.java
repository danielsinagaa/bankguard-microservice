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

@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String status;

    protected AccountEntity() {
    }

    public AccountEntity(CustomerEntity customer, String accountNumber, String status) {
        this.customer = customer;
        this.accountNumber = accountNumber;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getStatus() {
        return status;
    }
}
