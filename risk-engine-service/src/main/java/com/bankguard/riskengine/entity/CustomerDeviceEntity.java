package com.bankguard.riskengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_devices")
public class CustomerDeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(nullable = false)
    private boolean trusted;

    protected CustomerDeviceEntity() {
    }

    public boolean isTrusted() {
        return trusted;
    }
}
