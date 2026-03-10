# Audit Service

**Audit** service in the SAGA flow: consumes transfer events sent by the orchestrator or other services, persists audit records in **MongoDB** and publishes confirmation via Kafka. Supports traceability, compliance and business event analysis.

---

## Role in the SAGA Flow

- **Position:** Can be invoked at one or more points in the flow (e.g. on transfer start, on completion, on failure). The orchestrator or services publish to `audit.record` for centralized recording.
- **Input:** Command on topic `audit.record` with event payload (transferId, step, state, relevant data, timestamp).
- **Output:** Publishes `audit.recorded` after persisting the record, so consumers know the event was audited.
- **Persistence:** `AuditLog` documents in MongoDB, with retention and queries aligned to audit policy (distinct from operational logs, which may go to Loki/ELK per ARCHITECTURE.md).

This service does not orchestrate; it only records events and emits confirmation.

---

## Responsibilities

- **Record events:** Consume topic `audit.record` and persist each event as an audit document in MongoDB.
- **Record structure:** Store transferId, event type, saga state, relevant payload and timestamp for queries and reports.
- **Publish confirmation:** Send `audit.recorded` with the transfer identifier (and optionally the record id) to the orchestrator or other systems.
- **Traceability:** Support for audit and regulatory compliance reports; MongoDB used only for audit and business events (operational logs in a dedicated tool).
- **Performance:** Asynchronous processing so it does not affect main saga flow latency.

---

## Kafka Topics

| Direction | Topic            | Use                                          |
|-----------|------------------|----------------------------------------------|
| Consumes  | `audit.record`   | Command to record an audit event.            |
| Produces  | `audit.recorded` | Confirmation that the record was persisted.   |

---

## Stack and Dependencies

- **Java 21**, **Spring Boot 3**, **Spring Data MongoDB**, **Spring Kafka**
- **MongoDB:** audit log collection (`AuditLog`)
- **Apache Kafka:** receiving events and publishing confirmation

Main class: `com.saga.audit.AuditServiceApplication`.

---

## Build and Run

```bash
mvn clean install
cd audit-service
mvn spring-boot:run
```

**Default port:** 8089. Requires MongoDB and Kafka.
