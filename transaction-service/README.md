# Transaction Service

**Transaction record and orchestration** (debit and credit) service in the SAGA flow: persists each operation as `TransactionRecord` in **PostgreSQL** and notifies the orchestrator via Kafka. Ensures traceability of the transfer’s debit and credit steps.

---

## Role in the SAGA Flow

- **Position:** Invoked after currency conversion in two sequential steps: first **debit** on origin account (state `DEBITING`), then **credit** on destination account (state `CREDITING`).
- **Input:** Commands on topics `transaction.debit` and `transaction.credit`, with transfer data (transferId, accounts, converted amount, etc.).
- **Output:** Publishes `transaction.debited` after recording the debit and `transaction.credited` after recording the credit, so the orchestrator can advance to saga completion.
- **Persistence:** Each operation creates a `TransactionRecord` (transferId, debit/credit type, account, amount, timestamp) in PostgreSQL.

This service does not directly move account balances; it coordinates transaction recording and signaling to the orchestrator (actual balance movement is handled by Account Service when integrated with these commands).

---

## Responsibilities

- **Record debit:** Consume `transaction.debit`, persist the debit operation record and publish `transaction.debited`.
- **Record credit:** Consume `transaction.credit`, persist the credit operation record and publish `transaction.credited`.
- **Maintain history:** `TransactionRecord` entity and JPA repository for query and audit.
- **Internal orchestration:** Orchestration service that processes the message, persists and publishes the event to the correct topic.
- **Consistency:** Use of transactions and transfer identifier to correlate debit and credit of the same saga.

---

## Kafka Topics

| Direction | Topic                  | Use                                                |
|-----------|------------------------|----------------------------------------------------|
| Consumes  | `transaction.debit`    | Command to record and process debit.               |
| Consumes  | `transaction.credit`   | Command to record and process credit.              |
| Produces  | `transaction.debited`  | Debit complete event for the orchestrator.         |
| Produces  | `transaction.credited` | Credit complete event for the orchestrator.        |

---

## Stack and Dependencies

- **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **Spring Kafka**
- **PostgreSQL:** storage for `TransactionRecord`
- **Apache Kafka:** communication with the orchestrator

Main class: `com.saga.transaction.TransactionServiceApplication`.

---

## Build and Run

```bash
mvn clean install
cd transaction-service
mvn spring-boot:run
```

**Default port:** 8087. Requires PostgreSQL and Kafka.
