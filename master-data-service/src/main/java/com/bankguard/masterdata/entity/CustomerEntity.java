package com.bankguard.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cif_number", nullable = false, length = 30)
    private String cifNumber;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String status;

    protected CustomerEntity() {
    }

    public CustomerEntity(String cifNumber, String fullName, String status) {
        this.cifNumber = cifNumber;
        this.fullName = fullName;
        this.status = status;
    }

    public Long getId() {
        return id;
    }
}
