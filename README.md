# BankGuard

BankGuard is a Spring Boot 4.0.6 microservice platform for real-time fraud detection and transaction intelligence. It accepts transaction submissions, scores risk asynchronously, stores audit/search data, and exposes operational reports for investigation.

## Architecture

The project is split into five Maven modules:

- `common-library` - shared DTOs, constants, security helpers, validation, and error handling.
- `transaction-service` - authentication, transaction APIs, idempotency, PostgreSQL persistence, Kafka publishing, and native SQL reporting.
- `risk-engine-service` - Kafka consumer, Redis-backed risk context and velocity counters, risk scoring, and risk result publishing.
- `audit-search-service` - Kafka consumer and Elasticsearch audit search API.
- `master-data-service` - blacklist, trusted device, customer risk profile, and risk rule management with Redis cache invalidation.

Local infrastructure is managed by Docker Compose with PostgreSQL, Kafka, Redis, Elasticsearch, and all application services.

## Requirements

- Java 21
- Maven 3.6.3 or later
- Docker Desktop
- PowerShell for the demo smoke script

## Build And Test

```bash
mvn test
mvn clean verify
```

`mvn test` runs unit tests. `mvn clean verify` also runs integration tests through Failsafe and Testcontainers, then produces JaCoCo reports under each module's `target/site/jacoco/` directory.

## Run Locally

Build the application jars first:

```bash
mvn clean package
```

Start the full local stack:

```bash
docker compose up -d --build
docker compose ps
```

Default host ports:

- Transaction Service: `http://localhost:18081`
- Risk Engine Service: `http://localhost:18082`
- Audit Search Service: `http://localhost:18083`
- Master Data Service: `http://localhost:18084`
- PostgreSQL: `localhost:5432`
- Kafka: `localhost:9092`
- Redis: `localhost:6379`
- Elasticsearch: `http://localhost:9200`

The services still run internally on ports `8081` to `8084`. Host ports can be overridden with `TRANSACTION_HOST_PORT`, `RISK_ENGINE_HOST_PORT`, `AUDIT_SEARCH_HOST_PORT`, and `MASTER_DATA_HOST_PORT`.

Stop the stack:

```bash
docker compose down
```

Reset local data volumes:

```bash
docker compose down -v
```

## Demo Smoke Flow

After the stack is healthy, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo-smoke.ps1
```

The script verifies service health, demo user login, low-risk approval, review and blocked risk decisions, Redis velocity counters, master data updates, Elasticsearch audit search, and all reporting endpoints.

## Documentation

- `docs/01_PRD.md`
- `docs/02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `docs/03_TRD_TECHNICAL_REQUIREMENTS.md`
- `docs/04_API_CONTRACT.md`
- `docs/05_DATABASE_SCHEMA.md`
- `docs/06_KAFKA_EVENT_CONTRACT.md`
- `docs/07_REDIS_KEY_CONTRACT.md`
- `docs/08_TEST_SCENARIOS.md`
- `docs/09_DEMO_SCENARIOS.md`
- `docs/10_UNIT_TEST_REQUIREMENTS.md`
- `docs/11_TODO_LIST.md`
