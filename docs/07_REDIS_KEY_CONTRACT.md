# Redis Key Contract
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the Redis key contract for **BankGuard**.

The Redis Key Contract provides implementation-level specifications for:

- Redis usage boundaries.
- Cache key naming conventions.
- Real-time counter key naming conventions.
- Value schemas.
- Time-to-live strategy.
- Cache-aside behavior.
- Cache invalidation triggers.
- Service ownership.
- PostgreSQL source mapping.
- Risk rule dependencies.
- Observability requirements.
- Testing and acceptance criteria.

This document is aligned with:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`
- `06_KAFKA_EVENT_CONTRACT.md`

---

### 1.2 Scope

This contract covers Redis usage required for the MVP:

1. Blacklisted account lookup cache.
2. Customer risk profile cache.
3. Trusted device lookup cache.
4. Risk rule configuration cache.
5. Transaction velocity count counter.
6. Transaction velocity amount counter.
7. Cache invalidation triggered by Master Data Service.
8. Redis usage by Risk Engine Service during risk scoring.
9. Observability and testing requirements for cache and counter behavior.

---

### 1.3 Non-Scope

This contract does not cover:

- Redis as a durable database.
- Redis as a source of truth.
- Redis Streams.
- Redis Pub/Sub.
- Redis Cluster production topology.
- Distributed locking.
- Session storage.
- Rate limiting for public APIs.
- Long-term analytics storage.
- Feature store for machine learning.

These may be introduced in later phases if required.

---

## 2. Redis Design Principles

### 2.1 PostgreSQL Remains Source of Truth

Redis must never be treated as the authoritative storage for business data.

PostgreSQL remains the source of truth for:

- blacklisted accounts
- customer risk profiles
- customer trusted devices
- risk rule configurations
- customer and account data
- transaction records
- risk factors

Redis stores only:

- temporary cached copies of frequently accessed risk reference data
- short-lived transaction velocity counters

If Redis data is missing, expired, or unavailable, the system must be able to fall back to PostgreSQL for cacheable reference data.

---

### 2.2 Redis Is Used to Reduce Repeated Risk Data Reads

Risk scoring requires repeated access to the same reference data, such as blacklist status, device trust status, customer risk profile, and risk rule configuration.

Redis is used to reduce repeated PostgreSQL lookups during transaction spikes.

---

### 2.3 Redis Is Used for Short-Lived Real-Time Risk Signals

The `HIGH_FREQUENCY_TRANSACTION` rule needs transaction velocity information within a short time window.

Redis counters are used to track:

- number of transactions by source account in a 10-minute window
- total transaction amount by source account in a 10-minute window

These counters are short-lived and expire automatically.

---

### 2.4 Cache Invalidation Is Required for Risk-Sensitive Data

Risk-sensitive data such as blacklisted accounts, trusted devices, customer risk profiles, and risk rule configurations must not rely only on TTL expiration.

When Master Data Service updates related PostgreSQL records, it must invalidate the affected Redis keys.

---

### 2.5 Cache Failures Must Not Break the Core Flow

For cacheable reference data, Redis failure should not automatically fail risk scoring.

Expected behavior:

1. Attempt Redis lookup.
2. If Redis is unavailable or key is missing, query PostgreSQL.
3. If PostgreSQL succeeds, continue risk evaluation.
4. Log Redis failure as a dependency warning.

For real-time counters, Redis unavailability may reduce velocity detection accuracy. The system should continue processing but log the degraded behavior.

---

## 3. Redis Ownership Matrix

| Redis Data Area | Key Pattern | Primary Writer | Primary Reader | Source of Truth |
|---|---|---|---|---|
| Blacklisted Account Cache | `blacklist:account:{accountNumber}` | Risk Engine Service | Risk Engine Service | `blacklisted_accounts` |
| Customer Risk Profile Cache | `customer:risk-profile:{customerId}` | Risk Engine Service | Risk Engine Service | `customer_risk_profiles` |
| Trusted Device Cache | `customer:trusted-device:{customerId}:{deviceId}` | Risk Engine Service | Risk Engine Service | `customer_devices` |
| Risk Rule Config Cache | `risk-rule:config:{ruleCode}` | Risk Engine Service | Risk Engine Service | `risk_rule_configs` |
| Velocity Count Counter | `risk:velocity:count:{sourceAccountNumber}:10m` | Risk Engine Service | Risk Engine Service | Redis temporary counter |
| Velocity Amount Counter | `risk:velocity:amount:{sourceAccountNumber}:10m` | Risk Engine Service | Risk Engine Service | Redis temporary counter |
| Cache Invalidation | all related keys | Master Data Service | Redis | PostgreSQL update event/API action |

---

## 4. Key Naming Convention

### 4.1 General Format

Redis keys must use lowercase domain-based names with colon separators.

```text
{domain}:{entity}:{identifier}
```

For scoped keys:

```text
{domain}:{entity}:{parentIdentifier}:{childIdentifier}
```

For windowed counters:

```text
{domain}:{metric}:{type}:{identifier}:{window}
```

---

### 4.2 Naming Rules

| Rule | Requirement |
|---|---|
| Separator | Use colon `:` |
| Case | Use lowercase for key segments |
| Identifiers | Preserve original identifier value where needed, such as account number or device ID |
| Spaces | Not allowed |
| Window suffix | Must be explicit, for example `10m` |
| Environment prefix | Optional for local development, recommended for shared Redis environments |

---

### 4.3 Optional Environment Prefix

For shared environments, keys may include an environment prefix.

```text
{environment}:bankguard:{key}
```

Examples:

```text
local:bankguard:blacklist:account:9876543210
dev:bankguard:risk:velocity:count:1234567890:10m
```

For MVP local development, the prefix is optional.

---

## 5. Cache Key Catalog

## 5.1 Blacklisted Account Cache

### Key Pattern

```text
blacklist:account:{accountNumber}
```

### Example

```text
blacklist:account:9876543210
```

### Purpose

Stores whether a destination account is actively blacklisted.

Used by the risk rule:

```text
BLACKLISTED_DESTINATION
```

### Source Table

```text
blacklisted_accounts
```

### Source Query

```sql
SELECT account_number, reason, active, updated_at
FROM blacklisted_accounts
WHERE account_number = :accountNumber
  AND active = TRUE;
```

### Value Type

JSON object.

### Value Schema

```json
{
  "accountNumber": "9876543210",
  "active": true,
  "reason": "Reported mule account",
  "updatedAt": "2026-06-04T10:00:00Z"
}
```

### TTL

```text
10 minutes
```

### TTL Reason

Blacklist status is risk-sensitive and may change. A short TTL reduces stale data risk while still reducing repeated database reads.

### Read Behavior

```text
1. Risk Engine checks Redis key.
2. If key exists and active = true, mark destinationBlacklisted = true.
3. If key does not exist, query PostgreSQL.
4. If PostgreSQL returns active blacklist record, store JSON value in Redis.
5. If PostgreSQL returns no active record, optionally cache a negative lookup.
```

### Negative Cache Option

For MVP, negative caching is optional.

If implemented:

```text
blacklist:account:{accountNumber}:negative
```

TTL:

```text
1 minute
```

Negative cache must be invalidated when a blacklist record is created or activated.

### Invalidation Trigger

Master Data Service must delete this key after:

- creating a blacklisted account
- updating a blacklisted account
- deactivating a blacklisted account

### Invalidation Command

```text
DEL blacklist:account:{accountNumber}
DEL blacklist:account:{accountNumber}:negative
```

### Failure Handling

If Redis is unavailable, Risk Engine must query PostgreSQL directly.

---

## 5.2 Customer Risk Profile Cache

### Key Pattern

```text
customer:risk-profile:{customerId}
```

### Example

```text
customer:risk-profile:1001
```

### Purpose

Stores customer risk metadata used during scoring.

Used by the risk rule:

```text
HIGH_RISK_CUSTOMER_PROFILE
```

### Source Table

```text
customer_risk_profiles
```

### Source Query

```sql
SELECT customer_id, risk_level, risk_reason, last_reviewed_at, updated_at
FROM customer_risk_profiles
WHERE customer_id = :customerId;
```

### Value Type

JSON object.

### Value Schema

```json
{
  "customerId": 1001,
  "riskLevel": "HIGH",
  "riskReason": "Previous suspicious activity",
  "lastReviewedAt": "2026-06-01T09:00:00Z",
  "updatedAt": "2026-06-04T10:00:00Z"
}
```

### TTL

```text
5 minutes
```

### TTL Reason

Customer risk profile is important for scoring and may be updated by administrators. A short TTL combined with explicit invalidation helps keep risk decisions current.

### Read Behavior

```text
1. Risk Engine checks Redis key.
2. If key exists, use riskLevel from Redis.
3. If key does not exist, query PostgreSQL.
4. Store PostgreSQL result in Redis with TTL.
5. If no profile exists, use LOW as default risk level.
```

### Default Behavior

If no record exists in PostgreSQL:

```text
riskLevel = LOW
```

For MVP, do not store missing profile as permanent data in Redis.

### Invalidation Trigger

Master Data Service must delete this key after:

- creating customer risk profile
- updating customer risk profile
- changing risk level

### Invalidation Command

```text
DEL customer:risk-profile:{customerId}
```

---

## 5.3 Trusted Device Cache

### Key Pattern

```text
customer:trusted-device:{customerId}:{deviceId}
```

### Example

```text
customer:trusted-device:1001:IPHONE-15-DEVICE-001
```

### Purpose

Stores whether a device is trusted for a customer.

Used by the risk rule:

```text
NEW_DEVICE
```

### Source Table

```text
customer_devices
```

### Source Query

```sql
SELECT customer_id, device_id, device_name, trusted, first_seen_at, last_used_at, updated_at
FROM customer_devices
WHERE customer_id = :customerId
  AND device_id = :deviceId;
```

### Value Type

JSON object.

### Value Schema

```json
{
  "customerId": 1001,
  "deviceId": "IPHONE-15-DEVICE-001",
  "deviceName": "Daniel iPhone",
  "trusted": true,
  "firstSeenAt": "2026-05-01T08:00:00Z",
  "lastUsedAt": "2026-06-04T10:00:00Z",
  "updatedAt": "2026-06-04T10:00:00Z"
}
```

### TTL

```text
10 minutes
```

### TTL Reason

Trusted device data is frequently read during mobile and internet banking transaction scoring. TTL reduces database read pressure while allowing updates to eventually refresh.

### Read Behavior

```text
1. If deviceId is null, do not check Redis and do not trigger NEW_DEVICE.
2. Risk Engine checks trusted device key.
3. If key exists and trusted = true, set trustedDevice = true.
4. If key exists and trusted = false, set trustedDevice = false.
5. If key does not exist, query PostgreSQL.
6. Store PostgreSQL result in Redis.
7. If no PostgreSQL record exists, set trustedDevice = false.
```

### Invalidation Trigger

Master Data Service must delete this key after:

- registering trusted device
- updating trusted device
- changing `trusted` flag

### Invalidation Command

```text
DEL customer:trusted-device:{customerId}:{deviceId}
```

---

## 5.4 Risk Rule Config Cache

### Key Pattern

```text
risk-rule:config:{ruleCode}
```

### Example

```text
risk-rule:config:HIGH_AMOUNT
risk-rule:config:HIGH_FREQUENCY_TRANSACTION_COUNT
risk-rule:config:HIGH_FREQUENCY_TRANSACTION_AMOUNT
```

### Purpose

Stores rule threshold, score, active status, and description.

Used by Risk Engine Service to evaluate active rules.

### Source Table

```text
risk_rule_configs
```

### Source Query

```sql
SELECT rule_code, rule_name, score, threshold_value, active, description, updated_at
FROM risk_rule_configs
WHERE rule_code = :ruleCode;
```

### Value Type

JSON object.

### Value Schema

```json
{
  "ruleCode": "HIGH_AMOUNT",
  "ruleName": "High Amount Transaction",
  "score": 25,
  "thresholdValue": 10000000,
  "active": true,
  "description": "Triggered when transaction amount exceeds configured threshold",
  "updatedAt": "2026-06-04T10:00:00Z"
}
```

### TTL

```text
5 minutes
```

### TTL Reason

Rule configuration may be updated by administrators. A short TTL allows configuration updates to take effect quickly while reducing repeated database reads.

### Read Behavior

```text
1. Risk Engine checks Redis key for each required rule.
2. If key exists, use cached rule config.
3. If key does not exist, query PostgreSQL.
4. Store result in Redis with TTL.
5. If no PostgreSQL config exists, use application default only for MVP fallback.
```

### Required Rule Codes

```text
HIGH_AMOUNT
NEW_DEVICE
BLACKLISTED_DESTINATION
HIGH_FREQUENCY_TRANSACTION_COUNT
HIGH_FREQUENCY_TRANSACTION_AMOUNT
UNUSUAL_LOCATION
HIGH_RISK_CUSTOMER_PROFILE
```

### Invalidation Trigger

Master Data Service must delete this key after:

- updating rule score
- updating rule threshold
- activating or deactivating rule
- updating rule description

### Invalidation Command

```text
DEL risk-rule:config:{ruleCode}
```

---

## 6. Real-Time Counter Key Catalog

## 6.1 Transaction Velocity Count Counter

### Key Pattern

```text
risk:velocity:count:{sourceAccountNumber}:10m
```

### Example

```text
risk:velocity:count:1234567890:10m
```

### Purpose

Stores the number of submitted transactions from a source account during a rolling 10-minute window.

Used by the risk rule:

```text
HIGH_FREQUENCY_TRANSACTION
```

### Value Type

Integer.

### Example Value

```text
6
```

### TTL

```text
10 minutes
```

### Write Operation

Risk Engine Service must increment this key during transaction context preparation or scoring.

```text
INCR risk:velocity:count:{sourceAccountNumber}:10m
EXPIRE risk:velocity:count:{sourceAccountNumber}:10m 600
```

### Recommended Atomic Behavior

The increment and TTL assignment should be executed atomically using a Redis transaction, Lua script, or application logic that sets TTL only when the counter is first created.

Recommended behavior:

```text
1. INCR key.
2. If result == 1, EXPIRE key 600 seconds.
```

This prevents extending the window on every transaction.

### Read Behavior

Risk Engine reads the counter value after increment.

```text
velocityCount10m = current counter value
```

### Default Value

If key does not exist:

```text
0
```

### Threshold

Default trigger threshold:

```text
velocityCount10m >= 5
```

### Expiration Behavior

The key must expire automatically after 10 minutes.

Expired counters must not be restored from PostgreSQL.

---

## 6.2 Transaction Velocity Amount Counter

### Key Pattern

```text
risk:velocity:amount:{sourceAccountNumber}:10m
```

### Example

```text
risk:velocity:amount:1234567890:10m
```

### Purpose

Stores the total transaction amount from a source account during a rolling 10-minute window.

Used by the risk rule:

```text
HIGH_FREQUENCY_TRANSACTION
```

### Value Type

Decimal number represented as Redis string.

### Example Value

```text
75000000
```

### TTL

```text
10 minutes
```

### Write Operation

Risk Engine Service must increment this key by transaction amount.

```text
INCRBYFLOAT risk:velocity:amount:{sourceAccountNumber}:10m 25000000
EXPIRE risk:velocity:amount:{sourceAccountNumber}:10m 600
```

### Recommended Atomic Behavior

The amount increment and TTL assignment should follow the same first-write TTL behavior as the count counter.

Recommended behavior:

```text
1. INCRBYFLOAT key amount.
2. If key was newly created, EXPIRE key 600 seconds.
```

### Read Behavior

Risk Engine reads the amount counter after increment.

```text
velocityAmount10m = current accumulated amount
```

### Default Value

If key does not exist:

```text
0
```

### Threshold

Default trigger threshold:

```text
velocityAmount10m >= 50000000 IDR
```

### Expiration Behavior

The key must expire automatically after 10 minutes.

Expired counters must not be restored from PostgreSQL.

---

## 7. Cache-Aside Requirements

### 7.1 Standard Cache-Aside Flow

All cacheable reference data must follow this flow:

```text
1. Build Redis key.
2. Attempt Redis GET.
3. If cache hit, deserialize value.
4. If cache miss, query PostgreSQL.
5. If PostgreSQL returns data, serialize and store in Redis with TTL.
6. If PostgreSQL returns no data, use safe default behavior.
7. Continue risk scoring.
```

---

### 7.2 Cache Hit Requirement

A cache hit occurs when:

- key exists
- value can be deserialized
- value passes basic validation

If deserialization fails, the key must be deleted and the system must fall back to PostgreSQL.

---

### 7.3 Cache Miss Requirement

A cache miss occurs when:

- key does not exist
- key expired
- key is deleted by invalidation
- value is invalid and removed

On cache miss, Risk Engine must query PostgreSQL.

---

### 7.4 Cache Write Requirement

Cache writes must include TTL.

Cache values must be stored as JSON strings for structured objects.

Example:

```text
SET customer:risk-profile:1001 '{...json...}' EX 300
```

---

## 8. Cache Invalidation Requirements

### 8.1 Invalidation Owner

Master Data Service owns cache invalidation for master data changes.

Risk Engine Service may write cache entries, but Master Data Service must invalidate keys when source data changes.

---

### 8.2 Invalidation Matrix

| Source Change | Source Table | API Trigger | Redis Key Deleted |
|---|---|---|---|
| Create blacklisted account | `blacklisted_accounts` | `POST /api/v1/blacklisted-accounts` | `blacklist:account:{accountNumber}` |
| Update blacklisted account | `blacklisted_accounts` | `PUT /api/v1/blacklisted-accounts/{id}` | `blacklist:account:{accountNumber}` |
| Deactivate blacklisted account | `blacklisted_accounts` | `DELETE /api/v1/blacklisted-accounts/{id}` | `blacklist:account:{accountNumber}` |
| Register trusted device | `customer_devices` | `POST /api/v1/customers/{customerId}/trusted-devices` | `customer:trusted-device:{customerId}:{deviceId}` |
| Update trusted device | `customer_devices` | `PUT /api/v1/customers/{customerId}/trusted-devices/{deviceId}` | `customer:trusted-device:{customerId}:{deviceId}` |
| Update customer risk profile | `customer_risk_profiles` | `PUT /api/v1/customers/{customerId}/risk-profile` | `customer:risk-profile:{customerId}` |
| Update risk rule config | `risk_rule_configs` | `PUT /api/v1/risk-rules/{ruleCode}` | `risk-rule:config:{ruleCode}` |

---

### 8.3 Invalidation Timing

Cache invalidation must happen only after the PostgreSQL transaction has successfully committed.

If database update fails:

```text
Do not invalidate cache.
```

If cache invalidation fails after database success:

```text
Return successful API response if data update succeeded.
Log cache invalidation failure.
Allow TTL to expire stale data.
```

This is acceptable for MVP because TTL is short.

---

## 9. Redis Usage by Risk Rule

| Risk Rule | Redis Dependency | Key |
|---|---|---|
| `HIGH_AMOUNT` | Risk rule config cache | `risk-rule:config:HIGH_AMOUNT` |
| `NEW_DEVICE` | Trusted device cache and rule config cache | `customer:trusted-device:{customerId}:{deviceId}`, `risk-rule:config:NEW_DEVICE` |
| `BLACKLISTED_DESTINATION` | Blacklisted account cache and rule config cache | `blacklist:account:{destinationAccountNumber}`, `risk-rule:config:BLACKLISTED_DESTINATION` |
| `HIGH_FREQUENCY_TRANSACTION` | Velocity counters and rule config cache | `risk:velocity:count:{sourceAccountNumber}:10m`, `risk:velocity:amount:{sourceAccountNumber}:10m`, `risk-rule:config:HIGH_FREQUENCY_TRANSACTION_COUNT`, `risk-rule:config:HIGH_FREQUENCY_TRANSACTION_AMOUNT` |
| `UNUSUAL_LOCATION` | Rule config cache; known location is read from PostgreSQL for MVP | `risk-rule:config:UNUSUAL_LOCATION` |
| `HIGH_RISK_CUSTOMER_PROFILE` | Customer risk profile cache and rule config cache | `customer:risk-profile:{customerId}`, `risk-rule:config:HIGH_RISK_CUSTOMER_PROFILE` |

---

## 10. Serialization Requirements

### 10.1 Object Values

Structured cache values must be serialized as JSON strings.

Required object keys must use camelCase.

Example:

```json
{
  "customerId": 1001,
  "riskLevel": "HIGH",
  "updatedAt": "2026-06-04T10:00:00Z"
}
```

---

### 10.2 Numeric Counter Values

Counter values must be stored as Redis numeric strings.

Examples:

```text
6
75000000
```

Application code must parse amount counters into `BigDecimal`.

---

### 10.3 Datetime Values

Datetime values inside JSON must use ISO-8601 UTC.

Example:

```text
2026-06-04T10:00:00Z
```

---

## 11. TTL Requirements

## 11.1 TTL Summary

| Key Pattern | TTL |
|---|---:|
| `blacklist:account:{accountNumber}` | 10 minutes |
| `blacklist:account:{accountNumber}:negative` | 1 minute |
| `customer:risk-profile:{customerId}` | 5 minutes |
| `customer:trusted-device:{customerId}:{deviceId}` | 10 minutes |
| `risk-rule:config:{ruleCode}` | 5 minutes |
| `risk:velocity:count:{sourceAccountNumber}:10m` | 10 minutes |
| `risk:velocity:amount:{sourceAccountNumber}:10m` | 10 minutes |

---

## 11.2 TTL Rules

- Every cache key must have TTL.
- Every counter key must have TTL.
- Cache keys must not be stored without expiration.
- Counter TTL must represent the window size.
- Velocity counter TTL should be set only when the key is first created.
- Invalidation should not rely only on TTL for risk-sensitive master data.

---

## 12. Redis Failure Handling

### 12.1 Redis Read Failure

If Redis read fails for cacheable reference data:

```text
1. Log Redis read failure.
2. Query PostgreSQL.
3. Continue risk scoring if PostgreSQL succeeds.
```

---

### 12.2 Redis Write Failure

If PostgreSQL lookup succeeds but Redis write fails:

```text
1. Log Redis write failure.
2. Continue risk scoring using PostgreSQL result.
```

---

### 12.3 Redis Counter Failure

If velocity counter update fails:

```text
1. Log counter update failure.
2. Continue scoring without velocity contribution.
3. Mark velocity metadata as unavailable.
```

The `HIGH_FREQUENCY_TRANSACTION` rule must not trigger from missing counter values alone.

---

### 12.4 Redis Invalidation Failure

If invalidation fails after a successful master data update:

```text
1. Log invalidation failure.
2. Return success for the master data update.
3. Allow TTL to eventually expire the old cache.
```

---

## 13. Observability Requirements

### 13.1 Required Log Fields

Redis-related logs must include:

- `service`
- `requestId` where available
- `eventId` where available
- `transactionRef` where available
- `redisKey`
- `operation`
- `result`
- `durationMs`

Example:

```json
{
  "service": "risk-engine-service",
  "transactionRef": "TRX-20260604-000001",
  "redisKey": "blacklist:account:9876543210",
  "operation": "GET",
  "result": "CACHE_HIT",
  "durationMs": 4
}
```

---

### 13.2 Required Metrics

Recommended metrics:

```text
redis_cache_hit_total
redis_cache_miss_total
redis_cache_write_total
redis_cache_invalidation_total
redis_cache_error_total
redis_counter_increment_total
redis_counter_error_total
redis_operation_duration_ms
```

Labels:

```text
service
keyType
operation
result
```

---

## 14. Security and Data Sensitivity

### 14.1 Sensitive Data Rules

Redis must not store:

- password hashes
- JWT tokens
- raw authentication credentials
- full customer personal profile beyond what is required for risk scoring

Redis may store:

- account numbers required for blacklist lookup
- device IDs required for trusted device lookup
- customer IDs
- risk levels
- rule configuration

---

### 14.2 Access Control

For local MVP deployment, Redis may run without authentication inside Docker Compose network.

For non-local environments, Redis must require authentication and must not be exposed publicly.

---

## 15. Local Development Configuration

### 15.1 Docker Compose Service

```yaml
redis:
  image: redis:7
  container_name: bankguard-redis
  ports:
    - "6379:6379"
```

---

### 15.2 Spring Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

---

### 15.3 Required Environment Variables

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD optional for local MVP
```

---

## 16. Testing Requirements

### 16.1 Cache Hit Test

Scenario:

```text
Given Redis contains blacklist:account:9876543210
When Risk Engine evaluates a transaction to destination account 9876543210
Then Risk Engine uses Redis value
And PostgreSQL blacklist query is not required
And BLACKLISTED_DESTINATION is triggered
```

---

### 16.2 Cache Miss Test

Scenario:

```text
Given Redis does not contain customer:risk-profile:1001
And PostgreSQL contains risk profile HIGH for customer 1001
When Risk Engine builds transaction context
Then Risk Engine loads risk profile from PostgreSQL
And stores customer:risk-profile:1001 in Redis with TTL
```

---

### 16.3 Cache Invalidation Test

Scenario:

```text
Given Redis contains risk-rule:config:HIGH_AMOUNT
When Master Data Service updates HIGH_AMOUNT threshold
Then Master Data Service deletes risk-rule:config:HIGH_AMOUNT
And the next Risk Engine lookup reloads the new value from PostgreSQL
```

---

### 16.4 Velocity Counter Test

Scenario:

```text
Given source account 1234567890 submits multiple transactions
When Risk Engine processes the fifth transaction within 10 minutes
Then risk:velocity:count:1234567890:10m is greater than or equal to 5
And HIGH_FREQUENCY_TRANSACTION is triggered
```

---

### 16.5 Counter Expiration Test

Scenario:

```text
Given risk:velocity:count:1234567890:10m exists
When 10 minutes passes
Then the key expires automatically
And a new transaction starts a new velocity window
```

---

### 16.6 Redis Failure Fallback Test

Scenario:

```text
Given Redis is unavailable
When Risk Engine checks customer risk profile
Then Risk Engine queries PostgreSQL directly
And risk scoring continues
And Redis failure is logged
```

---

## 17. Acceptance Criteria

Redis implementation is considered complete when:

1. All required cache keys follow the documented key patterns.
2. All structured cache values follow the documented JSON schemas.
3. All cache keys have TTL.
4. All velocity counters have TTL.
5. Risk Engine uses Redis cache-aside before PostgreSQL for cacheable data.
6. Risk Engine updates velocity count and amount counters during scoring.
7. Master Data Service invalidates related keys after successful data changes.
8. Redis cache miss falls back to PostgreSQL.
9. Redis read/write failure does not stop risk scoring when PostgreSQL is available.
10. High-frequency transaction rule can use Redis counters.
11. Redis operations are logged with key, operation, result, and duration.
12. Cache hit, cache miss, invalidation, counter expiration, and Redis fallback tests pass.

---

## 18. Traceability Matrix

| PRD Capability | Redis Support |
|---|---|
| MVP-02 Risk Scoring Engine | Provides cached risk reference data and velocity counters for risk rule evaluation. |
| MVP-04 Redis Risk Cache and Real-Time Risk Counters | Fully defined by this contract. |
| MVP-06 Transaction Investigation API | Indirect support through consistent risk scoring results stored in PostgreSQL and indexed to Elasticsearch. |

| System Design Area | Redis Support |
|---|---|
| Risk Engine Service | Cache lookup, cache fallback, velocity counters. |
| Master Data Service | Cache invalidation after master data updates. |
| Data Architecture | Redis remains non-authoritative temporary storage. |
| Reliability | Redis fallback and degraded counter behavior. |

| Database Table | Redis Key |
|---|---|
| `blacklisted_accounts` | `blacklist:account:{accountNumber}` |
| `customer_risk_profiles` | `customer:risk-profile:{customerId}` |
| `customer_devices` | `customer:trusted-device:{customerId}:{deviceId}` |
| `risk_rule_configs` | `risk-rule:config:{ruleCode}` |
| temporary counter only | `risk:velocity:count:{sourceAccountNumber}:10m` |
| temporary counter only | `risk:velocity:amount:{sourceAccountNumber}:10m` |
