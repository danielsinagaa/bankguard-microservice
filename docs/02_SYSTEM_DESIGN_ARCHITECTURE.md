# System Design and Architecture Specification
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the technical architecture, service boundaries, data model, API contracts, event flows, security design, caching strategy, search model, reporting queries, deployment structure, and operational considerations for **BankGuard**.

BankGuard is designed to process transaction submissions, evaluate fraud risk, generate explainable risk decisions, index audit data for investigation, and provide reporting APIs for transaction risk analysis.

This specification is aligned with the BankGuard Product Requirements Document and covers the MVP capabilities described in the product scope.

### 1.2 Scope

This document covers:

- Application architecture.
- Service decomposition.
- Data ownership and data model.
- API contracts.
- Kafka event design.
- Redis cache and real-time counter design.
- Elasticsearch audit search design.
- Native SQL reporting design.
- Security and authorization.
- Idempotency.
- Reliability and failure handling.
- Containerized deployment.
- Testing strategy.
- Observability.

### 1.3 System Objectives

BankGuard must support the following technical objectives:

- Accept transaction submissions through secure APIs.
- Store transaction records in PostgreSQL as the source of truth.
- Process risk scoring asynchronously through Kafka.
- Evaluate transactions using configurable fraud rules.
- Store risk factors for explainability and audit.
- Use Redis for cacheable risk reference data and short-lived velocity counters.
- Use Elasticsearch as a denormalized read model for investigation search.
- Provide native SQL reporting APIs for fraud and transaction risk analysis.
- Support retry and dead-letter handling for failed events.
- Run the complete platform through Docker Compose.

---

## 2. Architecture Overview

### 2.1 High-Level Architecture

```text
+-------------------------+
| Client / Internal Tool  |
| Swagger / Postman       |
+-----------+-------------+
            |
            | HTTPS + JWT
            v
+-------------------------+
| Transaction Service     |
| - Submit transaction    |
| - Transaction detail    |
| - Reporting APIs        |
+-----------+-------------+
            |
            | PostgreSQL write
            v
+-------------------------+
| PostgreSQL              |
| Source of Truth         |
+-------------------------+

Transaction Service
        |
        | publish transaction.created
        v
+-------------------------+
| Kafka                   |
| Event Streaming         |
+-----------+-------------+
            |
            | consume transaction.created
            v
+-------------------------+
| Risk Engine Service     |
| - Fraud rules           |
| - Risk scoring          |
| - Risk decision         |
| - Risk factors          |
+-----+---------+---------+
      |         |
      |         | Redis lookup / counter
      |         v
      |   +---------------------+
      |   | Redis               |
      |   | Cache + Counters    |
      |   +---------------------+
      |
      | PostgreSQL update
      v
+-------------------------+
| PostgreSQL              |
| Transaction Risk Result |
+-------------------------+

Risk Engine Service
        |
        | publish transaction.risk-scored
        v
+-------------------------+
| Kafka                   |
+-----------+-------------+
            |
            | consume transaction.risk-scored
            v
+-------------------------+
| Audit Search Service    |
| - Build audit document  |
| - Mask sensitive fields |
| - Search API            |
+-----------+-------------+
            |
            | index document
            v
+-------------------------+
| Elasticsearch           |
| Investigation Read Model|
+-------------------------+

+-------------------------+
| Master Data Service     |
| - Blacklist management  |
| - Trusted device data   |
| - Risk profile data     |
| - Cache invalidation    |
+-----------+-------------+
            |
            | PostgreSQL + Redis invalidation
            v
+-------------------------+
| PostgreSQL / Redis      |
+-------------------------+
```

### 2.2 Architectural Style

BankGuard uses an event-driven microservice architecture.

The main design decisions are:

1. **Transaction ingestion is separated from risk scoring.**  
   Transaction submission stores the transaction and publishes an event. Risk scoring is processed asynchronously.

2. **PostgreSQL is the system of record.**  
   Customer, account, transaction, risk factor, and master data records are stored in PostgreSQL.

3. **Kafka is used for asynchronous processing.**  
   Kafka connects transaction creation, risk scoring, and audit indexing.

4. **Redis is used for frequently accessed risk data and short-lived counters.**  
   Redis reduces database reads and supports fast velocity-based fraud checks.

5. **Elasticsearch is used for investigation search.**  
   Elasticsearch stores a denormalized audit document optimized for search and filtering.

6. **Native SQL is used for analytical reporting.**  
   Reporting APIs use native SQL for complex joins, aggregations, CTEs, and window functions.

---

## 3. Service Architecture

## 3.1 Transaction Service

### 3.1.1 Responsibility

Transaction Service is responsible for the initial transaction lifecycle.

It handles:

- Transaction submission.
- Request validation.
- Source account validation.
- Idempotency validation.
- Transaction reference generation.
- Initial transaction persistence.
- Publishing `transaction.created` events.
- Transaction detail retrieval.
- Native SQL reporting APIs.

### 3.1.2 Main Capabilities

| Capability | Description |
|---|---|
| Submit Transaction | Accepts transaction requests from internal clients. |
| Validate Account | Ensures the source account exists and is active. |
| Idempotency Handling | Prevents duplicate transaction creation. |
| Publish Transaction Event | Publishes event only after the transaction is persisted. |
| Query Transaction Detail | Returns transaction state, risk decision, and risk factors. |
| Reporting | Provides risk reports using native SQL. |

### 3.1.3 Data Ownership

Transaction Service owns the following logical data areas:

- Customer records.
- Account records.
- Transaction records.
- Transaction risk factor reads for transaction detail.
- Reporting read queries over transaction-related data.

For the MVP deployment, all services use one PostgreSQL instance for operational simplicity. Logical ownership is still maintained by service responsibility.

### 3.1.4 Success Conditions

Transaction Service is considered operational when:

- A valid transaction request creates exactly one transaction.
- A duplicate request with the same idempotency key does not create a new transaction.
- Invalid requests return structured validation errors.
- A `transaction.created` event is published after successful persistence.
- Transaction detail can be retrieved using transaction reference.
- Reporting endpoints return correct aggregated data.

---

## 3.2 Risk Engine Service

### 3.2.1 Responsibility

Risk Engine Service evaluates transaction risk.

It handles:

- Consuming `transaction.created` events.
- Loading transaction context.
- Reading customer, account, device, location, blacklist, and rule configuration data.
- Using Redis cache before database fallback for cacheable reference data.
- Updating Redis velocity counters.
- Executing fraud rules.
- Calculating total risk score.
- Determining risk decision.
- Storing triggered risk factors.
- Updating transaction risk status.
- Publishing `transaction.risk-scored` events.

### 3.2.2 Rule Execution Model

Each fraud rule implements a common contract:

```java
public interface RiskRule {
    RiskFactor evaluate(TransactionContext context);
}
```

The Risk Engine receives all active rule implementations through dependency injection:

```java
private final List<RiskRule> riskRules;
```

This allows the scoring workflow to execute all configured rules without hardcoding each rule inside the orchestration logic.

### 3.2.3 Java Stream Processing

Risk rules are evaluated through a rule pipeline:

```java
List<RiskFactor> triggeredFactors = riskRules.stream()
    .map(rule -> rule.evaluate(context))
    .filter(RiskFactor::triggered)
    .toList();

int totalScore = triggeredFactors.stream()
    .mapToInt(RiskFactor::score)
    .sum();
```

### 3.2.4 Initial Rules

| Rule Code | Description | Default Score |
|---|---|---:|
| HIGH_AMOUNT | Transaction amount exceeds configured threshold. | 25 |
| NEW_DEVICE | Device is not trusted for the customer. | 20 |
| BLACKLISTED_DESTINATION | Destination account is blacklisted. | 50 |
| HIGH_FREQUENCY_TRANSACTION | Source account has high transaction count in short window. | 30 |
| UNUSUAL_LOCATION | Transaction location is not known for the customer. | 20 |
| HIGH_RISK_CUSTOMER_PROFILE | Customer profile has elevated risk level. | 25 |

### 3.2.5 Decision Threshold

| Risk Score Range | Decision |
|---:|---|
| 0 - 49 | APPROVED |
| 50 - 79 | REVIEW |
| 80 - 100+ | BLOCKED |

### 3.2.6 Success Conditions

Risk Engine Service is considered operational when:

- It consumes `transaction.created` events.
- It evaluates all active rules.
- It stores all triggered risk factors.
- It updates transaction risk score and decision.
- It publishes `transaction.risk-scored` events.
- It uses Redis for cacheable risk data.
- It updates Redis velocity counters.
- Failed messages are retried and moved to DLQ after retry exhaustion.

---

## 3.3 Audit Search Service

### 3.3.1 Responsibility

Audit Search Service provides searchable fraud investigation data.

It handles:

- Consuming `transaction.risk-scored` events.
- Building denormalized audit documents.
- Masking sensitive account data.
- Indexing documents into Elasticsearch.
- Providing search APIs for fraud investigation.
- Supporting keyword search and structured filters.

### 3.3.2 Data Model Strategy

PostgreSQL remains the source of truth. Elasticsearch stores a denormalized projection optimized for investigation queries.

The audit document is not used as the authoritative transaction state. If exact latest state is required, clients should retrieve transaction detail from Transaction Service.

### 3.3.3 Success Conditions

Audit Search Service is considered operational when:

- Every scored transaction produces one searchable audit document.
- Account numbers are masked before indexing.
- Audit search supports keyword search.
- Audit search supports structured filters.
- Search does not directly query primary transaction tables.

---

## 3.4 Master Data Service

### 3.4.1 Responsibility

Master Data Service manages fraud reference data.

It handles:

- Blacklisted account management.
- Trusted device management.
- Customer risk profile management.
- Risk rule configuration reads and updates.
- Redis cache invalidation after master data changes.

### 3.4.2 Managed Data

| Data Type | Purpose |
|---|---|
| Blacklisted Account | Used to detect risky destination accounts. |
| Trusted Device | Used to detect new or untrusted device usage. |
| Customer Risk Profile | Used as a risk signal during scoring. |
| Risk Rule Configuration | Used to configure thresholds, scores, and active status. |

### 3.4.3 Cache Invalidation

When master data changes, related Redis keys are deleted so the next scoring operation reloads fresh data from PostgreSQL.

Examples:

```text
DELETE blacklist:account:{accountNumber}
DELETE customer:trusted-device:{customerId}:{deviceId}
DELETE customer:risk-profile:{customerId}
DELETE risk-rule:config:{ruleCode}
```

### 3.4.4 Success Conditions

Master Data Service is considered operational when:

- Blacklisted accounts can be created, listed, updated, and deactivated.
- Trusted device records can be created and updated.
- Customer risk profiles can be updated.
- Risk rule configurations can be retrieved.
- Cache invalidation occurs after relevant data changes.

---

## 4. Data Architecture

### 4.1 Database Strategy

BankGuard uses PostgreSQL as the source of truth for operational data.

PostgreSQL stores:

- Customers.
- Accounts.
- Transactions.
- Risk factors.
- Blacklisted accounts.
- Customer devices.
- Customer locations.
- Rule configurations.
- Event processing logs.
- Internal users and roles.

Elasticsearch stores searchable audit projections.

Redis stores temporary and frequently accessed data.

### 4.2 Entity Relationship Overview

```text
customers 1---N accounts
accounts 1---N transactions
transactions 1---N transaction_risk_factors
customers 1---N customer_devices
customers 1---N customer_locations
customers 1---1 customer_risk_profiles
risk_rule_configs independent
blacklisted_accounts independent
kafka_event_logs independent
users N---N roles
```

---

## 4.3 PostgreSQL Tables

### 4.3.1 customers

Stores bank customer identity and base status.

```sql
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    cif_number VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone_number VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

Recommended indexes:

```sql
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_full_name ON customers(full_name);
```

---

### 4.3.2 customer_risk_profiles

Stores customer-level risk metadata.

```sql
CREATE TABLE customer_risk_profiles (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE REFERENCES customers(id),
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    risk_reason TEXT,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

Allowed risk levels:

```text
LOW
MEDIUM
HIGH
```

---

### 4.3.3 accounts

Stores customer accounts.

```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    account_number VARCHAR(30) NOT NULL UNIQUE,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

Recommended indexes:

```sql
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(status);
```

---

### 4.3.4 transactions

Stores transaction records and risk result summary.

```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_ref VARCHAR(50) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    source_account_id BIGINT NOT NULL REFERENCES accounts(id),
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
    updated_at TIMESTAMP
);
```

Allowed transaction statuses:

```text
PENDING_RISK_CHECK
APPROVED
REVIEW
BLOCKED
FAILED
```

Recommended indexes:

```sql
CREATE INDEX idx_transactions_source_account_id ON transactions(source_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_risk_score ON transactions(risk_score);
CREATE INDEX idx_transactions_destination_account ON transactions(destination_account_number);
CREATE INDEX idx_transactions_status_created_at ON transactions(status, created_at);
CREATE INDEX idx_transactions_risk_created_at ON transactions(risk_score, created_at);
```

---

### 4.3.5 transaction_risk_factors

Stores explainable risk reasons for each transaction.

```sql
CREATE TABLE transaction_risk_factors (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    factor_code VARCHAR(50) NOT NULL,
    factor_description TEXT NOT NULL,
    score INT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Recommended indexes:

```sql
CREATE INDEX idx_risk_factors_transaction_id ON transaction_risk_factors(transaction_id);
CREATE INDEX idx_risk_factors_factor_code ON transaction_risk_factors(factor_code);
```

---

### 4.3.6 blacklisted_accounts

Stores risky destination accounts.

```sql
CREATE TABLE blacklisted_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    reason TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

Recommended indexes:

```sql
CREATE INDEX idx_blacklisted_accounts_active ON blacklisted_accounts(active);
CREATE INDEX idx_blacklisted_accounts_account_active ON blacklisted_accounts(account_number, active);
```

---

### 4.3.7 customer_devices

Stores customer device trust data.

```sql
CREATE TABLE customer_devices (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100),
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    UNIQUE(customer_id, device_id)
);
```

Recommended indexes:

```sql
CREATE INDEX idx_customer_devices_customer_id ON customer_devices(customer_id);
CREATE INDEX idx_customer_devices_trusted ON customer_devices(trusted);
```

---

### 4.3.8 customer_locations

Stores known customer transaction locations.

```sql
CREATE TABLE customer_locations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    location VARCHAR(100) NOT NULL,
    usage_count INT NOT NULL DEFAULT 1,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    UNIQUE(customer_id, location)
);
```

Recommended indexes:

```sql
CREATE INDEX idx_customer_locations_customer_id ON customer_locations(customer_id);
CREATE INDEX idx_customer_locations_location ON customer_locations(location);
```

---

### 4.3.9 risk_rule_configs

Stores rule configuration.

```sql
CREATE TABLE risk_rule_configs (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    score INT NOT NULL,
    threshold_value NUMERIC(19,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

Recommended indexes:

```sql
CREATE INDEX idx_risk_rule_configs_active ON risk_rule_configs(active);
```

---

### 4.3.10 kafka_event_logs

Stores event processing records for traceability.

```sql
CREATE TABLE kafka_event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    topic_name VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);
```

Allowed statuses:

```text
PUBLISHED
CONSUMED
FAILED
DLQ
```

---

### 4.3.11 users

Stores internal application users.

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
```

---

### 4.3.12 roles

Stores access roles.

```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL
);
```

---

### 4.3.13 user_roles

Maps users to roles.

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

---

## 5. API Design

### 5.1 Common API Standards

All APIs follow these standards:

- JSON request and response body.
- JWT authentication.
- Role-based authorization.
- ISO-8601 datetime format.
- Structured error response.
- Sensitive account numbers masked in responses.
- Pagination for search and reporting endpoints.

### 5.2 Common Error Response

```json
{
  "timestamp": "2026-06-04T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "amount",
      "message": "Amount must be greater than zero"
    }
  ]
}
```

---

## 5.3 Transaction Service APIs

### 5.3.1 Submit Transaction

```http
POST /api/v1/transactions
Authorization: Bearer <jwt>
Idempotency-Key: unique-request-key
Content-Type: application/json
```

Request:

```json
{
  "sourceAccountNumber": "1234567890",
  "destinationAccountNumber": "9876543210",
  "amount": 25000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "deviceId": "IPHONE-15-DEVICE-001",
  "ipAddress": "36.77.88.12",
  "location": "Jakarta"
}
```

Response:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "status": "PENDING_RISK_CHECK",
  "message": "Transaction submitted for risk scoring"
}
```

Validation rules:

- `sourceAccountNumber` is required.
- `destinationAccountNumber` is required.
- `amount` must be greater than zero.
- `currency` is required.
- `channel` must be valid.
- `Idempotency-Key` header is required.
- Source account must exist.
- Source account must be active.

---

### 5.3.2 Get Transaction Detail

```http
GET /api/v1/transactions/{transactionRef}
Authorization: Bearer <jwt>
```

Response:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "sourceAccountNumber": "123****890",
  "destinationAccountNumber": "987****210",
  "amount": 25000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "status": "REVIEW",
  "riskScore": 72,
  "riskDecision": "REVIEW",
  "riskFactors": [
    {
      "code": "HIGH_AMOUNT",
      "description": "Transaction amount is above threshold",
      "score": 25
    },
    {
      "code": "NEW_DEVICE",
      "description": "Device is not trusted",
      "score": 20
    }
  ],
  "createdAt": "2026-06-04T10:00:00Z",
  "updatedAt": "2026-06-04T10:00:03Z"
}
```

---

### 5.3.3 High-Risk Transaction Report

```http
GET /api/v1/reports/high-risk-transactions?minimumRiskScore=50&startDate=2026-06-01&endDate=2026-06-04&page=0&size=20
Authorization: Bearer <jwt>
```

Response:

```json
{
  "data": [
    {
      "transactionRef": "TRX-20260604-000001",
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "sourceAccountNumber": "123****890",
      "destinationAccountNumber": "987****210",
      "amount": 25000000,
      "riskScore": 72,
      "decision": "REVIEW",
      "createdAt": "2026-06-04T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 5.4 Audit Search Service APIs

### 5.4.1 Search Fraud Audit

```http
GET /api/v1/audits/search?keyword=Jakarta&decision=REVIEW&minimumRiskScore=50&startDate=2026-06-01&endDate=2026-06-04&page=0&size=20
Authorization: Bearer <jwt>
```

Response:

```json
{
  "data": [
    {
      "transactionRef": "TRX-20260604-000001",
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "amount": 25000000,
      "currency": "IDR",
      "channel": "MOBILE_BANKING",
      "location": "Jakarta",
      "riskScore": 72,
      "decision": "REVIEW",
      "riskFactors": [
        "HIGH_AMOUNT",
        "NEW_DEVICE"
      ],
      "createdAt": "2026-06-04T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 5.5 Master Data Service APIs

### 5.5.1 Create Blacklisted Account

```http
POST /api/v1/blacklisted-accounts
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request:

```json
{
  "accountNumber": "9876543210",
  "reason": "Reported mule account"
}
```

Response:

```json
{
  "id": 1,
  "accountNumber": "9876543210",
  "reason": "Reported mule account",
  "active": true,
  "createdAt": "2026-06-04T10:00:00Z"
}
```

Side effect:

```text
DELETE blacklist:account:9876543210
```

---

### 5.5.2 Register Trusted Device

```http
POST /api/v1/customers/{customerId}/trusted-devices
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request:

```json
{
  "deviceId": "IPHONE-15-DEVICE-001",
  "deviceName": "Daniel iPhone"
}
```

Side effect:

```text
DELETE customer:trusted-device:{customerId}:IPHONE-15-DEVICE-001
```

---

## 6. Kafka Event Design

### 6.1 Topic List

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| transaction.created | Transaction Service | Risk Engine Service | Notify that a transaction has been created. |
| transaction.risk-scored | Risk Engine Service | Audit Search Service | Notify that a transaction has been scored. |
| transaction.created.dlq | Risk Engine Service | Operational review | Store failed transaction-created messages. |
| transaction.risk-scored.dlq | Audit Search Service | Operational review | Store failed risk-scored messages. |

The MVP uses `transaction.risk-scored` directly for audit indexing. A separate audit-created topic is not required for the initial event flow.

---

### 6.2 Event Envelope

All Kafka events use a common envelope:

```json
{
  "eventId": "evt-001",
  "eventType": "TRANSACTION_CREATED",
  "eventVersion": "1.0",
  "aggregateId": "TRX-20260604-000001",
  "occurredAt": "2026-06-04T10:00:00Z",
  "producer": "transaction-service",
  "payload": {}
}
```

### 6.3 transaction.created

Kafka key:

```text
transactionRef
```

Payload:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "sourceAccountNumber": "1234567890",
  "destinationAccountNumber": "9876543210",
  "amount": 25000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "deviceId": "IPHONE-15-DEVICE-001",
  "ipAddress": "36.77.88.12",
  "location": "Jakarta"
}
```

### 6.4 transaction.risk-scored

Kafka key:

```text
transactionRef
```

Payload:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "riskScore": 72,
  "decision": "REVIEW",
  "riskFactors": [
    {
      "code": "HIGH_AMOUNT",
      "description": "Transaction amount is above threshold",
      "score": 25
    },
    {
      "code": "NEW_DEVICE",
      "description": "Device is not trusted",
      "score": 20
    }
  ],
  "scoredAt": "2026-06-04T10:00:03Z"
}
```

### 6.5 Consumer Configuration

Recommended configuration:

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
      auto-offset-reset: earliest
    listener:
      ack-mode: manual
```

Processing policy:

```text
Maximum retry attempts: 3
Backoff strategy: exponential
DLQ: enabled
Consumer group: one group per consuming service
```

### 6.6 Event Processing Rules

- Consumers must be idempotent.
- Event processing must log `eventId`, `transactionRef`, and `consumerGroup`.
- Failed events must be retried.
- Failed events after retry exhaustion must be moved to DLQ.
- DLQ payload must include original event and error metadata.

---

## 7. Redis Design

### 7.1 Redis Usage

Redis is used for two purposes:

1. **Cache-aside storage** for frequently accessed risk reference data.
2. **Short-lived real-time counters** for transaction velocity checks.

### 7.2 Cache Key Strategy

| Data | Key Pattern | TTL |
|---|---|---:|
| Blacklisted account | `blacklist:account:{accountNumber}` | 10 minutes |
| Customer risk profile | `customer:risk-profile:{customerId}` | 5 minutes |
| Trusted device | `customer:trusted-device:{customerId}:{deviceId}` | 10 minutes |
| Risk rule config | `risk-rule:config:{ruleCode}` | 5 minutes |

### 7.3 Real-Time Counter Key Strategy

| Counter | Key Pattern | TTL |
|---|---|---:|
| Transaction count | `risk:velocity:count:{sourceAccountNumber}:10m` | 10 minutes |
| Transaction amount | `risk:velocity:amount:{sourceAccountNumber}:10m` | 10 minutes |

Example operations:

```text
INCR risk:velocity:count:1234567890:10m
EXPIRE risk:velocity:count:1234567890:10m 600

INCRBYFLOAT risk:velocity:amount:1234567890:10m 25000000
EXPIRE risk:velocity:amount:1234567890:10m 600
```

### 7.4 Cache-Aside Flow

```text
1. Risk Engine requests risk reference data.
2. Risk Engine checks Redis.
3. If Redis contains the data, use cached value.
4. If Redis misses, query PostgreSQL.
5. Store result in Redis with TTL.
6. Continue risk evaluation.
```

### 7.5 Cache Invalidation

Master Data Service invalidates related Redis keys after successful database update.

This prevents sensitive fraud reference data from remaining stale until TTL expiration.

---

## 8. Elasticsearch Design

### 8.1 Index Strategy

Index name:

```text
fraud-audit-events
```

### 8.2 Document Purpose

The Elasticsearch document is a denormalized search projection of scored transactions.

It supports:

- Keyword search.
- Structured filtering.
- Investigation workflows.
- Audit review.

It does not replace PostgreSQL as the source of truth.

### 8.3 Document Structure

```json
{
  "transactionRef": "TRX-20260604-000001",
  "customerCif": "CIF001",
  "customerName": "Daniel Sinaga",
  "sourceAccountMasked": "123****890",
  "destinationAccountMasked": "987****210",
  "amount": 25000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "location": "Jakarta",
  "riskScore": 72,
  "decision": "REVIEW",
  "riskFactors": [
    "HIGH_AMOUNT",
    "NEW_DEVICE"
  ],
  "createdAt": "2026-06-04T10:00:00Z",
  "scoredAt": "2026-06-04T10:00:03Z"
}
```

### 8.4 Mapping

```json
{
  "mappings": {
    "properties": {
      "transactionRef": { "type": "keyword" },
      "customerCif": { "type": "keyword" },
      "customerName": { "type": "text" },
      "sourceAccountMasked": { "type": "keyword" },
      "destinationAccountMasked": { "type": "keyword" },
      "amount": { "type": "double" },
      "currency": { "type": "keyword" },
      "channel": { "type": "keyword" },
      "location": { "type": "text" },
      "riskScore": { "type": "integer" },
      "decision": { "type": "keyword" },
      "riskFactors": { "type": "keyword" },
      "createdAt": { "type": "date" },
      "scoredAt": { "type": "date" }
    }
  }
}
```

---

## 9. Native SQL Reporting Design

### 9.1 Reporting Principles

Native SQL is used for reporting where direct SQL provides better control over:

- Joins.
- Aggregations.
- Window functions.
- Ranking.
- Time-based analysis.
- Query plans and indexes.

All report endpoints must support date range filtering.

---

### 9.2 High-Risk Transactions

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

### 9.3 Transaction Velocity by Account

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

### 9.4 Top Risk Customers

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

### 9.5 Suspicious Destination Accounts

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

### 9.6 Daily Fraud Trend

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

### 9.7 Risk Score Distribution

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

### 9.8 Daily Top Risky Customers Using CTE and Window Function

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

## 10. Security Design

### 10.1 Authentication

BankGuard uses JWT-based authentication.

For the MVP, authentication can be implemented as a simple internal authentication endpoint that issues JWT tokens for predefined users.

### 10.2 Authorization Roles

| Role | Description |
|---|---|
| ROLE_ADMIN | Manages system configuration and master data. |
| ROLE_BACKOFFICE | Submits and reviews transactions. |
| ROLE_FRAUD_ANALYST | Searches and investigates suspicious transactions. |
| ROLE_SYSTEM | Used for internal service-to-service operations if required. |

### 10.3 Endpoint Access Matrix

| Endpoint | ADMIN | BACKOFFICE | FRAUD_ANALYST |
|---|---:|---:|---:|
| POST /api/v1/transactions | Yes | Yes | No |
| GET /api/v1/transactions/{ref} | Yes | Yes | Yes |
| GET /api/v1/audits/search | Yes | No | Yes |
| GET /api/v1/reports/** | Yes | No | Yes |
| POST /api/v1/blacklisted-accounts | Yes | No | No |
| POST /api/v1/customers/{id}/trusted-devices | Yes | No | No |

### 10.4 Data Masking

Account numbers must be masked in API responses and Elasticsearch documents.

Example:

```text
1234567890 -> 123****890
```

### 10.5 Security Controls

- JWT validation.
- Role-based access control.
- Input validation.
- Global exception handling.
- Account number masking.
- Idempotency enforcement.
- Structured security error responses.

---

## 11. Idempotency Design

### 11.1 Problem

Transaction submission may be retried because of timeout, network issues, or client-side retries. Without idempotency, the same request can create duplicate transaction records.

### 11.2 Solution

Each transaction submission must include:

```http
Idempotency-Key: unique-request-key
```

Processing behavior:

```text
If idempotency key already exists:
    Return the existing transaction response.

If idempotency key does not exist:
    Create a new transaction.
    Store the idempotency key.
    Publish transaction.created.
```

### 11.3 Database Protection

```sql
ALTER TABLE transactions
ADD CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key);
```

The database unique constraint is required as the final protection against race conditions.

---

## 12. End-to-End Flow

### 12.1 Transaction Submission to Audit Indexing

```text
1. Client submits transaction with JWT and Idempotency-Key.
2. Transaction Service validates request.
3. Transaction Service checks idempotency key.
4. Transaction Service validates source account.
5. Transaction Service creates transaction with PENDING_RISK_CHECK.
6. Transaction Service publishes transaction.created.
7. Risk Engine Service consumes transaction.created.
8. Risk Engine loads transaction context.
9. Risk Engine reads Redis cache and PostgreSQL fallback as needed.
10. Risk Engine updates Redis velocity counters.
11. Risk Engine executes active risk rules.
12. Risk Engine calculates score and decision.
13. Risk Engine stores risk factors.
14. Risk Engine updates transaction status.
15. Risk Engine publishes transaction.risk-scored.
16. Audit Search Service consumes transaction.risk-scored.
17. Audit Search Service builds masked audit document.
18. Audit Search Service indexes document to Elasticsearch.
19. Fraud Analyst searches audit data through API.
```

---

## 13. Reliability and Failure Handling

### 13.1 Kafka Retry

Consumer failures are retried before moving the message to DLQ.

Policy:

```text
Retry attempts: 3
Backoff: exponential
DLQ: enabled
Manual ack: enabled
```

### 13.2 Dead Letter Topics

| Source Topic | DLQ Topic |
|---|---|
| transaction.created | transaction.created.dlq |
| transaction.risk-scored | transaction.risk-scored.dlq |

### 13.3 Idempotent Consumers

Consumers must avoid duplicate side effects.

Risk Engine should check whether a transaction has already been scored before applying scoring again.

Audit Search Service should use `transactionRef` as the Elasticsearch document ID so reprocessing updates the same audit document instead of creating duplicates.

### 13.4 Transaction Boundary

Risk Engine database changes should be executed in one transaction:

```text
1. Store risk factors.
2. Update transaction risk score and decision.
3. Mark processing log as consumed.
```

The event publish step follows the successful database update.

For later phases, an outbox pattern can be introduced for stronger event publishing guarantees.

---

## 14. Containerization and Deployment

### 14.1 Docker Compose Components

```yaml
services:
  postgres:
    image: postgres:16

  redis:
    image: redis:7

  kafka:
    image: confluentinc/cp-kafka

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0

  transaction-service:
    build: ./transaction-service
    depends_on:
      - postgres
      - kafka

  risk-engine-service:
    build: ./risk-engine-service
    depends_on:
      - postgres
      - kafka
      - redis

  audit-search-service:
    build: ./audit-search-service
    depends_on:
      - kafka
      - elasticsearch

  master-data-service:
    build: ./master-data-service
    depends_on:
      - postgres
      - redis
```

### 14.2 Environment Variables

```text
DB_URL
DB_USERNAME
DB_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
REDIS_HOST
REDIS_PORT
ELASTICSEARCH_URL
JWT_SECRET
JWT_EXPIRATION_SECONDS
SERVICE_PORT
```

### 14.3 Health Checks

Each service should expose:

```http
GET /actuator/health
```

Infrastructure health checks:

- PostgreSQL connection.
- Redis connection.
- Kafka broker availability.
- Elasticsearch availability.

---

## 15. Testing Strategy

### 15.1 Unit Tests

Unit tests cover:

- Request validation.
- Transaction reference generation.
- Idempotency service behavior.
- Risk rule evaluation.
- Risk score calculation.
- Decision threshold mapping.
- Masking utility.
- Mapper logic.

### 15.2 Integration Tests

Integration tests cover:

- PostgreSQL repository behavior.
- Native SQL reports.
- Kafka producer and consumer flow.
- Redis cache hit and miss behavior.
- Redis counter expiration.
- Elasticsearch indexing and search.
- JWT security and role-based authorization.

### 15.3 End-to-End Tests

End-to-end tests cover:

```text
Submit transaction
-> publish transaction.created
-> consume and score transaction
-> publish transaction.risk-scored
-> index audit document
-> search audit document
```

### 15.4 Testcontainers

Recommended containers:

```text
PostgreSQLContainer
KafkaContainer
RedisContainer
ElasticsearchContainer
```

---

## 16. Observability

### 16.1 Logging

Each service logs structured JSON fields:

```json
{
  "timestamp": "2026-06-04T10:00:00Z",
  "service": "risk-engine-service",
  "requestId": "req-001",
  "eventId": "evt-001",
  "transactionRef": "TRX-20260604-000001",
  "message": "Risk scoring completed",
  "riskScore": 72,
  "decision": "REVIEW"
}
```

### 16.2 Correlation Fields

The following identifiers must be propagated where applicable:

- `requestId`
- `eventId`
- `transactionRef`
- `idempotencyKey`

### 16.3 Operational Metrics

Recommended metrics:

- Transaction submission count.
- Transaction submission latency.
- Kafka consumer lag.
- Risk scoring duration.
- Risk decision count by status.
- Redis cache hit and miss count.
- Elasticsearch indexing failure count.
- DLQ message count.

---

## 17. Project Structure

```text
bankguard/
├── docker-compose.yml
├── README.md
├── docs/
│   ├── PRD.md
│   ├── SYSTEM_DESIGN.md
│   └── API_COLLECTION.json
├── transaction-service/
│   ├── src/main/java/.../controller/
│   ├── src/main/java/.../service/
│   ├── src/main/java/.../repository/
│   ├── src/main/java/.../entity/
│   ├── src/main/java/.../dto/
│   ├── src/main/java/.../kafka/
│   ├── src/main/java/.../security/
│   └── src/main/resources/db/migration/
├── risk-engine-service/
│   ├── src/main/java/.../consumer/
│   ├── src/main/java/.../rule/
│   ├── src/main/java/.../service/
│   ├── src/main/java/.../repository/
│   ├── src/main/java/.../cache/
│   └── src/main/java/.../config/
├── audit-search-service/
│   ├── src/main/java/.../consumer/
│   ├── src/main/java/.../document/
│   ├── src/main/java/.../repository/
│   ├── src/main/java/.../controller/
│   └── src/main/java/.../config/
└── master-data-service/
    ├── src/main/java/.../controller/
    ├── src/main/java/.../service/
    ├── src/main/java/.../repository/
    ├── src/main/java/.../entity/
    ├── src/main/java/.../cache/
    └── src/main/resources/db/migration/
```

---

## 18. MVP Traceability Matrix

| PRD MVP | System Design Coverage |
|---|---|
| MVP-01 Transaction Submission | Transaction Service, Transaction API, transactions table, idempotency, Kafka producer |
| MVP-02 Risk Scoring Engine | Risk Engine Service, RiskRule interface, Java Stream pipeline, risk factors, scoring thresholds |
| MVP-03 Event-Based Transaction Processing | Kafka topics, event envelope, consumer strategy, retry, DLQ |
| MVP-04 Redis Risk Cache and Real-Time Risk Counters | Redis key strategy, TTL, cache-aside flow, velocity counters |
| MVP-05 Elasticsearch Audit Search | Audit Search Service, index mapping, document design, search API |
| MVP-06 Transaction Investigation API | Transaction detail API, audit search API, masking rules |
| MVP-07 Native SQL Reporting | Reporting APIs, native SQL query catalog, CTE and window function reports |

---

## 19. Implementation Priorities

### Phase 1: Core Infrastructure

- Docker Compose.
- PostgreSQL.
- Kafka.
- Redis.
- Elasticsearch.
- Shared configuration.

### Phase 2: Transaction Flow

- Transaction Service.
- Transaction API.
- Idempotency.
- PostgreSQL persistence.
- Kafka producer.

### Phase 3: Risk Scoring

- Risk Engine consumer.
- Risk rules.
- Redis cache lookup.
- Redis velocity counters.
- Risk score update.
- Risk event publishing.

### Phase 4: Audit Search

- Audit Search consumer.
- Elasticsearch document.
- Indexing.
- Search API.

### Phase 5: Master Data and Reporting

- Blacklist management.
- Trusted device management.
- Cache invalidation.
- Native SQL reporting APIs.

### Phase 6: Reliability and Testing

- Retry and DLQ.
- Integration tests.
- End-to-end test.
- Security test.
- Documentation finalization.
