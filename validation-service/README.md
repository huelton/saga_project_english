# Validation Service

**Compliance and regulatory validation** service in the SAGA flow: runs compliance checks (AML, limits, policies) for the transfer and records the result in **MongoDB**. Publishes approval or rejection via Kafka for the orchestrator to decide the next step.

---

## Role in the SAGA Flow

- **Position:** Invoked after account validation (orchestrator state `VALIDATING_COMPLIANCE`).
- **Input:** Command on topic `compliance.validate` with transfer data (transferId, accounts, amount, currency).
- **Output:** Publishes `compliance.approved` (and, on rejection, may publish `compliance.rejected`) for the orchestrator to advance or trigger compensation.
- **Persistence:** Compliance records as `ComplianceLog` documents in MongoDB, for audit and traceability.

This service does not orchestrate; it only evaluates the operation and emits the approval or rejection event.

---

## Responsibilities

- **Validate compliance:** Apply anti-money laundering (AML), limit (daily/monthly) and internal policy rules to the transfer.
- **Consume command:** Listen to topic `compliance.validate` and process the payload.
- **Record decision:** Persist in MongoDB (`ComplianceLog`) the validation result with transfer identifier and metadata.
- **Publish result:** Send `compliance.approved` or `compliance.rejected` to the orchestrator.
- **Future integration:** Ready for sanctions list or external compliance API integration if needed.

---

## Kafka Topics

| Direction | Topic                 | Use                                                |
|-----------|------------------------|----------------------------------------------------|
| Consumes  | `compliance.validate`  | Command to run compliance validation.              |
| Produces  | `compliance.approved`  | Approval for orchestrator to proceed.             |
| Produces  | `compliance.rejected`  | Rejection (triggers compensation in orchestrator if implemented). |

---

## Stack and Dependencies

- **Java 21**, **Spring Boot 3**, **Spring Data MongoDB**, **Spring Kafka**
- **MongoDB:** compliance log collection
- **Apache Kafka:** communication with the orchestrator

Main class: `com.saga.validation.ValidationServiceApplication`.

---

## Build and Run

```bash
mvn clean install
cd validation-service
mvn spring-boot:run
```

**Default port:** 8085. Requires MongoDB and Kafka.
