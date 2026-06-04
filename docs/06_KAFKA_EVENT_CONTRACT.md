# Kafka Event Contract
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the Kafka event contract for **BankGuard**.

The Kafka Event Contract provides implementation-level specifications for:

- Kafka topic ownership.
- Event lifecycle.
- Event envelope format.
- Event payload schemas.
- Producer behavior.
- Consumer behavior.
- Message key strategy.
- Retry and dead-letter handling.
- Idempotency rules.
- Event logging requirements.
- Schema compatibility rules.
- Operational and testing requirements.

This document is aligned with:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`

---

### 1.2 Scope

This contract covers Kafka events required for the MVP flow:

1. A transaction is submitted through Transaction Service.
2. Transaction Service persists the transaction in PostgreSQL.
3. Transaction Service publishes `transaction.created`.
4. Risk Engine Service consumes `transaction.created`.
5. Risk Engine Service evaluates fraud rules and updates risk result.
6. Risk Engine Service publishes `transaction.risk-scored`.
7. Audit Search Service consumes `transaction.risk-scored`.
8. Audit Search Service indexes the audit document into Elasticsearch.
9. Failed event processing is retried and then moved to the relevant dead-letter topic.

---

### 1.3 Non-Scope

This contract does not cover:

- Kafka Connect.
- Change Data Capture.
- Schema Registry deployment.
- Real-time dashboard streaming.
- Machine learning feature streaming.
- Core banking integration events.
- Payment gateway events.
- Regulatory reporting events.
- Multi-region Kafka replication.

These may be introduced in future phases.

---

## 2. Event-Driven Design Principles

### 2.1 Kafka as Asynchronous Processing Backbone

Kafka is used to decouple transaction submission from risk scoring and audit indexing.

Transaction submission must not wait for fraud scoring to complete. The API returns after the transaction is stored and the `transaction.created` event is published.

---

### 2.2 PostgreSQL Remains Source of Truth

Kafka events represent state changes and processing signals, but they are not the source of truth.

Authoritative data remains in PostgreSQL for:

- transaction records
- risk decisions
- risk factors
- customer data
- account data
- master data
- event processing logs

Elasticsearch is populated from Kafka events but remains a read model for search.

---

### 2.3 Event Consumers Must Be Idempotent

Kafka may deliver messages more than once under retry or recovery scenarios.

Each consumer must be safe to reprocess the same event.

Required behavior:

- Risk Engine Service must skip scoring if a transaction already has a final risk decision.
- Audit Search Service must use `transactionRef` as the Elasticsearch document ID so repeated processing updates the same document.
- All consumers must check `eventId` in `kafka_event_logs` where applicable.

---

### 2.4 Events Must Be Traceable

Every event must include correlation fields that allow debugging across services.

Minimum traceability fields:

- `eventId`
- `eventType`
- `eventVersion`
- `aggregateId`
- `transactionRef`
- `occurredAt`
- `producer`
- `requestId`

---

### 2.5 Events Must Be Backward Compatible

Event changes must avoid breaking existing consumers.

Allowed compatible changes:

- adding optional fields
- adding metadata fields
- adding new event versions while keeping old consumers supported

Breaking changes require a new `eventVersion` and consumer compatibility handling.

---

## 3. Topic Catalog

## 3.1 Required MVP Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `transaction.created` | Transaction Service | Risk Engine Service | Notifies that a transaction was created and is ready for risk scoring. |
| `transaction.risk-scored` | Risk Engine Service | Audit Search Service | Notifies that a transaction has been scored and is ready for audit indexing. |
| `transaction.created.dlq` | Risk Engine Service | Operational Review | Stores failed `transaction.created` events after retry exhaustion. |
| `transaction.risk-scored.dlq` | Audit Search Service | Operational Review | Stores failed `transaction.risk-scored` events after retry exhaustion. |

---

## 3.2 Topic Naming Convention

Kafka topic names must use lowercase kebab/dot notation:

```text
<domain>.<event-name>
<domain>.<event-name>.dlq
```

Examples:

```text
transaction.created
transaction.risk-scored
transaction.created.dlq
transaction.risk-scored.dlq
```

---

## 3.3 Topics Not Included in MVP

The MVP does not require a separate `fraud.audit-created` topic.

Audit Search Service consumes `transaction.risk-scored` directly and indexes the audit document into Elasticsearch.

A separate audit topic may be introduced later if there are multiple audit consumers or additional audit orchestration requirements.

---

## 4. Event Lifecycle

## 4.1 Transaction Created Flow

```text
1. Client submits transaction to Transaction Service.
2. Transaction Service validates request and idempotency key.
3. Transaction Service stores transaction with PENDING_RISK_CHECK status.
4. Transaction Service publishes transaction.created.
5. Transaction Service records kafka_event_logs with PUBLISHED status.
6. Risk Engine Service consumes transaction.created.
7. Risk Engine Service evaluates risk.
8. Risk Engine Service updates transaction status, risk score, risk decision, and risk factors.
9. Risk Engine Service records kafka_event_logs with CONSUMED status.
10. Risk Engine Service publishes transaction.risk-scored.
```

---

## 4.2 Risk Scored Flow

```text
1. Risk Engine Service publishes transaction.risk-scored.
2. Audit Search Service consumes transaction.risk-scored.
3. Audit Search Service builds a masked audit document.
4. Audit Search Service indexes the document into Elasticsearch using transactionRef as document ID.
5. Audit Search Service records kafka_event_logs with CONSUMED status.
```

---

## 4.3 Failure Flow

```text
1. Consumer receives an event.
2. Consumer validates envelope and payload.
3. Consumer processing fails.
4. Consumer retries according to retry policy.
5. If retry succeeds, message is acknowledged.
6. If retry is exhausted, event is published to DLQ.
7. kafka_event_logs is updated with FAILED or DLQ status.
8. Original event and error metadata are preserved in DLQ payload.
```

---

## 5. Common Event Envelope

## 5.1 Envelope Format

All Kafka events must use the following envelope:

```json
{
  "eventId": "7a7f0e9b-3dd6-4f22-b23a-44e42c25a001",
  "eventType": "TRANSACTION_CREATED",
  "eventVersion": "1.0",
  "aggregateType": "TRANSACTION",
  "aggregateId": "TRX-20260604-000001",
  "occurredAt": "2026-06-04T10:00:00Z",
  "producer": "transaction-service",
  "requestId": "req-001",
  "correlationId": "corr-001",
  "payload": {}
}
```

---

## 5.2 Envelope Field Definition

| Field | Type | Required | Description |
|---|---|---:|---|
| `eventId` | string UUID | yes | Globally unique event identifier. |
| `eventType` | string | yes | Event type name. |
| `eventVersion` | string | yes | Event schema version. Default is `1.0`. |
| `aggregateType` | string | yes | Business aggregate type. For MVP: `TRANSACTION`. |
| `aggregateId` | string | yes | Aggregate identifier. For transaction events: `transactionRef`. |
| `occurredAt` | string datetime | yes | ISO-8601 UTC timestamp when the event occurred. |
| `producer` | string | yes | Producing service name. |
| `requestId` | string | no | Request identifier from API layer or generated internally. |
| `correlationId` | string | no | Cross-service correlation identifier. |
| `payload` | object | yes | Event-specific payload. |

---

## 5.3 Envelope Validation Rules

| Field | Validation |
|---|---|
| `eventId` | must be non-blank and valid UUID |
| `eventType` | must match the topic event type |
| `eventVersion` | must be supported by the consumer |
| `aggregateType` | must be `TRANSACTION` for MVP transaction events |
| `aggregateId` | must equal `transactionRef` inside payload |
| `occurredAt` | must be valid ISO-8601 UTC datetime |
| `producer` | must be known service name |
| `payload` | must not be null |

---

## 5.4 Supported Producer Names

```text
transaction-service
risk-engine-service
audit-search-service
master-data-service
```

For MVP, only the following services publish Kafka events:

```text
transaction-service
risk-engine-service
```

---

## 5.5 Supported Event Versions

Initial MVP event version:

```text
1.0
```

Consumers must reject unsupported event versions with controlled error handling.

---

## 6. Kafka Message Key Strategy

### 6.1 Message Key

All transaction events must use:

```text
transactionRef
```

as the Kafka message key.

Example:

```text
TRX-20260604-000001
```

---

### 6.2 Reason

Using `transactionRef` as the key ensures all events for the same transaction are routed consistently by Kafka partitioning.

This supports ordered processing per transaction when topics have multiple partitions.

---

### 6.3 Key Validation

The message key must:

- be non-blank
- equal `aggregateId`
- equal `payload.transactionRef`
- follow the transaction reference format

```text
TRX-yyyyMMdd-000001
```

---

## 7. Event Contract: transaction.created

## 7.1 Topic

```text
transaction.created
```

---

## 7.2 Purpose

Notifies downstream consumers that a transaction has been successfully created and is ready for risk scoring.

---

## 7.3 Producer

```text
transaction-service
```

---

## 7.4 Consumer

```text
risk-engine-service
```

---

## 7.5 Event Type

```text
TRANSACTION_CREATED
```

---

## 7.6 Kafka Key

```text
transactionRef
```

---

## 7.7 Publish Trigger

Transaction Service must publish this event only after:

1. request validation succeeds
2. source account validation succeeds
3. idempotency check confirms this is a new transaction
4. transaction record is successfully stored in PostgreSQL with `PENDING_RISK_CHECK`
5. transaction reference has been generated

---

## 7.8 Payload Schema

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
  "location": "Jakarta",
  "createdAt": "2026-06-04T10:00:00Z"
}
```

---

## 7.9 Payload Field Definition

| Field | Type | Required | Description |
|---|---|---:|---|
| `transactionRef` | string | yes | Unique transaction reference. |
| `sourceAccountNumber` | string | yes | Source account number used for risk context lookup. |
| `destinationAccountNumber` | string | yes | Destination account number used for blacklist checking. |
| `amount` | number | yes | Transaction amount. |
| `currency` | string | yes | Transaction currency. MVP supports `IDR`. |
| `channel` | string | yes | Transaction channel. |
| `deviceId` | string | no | Device identifier used for trusted device checking. |
| `ipAddress` | string | no | IP address used for audit and future rules. |
| `location` | string | no | Transaction location used for unusual location checking. |
| `createdAt` | string datetime | yes | Transaction creation timestamp. |

---

## 7.10 Payload Validation Rules

| Field | Validation |
|---|---|
| `transactionRef` | required, must match transaction reference format |
| `sourceAccountNumber` | required, non-blank |
| `destinationAccountNumber` | required, non-blank |
| `amount` | required, must be greater than 0 |
| `currency` | required, must be `IDR` for MVP |
| `channel` | required, must be one of allowed transaction channels |
| `deviceId` | optional, max 100 characters |
| `ipAddress` | optional, valid IPv4 or IPv6 when present |
| `location` | optional, max 100 characters |
| `createdAt` | required, valid ISO-8601 UTC datetime |

Allowed transaction channels:

```text
MOBILE_BANKING
INTERNET_BANKING
ATM
BRANCH
BACK_OFFICE
```

---

## 7.11 Processing Behavior in Risk Engine Service

Risk Engine Service must process `transaction.created` as follows:

```text
1. Receive message from transaction.created.
2. Validate Kafka key.
3. Validate event envelope.
4. Validate payload.
5. Check eventId in kafka_event_logs.
6. If eventId already consumed, ignore safely.
7. Load transaction by transactionRef from PostgreSQL.
8. If transaction does not exist, fail and retry.
9. If transaction status is APPROVED, REVIEW, or BLOCKED, mark as IGNORED.
10. Build TransactionContext.
11. Load risk reference data using Redis cache-aside.
12. Update Redis velocity counters.
13. Execute active risk rules.
14. Store transaction risk factors.
15. Update transaction risk_score, risk_decision, and status.
16. Mark event as CONSUMED.
17. Publish transaction.risk-scored.
18. Acknowledge Kafka message.
```

---

## 7.12 Success Side Effects

Successful consumption must result in:

- `transactions.status` updated to `APPROVED`, `REVIEW`, or `BLOCKED`
- `transactions.risk_score` updated
- `transactions.risk_decision` updated
- one or more `transaction_risk_factors` inserted if rules are triggered
- `kafka_event_logs` updated with `CONSUMED`
- `transaction.risk-scored` event published

---

## 7.13 Failure Handling

| Failure Case | Behavior |
|---|---|
| Invalid envelope | Do not process. Send to DLQ after retry policy. |
| Invalid payload | Do not process. Send to DLQ after retry policy. |
| Transaction not found | Retry because transaction may not be visible yet due to timing issue. |
| PostgreSQL unavailable | Retry. |
| Redis unavailable | Continue with PostgreSQL fallback for cache lookups where possible; fail only if rule evaluation cannot safely continue. |
| Rule execution error | Retry. |
| Transaction already scored | Mark event as `IGNORED`; acknowledge message. |
| Failed to publish `transaction.risk-scored` | Retry or mark event processing as failed depending on implementation policy. |

---

## 8. Event Contract: transaction.risk-scored

## 8.1 Topic

```text
transaction.risk-scored
```

---

## 8.2 Purpose

Notifies downstream consumers that a transaction has completed risk scoring and is ready for audit indexing.

---

## 8.3 Producer

```text
risk-engine-service
```

---

## 8.4 Consumer

```text
audit-search-service
```

---

## 8.5 Event Type

```text
TRANSACTION_RISK_SCORED
```

---

## 8.6 Kafka Key

```text
transactionRef
```

---

## 8.7 Publish Trigger

Risk Engine Service must publish this event only after:

1. `transaction.created` has been consumed successfully
2. risk rules have been evaluated
3. transaction risk score has been calculated
4. risk decision has been mapped
5. risk factors have been stored
6. transaction row has been updated in PostgreSQL
7. event processing log has been marked as `CONSUMED`

---

## 8.8 Payload Schema

```json
{
  "transactionRef": "TRX-20260604-000001",
  "customerCif": "CIF001",
  "customerName": "Daniel Sinaga",
  "sourceAccountNumber": "1234567890",
  "destinationAccountNumber": "9876543210",
  "amount": 25000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "deviceId": "IPHONE-15-DEVICE-001",
  "ipAddress": "36.77.88.12",
  "location": "Jakarta",
  "riskScore": 72,
  "decision": "REVIEW",
  "riskFactors": [
    {
      "code": "HIGH_AMOUNT",
      "description": "Transaction amount is above configured threshold",
      "score": 25,
      "metadata": {
        "threshold": 10000000,
        "actualAmount": 25000000
      }
    },
    {
      "code": "NEW_DEVICE",
      "description": "Device is not trusted for this customer",
      "score": 20,
      "metadata": {
        "deviceId": "IPHONE-15-DEVICE-001"
      }
    }
  ],
  "createdAt": "2026-06-04T10:00:00Z",
  "scoredAt": "2026-06-04T10:00:03Z"
}
```

---

## 8.9 Payload Field Definition

| Field | Type | Required | Description |
|---|---|---:|---|
| `transactionRef` | string | yes | Unique transaction reference. |
| `customerCif` | string | yes | Customer CIF used for search and audit. |
| `customerName` | string | yes | Customer name used for search and audit. |
| `sourceAccountNumber` | string | yes | Source account number. Must be masked before Elasticsearch indexing. |
| `destinationAccountNumber` | string | yes | Destination account number. Must be masked before Elasticsearch indexing. |
| `amount` | number | yes | Transaction amount. |
| `currency` | string | yes | Transaction currency. |
| `channel` | string | yes | Transaction channel. |
| `deviceId` | string | no | Device identifier. |
| `ipAddress` | string | no | IP address. |
| `location` | string | no | Transaction location. |
| `riskScore` | integer | yes | Total risk score. |
| `decision` | string | yes | Risk decision. |
| `riskFactors` | array | yes | Triggered risk factors. Can be empty if risk score is 0. |
| `createdAt` | string datetime | yes | Original transaction creation timestamp. |
| `scoredAt` | string datetime | yes | Risk scoring completion timestamp. |

---

## 8.10 Risk Factor Object Schema

```json
{
  "code": "HIGH_AMOUNT",
  "description": "Transaction amount is above configured threshold",
  "score": 25,
  "metadata": {
    "threshold": 10000000,
    "actualAmount": 25000000
  }
}
```

| Field | Type | Required | Description |
|---|---|---:|---|
| `code` | string | yes | Risk factor code. |
| `description` | string | yes | Human-readable explanation. |
| `score` | integer | yes | Score contributed by the factor. |
| `metadata` | object | no | Additional contextual data for audit and debugging. |

---

## 8.11 Payload Validation Rules

| Field | Validation |
|---|---|
| `transactionRef` | required, must match transaction reference format |
| `customerCif` | required, non-blank |
| `customerName` | required, non-blank |
| `sourceAccountNumber` | required, non-blank |
| `destinationAccountNumber` | required, non-blank |
| `amount` | required, greater than 0 |
| `currency` | required, must be `IDR` for MVP |
| `channel` | required, must be valid channel |
| `riskScore` | required, must be >= 0 |
| `decision` | required, must be `APPROVED`, `REVIEW`, or `BLOCKED` |
| `riskFactors` | required array, may be empty only when `riskScore = 0` |
| `createdAt` | required, ISO-8601 UTC |
| `scoredAt` | required, ISO-8601 UTC |

---

## 8.12 Processing Behavior in Audit Search Service

Audit Search Service must process `transaction.risk-scored` as follows:

```text
1. Receive message from transaction.risk-scored.
2. Validate Kafka key.
3. Validate event envelope.
4. Validate payload.
5. Check eventId in kafka_event_logs.
6. If eventId already consumed, ignore safely.
7. Build fraud audit document.
8. Mask source and destination account numbers.
9. Use transactionRef as Elasticsearch document ID.
10. Upsert document into fraud-audit-events index.
11. Mark event as CONSUMED.
12. Acknowledge Kafka message.
```

---

## 8.13 Success Side Effects

Successful consumption must result in:

- one document upserted into Elasticsearch index `fraud-audit-events`
- source account number masked
- destination account number masked
- risk factors searchable
- `kafka_event_logs` updated with `CONSUMED`

---

## 8.14 Failure Handling

| Failure Case | Behavior |
|---|---|
| Invalid envelope | Do not process. Send to DLQ after retry policy. |
| Invalid payload | Do not process. Send to DLQ after retry policy. |
| Elasticsearch unavailable | Retry. |
| Elasticsearch mapping error | Send to DLQ after retry. |
| Duplicate event | Upsert same document ID or mark as ignored. |
| Account masking failure | Fail processing and retry. |

---

## 9. Dead Letter Queue Contract

## 9.1 DLQ Topics

| Source Topic | DLQ Topic | DLQ Producer |
|---|---|---|
| `transaction.created` | `transaction.created.dlq` | Risk Engine Service |
| `transaction.risk-scored` | `transaction.risk-scored.dlq` | Audit Search Service |

---

## 9.2 DLQ Purpose

DLQ topics store events that could not be processed after all retry attempts.

They are used for:

- operational review
- debugging
- replay after fixing data or code issues
- preserving failed payloads for investigation

---

## 9.3 DLQ Envelope

DLQ messages must use the following structure:

```json
{
  "dlqEventId": "5b743cfb-f21c-4de4-8ac2-d97d86bc6001",
  "originalTopic": "transaction.created",
  "originalKey": "TRX-20260604-000001",
  "originalEvent": {
    "eventId": "7a7f0e9b-3dd6-4f22-b23a-44e42c25a001",
    "eventType": "TRANSACTION_CREATED",
    "eventVersion": "1.0",
    "aggregateType": "TRANSACTION",
    "aggregateId": "TRX-20260604-000001",
    "occurredAt": "2026-06-04T10:00:00Z",
    "producer": "transaction-service",
    "requestId": "req-001",
    "correlationId": "corr-001",
    "payload": {}
  },
  "error": {
    "errorType": "PROCESSING_ERROR",
    "errorMessage": "Transaction was not found",
    "stackTrace": "optional-for-local-dev",
    "failedService": "risk-engine-service",
    "failedAt": "2026-06-04T10:00:10Z",
    "attempt": 3
  }
}
```

---

## 9.4 DLQ Field Definition

| Field | Type | Required | Description |
|---|---|---:|---|
| `dlqEventId` | string UUID | yes | Unique DLQ event identifier. |
| `originalTopic` | string | yes | Source topic. |
| `originalKey` | string | yes | Original Kafka message key. |
| `originalEvent` | object | yes | Original event envelope and payload. |
| `error` | object | yes | Failure metadata. |

---

## 9.5 DLQ Error Types

Allowed `errorType` values:

```text
VALIDATION_ERROR
PROCESSING_ERROR
DEPENDENCY_UNAVAILABLE
MAPPING_ERROR
UNKNOWN_ERROR
```

---

## 9.6 DLQ Processing Requirement

The MVP does not require an automated DLQ replay service.

However, DLQ messages must preserve enough information to allow manual replay or manual diagnosis.

---

## 10. Producer Requirements

## 10.1 General Producer Rules

All producers must:

- use the common event envelope
- generate a unique `eventId`
- set `eventVersion`
- set `aggregateType`
- set `aggregateId`
- use `transactionRef` as Kafka message key
- serialize events as JSON
- publish only after required database state is committed or safely persisted
- log event publishing attempts
- record event publishing result in `kafka_event_logs`

---

## 10.2 Transaction Service Producer Rules

Transaction Service publishes:

```text
transaction.created
```

It must publish only after transaction persistence succeeds.

If event publishing fails:

- API may return `EVENT_PUBLISH_FAILED`
- transaction may remain in `PENDING_RISK_CHECK`
- `kafka_event_logs` should capture the failure where possible

For MVP, direct publishing after transaction insert is acceptable.

Future improvement:

```text
Transactional Outbox Pattern
```

---

## 10.3 Risk Engine Service Producer Rules

Risk Engine Service publishes:

```text
transaction.risk-scored
```

It must publish only after:

- risk factors are stored
- transaction status is updated
- transaction risk score is updated
- event log is marked as consumed

If event publishing fails:

- message processing must be treated as failed if audit indexing cannot be triggered
- retry policy must apply

---

## 11. Consumer Requirements

## 11.1 General Consumer Rules

All consumers must:

- disable Kafka auto commit
- use manual acknowledgment
- validate event envelope
- validate event payload
- check idempotency
- log processing lifecycle
- update `kafka_event_logs`
- acknowledge only after successful processing
- send failed events to DLQ after retry exhaustion

---

## 11.2 Consumer Group IDs

| Service | Topic | Consumer Group ID |
|---|---|---|
| Risk Engine Service | `transaction.created` | `risk-engine-service-group` |
| Audit Search Service | `transaction.risk-scored` | `audit-search-service-group` |

---

## 11.3 Consumer Offset Policy

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
      auto-offset-reset: earliest
    listener:
      ack-mode: manual
```

---

## 11.4 Retry Policy

| Setting | Value |
|---|---|
| Maximum attempts | 3 |
| Backoff strategy | Exponential |
| Initial backoff | 1 second |
| Maximum backoff | 10 seconds |
| DLQ after retry exhausted | yes |

---

## 11.5 Retryable Errors

The following errors should be treated as retryable:

```text
PostgreSQL unavailable
Kafka temporary failure
Redis unavailable when fallback cannot continue
Elasticsearch unavailable
Transient network error
Unexpected processing exception
```

---

## 11.6 Non-Retryable Errors

The following errors should be treated as non-retryable or immediately DLQ after validation failure:

```text
Unsupported event version
Invalid required payload field
Invalid risk decision value
Invalid transaction amount
Invalid transaction channel
Malformed JSON
```

---

## 12. Kafka Event Log Mapping

## 12.1 Database Table

Kafka event lifecycle must be stored in:

```text
kafka_event_logs
```

---

## 12.2 Required Columns

Relevant columns:

```text
event_id
event_type
aggregate_id
topic_name
payload
status
error_message
created_at
processed_at
```

---

## 12.3 Event Log Status Values

Allowed values:

```text
PUBLISHED
CONSUMED
FAILED
DLQ
IGNORED
```

---

## 12.4 Producer Event Log Behavior

When a service publishes an event successfully:

```text
status = PUBLISHED
event_id = envelope.eventId
event_type = envelope.eventType
aggregate_id = envelope.aggregateId
topic_name = topic name
payload = full event envelope
created_at = current timestamp
```

---

## 12.5 Consumer Event Log Behavior

When a service consumes an event successfully:

```text
status = CONSUMED
event_id = envelope.eventId
event_type = envelope.eventType
aggregate_id = envelope.aggregateId
topic_name = consumed topic name
payload = full event envelope
processed_at = current timestamp
```

---

## 12.6 Duplicate Event Behavior

If an event was already processed:

```text
status = IGNORED
error_message = Duplicate event ignored
processed_at = current timestamp
```

Consumers must not create duplicate side effects.

---

## 12.7 Failure Event Log Behavior

If an event fails processing:

```text
status = FAILED
error_message = failure reason
processed_at = current timestamp
```

If moved to DLQ:

```text
status = DLQ
error_message = final failure reason
processed_at = current timestamp
```

---

## 13. Event Schema Compatibility

## 13.1 Versioning

Each event must include:

```text
eventVersion
```

Initial version:

```text
1.0
```

---

## 13.2 Compatible Changes

Allowed compatible changes:

- add optional payload field
- add optional metadata field
- add new event type
- add new risk factor code

---

## 13.3 Breaking Changes

Breaking changes require a new event version.

Breaking changes include:

- removing required field
- renaming required field
- changing field type
- changing enum semantics
- changing event meaning
- changing decision threshold semantics in event interpretation

---

## 13.4 Consumer Compatibility Rule

Consumers must explicitly validate supported versions.

If unsupported:

```text
Reject event -> DLQ with errorType = VALIDATION_ERROR
```

---

## 14. Serialization Requirements

### 14.1 Format

All Kafka events must be serialized as JSON.

---

### 14.2 Date and Time

Datetime fields must use ISO-8601 UTC.

Example:

```text
2026-06-04T10:00:00Z
```

---

### 14.3 Money

Money fields must be represented as JSON numbers.

Java implementation must use:

```text
BigDecimal
```

---

### 14.4 Null Handling

Required fields must not be null.

Optional fields may be omitted or set to null.

Consumers must treat missing optional fields safely.

---

## 15. Security and Data Sensitivity

## 15.1 Account Numbers in Events

Kafka events may contain unmasked account numbers because Risk Engine requires raw account numbers for:

- source account lookup
- blacklist checking
- transaction context building
- audit document construction

However:

- account numbers must be masked before API response
- account numbers must be masked before Elasticsearch indexing
- Kafka access must be limited to internal services only

---

## 15.2 Sensitive Event Logging

Application logs must not print full raw event payloads at INFO level.

Allowed INFO logs:

```text
eventId
eventType
transactionRef
topic
consumerGroup
status
```

Full payload logging is allowed only at DEBUG level in local development.

---

## 15.3 JWT and User Data

JWT tokens must never be published in Kafka events.

Kafka events must not include:

- Authorization header
- passwords
- password hashes
- access tokens
- refresh tokens

---

## 16. Observability Requirements

## 16.1 Required Log Fields

All Kafka producers and consumers must log:

```text
eventId
eventType
topic
transactionRef
requestId
correlationId
producer
consumerGroup
status
durationMs
```

---

## 16.2 Producer Logs

Producer must log:

```text
EVENT_PUBLISH_STARTED
EVENT_PUBLISH_SUCCEEDED
EVENT_PUBLISH_FAILED
```

---

## 16.3 Consumer Logs

Consumer must log:

```text
EVENT_CONSUME_STARTED
EVENT_VALIDATION_FAILED
EVENT_PROCESSING_SUCCEEDED
EVENT_PROCESSING_FAILED
EVENT_SENT_TO_DLQ
EVENT_IGNORED_DUPLICATE
```

---

## 16.4 Metrics

Recommended metrics:

| Metric | Description |
|---|---|
| `kafka_event_published_total` | Count of published events by topic and event type. |
| `kafka_event_consumed_total` | Count of consumed events by topic and consumer group. |
| `kafka_event_failed_total` | Count of failed event processing attempts. |
| `kafka_event_dlq_total` | Count of events sent to DLQ. |
| `kafka_consumer_lag` | Consumer lag by group and topic. |
| `risk_scoring_event_duration_ms` | Time to process transaction.created event. |
| `audit_indexing_event_duration_ms` | Time to process transaction.risk-scored event. |

---

## 17. Local Development Configuration

## 17.1 Kafka Bootstrap Servers

Environment variable:

```text
KAFKA_BOOTSTRAP_SERVERS
```

Example:

```text
localhost:9092
```

---

## 17.2 Required Topic Creation

Docker Compose or application startup must ensure required topics exist:

```text
transaction.created
transaction.risk-scored
transaction.created.dlq
transaction.risk-scored.dlq
```

---

## 17.3 Recommended Topic Settings for Local MVP

| Topic | Partitions | Replication Factor |
|---|---:|---:|
| `transaction.created` | 3 | 1 |
| `transaction.risk-scored` | 3 | 1 |
| `transaction.created.dlq` | 1 | 1 |
| `transaction.risk-scored.dlq` | 1 | 1 |

---

## 17.4 Spring Kafka Configuration Example

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: "com.bankguard.*"
    listener:
      ack-mode: manual
```

---

## 18. Testing Requirements

## 18.1 Producer Tests

Required tests:

- Transaction Service publishes `transaction.created` after transaction persistence.
- Transaction Service does not publish event for invalid request.
- Transaction Service does not publish new event for duplicate idempotency key.
- Risk Engine Service publishes `transaction.risk-scored` after successful risk update.
- Event envelope contains required fields.

---

## 18.2 Consumer Tests

Required tests:

- Risk Engine Service consumes valid `transaction.created`.
- Risk Engine Service rejects invalid event envelope.
- Risk Engine Service skips already-scored transaction.
- Audit Search Service consumes valid `transaction.risk-scored`.
- Audit Search Service upserts document using `transactionRef`.
- Consumer acknowledges only after successful processing.

---

## 18.3 DLQ Tests

Required tests:

- Invalid `transaction.created` event is sent to `transaction.created.dlq`.
- Invalid `transaction.risk-scored` event is sent to `transaction.risk-scored.dlq`.
- DLQ payload contains original event and error metadata.
- Retry occurs before DLQ.

---

## 18.4 Idempotency Tests

Required tests:

- Duplicate `transaction.created` event does not create duplicate risk factors.
- Duplicate `transaction.risk-scored` event does not create duplicate Elasticsearch documents.
- Duplicate event is logged as `IGNORED`.

---

## 18.5 End-to-End Kafka Test

Required E2E flow:

```text
POST /api/v1/transactions
-> transaction.created published
-> Risk Engine consumes transaction.created
-> transaction status updated
-> transaction.risk-scored published
-> Audit Search consumes transaction.risk-scored
-> fraud-audit-events document indexed
-> GET /api/v1/audits/search returns indexed transaction
```

---

## 19. Acceptance Criteria

Kafka event processing is considered complete when:

1. `transaction.created` is published after successful transaction persistence.
2. `transaction.created` is consumed by Risk Engine Service.
3. Risk Engine Service updates transaction risk result.
4. Risk Engine Service publishes `transaction.risk-scored`.
5. `transaction.risk-scored` is consumed by Audit Search Service.
6. Audit Search Service indexes the audit document into Elasticsearch.
7. Retry works for temporary consumer failure.
8. DLQ receives failed events after retry exhaustion.
9. Duplicate events do not create duplicate side effects.
10. `kafka_event_logs` contains correct lifecycle records.
11. Kafka events use the common envelope.
12. Kafka message keys use `transactionRef`.
13. Kafka-related logs include traceable identifiers.

---

## 20. Traceability Matrix

| Kafka Contract Area | Related PRD / Design Area |
|---|---|
| `transaction.created` | MVP-01 Transaction Submission, MVP-03 Event-Based Transaction Processing |
| `transaction.risk-scored` | MVP-02 Risk Scoring Engine, MVP-03 Event-Based Transaction Processing, MVP-05 Elasticsearch Audit Search |
| DLQ Topics | MVP-03 Event-Based Transaction Processing |
| Event Envelope | TRD Common Event Contract |
| Event Log Mapping | Database Schema `kafka_event_logs` |
| Kafka Key Strategy | System Design Kafka Event Design |
| Consumer Idempotency | TRD Idempotency Principle |
| Audit Indexing Event | Elasticsearch Audit Search |
| Risk Scoring Event | Risk Engine Service |
