# Product Requirements Document
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Product Overview

### 1.1 Product Name

**BankGuard**  
Real-Time Fraud Detection and Transaction Intelligence Platform

### 1.2 Product Summary

BankGuard is a transaction risk monitoring platform designed to detect suspicious banking transactions, calculate risk scores, generate audit trails, and provide searchable investigation data for fraud operations teams.

The platform processes transaction events in near real time, evaluates them against configurable fraud indicators, stores risk decisions, and provides investigation APIs for reviewing suspicious activities.

BankGuard is designed to support transaction monitoring use cases such as high-value transactions, unusual transaction velocity, blacklisted destination accounts, new device usage, unusual locations, and customer risk profile changes.

---

## 2. Background

Modern banking systems process a high volume of transactions across multiple channels such as mobile banking, internet banking, ATM, branch systems, and back-office operations.

As transaction volume increases, fraud detection becomes more difficult when checks rely only on manual review or post-transaction investigation. Delayed detection can increase financial loss, operational burden, customer complaints, and reputational risk.

A dedicated fraud detection and transaction intelligence platform is needed to help identify risky transactions earlier, provide structured risk decisions, and support faster investigation through searchable audit data.

---

## 3. Business Problems

### 3.1 Fraud May Be Detected Too Late

Fraudulent or suspicious transactions may only be identified after they have already been processed.

**Impact:**

- Potential financial loss.
- Increased recovery effort.
- Higher customer dispute volume.
- Reputational risk for the institution.

### 3.2 Transaction Data Is Difficult to Investigate

Transaction-related information may be spread across multiple data sources, making investigation slower and less consistent.

**Impact:**

- Fraud analysts need more time to collect information.
- Investigation quality may vary between cases.
- Historical pattern analysis becomes harder.

### 3.3 Analytical Queries Can Overload Transaction Databases

Operational databases are optimized for transaction processing, not flexible fraud investigation or full-text search.

**Impact:**

- Slow investigation queries.
- Increased load on the primary transaction database.
- Lower reliability during high-volume periods.

### 3.4 Repeated Risk Data Lookup Can Create Database Bottlenecks

Risk scoring often requires repeated lookups to the same reference data, such as blacklisted accounts, trusted devices, and customer risk profiles.

**Impact:**

- Increased database load.
- Slower risk scoring process.
- Reduced ability to scale during transaction spikes.

### 3.5 Real-Time Event Processing Is Required

Batch-based or manually triggered fraud checks are not enough for fast-moving transaction scenarios.

**Impact:**

- Delayed fraud detection.
- Limited ability to react to rapid transaction patterns.
- Reduced support for near real-time monitoring.

---

## 4. Proposed Solution

BankGuard will provide an event-driven fraud detection workflow.

When a transaction is submitted:

1. The transaction is validated and stored.
2. A transaction event is published.
3. The risk engine consumes the event.
4. Fraud rules are evaluated.
5. A risk score and decision are generated.
6. Risk factors are stored for traceability.
7. Audit data is indexed for investigation.
8. Fraud analysts can search and review suspicious activity.

The platform separates transaction ingestion, risk evaluation, reference data management, and investigation search into clear functional areas. This separation improves maintainability, scalability, and operational clarity.

---

## 5. Goals

### 5.1 Functional Goals

- Accept transaction submissions through a secure API.
- Generate unique transaction references.
- Prevent duplicate transaction submissions using idempotency.
- Evaluate transactions using risk rules.
- Produce a risk score and decision for every transaction.
- Store risk factors for audit and explanation.
- Provide APIs for transaction lookup and fraud investigation.
- Provide reporting for high-risk transaction patterns.

### 5.2 Operational Goals

- Process transaction risk evaluation asynchronously.
- Reduce repeated database reads using cache.
- Provide searchable audit data without overloading the primary relational database.
- Support retry and dead-letter handling for failed event processing.
- Support containerized local deployment for all required components.

### 5.3 Data Goals

- Keep PostgreSQL as the source of truth for transaction and master data.
- Use Redis for frequently accessed risk reference data and short-lived real-time counters.
- Use Elasticsearch as a denormalized search model for investigation and audit search.
- Store risk factors in structured form for explainability.

---

## 6. Non-Goals

The following items are not part of the initial release:

- Real money movement between bank accounts.
- Integration with external banking core systems.
- Integration with payment gateways.
- Machine learning-based fraud scoring.
- Analyst dashboard UI.
- Multi-region production deployment.
- Customer notification through SMS, email, or push notification.
- Regulatory reporting integration.
- Automated case management workflow.

These items may be considered in future phases after the core transaction risk workflow is stable.

---

## 7. User Roles

### 7.1 Back Office User

Responsible for submitting or reviewing transaction-related data through internal tools.

**Primary Needs:**

- Submit transaction requests.
- View transaction status.
- Check risk decision results.

### 7.2 Fraud Analyst

Responsible for reviewing suspicious transactions and investigating fraud patterns.

**Primary Needs:**

- Search suspicious transactions.
- Filter transactions by risk score, status, amount, date, location, and customer.
- Review risk factors behind a transaction decision.

### 7.3 System Administrator

Responsible for managing risk reference data and system configuration.

**Primary Needs:**

- Manage blacklisted accounts.
- Manage customer risk profiles.
- Manage trusted devices.
- Trigger cache invalidation through data updates.

### 7.4 System Service

Internal system actor responsible for event processing between services.

**Primary Needs:**

- Publish transaction events.
- Consume risk scoring events.
- Store audit documents.
- Process retries and failed events.

---

## 8. MVP Scope

## MVP-01 Transaction Submission

### Description

The system must provide an API to submit transaction data. Each submitted transaction must be validated, assigned a transaction reference, stored with an initial status, and published as an event for risk scoring.

### Purpose

Transaction submission is the starting point of the fraud detection workflow. Without a reliable transaction ingestion process, downstream risk scoring, audit indexing, and investigation features cannot operate consistently.

### Included Capabilities

- Accept transaction request payload.
- Validate required transaction fields.
- Validate source account status.
- Generate unique transaction reference.
- Store transaction with `PENDING_RISK_CHECK` status.
- Require idempotency key to prevent duplicate submission.
- Publish `transaction.created` event after transaction creation.

### Expected Output

- Transaction record stored in PostgreSQL.
- Transaction reference returned to caller.
- Transaction event published to the event stream.

### Acceptance Criteria

- A valid transaction request creates exactly one transaction record.
- An invalid request returns a clear validation error.
- A duplicate request with the same idempotency key returns the existing transaction response.
- A transaction event is published only after the transaction record is successfully stored.

---

## MVP-02 Risk Scoring Engine

### Description

The system must evaluate each transaction using a set of fraud detection rules. Each triggered rule contributes to the total risk score and produces a risk factor explaining why the transaction was considered risky.

### Purpose

Risk scoring provides a structured decision-making process for identifying potentially suspicious transactions. It also improves auditability because every decision can be traced back to specific risk factors.

### Initial Risk Rules

- `HIGH_AMOUNT`: triggered when transaction amount exceeds a configured threshold.
- `NEW_DEVICE`: triggered when the transaction uses an untrusted device.
- `BLACKLISTED_DESTINATION`: triggered when the destination account is blacklisted.
- `HIGH_FREQUENCY_TRANSACTION`: triggered when the source account performs many transactions in a short period.
- `UNUSUAL_LOCATION`: triggered when the transaction location differs from the customer’s known locations.
- `HIGH_RISK_CUSTOMER_PROFILE`: triggered when the customer has an elevated risk profile.

### Risk Decision Threshold

- `APPROVED`: risk score below 50.
- `REVIEW`: risk score between 50 and 79.
- `BLOCKED`: risk score 80 or higher.

### Expected Output

- Total risk score.
- Risk decision.
- List of triggered risk factors.
- Updated transaction status.

### Acceptance Criteria

- Every consumed transaction event is evaluated by all active rules.
- The total risk score equals the sum of all triggered risk factors.
- The final decision follows the configured threshold.
- Risk factors are stored and can be retrieved from the transaction detail API.

---

## MVP-03 Event-Based Transaction Processing

### Description

The system must use event streaming to decouple transaction submission from risk scoring and audit indexing.

### Purpose

Transaction submission should not be blocked by risk scoring operations. Event-based processing allows the system to process transactions asynchronously and scale ingestion, scoring, and audit indexing independently.

### Included Capabilities

- Publish `transaction.created` event after transaction submission.
- Consume `transaction.created` event in the risk engine.
- Publish `transaction.risk-scored` event after scoring completion.
- Consume `transaction.risk-scored` event in the audit search service.
- Support retry for failed event processing.
- Move failed messages to a dead-letter topic after retry exhaustion.

### Expected Output

- Reliable asynchronous transaction processing.
- Clear event lifecycle from submission to audit indexing.
- Failed messages isolated for later investigation.

### Acceptance Criteria

- Event producer can publish transaction events.
- Risk engine can consume and process transaction events.
- Audit search service can consume scored transaction events.
- Failed event processing is retried according to the configured retry policy.
- Failed messages are moved to dead-letter topics after maximum retry attempts.

---

## MVP-04 Redis Risk Cache and Real-Time Risk Counters

### Description

The system must use Redis to cache frequently accessed risk reference data and maintain short-lived counters for real-time transaction velocity checks.

### Purpose

Risk scoring often reads the same data repeatedly, such as blacklisted accounts, customer risk profiles, trusted devices, and risk rule configuration. Redis reduces database load and improves scoring response time.

Redis is also used for short-lived transaction velocity counters so the system can detect high-frequency transaction patterns without relying only on database aggregation.

### Cached Data

- Blacklisted account lookup.
- Customer risk profile.
- Trusted device lookup.
- Risk rule configuration.

### Real-Time Counter Data

- Transaction count per source account within a short time window.
- Transaction amount accumulation per source account within a short time window.

### Expected Redis Key Examples

- `blacklist:account:{accountNumber}`
- `customer:risk-profile:{customerId}`
- `customer:trusted-device:{customerId}:{deviceId}`
- `risk-rule:config:{ruleCode}`
- `risk:velocity:count:{sourceAccountNumber}:10m`
- `risk:velocity:amount:{sourceAccountNumber}:10m`

### Acceptance Criteria

- Risk engine checks Redis before querying PostgreSQL for cacheable data.
- Cache miss falls back to PostgreSQL and repopulates Redis.
- Blacklist and trusted device updates invalidate related Redis keys.
- Velocity counters expire automatically according to the configured time window.
- High-frequency transaction rule can use Redis counters during scoring.

---

## MVP-05 Elasticsearch Audit Search

### Description

The system must index scored transaction data into Elasticsearch to support fast search and investigation.

### Purpose

Fraud investigation requires flexible search across customer information, account data, location, amount, risk factors, and decision status. Elasticsearch provides a denormalized read model optimized for investigation queries.

PostgreSQL remains the source of truth for transaction and risk data. Elasticsearch is used as a searchable projection of scored transaction data.

### Included Capabilities

- Consume scored transaction event.
- Build fraud audit document.
- Mask sensitive account data before indexing.
- Store document in Elasticsearch.
- Search audit documents by keyword and filters.

### Search Filters

- Transaction reference.
- Customer CIF.
- Customer name.
- Decision status.
- Minimum risk score.
- Risk factor.
- Date range.
- Channel.
- Location.

### Acceptance Criteria

- Every scored transaction creates or updates one audit document.
- Account numbers are masked in the search document.
- Search API returns matching audit documents.
- Filter by status, risk score, and date range works correctly.
- Audit search does not query the primary transaction tables directly.

---

## MVP-06 Transaction Investigation API

### Description

The system must provide APIs for fraud analysts to retrieve transaction details and search suspicious transaction activity.

### Purpose

Fraud analysts need a consistent way to investigate suspicious transactions and understand why a transaction received a specific risk decision.

### Included Capabilities

- Retrieve transaction detail by transaction reference.
- View risk score and decision.
- View risk factors.
- Search audit data through Elasticsearch.
- Filter investigation results by risk attributes.

### Acceptance Criteria

- Transaction detail API returns transaction status, risk score, decision, and risk factors.
- Investigation search API supports keyword and structured filters.
- Sensitive account data is masked in API responses.
- API returns clear response when transaction reference is not found.

---

## MVP-07 Native SQL Reporting

### Description

The system must provide reporting APIs based on native SQL queries for transaction risk analysis.

### Purpose

Some analytical reports require complex joins, aggregations, window functions, ranking, or time-based calculations. Native SQL is used where direct SQL provides better control, readability, and performance than generic ORM-generated queries.

### Reports Included

1. High-risk transactions.
2. Transaction velocity by account.
3. Top risk customers.
4. Suspicious destination accounts.
5. Daily fraud trend.
6. Risk score distribution.
7. Daily top risky customers using ranking.

### Acceptance Criteria

- Reporting endpoints return correct aggregated data.
- Queries support date range parameters.
- Queries use proper indexes where needed.
- At least one report uses CTE or window function.
- Report result structure is documented in the API contract.

---

## 9. Non-MVP Scope

## 9.1 Machine Learning Risk Scoring

### Description

Future versions may use machine learning models to improve risk prediction beyond rule-based scoring.

### Purpose

Machine learning can identify patterns that are difficult to represent using static rules.

### Reason for Exclusion from MVP

The initial version focuses on deterministic and explainable risk decisions. Rule-based scoring is easier to audit and validate during early implementation.

---

## 9.2 Real-Time Monitoring Dashboard

### Description

A dashboard may be added to visualize transaction volume, risk trends, blocked transactions, and fraud investigation queues.

### Purpose

The dashboard would help operations teams monitor risk activity in real time.

### Reason for Exclusion from MVP

The initial release focuses on backend APIs and core processing workflow. Dashboard development can be added after stable backend capabilities exist.

---

## 9.3 Anti-Money Laundering Pattern Detection

### Description

Future versions may add AML-specific scenarios such as structuring, rapid fund movement, circular transactions, and suspicious beneficiary patterns.

### Purpose

AML detection supports regulatory and compliance workflows.

### Reason for Exclusion from MVP

AML requires more complex domain rules, longer transaction history, and additional regulatory context. The MVP focuses on fraud risk detection.

---

## 9.4 Rule Management UI

### Description

A user interface may be added to allow authorized users to manage risk rules, thresholds, and rule activation.

### Purpose

This would reduce dependency on code changes for rule updates.

### Reason for Exclusion from MVP

The MVP can start with database-managed rule configuration and predefined rule implementations.

---

## 9.5 Case Management Workflow

### Description

Future versions may include case creation, assignment, analyst notes, escalation, and resolution status.

### Purpose

Case management helps fraud operations teams track investigation progress.

### Reason for Exclusion from MVP

The first release focuses on detection, scoring, audit indexing, and search. Case workflow can be built on top of investigation results.

---

## 9.6 Multi-Region Deployment

### Description

Future architecture may support multi-region deployment for high availability and disaster recovery.

### Purpose

Multi-region deployment improves service resilience and availability.

### Reason for Exclusion from MVP

The MVP targets a single deployment environment using containerized services.

---

## 9.7 Advanced Observability Dashboard

### Description

Future versions may include Grafana dashboards, distributed tracing, and advanced alerting.

### Purpose

Observability helps operations teams monitor service health, latency, event processing, and error rates.

### Reason for Exclusion from MVP

The MVP includes structured logging and basic operational visibility. Full observability dashboards can be added in later phases.

---

## 10. Definition of Done

## 10.1 Functional Completion

### Criteria

- Transaction submission API is available.
- Transaction detail API is available.
- Risk scoring workflow is completed.
- Audit search API is available.
- Master data APIs for blacklist and trusted device management are available.
- Reporting APIs are available.

### Completion Standard

This is considered complete when a transaction can be submitted, scored, stored, indexed, searched, and reported through documented APIs.

---

## 10.2 End-to-End Transaction Flow

### Criteria

- Transaction is created in PostgreSQL.
- `transaction.created` event is published.
- Risk engine consumes the event.
- Risk engine updates risk score and decision.
- `transaction.risk-scored` event is published.
- Audit search service indexes the result in Elasticsearch.

### Completion Standard

This is considered complete when one transaction can move from submission to searchable audit document without manual database modification.

---

## 10.3 Risk Scoring Accuracy

### Criteria

- All active rules are evaluated.
- Triggered rules generate risk factors.
- Total score matches the sum of risk factors.
- Decision matches the configured threshold.

### Completion Standard

This is considered complete when test scenarios for approved, review, and blocked decisions produce the expected result.

---

## 10.4 Event Processing Reliability

### Criteria

- Event producer publishes to the correct topic.
- Consumer processes events successfully.
- Retry is executed when processing fails.
- Dead-letter topic receives failed messages after retry exhaustion.

### Completion Standard

This is considered complete when both successful and failed event scenarios can be demonstrated.

---

## 10.5 Cache and Counter Behavior

### Criteria

- Cacheable data is read from Redis when available.
- Cache miss loads data from PostgreSQL.
- Cache invalidation works after master data changes.
- Velocity counters expire automatically.

### Completion Standard

This is considered complete when logs or tests can prove cache hit, cache miss, invalidation, and counter expiration behavior.

---

## 10.6 Search Capability

### Criteria

- Scored transactions are indexed in Elasticsearch.
- Search supports keyword query.
- Search supports structured filters.
- Sensitive data is masked.

### Completion Standard

This is considered complete when fraud audit documents can be searched by transaction reference, customer, decision, risk score, risk factor, and date range.

---

## 10.7 Reporting Capability

### Criteria

- Native SQL reporting endpoints return data.
- Reports support date range filters.
- Reports include aggregation and ranking where applicable.
- Query results match seeded test data.

### Completion Standard

This is considered complete when each report can be called through API and validated against known test scenarios.

---

## 10.8 Security

### Criteria

- APIs require authentication.
- Role-based authorization is enforced.
- Sensitive account information is masked.
- Invalid or missing token returns proper error response.

### Completion Standard

This is considered complete when protected endpoints reject unauthorized requests and allow valid role-based access.

---

## 10.9 Deployment

### Criteria

- Application services can be started with Docker Compose.
- PostgreSQL, Redis, Kafka, and Elasticsearch run in containers.
- Required environment variables are documented.
- Service health can be verified after startup.

### Completion Standard

This is considered complete when the full platform can be started from a clean environment and the main end-to-end transaction flow can be executed.

---

## 10.10 Testing

### Criteria

- Unit tests cover risk rules and validation logic.
- Integration tests cover database, Kafka, Redis, and Elasticsearch interactions.
- Security tests cover unauthorized and role-based access.
- End-to-end test covers transaction submission through audit indexing.

### Completion Standard

This is considered complete when the test suite runs successfully and covers the critical business flow.

---

## 11. Success Metrics

### Processing Metrics

- Transaction submission API responds successfully for valid requests.
- Risk scoring completes within acceptable processing time for normal transaction volume.
- Event consumer lag remains low during standard load.

### Reliability Metrics

- Failed event processing is captured in dead-letter topics.
- Duplicate transaction submission is prevented by idempotency key.
- Cache invalidation works after risk reference data changes.

### Investigation Metrics

- Fraud audit data is searchable after scoring completion.
- Search filters return correct results.
- Transaction detail explains the risk decision through risk factors.

---

## 12. Assumptions

- Transaction submission is simulated through API and does not move real funds.
- PostgreSQL is the source of truth for transaction and master data.
- Elasticsearch is used for search and investigation read model only.
- Redis is used for cache and short-lived real-time risk counters.
- Kafka is used as the primary event streaming mechanism between services.
- Initial fraud detection is rule-based and explainable.
- All services are deployed in a single containerized environment for the initial release.

---

## 13. Risks and Mitigations

### Risk 1: Event Processing Failure

**Impact:**

Risk scoring or audit indexing may be delayed.

**Mitigation:**

Use retry policy, dead-letter topic, and event processing logs.

---

### Risk 2: Cache Staleness

**Impact:**

Risk engine may use outdated blacklist or trusted device data.

**Mitigation:**

Use explicit cache invalidation after master data changes and apply short TTL for sensitive risk data.

---

### Risk 3: Search Data Not Synchronized

**Impact:**

Audit search may not immediately reflect latest transaction decision.

**Mitigation:**

Use event-based indexing and expose transaction detail API from the source of truth when exact latest state is required.

---

### Risk 4: Duplicate Transaction Submission

**Impact:**

The same transaction may be created more than once.

**Mitigation:**

Require idempotency key and enforce unique constraint at the database level.

---

### Risk 5: Reporting Query Performance

**Impact:**

Large date range reports may become slow.

**Mitigation:**

Use proper indexing, date range filtering, pagination, and query optimization.

---

## 14. Future Enhancements

- Machine learning scoring engine.
- Analyst case management workflow.
- Real-time operational dashboard.
- AML pattern detection.
- Rule configuration UI.
- Notification integration.
- Distributed tracing.
- Advanced monitoring and alerting.
- Multi-region deployment support.
