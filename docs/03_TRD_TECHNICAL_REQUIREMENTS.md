# Technical Requirements Document
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This Technical Requirements Document defines the implementation-level technical requirements for **BankGuard**, a real-time fraud detection and transaction intelligence platform.

This document translates the Product Requirements Document and System Design and Architecture Specification into concrete engineering requirements that can be followed during implementation.

The TRD focuses on:

- Service-level technical behavior.
- API implementation rules.
- DTO and validation requirements.
- Kafka event contracts.
- Redis cache and counter contracts.
- PostgreSQL migration and repository requirements.
- Elasticsearch indexing and search requirements.
- Security implementation requirements.
- Error handling requirements.
- Testing requirements.
- Local deployment requirements.
- End-to-end verification scenarios.

### 1.2 Source Documents

This TRD is aligned with:

- `PRD.md`
- `System_Design_Architecture.md`

### 1.3 Scope

This document covers the MVP implementation of BankGuard:

1. Transaction Submission.
2. Risk Scoring Engine.
3. Event-Based Transaction Processing.
4. Redis Risk Cache and Real-Time Risk Counters.
5. Elasticsearch Audit Search.
6. Transaction Investigation API.
7. Native SQL Reporting.
8. Security.
9. Reliability and Failure Handling.
10. Containerized Local Deployment.
11. Testing and Verification.

### 1.4 Non-Scope

The following are not part of this TRD:

- Real money movement.
- Core banking integration.
- Payment gateway integration.
- Machine learning scoring.
- Analyst dashboard UI.
- Case management workflow.
- Multi-region deployment.
- Regulatory report integration.
- Production-grade distributed tracing platform.

---

## 2. Technical Principles

### 2.1 Source of Truth

PostgreSQL must be treated as the source of truth for:

- Customers.
- Accounts.
- Transactions.
- Risk factors.
- Risk profiles.
- Blacklisted accounts.
- Trusted devices.
- Rule configuration.
- Internal users and roles.
- Event processing logs.

Elasticsearch must not be treated as the authoritative state of a transaction. It is only a denormalized read model for search and investigation.

Redis must not be treated as durable storage. It is used only for cacheable risk reference data and short-lived transaction velocity counters.

### 2.2 Event-Driven Processing

Transaction submission must not synchronously execute fraud scoring.

The transaction flow must follow this pattern:

1. Transaction Service stores the transaction.
2. Transaction Service publishes `transaction.created`.
3. Risk Engine Service consumes `transaction.created`.
4. Risk Engine Service calculates risk score.
5. Risk Engine Service stores risk result and risk factors.
6. Risk Engine Service publishes `transaction.risk-scored`.
7. Audit Search Service consumes `transaction.risk-scored`.
8. Audit Search Service indexes audit document into Elasticsearch.

### 2.3 Idempotency

Transaction submission must be idempotent.

Any request with the same `Idempotency-Key` must not create more than one transaction record.

Kafka consumers must also be idempotent:

- Risk Engine Service must avoid re-scoring a transaction that has already reached a final decision.
- Audit Search Service must use `transactionRef` as the Elasticsearch document ID so repeated event processing updates the same document instead of creating duplicates.

### 2.4 Explainability

Every risk decision must be explainable through stored risk factors.

A transaction decision without risk factors is invalid except when the risk score is zero and no fraud rule is triggered.

### 2.5 Consistent API Behavior

All APIs must follow the same standards:

- JSON request and response.
- ISO-8601 UTC datetime format.
- Structured error response.
- Role-based authorization.
- Sensitive account number masking.
- Pagination for list/search/report endpoints.
- Clear validation errors.

---

## 3. MVP Traceability Matrix

| PRD MVP | Technical Implementation Area | Required Output |
|---|---|---|
| MVP-01 Transaction Submission | Transaction Service, PostgreSQL, Kafka Producer, Idempotency | Transaction stored and `transaction.created` published |
| MVP-02 Risk Scoring Engine | Risk Engine Service, RiskRule interface, Redis lookup, PostgreSQL update | Risk score, decision, and risk factors stored |
| MVP-03 Event-Based Transaction Processing | Kafka topics, event envelope, retry, DLQ | Async processing with failure isolation |
| MVP-04 Redis Risk Cache and Real-Time Risk Counters | Redis cache-aside, TTL, velocity counters, invalidation | Cache hit/miss behavior and real-time velocity checks |
| MVP-05 Elasticsearch Audit Search | Audit Search Service, Elasticsearch document, search API | Searchable fraud audit documents |
| MVP-06 Transaction Investigation API | Transaction detail API and audit search API | Fraud analyst can review risk decision and factors |
| MVP-07 Native SQL Reporting | Native SQL repositories and reporting APIs | Aggregated fraud and risk reports |

---

## 4. Technical Stack Requirements

### 4.1 Runtime and Language

| Component | Requirement |
|---|---|
| Java | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Build Tool | Maven |
| API Style | REST JSON |
| Database Migration | Flyway |
| Authentication | JWT |
| Local Deployment | Docker Compose |

### 4.2 Spring Dependencies

Each service must include only the dependencies it needs.

Common dependencies:

- Spring Boot Starter Web.
- Spring Boot Starter Validation.
- Spring Boot Starter Security.
- Spring Boot Starter Actuator.
- Lombok.
- MapStruct or manual mapper.
- Springdoc OpenAPI.

Transaction Service:

- Spring Data JPA.
- PostgreSQL Driver.
- Flyway.
- Spring Kafka.

Risk Engine Service:

- Spring Data JPA.
- PostgreSQL Driver.
- Spring Kafka.
- Spring Data Redis.
- Flyway if it owns migrations.

Audit Search Service:

- Spring Kafka.
- Spring Data Elasticsearch.
- Spring Boot Starter Validation.

Master Data Service:

- Spring Data JPA.
- PostgreSQL Driver.
- Spring Data Redis.
- Flyway if it owns migrations.

Testing:

- JUnit 5.
- Mockito.
- AssertJ.
- Spring Boot Test.
- Testcontainers PostgreSQL.
- Testcontainers Kafka.
- Testcontainers Redis-compatible container.
- Testcontainers Elasticsearch.

---

## 5. Repository and Module Requirements

### 5.1 Repository Structure

The repository must use this structure:

```text
bankguard/
├── docker-compose.yml
├── README.md
├── docs/
│   ├── PRD.md
│   ├── SYSTEM_DESIGN_ARCHITECTURE.md
│   ├── TRD.md
│   ├── API_CONTRACT.md
│   ├── KAFKA_EVENT_CONTRACT.md
│   ├── REDIS_KEY_CONTRACT.md
│   └── DEMO_SCENARIOS.md
├── common-library/
├── transaction-service/
├── risk-engine-service/
├── audit-search-service/
└── master-data-service/
```

### 5.2 Common Library

A `common-library` module should contain shared objects that must remain consistent across services.

Required shared components:

- `EventEnvelope<T>`
- Common error response DTO.
- Common pagination response DTO.
- Common constants for transaction statuses.
- Common constants for risk decisions.
- Common constants for Kafka topic names.
- Common masking utility.
- Common correlation ID filter.
- Common exception model.

The common library must not contain service-specific business logic.

### 5.3 Package Convention

Each service must follow this package convention:

```text
com.bankguard.<service_name>
├── config
├── controller
├── dto
│   ├── request
│   ├── response
│   └── event
├── entity
├── repository
├── service
├── mapper
├── exception
├── security
└── util
```

Risk Engine Service may include:

```text
com.bankguard.riskengine.rule
com.bankguard.riskengine.consumer
com.bankguard.riskengine.cache
```

Audit Search Service may include:

```text
com.bankguard.auditsearch.document
com.bankguard.auditsearch.consumer
com.bankguard.auditsearch.search
```

---

## 6. Common API Requirements

### 6.1 API Versioning

All public/internal REST endpoints must use:

```text
/api/v1
```

### 6.2 Datetime Format

All datetime fields must use ISO-8601 UTC format:

```text
2026-06-04T10:00:00Z
```

Java requirement:

```java
OffsetDateTime
```

or

```java
Instant
```

### 6.3 Common Success List Response

All paginated list/search/report responses must follow this format:

```json
{
  "data": [],
  "page": 0,
  "size": 20,
  "total": 0
}
```

### 6.4 Common Error Response

All errors must follow this format:

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
  ],
  "path": "/api/v1/transactions",
  "requestId": "req-001"
}
```

### 6.5 Required Headers

For protected APIs:

```http
Authorization: Bearer <jwt>
```

For transaction submission:

```http
Idempotency-Key: <unique-request-key>
```

For request tracing:

```http
X-Request-Id: <request-id>
```

If `X-Request-Id` is not supplied, the service must generate one.

### 6.6 Account Number Masking

All account numbers returned in API responses and Elasticsearch documents must be masked.

Masking rule:

```text
If account number length >= 7:
    keep first 3 characters
    keep last 3 characters
    replace middle characters with ****

Example:
1234567890 -> 123****890
```

If account number length is less than 7, mask all except the last 2 characters.

---

## 7. Transaction Service Technical Requirements

### 7.1 Responsibility

Transaction Service implements:

- Transaction submission.
- Idempotency handling.
- Source account validation.
- Transaction reference generation.
- Transaction persistence.
- `transaction.created` Kafka publishing.
- Transaction detail API.
- Reporting APIs using native SQL.

### 7.2 Required Controllers

```text
TransactionController
ReportController
AuthController
```

For MVP, `AuthController` may issue JWT tokens for predefined internal users.

### 7.3 Required Services

```text
TransactionApplicationService
TransactionValidationService
AccountLookupService
IdempotencyService
TransactionReferenceGenerator
TransactionEventPublisher
TransactionQueryService
ReportService
```

### 7.4 Required Repositories

```text
CustomerRepository
AccountRepository
TransactionRepository
TransactionRiskFactorRepository
ReportNativeQueryRepository
UserRepository
RoleRepository
```

### 7.5 Submit Transaction API

Endpoint:

```http
POST /api/v1/transactions
```

Required headers:

```http
Authorization: Bearer <jwt>
Idempotency-Key: unique-request-key
```

Request DTO:

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

Response DTO:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "status": "PENDING_RISK_CHECK",
  "message": "Transaction submitted for risk scoring"
}
```

### 7.6 Submit Transaction Validation Rules

| Field | Requirement | Error Code |
|---|---|---|
| sourceAccountNumber | Required, non-blank | VALIDATION_ERROR |
| destinationAccountNumber | Required, non-blank | VALIDATION_ERROR |
| amount | Required, greater than 0 | VALIDATION_ERROR |
| currency | Required, default IDR allowed | VALIDATION_ERROR |
| channel | Must be one of allowed channels | INVALID_CHANNEL |
| deviceId | Optional but recommended | - |
| ipAddress | Optional but must be valid IP if present | INVALID_IP_ADDRESS |
| location | Optional but recommended | - |
| Idempotency-Key | Required | IDEMPOTENCY_KEY_REQUIRED |
| source account | Must exist | SOURCE_ACCOUNT_NOT_FOUND |
| source account status | Must be ACTIVE | SOURCE_ACCOUNT_INACTIVE |

Allowed channels:

```text
MOBILE_BANKING
INTERNET_BANKING
ATM
BRANCH
BACK_OFFICE
```

### 7.7 Transaction Creation Behavior

The service must execute this flow:

```text
1. Validate JWT and role.
2. Validate Idempotency-Key header.
3. Check whether idempotency key already exists.
4. If exists, return existing transaction response.
5. Validate request body.
6. Find source account by account number.
7. Verify source account status is ACTIVE.
8. Generate transaction reference.
9. Store transaction with PENDING_RISK_CHECK status.
10. Publish transaction.created event.
11. Store kafka_event_logs record with PUBLISHED status.
12. Return transaction reference to caller.
```

### 7.8 Transaction Reference Format

Transaction reference format:

```text
TRX-yyyyMMdd-000001
```

Example:

```text
TRX-20260604-000001
```

Requirements:

- Must be unique.
- Must be human-readable.
- Must be sortable by date prefix.
- Sequence may be database-based for MVP.

### 7.9 Transaction Status Values

Allowed values:

```text
PENDING_RISK_CHECK
APPROVED
REVIEW
BLOCKED
FAILED
```

Transaction Service may create only:

```text
PENDING_RISK_CHECK
FAILED
```

Risk Engine Service is responsible for setting:

```text
APPROVED
REVIEW
BLOCKED
```

### 7.10 Transaction Detail API

Endpoint:

```http
GET /api/v1/transactions/{transactionRef}
```

Response must include:

- transactionRef.
- masked source account number.
- masked destination account number.
- amount.
- currency.
- channel.
- status.
- riskScore.
- riskDecision.
- riskFactors.
- createdAt.
- updatedAt.

If not found:

```http
404 TRANSACTION_NOT_FOUND
```

### 7.11 Reporting APIs

Transaction Service must expose these reporting APIs:

```text
GET /api/v1/reports/high-risk-transactions
GET /api/v1/reports/transaction-velocity
GET /api/v1/reports/top-risk-customers
GET /api/v1/reports/suspicious-destination-accounts
GET /api/v1/reports/daily-fraud-trend
GET /api/v1/reports/risk-score-distribution
GET /api/v1/reports/daily-top-risky-customers
```

All report endpoints must support:

- `startDate`
- `endDate`
- pagination where applicable
- role access for `ROLE_ADMIN` and `ROLE_FRAUD_ANALYST`

---

## 8. Risk Engine Service Technical Requirements

### 8.1 Responsibility

Risk Engine Service implements:

- Kafka consumer for `transaction.created`.
- Transaction context loading.
- Redis cache lookup.
- Redis velocity counter update.
- Risk rule execution.
- Risk score calculation.
- Decision mapping.
- Risk factor persistence.
- Transaction status update.
- Kafka producer for `transaction.risk-scored`.

### 8.2 Required Components

```text
TransactionCreatedConsumer
RiskScoringApplicationService
TransactionContextBuilder
RiskRuleRegistry
RiskDecisionService
RiskFactorPersistenceService
TransactionRiskUpdateService
RiskScoredEventPublisher
RedisRiskCacheService
RedisVelocityCounterService
EventProcessingLogService
```

### 8.3 RiskRule Interface

All risk rules must implement:

```java
public interface RiskRule {
    String code();
    boolean active();
    RiskFactor evaluate(TransactionContext context);
}
```

### 8.4 RiskFactor Model

Risk factor must include:

```java
public record RiskFactor(
    String code,
    String description,
    int score,
    boolean triggered,
    Map<String, Object> metadata
) {}
```

### 8.5 TransactionContext Model

Transaction context must include:

```java
public record TransactionContext(
    String transactionRef,
    Long customerId,
    String cifNumber,
    String sourceAccountNumber,
    String destinationAccountNumber,
    BigDecimal amount,
    String currency,
    String channel,
    String deviceId,
    String ipAddress,
    String location,
    String customerRiskLevel,
    boolean destinationBlacklisted,
    boolean trustedDevice,
    boolean knownLocation,
    int velocityCount10m,
    BigDecimal velocityAmount10m,
    Map<String, RiskRuleConfig> ruleConfigs
) {}
```

### 8.6 Rule Implementations

Required rules:

#### 8.6.1 HIGH_AMOUNT

Triggered when:

```text
transaction.amount >= configured threshold
```

Default threshold:

```text
10,000,000 IDR
```

Default score:

```text
25
```

#### 8.6.2 NEW_DEVICE

Triggered when:

```text
deviceId is present AND device is not trusted for the customer
```

Default score:

```text
20
```

If `deviceId` is null, this rule must not trigger by default.

#### 8.6.3 BLACKLISTED_DESTINATION

Triggered when:

```text
destinationAccountNumber exists in active blacklisted_accounts
```

Default score:

```text
50
```

#### 8.6.4 HIGH_FREQUENCY_TRANSACTION

Triggered when:

```text
velocityCount10m >= configured count threshold
OR velocityAmount10m >= configured amount threshold
```

Default thresholds:

```text
count threshold: 5 transactions
amount threshold: 50,000,000 IDR
```

Default score:

```text
30
```

#### 8.6.5 UNUSUAL_LOCATION

Triggered when:

```text
location is present AND location does not exist in customer_locations
```

Default score:

```text
20
```

If `location` is null, this rule must not trigger by default.

#### 8.6.6 HIGH_RISK_CUSTOMER_PROFILE

Triggered when:

```text
customer risk level = HIGH
```

Default score:

```text
25
```

### 8.7 Rule Execution Flow

Risk Engine must execute rules using Java Stream:

```java
List<RiskFactor> triggeredFactors = riskRules.stream()
    .filter(RiskRule::active)
    .map(rule -> rule.evaluate(context))
    .filter(RiskFactor::triggered)
    .toList();

int totalScore = triggeredFactors.stream()
    .mapToInt(RiskFactor::score)
    .sum();
```

### 8.8 Decision Mapping

Risk decision must be mapped as:

```text
score < 50       -> APPROVED
50 <= score < 80 -> REVIEW
score >= 80      -> BLOCKED
```

### 8.9 Risk Engine Consumer Flow

```text
1. Receive transaction.created event.
2. Validate event envelope.
3. Check kafka_event_logs by eventId.
4. Skip if event already consumed.
5. Load transaction by transactionRef.
6. Skip if transaction is already APPROVED, REVIEW, or BLOCKED.
7. Build transaction context.
8. Read blacklist, risk profile, trusted device, and rule config using Redis cache-aside.
9. Update Redis velocity counters.
10. Execute active risk rules.
11. Calculate total score.
12. Determine decision.
13. Store risk factors.
14. Update transaction status, risk_score, and risk_decision.
15. Store event log as CONSUMED.
16. Publish transaction.risk-scored.
17. Acknowledge Kafka message.
```

### 8.10 Database Transaction Boundary

The following operations must run in one database transaction:

```text
Store risk factors.
Update transaction risk result.
Update event processing log.
```

Kafka message acknowledgment must happen only after the database transaction is committed and the next event is published or queued for publishing.

For MVP, direct Kafka publish after database commit is acceptable.

### 8.11 Duplicate Event Handling

If `transaction.created` is received again for a transaction that is already scored:

- Do not insert duplicate risk factors.
- Do not update risk score again.
- Do not publish duplicate `transaction.risk-scored` unless required for recovery.
- Log the event as duplicate or ignored.

---

## 9. Audit Search Service Technical Requirements

### 9.1 Responsibility

Audit Search Service implements:

- Kafka consumer for `transaction.risk-scored`.
- Audit document builder.
- Account masking before indexing.
- Elasticsearch document indexing.
- Fraud audit search API.
- Idempotent reprocessing using `transactionRef` as document ID.

### 9.2 Required Components

```text
RiskScoredConsumer
FraudAuditDocumentBuilder
FraudAuditIndexer
FraudAuditSearchService
FraudAuditSearchController
ElasticsearchIndexInitializer
EventProcessingLogService
```

### 9.3 Elasticsearch Document ID

The document ID must be:

```text
transactionRef
```

This ensures repeated processing updates the same document.

### 9.4 Fraud Audit Document

Required fields:

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
  "riskFactors": ["HIGH_AMOUNT", "NEW_DEVICE"],
  "createdAt": "2026-06-04T10:00:00Z",
  "scoredAt": "2026-06-04T10:00:03Z"
}
```

### 9.5 Index Creation Requirement

On service startup, Audit Search Service must ensure that the `fraud-audit-events` index exists.

If the index does not exist, create it with the required mapping.

### 9.6 Search API

Endpoint:

```http
GET /api/v1/audits/search
```

Supported query parameters:

| Parameter | Type | Required |
|---|---|---|
| keyword | string | no |
| transactionRef | string | no |
| customerCif | string | no |
| decision | string | no |
| minimumRiskScore | integer | no |
| riskFactor | string | no |
| channel | string | no |
| location | string | no |
| startDate | date | no |
| endDate | date | no |
| page | integer | no |
| size | integer | no |

Default pagination:

```text
page = 0
size = 20
```

Maximum size:

```text
100
```

### 9.7 Search Behavior

- `keyword` must search across customer name, location, transaction reference, and risk factor.
- `decision` must use exact match.
- `minimumRiskScore` must use range query.
- `startDate` and `endDate` must filter `createdAt`.
- Results must be sorted by `createdAt DESC`.

---

## 10. Master Data Service Technical Requirements

### 10.1 Responsibility

Master Data Service implements:

- Blacklisted account management.
- Trusted device registration.
- Customer risk profile management.
- Risk rule configuration retrieval.
- Redis cache invalidation.

### 10.2 Required Components

```text
BlacklistedAccountController
TrustedDeviceController
CustomerRiskProfileController
RiskRuleConfigController
BlacklistedAccountService
TrustedDeviceService
CustomerRiskProfileService
RiskRuleConfigService
RedisCacheInvalidationService
```

### 10.3 Blacklisted Account APIs

Required endpoints:

```text
POST /api/v1/blacklisted-accounts
GET /api/v1/blacklisted-accounts
GET /api/v1/blacklisted-accounts/{id}
PUT /api/v1/blacklisted-accounts/{id}
DELETE /api/v1/blacklisted-accounts/{id}
```

Delete behavior:

- Do not physically delete for MVP.
- Set `active = false`.

Cache invalidation:

```text
DELETE blacklist:account:{accountNumber}
```

### 10.4 Trusted Device APIs

Required endpoints:

```text
POST /api/v1/customers/{customerId}/trusted-devices
GET /api/v1/customers/{customerId}/trusted-devices
PUT /api/v1/customers/{customerId}/trusted-devices/{deviceId}
```

Cache invalidation:

```text
DELETE customer:trusted-device:{customerId}:{deviceId}
```

### 10.5 Customer Risk Profile APIs

Required endpoints:

```text
GET /api/v1/customers/{customerId}/risk-profile
PUT /api/v1/customers/{customerId}/risk-profile
```

Allowed values:

```text
LOW
MEDIUM
HIGH
```

Cache invalidation:

```text
DELETE customer:risk-profile:{customerId}
```

### 10.6 Risk Rule Config APIs

Required endpoints:

```text
GET /api/v1/risk-rules
GET /api/v1/risk-rules/{ruleCode}
PUT /api/v1/risk-rules/{ruleCode}
```

Cache invalidation:

```text
DELETE risk-rule:config:{ruleCode}
```

---

## 11. Kafka Technical Requirements

### 11.1 Topic Requirements

Required topics:

```text
transaction.created
transaction.risk-scored
transaction.created.dlq
transaction.risk-scored.dlq
```

### 11.2 Event Envelope

All events must use:

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

Required envelope fields:

| Field | Requirement |
|---|---|
| eventId | Unique UUID |
| eventType | Required |
| eventVersion | Required, default `1.0` |
| aggregateId | Transaction reference |
| occurredAt | ISO-8601 UTC |
| producer | Service name |
| payload | Event-specific payload |

### 11.3 transaction.created Contract

Producer:

```text
transaction-service
```

Consumer:

```text
risk-engine-service
```

Kafka key:

```text
transactionRef
```

Event type:

```text
TRANSACTION_CREATED
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

### 11.4 transaction.risk-scored Contract

Producer:

```text
risk-engine-service
```

Consumer:

```text
audit-search-service
```

Kafka key:

```text
transactionRef
```

Event type:

```text
TRANSACTION_RISK_SCORED
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
    }
  ],
  "scoredAt": "2026-06-04T10:00:03Z"
}
```

### 11.5 Producer Requirements

- Producer must use `transactionRef` as the Kafka message key.
- Producer must log event publishing attempt.
- Producer must store event log with status `PUBLISHED` after successful send.
- Failed publish must return controlled error or be logged for retry depending on flow.

### 11.6 Consumer Requirements

- Auto commit must be disabled.
- Manual acknowledgment must be enabled.
- Event schema must be validated before processing.
- Consumer must log `eventId`, `transactionRef`, `topic`, and `consumerGroup`.
- Consumer must retry failed processing up to 3 attempts.
- Consumer must send failed events to DLQ after retry exhaustion.
- Consumer must be idempotent.

### 11.7 DLQ Payload Requirement

DLQ message must include:

```json
{
  "originalTopic": "transaction.created",
  "originalPayload": {},
  "errorMessage": "Failed to process event",
  "failedAt": "2026-06-04T10:00:00Z",
  "consumerGroup": "risk-engine-service"
}
```

---

## 12. Redis Technical Requirements

### 12.1 Redis Value Format

Redis values must be stored as JSON strings.

Example:

```json
{
  "accountNumber": "9876543210",
  "active": true,
  "reason": "Reported mule account"
}
```

### 12.2 Cache Keys

| Key | Value | TTL |
|---|---|---:|
| `blacklist:account:{accountNumber}` | blacklist lookup result | 10 minutes |
| `customer:risk-profile:{customerId}` | customer risk profile | 5 minutes |
| `customer:trusted-device:{customerId}:{deviceId}` | trusted device lookup result | 10 minutes |
| `risk-rule:config:{ruleCode}` | rule configuration | 5 minutes |

### 12.3 Counter Keys

| Key | Value | TTL |
|---|---|---:|
| `risk:velocity:count:{sourceAccountNumber}:10m` | integer count | 10 minutes |
| `risk:velocity:amount:{sourceAccountNumber}:10m` | decimal amount | 10 minutes |

### 12.4 Cache-Aside Requirement

Each cacheable lookup must follow:

```text
Check Redis.
If found, return cached data.
If not found, query PostgreSQL.
Store result in Redis with TTL.
Return result.
```

### 12.5 Negative Cache Requirement

For blacklist lookup, Redis should also cache negative lookup results for a shorter TTL.

Example:

```text
blacklist:account:1234567890 = {"active": false}
TTL = 2 minutes
```

This prevents repeated database queries for non-blacklisted accounts.

### 12.6 Velocity Counter Requirement

When Risk Engine processes a transaction:

```text
INCR risk:velocity:count:{sourceAccountNumber}:10m
INCRBYFLOAT risk:velocity:amount:{sourceAccountNumber}:10m {amount}
EXPIRE both keys to 600 seconds if newly created
```

Counter values must be read into `TransactionContext`.

### 12.7 Cache Invalidation Requirement

Master Data Service must invalidate cache only after successful PostgreSQL update.

---

## 13. Elasticsearch Technical Requirements

### 13.1 Index Name

```text
fraud-audit-events
```

### 13.2 Index Mapping

Audit Search Service must create this mapping if the index does not exist:

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

### 13.3 Indexing Requirement

When `transaction.risk-scored` is consumed:

1. Build audit document.
2. Mask source account number.
3. Mask destination account number.
4. Use transactionRef as document ID.
5. Upsert document to Elasticsearch.
6. Log indexing result.

### 13.4 Search Query Requirement

The search API must support:

- Full-text keyword query.
- Exact decision filter.
- Exact customer CIF filter.
- Exact risk factor filter.
- Minimum risk score range filter.
- Date range filter.
- Pagination.
- Sorting by `createdAt DESC`.

---

## 14. PostgreSQL Migration Requirements

### 14.1 Migration Tool

Flyway must be used for database migrations.

### 14.2 Migration File Order

Required migration files:

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
V11__create_users_roles_tables.sql
V12__seed_initial_data.sql
```

### 14.3 Seed Data Requirement

Initial seed data must include:

- At least 3 customers.
- At least 3 accounts.
- At least 1 high-risk customer profile.
- At least 1 blacklisted account.
- At least 1 trusted device.
- All initial risk rule configurations.
- Internal users for ADMIN, BACKOFFICE, and FRAUD_ANALYST roles.

### 14.4 Required Indexes

All indexes defined in the System Design must be created through migrations.

Additional required indexes:

```sql
CREATE INDEX idx_transactions_created_status_risk
ON transactions(created_at, status, risk_score);

CREATE INDEX idx_transactions_destination_created
ON transactions(destination_account_number, created_at);

CREATE INDEX idx_transactions_source_created
ON transactions(source_account_id, created_at);
```

---

## 15. Native SQL Reporting Technical Requirements

### 15.1 Repository Requirement

Native SQL reports must be implemented in:

```text
ReportNativeQueryRepository
```

Projection interfaces or DTO mapping must be used for report responses.

### 15.2 Report Endpoints

Required endpoints:

```text
GET /api/v1/reports/high-risk-transactions
GET /api/v1/reports/transaction-velocity
GET /api/v1/reports/top-risk-customers
GET /api/v1/reports/suspicious-destination-accounts
GET /api/v1/reports/daily-fraud-trend
GET /api/v1/reports/risk-score-distribution
GET /api/v1/reports/daily-top-risky-customers
```

### 15.3 Common Query Parameters

| Parameter | Required | Default |
|---|---:|---|
| startDate | Yes | - |
| endDate | Yes | - |
| page | No | 0 |
| size | No | 20 |

Report-specific parameters:

| Report | Parameter |
|---|---|
| high-risk-transactions | minimumRiskScore |
| transaction-velocity | minimumCount, minimumAmount |
| top-risk-customers | minimumAverageRiskScore |
| suspicious-destination-accounts | minimumUniqueSenders, minimumTotalAmount |
| daily-top-risky-customers | topN |

### 15.4 Performance Requirement

Reports must:

- Use indexed columns in filters.
- Require date range filter.
- Avoid unbounded full-table scan for API calls.
- Support pagination where row result can be large.

---

## 16. Security Technical Requirements

### 16.1 Authentication

JWT authentication must be implemented.

For MVP:

- Static seeded users are allowed.
- Login endpoint may issue JWT.
- Password must be stored as BCrypt hash.
- JWT must include username and roles.

### 16.2 Login API

Endpoint:

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "username": "admin",
  "password": "password"
}
```

Response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 16.3 Authorization Matrix

| Endpoint | ROLE_ADMIN | ROLE_BACKOFFICE | ROLE_FRAUD_ANALYST |
|---|---:|---:|---:|
| POST /api/v1/transactions | Yes | Yes | No |
| GET /api/v1/transactions/{ref} | Yes | Yes | Yes |
| GET /api/v1/audits/search | Yes | No | Yes |
| GET /api/v1/reports/** | Yes | No | Yes |
| POST /api/v1/blacklisted-accounts | Yes | No | No |
| PUT /api/v1/blacklisted-accounts/{id} | Yes | No | No |
| DELETE /api/v1/blacklisted-accounts/{id} | Yes | No | No |
| POST /api/v1/customers/{id}/trusted-devices | Yes | No | No |
| PUT /api/v1/customers/{id}/risk-profile | Yes | No | No |
| GET /api/v1/risk-rules | Yes | No | No |
| PUT /api/v1/risk-rules/{ruleCode} | Yes | No | No |

### 16.4 Unauthorized Response

Missing or invalid token:

```http
401 UNAUTHORIZED
```

### 16.5 Forbidden Response

Valid token but insufficient role:

```http
403 FORBIDDEN
```

---

## 17. Error Handling Requirements

### 17.1 Error Code Matrix

| Scenario | HTTP Status | Error Code |
|---|---:|---|
| Request validation failed | 400 | VALIDATION_ERROR |
| Missing idempotency key | 400 | IDEMPOTENCY_KEY_REQUIRED |
| Invalid channel | 400 | INVALID_CHANNEL |
| Invalid IP address | 400 | INVALID_IP_ADDRESS |
| Source account not found | 404 | SOURCE_ACCOUNT_NOT_FOUND |
| Transaction not found | 404 | TRANSACTION_NOT_FOUND |
| Customer not found | 404 | CUSTOMER_NOT_FOUND |
| Blacklisted account not found | 404 | BLACKLISTED_ACCOUNT_NOT_FOUND |
| Source account inactive | 409 | SOURCE_ACCOUNT_INACTIVE |
| Duplicate idempotency key | 200 | IDEMPOTENT_RESPONSE |
| Invalid credentials | 401 | INVALID_CREDENTIALS |
| Missing token | 401 | UNAUTHORIZED |
| Insufficient role | 403 | FORBIDDEN |
| Kafka publish failed | 500 | EVENT_PUBLISH_FAILED |
| Elasticsearch indexing failed | 500 | AUDIT_INDEXING_FAILED |
| Unexpected error | 500 | INTERNAL_SERVER_ERROR |

### 17.2 Exception Handling

Each service must implement global exception handling using:

```java
@RestControllerAdvice
```

### 17.3 Error Logging

Every server-side error must log:

- requestId.
- service name.
- path.
- error code.
- exception message.
- stack trace for internal logs.
- transactionRef if available.
- eventId if available.

---

## 18. Reliability Technical Requirements

### 18.1 Kafka Retry

Consumer retry policy:

```text
Retry attempts: 3
Backoff: exponential
DLQ: enabled
Manual ack: enabled
```

### 18.2 DLQ Topics

```text
transaction.created.dlq
transaction.risk-scored.dlq
```

### 18.3 Event Log Requirement

Each produced or consumed event must create/update a row in `kafka_event_logs`.

Statuses:

```text
PUBLISHED
CONSUMED
FAILED
DLQ
IGNORED_DUPLICATE
```

### 18.4 Recovery Requirement

For MVP, DLQ messages may be reviewed manually through logs and Kafka tooling.

No automated DLQ reprocessing endpoint is required.

### 18.5 Graceful Failure

If Risk Engine fails to process a transaction event:

- Do not acknowledge the Kafka message.
- Retry according to policy.
- Send to DLQ after retry exhaustion.
- Mark event log as FAILED or DLQ.

If Audit Search fails to index a document:

- Retry according to policy.
- Send to DLQ after retry exhaustion.
- Do not modify PostgreSQL transaction state.

---

## 19. Observability Technical Requirements

### 19.1 Logging Format

Logs should use structured JSON format.

Required fields:

```json
{
  "timestamp": "2026-06-04T10:00:00Z",
  "level": "INFO",
  "service": "risk-engine-service",
  "requestId": "req-001",
  "eventId": "evt-001",
  "transactionRef": "TRX-20260604-000001",
  "message": "Risk scoring completed"
}
```

### 19.2 Correlation ID

The following IDs must be propagated where applicable:

- `requestId`
- `eventId`
- `transactionRef`
- `idempotencyKey`

### 19.3 Actuator

Each service must expose:

```http
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

### 19.4 Required Metrics

Recommended service metrics:

- transaction submissions count.
- transaction submission latency.
- Kafka publish success count.
- Kafka publish failure count.
- Kafka consumer processing duration.
- risk scoring duration.
- risk decision count by type.
- Redis cache hit count.
- Redis cache miss count.
- Elasticsearch indexing success count.
- Elasticsearch indexing failure count.
- DLQ count.

---

## 20. Docker and Local Deployment Requirements

### 20.1 Docker Compose Services

Required services:

```text
postgres
redis
kafka
elasticsearch
transaction-service
risk-engine-service
audit-search-service
master-data-service
```

### 20.2 Required Environment Variables

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

### 20.3 Startup Requirement

A clean local environment must be able to start with:

```bash
docker compose up --build
```

### 20.4 Health Verification

After startup:

- PostgreSQL must accept connections.
- Redis must respond to ping.
- Kafka topics must be available.
- Elasticsearch health must be green or yellow.
- Each service `/actuator/health` must return UP.

---

## 21. Testing Technical Requirements

### 21.1 Unit Test Requirements

Transaction Service:

- valid transaction request.
- invalid amount.
- missing idempotency key.
- account not found.
- inactive account.
- duplicate idempotency key.
- account number masking.

Risk Engine Service:

- HIGH_AMOUNT rule.
- NEW_DEVICE rule.
- BLACKLISTED_DESTINATION rule.
- HIGH_FREQUENCY_TRANSACTION rule.
- UNUSUAL_LOCATION rule.
- HIGH_RISK_CUSTOMER_PROFILE rule.
- total score calculation.
- decision threshold mapping.
- duplicate event skip.

Audit Search Service:

- audit document builder.
- account masking before indexing.
- search request validation.

Master Data Service:

- blacklist create/update/deactivate.
- trusted device registration.
- risk profile update.
- cache invalidation trigger.

### 21.2 Integration Test Requirements

Required integration tests:

- PostgreSQL migration loads successfully.
- Transaction repository persists transaction.
- Kafka producer sends `transaction.created`.
- Risk Engine consumes `transaction.created`.
- Redis cache hit behavior.
- Redis cache miss and database fallback behavior.
- Redis velocity counter expiration.
- Elasticsearch index creation.
- Elasticsearch document indexing.
- Audit search filters.
- Native SQL reports.
- JWT authentication and authorization.

### 21.3 End-to-End Test Requirement

Required E2E scenario:

```text
1. Login as BACKOFFICE.
2. Submit transaction.
3. Verify transaction is stored as PENDING_RISK_CHECK.
4. Verify transaction.created event is published.
5. Risk Engine scores transaction.
6. Verify transaction status becomes APPROVED, REVIEW, or BLOCKED.
7. Verify risk factors are stored.
8. Verify transaction.risk-scored event is published.
9. Audit Search Service indexes audit document.
10. Login as FRAUD_ANALYST.
11. Search audit document.
12. Retrieve transaction detail.
```

### 21.4 Test Data Requirement

Test data must include:

- normal customer.
- high-risk customer.
- active account.
- inactive account.
- blacklisted destination account.
- trusted device.
- untrusted device.
- known location.
- unusual location.
- transactions that produce APPROVED, REVIEW, and BLOCKED decisions.

---

## 22. Demo Scenario Requirements

The project must include documented demo scenarios.

### 22.1 Approved Transaction Scenario

Input:

- normal amount.
- trusted device.
- known location.
- non-blacklisted destination.
- low-risk customer.

Expected:

```text
APPROVED
riskScore < 50
No or low risk factors
```

### 22.2 Review Transaction Scenario

Input:

- high amount.
- new device.

Expected:

```text
REVIEW
50 <= riskScore < 80
Risk factors include HIGH_AMOUNT and NEW_DEVICE
```

### 22.3 Blocked Transaction Scenario

Input:

- blacklisted destination.
- high amount.
- high-risk customer.

Expected:

```text
BLOCKED
riskScore >= 80
Risk factors include BLACKLISTED_DESTINATION
```

### 22.4 High Frequency Scenario

Input:

- multiple transactions from same source account within 10 minutes.

Expected:

```text
HIGH_FREQUENCY_TRANSACTION triggered
Redis velocity counter increased
```

### 22.5 Search Scenario

Input:

- scored transaction indexed in Elasticsearch.

Expected:

```text
Fraud analyst can search by transactionRef, decision, riskScore, riskFactor, and date range.
```

---

## 23. Documentation Requirements

The repository must include:

```text
README.md
docs/PRD.md
docs/SYSTEM_DESIGN_ARCHITECTURE.md
docs/TRD.md
docs/API_CONTRACT.md
docs/KAFKA_EVENT_CONTRACT.md
docs/REDIS_KEY_CONTRACT.md
docs/DEMO_SCENARIOS.md
```

### 23.1 README Requirements

README must include:

- Project overview.
- Architecture summary.
- Tech stack.
- How to run locally.
- How to run tests.
- Default users.
- Example API calls.
- Demo scenarios.
- Known limitations.

---

## 24. Final Acceptance Criteria

The BankGuard MVP implementation is considered technically complete when:

1. All services start successfully through Docker Compose.
2. PostgreSQL migrations run successfully.
3. Seed data is inserted.
4. JWT login works.
5. Transaction submission works with idempotency.
6. `transaction.created` is published.
7. Risk Engine consumes and scores transaction.
8. Redis cache and velocity counters are used during scoring.
9. Risk factors are stored in PostgreSQL.
10. `transaction.risk-scored` is published.
11. Audit Search Service indexes transaction into Elasticsearch.
12. Fraud audit search returns indexed transaction.
13. Transaction detail API returns risk result and risk factors.
14. Master Data Service updates invalidate Redis keys.
15. Native SQL reporting endpoints return expected data.
16. Kafka retry and DLQ behavior can be demonstrated.
17. Protected APIs enforce role-based authorization.
18. Unit, integration, and end-to-end tests cover the critical flow.
19. Logs include requestId, eventId, and transactionRef.
20. Documentation explains setup, usage, and demo scenarios.

