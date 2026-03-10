# Account Service

**Bank account** service in the SAGA flow: validates existence and eligibility of origin and destination accounts and publishes the result to the orchestrator. Also supports debit and credit operations when triggered by transaction commands (via Transaction Service in the current flow). Persistence in **PostgreSQL**.

---

## Role in the SAGA Flow

- **Position:** First service invoked by the orchestrator after transfer start (state `VALIDATING_ORIGIN`).
- **Input:** Command on topic `account.validate.origin` (and, in extended flows, `account.validate.destination`) with transfer data (transferId, accounts, amount, currency).
- **Output:** Publishes to topic `account.validated` for the orchestrator to proceed to compliance.
- **Data:** Maintains `Account` entity (identifier, balance, etc.); `DataInitializer` creates sample accounts (e.g. ACC-001, ACC-002) when the database is empty, for tests and demos.

This service does not decide flow order; it only processes the validation command and emits the corresponding event.

---

## Responsibilities

- **Validate accounts:** Check existence and conditions of origin and destination accounts per business rules.
- **Consume commands:** Listen to `account.validate.origin` (and optionally `account.validate.destination`) and process the message.
- **Publish result:** Send event `account.validated` with the same `transferId` and data needed for the next step.
- **Debit/credit support:** Ready for movement operations when integrated with transaction commands (transaction.debit / transaction.credit).
- **Persistence:** Spring Data JPA with account repository in PostgreSQL.

---

## Kafka Topics

| Direction | Topic                          | Use                                                |
|-----------|---------------------------------|----------------------------------------------------|
| Consumes  | `account.validate.origin`       | Command to validate origin account.               |
| Consumes  | `account.validate.destination`  | Command to validate destination account (if used).|
| Produces  | `account.validated`             | Validation complete event for the orchestrator.    |

---

## Stack and Dependencies

- **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **Spring Kafka**
- **PostgreSQL:** account storage
- **Apache Kafka:** communication with the orchestrator

Main class: `com.saga.account.AccountServiceApplication`.

---

## Build and Run

```bash
# From project root (to install shared dependencies if any)
mvn clean install
cd account-service
mvn spring-boot:run
```

**Default port:** 8084. Requires PostgreSQL and Kafka.
