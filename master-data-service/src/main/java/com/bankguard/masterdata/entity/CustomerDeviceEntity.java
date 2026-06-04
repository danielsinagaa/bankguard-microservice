package com.bankguard.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

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

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(nullable = false)
    private boolean trusted;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected CustomerDeviceEntity() {
    }

    public CustomerDeviceEntity(Long customerId, String deviceId, String deviceName, boolean trusted, Instant createdAt) {
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.trusted = trusted;
        this.firstSeenAt = createdAt;
        this.lastUsedAt = createdAt;
        this.createdAt = createdAt;
    }

    public void update(String deviceName, boolean trusted, Instant updatedAt) {
        this.deviceName = deviceName;
        this.trusted = trusted;
        this.lastUsedAt = updatedAt;
        this.updatedAt = updatedAt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}
