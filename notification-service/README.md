# Notification Service

**Notification** service in the SAGA flow: consumes send requests via Kafka (e.g. transfer confirmation to the client) and publishes send confirmation. In the current version, sending is **simulated** (no real email/SMS/push integration), allowing end-to-end flow validation.

---

## Role in the SAGA Flow

- **Position:** Can be invoked by the orchestrator after transfer completion (state `COMPLETED`) or at points defined by business rules (e.g. “transfer started” or “transfer completed” notification).
- **Input:** Command on topic `notification.send` with transfer and recipient data (transferId, notification type, client data).
- **Output:** Publishes `notification.sent` to signal that the request was processed (and, on failure, may publish `notification.failed` for retry or DLQ).
- **Decoupling:** Does not block the main saga flow; the orchestrator can consider the saga complete once it receives `transaction.credited` and trigger notification asynchronously.

This service does not orchestrate; it only processes the notification command and emits the corresponding event.

---

## Responsibilities

- **Process notification requests:** Consume messages from topic `notification.send` and run send logic (simulated or real).
- **Publish confirmation:** Send `notification.sent` with the same `transferId` to the orchestrator or other consumers.
- **Simulation:** Current implementation simulates sending (log or in-memory operation) for development and tests without external dependencies.
- **Extensibility:** Ready for integration with email, SMS or push providers and for Circuit Breaker and retry on temporary failure.

---

## Kafka Topics

| Direction | Topic                 | Use                                          |
|-----------|------------------------|----------------------------------------------|
| Consumes  | `notification.send`    | Command to send notification.                |
| Produces  | `notification.sent`    | Confirmation that send was processed.       |
| Produces  | `notification.failed`  | Send failure (if implemented for retry/DLQ). |

---

## Stack and Dependencies

- **Java 21**, **Spring Boot 3**, **Spring Kafka**
- **Apache Kafka:** communication with the orchestrator (no dedicated database in current version)

Main class: `com.saga.notification.NotificationServiceApplication`.

---

## Build and Run

```bash
mvn clean install
cd notification-service
mvn spring-boot:run
```

**Default port:** 8088. Requires Kafka.
