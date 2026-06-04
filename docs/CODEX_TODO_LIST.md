# Codex Execution Todo List
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document is a Codex-oriented execution checklist for building **BankGuard** from an initialized or newly generated project into a complete application that matches the existing requirement documents.

This file is different from the general implementation todo list.

The purpose of this document is to guide Codex step by step so it can:

1. inspect the current project state,
2. verify whether the project structure matches the required architecture,
3. check whether the technology stack and dependencies match the requirements,
4. implement missing modules one by one,
5. update the code safely in small increments,
6. run tests after each meaningful change,
7. mark completed steps,
8. stop and report clearly when a requirement cannot be completed.

---

### 1.2 Source Documents

Codex must treat the following files as the source of truth:

```text
docs/
├── 01_PRD.md
├── 02_SYSTEM_DESIGN_ARCHITECTURE.md
├── 03_TRD_TECHNICAL_REQUIREMENTS.md
├── 04_API_CONTRACT.md
├── 05_DATABASE_SCHEMA.md
├── 06_KAFKA_EVENT_CONTRACT.md
├── 07_REDIS_KEY_CONTRACT.md
├── 08_TEST_SCENARIOS.md
├── 09_DEMO_SCENARIOS.md
├── 10_UNIT_TEST_REQUIREMENTS.md
└── 11_TODO_LIST.md
```

If any document is missing, Codex must not guess silently. It must record the missing document in the execution notes and continue only when the missing information can be safely inferred from the remaining files.

---

### 1.3 Execution Rule for Codex

Codex must execute this checklist sequentially unless a task explicitly says it may be done in parallel.

For every completed task, Codex should change:

```text
[ ] 
```

to:

```text
[ ] 
```

For every blocked task, Codex should mark:

```text
[!] Blocked
```

and add a short reason under the task.

For every skipped task, Codex should mark:

```text
[-] Skipped
```

and explain why it was skipped.

---

### 1.4 Completion Meaning

A task is complete only when:

1. code or configuration has been updated,
2. the update is consistent with the relevant requirement documents,
3. related unit tests are added where applicable,
4. related integration/API/event tests are added where applicable,
5. `mvn test` passes for affected modules,
6. the project still compiles,
7. no unrelated behavior is broken.

Code compiling alone is not enough.

---

### 1.5 Codex Working Principles

Codex must follow these principles:

- Make small changes.
- Prefer one feature slice at a time.
- Do not implement unrelated features early.
- Do not rename public contracts without checking API/Kafka/Redis/DB documents.
- Do not invent new endpoints unless required by the existing docs.
- Do not change the transaction flow without checking PRD, System Design, TRD, Kafka Contract, and Demo Scenarios.
- Keep PostgreSQL as the source of truth.
- Keep Elasticsearch as an investigation read model.
- Keep Redis as cache and short-lived counters only.
- Keep Kafka consumers idempotent.
- Keep unit tests isolated from infrastructure.
- Run tests often.
- Update this checklist after each completed step.

---

## 2. Phase 0 - Initial Project Scan

---

### 2.1 Inspect Repository Root

[x] Scan repository root.

Goal:  
Understand the current project state before modifying anything.

Codex action:

- list root files and folders.
- identify whether this is:
  - empty repository,
  - partially initialized Maven project,
  - single Spring Boot app,
  - multi-module Maven project,
  - unrelated existing project.

Expected output:

```text
Repository state identified.
```

Done when:

- Codex knows whether it must create a new structure or adapt an existing one.
- Codex records any mismatch with the expected structure.

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `11_TODO_LIST.md`

---

### 2.2 Check Required Documentation Files

[x] Verify all requirement documents exist.

Codex action:

- inspect `docs/`.
- check for all source documents listed in section 1.2.
- report missing or differently named documents.

Done when:

- all required documents are found, or
- missing files are documented as blockers/warnings.

Related docs:

- All documentation files.

---

### 2.3 Read Source Documents Before Coding

[x] Read the requirement documents.

Codex action:

Read at minimum:

- PRD MVP scope.
- System Design service architecture.
- TRD repository and technical stack requirements.
- API Contract endpoint list and standards.
- Database Schema table list and migration order.
- Kafka Event Contract topic and envelope requirements.
- Redis Key Contract key patterns and TTL requirements.
- Unit Test Requirements quality gates.
- Test Scenarios and Demo Scenarios acceptance flows.

Done when:

- Codex can summarize the required application structure.
- Codex can identify the main flow:

```text
Transaction API
-> PostgreSQL
-> transaction.created
-> Risk Engine
-> Redis lookup/counter
-> PostgreSQL risk update
-> transaction.risk-scored
-> Audit Search Service
-> Elasticsearch
-> Investigation and Reporting APIs
```

Related docs:

- All documentation files.

---

### 2.4 Inspect Existing Maven Configuration

[x] Inspect Maven configuration.

Codex action:

- check whether root `pom.xml` exists.
- check parent/child module configuration.
- check Java version.
- check Spring Boot version.
- check dependency management.
- check plugin configuration.

Done when:

- Codex knows whether Maven must be created or fixed.
- Java version is verified against requirement.
- modules are verified or missing modules are listed.

Expected requirement:

```text
Java 21
Spring Boot 4.0.6
Maven multi-module project
```

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 2.5 Inspect Existing Application Modules

[x] Inspect service modules.

Codex action:

Check whether these modules exist:

```text
common-library
transaction-service
risk-engine-service
audit-search-service
master-data-service
```

Done when:

- existing modules are identified.
- missing modules are listed.
- incorrect module names are listed.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`

---

### 2.6 Inspect Existing Docker Compose

[x] Inspect Docker Compose configuration.

Codex action:

- check whether `docker-compose.yml` exists.
- verify required services:
  - PostgreSQL
  - Kafka
  - Redis
  - Elasticsearch
  - Transaction Service
  - Risk Engine Service
  - Audit Search Service
  - Master Data Service

Done when:

- infrastructure gaps are listed.
- service ports are compared to Demo Scenarios.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `09_DEMO_SCENARIOS.md`

---

### 2.7 Inspect Existing Tests

[x] Inspect current test structure.

Codex action:

- locate test directories.
- check whether JUnit 5 is configured.
- check whether Mockito and AssertJ are available.
- check whether Testcontainers dependencies exist.
- identify current unit/integration test separation.

Done when:

- test baseline is known.
- missing test tooling is listed.

Related docs:

- `08_TEST_SCENARIOS.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

## 3. Phase 1 - Repository and Build Foundation

---

### 3.1 Create or Fix Root Maven Parent

[x] Ensure root Maven parent project exists.

Codex action:

- create/fix root `pom.xml`.
- set packaging to `pom`.
- register modules:
  - `common-library`
  - `transaction-service`
  - `risk-engine-service`
  - `audit-search-service`
  - `master-data-service`.

Done when:

- `mvn clean validate` works from root.
- all modules are recognized.

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `11_TODO_LIST.md`

---

### 3.2 Configure Java and Spring Boot Version

[x] Ensure Java and Spring Boot versions match requirements.

Codex action:

- set Java version to `21`.
- use Spring Boot `4.0.6`.
- centralize dependency versions through dependency management.

Done when:

- all modules compile against Java 21.
- no module overrides Java version incorrectly.

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`

---

### 3.3 Configure Common Test Dependencies

[x] Add test dependencies.

Codex action:

Add or verify:

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- Testcontainers dependencies where required
- JaCoCo for coverage

Done when:

- unit tests can run with `mvn test`.
- coverage plugin is configured.
- no infrastructure is required for unit tests.

Related docs:

- `08_TEST_SCENARIOS.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 3.4 Create Standard Project Metadata

[x] Add standard project files.

Codex action:

Create/update:

- `.gitignore`
- `.editorconfig`
- `README.md`
- optional `.env.example`

Done when:

- common generated files are ignored.
- basic project instructions exist.
- environment variables are documented.

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `09_DEMO_SCENARIOS.md`

---

### 3.5 Create Docs Folder Order

[x] Ensure docs are placed in final order.

Codex action:

Verify or create:

```text
docs/
├── 01_PRD.md
├── 02_SYSTEM_DESIGN_ARCHITECTURE.md
├── 03_TRD_TECHNICAL_REQUIREMENTS.md
├── 04_API_CONTRACT.md
├── 05_DATABASE_SCHEMA.md
├── 06_KAFKA_EVENT_CONTRACT.md
├── 07_REDIS_KEY_CONTRACT.md
├── 08_TEST_SCENARIOS.md
├── 09_DEMO_SCENARIOS.md
├── 10_UNIT_TEST_REQUIREMENTS.md
└── 11_TODO_LIST.md
```

Done when:

- README links to the docs.
- file naming is consistent.

Related docs:

- All docs.

---

## 4. Phase 2 - Common Library Implementation

---

### 4.1 Create Common Library Module

[ ] Create `common-library`.

Codex action:

- create Maven module.
- set package root:

```text
com.bankguard.common
```

Done when:

- other services can depend on it.
- module has no service-specific business logic.

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`

---

### 4.2 Implement Common Error Contract

[ ] Implement common API error model.

Codex action:

Create:

- `CommonErrorResponse`
- `ErrorDetail`
- standard error code constants or enum.

Done when:

- fields match API Contract:
  - timestamp
  - status
  - error
  - message
  - details
  - path
  - requestId.
- unit tests cover object creation where logic exists.

Related docs:

- `04_API_CONTRACT.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 4.3 Implement Pagination Contract

[ ] Implement `PageResponse<T>`.

Codex action:

- create generic response wrapper.
- fields:
  - data
  - page
  - size
  - total.

Done when:

- format matches API Contract.
- tests cover constructor/static factory behavior if any.

Related docs:

- `04_API_CONTRACT.md`

---

### 4.4 Implement Account Masking Utility

[ ] Implement account number masking.

Codex action:

Create `AccountMaskingUtil`.

Rules:

- length >= 7: keep first 3 and last 3, replace middle with `****`.
- length < 7: mask all except last 2.
- null/blank behavior must be deterministic.

Done when:

- `1234567890` becomes `123****890`.
- short/null/blank cases are unit tested.

Related docs:

- `04_API_CONTRACT.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 4.5 Implement Event Envelope

[ ] Implement `EventEnvelope<T>`.

Codex action:

Create event envelope with:

- eventId
- eventType
- eventVersion
- aggregateType
- aggregateId
- occurredAt
- producer
- requestId
- correlationId
- payload.

Done when:

- structure matches Kafka Event Contract.
- helper/builder uses deterministic testable time/id generation.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 4.6 Implement Common Constants and Enums

[ ] Implement shared constants/enums.

Codex action:

Create constants/enums for:

- transaction status
- risk decision
- roles
- channels
- Kafka topics
- producer names
- cache key base segments
- error codes.

Done when:

- values match API, Database, Kafka, and Redis contracts.
- no duplicated string values appear unnecessarily in services.

Related docs:

- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`
- `06_KAFKA_EVENT_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`

---

### 4.7 Implement Correlation ID Support

[ ] Implement correlation ID utilities.

Codex action:

- create constants for `X-Request-Id`.
- create request ID fallback generator.
- create optional servlet filter if shared.

Done when:

- services can include requestId in logs and errors.
- unit tests cover fallback generation logic where possible.

Related docs:

- `04_API_CONTRACT.md`
- `06_KAFKA_EVENT_CONTRACT.md`

---

## 5. Phase 3 - Local Infrastructure

---

### 5.1 Create Docker Compose Infrastructure

[ ] Create or update `docker-compose.yml`.

Codex action:

Add services:

- PostgreSQL
- Kafka
- Redis
- Elasticsearch.

Done when:

- `docker compose config` passes.
- infrastructure service names are stable.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `09_DEMO_SCENARIOS.md`

---

### 5.2 Configure PostgreSQL

[ ] Configure PostgreSQL container.

Codex action:

- use PostgreSQL 16.
- expose port `5432`.
- create database `bankguard`.
- configure user/password.
- add volume.

Done when:

- app services can connect.
- database survives container restart.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `09_DEMO_SCENARIOS.md`

---

### 5.3 Configure Kafka

[ ] Configure Kafka container.

Codex action:

- expose port `9092`.
- ensure services can connect by internal Docker hostname.
- prepare required topics:
  - `transaction.created`
  - `transaction.risk-scored`
  - `transaction.created.dlq`
  - `transaction.risk-scored.dlq`.

Done when:

- topics exist or are auto-created safely.
- Kafka can be reached by services.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`

---

### 5.4 Configure Redis

[ ] Configure Redis container.

Codex action:

- expose port `6379`.
- keep Redis as cache/counter only.
- do not configure Redis as source of truth.

Done when:

- services can connect to Redis.
- Redis health check passes if configured.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`

---

### 5.5 Configure Elasticsearch

[ ] Configure Elasticsearch container.

Codex action:

- expose port `9200`.
- configure local single-node mode.
- ensure Audit Search Service can connect.

Done when:

- Elasticsearch health is available.
- index can be created by service or startup flow.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `09_DEMO_SCENARIOS.md`

---

## 6. Phase 4 - Database Migration and Seed Data

---

### 6.1 Add Flyway to Transaction Service

[ ] Configure Flyway.

Codex action:

- add Flyway dependency where migration is owned.
- create migration folder:

```text
transaction-service/src/main/resources/db/migration
```

Done when:

- migrations run at startup.
- failed migration stops application startup.

Related docs:

- `05_DATABASE_SCHEMA.md`

---

### 6.2 Create User and Role Tables

[ ] Create identity tables migration.

Codex action:

Implement tables:

- `users`
- `roles`
- `user_roles`.

Done when:

- constraints match Database Schema.
- seed users can be inserted.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `04_API_CONTRACT.md`

---

### 6.3 Create Customer and Account Tables

[ ] Create customer/account migrations.

Codex action:

Implement tables:

- `customers`
- `customer_risk_profiles`
- `accounts`.

Done when:

- table names, columns, constraints, and indexes match Database Schema.
- account status enum constraints are enforced.

Related docs:

- `05_DATABASE_SCHEMA.md`

---

### 6.4 Create Transaction Tables

[ ] Create transaction migrations.

Codex action:

Implement tables:

- `transactions`
- `transaction_risk_factors`.

Done when:

- transaction ref is unique.
- idempotency key is unique.
- amount positive check exists.
- status and decision constraints exist.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `04_API_CONTRACT.md`

---

### 6.5 Create Fraud Reference Tables

[ ] Create fraud reference migrations.

Codex action:

Implement tables:

- `blacklisted_accounts`
- `customer_devices`
- `customer_locations`
- `risk_rule_configs`.

Done when:

- tables support all initial risk rules.
- indexes support lookup needs.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `07_REDIS_KEY_CONTRACT.md`

---

### 6.6 Create Kafka Event Log Table

[ ] Create `kafka_event_logs`.

Codex action:

- implement table according to Database Schema.
- include status values:
  - PUBLISHED
  - CONSUMED
  - FAILED
  - DLQ
  - IGNORED.

Done when:

- event publishing and consuming can be traced.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `06_KAFKA_EVENT_CONTRACT.md`

---

### 6.7 Add Seed Data

[ ] Add deterministic seed data.

Codex action:

Insert demo/test data:

- admin/backoffice/analyst users.
- roles.
- customers.
- accounts.
- customer risk profiles.
- blacklisted accounts.
- trusted devices.
- known locations.
- risk rule configs.

Done when:

- seed data matches Test Scenarios and Demo Scenarios.
- login demo users exist.
- transaction demo accounts exist.

Related docs:

- `08_TEST_SCENARIOS.md`
- `09_DEMO_SCENARIOS.md`

---

### 6.8 Verify Migration Startup

[ ] Verify app can start and migrate database.

Codex action:

- run the service with PostgreSQL.
- verify Flyway applies migrations.
- query tables manually or through test.

Done when:

- all tables exist.
- seed rows exist.
- no migration error occurs.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `09_DEMO_SCENARIOS.md`

---

## 7. Phase 5 - Service Boilerplate

---

### 7.1 Create Transaction Service Boilerplate

[ ] Create Transaction Service Spring Boot app.

Codex action:

- package root:

```text
com.bankguard.transaction
```

- create standard packages:
  - config
  - controller
  - dto
  - entity
  - repository
  - service
  - mapper
  - exception
  - security
  - kafka
  - util.

Done when:

- service starts.
- health endpoint is available.
- service uses port `8081`.

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`

---

### 7.2 Create Risk Engine Service Boilerplate

[ ] Create Risk Engine Service Spring Boot app.

Codex action:

- package root:

```text
com.bankguard.riskengine
```

- create packages:
  - consumer
  - rule
  - service
  - repository
  - cache
  - config
  - dto
  - mapper
  - exception.

Done when:

- service starts.
- service can connect to Kafka/PostgreSQL/Redis.
- no REST API is required unless health endpoint.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`

---

### 7.3 Create Audit Search Service Boilerplate

[ ] Create Audit Search Service Spring Boot app.

Codex action:

- package root:

```text
com.bankguard.auditsearch
```

- create packages:
  - consumer
  - document
  - repository
  - controller
  - service
  - mapper
  - search
  - config
  - exception.

Done when:

- service starts.
- service uses port `8083`.
- service can connect to Kafka and Elasticsearch.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `04_API_CONTRACT.md`

---

### 7.4 Create Master Data Service Boilerplate

[ ] Create Master Data Service Spring Boot app.

Codex action:

- package root:

```text
com.bankguard.masterdata
```

- create packages:
  - controller
  - service
  - repository
  - entity
  - dto
  - mapper
  - cache
  - config
  - exception.

Done when:

- service starts.
- service uses port `8084`.
- service can connect to PostgreSQL and Redis.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `04_API_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`

---

## 8. Phase 6 - Security and Common API Behavior

---

### 8.1 Implement JWT Login

[ ] Implement `POST /api/v1/auth/login`.

Codex action:

- implement login in Transaction Service.
- authenticate against seeded users.
- return access token, token type, expiry, and roles.

Done when:

- admin/backoffice/analyst can login.
- invalid credentials return `UNAUTHORIZED`.
- response matches API Contract.

Related docs:

- `04_API_CONTRACT.md`
- `09_DEMO_SCENARIOS.md`

---

### 8.2 Implement JWT Validation

[ ] Implement JWT validation for protected endpoints.

Codex action:

- add Spring Security.
- parse JWT.
- validate signature and expiration.
- populate user roles.

Done when:

- missing token returns 401.
- invalid token returns 401.
- valid token allows request.

Related docs:

- `04_API_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 8.3 Implement Role-Based Authorization

[ ] Implement role access rules.

Codex action:

Enforce:

- BACKOFFICE and ADMIN can submit transactions.
- FRAUD_ANALYST and ADMIN can search audits and reports.
- ADMIN manages master data.
- unauthorized role returns 403.

Done when:

- access matrix matches API Contract.
- API tests cover forbidden access.

Related docs:

- `04_API_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 8.4 Implement Common Error Handling

[ ] Implement global exception handling.

Codex action:

- create `@ControllerAdvice`.
- map validation errors.
- map domain exceptions.
- map unauthorized/forbidden errors.
- include `requestId`.

Done when:

- error responses match API Contract.
- validation errors include field details.

Related docs:

- `04_API_CONTRACT.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 8.5 Implement Request ID Propagation

[ ] Implement request ID handling.

Codex action:

- read `X-Request-Id`.
- generate one if missing.
- include in logs and error responses.
- propagate into Kafka event envelope when publishing.

Done when:

- requestId appears in API response errors.
- event envelope contains requestId where applicable.

Related docs:

- `04_API_CONTRACT.md`
- `06_KAFKA_EVENT_CONTRACT.md`

---

## 9. Phase 7 - Transaction Service Implementation

---

### 9.1 Implement Transaction Entities and Repositories

[ ] Implement Transaction Service JPA entities.

Codex action:

Create entities/repositories for:

- Customer
- Account
- Transaction
- TransactionRiskFactor
- User
- Role.

Done when:

- mappings match Database Schema.
- repository methods support required lookups.

Related docs:

- `05_DATABASE_SCHEMA.md`

---

### 9.2 Implement Transaction Request and Response DTOs

[ ] Implement transaction DTOs.

Codex action:

Create:

- `SubmitTransactionRequest`
- `SubmitTransactionResponse`
- `TransactionDetailResponse`
- `RiskFactorResponse`.

Done when:

- request/response fields match API Contract.
- validation annotations reflect required fields.

Related docs:

- `04_API_CONTRACT.md`

---

### 9.3 Implement Source Account Validation

[ ] Implement account lookup and validation.

Codex action:

- find source account by account number.
- reject missing source account.
- reject inactive/blocked/frozen/closed account.

Done when:

- missing account returns `SOURCE_ACCOUNT_NOT_FOUND`.
- inactive account returns `SOURCE_ACCOUNT_INACTIVE`.
- unit tests cover valid and invalid account status.

Related docs:

- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 9.4 Implement Idempotency Handling

[ ] Implement idempotency logic.

Codex action:

- require `Idempotency-Key`.
- check existing transaction by idempotency key.
- return existing response if key already exists.
- create new transaction only when key is new.

Done when:

- duplicate request does not create duplicate rows.
- response is deterministic.
- unit and API tests cover duplicate submission.

Related docs:

- `04_API_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 9.5 Implement Transaction Reference Generator

[ ] Implement transaction reference generation.

Codex action:

- format:

```text
TRX-yyyyMMdd-000001
```

- keep generator testable by injecting time/sequence dependency.

Done when:

- references are unique.
- format is covered by unit tests.

Related docs:

- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 9.6 Implement Submit Transaction Use Case

[ ] Implement transaction submission flow.

Codex action:

Flow:

1. validate request,
2. validate idempotency key,
3. validate source account,
4. generate transaction ref,
5. persist transaction as `PENDING_RISK_CHECK`,
6. publish `transaction.created`,
7. record event log,
8. return response.

Done when:

- valid API call creates transaction.
- Kafka event is published after persistence.
- response matches API Contract.

Related docs:

- `01_PRD.md`
- `04_API_CONTRACT.md`
- `06_KAFKA_EVENT_CONTRACT.md`

---

### 9.7 Implement Transaction Created Event Publisher

[ ] Implement Kafka producer for `transaction.created`.

Codex action:

- build `EventEnvelope<TransactionCreatedPayload>`.
- use transactionRef as Kafka key.
- include requestId/correlationId.
- store `kafka_event_logs` as PUBLISHED.

Done when:

- payload matches Kafka Event Contract.
- event key equals transactionRef.
- unit tests cover event building.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`

---

### 9.8 Implement Transaction Detail API

[ ] Implement `GET /api/v1/transactions/{transactionRef}`.

Codex action:

- find transaction by ref.
- include masked account numbers.
- include status, risk score, decision.
- include risk factors.
- return 404 if not found.

Done when:

- response matches API Contract.
- account numbers are masked.
- role access is enforced.

Related docs:

- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`

---

## 10. Phase 8 - Risk Engine Implementation

---

### 10.1 Implement Risk Context Loader

[ ] Implement transaction context loading.

Codex action:

Load:

- transaction
- source account
- customer
- customer risk profile
- trusted device info
- known locations
- blacklist status
- rule configs
- Redis velocity counters.

Done when:

- context contains all information required by risk rules.
- missing required transaction results in controlled failure.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `07_REDIS_KEY_CONTRACT.md`

---

### 10.2 Implement RiskRule Interface

[ ] Implement common `RiskRule` interface.

Codex action:

Create:

```java
RiskFactor evaluate(TransactionContext context);
```

Done when:

- all risk rules implement same contract.
- rules can be injected as `List<RiskRule>`.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 10.3 Implement HIGH_AMOUNT Rule

[ ] Implement high amount rule.

Codex action:

- compare amount against configured threshold.
- produce risk factor with configured score when triggered.

Done when:

- threshold is configurable.
- unit tests cover below, equal, above threshold.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `08_TEST_SCENARIOS.md`

---

### 10.4 Implement NEW_DEVICE Rule

[ ] Implement new/untrusted device rule.

Codex action:

- check trusted device cache/database.
- trigger when device is not trusted or unknown.

Done when:

- trusted device does not trigger.
- untrusted/unknown device triggers.
- unit tests cover all cases.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 10.5 Implement BLACKLISTED_DESTINATION Rule

[ ] Implement blacklisted destination rule.

Codex action:

- check Redis blacklist cache first.
- fallback to PostgreSQL if cache miss.
- trigger if active blacklist exists.

Done when:

- active blacklist triggers.
- inactive blacklist does not trigger.
- Redis fallback behavior is implemented.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`

---

### 10.6 Implement HIGH_FREQUENCY_TRANSACTION Rule

[ ] Implement high frequency rule.

Codex action:

- update/read Redis velocity count key.
- update/read Redis velocity amount key.
- trigger when count or amount threshold is exceeded.

Required keys:

```text
risk:velocity:count:{sourceAccountNumber}:10m
risk:velocity:amount:{sourceAccountNumber}:10m
```

Done when:

- counter TTL is applied only on first creation.
- repeated transactions increment counters.
- rule triggers above configured threshold.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 10.7 Implement UNUSUAL_LOCATION Rule

[ ] Implement unusual location rule.

Codex action:

- compare transaction location with known customer locations.
- trigger if location is not known.

Done when:

- known location does not trigger.
- unknown location triggers.
- missing location behavior is deterministic.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `08_TEST_SCENARIOS.md`

---

### 10.8 Implement HIGH_RISK_CUSTOMER_PROFILE Rule

[ ] Implement customer risk profile rule.

Codex action:

- read customer risk profile from Redis/database.
- trigger if risk level is HIGH.

Done when:

- HIGH profile triggers.
- LOW/MEDIUM behavior matches scoring requirement.
- unit tests cover supported risk levels.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`

---

### 10.9 Implement Risk Scoring Orchestration

[ ] Implement scoring pipeline.

Codex action:

- inject all `RiskRule` beans.
- evaluate using Java Stream.
- filter triggered factors.
- sum score.
- map score to decision:
  - `< 50 APPROVED`
  - `50-79 REVIEW`
  - `>= 80 BLOCKED`.

Done when:

- all active rules are evaluated.
- score and decision are deterministic.
- unit tests cover decision boundaries 49, 50, 79, 80.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 10.10 Implement Transaction Created Consumer

[ ] Implement Kafka consumer for `transaction.created`.

Codex action:

- consume event envelope.
- validate event type/version/key.
- skip if transaction already has final decision.
- call risk scoring orchestration.
- update transaction and risk factors.
- publish `transaction.risk-scored`.
- update `kafka_event_logs`.

Done when:

- consumer is idempotent.
- successful event updates transaction status.
- event processing logs are written.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 10.11 Implement Risk Scored Event Publisher

[ ] Implement Kafka producer for `transaction.risk-scored`.

Codex action:

- build `EventEnvelope<TransactionRiskScoredPayload>`.
- use transactionRef as Kafka key.
- include risk score, decision, and risk factors.

Done when:

- payload matches Kafka Event Contract.
- event is published after risk update.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`

---

## 11. Phase 9 - Redis Implementation

---

### 11.1 Implement Redis Configuration

[ ] Configure Redis client.

Codex action:

- add Spring Data Redis.
- configure JSON serialization.
- configure connection properties.

Done when:

- Risk Engine can read/write Redis.
- Master Data can delete Redis keys.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`

---

### 11.2 Implement Redis Key Builder

[ ] Implement Redis key builder.

Codex action:

Create builder methods for:

- blacklist account
- customer risk profile
- trusted device
- risk rule config
- velocity count
- velocity amount.

Done when:

- generated keys exactly match Redis Key Contract.
- unit tests cover every key pattern.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 11.3 Implement Cache-Aside Services

[ ] Implement Redis cache-aside lookup.

Codex action:

For cacheable data:

1. read Redis,
2. if miss, query PostgreSQL,
3. write Redis with TTL,
4. return value.

Done when:

- blacklist, risk profile, trusted device, and rule config use cache-aside.
- Redis failure falls back to PostgreSQL.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`

---

### 11.4 Implement Velocity Counter Service

[ ] Implement Redis velocity counter.

Codex action:

- increment count counter.
- add amount counter.
- set TTL only when key is newly created.
- read current counter values.

Done when:

- count and amount keys use `10m` suffix.
- TTL behavior follows Redis Contract.
- unit tests cover first increment and subsequent increment behavior with mocked Redis.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`

---

### 11.5 Implement Master Data Cache Invalidation

[ ] Implement Redis invalidation from Master Data Service.

Codex action:

Invalidate keys after:

- blacklist create/update/deactivate,
- trusted device create/update,
- customer risk profile update,
- risk rule config update.

Done when:

- related keys are deleted after update.
- invalidation is covered by unit/API tests.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `04_API_CONTRACT.md`

---

## 12. Phase 10 - Master Data Service Implementation

---

### 12.1 Implement Blacklisted Account APIs

[ ] Implement blacklist CRUD APIs.

Codex action:

Implement:

- create blacklisted account
- list blacklisted accounts
- get blacklisted account by id
- update blacklisted account
- deactivate/delete blacklisted account.

Done when:

- endpoints match API Contract.
- ADMIN role is required.
- cache invalidation occurs.

Related docs:

- `04_API_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`

---

### 12.2 Implement Trusted Device APIs

[ ] Implement trusted device APIs.

Codex action:

Implement:

- create trusted device
- list trusted devices by customer
- update trusted device trust status.

Done when:

- endpoints match API Contract.
- cache invalidation occurs after changes.

Related docs:

- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`

---

### 12.3 Implement Customer Risk Profile APIs

[ ] Implement risk profile APIs.

Codex action:

Implement:

- get customer risk profile
- update customer risk profile.

Done when:

- ADMIN can update.
- FRAUD_ANALYST can read where allowed.
- cache invalidation occurs after update.

Related docs:

- `04_API_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`

---

### 12.4 Implement Risk Rule Config APIs

[ ] Implement risk rule APIs.

Codex action:

Implement:

- list risk rules
- get risk rule by code
- update risk rule config.

Done when:

- score, threshold, and active status can be managed.
- cache invalidation occurs after update.

Related docs:

- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`

---

## 13. Phase 11 - Audit Search Service Implementation

---

### 13.1 Implement Elasticsearch Configuration

[ ] Configure Elasticsearch client.

Codex action:

- connect to Elasticsearch.
- create/index mapping if needed.
- configure document ID as transactionRef.

Done when:

- service can index and search documents.
- index mapping supports filters and keyword search.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `08_TEST_SCENARIOS.md`

---

### 13.2 Implement Audit Document Model

[ ] Implement fraud audit document.

Codex action:

Create document with:

- transactionRef
- customerCif
- customerName
- masked source account
- masked destination account
- amount
- currency
- channel
- location
- riskScore
- decision
- riskFactors
- createdAt.

Done when:

- sensitive fields are masked.
- document matches System Design.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `04_API_CONTRACT.md`

---

### 13.3 Implement Risk Scored Consumer

[ ] Implement Kafka consumer for `transaction.risk-scored`.

Codex action:

- consume event.
- validate envelope.
- build audit document.
- index into Elasticsearch using transactionRef as ID.
- update event log.

Done when:

- repeated event updates same document.
- account numbers are masked.
- consumer is idempotent.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 13.4 Implement Audit Search API

[ ] Implement `GET /api/v1/audits/search`.

Codex action:

- support keyword search.
- support filters:
  - status/decision
  - minimumRiskScore
  - date range
  - page/size.
- enforce role access.

Done when:

- response matches API Contract.
- FRAUD_ANALYST and ADMIN can access.
- BACKOFFICE cannot access if not allowed.

Related docs:

- `04_API_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

## 14. Phase 12 - Native SQL Reporting

---

### 14.1 Implement Report Repository

[ ] Implement native SQL report repository.

Codex action:

- create repository using native SQL.
- do not force complex analytical reports into JPA method names.
- map result rows into DTOs.

Done when:

- all required report endpoints can query data.
- DTO mapping is deterministic.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `05_DATABASE_SCHEMA.md`

---

### 14.2 Implement High Risk Transactions Report

[ ] Implement high-risk transaction report.

Codex action:

- endpoint under `/api/v1/reports/**`.
- support risk score and date filters.
- use native SQL.

Done when:

- result contains expected high-risk transactions.
- pagination works.

Related docs:

- `04_API_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 14.3 Implement Transaction Velocity Report

[ ] Implement velocity report.

Codex action:

- aggregate transactions per source account.
- support count/amount thresholds.
- use native SQL.

Done when:

- report identifies high-frequency accounts.
- test data produces deterministic result.

Related docs:

- `05_DATABASE_SCHEMA.md`
- `08_TEST_SCENARIOS.md`

---

### 14.4 Implement Top Risk Customers Report

[ ] Implement top risk customers report.

Codex action:

- aggregate risk by customer.
- sort by average/max risk score.
- use native SQL.

Done when:

- high-risk customers are ranked correctly.

Related docs:

- `05_DATABASE_SCHEMA.md`

---

### 14.5 Implement Suspicious Destination Report

[ ] Implement suspicious destination report.

Codex action:

- group by destination account.
- count unique senders.
- sum transaction amount.
- use native SQL.

Done when:

- suspicious destination accounts are returned based on thresholds.

Related docs:

- `05_DATABASE_SCHEMA.md`

---

### 14.6 Implement Daily Fraud Trend and Risk Distribution Reports

[ ] Implement trend and distribution reports.

Codex action:

- use CTE/window functions where appropriate.
- expose report endpoints.
- return paginated/structured response.

Done when:

- reports match System Design and Test Scenarios.
- SQL is maintainable and documented.

Related docs:

- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `08_TEST_SCENARIOS.md`

---

## 15. Phase 13 - Reliability and Failure Handling

---

### 15.1 Implement Kafka Retry

[ ] Configure Kafka retry behavior.

Codex action:

- configure retry attempts.
- configure backoff.
- avoid infinite retry loops.

Done when:

- transient consumer failures retry.
- permanent failures eventually go to DLQ.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 15.2 Implement Dead Letter Queue Handling

[ ] Configure DLQ publishing.

Codex action:

- route failed `transaction.created` events to `transaction.created.dlq`.
- route failed `transaction.risk-scored` events to `transaction.risk-scored.dlq`.
- preserve original payload and error metadata.

Done when:

- DLQ topics receive failed messages.
- event log status becomes DLQ.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`

---

### 15.3 Implement Idempotent Consumer Safeguards

[ ] Add consumer idempotency checks.

Codex action:

- Risk Engine skips already-final transactions.
- Audit Search indexes by transactionRef.
- eventId is checked/logged where applicable.

Done when:

- duplicate Kafka delivery does not duplicate risk factors or audit documents.

Related docs:

- `06_KAFKA_EVENT_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 15.4 Implement Dependency Degradation Behavior

[ ] Implement degraded behavior for dependencies.

Codex action:

- Redis unavailable: fallback to PostgreSQL for reference data.
- Redis counters unavailable: continue scoring with warning.
- Elasticsearch unavailable: handle audit indexing failure via retry/DLQ.
- Kafka unavailable during publish: return appropriate error or log failed event based on flow.

Done when:

- behavior matches Test Scenarios and Demo Scenarios.
- errors are not swallowed silently.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `08_TEST_SCENARIOS.md`
- `09_DEMO_SCENARIOS.md`

---

## 16. Phase 14 - Testing Implementation

---

### 16.1 Implement Unit Test Foundation

[ ] Configure unit test standards.

Codex action:

- ensure tests run with `mvn test`.
- unit tests must not require Docker.
- configure JaCoCo.

Done when:

- unit tests run fast.
- coverage report is generated.

Related docs:

- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 16.2 Implement Common Library Unit Tests

[ ] Add unit tests for common-library.

Codex action:

Test:

- masking utility.
- event envelope builder.
- pagination response if logic exists.
- enum/constant conversion if logic exists.
- request ID utility.

Done when:

- meaningful common logic is covered.
- trivial Lombok getters are not tested.

Related docs:

- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 16.3 Implement Transaction Service Unit Tests

[ ] Add transaction service unit tests.

Codex action:

Test:

- transaction validation.
- account validation.
- idempotency logic.
- transaction reference generation.
- transaction event builder.
- response mapper.
- error mapping.

Done when:

- unit tests cover business logic and edge cases.
- no external infrastructure is used.

Related docs:

- `10_UNIT_TEST_REQUIREMENTS.md`
- `08_TEST_SCENARIOS.md`

---

### 16.4 Implement Risk Engine Unit Tests

[ ] Add risk engine unit tests.

Codex action:

Test:

- every risk rule.
- risk score aggregation.
- decision thresholds.
- rule activation behavior.
- event payload builder.
- Redis key builder behavior.

Done when:

- risk rule implementation coverage meets unit test requirements.
- boundary scores 49/50/79/80 are tested.

Related docs:

- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 16.5 Implement Audit Search Unit Tests

[ ] Add audit search unit tests.

Codex action:

Test:

- audit document builder.
- account masking usage.
- search filter builder.
- event-to-document mapper.

Done when:

- sensitive fields are never passed unmasked to document builder output.

Related docs:

- `10_UNIT_TEST_REQUIREMENTS.md`
- `04_API_CONTRACT.md`

---

### 16.6 Implement Master Data Unit Tests

[ ] Add master data unit tests.

Codex action:

Test:

- blacklist validation.
- trusted device validation.
- risk profile update logic.
- rule config update validation.
- cache invalidation key selection.

Done when:

- all cache invalidation decisions are covered.

Related docs:

- `10_UNIT_TEST_REQUIREMENTS.md`
- `07_REDIS_KEY_CONTRACT.md`

---

### 16.7 Implement Integration Tests

[ ] Add integration tests with Testcontainers.

Codex action:

Cover:

- PostgreSQL repositories/migrations.
- Kafka producer/consumer flow.
- Redis cache behavior.
- Elasticsearch indexing/search.
- Spring Security access.

Done when:

- integration tests can run in CI/local with Testcontainers.
- they do not replace unit tests.

Related docs:

- `08_TEST_SCENARIOS.md`

---

### 16.8 Implement API Contract Tests

[ ] Add API tests.

Codex action:

Cover:

- login.
- submit transaction.
- transaction detail.
- audit search.
- master data APIs.
- reporting APIs.
- error response format.
- role access.

Done when:

- API responses match API Contract.

Related docs:

- `04_API_CONTRACT.md`
- `08_TEST_SCENARIOS.md`

---

### 16.9 Implement End-to-End Tests

[ ] Add E2E test flow.

Codex action:

Validate:

```text
submit transaction
-> transaction.created
-> risk scored
-> transaction.risk-scored
-> audit indexed
-> audit searchable
```

Done when:

- complete flow passes.
- final transaction state and audit document are verified.

Related docs:

- `08_TEST_SCENARIOS.md`
- `09_DEMO_SCENARIOS.md`

---

## 17. Phase 15 - Demo Readiness

---

### 17.1 Implement Demo Health Checks

[ ] Ensure health endpoints work.

Codex action:

- verify `/actuator/health` for REST services.
- configure health indicators where useful.

Done when:

- Demo Scenarios health check commands return UP.

Related docs:

- `09_DEMO_SCENARIOS.md`

---

### 17.2 Verify Demo Users

[ ] Verify demo login users.

Codex action:

- admin/admin123
- backoffice/backoffice123
- analyst/analyst123

Done when:

- all demo users can login.
- roles match Demo Scenarios.

Related docs:

- `09_DEMO_SCENARIOS.md`

---

### 17.3 Verify Low-Risk Transaction Demo

[ ] Verify low-risk approved flow.

Codex action:

- submit trusted-device/known-location transaction below threshold.
- wait for risk engine.
- check transaction detail.
- check audit search.

Done when:

- decision is APPROVED.
- audit document is searchable.

Related docs:

- `09_DEMO_SCENARIOS.md`

---

### 17.4 Verify Review Transaction Demo

[ ] Verify review transaction flow.

Codex action:

- submit transaction that triggers enough risk score for REVIEW.
- avoid claiming HIGH_AMOUNT alone is enough unless score threshold supports it.

Done when:

- score is 50-79.
- decision is REVIEW.
- risk factors explain the score.

Related docs:

- `01_PRD.md`
- `09_DEMO_SCENARIOS.md`

---

### 17.5 Verify Blocked Transaction Demo

[ ] Verify blocked transaction flow.

Codex action:

- submit transaction to active blacklisted destination.
- combine with other risk factors if required to reach >= 80.

Done when:

- decision is BLOCKED.
- BLACKLISTED_DESTINATION factor is present.

Related docs:

- `09_DEMO_SCENARIOS.md`

---

### 17.6 Verify Redis Velocity Demo

[ ] Verify high-frequency transaction demo.

Codex action:

- submit repeated transactions from same source account.
- inspect Redis keys:
  - count counter
  - amount counter.
- verify HIGH_FREQUENCY_TRANSACTION triggers.

Done when:

- velocity counters increment.
- risk factor appears after threshold.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `09_DEMO_SCENARIOS.md`

---

### 17.7 Verify Master Data Invalidation Demo

[ ] Verify cache invalidation demo.

Codex action:

- create/update/deactivate blacklist.
- verify Redis related keys are deleted.
- submit transaction and verify fresh DB state is used.

Done when:

- cache invalidation is visible.
- behavior does not wait for TTL only.

Related docs:

- `07_REDIS_KEY_CONTRACT.md`
- `09_DEMO_SCENARIOS.md`

---

### 17.8 Verify Reporting Demo

[ ] Verify reporting endpoints.

Codex action:

Run all required reports:

- high-risk transactions
- transaction velocity
- top risk customers
- suspicious destination accounts
- daily fraud trend
- risk distribution.

Done when:

- endpoints return deterministic data.
- role access is enforced.

Related docs:

- `04_API_CONTRACT.md`
- `08_TEST_SCENARIOS.md`
- `09_DEMO_SCENARIOS.md`

---

## 18. Phase 16 - Final Codex Validation

---

### 18.1 Run Full Maven Build

[ ] Run full Maven build.

Codex action:

```bash
mvn clean verify
```

Done when:

- all modules compile.
- all unit tests pass.
- integration tests pass or are clearly separated by profile.

Related docs:

- `10_UNIT_TEST_REQUIREMENTS.md`
- `08_TEST_SCENARIOS.md`

---

### 18.2 Run Docker Compose Validation

[ ] Run full local stack.

Codex action:

```bash
docker compose up -d --build
docker compose ps
```

Done when:

- all required containers are running.
- health endpoints are UP.

Related docs:

- `09_DEMO_SCENARIOS.md`

---

### 18.3 Execute Core Demo Flow

[ ] Execute complete demo flow.

Codex action:

Run:

1. login,
2. submit transaction,
3. verify Kafka/event logs,
4. verify risk decision,
5. verify Redis keys,
6. verify Elasticsearch audit search,
7. verify reports.

Done when:

- end-to-end flow matches Demo Scenarios.

Related docs:

- `09_DEMO_SCENARIOS.md`

---

### 18.4 Verify No Contract Drift

[ ] Compare implementation with contracts.

Codex action:

Check that implementation has not drifted from:

- API endpoints.
- error response format.
- DB schema.
- Kafka topics and envelope.
- Redis keys.
- test expectations.
- demo commands.

Done when:

- no public contract mismatch remains.
- any intentional deviation is documented.

Related docs:

- All documents.

---

### 18.5 Update README

[ ] Update final README.

Codex action:

README must include:

- project summary.
- architecture summary.
- required tools.
- how to run.
- how to test.
- demo command order.
- docs index.

Done when:

- a new developer can run the project from README alone.

Related docs:

- `09_DEMO_SCENARIOS.md`
- All docs.

---

### 18.6 Final Completion Checklist

[ ] Complete final checklist.

Codex action:

Confirm:

- [ ] Repository structure matches TRD.
- [ ] All services compile.
- [ ] Docker Compose starts required infrastructure.
- [ ] Database migrations run successfully.
- [ ] Seed data exists.
- [ ] Login works.
- [ ] Transaction submission works.
- [ ] Idempotency works.
- [ ] `transaction.created` is published.
- [ ] Risk Engine consumes event.
- [ ] Redis cache/counters are used.
- [ ] Risk score and decision are stored.
- [ ] `transaction.risk-scored` is published.
- [ ] Audit Search consumes event.
- [ ] Elasticsearch document is indexed.
- [ ] Audit search works.
- [ ] Reporting APIs work.
- [ ] Master Data APIs work.
- [ ] Cache invalidation works.
- [ ] Kafka retry/DLQ behavior works.
- [ ] Unit tests pass.
- [ ] Integration/API/E2E tests pass.
- [ ] Demo scenarios can be executed.
- [ ] README is complete.

Done when:

- every item is checked or documented as intentionally deferred.

Related docs:

- All docs.

---

## 19. Codex Execution Notes

Codex should add notes below during implementation.

### 19.1 Current Project State

```text
Phase 0 scan completed.
Phase 1 repository and build foundation completed.

Repository state:
- Existing project has been converted from a single Spring Boot application into a Maven multi-module project.
- Root files/folders include: .github, .mvn, docs, common-library, transaction-service, risk-engine-service, audit-search-service, master-data-service, target, .editorconfig, .env.example, .gitattributes, .gitignore, HELP.md, README.md, mvnw, mvnw.cmd, pom.xml.
- Application package root currently exists as com.bankguard.transaction.
- Main application class exists at transaction-service/src/main/java/com/bankguard/transaction/TransactionServiceApplication.java.
- Current test class exists at transaction-service/src/test/java/com/bankguard/transaction/TransactionServiceApplicationTests.java.
- Root pom.xml is now a parent POM with packaging pom.
- Registered modules: common-library, transaction-service, risk-engine-service, audit-search-service, master-data-service.

Documentation:
- Required source documents 01_PRD.md through 11_TODO_LIST.md exist in docs/.
- CODEX_TODO_LIST.md also exists as the Codex execution checklist.
```

---

### 19.2 Missing Files or Mismatches

```text
Phase 0 findings:

Documentation:
- No required source document is missing.
- CODEX_TODO_LIST.md is an additional execution file and is not part of the required 01-11 document set.

Maven/build:
- pom.xml exists and uses Spring Boot 4.0.6.
- pom.xml sets java.version to 21.
- Maven installed in shell: Apache Maven 3.9.13.
- Shell Java runtime for Maven is now Java 21.0.10.
- JAVA_HOME has been set to C:\Program Files\Java\jdk-21.0.10 for the user environment.
- mvn -q clean validate succeeds.
- mvn -q test succeeds.
- Root pom.xml is now a parent POM with centralized Java 21, Spring Boot 4.0.6, Springdoc OpenAPI 3.0.3, Testcontainers 2.0.5, and JaCoCo configuration.

Modules:
- Required module directories and module POMs now exist.
- Existing Spring Boot scaffold was moved into transaction-service.

Docker Compose:
- docker-compose.yml/docker-compose.yaml was not found.
- Required local services are not configured yet: PostgreSQL, Kafka, Redis, Elasticsearch, transaction-service, risk-engine-service, audit-search-service, master-data-service.

Tests:
- Current test baseline contains one lightweight transaction-service unit-style smoke test.
- No full unit/integration test package separation is present yet.
- Testcontainers dependencies are now available through module test dependencies and the root Testcontainers BOM.
```

---

### 19.3 Deferred Items

```text
Phase 0 was scan-only.
Phase 1 completed repository/build foundation only.
Phase 2 and later tasks are intentionally deferred until requested.
```

---

### 19.4 Known Technical Risks

```text
Known after Phase 1:
- Java 21 is now active for Maven and mvn test succeeds.
- Multi-module layout exists, but risk-engine-service, audit-search-service, master-data-service, and common-library do not yet contain application source code.
- Docker Compose is still absent; local end-to-end validation remains unavailable until Phase 3.
- Full unit/integration test structure still needs to be expanded in later phases.
```

---

### 19.5 Final Result Summary

```text
To be filled by Codex after completing the checklist.
```
