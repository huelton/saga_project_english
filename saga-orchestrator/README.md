# SAGA Orchestrator

Central **Orchestrated SAGA** orchestrator for international bank transfers. Responsible for starting the flow, coordinating each step in the defined order and updating saga state; on failure, consumes compensation events and persists final state.

---

## Role in the SAGA Flow

This service is the **entry point** and **coordinator** of the flow:

1. **Start:** Receives `POST /api/transfers`, generates a `transferId`, persists a saga instance in state `VALIDATING_ORIGIN` and sends the command to validate the origin account.
2. **Sequence:** On each received event (account validated, compliance approved, currency converted, debit done, credit done), advances state and triggers the next command until completion or failure.
3. **End:** Updates state to `COMPLETED` after credit to destination account, or to `FAILED` on failure/compensation events.
4. **Query:** Offers `GET /api/transfers/{transferId}/status` to track transfer status.

No other service orchestrates the flow; all react to commands and publish events.

---

## Responsibilities

- **Manage saga state:** Persist `SagaInstance` (and optionally `SagaStep`) in PostgreSQL, with states aligned to `SagaConstants`.
- **Publish commands:** Send messages to command topics (account validate origin, compliance validate, currency convert, transaction debit/credit) with `SagaEvent` payload (contract in `saga-common`).
- **Consume events:** Listen to event topics (account.validated, compliance.approved, currency.converted, transaction.debited, transaction.credited, transfer.failed, transfer.compensated) and delegate to `SagaOrchestratorService`.
- **REST API:** Expose endpoints to start transfer and query status; use shared DTOs (`TransferRequest`, `TransferResponse`) from `saga-common`.

---

## Kafka Topics

| Direction | Topic                     | Use                                                                 |
|-----------|---------------------------|---------------------------------------------------------------------|
| Produces  | `account.validate.origin` | Command to validate origin account (first step after start).        |
| Produces  | `compliance.validate`     | Command to validate compliance after account validated.            |
| Produces  | `currency.convert`        | Command for currency conversion after compliance approved.         |
| Produces  | `transaction.debit`       | Command for debit after conversion.                                |
| Produces  | `transaction.credit`     | Command for credit after debit.                                    |
| Consumes  | `account.validated`      | Advance to compliance validation.                                  |
| Consumes  | `compliance.approved`    | Advance to currency conversion.                                    |
| Consumes  | `currency.converted`     | Advance to debit.                                                   |
| Consumes  | `transaction.debited`    | Advance to credit.                                                 |
| Consumes  | `transaction.credited`  | Mark saga as completed.                                            |
| Consumes  | `transfer.failed`, `transfer.compensated` | Mark saga as failed/compensation.                    |

---

## API

| Method | Path                                | Description                                                                 |
|--------|-------------------------------------|-----------------------------------------------------------------------------|
| POST   | `/api/transfers`                    | Start a transfer. Body: `originAccountId`, `destinationAccountId`, `amount`, `currency`. Response 202 with identifier and status `STARTED`. |
| GET    | `/api/transfers/{transferId}/status`| Return current saga status (e.g. VALIDATING_ORIGIN, COMPLETED, FAILED). 404 if not found. |

---

## Stack and Dependencies

- **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **Spring Kafka**
- **PostgreSQL:** persistence for `SagaInstance` (and `SagaStep` if used)
- **Apache Kafka:** messaging with other microservices
- **saga-common:** DTOs `SagaEvent`, `TransferRequest`, `TransferResponse`

Main class: `com.saga.orchestrator.SagaOrchestratorApplication`.

---

## Build and Run

Build from the **repository root** to install the `saga-common` module:

```bash
# From project root
mvn clean install
cd saga-orchestrator
mvn spring-boot:run
```

**Default port:** 8083. Requires PostgreSQL and Kafka (e.g. `docker-compose up -d` at root).
