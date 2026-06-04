# Todo List
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the implementation todo list for **BankGuard**.

The todo list translates all existing project documents into executable engineering tasks. It is intended to guide implementation from repository setup until the project is ready for local demo and final validation.

This document is derived from and aligned with:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`
- `06_KAFKA_EVENT_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`
- `08_TEST_SCENARIOS.md`
- `09_DEMO_SCENARIOS.md`
- `10_UNIT_TEST_REQUIREMENTS.md`

---

### 1.2 Implementation Principle

Each todo item must be small enough to be completed in a focused development session.

Recommended task size:

```text
1 - 2 hours per task
```

A task is too large if it combines:

- API implementation
- database changes
- Kafka logic
- Redis behavior
- test implementation

Those must be separated.

---

### 1.3 Task Format

Each task follows this structure:

```text
[ ] Task title
    Goal:
    Output:
    Done when:
    Depends on:
    Related docs:
```

---

### 1.4 Completion Rule

A task is not considered complete only because the code compiles.

A task is complete only when:

1. The implementation matches the related contract document.
2. Unit tests are added for unit-testable logic.
3. Integration/API/event tests are added where required.
4. Logs and error handling are consistent.
5. The behavior can be demonstrated or verified.

---

## 2. Phase 0 - Repository and Development Foundation

---

### 2.1 Create Project Repository Structure

[ ] Create root repository structure.

Goal:  
Prepare a clean multi-module repository for all BankGuard services.

Output:

```text
bankguard/
├── docs/
├── common-library/
├── transaction-service/
├── risk-engine-service/
├── audit-search-service/
├── master-data-service/
├── docker-compose.yml
└── README.md
```

Done when:

- All directories exist.
- The root project can be opened in IDE.
- The structure matches TRD repository requirements.

Depends on:

- None.

Related docs:

- TRD Technical Requirements
- System Design Architecture

---

### 2.2 Create Root Maven Parent Project

[ ] Create root `pom.xml`.

Goal:  
Centralize dependency versions and module registration.

Output:

- Parent Maven POM.
- Java version set to `21`.
- Spring Boot version set consistently.
- All service modules registered.

Done when:

- `mvn clean validate` runs from root.
- All modules are detected by Maven.

Depends on:

- 2.1

Related docs:

- TRD Technical Stack Requirements
- Unit Test Requirements

---

### 2.3 Configure Shared Dependency Management

[ ] Configure dependency management for common libraries.

Goal:  
Ensure all services use consistent dependency versions.

Output:

- Spring Boot dependency management.
- JUnit 5 dependency.
- Mockito dependency.
- AssertJ dependency.
- Spring Kafka version.
- Spring Data Redis version.
- Spring Data Elasticsearch version.
- PostgreSQL driver.
- Flyway.
- Springdoc OpenAPI.

Done when:

- Dependency versions are not duplicated unnecessarily across service modules.
- Each service can import only the dependencies it needs.

Depends on:

- 2.2

Related docs:

- TRD Technical Stack Requirements
- Unit Test Requirements

---

### 2.4 Add Project-Wide Code Formatting Rules

[ ] Add formatter or editor configuration.

Goal:  
Keep code style consistent across services.

Output:

- `.editorconfig`
- optional Checkstyle/Spotless configuration

Done when:

- Common indentation and line-ending rules are defined.
- Format command or IDE format produces consistent output.

Depends on:

- 2.1

Related docs:

- TRD Package Convention

---

### 2.5 Create Documentation Folder Structure

[ ] Create final docs folder and place all MD documents.

Goal:  
Maintain documentation in final project order.

Output:

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

- Documents are named consistently.
- README references the docs folder.

Depends on:

- 2.1

Related docs:

- All documents

---

## 3. Phase 1 - Common Library Foundation

---

### 3.1 Create Common Library Module

[ ] Create `common-library` Maven module.

Goal:  
Provide shared contracts and utilities used across services.

Output:

- `common-library/pom.xml`
- package root `com.bankguard.common`

Done when:

- Other service modules can depend on `common-library`.
- No service-specific business logic is inside this module.

Depends on:

- 2.2

Related docs:

- TRD Common Library
- Kafka Event Contract
- API Contract

---

### 3.2 Implement Common Event Envelope

[ ] Create `EventEnvelope<T>`.

Goal:  
Standardize Kafka event envelope across producers and consumers.

Output:

- `EventEnvelope<T>`
- fields:
  - `eventId`
  - `eventType`
  - `eventVersion`
  - `aggregateType`
  - `aggregateId`
  - `occurredAt`
  - `producer`
  - `requestId`
  - `correlationId`
  - `payload`

Done when:

- Envelope matches Kafka Event Contract.
- Unit tests validate required field construction.

Depends on:

- 3.1

Related docs:

- Kafka Event Contract
- TRD Technical Requirements
- Unit Test Requirements

---

### 3.3 Implement Common API Error Response

[ ] Create common error response DTO.

Goal:  
Ensure all services return consistent error responses.

Output:

- `CommonErrorResponse`
- `ErrorDetail`
- error code constants

Done when:

- Fields match API Contract:
  - timestamp
  - status
  - error
  - message
  - details
  - path
  - requestId

Depends on:

- 3.1

Related docs:

- API Contract
- TRD Common API Requirements

---

### 3.4 Implement Common Pagination Response

[ ] Create `PageResponse<T>`.

Goal:  
Standardize list/search/report response format.

Output:

- `data`
- `page`
- `size`
- `total`

Done when:

- Format matches API Contract.
- Unit tests verify object creation and mapping.

Depends on:

- 3.1

Related docs:

- API Contract

---

### 3.5 Implement Account Number Masking Utility

[ ] Create `AccountMaskingUtil`.

Goal:  
Centralize sensitive account masking rules.

Output:

- method to mask account number with first 3 and last 3 characters.
- fallback rule for short account numbers.

Done when:

- `1234567890` becomes `123****890`.
- short account numbers are masked according to API Contract.
- Unit tests cover normal, short, null, and blank cases.

Depends on:

- 3.1

Related docs:

- API Contract
- TRD Common API Requirements
- Unit Test Requirements

---

### 3.6 Implement Correlation ID Support

[ ] Create correlation ID constants and helper.

Goal:  
Standardize request tracing fields.

Output:

- `X-Request-Id` constant.
- `correlationId` constant.
- helper to generate fallback request ID.

Done when:

- Services can reuse constants.
- Unit tests validate fallback generation behavior with mocked generator where applicable.

Depends on:

- 3.1

Related docs:

- API Contract
- Kafka Event Contract
- System Design Observability

---

### 3.7 Implement Common Domain Constants

[ ] Create shared constants/enums.

Goal:  
Avoid inconsistent status, role, event, and decision values.

Output:

- transaction status constants
- risk decision constants
- role constants
- Kafka topic constants
- producer name constants
- supported channel constants

Done when:

- Constants match API Contract, Database Schema, and Kafka Contract.
- Unit tests validate enum conversion if conversion logic exists.

Depends on:

- 3.1

Related docs:

- API Contract
- Database Schema
- Kafka Event Contract

---

## 4. Phase 2 - Local Infrastructure and Containerization

---

### 4.1 Create Docker Compose Base File

[ ] Create `docker-compose.yml`.

Goal:  
Run all infrastructure and services locally.

Output:

- PostgreSQL
- Kafka
- Redis
- Elasticsearch
- Transaction Service
- Risk Engine Service
- Audit Search Service
- Master Data Service

Done when:

- `docker compose config` passes.
- Service names match Demo Scenarios.

Depends on:

- 2.1

Related docs:

- System Design Architecture
- Demo Scenarios
- TRD Local Deployment

---

### 4.2 Configure PostgreSQL Container

[ ] Add PostgreSQL service to Docker Compose.

Goal:  
Provide local source-of-truth database.

Output:

- PostgreSQL 16 image.
- database name `bankguard`.
- username and password.
- port `5432`.

Done when:

- container starts.
- application can connect using environment variables.
- database persists through named volume.

Depends on:

- 4.1

Related docs:

- Database Schema
- Demo Scenarios

---

### 4.3 Configure Kafka Container

[ ] Add Kafka service to Docker Compose.

Goal:  
Provide event streaming backbone.

Output:

- Kafka broker.
- exposed port `9092`.
- topics can be created by application or startup script.

Done when:

- Kafka is reachable from services.
- topics can be produced and consumed locally.

Depends on:

- 4.1

Related docs:

- Kafka Event Contract
- System Design Architecture

---

### 4.4 Configure Redis Container

[ ] Add Redis service to Docker Compose.

Goal:  
Provide cache and short-lived velocity counters.

Output:

- Redis 7 image.
- exposed port `6379`.

Done when:

- Redis is reachable from Risk Engine and Master Data Service.
- `redis-cli ping` returns `PONG`.

Depends on:

- 4.1

Related docs:

- Redis Key Contract
- Demo Scenarios

---

### 4.5 Configure Elasticsearch Container

[ ] Add Elasticsearch service to Docker Compose.

Goal:  
Provide audit search read model.

Output:

- Elasticsearch container.
- exposed port `9200`.
- local single-node mode.

Done when:

- `GET /_cluster/health` is available.
- Audit Search Service can connect.

Depends on:

- 4.1

Related docs:

- System Design Architecture
- Demo Scenarios

---

### 4.6 Add Service Environment Variables

[ ] Define environment variables for all services.

Goal:  
Avoid hardcoded infrastructure addresses.

Output:

- DB URL
- Kafka bootstrap servers
- Redis host and port
- Elasticsearch URL
- JWT secret
- service ports

Done when:

- all services can read config from environment.
- local defaults are documented in README.

Depends on:

- 4.1

Related docs:

- TRD Technical Requirements
- Demo Scenarios

---

## 5. Phase 3 - Database Migration and Seed Data

---

### 5.1 Configure Flyway

[ ] Add Flyway configuration.

Goal:  
Manage PostgreSQL schema migration consistently.

Output:

- Flyway dependency.
- migration folder.
- baseline migration naming.

Done when:

- migrations run automatically on service startup.
- `flyway_schema_history` is created.

Depends on:

- 4.2

Related docs:

- Database Schema
- TRD Technical Requirements

---

### 5.2 Create Customers Table Migration

[ ] Add migration for `customers`.

Goal:  
Store customer identity and status.

Output:

- `customers` table.
- constraints and indexes.

Done when:

- table matches Database Schema.
- unique constraint on `cif_number` exists.
- check constraint on status exists.

Depends on:

- 5.1

Related docs:

- Database Schema

---

### 5.3 Create Customer Risk Profiles Table Migration

[ ] Add migration for `customer_risk_profiles`.

Goal:  
Store customer risk metadata.

Output:

- table with one profile per customer.
- risk level check constraint.

Done when:

- foreign key to `customers` exists.
- `LOW`, `MEDIUM`, `HIGH` are enforced.

Depends on:

- 5.2

Related docs:

- Database Schema
- Redis Key Contract

---

### 5.4 Create Accounts Table Migration

[ ] Add migration for `accounts`.

Goal:  
Store customer account records for transaction submission.

Output:

- account number unique constraint.
- account status check constraint.
- balance non-negative constraint.

Done when:

- source account validation can query this table.
- account statuses match API and Database Schema.

Depends on:

- 5.2

Related docs:

- Database Schema
- API Contract

---

### 5.5 Create Transactions Table Migration

[ ] Add migration for `transactions`.

Goal:  
Store submitted transactions and risk result summary.

Output:

- transaction reference unique constraint.
- idempotency key unique constraint.
- amount positive check constraint.
- transaction status and decision constraints.
- reporting indexes.

Done when:

- transaction creation can persist `PENDING_RISK_CHECK`.
- duplicate idempotency key is impossible at DB level.
- indexes support reporting queries.

Depends on:

- 5.4

Related docs:

- Database Schema
- API Contract
- Kafka Event Contract

---

### 5.6 Create Transaction Risk Factors Table Migration

[ ] Add migration for `transaction_risk_factors`.

Goal:  
Store explainable risk factors.

Output:

- risk factor table.
- foreign key to `transactions`.
- factor code index.
- metadata JSONB column.

Done when:

- risk factors can be stored per transaction.
- transaction detail can retrieve risk factors.

Depends on:

- 5.5

Related docs:

- Database Schema
- PRD Risk Scoring Engine

---

### 5.7 Create Blacklisted Accounts Table Migration

[ ] Add migration for `blacklisted_accounts`.

Goal:  
Support blacklisted destination rule.

Output:

- unique account number.
- active flag.
- reason field.

Done when:

- Master Data Service can create/update/deactivate blacklist records.
- Risk Engine can query active blacklist records.

Depends on:

- 5.1

Related docs:

- Database Schema
- Redis Key Contract

---

### 5.8 Create Customer Devices Table Migration

[ ] Add migration for `customer_devices`.

Goal:  
Support trusted device and new device rules.

Output:

- unique `(customer_id, device_id)`.
- trusted flag.
- timestamps.

Done when:

- trusted device lookup can be performed.
- Master Data Service can manage trusted devices.

Depends on:

- 5.2

Related docs:

- Database Schema
- Redis Key Contract

---

### 5.9 Create Customer Locations Table Migration

[ ] Add migration for `customer_locations`.

Goal:  
Support unusual location rule.

Output:

- unique `(customer_id, location)`.
- usage count.
- last used timestamp.

Done when:

- Risk Engine can determine known or unusual location.
- table supports future usage count updates.

Depends on:

- 5.2

Related docs:

- Database Schema
- Risk Scoring requirements

---

### 5.10 Create Risk Rule Configs Table Migration

[ ] Add migration for `risk_rule_configs`.

Goal:  
Store configurable rule score, threshold, and active status.

Output:

- rule code unique constraint.
- score field.
- threshold value field.
- active flag.

Done when:

- Risk Engine can load rule configuration.
- Master Data Service can retrieve and update rule configuration.

Depends on:

- 5.1

Related docs:

- Database Schema
- Redis Key Contract
- TRD Risk Engine Requirements

---

### 5.11 Create Kafka Event Logs Table Migration

[ ] Add migration for `kafka_event_logs`.

Goal:  
Store event publish and consume trace.

Output:

- event id unique constraint.
- event type.
- aggregate id.
- topic name.
- payload JSONB.
- status.
- error message.

Done when:

- producers and consumers can insert/update event logs.
- allowed statuses match Database Schema.

Depends on:

- 5.1

Related docs:

- Database Schema
- Kafka Event Contract
- Demo Scenarios

---

### 5.12 Create Users, Roles, and User Roles Migration

[ ] Add authentication tables.

Goal:  
Support MVP JWT login and authorization.

Output:

- `users`
- `roles`
- `user_roles`

Done when:

- admin, backoffice, and analyst users can be seeded.
- roles match API Contract authorization matrix.

Depends on:

- 5.1

Related docs:

- API Contract
- Database Schema

---

### 5.13 Create Seed Data Migration

[ ] Add seed data migration.

Goal:  
Provide deterministic data for tests and demo.

Output:

- demo customers.
- demo accounts.
- blacklisted accounts.
- trusted devices.
- known locations.
- risk rule config.
- users and roles.

Done when:

- seed data matches Test Scenarios and Demo Scenarios.
- demo can run without manual database inserts.

Depends on:

- 5.2 through 5.12

Related docs:

- Test Scenarios
- Demo Scenarios
- Database Schema

---

## 6. Phase 4 - Service Boilerplate

---

### 6.1 Create Transaction Service Module

[ ] Create Transaction Service Spring Boot module.

Goal:  
Provide service for authentication, transaction APIs, and reporting APIs.

Output:

- Spring Boot main class.
- package structure.
- application configuration.
- dependencies.

Done when:

- service starts locally.
- actuator health endpoint returns `UP`.

Depends on:

- 2.2
- 4.2
- 5.1

Related docs:

- System Design Architecture
- API Contract
- TRD

---

### 6.2 Create Risk Engine Service Module

[ ] Create Risk Engine Service Spring Boot module.

Goal:  
Provide Kafka consumer and risk scoring workflow.

Output:

- Spring Boot main class.
- package structure.
- Kafka and Redis config.
- database config.

Done when:

- service starts locally.
- health endpoint or logs confirm startup.
- service does not expose public business API.

Depends on:

- 2.2
- 4.2
- 4.3
- 4.4

Related docs:

- System Design Architecture
- Kafka Event Contract
- Redis Key Contract

---

### 6.3 Create Audit Search Service Module

[ ] Create Audit Search Service Spring Boot module.

Goal:  
Provide Kafka consumer and audit search API.

Output:

- Spring Boot main class.
- Elasticsearch config.
- Kafka consumer config.
- search API package.

Done when:

- service starts locally.
- actuator health endpoint returns `UP`.
- Elasticsearch connectivity is configured.

Depends on:

- 2.2
- 4.3
- 4.5

Related docs:

- System Design Architecture
- Kafka Event Contract
- API Contract

---

### 6.4 Create Master Data Service Module

[ ] Create Master Data Service Spring Boot module.

Goal:  
Provide APIs for blacklist, trusted devices, customer risk profile, and rule configuration.

Output:

- Spring Boot main class.
- package structure.
- PostgreSQL config.
- Redis config.

Done when:

- service starts locally.
- actuator health endpoint returns `UP`.

Depends on:

- 2.2
- 4.2
- 4.4

Related docs:

- API Contract
- Redis Key Contract
- Database Schema

---

### 6.5 Add OpenAPI to API Services

[ ] Configure Springdoc OpenAPI.

Goal:  
Allow API exploration using Swagger UI.

Output:

- Swagger UI available for Transaction Service.
- Swagger UI available for Audit Search Service.
- Swagger UI available for Master Data Service.

Done when:

- each API service exposes OpenAPI documentation.
- documented endpoints match API Contract.

Depends on:

- 6.1
- 6.3
- 6.4

Related docs:

- API Contract
- Demo Scenarios

---

## 7. Phase 5 - Security Foundation

---

### 7.1 Implement JWT Configuration

[ ] Implement JWT generation and validation support.

Goal:  
Secure protected APIs.

Output:

- JWT utility.
- token generation.
- token validation.
- expiration handling.

Done when:

- login can issue token.
- protected endpoints reject invalid or missing token.
- unit tests cover token helper where practical.

Depends on:

- 6.1
- 5.12

Related docs:

- API Contract
- TRD Security Requirements

---

### 7.2 Implement Authentication Endpoint

[ ] Implement `POST /api/v1/auth/login`.

Goal:  
Allow demo users to obtain JWT tokens.

Output:

- login request DTO.
- login response DTO.
- credential validation.
- role retrieval.

Done when:

- admin, backoffice, and analyst can login.
- invalid credentials return `UNAUTHORIZED`.
- response matches API Contract.

Depends on:

- 7.1
- 5.13

Related docs:

- API Contract
- Demo Scenarios
- Test Scenarios

---

### 7.3 Implement Role-Based Authorization

[ ] Configure endpoint authorization rules.

Goal:  
Restrict APIs by role.

Output:

- security configuration.
- role rules for transaction, reports, audit search, and master data.

Done when:

- analyst cannot submit transaction.
- backoffice can submit transaction.
- admin can manage master data.
- fraud analyst can access reports and audit search.

Depends on:

- 7.1
- 7.2

Related docs:

- API Contract Authorization Matrix
- Test Scenarios

---

### 7.4 Implement Common Security Error Handling

[ ] Return consistent security error responses.

Goal:  
Ensure unauthorized and forbidden errors match API Contract.

Output:

- authentication entry point.
- access denied handler.

Done when:

- missing token returns `401 UNAUTHORIZED`.
- insufficient role returns `403 FORBIDDEN`.
- response uses common error format.

Depends on:

- 7.3
- 3.3

Related docs:

- API Contract
- Unit Test Requirements

---

## 8. Phase 6 - Transaction Service Implementation

---

### 8.1 Create Transaction Request and Response DTOs

[ ] Create transaction submission DTOs.

Goal:  
Represent request and response payloads.

Output:

- `SubmitTransactionRequest`
- `SubmitTransactionResponse`
- validation annotations.

Done when:

- fields match API Contract.
- validation annotations cover required fields.
- unit tests cover custom validation if any.

Depends on:

- 6.1

Related docs:

- API Contract
- TRD Transaction Service Requirements

---

### 8.2 Create Transaction Entity and Repository

[ ] Implement Transaction JPA entity and repository.

Goal:  
Persist transaction data.

Output:

- `Transaction` entity.
- `TransactionRepository`.
- lookup by transaction reference.
- lookup by idempotency key.

Done when:

- entity maps to `transactions` table.
- repository methods support submission and idempotency.

Depends on:

- 5.5
- 6.1

Related docs:

- Database Schema

---

### 8.3 Create Account Entity and Repository

[ ] Implement Account entity and repository.

Goal:  
Support source account validation.

Output:

- `Account` entity.
- `Customer` relationship if required.
- `AccountRepository.findByAccountNumber`.

Done when:

- source account can be loaded with customer data.
- account status can be checked.

Depends on:

- 5.2
- 5.4
- 6.1

Related docs:

- Database Schema
- API Contract

---

### 8.4 Implement Transaction Validation Service

[ ] Create `TransactionValidationService`.

Goal:  
Validate business rules before transaction creation.

Output:

- source account existence validation.
- account active status validation.
- amount and channel validation if not fully handled by DTO.

Done when:

- missing source account returns `SOURCE_ACCOUNT_NOT_FOUND`.
- inactive source account returns `SOURCE_ACCOUNT_INACTIVE`.
- invalid channel returns `INVALID_CHANNEL`.
- unit tests cover success and failure cases.

Depends on:

- 8.1
- 8.3

Related docs:

- API Contract
- Unit Test Requirements
- Test Scenarios

---

### 8.5 Implement Idempotency Service

[ ] Create `IdempotencyService`.

Goal:  
Prevent duplicate transaction creation.

Output:

- lookup by idempotency key.
- return existing transaction response if found.
- allow new transaction if key is absent.

Done when:

- same idempotency key never creates a second transaction.
- duplicate request returns existing transaction response.
- unit tests cover existing and new key behavior.

Depends on:

- 8.2

Related docs:

- API Contract
- TRD Idempotency
- Test Scenarios
- Unit Test Requirements

---

### 8.6 Implement Transaction Reference Generator

[ ] Create transaction reference generator.

Goal:  
Generate unique transaction references.

Output:

- format `TRX-yyyyMMdd-000001`.
- sequence provider or repository-backed sequence.

Done when:

- generated reference matches format.
- date is deterministic in unit tests using fixed clock.
- unit tests cover formatting.

Depends on:

- 8.2

Related docs:

- API Contract
- TRD Transaction Service Requirements

---

### 8.7 Implement Transaction Created Event Payload

[ ] Create `TransactionCreatedPayload`.

Goal:  
Represent `transaction.created` payload.

Output:

- payload DTO matching Kafka Event Contract.

Done when:

- fields match Kafka Contract.
- account numbers in event remain raw only for internal scoring context.
- unit tests validate mapping from transaction domain to payload.

Depends on:

- 3.2
- 8.2

Related docs:

- Kafka Event Contract

---

### 8.8 Implement Transaction Event Publisher

[ ] Create `TransactionEventPublisher`.

Goal:  
Publish `transaction.created` to Kafka.

Output:

- KafkaTemplate publisher.
- event envelope builder.
- topic constant usage.
- message key as `transactionRef`.

Done when:

- event envelope matches Kafka Contract.
- key equals transaction reference.
- event log can be written after publish.
- unit tests cover envelope construction with mocked UUID/time.

Depends on:

- 3.2
- 3.7
- 7.7

Related docs:

- Kafka Event Contract
- Unit Test Requirements

---

### 8.9 Implement Kafka Event Log Writer

[ ] Create event log writer in Transaction Service.

Goal:  
Record published event trace.

Output:

- `KafkaEventLog` entity.
- repository.
- method to save `PUBLISHED` event.

Done when:

- event log is inserted after successful publish.
- payload is stored as JSONB.
- failure can be recorded where applicable.

Depends on:

- 5.11
- 8.8

Related docs:

- Database Schema
- Kafka Event Contract

---

### 8.10 Implement Transaction Application Service

[ ] Create transaction creation orchestration.

Goal:  
Implement full transaction submission behavior.

Output flow:

```text
validate token/role
validate idempotency key
check existing transaction
validate request
validate account
generate transactionRef
save transaction
publish transaction.created
save event log
return response
```

Done when:

- valid request returns `201 Created`.
- duplicate idempotency returns `200 OK`.
- invalid request returns contract error.
- unit tests cover orchestration branches.

Depends on:

- 8.1 through 8.9

Related docs:

- PRD MVP-01
- API Contract
- Kafka Event Contract
- Unit Test Requirements

---

### 8.11 Implement Transaction Controller

[ ] Create `TransactionController`.

Goal:  
Expose transaction submission API.

Output:

- `POST /api/v1/transactions`.

Done when:

- endpoint matches API Contract.
- headers are validated.
- response status codes match contract.
- API tests cover endpoint behavior.

Depends on:

- 8.10
- 7.3

Related docs:

- API Contract
- Test Scenarios

---

### 8.12 Implement Transaction Detail Query

[ ] Implement transaction detail retrieval.

Goal:  
Allow users to view transaction status, risk decision, and factors.

Output:

- transaction detail response DTO.
- query by transaction reference.
- risk factor mapping.
- masked account numbers.

Done when:

- response matches API Contract.
- not found returns `TRANSACTION_NOT_FOUND`.
- account numbers are masked.
- unit tests cover mapper and masking behavior.

Depends on:

- 8.2
- 5.6
- 3.5

Related docs:

- API Contract
- Database Schema
- Test Scenarios

---

### 8.13 Expose Transaction Detail Endpoint

[ ] Add `GET /api/v1/transactions/{transactionRef}`.

Goal:  
Expose investigation detail API.

Output:

- controller method.
- authorization rules.
- response mapping.

Done when:

- admin, backoffice, and fraud analyst can access.
- missing transaction returns 404.
- response includes risk factors after scoring.

Depends on:

- 8.12
- 7.3

Related docs:

- API Contract
- Demo Scenarios

---

## 9. Phase 7 - Risk Engine Implementation

---

### 9.1 Create Transaction Created Kafka Consumer

[ ] Implement `TransactionCreatedConsumer`.

Goal:  
Consume `transaction.created`.

Output:

- listener for topic `transaction.created`.
- message key validation.
- envelope validation.
- payload validation.

Done when:

- consumer receives valid event.
- invalid envelope is handled consistently.
- unit tests cover validation logic.
- integration test verifies real Kafka consumption.

Depends on:

- 6.2
- 3.2
- 4.3

Related docs:

- Kafka Event Contract
- Test Scenarios

---

### 9.2 Implement Event Idempotency Check

[ ] Add consumer idempotency logic.

Goal:  
Avoid duplicate re-scoring.

Output:

- check transaction status before scoring.
- skip if transaction already has final decision.
- record ignored event where applicable.

Done when:

- duplicate event does not duplicate risk factors.
- duplicate event does not republish risk-scored.
- unit tests cover pending and final transaction states.

Depends on:

- 9.1
- 8.2

Related docs:

- Kafka Event Contract
- TRD Idempotency
- Unit Test Requirements

---

### 9.3 Create Transaction Context Builder

[ ] Implement `TransactionContextBuilder`.

Goal:  
Build all data required by risk rules.

Output:

- load transaction.
- load customer.
- load account.
- load customer risk profile.
- load blacklist status.
- load trusted device status.
- load known location status.
- load velocity counters.
- load rule configs.

Done when:

- context contains all fields required by risk rules.
- missing optional fields are handled safely.
- unit tests cover context assembly using mocked dependencies.

Depends on:

- 9.1
- Redis services in Phase 10
- database repositories

Related docs:

- TRD Risk Engine Requirements
- Redis Key Contract
- Database Schema

---

### 9.4 Create RiskRule Interface and RiskFactor Model

[ ] Define risk rule contract.

Goal:  
Standardize risk rule implementation.

Output:

- `RiskRule` interface.
- `RiskFactor` model.
- `TransactionContext` model.

Done when:

- interface supports code, active status, and evaluate method.
- model fields match TRD.
- unit tests cover model behavior if behavior exists.

Depends on:

- 6.2

Related docs:

- System Design Architecture
- TRD Risk Engine Requirements

---

### 9.5 Implement High Amount Rule

[ ] Create `HighAmountRule`.

Goal:  
Trigger when amount exceeds configured threshold.

Output:

- rule code `HIGH_AMOUNT`.
- uses threshold from rule config.
- returns configured score.

Done when:

- amount below threshold does not trigger.
- amount equal boundary is handled according to requirement.
- amount above threshold triggers.
- unit tests cover all boundaries.

Depends on:

- 9.4
- risk rule config loading

Related docs:

- PRD MVP-02
- Unit Test Requirements
- Test Scenarios

---

### 9.6 Implement New Device Rule

[ ] Create `NewDeviceRule`.

Goal:  
Trigger when device is not trusted for customer.

Output:

- rule code `NEW_DEVICE`.

Done when:

- trusted device does not trigger.
- unknown/untrusted device triggers.
- missing device is handled explicitly.
- unit tests cover all cases.

Depends on:

- 9.4

Related docs:

- PRD MVP-02
- Redis Key Contract

---

### 9.7 Implement Blacklisted Destination Rule

[ ] Create `BlacklistedDestinationRule`.

Goal:  
Trigger when destination account is blacklisted.

Output:

- rule code `BLACKLISTED_DESTINATION`.

Done when:

- active blacklist triggers.
- inactive/nonexistent blacklist does not trigger.
- unit tests cover cache-derived context values.

Depends on:

- 9.4

Related docs:

- PRD MVP-02
- Redis Key Contract
- Database Schema

---

### 9.8 Implement High Frequency Transaction Rule

[ ] Create `HighFrequencyTransactionRule`.

Goal:  
Trigger when Redis velocity count or amount exceeds configured threshold.

Output:

- count threshold logic.
- amount threshold logic.
- rule code `HIGH_FREQUENCY_TRANSACTION`.

Done when:

- below count and amount threshold does not trigger.
- count threshold triggers.
- amount threshold triggers.
- both thresholds trigger only one risk factor unless otherwise designed.
- unit tests cover boundaries.

Depends on:

- 9.4
- Redis velocity counter service

Related docs:

- Redis Key Contract
- PRD MVP-04
- Unit Test Requirements

---

### 9.9 Implement Unusual Location Rule

[ ] Create `UnusualLocationRule`.

Goal:  
Trigger when location is not known for customer.

Output:

- rule code `UNUSUAL_LOCATION`.

Done when:

- known location does not trigger.
- unknown location triggers.
- missing location is handled explicitly.
- unit tests cover all cases.

Depends on:

- 9.4

Related docs:

- PRD MVP-02
- Database Schema

---

### 9.10 Implement High Risk Customer Profile Rule

[ ] Create `HighRiskCustomerProfileRule`.

Goal:  
Trigger when customer risk profile is elevated.

Output:

- rule code `HIGH_RISK_CUSTOMER_PROFILE`.

Done when:

- LOW does not trigger.
- MEDIUM behavior is defined and tested.
- HIGH triggers.
- unit tests cover risk levels.

Depends on:

- 9.4

Related docs:

- PRD MVP-02
- Database Schema

---

### 9.11 Implement Risk Scoring Application Service

[ ] Create `RiskScoringApplicationService`.

Goal:  
Run all active rules and calculate total score.

Output:

- Java Stream rule pipeline.
- sum triggered factor scores.
- cap score if required or allow 100+ according to design.
- decision mapping.

Done when:

- all rules execute.
- only triggered factors are stored.
- total score is correct.
- unit tests cover single rule, multiple rules, no rules, inactive rules.

Depends on:

- 9.4 through 9.10

Related docs:

- System Design Architecture
- TRD Risk Engine Requirements
- Unit Test Requirements

---

### 9.12 Implement Risk Decision Service

[ ] Create `RiskDecisionService`.

Goal:  
Map risk score to decision.

Output:

- `APPROVED` for score below 50.
- `REVIEW` for 50-79.
- `BLOCKED` for 80 or higher.

Done when:

- boundary scores 49, 50, 79, 80 are tested.
- decision values match Database Schema and API Contract.

Depends on:

- 9.11

Related docs:

- PRD MVP-02
- Database Schema
- Unit Test Requirements

---

### 9.13 Implement Risk Factor Persistence

[ ] Create `RiskFactorPersistenceService`.

Goal:  
Store triggered risk factors.

Output:

- save factor code.
- save description.
- save score.
- save metadata JSON.

Done when:

- all triggered factors are persisted.
- no untriggered factor is persisted.
- duplicate scoring does not create duplicate factors.

Depends on:

- 5.6
- 9.11

Related docs:

- Database Schema
- TRD Explainability

---

### 9.14 Implement Transaction Risk Update Service

[ ] Create transaction update service.

Goal:  
Update transaction status, risk score, and decision.

Output:

- update `transactions.risk_score`.
- update `transactions.risk_decision`.
- update `transactions.status`.
- update `updated_at`.

Done when:

- transaction status becomes APPROVED, REVIEW, or BLOCKED.
- failed transaction is handled clearly.
- integration test verifies database update.

Depends on:

- 5.5
- 9.12

Related docs:

- Database Schema
- PRD MVP-02

---

### 9.15 Implement Risk Scored Event Payload

[ ] Create `TransactionRiskScoredPayload`.

Goal:  
Represent `transaction.risk-scored` payload.

Output:

- transactionRef.
- riskScore.
- decision.
- riskFactors.
- scoredAt.

Done when:

- payload matches Kafka Event Contract.
- mapper unit tests validate payload generation.

Depends on:

- 9.11
- 9.12

Related docs:

- Kafka Event Contract

---

### 9.16 Implement Risk Scored Event Publisher

[ ] Publish `transaction.risk-scored`.

Goal:  
Notify Audit Search Service after scoring is completed.

Output:

- Kafka producer.
- event envelope builder.
- topic `transaction.risk-scored`.
- key `transactionRef`.

Done when:

- event is published after DB update and risk factor persistence.
- event log is recorded.
- integration test verifies event publication.

Depends on:

- 9.15
- 9.14

Related docs:

- Kafka Event Contract
- Test Scenarios

---

### 9.17 Orchestrate Full Risk Processing Flow

[ ] Connect consumer to scoring workflow.

Goal:  
Complete event-to-risk-result pipeline.

Output flow:

```text
consume transaction.created
validate envelope
check idempotency
build context
update Redis counters
execute rules
store factors
update transaction
publish transaction.risk-scored
ack message
```

Done when:

- full scoring flow works for low-risk, review, and blocked cases.
- retry happens on controlled failure.
- unit and integration tests cover major branches.

Depends on:

- 9.1 through 9.16
- Phase 10 Redis

Related docs:

- System Design Architecture
- Kafka Event Contract
- Redis Key Contract
- Test Scenarios

---

## 10. Phase 8 - Redis Cache and Counter Implementation

---

### 10.1 Configure Redis Client

[ ] Configure Redis in Risk Engine and Master Data Service.

Goal:  
Enable Redis access for cache, counters, and invalidation.

Output:

- Redis connection config.
- serializer config.
- timeout config.

Done when:

- services can connect to Redis.
- connection failure is handled safely.
- integration test can use Redis Testcontainer or local Redis.

Depends on:

- 4.4
- 6.2
- 6.4

Related docs:

- Redis Key Contract

---

### 10.2 Implement Redis Key Builder

[ ] Create Redis key builder utility.

Goal:  
Generate Redis keys consistently.

Output:

- blacklist account key.
- customer risk profile key.
- trusted device key.
- risk rule config key.
- velocity count key.
- velocity amount key.
- optional environment prefix.

Done when:

- generated keys exactly match Redis Key Contract.
- unit tests cover every key pattern.

Depends on:

- 10.1

Related docs:

- Redis Key Contract
- Unit Test Requirements

---

### 10.3 Implement Blacklist Cache Service

[ ] Create blacklist lookup cache service.

Goal:  
Support `BLACKLISTED_DESTINATION` rule.

Output:

- Redis lookup.
- PostgreSQL fallback.
- cache population.
- optional negative cache.

Done when:

- cache hit avoids DB lookup.
- cache miss queries DB and populates Redis.
- Redis failure falls back to DB.
- unit tests cover hit, miss, fallback, and failure path.

Depends on:

- 10.2
- 5.7

Related docs:

- Redis Key Contract
- Database Schema

---

### 10.4 Implement Customer Risk Profile Cache Service

[ ] Create customer risk profile cache service.

Goal:  
Support `HIGH_RISK_CUSTOMER_PROFILE` rule.

Output:

- Redis lookup.
- PostgreSQL fallback.
- JSON serialization.

Done when:

- risk profile is cached with correct TTL.
- cache miss repopulates Redis.
- Redis failure falls back to DB.

Depends on:

- 10.2
- 5.3

Related docs:

- Redis Key Contract

---

### 10.5 Implement Trusted Device Cache Service

[ ] Create trusted device cache service.

Goal:  
Support `NEW_DEVICE` rule.

Output:

- Redis lookup by customer ID and device ID.
- PostgreSQL fallback.
- cache population.

Done when:

- trusted and untrusted device behavior is correct.
- missing device behavior is explicit.
- unit tests cover key cases.

Depends on:

- 10.2
- 5.8

Related docs:

- Redis Key Contract

---

### 10.6 Implement Risk Rule Config Cache Service

[ ] Create risk rule config cache service.

Goal:  
Load rule threshold, score, and active status.

Output:

- cache lookup per rule.
- fallback to PostgreSQL.
- TTL handling.

Done when:

- active and inactive rules are loaded correctly.
- threshold values are available to rules.
- unit tests cover missing config fallback behavior.

Depends on:

- 10.2
- 5.10

Related docs:

- Redis Key Contract
- TRD Risk Engine Requirements

---

### 10.7 Implement Velocity Counter Service

[ ] Create Redis velocity counter service.

Goal:  
Support real-time transaction frequency detection.

Output:

- increment count counter.
- increment amount counter.
- set TTL only when key is first created.
- read current counter values.

Done when:

- keys match Redis Key Contract.
- TTL is not reset on every transaction.
- count and amount windows behave as designed.
- unit tests cover first increment and subsequent increment behavior.

Depends on:

- 10.2

Related docs:

- Redis Key Contract
- PRD MVP-04
- Unit Test Requirements

---

### 10.8 Integrate Redis Services into Transaction Context Builder

[ ] Use Redis cache and counters during context building.

Goal:  
Provide complete risk context to rules.

Output:

- destinationBlacklisted.
- customerRiskLevel.
- trustedDevice.
- velocityCount10m.
- velocityAmount10m.
- ruleConfigs.

Done when:

- Risk Engine uses Redis before DB fallback.
- context builder handles Redis degraded behavior.
- integration test verifies Redis key creation.

Depends on:

- 9.3
- 10.3 through 10.7

Related docs:

- Redis Key Contract
- Test Scenarios

---

## 11. Phase 9 - Master Data Service Implementation

---

### 11.1 Implement Blacklisted Account Entity and Repository

[ ] Add blacklisted account persistence.

Goal:  
Manage blacklist source data.

Output:

- entity mapping.
- repository methods.

Done when:

- table mapping matches Database Schema.
- active record lookup works.

Depends on:

- 5.7
- 6.4

Related docs:

- Database Schema

---

### 11.2 Implement Blacklisted Account APIs

[ ] Implement blacklist CRUD APIs.

Goal:  
Allow admin to manage blacklist records.

Output:

- POST create.
- GET list.
- GET detail.
- PUT update.
- DELETE/deactivate.

Done when:

- endpoints match API Contract.
- only admin can access mutation endpoints.
- duplicate account returns `DUPLICATE_RESOURCE`.

Depends on:

- 11.1
- 7.3

Related docs:

- API Contract
- Demo Scenarios

---

### 11.3 Implement Blacklist Cache Invalidation

[ ] Delete related Redis keys after blacklist changes.

Goal:  
Ensure updated blacklist status affects next scoring.

Output:

- delete positive blacklist key.
- delete optional negative blacklist key.

Done when:

- create/update/deactivate invalidates Redis key.
- demo can show cache invalidation behavior.

Depends on:

- 10.2
- 11.2

Related docs:

- Redis Key Contract
- Demo Scenarios

---

### 11.4 Implement Trusted Device Entity and Repository

[ ] Add trusted device persistence.

Goal:  
Manage trusted device source data.

Output:

- entity mapping.
- repository methods.

Done when:

- unique customer-device mapping works.
- trusted status is readable.

Depends on:

- 5.8
- 6.4

Related docs:

- Database Schema

---

### 11.5 Implement Trusted Device APIs

[ ] Implement trusted device APIs.

Goal:  
Allow admin to manage device trust.

Output:

- create trusted device.
- list customer devices.
- update device trust.

Done when:

- endpoints match API Contract.
- admin-only access is enforced.
- not found behavior is consistent.

Depends on:

- 11.4
- 7.3

Related docs:

- API Contract

---

### 11.6 Implement Trusted Device Cache Invalidation

[ ] Delete trusted device Redis key after device update.

Goal:  
Ensure next scoring uses latest trust status.

Output:

- delete `customer:trusted-device:{customerId}:{deviceId}`.

Done when:

- cache invalidation happens after trusted device create/update.
- unit tests cover key generation and invalidation call.

Depends on:

- 10.2
- 11.5

Related docs:

- Redis Key Contract

---

### 11.7 Implement Customer Risk Profile APIs

[ ] Implement risk profile read/update APIs.

Goal:  
Allow admin to update customer risk level and analyst/admin to read it.

Output:

- GET customer risk profile.
- PUT customer risk profile.

Done when:

- access matrix matches API Contract.
- risk level values are validated.
- update persists to PostgreSQL.

Depends on:

- 5.3
- 7.3

Related docs:

- API Contract
- Database Schema

---

### 11.8 Implement Customer Risk Profile Cache Invalidation

[ ] Delete customer risk profile Redis key after update.

Goal:  
Ensure risk scoring uses latest risk level.

Output:

- delete `customer:risk-profile:{customerId}`.

Done when:

- update API invalidates cache.
- unit tests cover invalidation call.

Depends on:

- 10.2
- 11.7

Related docs:

- Redis Key Contract

---

### 11.9 Implement Risk Rule Config APIs

[ ] Implement risk rule config APIs.

Goal:  
Allow rule threshold and score inspection/update.

Output:

- GET list risk rules.
- GET rule detail.
- PUT update rule.

Done when:

- endpoints match API Contract.
- role access matches authorization matrix.
- invalid rule code returns `RISK_RULE_NOT_FOUND`.

Depends on:

- 5.10
- 7.3

Related docs:

- API Contract
- Redis Key Contract

---

### 11.10 Implement Risk Rule Config Cache Invalidation

[ ] Delete rule config cache after rule update.

Goal:  
Ensure Risk Engine uses latest rule config.

Output:

- delete `risk-rule:config:{ruleCode}`.

Done when:

- update invalidates the relevant Redis key.
- unit tests cover invalidation.

Depends on:

- 10.2
- 11.9

Related docs:

- Redis Key Contract

---

## 12. Phase 10 - Audit Search Service Implementation

---

### 12.1 Create Elasticsearch Audit Document Model

[ ] Create audit document class.

Goal:  
Represent searchable fraud audit projection.

Output:

- transactionRef.
- customerCif.
- customerName.
- masked source account.
- masked destination account.
- amount.
- currency.
- channel.
- location.
- riskScore.
- decision.
- riskFactors.
- createdAt.

Done when:

- fields match System Design and API Contract.
- account numbers are masked before indexing.

Depends on:

- 6.3
- 3.5

Related docs:

- System Design Architecture
- API Contract

---

### 12.2 Configure Elasticsearch Index

[ ] Add index configuration for audit documents.

Goal:  
Prepare search index.

Output:

- index name.
- mapping strategy.
- keyword/text/date/number field types.

Done when:

- index can be created locally.
- search filters work as expected.

Depends on:

- 4.5
- 12.1

Related docs:

- System Design Architecture
- TRD Elasticsearch Requirements

---

### 12.3 Create Transaction Risk Scored Consumer

[ ] Consume `transaction.risk-scored`.

Goal:  
Index audit document after scoring.

Output:

- Kafka listener.
- envelope validation.
- payload validation.
- idempotent indexing.

Done when:

- event is consumed.
- document ID uses transactionRef.
- duplicate event updates same document.
- integration test verifies indexing.

Depends on:

- 6.3
- 12.1
- 4.3

Related docs:

- Kafka Event Contract
- Test Scenarios

---

### 12.4 Implement Audit Document Builder

[ ] Build Elasticsearch document from scored event and supporting data.

Goal:  
Create denormalized audit document.

Output:

- mapper from event payload to document.
- account number masking.
- risk factor mapping.

Done when:

- no raw account numbers are indexed.
- risk factors are searchable.
- unit tests cover document builder.

Depends on:

- 12.1
- 3.5

Related docs:

- API Contract
- Unit Test Requirements

---

### 12.5 Implement Audit Indexing Service

[ ] Save audit document to Elasticsearch.

Goal:  
Make scored transaction searchable.

Output:

- Elasticsearch repository/client call.
- upsert by transactionRef.

Done when:

- document is indexed.
- repeated processing does not duplicate document.
- dependency error is handled consistently.

Depends on:

- 12.2
- 12.4

Related docs:

- System Design Architecture
- Kafka Event Contract

---

### 12.6 Implement Audit Search API

[ ] Implement `GET /api/v1/audits/search`.

Goal:  
Allow fraud analyst to search audit data.

Output:

- keyword search.
- risk score filter.
- decision/status filter.
- date range filter.
- pagination.

Done when:

- endpoint matches API Contract.
- access is limited to admin and fraud analyst.
- search response uses paginated format.

Depends on:

- 12.5
- 7.3

Related docs:

- API Contract
- Demo Scenarios
- Test Scenarios

---

## 13. Phase 11 - Native SQL Reporting

---

### 13.1 Implement Report DTOs

[ ] Create DTOs for all report responses.

Goal:  
Standardize report API responses.

Output DTOs:

- high-risk transaction report.
- transaction velocity report.
- top risk customers report.
- suspicious destination accounts report.
- daily fraud trend report.
- risk score distribution report.
- daily top risky customers report.

Done when:

- response fields match API Contract.
- money fields use BigDecimal.
- datetime/date fields use required format.

Depends on:

- 6.1

Related docs:

- API Contract
- Database Schema

---

### 13.2 Implement High Risk Transactions Native Query

[ ] Add native SQL for high-risk transactions.

Goal:  
Return transactions above risk threshold.

Output:

- repository method.
- query parameters:
  - startDate
  - endDate
  - minimumRiskScore
  - pagination

Done when:

- query joins customers, accounts, and transactions correctly.
- results are ordered by risk score and amount.
- integration test validates expected output.

Depends on:

- 5.5
- 5.6
- 13.1

Related docs:

- Database Schema
- API Contract
- Test Scenarios

---

### 13.3 Implement Transaction Velocity Native Query

[ ] Add native SQL for transaction velocity.

Goal:  
Report accounts with high transaction frequency or amount.

Output:

- count and total amount per source account.
- configurable time window.

Done when:

- query groups by source account.
- HAVING clause applies count or amount threshold.
- integration test validates high-frequency case.

Depends on:

- 13.1

Related docs:

- Database Schema
- PRD MVP-07

---

### 13.4 Implement Top Risk Customers Native Query

[ ] Add native SQL for top risk customers.

Goal:  
Identify customers with high average or maximum risk.

Output:

- customer CIF.
- customer name.
- total transactions.
- average risk score.
- highest risk score.
- total amount.

Done when:

- query aggregates by customer.
- date range is applied.
- integration test validates ranking.

Depends on:

- 13.1

Related docs:

- Database Schema
- Test Scenarios

---

### 13.5 Implement Suspicious Destination Accounts Native Query

[ ] Add native SQL for suspicious destination accounts.

Goal:  
Identify destination accounts receiving funds from many unique senders.

Output:

- destination account.
- unique sender count.
- total received.
- average risk score.

Done when:

- query uses COUNT DISTINCT source accounts.
- thresholds are applied correctly.
- integration test validates expected suspicious destination.

Depends on:

- 13.1

Related docs:

- Database Schema
- PRD MVP-07

---

### 13.6 Implement Daily Fraud Trend Query

[ ] Add daily fraud trend native query.

Goal:  
Track fraud decision trends over time.

Output:

- date.
- approved count.
- review count.
- blocked count.
- average risk score.

Done when:

- query groups by date.
- results are sorted by date.
- integration test validates trend output.

Depends on:

- 13.1

Related docs:

- PRD MVP-07
- Test Scenarios

---

### 13.7 Implement Risk Score Distribution Query

[ ] Add risk score distribution native query.

Goal:  
Show distribution by score buckets.

Output:

- bucket label.
- transaction count.
- total amount.

Done when:

- score bucket logic is implemented in SQL.
- buckets match report contract.
- integration test validates bucket assignment.

Depends on:

- 13.1

Related docs:

- PRD MVP-07
- API Contract

---

### 13.8 Implement Daily Top Risky Customers Query with CTE and Window Function

[ ] Add advanced native SQL using CTE and window function.

Goal:  
Demonstrate advanced daily ranking of risky customers.

Output:

- CTE for daily customer stats.
- window function `RANK() OVER`.
- top N customers per day.

Done when:

- query uses CTE.
- query uses window function.
- top N per day output is correct.
- integration test validates ranking.

Depends on:

- 13.1

Related docs:

- System Design Architecture
- Database Schema

---

### 13.9 Implement Report Controller

[ ] Expose reporting endpoints.

Goal:  
Make reports accessible through API.

Output endpoints:

```text
GET /api/v1/reports/high-risk-transactions
GET /api/v1/reports/transaction-velocity
GET /api/v1/reports/top-risk-customers
GET /api/v1/reports/suspicious-destination-accounts
GET /api/v1/reports/daily-fraud-trend
GET /api/v1/reports/risk-score-distribution
GET /api/v1/reports/daily-top-risky-customers
```

Done when:

- endpoints match API Contract.
- admin and fraud analyst can access.
- backoffice cannot access reports.
- date range validation works.

Depends on:

- 13.2 through 13.8
- 7.3

Related docs:

- API Contract
- Test Scenarios
- Demo Scenarios

---

## 14. Phase 12 - Kafka Reliability and Failure Handling

---

### 14.1 Configure Kafka Producer Settings

[ ] Configure producer reliability settings.

Goal:  
Improve event publishing reliability.

Output:

- JSON serializer.
- acknowledgment configuration.
- retry configuration where appropriate.

Done when:

- producer can publish envelope events.
- publish failure is logged and mapped to expected error behavior.

Depends on:

- 4.3
- 8.8
- 9.16

Related docs:

- Kafka Event Contract

---

### 14.2 Configure Kafka Consumer Retry

[ ] Configure consumer retry strategy.

Goal:  
Retry failed event processing before DLQ.

Output:

- retry attempts.
- backoff configuration.
- recoverer to DLQ topic.

Done when:

- failed consumer processing retries expected number of times.
- exhausted failure is sent to DLQ.
- event log records FAILED or DLQ.

Depends on:

- 9.1
- 12.3

Related docs:

- Kafka Event Contract
- Test Scenarios

---

### 14.3 Configure Transaction Created DLQ

[ ] Configure `transaction.created.dlq`.

Goal:  
Preserve failed transaction created events.

Output:

- DLQ topic.
- original payload.
- error metadata.

Done when:

- failed risk engine processing sends message to DLQ.
- DLQ message preserves original event.

Depends on:

- 14.2

Related docs:

- Kafka Event Contract

---

### 14.4 Configure Transaction Risk Scored DLQ

[ ] Configure `transaction.risk-scored.dlq`.

Goal:  
Preserve failed risk-scored events.

Output:

- DLQ topic.
- original payload.
- error metadata.

Done when:

- failed audit indexing sends event to DLQ.
- DLQ message preserves original event.

Depends on:

- 14.2

Related docs:

- Kafka Event Contract

---

### 14.5 Implement Consumer Error Logging

[ ] Log event processing failures.

Goal:  
Make failures diagnosable.

Output:

- eventId.
- transactionRef.
- topic.
- consumer group.
- error message.
- stack trace where useful.

Done when:

- failed event logs contain enough context.
- sensitive data is not logged unnecessarily.

Depends on:

- 14.2

Related docs:

- Kafka Event Contract
- System Design Observability

---

## 15. Phase 13 - Error Handling and Validation Consistency

---

### 15.1 Implement Global Exception Handler

[ ] Create global error handler for API services.

Goal:  
Return consistent error response.

Output:

- validation error handler.
- business exception handler.
- authentication/authorization errors.
- dependency errors.

Done when:

- all errors match API Contract.
- requestId is included.
- unit tests cover error mapping logic.

Depends on:

- 3.3
- API service modules

Related docs:

- API Contract
- Unit Test Requirements

---

### 15.2 Implement Business Exception Model

[ ] Create business exception classes.

Goal:  
Represent domain-level errors consistently.

Output:

- error code.
- HTTP status.
- message.
- optional details.

Done when:

- services throw meaningful business exceptions.
- global handler maps them correctly.

Depends on:

- 15.1

Related docs:

- API Contract

---

### 15.3 Implement Date Range Validation

[ ] Validate report and audit search date range.

Goal:  
Prevent invalid report/search queries.

Output:

- startDate <= endDate.
- valid date format.
- error `INVALID_DATE_RANGE`.

Done when:

- invalid range returns 400.
- unit tests cover valid and invalid ranges.

Depends on:

- 13.9
- 12.6

Related docs:

- API Contract
- Test Scenarios

---

### 15.4 Implement Pagination Validation

[ ] Validate page and size parameters.

Goal:  
Enforce pagination standard.

Output:

- default page `0`.
- default size `20`.
- maximum size `100`.

Done when:

- invalid size returns `INVALID_PAGE_SIZE`.
- all paginated endpoints behave consistently.

Depends on:

- 3.4
- 12.6
- 13.9

Related docs:

- API Contract

---

## 16. Phase 14 - Unit Test Implementation

---

### 16.1 Configure Unit Test Dependencies

[ ] Configure JUnit 5, Mockito, and AssertJ.

Goal:  
Prepare unit testing environment.

Output:

- test dependencies.
- Maven Surefire configuration.
- test naming support.

Done when:

- `mvn test` runs without Docker or Spring context.
- sample unit test passes.

Depends on:

- 2.3

Related docs:

- Unit Test Requirements

---

### 16.2 Configure Coverage Tool

[ ] Configure JaCoCo.

Goal:  
Measure unit test coverage.

Output:

- JaCoCo Maven plugin.
- coverage report.
- minimum coverage thresholds.

Done when:

- report is generated.
- thresholds match Unit Test Requirements.
- build fails if minimum coverage is not met.

Depends on:

- 16.1

Related docs:

- Unit Test Requirements

---

### 16.3 Add Common Library Unit Tests

[ ] Write unit tests for common utilities and contracts.

Goal:  
Validate shared behavior.

Output:

- masking utility tests.
- event envelope builder tests.
- pagination response tests.
- error response factory tests.
- key constants tests only if conversion logic exists.

Done when:

- meaningful common-library logic is covered.
- trivial Lombok/data-only classes are not tested unnecessarily.

Depends on:

- Phase 3 common library

Related docs:

- Unit Test Requirements

---

### 16.4 Add Transaction Service Unit Tests

[ ] Write unit tests for Transaction Service logic.

Goal:  
Validate isolated transaction business logic.

Output coverage:

- validation service.
- idempotency service.
- transaction reference generator.
- event payload mapper.
- transaction detail mapper.
- masking behavior.
- error mapping.

Done when:

- no unit test starts Spring context.
- external dependencies are mocked.
- boundary cases are tested.

Depends on:

- Phase 6
- 16.1

Related docs:

- Unit Test Requirements
- Test Scenarios

---

### 16.5 Add Risk Engine Unit Tests

[ ] Write unit tests for Risk Engine logic.

Goal:  
Validate risk scoring behavior.

Output coverage:

- all risk rules.
- decision service.
- scoring orchestration.
- context builder with mocked dependencies.
- duplicate event skip logic.
- risk-scored event payload builder.

Done when:

- risk rules meet required coverage.
- boundary scores are tested.
- Java Stream pipeline behavior is validated through output, not implementation details.

Depends on:

- Phase 7
- 16.1

Related docs:

- Unit Test Requirements
- PRD MVP-02

---

### 16.6 Add Redis Logic Unit Tests

[ ] Write unit tests for Redis key and cache behavior logic.

Goal:  
Validate Redis-related logic without real Redis.

Output coverage:

- key builder.
- TTL selection logic.
- fallback decision logic.
- invalidation service calls.
- velocity counter first-increment TTL behavior.

Done when:

- Redis client is mocked.
- no real Redis is started.

Depends on:

- Phase 8
- 16.1

Related docs:

- Redis Key Contract
- Unit Test Requirements

---

### 16.7 Add Master Data Service Unit Tests

[ ] Write unit tests for master data logic.

Goal:  
Validate admin data updates and invalidation behavior.

Output coverage:

- blacklist create/update/deactivate.
- trusted device update.
- customer risk profile update.
- risk rule update.
- cache invalidation calls.

Done when:

- repository and Redis dependencies are mocked.
- duplicate resource and not found paths are covered.

Depends on:

- Phase 9
- 16.1

Related docs:

- API Contract
- Redis Key Contract

---

### 16.8 Add Audit Search Service Unit Tests

[ ] Write unit tests for audit search logic.

Goal:  
Validate document building and search request handling.

Output coverage:

- audit document builder.
- account masking before indexing.
- search request validation.
- event payload mapping.
- duplicate document ID behavior.

Done when:

- Elasticsearch client is mocked.
- no real Elasticsearch is started.

Depends on:

- Phase 10
- 16.1

Related docs:

- Unit Test Requirements
- API Contract

---

### 16.9 Add Reporting Unit Tests for Mapping and Validation

[ ] Write unit tests for report support logic.

Goal:  
Validate report input validation and result mapping.

Output coverage:

- date range validation.
- pagination validation.
- DTO mapping.
- sort mapping if supported.

Done when:

- SQL execution is not unit-tested.
- native SQL is covered by integration tests separately.

Depends on:

- Phase 11
- 16.1

Related docs:

- Unit Test Requirements
- API Contract

---

## 17. Phase 15 - Integration, API, Event, and E2E Tests

---

### 17.1 Add PostgreSQL Integration Tests

[ ] Write repository and migration integration tests.

Goal:  
Validate database schema and constraints.

Output coverage:

- Flyway migration starts cleanly.
- unique transaction reference.
- unique idempotency key.
- check constraints.
- foreign key relationships.
- native SQL reports.

Done when:

- tests run with Testcontainers PostgreSQL.
- failures prove DB constraints work.

Depends on:

- Phase 5
- Phase 11

Related docs:

- Database Schema
- Test Scenarios

---

### 17.2 Add API Contract Tests

[ ] Write API tests for all REST endpoints.

Goal:  
Validate API behavior against API Contract.

Output coverage:

- authentication.
- authorization.
- transaction submission.
- transaction detail.
- audit search.
- master data APIs.
- reporting APIs.
- error responses.

Done when:

- response body and status match API Contract.
- access matrix is verified.

Depends on:

- Phases 5, 6, 9, 10, 11, 13

Related docs:

- API Contract
- Test Scenarios

---

### 17.3 Add Kafka Integration Tests

[ ] Write Kafka producer/consumer tests.

Goal:  
Validate real Kafka behavior.

Output coverage:

- `transaction.created` publish.
- `transaction.created` consume.
- `transaction.risk-scored` publish.
- `transaction.risk-scored` consume.
- message key.
- envelope validation.
- DLQ behavior.

Done when:

- tests run with Testcontainers Kafka.
- event lifecycle matches Kafka Event Contract.

Depends on:

- Phase 7
- Phase 10
- Phase 12

Related docs:

- Kafka Event Contract
- Test Scenarios

---

### 17.4 Add Redis Integration Tests

[ ] Write Redis integration tests.

Goal:  
Validate cache and counter behavior with real Redis.

Output coverage:

- blacklist cache hit/miss.
- customer risk profile cache.
- trusted device cache.
- risk rule config cache.
- velocity count.
- velocity amount.
- TTL behavior.
- invalidation.

Done when:

- tests use Redis container or local test Redis.
- key names and TTLs match Redis Key Contract.

Depends on:

- Phase 8
- Phase 9

Related docs:

- Redis Key Contract
- Test Scenarios

---

### 17.5 Add Elasticsearch Integration Tests

[ ] Write Elasticsearch integration tests.

Goal:  
Validate audit indexing and search.

Output coverage:

- index creation.
- audit document indexing.
- keyword search.
- structured filters.
- duplicate event upsert behavior.

Done when:

- tests run with Elasticsearch Testcontainer.
- account numbers are masked in indexed documents.

Depends on:

- Phase 10

Related docs:

- System Design Architecture
- Test Scenarios

---

### 17.6 Add End-to-End Transaction Flow Test

[ ] Write E2E test for complete transaction lifecycle.

Goal:  
Validate full workflow from API to audit search.

Output flow:

```text
login
submit transaction
transaction stored
transaction.created published
risk engine scores
transaction updated
transaction.risk-scored published
audit document indexed
transaction detail returns risk factors
audit search returns document
```

Done when:

- end-to-end flow passes reliably.
- uses deterministic seed data.
- validates PostgreSQL, Kafka, Redis, and Elasticsearch side effects.

Depends on:

- Phases 6 through 12

Related docs:

- PRD
- Demo Scenarios
- Test Scenarios

---

### 17.7 Add Degraded Dependency Tests

[ ] Validate selected degraded dependency scenarios.

Goal:  
Ensure graceful behavior when dependencies fail.

Output coverage:

- Redis unavailable fallback to PostgreSQL.
- Elasticsearch unavailable during audit indexing.
- Kafka consumer processing failure to DLQ.
- protected API dependency unavailable returns correct error.

Done when:

- failure behavior matches contracts.
- logs and error codes are meaningful.

Depends on:

- Phase 12
- Phase 13

Related docs:

- Redis Key Contract
- Kafka Event Contract
- Test Scenarios

---

## 18. Phase 16 - Demo Scenario Implementation Support

---

### 18.1 Add Demo cURL Collection or Script

[ ] Create demo script file.

Goal:  
Make demo repeatable.

Output:

- `scripts/demo.sh` or Postman collection.
- login steps.
- submit low-risk transaction.
- submit review transaction.
- submit blocked transaction.
- audit search.
- reports.

Done when:

- demo commands match Demo Scenarios.
- script can be executed step-by-step.

Depends on:

- Phases 6 through 13

Related docs:

- Demo Scenarios
- API Contract

---

### 18.2 Add Seed Verification Script

[ ] Create seed verification SQL/script.

Goal:  
Confirm demo seed data exists.

Output:

- SQL select statements.
- optional shell wrapper.

Done when:

- demo user can verify customers, accounts, blacklist, devices, risk profiles, and rule configs.

Depends on:

- 5.13

Related docs:

- Demo Scenarios
- Database Schema

---

### 18.3 Add Kafka Topic Verification Script

[ ] Create Kafka verification commands.

Goal:  
Show event topics and messages during demo.

Output:

- list topics command.
- consume transaction.created.
- consume transaction.risk-scored.
- inspect DLQ topics.

Done when:

- demo can prove Kafka is actually used.

Depends on:

- Phase 12

Related docs:

- Demo Scenarios
- Kafka Event Contract

---

### 18.4 Add Redis Verification Script

[ ] Create Redis key inspection commands.

Goal:  
Show cache and counter usage during demo.

Output:

- inspect blacklist key.
- inspect risk profile key.
- inspect velocity count key.
- inspect velocity amount key.
- check TTL.

Done when:

- demo can prove Redis cache/counters are used.

Depends on:

- Phase 8

Related docs:

- Demo Scenarios
- Redis Key Contract

---

### 18.5 Add Elasticsearch Verification Script

[ ] Create Elasticsearch inspection commands.

Goal:  
Show audit documents indexed.

Output:

- query index.
- search by transactionRef.
- search by decision.
- search by risk factor.

Done when:

- demo can prove Elasticsearch is used as audit search read model.

Depends on:

- Phase 10

Related docs:

- Demo Scenarios

---

## 19. Phase 17 - Documentation and README

---

### 19.1 Write Main README

[ ] Create root `README.md`.

Goal:  
Explain how to run, test, and demo the project.

Output:

- project overview.
- architecture summary.
- prerequisites.
- startup command.
- service ports.
- demo commands.
- test commands.
- docs index.

Done when:

- a new developer can run the project using README only.

Depends on:

- Most implementation phases

Related docs:

- All documents

---

### 19.2 Document Environment Variables

[ ] Add environment variable documentation.

Goal:  
Make local config understandable.

Output:

- DB variables.
- Kafka variables.
- Redis variables.
- Elasticsearch variables.
- JWT variables.

Done when:

- values match Docker Compose.
- no secret is hardcoded in documentation beyond local demo values.

Depends on:

- Phase 2

Related docs:

- TRD Technical Requirements

---

### 19.3 Document Architecture Summary

[ ] Add architecture explanation to README.

Goal:  
Summarize how services interact.

Output:

- transaction flow diagram.
- service responsibility list.
- data storage explanation.

Done when:

- summary matches System Design.
- no contradiction with contracts.

Depends on:

- System implementation

Related docs:

- System Design Architecture

---

### 19.4 Document Known Limitations

[ ] Add known limitations section.

Goal:  
Clearly separate MVP from production capabilities.

Output:

- no real money movement.
- no core banking integration.
- no ML scoring.
- local-only Docker Compose deployment.
- simplified auth service for MVP.

Done when:

- limitations align with PRD non-goals.

Depends on:

- 19.1

Related docs:

- PRD
- TRD

---

## 20. Phase 18 - Final Validation

---

### 20.1 Run Full Unit Test Suite

[ ] Execute unit tests.

Goal:  
Verify isolated business logic.

Command:

```bash
mvn test
```

Done when:

- all unit tests pass.
- coverage threshold passes.
- no Docker dependency is required.

Depends on:

- Phase 14

Related docs:

- Unit Test Requirements

---

### 20.2 Run Integration Test Suite

[ ] Execute integration tests.

Goal:  
Verify external dependency integrations.

Done when:

- PostgreSQL tests pass.
- Kafka tests pass.
- Redis tests pass.
- Elasticsearch tests pass.

Depends on:

- Phase 15

Related docs:

- Test Scenarios

---

### 20.3 Run Docker Compose Smoke Test

[ ] Start full platform locally.

Command:

```bash
docker compose up -d
docker compose ps
```

Goal:  
Verify local deployment.

Done when:

- all containers are running.
- health endpoints return `UP`.
- services can connect to dependencies.

Depends on:

- Phase 2
- all service implementations

Related docs:

- Demo Scenarios

---

### 20.4 Run Full Demo Scenario

[ ] Execute demo scenario from start to finish.

Goal:  
Prove full MVP behavior.

Done when:

- login works.
- valid transaction submission works.
- idempotency works.
- risk scoring works.
- Redis keys are created.
- Kafka events are visible.
- PostgreSQL is updated.
- Elasticsearch audit document is searchable.
- reports return data.
- health checks pass.

Depends on:

- Phase 16

Related docs:

- Demo Scenarios

---

### 20.5 Validate Documentation Consistency

[ ] Review all documents against implementation.

Goal:  
Ensure implementation and docs are consistent.

Done when:

- API behavior matches API Contract.
- table schema matches Database Schema.
- Kafka event payloads match Kafka Contract.
- Redis keys match Redis Contract.
- test coverage matches Unit Test Requirements.
- demo steps match actual commands.

Depends on:

- all phases

Related docs:

- All documents

---

## 21. Final MVP Completion Checklist

The BankGuard MVP is complete only when all of the following are true:

[ ] All services start through Docker Compose.  
[ ] PostgreSQL migrations run successfully.  
[ ] Seed data is available.  
[ ] Login works for admin, backoffice, and analyst.  
[ ] Role-based authorization works.  
[ ] Valid transaction submission creates exactly one transaction.  
[ ] Duplicate idempotency key does not create duplicate transaction.  
[ ] `transaction.created` is published to Kafka.  
[ ] Risk Engine consumes `transaction.created`.  
[ ] Risk Engine evaluates all active risk rules.  
[ ] Redis cache lookup and fallback behavior works.  
[ ] Redis velocity counters are created with TTL.  
[ ] Risk factors are persisted.  
[ ] Transaction status, risk score, and decision are updated.  
[ ] `transaction.risk-scored` is published to Kafka.  
[ ] Audit Search Service consumes `transaction.risk-scored`.  
[ ] Elasticsearch audit document is indexed using `transactionRef` as document ID.  
[ ] Audit search returns indexed transaction.  
[ ] Transaction detail returns masked account numbers and risk factors.  
[ ] Master Data Service invalidates Redis cache after updates.  
[ ] Native SQL report endpoints return correct data.  
[ ] Kafka retry and DLQ behavior works for controlled failure scenario.  
[ ] API error response format is consistent.  
[ ] Unit test quality gate passes.  
[ ] Integration tests for PostgreSQL, Kafka, Redis, and Elasticsearch pass.  
[ ] E2E test passes.  
[ ] Demo scenario can be executed from README.  
[ ] All documentation is consistent with implementation.

---

## 22. MVP Traceability Matrix

| PRD MVP | Primary Todo Phases |
|---|---|
| MVP-01 Transaction Submission | Phase 5, Phase 6, Phase 13, Phase 15 |
| MVP-02 Risk Scoring Engine | Phase 7, Phase 8, Phase 14, Phase 15 |
| MVP-03 Event-Based Transaction Processing | Phase 7, Phase 10, Phase 12, Phase 15 |
| MVP-04 Redis Risk Cache and Real-Time Risk Counters | Phase 8, Phase 9, Phase 14, Phase 15 |
| MVP-05 Elasticsearch Audit Search | Phase 10, Phase 13, Phase 15 |
| MVP-06 Transaction Investigation API | Phase 6, Phase 10, Phase 13, Phase 15 |
| MVP-07 Native SQL Reporting | Phase 11, Phase 13, Phase 15 |

---

## 23. Service Traceability Matrix

| Service | Primary Todo Phases |
|---|---|
| Common Library | Phase 1 |
| Transaction Service | Phase 5, Phase 6, Phase 11, Phase 13 |
| Risk Engine Service | Phase 7, Phase 8, Phase 12 |
| Audit Search Service | Phase 10, Phase 12 |
| Master Data Service | Phase 9, Phase 13 |
| PostgreSQL | Phase 3, Phase 15 |
| Kafka | Phase 2, Phase 7, Phase 12, Phase 15 |
| Redis | Phase 2, Phase 8, Phase 9, Phase 15 |
| Elasticsearch | Phase 2, Phase 10, Phase 15 |

---

## 24. Suggested Implementation Order Summary

The recommended implementation order is:

```text
1. Repository and common library
2. Docker Compose infrastructure
3. PostgreSQL migrations and seed data
4. Security and login
5. Transaction submission
6. Kafka transaction.created publish
7. Risk Engine consumer and rules
8. Redis cache and velocity counters
9. Risk result persistence and transaction.risk-scored publish
10. Audit Search indexing and search API
11. Master Data APIs and cache invalidation
12. Native SQL reporting APIs
13. Error handling consistency
14. Unit tests
15. Integration tests
16. Demo scripts
17. README and final documentation review
```

Do not start from Kafka or Elasticsearch first. Start from stable data model, transaction submission, and one successful end-to-end flow. After the first full flow works, expand risk rules, cache behavior, reporting, and failure handling.

