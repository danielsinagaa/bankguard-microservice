# Unit Test Requirements Contract
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the unit test requirements and standards for **BankGuard**.

The purpose of this document is not to list every unit test that must be written. The codebase has not been fully implemented yet, and the expected direction is that as much meaningful application logic as possible should be covered by unit tests.

Instead, this document defines the minimum requirements, testing standards, quality gates, and implementation rules that every unit test must follow across all BankGuard services.

This document is aligned with:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`
- `04_API_CONTRACT.md`
- `05_DATABASE_SCHEMA.md`
- `06_KAFKA_EVENT_CONTRACT.md`
- `07_REDIS_KEY_CONTRACT.md`
- `08_TEST_SCENARIOS.md`
- `09_DEMO_SCENARIOS.md`

---

### 1.2 Relationship with Test Scenarios Document

`08_TEST_SCENARIOS.md` defines what system behavior must be validated across unit, integration, API, event, Redis, Elasticsearch, database, and end-to-end testing.

This document specifically defines how unit tests must be written and what minimum unit testing standards must be met.

In simple terms:

| Document | Purpose |
|---|---|
| `08_TEST_SCENARIOS.md` | Defines validation scenarios across the whole system. |
| `UNIT_TEST_REQUIREMENTS.md` | Defines unit test rules, quality gates, and minimum standards. |

---

### 1.3 Scope

This document covers unit testing requirements for:

1. Common library.
2. Transaction Service.
3. Risk Engine Service.
4. Audit Search Service.
5. Master Data Service.
6. DTO validation logic.
7. Service-layer business logic.
8. Risk rule logic.
9. Mapper logic.
10. Utility logic.
11. Error handling logic.
12. Security-related helper logic.
13. Kafka event builder logic.
14. Redis key builder and cache behavior logic.
15. Elasticsearch document builder logic.
16. Native SQL request/response mapping logic where applicable.

---

### 1.4 Non-Scope

This document does not define requirements for:

- Integration tests with real PostgreSQL.
- Integration tests with Kafka.
- Integration tests with Redis.
- Integration tests with Elasticsearch.
- Full API tests.
- End-to-end tests.
- Docker Compose verification.
- Manual demo steps.
- Production load testing.
- UI testing.

Those areas are covered by other documents, especially `08_TEST_SCENARIOS.md` and `09_DEMO_SCENARIOS.md`.

---

## 2. Unit Test Philosophy

### 2.1 Unit Tests Must Validate Business Logic, Not Framework Behavior

Unit tests must focus on BankGuard-specific behavior.

Unit tests should validate:

- validation decisions
- rule evaluation
- risk score calculation
- decision threshold mapping
- idempotency decisions
- event payload building
- cache key building
- mapping behavior
- masking logic
- error conversion
- authorization helper logic
- request-to-domain transformation
- domain-to-response transformation

Unit tests should not waste effort testing behavior already guaranteed by Spring, Jackson, JPA, Kafka client, Redis client, or Elasticsearch client.

Examples of low-value tests that should be avoided:

```text
Testing that @Autowired works.
Testing that Lombok getter returns a field.
Testing that Spring can instantiate a controller.
Testing that a JPA repository method exists.
Testing that ObjectMapper can serialize a normal POJO.
```

---

### 2.2 Unit Tests Must Be Fast

Unit tests must run without external infrastructure.

Unit tests must not start:

- PostgreSQL
- Kafka
- Redis
- Elasticsearch
- Docker containers
- full Spring application context

Unit tests must be executable quickly during local development and continuous integration.

Expected unit test behavior:

```text
mvn test
```

must run unit tests without requiring Docker Compose.

---

### 2.3 Unit Tests Must Be Deterministic

A unit test must produce the same result every time it runs.

Unit tests must not depend on:

- current system time unless clock is injected or fixed
- random UUIDs unless UUID generator is mocked
- database state
- external API calls
- Kafka broker availability
- Redis state
- Elasticsearch index state
- test execution order

If time is needed, use a fixed `Clock`.

If UUID generation is needed, inject a generator and mock it.

If sequence generation is needed, mock the sequence provider.

---

### 2.4 Unit Tests Must Be Isolated

A unit test must test one behavior at a time.

Each test should have:

- clear given condition
- one action under test
- clear expected result
- no dependency on previous test execution
- no shared mutable state unless reset before each test

---

### 2.5 Unit Tests Must Be Meaningful

The goal is not to increase coverage numbers by testing trivial code.

A good unit test should fail when business behavior is broken.

A bad unit test only checks that the current implementation was executed without proving correctness.

Example of weak assertion:

```java
assertNotNull(result);
```

Example of better assertion:

```java
assertThat(result.riskScore()).isEqualTo(75);
assertThat(result.decision()).isEqualTo(RiskDecision.REVIEW);
assertThat(result.riskFactors())
    .extracting(RiskFactor::code)
    .containsExactlyInAnyOrder("HIGH_AMOUNT", "NEW_DEVICE", "UNUSUAL_LOCATION");
```

---

## 3. Minimum Unit Test Quality Gates

### 3.1 Minimum Coverage Requirement

The project should enforce minimum coverage for unit-testable code.

Recommended minimum coverage:

| Area | Minimum Line Coverage | Minimum Branch Coverage |
|---|---:|---:|
| Risk rule implementations | 95% | 90% |
| Risk scoring orchestration | 90% | 85% |
| Validation services | 90% | 85% |
| Mapper classes | 80% | 70% |
| Utility classes | 90% | 85% |
| Event builder classes | 85% | 80% |
| Cache key builder classes | 90% | 90% |
| Service classes | 85% | 80% |
| Controller classes | Optional unit tests; API tests preferred | Optional |
| Repository classes | Not required as unit tests | Not required |

Overall recommended minimum:

```text
Line coverage: 80%
Branch coverage: 70%
```

Critical business logic should exceed the overall project average.

---

### 3.2 Coverage Exclusions

The following may be excluded from unit test coverage metrics:

- Spring Boot main application classes.
- Pure configuration classes.
- Generated code.
- Lombok-only data classes.
- DTOs without logic.
- JPA entity classes without behavior.
- OpenAPI generated classes.
- Migration scripts.
- Docker files.
- Exception classes without behavior.

Exclusions must not be used to hide untested business logic.

---

### 3.3 Build Failure Rule

The build should fail when:

- unit tests fail
- required coverage thresholds are not met
- mutation-prone critical logic has no branch coverage
- test naming does not clearly describe behavior
- flaky tests are detected and left unresolved

Recommended tool:

```text
JaCoCo
```

---

## 4. Test Naming Standard

### 4.1 Test Class Naming

Unit test class names must follow:

```text
<ClassUnderTest>Test
```

Examples:

```text
HighAmountRuleTest
RiskDecisionServiceTest
TransactionValidationServiceTest
AccountNumberMaskingUtilTest
TransactionCreatedEventBuilderTest
RedisKeyBuilderTest
```

---

### 4.2 Test Method Naming

Test method names must describe:

1. condition
2. action
3. expected result

Recommended format:

```text
should<ExpectedBehavior>When<Condition>
```

Examples:

```java
shouldTriggerHighAmountRuleWhenAmountExceedsThreshold()
shouldReturnReviewDecisionWhenRiskScoreIsBetween50And79()
shouldRejectTransactionWhenAmountIsNegative()
shouldReturnExistingTransactionWhenIdempotencyKeyAlreadyExists()
shouldMaskAccountNumberWhenLengthIsGreaterThanSeven()
shouldBuildTransactionCreatedEventWithRequiredEnvelopeFields()
```

Alternative BDD-style naming is allowed if consistent:

```java
givenHighAmount_whenEvaluateRule_thenRiskFactorIsTriggered()
```

A service/module should use one naming style consistently.

---

## 5. Test Structure Standard

### 5.1 Arrange-Act-Assert

Every unit test must follow the Arrange-Act-Assert structure.

```java
@Test
void shouldReturnBlockedDecisionWhenRiskScoreIsAtLeast80() {
    // Arrange
    int riskScore = 80;

    // Act
    RiskDecision decision = riskDecisionService.decide(riskScore);

    // Assert
    assertThat(decision).isEqualTo(RiskDecision.BLOCKED);
}
```

---

### 5.2 Given-When-Then Comments

Comments are optional, but recommended when the test has multiple setup steps.

Use:

```text
// Given
// When
// Then
```

or:

```text
// Arrange
// Act
// Assert
```

Do not over-comment obvious assertions.

---

### 5.3 One Behavior per Test

A unit test should verify one behavior.

Allowed:

```text
Test one risk rule trigger condition.
Test one validation failure.
Test one decision threshold boundary.
```

Avoid:

```text
Testing login, transaction creation, Kafka publishing, risk scoring, Redis, and Elasticsearch in one unit test.
```

That belongs to integration or end-to-end testing.

---

## 6. Mocking Standard

### 6.1 Mock External Dependencies

Unit tests must mock:

- repositories
- Kafka producers
- Redis clients/templates
- Elasticsearch repositories/templates
- HTTP clients
- password encoders when not testing encoding itself
- JWT utilities when testing services that depend on tokens
- time providers
- UUID generators
- sequence generators

---

### 6.2 Do Not Mock the Class Under Test

The class being tested must be a real instance.

Bad:

```java
RiskDecisionService riskDecisionService = mock(RiskDecisionService.class);
```

Good:

```java
RiskDecisionService riskDecisionService = new RiskDecisionService();
```

---

### 6.3 Prefer Real Value Objects

Do not mock simple value objects or records.

Bad:

```java
TransactionContext context = mock(TransactionContext.class);
```

Good:

```java
TransactionContext context = new TransactionContext(...);
```

Use builders or test fixtures for readability.

---

### 6.4 Mock Behavior Must Be Minimal

Only mock what the test actually needs.

Avoid large setup blocks that make the test hard to understand.

---

### 6.5 Verify Side Effects Carefully

For service methods that interact with dependencies, verify important side effects.

Examples:

```java
verify(transactionRepository).save(any(Transaction.class));
verify(transactionEventPublisher).publish(any(TransactionCreatedEvent.class));
verify(redisTemplate).delete("blacklist:account:9876543210");
```

Do not overuse `verifyNoMoreInteractions()` unless the absence of additional calls is part of the behavior.

---

## 7. Assertion Standard

### 7.1 Preferred Assertion Library

Use:

```text
AssertJ
```

Recommended dependency:

```text
org.assertj:assertj-core
```

---

### 7.2 Assertions Must Be Specific

Avoid weak assertions.

Bad:

```java
assertThat(result).isNotNull();
```

Good:

```java
assertThat(result.transactionRef()).isEqualTo("TRX-20260604-000001");
assertThat(result.status()).isEqualTo("PENDING_RISK_CHECK");
```

---

### 7.3 Collection Assertions

Use collection assertions for risk factors, validation errors, and roles.

Example:

```java
assertThat(riskFactors)
    .extracting(RiskFactor::code)
    .containsExactlyInAnyOrder(
        "HIGH_AMOUNT",
        "NEW_DEVICE"
    );
```

---

### 7.4 Exception Assertions

Exception tests must verify the exception type and key fields.

Example:

```java
assertThatThrownBy(() -> service.validate(request))
    .isInstanceOf(ValidationException.class)
    .hasMessageContaining("Amount must be greater than zero");
```

For custom exceptions:

```java
assertThatThrownBy(() -> service.validateSourceAccount(accountNumber))
    .isInstanceOf(BusinessException.class)
    .extracting("errorCode")
    .isEqualTo("SOURCE_ACCOUNT_NOT_FOUND");
```

---

## 8. Test Data Standard

### 8.1 Test Fixtures

Each service should have fixture classes for reusable objects.

Recommended fixture package:

```text
src/test/java/com/bankguard/<service>/fixture
```

Example fixture classes:

```text
TransactionRequestFixture
TransactionFixture
AccountFixture
CustomerFixture
RiskFactorFixture
EventEnvelopeFixture
RedisValueFixture
```

---

### 8.2 Fixture Rules

Fixtures must:

- create valid default objects
- allow overriding important fields
- keep test setup readable
- avoid hidden behavior
- avoid using random values by default

Example:

```java
TransactionRequest request = TransactionRequestFixture.valid()
    .amount(new BigDecimal("25000000"))
    .deviceId("DEVICE-UNTRUSTED-001")
    .location("Surabaya")
    .build();
```

---

### 8.3 No Random Test Data by Default

Avoid:

```java
UUID.randomUUID()
LocalDateTime.now()
new Random().nextInt()
```

Prefer:

```java
UUID.fromString("7a7f0e9b-3dd6-4f22-b23a-44e42c25a001")
Instant.parse("2026-06-04T10:00:00Z")
```

---

### 8.4 Test Data Must Match Domain Constraints

Unit test data should respect known allowed values unless the test is specifically checking invalid data.

Examples:

```text
currency = IDR
channel = MOBILE_BANKING
status = PENDING_RISK_CHECK
riskDecision = REVIEW
riskLevel = HIGH
```

Allowed values are defined in `05_DATABASE_SCHEMA.md`.

---

## 9. Unit Test Requirements by Component Type

## 9.1 Service Classes

Service class unit tests must validate:

- successful business flow
- validation failures
- dependency failure mapping where relevant
- conditional branches
- idempotency decisions
- state transition decisions
- correct dependency calls
- no dependency calls when validation fails

Service tests must mock repositories and infrastructure clients.

---

## 9.2 Validation Classes

Validation class tests must validate:

- required fields
- invalid enum-like values
- amount boundaries
- date range boundaries
- idempotency key requirement
- account status rules
- role-based decision helpers if implemented outside Spring Security

Validation tests must include boundary cases, not only normal cases.

---

## 9.3 Mapper Classes

Mapper unit tests must validate:

- all required fields are mapped
- sensitive fields are masked when required
- null optional fields are handled safely
- metadata maps are preserved
- date-time fields use UTC-compatible types
- BigDecimal values are not converted to floating point

Do not write mapper tests that only duplicate generated mapper behavior unless the mapper contains business rules.

---

## 9.4 Utility Classes

Utility class unit tests must validate:

- account number masking
- transaction reference formatting
- Redis key formatting
- date/time formatting
- error response construction
- correlation ID generation fallback
- event envelope validation helpers

Utility tests must include edge cases.

---

## 9.5 Event Builder Classes

Kafka event builder tests must validate:

- event envelope fields
- event type
- event version
- aggregate type
- aggregate ID
- payload fields
- producer name
- request ID propagation
- correlation ID propagation
- ISO-8601 timestamp formatting

Event builder tests must be aligned with `06_KAFKA_EVENT_CONTRACT.md`.

---

## 9.6 Kafka Consumer Logic

Pure consumer orchestration may be unit-tested by mocking service dependencies.

Unit tests must validate:

- valid event delegates to application service
- unsupported event version is rejected
- malformed envelope is rejected
- duplicate event is ignored when detected
- processing exception is propagated or mapped according to retry strategy

Actual Kafka broker behavior belongs to integration tests.

---

## 9.7 Redis Logic

Redis-related unit tests must validate:

- key builder output
- TTL selection
- cache miss decision
- cache hit decision
- PostgreSQL fallback branch
- invalidation key selection
- velocity counter key naming
- degraded Redis fallback decision

Actual Redis read/write behavior belongs to integration tests.

Redis unit tests must be aligned with `07_REDIS_KEY_CONTRACT.md`.

---

## 9.8 Risk Rule Classes

Each risk rule must have unit tests for:

- trigger condition
- non-trigger condition
- boundary condition
- missing optional input behavior
- score from rule configuration
- inactive rule behavior if handled inside rule
- metadata output

Risk rule tests are mandatory because they directly determine transaction decisions.

---

## 9.9 Risk Decision Classes

Risk decision mapping must have unit tests for boundary values.

Required boundary cases:

| Score | Expected Decision |
|---:|---|
| 0 | APPROVED |
| 49 | APPROVED |
| 50 | REVIEW |
| 79 | REVIEW |
| 80 | BLOCKED |
| 100 | BLOCKED |
| More than 100 | BLOCKED |

---

## 9.10 Security Helper Classes

If JWT utilities, role parsing, or user mapping logic is implemented, unit tests must validate:

- token claim extraction
- role extraction
- expired token handling
- invalid token handling
- inactive user handling
- missing role handling

Full Spring Security authorization must be covered by API or integration tests, not pure unit tests.

---

## 9.11 Native SQL Result Mapping

Native SQL execution itself should not be unit tested.

However, mapping from native query result rows to response DTOs must be unit tested if manual mapping is used.

Tests must validate:

- numeric type conversion
- BigDecimal preservation
- null handling
- date/time conversion
- field naming consistency with API response
- pagination metadata construction

---

## 10. Service-Specific Unit Test Requirements

## 10.1 Common Library

Minimum unit test requirements:

- common error response builder
- pagination response builder
- account masking utility
- event envelope factory
- event envelope validator
- correlation ID helper
- common enum parsing if implemented
- common constants should not require tests unless behavior exists

---

## 10.2 Transaction Service

Minimum unit test requirements:

- transaction request validation
- idempotency service behavior
- account lookup decision
- source account status validation
- transaction reference generator
- transaction creation orchestration
- transaction created event builder
- transaction detail response mapper
- transaction response account masking
- report parameter validation
- authentication token response builder if custom logic exists

Important behaviors:

```text
A duplicate idempotency key must not create a new transaction.
A missing idempotency key must fail before persistence.
A source account not found must not publish Kafka events.
A source account inactive must not publish Kafka events.
A valid new transaction must be stored with PENDING_RISK_CHECK.
```

---

## 10.3 Risk Engine Service

Minimum unit test requirements:

- transaction created event validation
- transaction context builder
- all risk rules
- risk score aggregation
- decision threshold mapping
- risk factor persistence orchestration
- transaction status update decision
- duplicate event handling
- already-scored transaction handling
- risk scored event builder
- Redis cache lookup decision
- Redis velocity counter decision

Important behaviors:

```text
All active rules must be evaluated.
Only triggered risk factors contribute to the score.
Final decision must follow the threshold.
Already-final transactions must not be rescored.
Risk factors must be stored before final status is considered complete.
```

---

## 10.4 Audit Search Service

Minimum unit test requirements:

- risk scored event validation
- fraud audit document builder
- account number masking before indexing
- Elasticsearch document ID selection
- search request validation
- search query builder logic
- search response mapper
- pagination response mapper
- duplicate event document overwrite decision

Important behaviors:

```text
transactionRef must be used as Elasticsearch document ID.
Account numbers must never be indexed unmasked.
Elasticsearch search response must follow API pagination format.
```

---

## 10.5 Master Data Service

Minimum unit test requirements:

- blacklisted account create/update/deactivate logic
- duplicate blacklist handling
- trusted device create/update logic
- customer risk profile update logic
- risk rule config update validation
- Redis invalidation key selection
- response mapper logic

Important behaviors:

```text
Creating or updating blacklist must invalidate related Redis keys.
Updating trusted device must invalidate related Redis keys.
Updating customer risk profile must invalidate related Redis keys.
Updating risk rule config must invalidate related Redis keys.
```

---

## 11. Boundary Testing Requirements

### 11.1 Numeric Boundaries

Unit tests must cover numeric boundaries for:

- transaction amount
- risk score
- threshold values
- page size
- page number
- velocity count
- velocity amount

Examples:

```text
amount = 0
amount = -1
amount = threshold - 1
amount = threshold
amount = threshold + 1
riskScore = 49
riskScore = 50
riskScore = 79
riskScore = 80
```

---

### 11.2 Optional Field Boundaries

Unit tests must cover optional fields:

- `deviceId` is null
- `location` is null
- `ipAddress` is null
- risk factor metadata is null or empty
- requestId is missing and generated internally
- correlationId is missing and propagated safely

---

### 11.3 Enum and Allowed Value Boundaries

Unit tests must cover invalid values for:

- transaction channel
- transaction status
- risk decision
- customer risk level
- account status
- role code
- Kafka event type
- Kafka event version

---

## 12. Error Handling Unit Test Requirements

### 12.1 Business Exceptions

Business exception unit tests must verify:

- error code
- message
- HTTP status mapping if handled in custom mapper
- details list when applicable

Examples:

```text
SOURCE_ACCOUNT_NOT_FOUND
SOURCE_ACCOUNT_INACTIVE
IDEMPOTENCY_KEY_REQUIRED
INVALID_CHANNEL
INVALID_DATE_RANGE
RISK_RULE_NOT_FOUND
```

---

### 12.2 No Side Effects After Validation Failure

When validation fails, unit tests must verify that no persistence or publishing side effects occur.

Example:

```java
verify(transactionRepository, never()).save(any());
verify(transactionEventPublisher, never()).publish(any());
```

---

### 12.3 Dependency Failure Mapping

If service logic catches dependency exceptions, unit tests must verify how they are mapped.

Examples:

```text
Kafka publish failure -> EVENT_PUBLISH_FAILED
Redis failure -> fallback to PostgreSQL for cacheable data
Elasticsearch failure -> propagated or mapped to DEPENDENCY_UNAVAILABLE
```

---

## 13. Unit Test Requirements for Idempotency

Unit tests must validate:

- new idempotency key creates new transaction flow
- existing idempotency key returns existing transaction response
- duplicate request does not call transaction creation path
- duplicate request does not publish new Kafka event
- idempotency key is required
- idempotency lookup failure is handled according to service rules

---

## 14. Unit Test Requirements for Risk Scoring

Unit tests must validate:

- each rule independently
- all active rules are evaluated
- inactive rules are excluded if active flag is part of rule logic
- triggered factors only are included
- score is sum of triggered factors
- decision is derived from total score
- no triggered factors results in score 0 and decision APPROVED
- high-risk customer contributes expected score
- blacklisted destination contributes expected score
- velocity count or amount can trigger high-frequency rule
- missing optional device/location does not trigger related rules by default

---

## 15. Unit Test Requirements for Kafka Event Logic

Unit tests must validate:

- `transaction.created` event builder
- `transaction.risk-scored` event builder
- event envelope validator
- event version validator
- event type validator
- aggregate ID consistency
- payload required field validation
- request ID propagation
- correlation ID propagation
- message key selection

Actual Kafka publish and consume behavior must be integration tested.

---

## 16. Unit Test Requirements for Redis Logic

Unit tests must validate:

- all cache key builders
- all counter key builders
- TTL resolver
- cache-aside decision tree
- negative cache decision if implemented
- invalidation key resolver
- Redis failure fallback decision
- velocity count threshold decision
- velocity amount threshold decision

Actual Redis commands and TTL behavior must be integration tested.

---

## 17. Unit Test Requirements for Elasticsearch Logic

Unit tests must validate:

- audit document builder
- masking before document creation
- document ID equals transactionRef
- risk factor list mapping
- search request validation
- search query parameter translation
- search response mapping
- pagination metadata mapping

Actual indexing and search behavior must be integration tested.

---

## 18. Unit Test Requirements for API Contract Support Logic

Controller behavior may be covered by API tests, but supporting logic must be unit tested.

Unit tests must validate:

- common error response builder
- pagination response builder
- sorting parameter validator
- date range validator
- request DTO custom validators
- role-to-access helper if implemented
- account masking used in response mapper
- report response mapper

---

## 19. Unit Test Requirements for Database Schema Support Logic

Database constraints are validated through integration tests.

However, unit tests must validate code-level rules that mirror database constraints, such as:

- amount must be greater than zero
- status values must be allowed
- currency must be IDR for MVP
- transaction channel must be allowed
- risk score must not be negative
- idempotency key must be present
- account number must be non-blank
- risk decision must match threshold mapping

---

## 20. Test Package Structure

Each service should use this test package structure:

```text
src/test/java/com/bankguard/<service_name>
├── fixture
├── unit
│   ├── service
│   ├── validator
│   ├── mapper
│   ├── util
│   ├── event
│   ├── security
│   └── rule
└── support
```

Example:

```text
risk-engine-service/
└── src/test/java/com/bankguard/riskengine
    ├── fixture
    ├── unit
    │   ├── rule
    │   ├── service
    │   ├── cache
    │   └── event
    └── support
```

---

## 21. Required Unit Test Dependencies

Recommended dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</dependency>
```

If mutation testing is introduced later:

```xml
<dependency>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-junit5-plugin</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 22. Recommended JUnit and Mockito Rules

### 22.1 JUnit 5

Use JUnit 5.

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
```

---

### 22.2 Mockito Extension

Use Mockito extension when mocking dependencies:

```java
@ExtendWith(MockitoExtension.class)
class TransactionApplicationServiceTest {
}
```

---

### 22.3 Constructor Injection in Tests

Prefer constructor injection in production classes and instantiate them directly in tests.

Example:

```java
@BeforeEach
void setUp() {
    service = new TransactionApplicationService(
        transactionRepository,
        accountLookupService,
        idempotencyService,
        transactionEventPublisher
    );
}
```

---

### 22.4 Avoid Full Spring Context for Unit Tests

Avoid:

```java
@SpringBootTest
```

for unit tests.

Use `@SpringBootTest` only for integration tests.

---

## 23. Unit Test Review Checklist

Before a pull request is accepted, unit tests should satisfy:

```text
[ ] Test names clearly describe behavior.
[ ] Test follows Arrange-Act-Assert or Given-When-Then.
[ ] Test does not require external infrastructure.
[ ] Test does not start Spring context unless explicitly classified as integration test.
[ ] Test uses deterministic data.
[ ] Test has meaningful assertions.
[ ] Test validates both success and failure paths.
[ ] Test covers boundary cases for critical logic.
[ ] Test verifies important side effects.
[ ] Test verifies no side effects when validation fails.
[ ] Test does not over-mock value objects.
[ ] Test does not assert implementation details that may change without behavior change.
[ ] Test aligns with API, Kafka, Redis, and Database contracts where relevant.
```

---

## 24. Definition of Done for Unit Tests

A feature is not considered complete until:

1. All new business logic has unit tests.
2. All changed business logic has updated unit tests.
3. All critical branches are covered.
4. All validation rules have success and failure tests.
5. All risk rules affected by the change are tested.
6. All mapper behavior with masking or transformation is tested.
7. All event builder changes are tested.
8. All Redis key or TTL logic changes are tested.
9. All error handling changes are tested.
10. All unit tests pass locally.
11. Unit tests pass in CI.
12. Coverage thresholds are met.
13. No flaky unit tests are present.
14. Tests are readable and maintainable.

---

## 25. Unit Test Traceability Matrix

| Source Document Area | Unit Test Requirement |
|---|---|
| PRD - Transaction Submission | Validate transaction submission business rules, idempotency decision, and initial status creation logic. |
| PRD - Risk Scoring Engine | Validate rule trigger behavior, score aggregation, and decision threshold mapping. |
| PRD - Event-Based Processing | Validate event builders, envelope validators, duplicate event decision logic, and consumer delegation logic. |
| PRD - Redis Cache and Counters | Validate cache key building, cache-aside decision logic, invalidation key selection, and velocity threshold decisions. |
| PRD - Elasticsearch Audit Search | Validate audit document builder, masking logic, document ID selection, and search request mapping. |
| PRD - Investigation API | Validate response mappers, risk factor mapping, account masking, and pagination support logic. |
| PRD - Native SQL Reporting | Validate report parameter validation and native query result mapping. |
| API Contract | Validate request validators, error builders, pagination builders, sorting validators, and role helper logic. |
| Database Schema | Validate code-level rules that mirror database constraints and allowed values. |
| Kafka Event Contract | Validate event envelope, event version, event type, message key, and payload builder logic. |
| Redis Key Contract | Validate Redis key naming, TTL resolver, cache-aside decisions, invalidation mapping, and counter key logic. |
| Test Scenarios | Ensure unit tests support lower-level validation for broader integration and end-to-end scenarios. |
| Demo Scenarios | Ensure demo-critical flows have unit-tested business logic before being demonstrated. |

---

## 26. Final Acceptance Criteria

The unit test implementation is acceptable when:

1. Unit tests can be executed without Docker or external services.
2. Unit tests validate meaningful business behavior.
3. Critical BankGuard decision logic is covered.
4. Risk scoring behavior is tested at rule and orchestration level.
5. API support logic is tested according to contract.
6. Kafka event creation and validation logic is tested without requiring Kafka.
7. Redis key and cache decision logic is tested without requiring Redis.
8. Elasticsearch document building and search mapping logic is tested without requiring Elasticsearch.
9. Database constraint-equivalent validation is covered at code level.
10. Unit tests are deterministic, readable, and maintainable.
11. Required coverage thresholds are satisfied.
12. All unit tests pass in local and CI environments.
