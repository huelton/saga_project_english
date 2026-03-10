# Testing — SAGA Project

## Overview

Each microservice has unit and integration tests with JUnit 5 and Spring Boot Test. **90%** line coverage is required, enforced by **JaCoCo** on `mvn verify`.

## Coverage (JaCoCo 90%)

- **Target:** 90% line coverage per module.
- **Command:** `mvn verify` (runs tests and generates report at `target/site/jacoco/index.html`).
- **Exclusions:** `Application`, `config`, `dto` packages (and where applicable `document`, `entity`) are excluded from the calculation to focus on business logic and integrations.

## Convention: Constants in Tests

Literal values in tests should be centralized in constants for clarity and maintenance:

- Each module has a **`TestConstants`** class in `src/test/.../constants/` with IDs, values and paths used in tests.
- Use production constants (e.g. `SagaConstants.STATE_PENDING`, `AccountConstants.TOPIC_*`) when the value matches production code.
- Examples: `TestConstants.TRANSFER_ID_1`, `TestConstants.ACCOUNT_ID_TEST`, `TestConstants.API_TRANSFERS`.

## Running Tests

### Per Microservice (Maven)

From each service root:

```bash
cd saga-orchestrator && mvn test
cd account-service && mvn test
cd validation-service && mvn test
cd currency-service && mvn test
cd transaction-service && mvn test
cd notification-service && mvn test
cd audit-service && mvn test
```

### Test Profile

Tests use the `test` profile and `application-test.yml` in each service, with:

- **JPA (PostgreSQL):** H2 in-memory for saga-orchestrator, account-service and transaction-service.
- **MongoDB:** Test URI for validation-service and audit-service.
- **Redis:** Local config for currency-service (can be mocked).
- **Kafka:** Local bootstrap servers (context tests may use mocks).

## Test Structure

- **Constants:** Tests for topic and constant values (KafkaConstants, SagaConstants, etc.).
- **Kafka:** Context load tests for consumers and producers (with Kafka mocked when needed).
- **Repository:** JPA/Mongo repository tests (DataJpaTest / embedded MongoDB where applicable).
- **Service:** Service tests with Kafka/other dependencies mocked.
- **Controller:** API tests with MockMvc and mocked services (e.g. TransferControllerTest).

## Notes

- Tests that depend on Kafka without an embedded broker may fail if Kafka is not available; use mocks or `@EmbeddedKafka` where needed.
- For full end-to-end integration, start infra with `docker-compose up -d` and run each service and its tests as documented in RUNNING_LOCALLY.md.
