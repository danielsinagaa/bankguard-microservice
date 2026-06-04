# Test Scenarios
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the test scenarios for **BankGuard**.

The purpose of this document is to provide a complete validation plan for the MVP capabilities defined across:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`
- `06_KAFKA_EVENT_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`

This document is intended to verify that BankGuard works correctly from the API layer through database persistence, Kafka event processing, Redis cache and counters, Elasticsearch indexing, native SQL reporting, and security enforcement.

---

### 1.2 Scope

This document covers test scenarios for:

1. Authentication and authorization.
2. Transaction submission.
3. Idempotency behavior.
4. Transaction detail and investigation.
5. Risk scoring rules.
6. Kafka event publishing and consuming.
7. Kafka retry and DLQ handling.
8. Redis cache behavior.
9. Redis velocity counters.
10. Master data cache invalidation.
11. Elasticsearch audit indexing and search.
12. Native SQL reporting.
13. Database constraints and integrity.
14. Error handling.
15. Observability.
16. End-to-end transaction flow.
17. Degraded dependency behavior.

---

### 1.3 Non-Scope

This document does not cover:

- Frontend testing.
- Real banking core integration testing.
- Payment settlement testing.
- Regulatory reporting certification.
- Machine learning model validation.
- Production load testing at bank-scale volume.
- Multi-region disaster recovery testing.

---

## 2. Test Strategy

### 2.1 Test Levels

| Test Level | Purpose |
|---|---|
| Unit Test | Validate isolated business logic, validators, mappers, utilities, and risk rules. |
| Integration Test | Validate integration with PostgreSQL, Kafka, Redis, Elasticsearch, and Spring Security. |
| API Test | Validate REST endpoints against API Contract. |
| Event Test | Validate Kafka event payload, producer, consumer, retry, DLQ, and idempotency behavior. |
| End-to-End Test | Validate the complete transaction flow from submission to audit search. |
| Regression Test | Ensure existing behavior remains valid after changes. |
| Negative Test | Validate failures, invalid inputs, unauthorized access, and dependency failures. |

---

### 2.2 Required Test Tools

| Area | Recommended Tool |
|---|---|
| Unit Test | JUnit 5, Mockito, AssertJ |
| Spring Integration Test | Spring Boot Test |
| Database Integration | Testcontainers PostgreSQL |
| Kafka Integration | Testcontainers Kafka |
| Redis Integration | Testcontainers Redis-compatible container |
| Elasticsearch Integration | Testcontainers Elasticsearch |
| API Testing | RestAssured or MockMvc |
| Manual Verification | Postman or Swagger |
| Container Verification | Docker Compose |

---

### 2.3 Test Data Principle

Test data must be deterministic.

Every test should clearly define:

- required seed data
- request input
- expected database state
- expected API response
- expected Kafka event
- expected Redis key behavior
- expected Elasticsearch document
- expected logs or event processing records

Tests must not depend on random production-like data.

---

## 3. MVP Traceability Matrix

| PRD MVP | Test Coverage |
|---|---|
| MVP-01 Transaction Submission | Authentication, validation, account lookup, idempotency, transaction persistence, Kafka event publish |
| MVP-02 Risk Scoring Engine | Individual rule tests, score aggregation, decision threshold, risk factor persistence |
| MVP-03 Event-Based Transaction Processing | Kafka producer, consumer, event envelope validation, retry, DLQ, idempotent consumer |
| MVP-04 Redis Risk Cache and Real-Time Risk Counters | Cache hit, cache miss, cache fallback, TTL, invalidation, velocity counters |
| MVP-05 Elasticsearch Audit Search | Audit document indexing, masking, keyword search, structured filters |
| MVP-06 Transaction Investigation API | Transaction detail, audit search, risk factor retrieval, masking |
| MVP-07 Native SQL Reporting | High-risk reports, velocity reports, top-risk customers, suspicious destination accounts, trend reports, distribution reports |

---

## 4. Common Test Data

### 4.1 Customers

| Customer ID | CIF | Name | Status | Risk Level |
|---:|---|---|---|---|
| 1001 | CIF001 | Daniel Sinaga | ACTIVE | LOW |
| 1002 | CIF002 | Maria Hutabarat | ACTIVE | HIGH |
| 1003 | CIF003 | Budi Sitorus | SUSPENDED | MEDIUM |

---

### 4.2 Accounts

| Account ID | Customer ID | Account Number | Type | Currency | Status | Balance |
|---:|---:|---|---|---|---|---:|
| 2001 | 1001 | 1234567890 | SAVINGS | IDR | ACTIVE | 100000000 |
| 2002 | 1002 | 2234567890 | SAVINGS | IDR | ACTIVE | 200000000 |
| 2003 | 1003 | 3234567890 | SAVINGS | IDR | BLOCKED | 50000000 |

---

### 4.3 Blacklisted Accounts

| ID | Account Number | Active | Reason |
|---:|---|---|---|
| 3001 | 9876543210 | true | Reported mule account |
| 3002 | 8876543210 | false | Previously reviewed |

---

### 4.4 Trusted Devices

| Customer ID | Device ID | Device Name | Trusted |
|---:|---|---|---|
| 1001 | DEVICE-TRUSTED-001 | Daniel iPhone | true |
| 1001 | DEVICE-UNTRUSTED-001 | Unknown Android | false |

---

### 4.5 Known Locations

| Customer ID | Location | Usage Count |
|---:|---|---:|
| 1001 | Jakarta | 10 |
| 1001 | Batam | 3 |
| 1002 | Medan | 5 |

---

### 4.6 Risk Rule Configuration

| Rule Code | Threshold | Score | Active |
|---|---:|---:|---|
| HIGH_AMOUNT | 10000000 | 25 | true |
| NEW_DEVICE | null | 20 | true |
| BLACKLISTED_DESTINATION | null | 50 | true |
| HIGH_FREQUENCY_TRANSACTION_COUNT | 5 | 30 | true |
| HIGH_FREQUENCY_TRANSACTION_AMOUNT | 50000000 | 30 | true |
| UNUSUAL_LOCATION | null | 20 | true |
| HIGH_RISK_CUSTOMER_PROFILE | null | 25 | true |

---

## 5. Authentication and Authorization Test Scenarios

## AUTH-001 Login with Valid Admin Credential

### Objective

Verify that a valid admin user can obtain a JWT token.

### Precondition

- User `admin` exists.
- User has `ROLE_ADMIN`.

### Steps

1. Send `POST /api/v1/auth/login`.
2. Use username `admin`.
3. Use valid password.

### Expected Result

- HTTP status is `200 OK`.
- Response contains `accessToken`.
- Response contains `tokenType = Bearer`.
- Response contains `ROLE_ADMIN`.
- Token expiration is returned.

### Related Documents

- API Contract: Authentication API.
- Database Schema: `users`, `roles`, `user_roles`.

---

## AUTH-002 Login with Invalid Credential

### Objective

Verify that invalid credentials are rejected.

### Steps

1. Send `POST /api/v1/auth/login`.
2. Use invalid password.

### Expected Result

- HTTP status is `401 Unauthorized`.
- Error code is `UNAUTHORIZED`.
- Response follows common error format.

---

## AUTH-003 Access Protected Endpoint Without Token

### Objective

Verify that protected endpoints reject missing tokens.

### Steps

1. Send `GET /api/v1/transactions/TRX-20260604-000001`.
2. Do not include `Authorization` header.

### Expected Result

- HTTP status is `401 Unauthorized`.
- Error code is `UNAUTHORIZED`.

---

## AUTH-004 Fraud Analyst Cannot Submit Transaction

### Objective

Verify role-based access control.

### Precondition

- User has `ROLE_FRAUD_ANALYST`.

### Steps

1. Login as fraud analyst.
2. Send `POST /api/v1/transactions`.

### Expected Result

- HTTP status is `403 Forbidden`.
- Error code is `FORBIDDEN`.
- No transaction is created.
- No Kafka event is published.

---

## AUTH-005 Back Office Can Submit Transaction

### Objective

Verify that `ROLE_BACKOFFICE` can submit transactions.

### Precondition

- User has `ROLE_BACKOFFICE`.

### Steps

1. Login as back office user.
2. Submit a valid transaction.

### Expected Result

- HTTP status is `201 Created`.
- Transaction is stored with `PENDING_RISK_CHECK`.
- `transaction.created` event is published.

---

## 6. Transaction Submission Test Scenarios

## TXN-001 Submit Valid Transaction

### Objective

Verify successful transaction submission.

### Request

```json
{
  "sourceAccountNumber": "1234567890",
  "destinationAccountNumber": "5556667770",
  "amount": 5000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "deviceId": "DEVICE-TRUSTED-001",
  "ipAddress": "36.77.88.12",
  "location": "Jakarta"
}
```

### Steps

1. Login as `ROLE_BACKOFFICE`.
2. Send `POST /api/v1/transactions` with `Idempotency-Key`.
3. Query PostgreSQL `transactions`.
4. Check Kafka topic `transaction.created`.

### Expected Result

- HTTP status is `201 Created`.
- Response contains `transactionRef`.
- Transaction status is `PENDING_RISK_CHECK`.
- `idempotency_key` is stored.
- Kafka event `transaction.created` is published.
- `kafka_event_logs` contains a `PUBLISHED` record.

---

## TXN-002 Submit Transaction Without Idempotency Key

### Objective

Verify that transaction submission requires an idempotency key.

### Steps

1. Send valid transaction request without `Idempotency-Key`.

### Expected Result

- HTTP status is `400 Bad Request`.
- Error code is `IDEMPOTENCY_KEY_REQUIRED`.
- No transaction is created.
- No Kafka event is published.

---

## TXN-003 Submit Transaction with Negative Amount

### Objective

Verify amount validation.

### Request

```json
{
  "sourceAccountNumber": "1234567890",
  "destinationAccountNumber": "5556667770",
  "amount": -1000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING"
}
```

### Expected Result

- HTTP status is `400 Bad Request`.
- Error code is `VALIDATION_ERROR`.
- Error detail includes `amount`.
- No transaction is created.

---

## TXN-004 Submit Transaction with Invalid Channel

### Objective

Verify transaction channel validation.

### Request Field

```json
{
  "channel": "WHATSAPP_BANKING"
}
```

### Expected Result

- HTTP status is `400 Bad Request`.
- Error code is `INVALID_CHANNEL`.
- No transaction is created.

---

## TXN-005 Submit Transaction with Non-Existing Source Account

### Objective

Verify source account lookup.

### Request Field

```json
{
  "sourceAccountNumber": "0000000000"
}
```

### Expected Result

- HTTP status is `404 Not Found`.
- Error code is `SOURCE_ACCOUNT_NOT_FOUND`.
- No transaction is created.
- No Kafka event is published.

---

## TXN-006 Submit Transaction with Inactive Source Account

### Objective

Verify source account status validation.

### Precondition

- Source account exists with status `BLOCKED`.

### Expected Result

- HTTP status is `409 Conflict`.
- Error code is `SOURCE_ACCOUNT_INACTIVE`.
- No transaction is created.
- No Kafka event is published.

---

## TXN-007 Duplicate Idempotency Key Returns Existing Transaction

### Objective

Verify idempotent behavior for repeated requests.

### Steps

1. Submit a valid transaction with `Idempotency-Key: idem-001`.
2. Submit the same request again with `Idempotency-Key: idem-001`.
3. Count rows in `transactions`.

### Expected Result

- First request returns `201 Created`.
- Second request returns `200 OK`.
- Both responses return the same `transactionRef`.
- Only one transaction row exists.
- Only one `transaction.created` event is published for the new transaction.

---

## TXN-008 Same Idempotency Key with Different Payload

### Objective

Verify safe behavior when the same idempotency key is reused with different payload.

### Steps

1. Submit transaction with `Idempotency-Key: idem-002`.
2. Submit another transaction with same key but different amount.

### Expected Result

Recommended behavior:

- Service returns existing transaction response.
- No new transaction is created.
- A warning is logged for idempotency key payload mismatch.

Alternative stricter behavior if implemented:

- HTTP status is `409 Conflict`.
- Error code is `DUPLICATE_RESOURCE`.

The selected behavior must be consistent with implementation documentation.

---

## 7. Transaction Detail and Investigation Test Scenarios

## INV-001 Get Transaction Detail After Scoring

### Objective

Verify transaction detail returns status, score, decision, and risk factors.

### Precondition

- A transaction has been submitted and scored.

### Steps

1. Call `GET /api/v1/transactions/{transactionRef}`.

### Expected Result

- HTTP status is `200 OK`.
- Response contains masked source account number.
- Response contains masked destination account number.
- Response contains `riskScore`.
- Response contains `riskDecision`.
- Response contains `riskFactors`.

---

## INV-002 Transaction Detail Masks Account Numbers

### Objective

Verify sensitive account numbers are masked.

### Expected Result

- `sourceAccountNumber` format is similar to `123****890`.
- `destinationAccountNumber` format is similar to `987****210`.
- Full account number is not returned.

---

## INV-003 Transaction Not Found

### Objective

Verify correct error when transaction reference does not exist.

### Steps

1. Call `GET /api/v1/transactions/TRX-20990101-999999`.

### Expected Result

- HTTP status is `404 Not Found`.
- Error code is `TRANSACTION_NOT_FOUND`.

---

## 8. Risk Scoring Unit Test Scenarios

## RULE-001 HIGH_AMOUNT Triggered

### Objective

Verify high amount rule.

### Given

- Transaction amount is `25,000,000`.
- Rule threshold is `10,000,000`.
- Rule score is `25`.

### Expected Result

- Rule is triggered.
- Risk factor code is `HIGH_AMOUNT`.
- Score is `25`.
- Metadata contains threshold and actual amount.

---

## RULE-002 HIGH_AMOUNT Not Triggered

### Given

- Transaction amount is `5,000,000`.
- Rule threshold is `10,000,000`.

### Expected Result

- Rule is not triggered.
- Score is not included in total risk score.

---

## RULE-003 NEW_DEVICE Triggered

### Given

- `deviceId` is present.
- Device is not trusted.

### Expected Result

- Rule is triggered.
- Risk factor code is `NEW_DEVICE`.
- Score is `20`.

---

## RULE-004 NEW_DEVICE Not Triggered for Trusted Device

### Given

- `deviceId = DEVICE-TRUSTED-001`.
- Device is trusted.

### Expected Result

- Rule is not triggered.

---

## RULE-005 NEW_DEVICE Not Triggered When Device ID Is Missing

### Given

- `deviceId` is null.

### Expected Result

- Rule is not triggered by default.

---

## RULE-006 BLACKLISTED_DESTINATION Triggered

### Given

- Destination account is `9876543210`.
- Account exists in active `blacklisted_accounts`.

### Expected Result

- Rule is triggered.
- Score is `50`.
- Metadata contains blacklist reason.

---

## RULE-007 BLACKLISTED_DESTINATION Not Triggered for Inactive Blacklist

### Given

- Destination account exists in `blacklisted_accounts`.
- `active = false`.

### Expected Result

- Rule is not triggered.

---

## RULE-008 HIGH_FREQUENCY_TRANSACTION Triggered by Count

### Given

- Redis velocity count is `5`.
- Count threshold is `5`.

### Expected Result

- Rule is triggered.
- Score is `30`.

---

## RULE-009 HIGH_FREQUENCY_TRANSACTION Triggered by Amount

### Given

- Redis velocity amount is `60,000,000`.
- Amount threshold is `50,000,000`.

### Expected Result

- Rule is triggered.
- Score is `30`.

---

## RULE-010 UNUSUAL_LOCATION Triggered

### Given

- Transaction location is `Surabaya`.
- Customer known locations are `Jakarta` and `Batam`.

### Expected Result

- Rule is triggered.
- Score is `20`.

---

## RULE-011 UNUSUAL_LOCATION Not Triggered for Known Location

### Given

- Transaction location is `Jakarta`.
- Customer known location includes `Jakarta`.

### Expected Result

- Rule is not triggered.

---

## RULE-012 HIGH_RISK_CUSTOMER_PROFILE Triggered

### Given

- Customer risk level is `HIGH`.

### Expected Result

- Rule is triggered.
- Score is `25`.

---

## RULE-013 Risk Score Aggregation

### Given

Triggered factors:

- `HIGH_AMOUNT = 25`
- `NEW_DEVICE = 20`
- `BLACKLISTED_DESTINATION = 50`

### Expected Result

- Total score is `95`.

---

## RULE-014 Decision APPROVED

### Given

- Total score is `25`.

### Expected Result

- Decision is `APPROVED`.

---

## RULE-015 Decision REVIEW

### Given

- Total score is `50`.

### Expected Result

- Decision is `REVIEW`.

---

## RULE-016 Decision BLOCKED

### Given

- Total score is `80`.

### Expected Result

- Decision is `BLOCKED`.

---

## 9. Risk Engine Integration Test Scenarios

## RISK-001 Consume transaction.created and Score APPROVED

### Objective

Verify low-risk transaction scoring flow.

### Given

- Amount below high amount threshold.
- Device is trusted.
- Destination is not blacklisted.
- Location is known.
- Customer risk level is LOW.
- Velocity counters below threshold.

### Expected Result

- Transaction status becomes `APPROVED`.
- Risk score is below `50`.
- No triggered risk factors or only non-risk metadata if implemented.
- `transaction.risk-scored` is published.

---

## RISK-002 Consume transaction.created and Score REVIEW

### Given

- Amount is high.
- Device is untrusted.
- No blacklist.

### Expected Result

- Risk score is between `50` and `79`.
- Transaction status becomes `REVIEW`.
- Risk factors are stored.
- `transaction.risk-scored` is published.

---

## RISK-003 Consume transaction.created and Score BLOCKED

### Given

- Destination account is actively blacklisted.
- Amount is high.
- Device is untrusted.

### Expected Result

- Risk score is `80` or higher.
- Transaction status becomes `BLOCKED`.
- Risk factors include `BLACKLISTED_DESTINATION`.
- `transaction.risk-scored` is published.

---

## RISK-004 Duplicate transaction.created Event Is Ignored

### Objective

Verify idempotent consumer behavior.

### Steps

1. Process a `transaction.created` event.
2. Process the same event again.

### Expected Result

- Risk factors are not duplicated.
- Risk score is not recalculated.
- Transaction final decision remains unchanged.
- Event log marks second processing as `IGNORED` or equivalent.

---

## RISK-005 Invalid Event Envelope Is Rejected

### Given

- Event has invalid `eventId`.
- Or `aggregateId` does not match `payload.transactionRef`.

### Expected Result

- Event processing fails gracefully.
- Error is logged.
- Message is retried and eventually moved to DLQ if not corrected.
- Transaction is not updated.

---

## 10. Kafka Test Scenarios

## KAFKA-001 transaction.created Published After Transaction Persistence

### Objective

Verify event publish ordering.

### Steps

1. Submit transaction.
2. Verify transaction row exists.
3. Verify Kafka event exists.

### Expected Result

- `transactions` row exists before event processing.
- Event payload contains correct transaction data.
- Kafka key equals `transactionRef`.

---

## KAFKA-002 transaction.created Envelope Is Valid

### Expected Result

Envelope contains:

- `eventId`
- `eventType = TRANSACTION_CREATED`
- `eventVersion = 1.0`
- `aggregateType = TRANSACTION`
- `aggregateId = transactionRef`
- `occurredAt`
- `producer = transaction-service`
- `payload`

---

## KAFKA-003 transaction.risk-scored Published After Risk Update

### Steps

1. Submit transaction.
2. Wait until risk scoring completes.
3. Check transaction status in PostgreSQL.
4. Check `transaction.risk-scored` event.

### Expected Result

- PostgreSQL contains final risk result.
- `transaction.risk-scored` payload matches PostgreSQL result.
- Kafka key equals `transactionRef`.

---

## KAFKA-004 Consumer Retry on Processing Failure

### Objective

Verify retry behavior.

### Given

- Risk Engine throws a controlled exception during processing.

### Expected Result

- Kafka consumer retries up to configured max attempts.
- Message is not acknowledged before successful processing.
- Error is logged with `eventId` and `transactionRef`.

---

## KAFKA-005 Message Sent to DLQ After Retry Exhaustion

### Given

- Event continues to fail after maximum retries.

### Expected Result

- Event is sent to the correct DLQ topic.
- DLQ payload contains original event.
- DLQ payload contains error metadata.
- `kafka_event_logs` status is `DLQ` or `FAILED`.

---

## KAFKA-006 Unsupported Event Version

### Given

- Event version is `9.9`.

### Expected Result

- Consumer rejects event.
- Error is logged.
- Event follows retry/DLQ policy.

---

## 11. Redis Cache Test Scenarios

## REDIS-001 Blacklist Cache Miss Loads PostgreSQL

### Given

- Redis key `blacklist:account:9876543210` does not exist.
- PostgreSQL contains active blacklist record.

### Steps

1. Risk Engine checks destination account.
2. Redis misses.
3. Risk Engine queries PostgreSQL.

### Expected Result

- PostgreSQL is queried.
- Redis key is populated.
- Rule `BLACKLISTED_DESTINATION` is triggered.

---

## REDIS-002 Blacklist Cache Hit Avoids PostgreSQL

### Given

- Redis key `blacklist:account:9876543210` exists.

### Expected Result

- Risk Engine uses Redis value.
- PostgreSQL blacklist query is not executed.
- Rule result is correct.

---

## REDIS-003 Customer Risk Profile Cache Miss Loads PostgreSQL

### Given

- Redis key `customer:risk-profile:1002` does not exist.
- PostgreSQL profile has `risk_level = HIGH`.

### Expected Result

- PostgreSQL is queried.
- Redis is populated.
- `HIGH_RISK_CUSTOMER_PROFILE` rule is triggered.

---

## REDIS-004 Trusted Device Cache Hit

### Given

- Redis key `customer:trusted-device:1001:DEVICE-TRUSTED-001` exists with `trusted = true`.

### Expected Result

- `NEW_DEVICE` rule is not triggered.
- PostgreSQL device lookup is not required.

---

## REDIS-005 Risk Rule Config Cache Miss

### Given

- Redis key `risk-rule:config:HIGH_AMOUNT` does not exist.
- PostgreSQL contains rule config.

### Expected Result

- Risk Engine loads rule config from PostgreSQL.
- Redis key is populated with TTL.
- Rule uses configured score and threshold.

---

## REDIS-006 Cache Invalidation After Blacklist Update

### Steps

1. Create or update a blacklisted account through Master Data Service.
2. Check Redis key.

### Expected Result

- `blacklist:account:{accountNumber}` is deleted.
- Negative cache key is deleted if implemented.
- Next risk scoring reloads fresh data from PostgreSQL.

---

## REDIS-007 Cache Invalidation After Trusted Device Update

### Steps

1. Update trusted device through Master Data Service.
2. Check Redis key.

### Expected Result

- `customer:trusted-device:{customerId}:{deviceId}` is deleted.

---

## REDIS-008 Cache Invalidation After Customer Risk Profile Update

### Steps

1. Update customer risk profile.
2. Check Redis key.

### Expected Result

- `customer:risk-profile:{customerId}` is deleted.
- Next scoring reflects updated risk level.

---

## REDIS-009 Cache Invalidation After Risk Rule Config Update

### Steps

1. Update risk rule config.
2. Check Redis key.

### Expected Result

- `risk-rule:config:{ruleCode}` is deleted.
- Next scoring uses updated threshold or score.

---

## REDIS-010 Redis Unavailable Fallback to PostgreSQL

### Given

- Redis is unavailable.
- PostgreSQL is available.

### Expected Result

- Risk Engine logs Redis dependency warning.
- Risk Engine queries PostgreSQL for cacheable reference data.
- Risk scoring continues.

---

## 12. Redis Velocity Counter Test Scenarios

## VEL-001 Velocity Count Counter Created

### Steps

1. Process a transaction for source account `1234567890`.
2. Check Redis key `risk:velocity:count:1234567890:10m`.

### Expected Result

- Counter exists.
- Counter value is incremented.
- TTL is set.

---

## VEL-002 Velocity Amount Counter Created

### Steps

1. Process transaction amount `25,000,000`.
2. Check Redis key `risk:velocity:amount:1234567890:10m`.

### Expected Result

- Amount counter exists.
- Counter value includes transaction amount.
- TTL is set.

---

## VEL-003 High Frequency Triggered by Count

### Given

- Counter is already `4`.
- New transaction increments it to `5`.

### Expected Result

- `HIGH_FREQUENCY_TRANSACTION` is triggered.

---

## VEL-004 High Frequency Triggered by Amount

### Given

- Amount counter is already `30,000,000`.
- New transaction amount is `25,000,000`.

### Expected Result

- Amount counter becomes `55,000,000`.
- `HIGH_FREQUENCY_TRANSACTION` is triggered.

---

## VEL-005 Counter Expires Automatically

### Given

- Counter TTL is configured for 10 minutes.

### Expected Result

- Key expires after TTL.
- New scoring after expiration starts a new counter window.

---

## VEL-006 Counter TTL Is Not Extended on Every Increment

### Objective

Verify fixed-window behavior if implemented according to Redis contract.

### Steps

1. Create counter.
2. Capture TTL.
3. Increment counter again.
4. Capture TTL again.

### Expected Result

- TTL is not reset to full 10 minutes on every increment.
- TTL continues decreasing from original window.

---

## 13. Elasticsearch Test Scenarios

## ES-001 Index Audit Document After transaction.risk-scored

### Steps

1. Submit and score transaction.
2. Audit Search Service consumes `transaction.risk-scored`.
3. Query Elasticsearch document by `transactionRef`.

### Expected Result

- Document exists in `fraud-audit-events`.
- Document ID equals `transactionRef`.
- Document contains risk score, decision, risk factors, channel, location, and timestamps.

---

## ES-002 Account Numbers Are Masked Before Indexing

### Expected Result

- `sourceAccountMasked` is masked.
- `destinationAccountMasked` is masked.
- Raw account numbers are not stored in Elasticsearch document.

---

## ES-003 Reprocessing Updates Same Document

### Steps

1. Process the same `transaction.risk-scored` event twice.
2. Count matching Elasticsearch documents by transactionRef.

### Expected Result

- Only one document exists.
- The second processing updates the same document ID.

---

## ES-004 Search by Keyword

### Given

- Audit document has location `Jakarta`.

### Steps

1. Call `GET /api/v1/audits/search?keyword=Jakarta`.

### Expected Result

- Matching document is returned.

---

## ES-005 Search by Decision

### Steps

1. Call `GET /api/v1/audits/search?decision=REVIEW`.

### Expected Result

- Only documents with `decision = REVIEW` are returned.

---

## ES-006 Search by Minimum Risk Score

### Steps

1. Call `GET /api/v1/audits/search?minimumRiskScore=80`.

### Expected Result

- Only documents with `riskScore >= 80` are returned.

---

## ES-007 Search by Risk Factor

### Steps

1. Call `GET /api/v1/audits/search?riskFactor=BLACKLISTED_DESTINATION`.

### Expected Result

- Documents containing the risk factor are returned.

---

## ES-008 Search by Date Range

### Steps

1. Call audit search with `startDate` and `endDate`.

### Expected Result

- Only documents within date range are returned.
- Invalid range returns `INVALID_DATE_RANGE`.

---

## ES-009 Elasticsearch Unavailable

### Given

- Elasticsearch is unavailable.

### Steps

1. Call audit search API.

### Expected Result

- HTTP status is `503 Service Unavailable`.
- Error code is `DEPENDENCY_UNAVAILABLE`.

---

## 14. Master Data Test Scenarios

## MD-001 Create Blacklisted Account

### Steps

1. Login as `ROLE_ADMIN`.
2. Call `POST /api/v1/blacklisted-accounts`.

### Expected Result

- HTTP status is `201 Created`.
- Row exists in `blacklisted_accounts`.
- Related Redis key is invalidated.

---

## MD-002 Duplicate Blacklisted Account

### Given

- Blacklisted account already exists.

### Expected Result

- HTTP status is `409 Conflict`.
- Error code is `DUPLICATE_RESOURCE`.

---

## MD-003 Deactivate Blacklisted Account

### Steps

1. Call `DELETE /api/v1/blacklisted-accounts/{id}`.

### Expected Result

- Record is not physically deleted.
- `active` becomes `false`.
- Redis blacklist key is invalidated.

---

## MD-004 Register Trusted Device

### Steps

1. Call `POST /api/v1/customers/{customerId}/trusted-devices`.

### Expected Result

- Device record is created or updated.
- `trusted = true`.
- Redis trusted device key is invalidated.

---

## MD-005 Update Customer Risk Profile

### Steps

1. Call `PUT /api/v1/customers/{customerId}/risk-profile`.
2. Set `riskLevel = HIGH`.

### Expected Result

- PostgreSQL row is updated.
- Redis profile key is invalidated.
- Next risk scoring uses updated risk level.

---

## MD-006 Update Risk Rule Config

### Steps

1. Call `PUT /api/v1/risk-rules/HIGH_AMOUNT`.
2. Change threshold or score.

### Expected Result

- PostgreSQL config is updated.
- Redis rule config key is invalidated.
- Next scoring uses updated config.

---

## MD-007 Non-Admin Cannot Update Master Data

### Given

- User has `ROLE_BACKOFFICE` or `ROLE_FRAUD_ANALYST`.

### Expected Result

- HTTP status is `403 Forbidden`.
- No master data is changed.

---

## 15. Native SQL Reporting Test Scenarios

## REPORT-001 High-Risk Transactions Report

### Steps

1. Seed transactions with different risk scores.
2. Call `GET /api/v1/reports/high-risk-transactions?minimumRiskScore=50`.

### Expected Result

- Only transactions with `risk_score >= 50` are returned.
- Results are sorted by risk score and amount.
- Account numbers are masked.

---

## REPORT-002 Transaction Velocity Report

### Steps

1. Seed multiple transactions for one source account within 10 minutes.
2. Call transaction velocity report.

### Expected Result

- Account appears with correct transaction count.
- Total amount is correct.
- Results match seeded data.

---

## REPORT-003 Top Risk Customers Report

### Steps

1. Seed multiple customers and transactions.
2. Call top risk customers report.

### Expected Result

- Customers are grouped correctly.
- Average risk score is correct.
- Highest risk score is correct.
- Total amount is correct.

---

## REPORT-004 Suspicious Destination Accounts Report

### Steps

1. Seed multiple source accounts sending to same destination.
2. Call suspicious destination accounts report.

### Expected Result

- Destination account appears when unique sender and amount thresholds are met.
- Aggregated count and total amount are correct.

---

## REPORT-005 Daily Fraud Trend Report

### Steps

1. Seed transactions across multiple dates and decisions.
2. Call daily fraud trend report.

### Expected Result

- Result is grouped by date.
- Approved, review, and blocked counts are correct.
- Average risk score is correct.

---

## REPORT-006 Risk Score Distribution Report

### Steps

1. Seed transactions across low, medium, and high score buckets.
2. Call risk score distribution report.

### Expected Result

- LOW bucket includes scores 0-49.
- MEDIUM bucket includes scores 50-79.
- HIGH bucket includes scores 80+.
- Counts and averages are correct.

---

## REPORT-007 Daily Top Risky Customers Report

### Objective

Validate CTE and window function report.

### Steps

1. Seed daily transaction stats for multiple customers.
2. Call daily top risky customers report with `topN = 3`.

### Expected Result

- Results are ranked per transaction date.
- Only top N customers per day are returned.
- Ranking follows average risk score and total amount.

---

## REPORT-008 Invalid Date Range

### Steps

1. Call any report with `startDate > endDate`.

### Expected Result

- HTTP status is `400 Bad Request`.
- Error code is `INVALID_DATE_RANGE`.

---

## REPORT-009 Report Page Size Exceeds Maximum

### Steps

1. Call report with `size = 500`.

### Expected Result

- HTTP status is `400 Bad Request`.
- Error code is `INVALID_PAGE_SIZE`.

---

## 16. Database Integrity Test Scenarios

## DB-001 Transaction Amount Constraint

### Steps

1. Attempt to insert transaction with `amount <= 0`.

### Expected Result

- Database rejects insert.
- Check constraint is enforced.

---

## DB-002 Unique Transaction Reference

### Steps

1. Insert two transactions with same `transaction_ref`.

### Expected Result

- Second insert fails due to unique constraint.

---

## DB-003 Unique Idempotency Key

### Steps

1. Insert two transactions with same `idempotency_key`.

### Expected Result

- Second insert fails due to unique constraint.

---

## DB-004 Foreign Key Source Account

### Steps

1. Insert transaction with non-existing `source_account_id`.

### Expected Result

- Database rejects insert due to foreign key constraint.

---

## DB-005 Risk Factor Requires Transaction

### Steps

1. Insert risk factor for non-existing transaction ID.

### Expected Result

- Database rejects insert due to foreign key constraint.

---

## DB-006 Check Constraint for Transaction Status

### Steps

1. Insert transaction with invalid status.

### Expected Result

- Database rejects insert.

---

## DB-007 User Role Mapping Requires Valid User and Role

### Steps

1. Insert `user_roles` row with invalid user ID or role ID.

### Expected Result

- Database rejects insert due to foreign key constraint.

---

## 17. Error Handling Test Scenarios

## ERR-001 Common Error Response Format

### Objective

Verify all API errors use the standard response format.

### Expected Fields

- `timestamp`
- `status`
- `error`
- `message`
- `details`
- `path`
- `requestId`

---

## ERR-002 Invalid JSON Body

### Steps

1. Send malformed JSON to `POST /api/v1/transactions`.

### Expected Result

- HTTP status is `400 Bad Request`.
- Error response follows common format.

---

## ERR-003 Dependency Unavailable

### Given

- PostgreSQL, Kafka, Redis, or Elasticsearch dependency is unavailable depending on test.

### Expected Result

- Service returns controlled error where applicable.
- Error code is `DEPENDENCY_UNAVAILABLE` or service-specific controlled error.
- Logs include dependency name and requestId/eventId.

---

## ERR-004 Event Publish Failure

### Given

- Kafka is unavailable during transaction submission.

### Expected Result

- Transaction Service returns `500 EVENT_PUBLISH_FAILED` if direct publish is required before response.
- Event failure is logged.
- No false success response is returned.

---

## 18. Observability Test Scenarios

## OBS-001 Request ID Is Generated When Missing

### Steps

1. Call API without `X-Request-Id`.

### Expected Result

- Service generates request ID.
- Error or success logs include generated request ID.

---

## OBS-002 Request ID Is Propagated to Kafka Event

### Steps

1. Call transaction submission with `X-Request-Id: req-test-001`.
2. Inspect `transaction.created`.

### Expected Result

- Event envelope contains `requestId = req-test-001`.

---

## OBS-003 Kafka Event Logs Are Written

### Steps

1. Submit and score transaction.
2. Query `kafka_event_logs`.

### Expected Result

- Event publish record exists.
- Event consume record exists.
- Status values follow allowed enum: `PUBLISHED`, `CONSUMED`, `FAILED`, `DLQ`, or `IGNORED`.

---

## OBS-004 Risk Scoring Logs Include Correlation Fields

### Expected Logs Include

- `eventId`
- `transactionRef`
- `riskScore`
- `decision`
- `service`
- `requestId` or `correlationId`

---

## 19. End-to-End Test Scenarios

## E2E-001 Approved Transaction Flow

### Objective

Verify complete low-risk transaction flow.

### Steps

1. Login as back office user.
2. Submit transaction with trusted device, known location, low amount, no blacklist.
3. Wait for Risk Engine processing.
4. Retrieve transaction detail.
5. Search audit document.
6. Query reporting endpoint.

### Expected Result

- Transaction is created.
- `transaction.created` is published.
- Risk Engine scores transaction as `APPROVED`.
- `transaction.risk-scored` is published.
- Audit document is indexed in Elasticsearch.
- Transaction detail returns `APPROVED`.
- Search API returns audit document.
- Reporting endpoints include transaction where applicable.

---

## E2E-002 Review Transaction Flow

### Objective

Verify complete medium-risk transaction flow.

### Given

- Transaction amount is high.
- Device is untrusted.
- Destination is not blacklisted.

### Expected Result

- Transaction decision is `REVIEW`.
- Risk factors include `HIGH_AMOUNT` and `NEW_DEVICE`.
- Audit search returns decision `REVIEW`.

---

## E2E-003 Blocked Transaction Flow

### Objective

Verify complete high-risk transaction flow.

### Given

- Destination account is blacklisted.
- Amount is high.
- Device is untrusted.

### Expected Result

- Transaction decision is `BLOCKED`.
- Risk score is at least `80`.
- Risk factors include `BLACKLISTED_DESTINATION`.
- Audit document is indexed.
- Reports include the transaction as high-risk.

---

## E2E-004 Master Data Update Affects Next Scoring

### Objective

Verify that master data updates and Redis invalidation affect future decisions.

### Steps

1. Submit transaction to non-blacklisted destination.
2. Verify destination does not trigger blacklist rule.
3. Add destination account to blacklist through Master Data Service.
4. Submit another transaction to same destination.
5. Wait for scoring.

### Expected Result

- First transaction does not trigger `BLACKLISTED_DESTINATION`.
- Master data update invalidates Redis key.
- Second transaction triggers `BLACKLISTED_DESTINATION`.

---

## E2E-005 Duplicate Submission Does Not Duplicate Downstream Processing

### Steps

1. Submit transaction with `Idempotency-Key: idem-e2e-001`.
2. Submit the same request again with same idempotency key.
3. Wait for processing.
4. Check transaction count, risk factor count, Kafka events, and audit documents.

### Expected Result

- One transaction exists.
- Risk factors are not duplicated.
- One audit document exists.
- Duplicate request returns existing transaction response.

---

## 20. Degraded Dependency Test Scenarios

## DEG-001 Redis Down During Risk Scoring

### Given

- Redis is unavailable.
- PostgreSQL is available.

### Expected Result

- Risk Engine uses PostgreSQL fallback for cacheable data.
- Velocity rule may be skipped or degraded according to implementation.
- Transaction is still scored.
- Logs indicate Redis degraded behavior.

---

## DEG-002 Elasticsearch Down During Audit Indexing

### Given

- Elasticsearch is unavailable when Audit Search Service processes `transaction.risk-scored`.

### Expected Result

- Event processing fails.
- Message is retried.
- After retry exhaustion, message is moved to `transaction.risk-scored.dlq`.
- Transaction state in PostgreSQL remains authoritative.

---

## DEG-003 Kafka Down During Transaction Submission

### Given

- Kafka is unavailable.

### Expected Result

- Transaction Service does not claim full success if event cannot be published.
- Error code is `EVENT_PUBLISH_FAILED` or controlled dependency error.
- Logs contain failure details.

---

## DEG-004 PostgreSQL Down During Transaction Submission

### Given

- PostgreSQL is unavailable.

### Expected Result

- Transaction cannot be created.
- HTTP status is `503 Service Unavailable`.
- Error code is `DEPENDENCY_UNAVAILABLE`.

---

## 21. Health Check Test Scenarios

## HEALTH-001 Transaction Service Health

### Steps

1. Call `GET /actuator/health` on Transaction Service.

### Expected Result

- Returns `UP` when service and required dependencies are available.

---

## HEALTH-002 Risk Engine Service Health

### Expected Result

- Returns `UP` when PostgreSQL, Kafka, and Redis are reachable.

---

## HEALTH-003 Audit Search Service Health

### Expected Result

- Returns `UP` when Kafka and Elasticsearch are reachable.

---

## HEALTH-004 Master Data Service Health

### Expected Result

- Returns `UP` when PostgreSQL and Redis are reachable.

---

## 22. Test Execution Groups

### 22.1 Fast Test Group

Run on every local build.

Includes:

- unit tests
- validators
- mappers
- risk rules
- decision mapping
- masking utility

### 22.2 Integration Test Group

Run before merge or final verification.

Includes:

- PostgreSQL repositories
- native SQL reports
- Kafka producer/consumer
- Redis cache/counter
- Elasticsearch indexing/search
- Spring Security

### 22.3 End-to-End Test Group

Run before final demo.

Includes:

- approved flow
- review flow
- blocked flow
- duplicate submission flow
- master data update flow

### 22.4 Failure Test Group

Run before final demo if time allows.

Includes:

- Kafka failure
- Redis failure
- Elasticsearch failure
- DLQ scenario
- invalid event schema

---

## 23. Final Acceptance Criteria

BankGuard testing is considered complete when:

1. A valid transaction can be submitted.
2. Duplicate transaction submission is prevented by idempotency.
3. `transaction.created` is published and consumed.
4. Risk Engine evaluates all active rules.
5. Risk score and decision are persisted in PostgreSQL.
6. Risk factors are persisted and visible from transaction detail.
7. `transaction.risk-scored` is published and consumed.
8. Audit document is indexed into Elasticsearch.
9. Audit search supports keyword and structured filters.
10. Redis cache hit, cache miss, and invalidation are proven.
11. Redis velocity counters are proven.
12. Native SQL reports return correct seeded data.
13. Role-based authorization works.
14. Account numbers are masked in API responses and Elasticsearch documents.
15. Kafka retry and DLQ behavior can be demonstrated.
16. All services can run through Docker Compose.
17. Critical unit and integration tests pass.

---

## 24. Traceability Matrix

| Test Area | Related Document | Related Capability |
|---|---|---|
| Authentication | API Contract, Database Schema | Login, JWT, role access |
| Transaction Submission | PRD, API Contract, TRD, Database Schema, Kafka Contract | MVP-01, MVP-03 |
| Risk Scoring | PRD, System Design, TRD, Redis Contract, Database Schema | MVP-02, MVP-04 |
| Kafka Events | Kafka Event Contract, TRD, Database Schema | MVP-03 |
| Redis Cache | Redis Key Contract, TRD, Database Schema | MVP-04 |
| Elasticsearch Search | System Design, API Contract, Kafka Contract | MVP-05, MVP-06 |
| Reporting | PRD, API Contract, Database Schema | MVP-07 |
| Master Data | API Contract, Database Schema, Redis Contract | Blacklist, trusted device, risk profile, rule config |
| E2E Flow | PRD, System Design, TRD | Complete transaction-to-investigation workflow |
| Failure Handling | Kafka Contract, Redis Contract, API Contract | Retry, DLQ, dependency errors |

