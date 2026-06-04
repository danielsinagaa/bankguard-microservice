# BankGuard

BankGuard is a real-time fraud detection and transaction intelligence platform.

## Modules

- `common-library`
- `transaction-service`
- `risk-engine-service`
- `audit-search-service`
- `master-data-service`

## Requirements

- Java 21
- Maven 3.6.3 or later
- Docker Desktop for local infrastructure in later phases

## Build

```bash
mvn clean validate
mvn test
```

## Documentation

The requirement and execution documents are in `docs/`:

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
- `11_TODO_LIST.md`
- `CODEX_TODO_LIST.md`
