# Demo Scenarios
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the demo scenarios for **BankGuard**.

The purpose of this document is to provide a structured demonstration plan that proves the BankGuard MVP works end-to-end across:

- secure API access
- transaction submission
- idempotency handling
- asynchronous Kafka event processing
- fraud risk scoring
- Redis cache and real-time counters
- PostgreSQL persistence
- Elasticsearch audit indexing
- fraud investigation search
- native SQL reporting
- master data cache invalidation
- degraded dependency behavior

This document is aligned with:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`
- `06_KAFKA_EVENT_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 1.2 Demo Objective

The demo must prove that a transaction can move through the complete BankGuard workflow:

```text
API Request
  -> Transaction Service
  -> PostgreSQL
  -> Kafka transaction.created
  -> Risk Engine Service
  -> Redis lookup and counters
  -> PostgreSQL risk result update
  -> Kafka transaction.risk-scored
  -> Audit Search Service
  -> Elasticsearch audit document
  -> Investigation API and Reporting API
```

The demo should not only show successful API responses. It must also show that the supporting infrastructure is actually used.

---

### 1.3 Demo Scope

The demo covers:

1. Environment startup.
2. Database migration and seed verification.
3. Authentication.
4. Transaction submission.
5. Idempotency.
6. Low-risk transaction approval.
7. High-amount review transaction.
8. Blacklisted destination blocked transaction.
9. New device and unusual location risk factors.
10. High-frequency transaction using Redis velocity counters.
11. Audit search through Elasticsearch.
12. Native SQL reporting.
13. Master data update and Redis cache invalidation.
14. Kafka event trace and DLQ visibility.
15. Health check verification.
16. Final end-to-end acceptance checklist.

---

### 1.4 Non-Scope

The demo does not cover:

- real fund transfer
- core banking integration
- payment settlement
- frontend dashboard
- machine learning scoring
- production observability dashboard
- multi-region deployment
- regulatory reporting integration

---

## 2. Demo Environment

### 2.1 Required Services

The following services must be running before the demo:

| Component | Purpose | Expected Port |
|---|---|---:|
| Transaction Service | Authentication, transaction API, reporting API | 8081 |
| Risk Engine Service | Kafka consumer and fraud scoring | internal |
| Audit Search Service | Audit search API and Elasticsearch indexing | 8083 |
| Master Data Service | Blacklist, trusted device, risk profile, rule config APIs | 8084 |
| PostgreSQL | Source of truth | 5432 |
| Kafka | Event streaming | 9092 |
| Redis | Cache and velocity counters | 6379 |
| Elasticsearch | Audit search read model | 9200 |

---

### 2.2 Start Command

From the project root:

```bash
docker compose up -d
```

---

### 2.3 Verify Containers

```bash
docker compose ps
```

Expected result:

```text
postgres              running
kafka                 running
redis                 running
elasticsearch         running
transaction-service   running
risk-engine-service   running
audit-search-service  running
master-data-service   running
```

---

### 2.4 Verify Service Health

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

---

## 3. Demo Users and Roles

### 3.1 Demo Users

| Username | Password | Role | Purpose |
|---|---|---|---|
| admin | admin123 | ROLE_ADMIN | Master data management and all access |
| backoffice | backoffice123 | ROLE_BACKOFFICE | Submit transactions and view transaction details |
| analyst | analyst123 | ROLE_FRAUD_ANALYST | Search audit data and view reports |

---

### 3.2 Login as Admin

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-login-admin" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Expected response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "roles": [
    "ROLE_ADMIN"
  ]
}
```

Store the token:

```bash
export ADMIN_TOKEN=<jwt>
```

---

### 3.3 Login as Back Office

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-login-backoffice" \
  -d '{
    "username": "backoffice",
    "password": "backoffice123"
  }'
```

Store the token:

```bash
export BACKOFFICE_TOKEN=<jwt>
```

---

### 3.4 Login as Fraud Analyst

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-login-analyst" \
  -d '{
    "username": "analyst",
    "password": "analyst123"
  }'
```

Store the token:

```bash
export ANALYST_TOKEN=<jwt>
```

---

## 4. Seed Data Verification

### 4.1 Required Seed Data

The demo assumes the following data exists.

#### Customers

| Customer ID | CIF | Name | Status | Risk Level |
|---:|---|---|---|---|
| 1001 | CIF001 | Daniel Sinaga | ACTIVE | LOW |
| 1002 | CIF002 | Maria Hutabarat | ACTIVE | HIGH |
| 1003 | CIF003 | Budi Sitorus | SUSPENDED | MEDIUM |

#### Accounts

| Account ID | Customer ID | Account Number | Status | Balance |
|---:|---:|---|---|---:|
| 2001 | 1001 | 1234567890 | ACTIVE | 100000000 |
| 2002 | 1002 | 2234567890 | ACTIVE | 200000000 |
| 2003 | 1003 | 3234567890 | BLOCKED | 50000000 |

#### Blacklisted Accounts

| Account Number | Active | Reason |
|---|---|---|
| 9876543210 | true | Reported mule account |
| 8876543210 | false | Previously reviewed |

#### Trusted Devices

| Customer ID | Device ID | Trusted |
|---:|---|---|
| 1001 | DEVICE-TRUSTED-001 | true |
| 1001 | DEVICE-UNTRUSTED-001 | false |

#### Known Locations

| Customer ID | Location |
|---:|---|
| 1001 | Jakarta |
| 1001 | Batam |
| 1002 | Medan |

---

### 4.2 Optional SQL Verification

```bash
docker exec -it bankguard-postgres psql -U bankguard -d bankguard
```

```sql
SELECT id, cif_number, full_name, status FROM customers;
SELECT id, account_number, status FROM accounts;
SELECT account_number, active FROM blacklisted_accounts;
SELECT customer_id, device_id, trusted FROM customer_devices;
SELECT customer_id, risk_level FROM customer_risk_profiles;
```

---

## 5. Demo Scenario 1 - Authorization Boundary

### 5.1 Objective

Prove that the API enforces role-based access control.

---

### 5.2 Fraud Analyst Attempts to Submit Transaction

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "Idempotency-Key: demo-auth-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-auth-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "5556667770",
    "amount": 5000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-TRUSTED-001",
    "ipAddress": "36.77.88.12",
    "location": "Jakarta"
  }'
```

Expected response:

```json
{
  "status": 403,
  "error": "FORBIDDEN"
}
```

---

### 5.3 Demo Success Criteria

- Fraud analyst cannot submit transactions.
- No transaction is inserted.
- No Kafka event is published.

---

## 6. Demo Scenario 2 - Low-Risk Transaction Approved

### 6.1 Objective

Prove the normal low-risk transaction path.

This scenario should trigger no risk factors or only low-risk behavior, resulting in an `APPROVED` decision.

---

### 6.2 Submit Transaction

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-low-risk-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-low-risk-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "5556667770",
    "amount": 5000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-TRUSTED-001",
    "ipAddress": "36.77.88.12",
    "location": "Jakarta"
  }'
```

Expected immediate response:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "status": "PENDING_RISK_CHECK",
  "message": "Transaction submitted for risk scoring"
}
```

---

### 6.3 Verify Event and Processing

Check Transaction Service logs:

```bash
docker compose logs transaction-service | grep demo-low-risk-001
```

Expected evidence:

```text
transaction.created published
```

Check Risk Engine logs:

```bash
docker compose logs risk-engine-service | grep TRX-
```

Expected evidence:

```text
transaction.created consumed
risk scoring completed
decision=APPROVED
transaction.risk-scored published
```

Check Audit Search Service logs:

```bash
docker compose logs audit-search-service | grep TRX-
```

Expected evidence:

```text
transaction.risk-scored consumed
fraud audit document indexed
```

---

### 6.4 Retrieve Transaction Detail

```bash
curl -X GET http://localhost:8081/api/v1/transactions/TRX-20260604-000001 \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "X-Request-Id: demo-low-risk-detail"
```

Expected response:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "sourceAccountNumber": "123****890",
  "destinationAccountNumber": "555****770",
  "amount": 5000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "status": "APPROVED",
  "riskScore": 0,
  "riskDecision": "APPROVED",
  "riskFactors": []
}
```

---

### 6.5 Demo Success Criteria

- Transaction is created with `PENDING_RISK_CHECK`.
- Kafka `transaction.created` is published.
- Risk Engine consumes the event.
- Risk Engine updates transaction to `APPROVED`.
- Kafka `transaction.risk-scored` is published.
- Audit Search Service indexes the document.
- Account numbers are masked in API response.

---

## 7. Demo Scenario 3 - Idempotency

### 7.1 Objective

Prove duplicate transaction submission is prevented using `Idempotency-Key`.

---

### 7.2 Submit the Same Request Again

Use the same `Idempotency-Key` from Demo Scenario 2:

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-low-risk-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-idempotency-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "5556667770",
    "amount": 5000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-TRUSTED-001",
    "ipAddress": "36.77.88.12",
    "location": "Jakarta"
  }'
```

Expected response:

```json
{
  "transactionRef": "TRX-20260604-000001",
  "status": "APPROVED",
  "message": "Existing transaction returned for the provided idempotency key"
}
```

---

### 7.3 Database Verification

```sql
SELECT COUNT(*)
FROM transactions
WHERE idempotency_key = 'demo-low-risk-001';
```

Expected result:

```text
1
```

---

### 7.4 Demo Success Criteria

- Duplicate request does not create a second transaction.
- Existing transaction reference is returned.
- No duplicate `transaction.created` event is published.

---

## 8. Demo Scenario 4 - High Amount Transaction Goes to Review

### 8.1 Objective

Prove that the `HIGH_AMOUNT` rule is triggered and produces a `REVIEW` decision when the score is within review threshold.

---

### 8.2 Submit High Amount Transaction

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-high-amount-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-high-amount-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "5556667771",
    "amount": 25000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-TRUSTED-001",
    "ipAddress": "36.77.88.12",
    "location": "Jakarta"
  }'
```

---

### 8.3 Retrieve Transaction Detail

Expected risk result:

```json
{
  "status": "APPROVED_OR_REVIEW_DEPENDING_ON_CONFIG",
  "riskScore": 25,
  "riskFactors": [
    {
      "code": "HIGH_AMOUNT",
      "score": 25
    }
  ]
}
```

Note:

If only `HIGH_AMOUNT` is triggered with default score `25`, the decision remains `APPROVED` because the review threshold starts at `50`.

To demonstrate `REVIEW`, combine high amount with another medium factor such as `NEW_DEVICE` or `UNUSUAL_LOCATION`.

---

### 8.4 Submit High Amount + New Device Transaction

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-review-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-review-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "5556667772",
    "amount": 25000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-NEW-REVIEW-001",
    "ipAddress": "36.77.88.12",
    "location": "Jakarta"
  }'
```

Expected result after scoring:

```json
{
  "status": "REVIEW",
  "riskScore": 45,
  "riskDecision": "APPROVED_OR_REVIEW_DEPENDING_ON_THRESHOLD",
  "riskFactors": [
    {
      "code": "HIGH_AMOUNT",
      "score": 25
    },
    {
      "code": "NEW_DEVICE",
      "score": 20
    }
  ]
}
```

Important:

With the default threshold defined in the PRD, `45` is still below `50`, so it remains `APPROVED`.

To guarantee `REVIEW`, use:

- high amount
- new device
- unusual location

Total score:

```text
25 + 20 + 20 = 65
```

---

### 8.5 Submit Guaranteed Review Transaction

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-review-guaranteed-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-review-guaranteed-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "5556667773",
    "amount": 25000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-NEW-REVIEW-002",
    "ipAddress": "36.77.88.12",
    "location": "Surabaya"
  }'
```

Expected result after scoring:

```json
{
  "status": "REVIEW",
  "riskScore": 65,
  "riskDecision": "REVIEW",
  "riskFactors": [
    {
      "code": "HIGH_AMOUNT",
      "score": 25
    },
    {
      "code": "NEW_DEVICE",
      "score": 20
    },
    {
      "code": "UNUSUAL_LOCATION",
      "score": 20
    }
  ]
}
```

---

### 8.6 Demo Success Criteria

- Risk factors are generated.
- Total score equals the sum of triggered factors.
- Final decision follows the decision threshold.
- Risk factors are stored in PostgreSQL.
- Transaction detail API returns the factors.

---

## 9. Demo Scenario 5 - Blacklisted Destination Blocked Transaction

### 9.1 Objective

Prove that a transaction to an active blacklisted account is detected and can be blocked when combined with other risk factors.

---

### 9.2 Submit Transaction to Blacklisted Destination

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-blocked-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-blocked-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "9876543210",
    "amount": 25000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-NEW-BLOCKED-001",
    "ipAddress": "36.77.88.12",
    "location": "Surabaya"
  }'
```

Expected score:

```text
BLACKLISTED_DESTINATION = 50
HIGH_AMOUNT = 25
NEW_DEVICE = 20
UNUSUAL_LOCATION = 20

Total = 115
```

Expected result:

```json
{
  "status": "BLOCKED",
  "riskScore": 115,
  "riskDecision": "BLOCKED",
  "riskFactors": [
    {
      "code": "BLACKLISTED_DESTINATION",
      "score": 50
    },
    {
      "code": "HIGH_AMOUNT",
      "score": 25
    },
    {
      "code": "NEW_DEVICE",
      "score": 20
    },
    {
      "code": "UNUSUAL_LOCATION",
      "score": 20
    }
  ]
}
```

---

### 9.3 Redis Verification

Check Redis key:

```bash
docker exec -it bankguard-redis redis-cli GET blacklist:account:9876543210
```

Expected:

```json
{
  "accountNumber": "9876543210",
  "active": true,
  "reason": "Reported mule account"
}
```

---

### 9.4 Demo Success Criteria

- Redis blacklist cache is used or populated.
- `BLACKLISTED_DESTINATION` risk factor is triggered.
- Transaction is updated to `BLOCKED`.
- Risk result is published to Kafka.
- Audit document is indexed into Elasticsearch.

---

## 10. Demo Scenario 6 - Redis Velocity Counter

### 10.1 Objective

Prove that Redis velocity counters are used by the `HIGH_FREQUENCY_TRANSACTION` rule.

---

### 10.2 Submit Multiple Transactions Quickly

Run several requests using different idempotency keys:

```bash
for i in 1 2 3 4 5 6; do
  curl -X POST http://localhost:8081/api/v1/transactions \
    -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
    -H "Idempotency-Key: demo-velocity-$i" \
    -H "Content-Type: application/json" \
    -H "X-Request-Id: demo-velocity-$i" \
    -d "{
      \"sourceAccountNumber\": \"1234567890\",
      \"destinationAccountNumber\": \"55566677$i\",
      \"amount\": 1000000,
      \"currency\": \"IDR\",
      \"channel\": \"MOBILE_BANKING\",
      \"deviceId\": \"DEVICE-TRUSTED-001\",
      \"ipAddress\": \"36.77.88.12\",
      \"location\": \"Jakarta\"
    }"
done
```

---

### 10.3 Verify Redis Counters

```bash
docker exec -it bankguard-redis redis-cli GET risk:velocity:count:1234567890:10m
docker exec -it bankguard-redis redis-cli GET risk:velocity:amount:1234567890:10m
docker exec -it bankguard-redis redis-cli TTL risk:velocity:count:1234567890:10m
```

Expected:

```text
count >= 5
amount >= 5000000
TTL > 0
```

---

### 10.4 Expected Risk Result

One or more later transactions should include:

```json
{
  "code": "HIGH_FREQUENCY_TRANSACTION",
  "score": 30
}
```

---

### 10.5 Demo Success Criteria

- Redis velocity count key is created.
- Redis velocity amount key is created.
- TTL is set.
- Counter values increase.
- High-frequency transaction rule triggers after threshold is reached.

---

## 11. Demo Scenario 7 - Master Data Update and Redis Cache Invalidation

### 11.1 Objective

Prove that Master Data Service invalidates Redis cache after risk-sensitive master data changes.

---

### 11.2 Create New Blacklisted Account

```bash
curl -X POST http://localhost:8084/api/v1/blacklisted-accounts \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-blacklist-create-001" \
  -d '{
    "accountNumber": "7778889990",
    "reason": "Newly reported mule account"
  }'
```

---

### 11.3 Verify Redis Invalidation

```bash
docker exec -it bankguard-redis redis-cli GET blacklist:account:7778889990
docker exec -it bankguard-redis redis-cli GET blacklist:account:7778889990:negative
```

Expected:

```text
nil
```

Explanation:

The next Risk Engine lookup should reload this value from PostgreSQL and repopulate Redis.

---

### 11.4 Submit Transaction to Newly Blacklisted Account

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-new-blacklist-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-new-blacklist-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "7778889990",
    "amount": 5000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-TRUSTED-001",
    "ipAddress": "36.77.88.12",
    "location": "Jakarta"
  }'
```

Expected risk factor:

```json
{
  "code": "BLACKLISTED_DESTINATION",
  "score": 50
}
```

---

### 11.5 Demo Success Criteria

- Master Data Service writes to PostgreSQL.
- Related Redis key is invalidated.
- Risk Engine reloads fresh blacklist data.
- Transaction scoring uses updated master data.

---

## 12. Demo Scenario 8 - Audit Search Through Elasticsearch

### 12.1 Objective

Prove that scored transactions are searchable through Elasticsearch.

---

### 12.2 Search Review Transactions

```bash
curl -X GET "http://localhost:8083/api/v1/audits/search?decision=REVIEW&minimumRiskScore=50&page=0&size=20" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "X-Request-Id: demo-audit-review-search"
```

Expected response:

```json
{
  "data": [
    {
      "transactionRef": "TRX-20260604-000003",
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "sourceAccountMasked": "123****890",
      "destinationAccountMasked": "555****773",
      "riskScore": 65,
      "decision": "REVIEW",
      "riskFactors": [
        "HIGH_AMOUNT",
        "NEW_DEVICE",
        "UNUSUAL_LOCATION"
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

### 12.3 Search Blocked Transactions

```bash
curl -X GET "http://localhost:8083/api/v1/audits/search?decision=BLOCKED&page=0&size=20" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "X-Request-Id: demo-audit-blocked-search"
```

Expected:

```json
{
  "data": [
    {
      "decision": "BLOCKED",
      "riskFactors": [
        "BLACKLISTED_DESTINATION"
      ]
    }
  ]
}
```

---

### 12.4 Keyword Search

```bash
curl -X GET "http://localhost:8083/api/v1/audits/search?keyword=Surabaya&page=0&size=20" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "X-Request-Id: demo-audit-keyword-search"
```

Expected:

```text
Transactions with location Surabaya are returned.
```

---

### 12.5 Demo Success Criteria

- Audit Search Service reads from Elasticsearch.
- Search supports structured filters.
- Search supports keyword matching.
- Account numbers are masked.
- Elasticsearch document is not treated as source of truth.

---

## 13. Demo Scenario 9 - Native SQL Reporting

### 13.1 Objective

Prove that Transaction Service provides analytical reports using native SQL.

---

### 13.2 High-Risk Transactions Report

```bash
curl -X GET "http://localhost:8081/api/v1/reports/high-risk-transactions?minimumRiskScore=50&startDate=2026-06-01&endDate=2026-06-30&page=0&size=20" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "X-Request-Id: demo-report-high-risk"
```

Expected:

```json
{
  "data": [
    {
      "transactionRef": "TRX-20260604-000003",
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "riskScore": 65,
      "decision": "REVIEW"
    }
  ]
}
```

---

### 13.3 Transaction Velocity Report

```bash
curl -X GET "http://localhost:8081/api/v1/reports/transaction-velocity?startDate=2026-06-01&endDate=2026-06-30&minimumCount=5&page=0&size=20" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "X-Request-Id: demo-report-velocity"
```

Expected:

```text
Source account 123****890 appears with transaction count above threshold.
```

---

### 13.4 Risk Score Distribution Report

```bash
curl -X GET "http://localhost:8081/api/v1/reports/risk-score-distribution?startDate=2026-06-01&endDate=2026-06-30" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "X-Request-Id: demo-report-distribution"
```

Expected:

```json
{
  "data": [
    {
      "riskBucket": "0-49",
      "transactionCount": 1
    },
    {
      "riskBucket": "50-79",
      "transactionCount": 1
    },
    {
      "riskBucket": "80+",
      "transactionCount": 1
    }
  ]
}
```

---

### 13.5 Daily Top Risky Customers Report

```bash
curl -X GET "http://localhost:8081/api/v1/reports/daily-top-risky-customers?startDate=2026-06-01&endDate=2026-06-30&limit=10" \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "X-Request-Id: demo-report-daily-top-risky"
```

Expected:

```text
Report returns ranked customers by daily average risk score using native SQL ranking/window function.
```

---

### 13.6 Demo Success Criteria

- Reporting endpoints are protected.
- Fraud analyst can access reports.
- Back office cannot access reports if not authorized.
- Reports return aggregated data.
- Reports use PostgreSQL source of truth.
- At least one report demonstrates CTE or window function behavior.

---

## 14. Demo Scenario 10 - Kafka Event Trace

### 14.1 Objective

Prove that Kafka events are published, consumed, and traceable.

---

### 14.2 Verify Event Logs in PostgreSQL

```sql
SELECT event_id, event_type, aggregate_id, topic_name, status, created_at, processed_at
FROM kafka_event_logs
ORDER BY created_at DESC
LIMIT 20;
```

Expected statuses:

```text
PUBLISHED
CONSUMED
```

Expected event types:

```text
TRANSACTION_CREATED
TRANSACTION_RISK_SCORED
```

---

### 14.3 Verify Kafka Topics

```bash
docker exec -it bankguard-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Expected topics:

```text
transaction.created
transaction.risk-scored
transaction.created.dlq
transaction.risk-scored.dlq
```

---

### 14.4 Demo Success Criteria

- `transaction.created` topic exists.
- `transaction.risk-scored` topic exists.
- Event log table records lifecycle.
- Event IDs, transaction references, and request IDs are traceable.

---

## 15. Demo Scenario 11 - Failure and DLQ Visibility

### 15.1 Objective

Show how failed event processing is isolated after retry exhaustion.

This scenario is optional for live demo because it may require temporarily breaking a dependency or publishing an invalid event manually.

---

### 15.2 Option A - Publish Invalid Event Manually

Publish an event with invalid envelope or missing payload fields to `transaction.created`.

Expected behavior:

```text
Risk Engine validates event.
Risk Engine fails controlled processing.
Retry is attempted.
After retry exhaustion, event is moved to transaction.created.dlq.
kafka_event_logs records FAILED or DLQ status.
```

---

### 15.3 Option B - Stop PostgreSQL Temporarily

```bash
docker compose stop postgres
```

Then publish or submit a transaction event that requires Risk Engine database access.

Expected behavior:

```text
Risk Engine cannot update transaction.
Consumer retries.
Message is not acknowledged before successful processing.
Failure is logged.
```

Restart PostgreSQL:

```bash
docker compose start postgres
```

---

### 15.4 Demo Success Criteria

- Failure does not silently disappear.
- Retry behavior is visible in logs.
- Failed event is moved to DLQ after retry exhaustion.
- DLQ payload preserves original event and error metadata.

---

## 16. Demo Scenario 12 - Redis Degraded Behavior

### 16.1 Objective

Prove the system can continue risk scoring for cacheable reference data when Redis is unavailable.

---

### 16.2 Stop Redis

```bash
docker compose stop redis
```

---

### 16.3 Submit Transaction to Blacklisted Account

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Authorization: Bearer $BACKOFFICE_TOKEN" \
  -H "Idempotency-Key: demo-redis-down-001" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: demo-redis-down-001" \
  -d '{
    "sourceAccountNumber": "1234567890",
    "destinationAccountNumber": "9876543210",
    "amount": 5000000,
    "currency": "IDR",
    "channel": "MOBILE_BANKING",
    "deviceId": "DEVICE-TRUSTED-001",
    "ipAddress": "36.77.88.12",
    "location": "Jakarta"
  }'
```

Expected behavior:

```text
Risk Engine logs Redis unavailable.
Risk Engine falls back to PostgreSQL for blacklist lookup.
BLACKLISTED_DESTINATION still triggers.
Velocity counter behavior is degraded.
```

---

### 16.4 Restart Redis

```bash
docker compose start redis
```

---

### 16.5 Demo Success Criteria

- Redis failure does not break cacheable risk lookup.
- Risk Engine uses PostgreSQL fallback.
- Logs clearly show degraded cache behavior.
- Redis velocity counter limitation is acknowledged.

---

## 17. Demo Scenario 13 - Elasticsearch Degraded Behavior

### 17.1 Objective

Prove that Elasticsearch failure affects audit search indexing, not transaction source of truth.

---

### 17.2 Stop Elasticsearch

```bash
docker compose stop elasticsearch
```

---

### 17.3 Submit Transaction

Submit any valid transaction.

Expected behavior:

```text
Transaction is stored in PostgreSQL.
Risk Engine scores transaction.
transaction.risk-scored is published.
Audit Search Service fails to index due to Elasticsearch unavailable.
Failure is retried or routed according to event handling policy.
```

---

### 17.4 Verify PostgreSQL Transaction Still Exists

```sql
SELECT transaction_ref, status, risk_score, risk_decision
FROM transactions
ORDER BY created_at DESC
LIMIT 1;
```

Expected:

```text
Transaction and risk decision exist in PostgreSQL.
```

---

### 17.5 Restart Elasticsearch

```bash
docker compose start elasticsearch
```

---

### 17.6 Demo Success Criteria

- PostgreSQL remains source of truth.
- Elasticsearch failure does not delete or corrupt transaction state.
- Audit indexing failure is visible.
- Retry or DLQ behavior is visible.

---

## 18. Demo Scenario 14 - Health Check and Dependency Visibility

### 18.1 Objective

Verify all service health endpoints and dependency visibility.

---

### 18.2 Commands

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

If dependency health details are enabled, expected components may include:

```text
db
redis
elasticsearch
kafka
diskSpace
```

---

### 18.3 Demo Success Criteria

- Service health is visible.
- Dependency failure changes health status or logs.
- Health endpoints can be used during demo setup and troubleshooting.

---

## 19. Recommended Demo Flow

For a clean live demo, use this order:

```text
1. Show Docker Compose services running.
2. Show health endpoints.
3. Login as admin, backoffice, and analyst.
4. Demonstrate authorization failure with fraud analyst submitting transaction.
5. Submit low-risk transaction.
6. Show transaction.created event evidence.
7. Show risk scoring result.
8. Show audit document searchable in Elasticsearch.
9. Demonstrate idempotency.
10. Submit guaranteed REVIEW transaction.
11. Submit BLOCKED transaction with blacklisted destination.
12. Show Redis blacklist key and velocity counters.
13. Update master data and show cache invalidation.
14. Show native SQL reporting endpoints.
15. Show kafka_event_logs.
16. Optionally show Redis/Elasticsearch degraded behavior.
17. End with final acceptance checklist.
```

---

## 20. Demo Checklist

Before demo:

```text
[ ] Docker Compose starts all services.
[ ] PostgreSQL migrations run successfully.
[ ] Seed data is available.
[ ] Kafka topics exist.
[ ] Redis is reachable.
[ ] Elasticsearch is reachable.
[ ] Login works for admin, backoffice, and analyst.
[ ] Swagger or Postman collection is ready.
[ ] Demo tokens are prepared.
```

During demo:

```text
[ ] Protected endpoint rejects missing or wrong role.
[ ] Transaction submission returns transactionRef.
[ ] Duplicate idempotency key returns existing transaction.
[ ] transaction.created is published.
[ ] Risk Engine consumes transaction.created.
[ ] Risk factors are stored.
[ ] Transaction status changes to APPROVED, REVIEW, or BLOCKED.
[ ] transaction.risk-scored is published.
[ ] Audit document is indexed.
[ ] Audit search returns masked accounts.
[ ] Redis cache key is populated.
[ ] Redis velocity counters increase.
[ ] Master data update invalidates Redis key.
[ ] Reporting endpoints return aggregated data.
[ ] kafka_event_logs shows event lifecycle.
```

After demo:

```text
[ ] End-to-end flow completed without manual database modification.
[ ] All core MVP acceptance criteria are demonstrated.
[ ] Known limitations are clearly identified.
```

---

## 21. Demo Traceability Matrix

| Demo Scenario | PRD MVP | Main Service | Supporting Contract |
|---|---|---|---|
| Authorization Boundary | Security / User Roles | Transaction Service | API Contract |
| Low-Risk Transaction Approved | MVP-01, MVP-02, MVP-03, MVP-05, MVP-06 | Transaction, Risk Engine, Audit Search | API, Kafka, DB |
| Idempotency | MVP-01 | Transaction Service | API, DB |
| High Amount Review | MVP-02 | Risk Engine Service | TRD, DB |
| Blacklisted Destination Blocked | MVP-02, MVP-04 | Risk Engine, Master Data | Redis, DB |
| Redis Velocity Counter | MVP-04 | Risk Engine Service | Redis Key Contract |
| Master Data Cache Invalidation | MVP-04 | Master Data Service | API, Redis, DB |
| Elasticsearch Audit Search | MVP-05, MVP-06 | Audit Search Service | API, Kafka |
| Native SQL Reporting | MVP-07 | Transaction Service | API, DB |
| Kafka Event Trace | MVP-03 | Transaction, Risk Engine, Audit Search | Kafka, DB |
| Failure and DLQ | MVP-03 | Risk Engine, Audit Search | Kafka |
| Redis Degraded Behavior | MVP-04 | Risk Engine Service | Redis |
| Elasticsearch Degraded Behavior | MVP-05 | Audit Search Service | Kafka, Elastic |
| Health Check | Operational Goal | All Services | API, TRD |

---

## 22. Final Demo Acceptance Criteria

The demo is considered successful when:

1. All required services run through Docker Compose.
2. User authentication works.
3. Authorization rules are enforced.
4. A valid transaction can be submitted.
5. Idempotency prevents duplicate transaction creation.
6. `transaction.created` is published.
7. Risk Engine consumes the event.
8. Risk Engine evaluates rules and stores risk factors.
9. Transaction status, risk score, and decision are updated in PostgreSQL.
10. `transaction.risk-scored` is published.
11. Audit Search Service indexes the result into Elasticsearch.
12. Fraud Analyst can search audit data.
13. Account numbers are masked in API and search responses.
14. Redis cache is used for risk reference data.
15. Redis velocity counters are created and expire automatically.
16. Master data updates invalidate related Redis keys.
17. Native SQL reports return aggregated risk information.
18. Event lifecycle is traceable through logs or `kafka_event_logs`.
19. At least one degraded dependency scenario is explained or demonstrated.
20. No manual database update is required to complete the normal end-to-end transaction flow.
