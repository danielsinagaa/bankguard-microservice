# Database Schema
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the PostgreSQL database schema for **BankGuard**.

The database schema supports the core MVP capabilities defined in the Product Requirements Document, System Design and Architecture Specification, Technical Requirements Document, and API Contract.

This document provides implementation-level details for:

- Logical data ownership.
- PostgreSQL schema structure.
- Entity relationship model.
- Table definitions.
- Column definitions.
- Data types.
- Primary keys and foreign keys.
- Unique constraints.
- Check constraints.
- Indexes.
- Migration order.
- Seed data.
- Reporting query support.
- Data retention considerations.
- Database acceptance criteria.

---

### 1.2 Source Documents

This schema is aligned with:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`

---

### 1.3 Scope

This database schema covers the MVP data requirements for:

1. Transaction submission.
2. Customer and account lookup.
3. Transaction idempotency.
4. Risk scoring result persistence.
5. Risk factor explainability.
6. Blacklisted account management.
7. Trusted device management.
8. Customer risk profile management.
9. Known customer location tracking.
10. Risk rule configuration.
11. Kafka event processing logs.
12. Internal users and roles.
13. Native SQL reporting.

---

### 1.4 Non-Scope

This schema does not cover:

- Real fund movement ledger.
- Core banking account balance mutation.
- Payment gateway settlement.
- Regulatory reporting storage.
- Machine learning feature store.
- Case management workflow.
- Analyst notes and escalation workflow.
- Long-term audit archive partitioning.
- Multi-region replication strategy.

These can be introduced in later phases.

---

## 2. Database Strategy

### 2.1 Database Engine

BankGuard uses:

```text
PostgreSQL 16
```

PostgreSQL is used as the source of truth for operational and reference data.

---

### 2.2 Source of Truth Rule

PostgreSQL is the authoritative storage for:

- customers
- accounts
- transactions
- transaction risk factors
- customer risk profiles
- blacklisted accounts
- trusted devices
- known customer locations
- risk rule configurations
- internal users and roles
- Kafka event processing logs

Elasticsearch is only a denormalized search projection.

Redis is only cache and short-lived counter storage.

---

### 2.3 Schema Ownership

For MVP simplicity, all services use one PostgreSQL database. Logical ownership must still be respected.

| Data Area | Main Tables | Owning Service | Access Pattern |
|---|---|---|---|
| Customer and Account Data | `customers`, `accounts` | Transaction Service | Read/write seed data, read during transaction submission |
| Transaction Data | `transactions`, `transaction_risk_factors` | Transaction Service / Risk Engine Service | Transaction Service creates, Risk Engine updates risk result |
| Fraud Reference Data | `blacklisted_accounts`, `customer_devices`, `customer_risk_profiles`, `customer_locations`, `risk_rule_configs` | Master Data Service | Write through Master Data Service, read by Risk Engine |
| Event Processing Logs | `kafka_event_logs` | Producing and consuming services | Insert/update during event lifecycle |
| Identity and Authorization | `users`, `roles`, `user_roles` | Transaction Service for MVP | Used for JWT login and role mapping |

---

### 2.4 Naming Conventions

| Object | Convention | Example |
|---|---|---|
| Table | snake_case plural noun | `transactions` |
| Primary Key | `id` | `id BIGSERIAL PRIMARY KEY` |
| Foreign Key | `{referenced_table_singular}_id` | `customer_id` |
| Unique Constraint | `uk_{table}_{column}` | `uk_transactions_transaction_ref` |
| Foreign Key Constraint | `fk_{table}_{referenced_table}` | `fk_accounts_customers` |
| Check Constraint | `chk_{table}_{purpose}` | `chk_transactions_amount_positive` |
| Index | `idx_{table}_{columns}` | `idx_transactions_created_at` |
| Timestamp Columns | `created_at`, `updated_at` | `created_at TIMESTAMP NOT NULL DEFAULT NOW()` |

---

## 3. Entity Relationship Overview

```text
customers 1---N accounts
customers 1---1 customer_risk_profiles
customers 1---N customer_devices
customers 1---N customer_locations
accounts 1---N transactions
transactions 1---N transaction_risk_factors
users N---N roles through user_roles

blacklisted_accounts independent reference table
risk_rule_configs independent reference table
kafka_event_logs independent event trace table
```

---

## 4. Table Summary

| Table | Purpose | Owner |
|---|---|---|
| `customers` | Stores customer identity and base status | Transaction Service |
| `customer_risk_profiles` | Stores customer-level risk metadata | Master Data Service |
| `accounts` | Stores customer account records | Transaction Service |
| `transactions` | Stores submitted transactions and risk result summary | Transaction Service / Risk Engine Service |
| `transaction_risk_factors` | Stores explainable risk factors per transaction | Risk Engine Service |
| `blacklisted_accounts` | Stores risky destination accounts | Master Data Service |
| `customer_devices` | Stores trusted or known devices | Master Data Service |
| `customer_locations` | Stores known customer transaction locations | Master Data Service / Risk Engine Service |
| `risk_rule_configs` | Stores rule score, threshold, and active status | Master Data Service |
| `kafka_event_logs` | Stores event publishing and consuming trace | All event services |
| `users` | Stores internal application users | Transaction Service for MVP |
| `roles` | Stores authorization roles | Transaction Service for MVP |
| `user_roles` | Maps users to roles | Transaction Service for MVP |

---

## 5. Enum and Allowed Values

### 5.1 Customer Status

```text
ACTIVE
INACTIVE
SUSPENDED
```

---

### 5.2 Account Status

```text
ACTIVE
FROZEN
BLOCKED
CLOSED
```

---

### 5.3 Account Type

```text
SAVINGS
CURRENT
PAYROLL
```

---

### 5.4 Currency

MVP supported currency:

```text
IDR
```

---

### 5.5 Transaction Channel

```text
MOBILE_BANKING
INTERNET_BANKING
ATM
BRANCH
BACK_OFFICE
```

---

### 5.6 Transaction Status

```text
PENDING_RISK_CHECK
APPROVED
REVIEW
BLOCKED
FAILED
```

---

### 5.7 Risk Decision

```text
APPROVED
REVIEW
BLOCKED
```

---

### 5.8 Customer Risk Level

```text
LOW
MEDIUM
HIGH
```

---

### 5.9 Kafka Event Log Status

```text
PUBLISHED
CONSUMED
FAILED
DLQ
IGNORED
```

---

### 5.10 Role Code

```text
ROLE_ADMIN
ROLE_BACKOFFICE
ROLE_FRAUD_ANALYST
ROLE_SYSTEM
```

---

## 6. Full Database Schema

## 6.1 customers

### Purpose

Stores customer identity and base operational status.

This table supports:

- Source account validation.
- Transaction detail enrichment.
- Reporting by customer.
- Fraud audit search document enrichment.

### Ownership

```text
Owning Service: Transaction Service
Primary Access: Transaction submission, transaction detail, reporting
```

### DDL

```sql
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    cif_number VARCHAR(30) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone_number VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_customers_cif_number UNIQUE (cif_number),
    CONSTRAINT chk_customers_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')
    )
);
```

### Indexes

```sql
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_full_name ON customers(full_name);
```

### Column Notes

| Column | Notes |
|---|---|
| `cif_number` | Customer Information File number. Must be unique. |
| `status` | Only active customers should be used in normal transaction flows. |
| `email`, `phone_number` | Optional in MVP, useful for future notification/case management. |

---

## 6.2 customer_risk_profiles

### Purpose

Stores risk metadata for a customer. Used by the `HIGH_RISK_CUSTOMER_PROFILE` rule.

### Ownership

```text
Owning Service: Master Data Service
Primary Access: Risk Engine Service via Redis cache-aside
```

### DDL

```sql
CREATE TABLE customer_risk_profiles (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    risk_reason TEXT,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_customer_risk_profiles_customer_id UNIQUE (customer_id),
    CONSTRAINT fk_customer_risk_profiles_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT chk_customer_risk_profiles_level CHECK (
        risk_level IN ('LOW', 'MEDIUM', 'HIGH')
    )
);
```

### Indexes

```sql
CREATE INDEX idx_customer_risk_profiles_risk_level ON customer_risk_profiles(risk_level);
```

### Cache Mapping

| Redis Key | Source Table |
|---|---|
| `customer:risk-profile:{customerId}` | `customer_risk_profiles` |

---

## 6.3 accounts

### Purpose

Stores customer account data used for transaction source validation.

### Ownership

```text
Owning Service: Transaction Service
Primary Access: Transaction submission and reporting
```

### DDL

```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT chk_accounts_type CHECK (
        account_type IN ('SAVINGS', 'CURRENT', 'PAYROLL')
    ),
    CONSTRAINT chk_accounts_currency CHECK (
        currency IN ('IDR')
    ),
    CONSTRAINT chk_accounts_status CHECK (
        status IN ('ACTIVE', 'FROZEN', 'BLOCKED', 'CLOSED')
    ),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);
```

### Indexes

```sql
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_currency ON accounts(currency);
```

### Column Notes

| Column | Notes |
|---|---|
| `balance` | Stored for realism but not mutated by MVP transaction submission. |
| `status` | Must be `ACTIVE` for transaction submission. |

---

## 6.4 transactions

### Purpose

Stores transaction submissions and the risk scoring result summary.

This table supports:

- Transaction submission.
- Idempotency.
- Risk status update.
- Transaction detail API.
- Native SQL reporting.
- Audit document enrichment.

### Ownership

```text
Owning Service: Transaction Service
Risk Update Writer: Risk Engine Service
Primary Access: Transaction detail, risk scoring, reporting
```

### DDL

```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_ref VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    source_account_id BIGINT NOT NULL,
    destination_account_number VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    channel VARCHAR(30) NOT NULL,
    device_id VARCHAR(100),
    ip_address VARCHAR(50),
    location VARCHAR(100),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_RISK_CHECK',
    risk_score INT NOT NULL DEFAULT 0,
    risk_decision VARCHAR(30),
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_transactions_transaction_ref UNIQUE (transaction_ref),
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_transactions_source_account FOREIGN KEY (source_account_id)
        REFERENCES accounts(id),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_currency CHECK (currency IN ('IDR')),
    CONSTRAINT chk_transactions_channel CHECK (
        channel IN ('MOBILE_BANKING', 'INTERNET_BANKING', 'ATM', 'BRANCH', 'BACK_OFFICE')
    ),
    CONSTRAINT chk_transactions_status CHECK (
        status IN ('PENDING_RISK_CHECK', 'APPROVED', 'REVIEW', 'BLOCKED', 'FAILED')
    ),
    CONSTRAINT chk_transactions_risk_decision CHECK (
        risk_decision IS NULL OR risk_decision IN ('APPROVED', 'REVIEW', 'BLOCKED')
    ),
    CONSTRAINT chk_transactions_risk_score_range CHECK (risk_score >= 0)
);
```

### Indexes

```sql
CREATE INDEX idx_transactions_source_account_id ON transactions(source_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_risk_score ON transactions(risk_score);
CREATE INDEX idx_transactions_destination_account ON transactions(destination_account_number);
CREATE INDEX idx_transactions_channel ON transactions(channel);
CREATE INDEX idx_transactions_status_created_at ON transactions(status, created_at);
CREATE INDEX idx_transactions_risk_created_at ON transactions(risk_score, created_at);
CREATE INDEX idx_transactions_decision_created_at ON transactions(risk_decision, created_at);
CREATE INDEX idx_transactions_source_created_at ON transactions(source_account_id, created_at);
```

### Important Constraints

| Constraint | Purpose |
|---|---|
| `uk_transactions_transaction_ref` | Ensures transaction reference uniqueness. |
| `uk_transactions_idempotency_key` | Prevents duplicate transaction submission. |
| `chk_transactions_amount_positive` | Rejects invalid transaction amount at database level. |
| `chk_transactions_status` | Keeps transaction lifecycle consistent. |

### Lifecycle

```text
Transaction Service creates:
PENDING_RISK_CHECK

Risk Engine Service updates to:
APPROVED
REVIEW
BLOCKED

Transaction Service may set:
FAILED
```

---

## 6.5 transaction_risk_factors

### Purpose

Stores explainable risk factors generated by the Risk Engine.

This table supports:

- Transaction detail explainability.
- Audit trail.
- Reporting by risk factor.
- Elasticsearch audit document construction.

### Ownership

```text
Owning Service: Risk Engine Service
Primary Access: Risk Engine writes, Transaction Service reads for transaction detail
```

### DDL

```sql
CREATE TABLE transaction_risk_factors (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    factor_code VARCHAR(50) NOT NULL,
    factor_description TEXT NOT NULL,
    score INT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transaction_risk_factors_transactions FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),
    CONSTRAINT chk_transaction_risk_factors_score_non_negative CHECK (score >= 0),
    CONSTRAINT uk_transaction_risk_factors_transaction_factor UNIQUE (transaction_id, factor_code)
);
```

### Indexes

```sql
CREATE INDEX idx_risk_factors_transaction_id ON transaction_risk_factors(transaction_id);
CREATE INDEX idx_risk_factors_factor_code ON transaction_risk_factors(factor_code);
CREATE INDEX idx_risk_factors_created_at ON transaction_risk_factors(created_at);
```

### Column Notes

| Column | Notes |
|---|---|
| `metadata` | Stores rule-specific context such as threshold and actual values. |
| `uk_transaction_risk_factors_transaction_factor` | Prevents duplicate risk factor insertion during event replay. |

---

## 6.6 blacklisted_accounts

### Purpose

Stores destination account numbers considered risky.

Used by the `BLACKLISTED_DESTINATION` risk rule.

### Ownership

```text
Owning Service: Master Data Service
Primary Access: Risk Engine Service via Redis cache-aside
```

### DDL

```sql
CREATE TABLE blacklisted_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL,
    reason TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_blacklisted_accounts_account_number UNIQUE (account_number)
);
```

### Indexes

```sql
CREATE INDEX idx_blacklisted_accounts_active ON blacklisted_accounts(active);
CREATE INDEX idx_blacklisted_accounts_account_active ON blacklisted_accounts(account_number, active);
```

### Cache Mapping

| Redis Key | Source Table | Invalidation Trigger |
|---|---|---|
| `blacklist:account:{accountNumber}` | `blacklisted_accounts` | Create, update, deactivate blacklist account |

---

## 6.7 customer_devices

### Purpose

Stores customer device information and trust status.

Used by the `NEW_DEVICE` risk rule.

### Ownership

```text
Owning Service: Master Data Service
Primary Access: Risk Engine Service via Redis cache-aside
```

### DDL

```sql
CREATE TABLE customer_devices (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100),
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_devices_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT uk_customer_devices_customer_device UNIQUE (customer_id, device_id)
);
```

### Indexes

```sql
CREATE INDEX idx_customer_devices_customer_id ON customer_devices(customer_id);
CREATE INDEX idx_customer_devices_trusted ON customer_devices(trusted);
CREATE INDEX idx_customer_devices_last_used_at ON customer_devices(last_used_at);
```

### Cache Mapping

| Redis Key | Source Table | Invalidation Trigger |
|---|---|---|
| `customer:trusted-device:{customerId}:{deviceId}` | `customer_devices` | Create/update trusted device |

---

## 6.8 customer_locations

### Purpose

Stores known customer transaction locations.

Used by the `UNUSUAL_LOCATION` risk rule.

### Ownership

```text
Owning Service: Master Data Service
Risk Update Writer: Risk Engine Service may update usage metadata after successful scoring
```

### DDL

```sql
CREATE TABLE customer_locations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    location VARCHAR(100) NOT NULL,
    usage_count INT NOT NULL DEFAULT 1,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_locations_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT uk_customer_locations_customer_location UNIQUE (customer_id, location),
    CONSTRAINT chk_customer_locations_usage_count_positive CHECK (usage_count >= 1)
);
```

### Indexes

```sql
CREATE INDEX idx_customer_locations_customer_id ON customer_locations(customer_id);
CREATE INDEX idx_customer_locations_location ON customer_locations(location);
CREATE INDEX idx_customer_locations_last_used_at ON customer_locations(last_used_at);
```

---

## 6.9 risk_rule_configs

### Purpose

Stores configurable risk rule metadata such as score, threshold, and active status.

Used by Risk Engine to evaluate active fraud rules.

### Ownership

```text
Owning Service: Master Data Service
Primary Access: Risk Engine Service via Redis cache-aside
```

### DDL

```sql
CREATE TABLE risk_rule_configs (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    score INT NOT NULL,
    threshold_value NUMERIC(19,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_risk_rule_configs_rule_code UNIQUE (rule_code),
    CONSTRAINT chk_risk_rule_configs_score_non_negative CHECK (score >= 0)
);
```

### Indexes

```sql
CREATE INDEX idx_risk_rule_configs_active ON risk_rule_configs(active);
```

### Required Initial Rule Configs

| Rule Code | Default Score | Threshold Value | Active |
|---|---:|---:|---:|
| `HIGH_AMOUNT` | 25 | 10000000 | true |
| `NEW_DEVICE` | 20 | null | true |
| `BLACKLISTED_DESTINATION` | 50 | null | true |
| `HIGH_FREQUENCY_TRANSACTION_COUNT` | 30 | 5 | true |
| `HIGH_FREQUENCY_TRANSACTION_AMOUNT` | 30 | 50000000 | true |
| `UNUSUAL_LOCATION` | 20 | null | true |
| `HIGH_RISK_CUSTOMER_PROFILE` | 25 | null | true |

### Cache Mapping

| Redis Key | Source Table | Invalidation Trigger |
|---|---|---|
| `risk-rule:config:{ruleCode}` | `risk_rule_configs` | Update rule configuration |

---

## 6.10 kafka_event_logs

### Purpose

Stores event publish and consume traces for operational troubleshooting and idempotent event handling.

### Ownership

```text
Owning Services: Transaction Service, Risk Engine Service, Audit Search Service
```

### DDL

```sql
CREATE TABLE kafka_event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    topic_name VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100),
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,

    CONSTRAINT uk_kafka_event_logs_event_consumer UNIQUE (event_id, consumer_group),
    CONSTRAINT chk_kafka_event_logs_status CHECK (
        status IN ('PUBLISHED', 'CONSUMED', 'FAILED', 'DLQ', 'IGNORED')
    ),
    CONSTRAINT chk_kafka_event_logs_retry_count_non_negative CHECK (retry_count >= 0)
);
```

### Indexes

```sql
CREATE INDEX idx_kafka_event_logs_event_id ON kafka_event_logs(event_id);
CREATE INDEX idx_kafka_event_logs_aggregate_id ON kafka_event_logs(aggregate_id);
CREATE INDEX idx_kafka_event_logs_topic_status ON kafka_event_logs(topic_name, status);
CREATE INDEX idx_kafka_event_logs_created_at ON kafka_event_logs(created_at);
```

### Notes

`consumer_group` may be null for producer-side `PUBLISHED` logs.

The unique constraint `(event_id, consumer_group)` supports idempotent consumer behavior.

---

## 6.11 users

### Purpose

Stores internal users for MVP authentication.

### Ownership

```text
Owning Service: Transaction Service for MVP
```

### DDL

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_users_username UNIQUE (username)
);
```

### Indexes

```sql
CREATE INDEX idx_users_active ON users(active);
```

---

## 6.12 roles

### Purpose

Stores role definitions for authorization.

### Ownership

```text
Owning Service: Transaction Service for MVP
```

### DDL

```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,

    CONSTRAINT uk_roles_role_code UNIQUE (role_code),
    CONSTRAINT chk_roles_role_code CHECK (
        role_code IN ('ROLE_ADMIN', 'ROLE_BACKOFFICE', 'ROLE_FRAUD_ANALYST', 'ROLE_SYSTEM')
    )
);
```

---

## 6.13 user_roles

### Purpose

Maps internal users to authorization roles.

### Ownership

```text
Owning Service: Transaction Service for MVP
```

### DDL

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id)
        REFERENCES users(id),
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id)
        REFERENCES roles(id)
);
```

### Indexes

```sql
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
```

---

## 7. Migration Plan

Flyway migration files must be ordered so foreign key dependencies are created after referenced tables.

```text
V1__create_customers_table.sql
V2__create_customer_risk_profiles_table.sql
V3__create_accounts_table.sql
V4__create_transactions_table.sql
V5__create_transaction_risk_factors_table.sql
V6__create_blacklisted_accounts_table.sql
V7__create_customer_devices_table.sql
V8__create_customer_locations_table.sql
V9__create_risk_rule_configs_table.sql
V10__create_kafka_event_logs_table.sql
V11__create_users_table.sql
V12__create_roles_table.sql
V13__create_user_roles_table.sql
V14__seed_roles.sql
V15__seed_users.sql
V16__seed_customers_accounts.sql
V17__seed_risk_reference_data.sql
V18__seed_sample_transactions_optional.sql
```

---

## 8. Seed Data Requirements

## 8.1 Roles

```sql
INSERT INTO roles (role_code, role_name) VALUES
('ROLE_ADMIN', 'Administrator'),
('ROLE_BACKOFFICE', 'Back Office User'),
('ROLE_FRAUD_ANALYST', 'Fraud Analyst'),
('ROLE_SYSTEM', 'System Service');
```

---

## 8.2 Users

Passwords must be stored as BCrypt hashes.

Example users:

| Username | Role |
|---|---|
| `admin` | `ROLE_ADMIN` |
| `backoffice` | `ROLE_BACKOFFICE` |
| `analyst` | `ROLE_FRAUD_ANALYST` |

---

## 8.3 Customers

Example seed data:

```sql
INSERT INTO customers (cif_number, full_name, email, phone_number, status)
VALUES
('CIF001', 'Daniel Sinaga', 'daniel.sinaga@example.com', '081111111111', 'ACTIVE'),
('CIF002', 'Maria Lestari', 'maria.lestari@example.com', '082222222222', 'ACTIVE'),
('CIF003', 'Budi Santoso', 'budi.santoso@example.com', '083333333333', 'ACTIVE');
```

---

## 8.4 Customer Risk Profiles

```sql
INSERT INTO customer_risk_profiles (customer_id, risk_level, risk_reason)
SELECT id, 'LOW', 'Default customer profile'
FROM customers
WHERE cif_number IN ('CIF001', 'CIF002');

INSERT INTO customer_risk_profiles (customer_id, risk_level, risk_reason)
SELECT id, 'HIGH', 'Customer requires enhanced monitoring'
FROM customers
WHERE cif_number = 'CIF003';
```

---

## 8.5 Accounts

```sql
INSERT INTO accounts (customer_id, account_number, account_type, currency, balance, status)
SELECT id, '1234567890', 'SAVINGS', 'IDR', 100000000, 'ACTIVE'
FROM customers WHERE cif_number = 'CIF001';

INSERT INTO accounts (customer_id, account_number, account_type, currency, balance, status)
SELECT id, '2223334445', 'SAVINGS', 'IDR', 50000000, 'ACTIVE'
FROM customers WHERE cif_number = 'CIF002';

INSERT INTO accounts (customer_id, account_number, account_type, currency, balance, status)
SELECT id, '3334445556', 'SAVINGS', 'IDR', 75000000, 'ACTIVE'
FROM customers WHERE cif_number = 'CIF003';
```

---

## 8.6 Blacklisted Accounts

```sql
INSERT INTO blacklisted_accounts (account_number, reason, active, created_by)
VALUES
('9876543210', 'Reported mule account', true, 'system'),
('9998887776', 'Fraud investigation watchlist', true, 'system');
```

---

## 8.7 Customer Devices

```sql
INSERT INTO customer_devices (customer_id, device_id, device_name, trusted)
SELECT id, 'IPHONE-15-DEVICE-001', 'Primary iPhone', true
FROM customers WHERE cif_number = 'CIF001';
```

---

## 8.8 Customer Locations

```sql
INSERT INTO customer_locations (customer_id, location, usage_count)
SELECT id, 'Jakarta', 10
FROM customers WHERE cif_number = 'CIF001';

INSERT INTO customer_locations (customer_id, location, usage_count)
SELECT id, 'Batam', 3
FROM customers WHERE cif_number = 'CIF002';
```

---

## 8.9 Risk Rule Configurations

```sql
INSERT INTO risk_rule_configs (rule_code, rule_name, score, threshold_value, active, description)
VALUES
('HIGH_AMOUNT', 'High Amount Transaction', 25, 10000000, true, 'Triggered when transaction amount exceeds threshold'),
('NEW_DEVICE', 'New or Untrusted Device', 20, NULL, true, 'Triggered when device is not trusted'),
('BLACKLISTED_DESTINATION', 'Blacklisted Destination Account', 50, NULL, true, 'Triggered when destination account is blacklisted'),
('HIGH_FREQUENCY_TRANSACTION_COUNT', 'High Frequency Transaction Count', 30, 5, true, 'Triggered when transaction count exceeds threshold within 10 minutes'),
('HIGH_FREQUENCY_TRANSACTION_AMOUNT', 'High Frequency Transaction Amount', 30, 50000000, true, 'Triggered when accumulated transaction amount exceeds threshold within 10 minutes'),
('UNUSUAL_LOCATION', 'Unusual Transaction Location', 20, NULL, true, 'Triggered when transaction location is not known for customer'),
('HIGH_RISK_CUSTOMER_PROFILE', 'High Risk Customer Profile', 25, NULL, true, 'Triggered when customer risk profile is HIGH');
```

---

## 9. Reporting Query Support

The schema supports the reporting APIs defined in the API Contract.

## 9.1 High-Risk Transactions

Supported by:

```text
transactions.risk_score
transactions.created_at
transactions.source_account_id
accounts.customer_id
customers.cif_number
```

Recommended indexes:

```sql
idx_transactions_risk_created_at
idx_transactions_source_account_id
idx_accounts_customer_id
```

---

## 9.2 Transaction Velocity by Account

Supported by:

```text
transactions.source_account_id
transactions.amount
transactions.created_at
```

Recommended indexes:

```sql
idx_transactions_source_created_at
```

---

## 9.3 Top Risk Customers

Supported by:

```text
transactions.risk_score
transactions.amount
transactions.created_at
accounts.customer_id
customers.cif_number
```

Recommended indexes:

```sql
idx_transactions_created_at
idx_accounts_customer_id
```

---

## 9.4 Suspicious Destination Accounts

Supported by:

```text
transactions.destination_account_number
transactions.source_account_id
transactions.amount
transactions.created_at
```

Recommended indexes:

```sql
idx_transactions_destination_account
idx_transactions_created_at
```

---

## 9.5 Daily Fraud Trend

Supported by:

```text
transactions.created_at
transactions.risk_decision
transactions.risk_score
transactions.amount
```

Recommended indexes:

```sql
idx_transactions_decision_created_at
```

---

## 9.6 Risk Score Distribution

Supported by:

```text
transactions.risk_score
transactions.created_at
```

Recommended indexes:

```sql
idx_transactions_risk_created_at
```

---

## 9.7 Daily Top Risky Customers

Supported by:

```text
transactions.created_at
transactions.risk_score
transactions.amount
accounts.customer_id
customers.cif_number
```

Recommended indexes:

```sql
idx_transactions_created_at
idx_accounts_customer_id
```

---

## 10. Query Catalog

## 10.1 High-Risk Transactions

```sql
SELECT
    t.transaction_ref,
    c.cif_number,
    c.full_name,
    a.account_number AS source_account,
    t.destination_account_number,
    t.amount,
    t.risk_score,
    t.risk_decision,
    t.status,
    t.created_at
FROM transactions t
JOIN accounts a ON t.source_account_id = a.id
JOIN customers c ON a.customer_id = c.id
WHERE t.risk_score >= :minimumRiskScore
  AND t.created_at BETWEEN :startDate AND :endDate
ORDER BY t.risk_score DESC, t.amount DESC;
```

---

## 10.2 Transaction Velocity by Account

```sql
SELECT
    a.account_number,
    COUNT(t.id) AS transaction_count,
    SUM(t.amount) AS total_amount,
    MAX(t.risk_score) AS max_risk_score
FROM transactions t
JOIN accounts a ON t.source_account_id = a.id
WHERE t.created_at >= NOW() - INTERVAL '10 minutes'
GROUP BY a.account_number
HAVING COUNT(t.id) >= :minimumCount
    OR SUM(t.amount) >= :minimumAmount
ORDER BY transaction_count DESC, total_amount DESC;
```

---

## 10.3 Top Risk Customers

```sql
SELECT
    c.cif_number,
    c.full_name,
    COUNT(t.id) AS total_transactions,
    AVG(t.risk_score) AS average_risk_score,
    MAX(t.risk_score) AS highest_risk_score,
    SUM(t.amount) AS total_amount
FROM transactions t
JOIN accounts a ON t.source_account_id = a.id
JOIN customers c ON a.customer_id = c.id
WHERE t.created_at BETWEEN :startDate AND :endDate
GROUP BY c.cif_number, c.full_name
HAVING AVG(t.risk_score) >= :minimumAverageRiskScore
ORDER BY average_risk_score DESC;
```

---

## 10.4 Suspicious Destination Accounts

```sql
SELECT
    t.destination_account_number,
    COUNT(t.id) AS total_received,
    COUNT(DISTINCT t.source_account_id) AS unique_senders,
    SUM(t.amount) AS total_amount,
    AVG(t.risk_score) AS average_risk_score
FROM transactions t
WHERE t.created_at BETWEEN :startDate AND :endDate
GROUP BY t.destination_account_number
HAVING COUNT(DISTINCT t.source_account_id) >= :minimumUniqueSenders
   AND SUM(t.amount) >= :minimumTotalAmount
ORDER BY total_amount DESC;
```

---

## 10.5 Daily Fraud Trend

```sql
SELECT
    DATE(t.created_at) AS transaction_date,
    COUNT(*) AS total_transactions,
    COUNT(*) FILTER (WHERE t.risk_decision = 'APPROVED') AS approved_count,
    COUNT(*) FILTER (WHERE t.risk_decision = 'REVIEW') AS review_count,
    COUNT(*) FILTER (WHERE t.risk_decision = 'BLOCKED') AS blocked_count,
    AVG(t.risk_score) AS average_risk_score,
    SUM(t.amount) AS total_amount
FROM transactions t
WHERE t.created_at BETWEEN :startDate AND :endDate
GROUP BY DATE(t.created_at)
ORDER BY transaction_date ASC;
```

---

## 10.6 Risk Score Distribution

```sql
SELECT
    CASE
        WHEN risk_score BETWEEN 0 AND 49 THEN 'LOW'
        WHEN risk_score BETWEEN 50 AND 79 THEN 'MEDIUM'
        ELSE 'HIGH'
    END AS risk_bucket,
    COUNT(*) AS total_transactions,
    MIN(risk_score) AS minimum_score,
    MAX(risk_score) AS maximum_score,
    AVG(risk_score) AS average_score
FROM transactions
WHERE created_at BETWEEN :startDate AND :endDate
GROUP BY risk_bucket
ORDER BY minimum_score;
```

---

## 10.7 Daily Top Risky Customers Using CTE and Window Function

```sql
WITH customer_daily_stats AS (
    SELECT
        c.id AS customer_id,
        c.cif_number,
        c.full_name,
        DATE(t.created_at) AS transaction_date,
        COUNT(t.id) AS total_transactions,
        SUM(t.amount) AS total_amount,
        AVG(t.risk_score) AS average_risk_score,
        MAX(t.risk_score) AS highest_risk_score
    FROM transactions t
    JOIN accounts a ON t.source_account_id = a.id
    JOIN customers c ON a.customer_id = c.id
    WHERE t.created_at BETWEEN :startDate AND :endDate
    GROUP BY c.id, c.cif_number, c.full_name, DATE(t.created_at)
),
ranked_customers AS (
    SELECT
        *,
        RANK() OVER (
            PARTITION BY transaction_date
            ORDER BY average_risk_score DESC, total_amount DESC
        ) AS risk_rank
    FROM customer_daily_stats
)
SELECT
    transaction_date,
    risk_rank,
    cif_number,
    full_name,
    total_transactions,
    total_amount,
    average_risk_score,
    highest_risk_score
FROM ranked_customers
WHERE risk_rank <= :topN
ORDER BY transaction_date DESC, risk_rank ASC;
```

---

## 11. Data Integrity Rules

### 11.1 Transaction Idempotency

The database must enforce uniqueness on:

```text
transactions.idempotency_key
```

This protects against duplicate transaction creation caused by retry or concurrent requests.

---

### 11.2 Transaction Reference Uniqueness

The database must enforce uniqueness on:

```text
transactions.transaction_ref
```

This supports transaction lookup, Kafka aggregate ID, Elasticsearch document ID, and audit traceability.

---

### 11.3 Risk Factor Uniqueness

The database must enforce uniqueness on:

```text
transaction_risk_factors(transaction_id, factor_code)
```

This prevents duplicate risk factors during event reprocessing.

---

### 11.4 Event Consumer Idempotency

The database must enforce uniqueness on:

```text
kafka_event_logs(event_id, consumer_group)
```

This helps prevent duplicate consumer side effects.

---

## 12. Data Masking Rules

Database values are stored in raw form for internal processing.

Masking must be applied at:

- API response layer.
- Elasticsearch document builder.
- Log output where account numbers may appear.

Masking rule:

```text
If account number length >= 7:
    keep first 3 characters
    keep last 3 characters
    replace middle characters with ****

If account number length < 7:
    mask all except last 2 characters
```

Example:

```text
1234567890 -> 123****890
```

---

## 13. Retention and Cleanup Considerations

For MVP, no automatic deletion is required.

Recommended future retention policy:

| Data | Suggested Retention |
|---|---:|
| `transactions` | 1 - 5 years depending on policy |
| `transaction_risk_factors` | Same as transactions |
| `kafka_event_logs` | 30 - 90 days |
| `blacklisted_accounts` | Until manually deactivated |
| `customer_devices` | Until manually removed or marked untrusted |
| `customer_locations` | Based on activity and review policy |

---

## 14. Database Acceptance Criteria

The database schema is considered complete when:

1. All tables can be created using Flyway from a clean PostgreSQL database.
2. All foreign key relationships are valid.
3. All unique constraints are enforced.
4. All check constraints reject invalid enum/status values.
5. Seed users, roles, customers, accounts, blacklist data, trusted device data, known location data, and rule configs can be inserted successfully.
6. Transaction submission can insert exactly one transaction per idempotency key.
7. Risk Engine can update transaction risk result and insert risk factors.
8. Duplicate risk factors are rejected by database constraint.
9. Kafka event logs support producer and consumer event tracing.
10. Native SQL reporting queries can run successfully against seeded data.
11. Required indexes exist for transaction detail, reporting, and risk lookup queries.
12. The schema supports all MVP API responses defined in `04_API_CONTRACT.md`.

---

## 15. Traceability Matrix

| PRD / API Capability | Tables Used |
|---|---|
| Transaction Submission | `accounts`, `transactions`, `kafka_event_logs` |
| Transaction Detail | `transactions`, `accounts`, `customers`, `transaction_risk_factors` |
| Risk Scoring | `transactions`, `transaction_risk_factors`, `blacklisted_accounts`, `customer_devices`, `customer_locations`, `customer_risk_profiles`, `risk_rule_configs` |
| Event Processing | `kafka_event_logs` |
| Redis Cache Source Data | `blacklisted_accounts`, `customer_devices`, `customer_risk_profiles`, `risk_rule_configs` |
| Elasticsearch Audit Document Source | `transactions`, `transaction_risk_factors`, `accounts`, `customers` |
| Blacklist Management API | `blacklisted_accounts` |
| Trusted Device API | `customer_devices` |
| Customer Risk Profile API | `customer_risk_profiles` |
| Risk Rule Config API | `risk_rule_configs` |
| Authentication API | `users`, `roles`, `user_roles` |
| Native SQL Reporting | `transactions`, `accounts`, `customers`, `transaction_risk_factors` |

