# Shared Modules

As in **ARCHITECTURE.md**, this folder contains libraries reused by the SAGA project microservices.

## Modules

| Module | Description | Use |
|--------|-------------|-----|
| **saga-common** | Contracts and DTOs | `SagaEvent`, `TransferRequest`, `TransferResponse` — Kafka and API serialization |
| **kafka-common** | Kafka configuration | Base producer/consumer (bootstrap, serializers) — optional per service |
| **circuit-breaker** | Resilience4j | Circuit breaker and retry configuration — imported by services that call external APIs |

## Build

From the repository root:

```bash
mvn clean install
```

Shared modules are built before the services (module order in root POM).

## Service Dependency

Example in a service `pom.xml`:

```xml
<dependency>
    <groupId>com.saga</groupId>
    <artifactId>saga-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**saga-orchestrator** already uses `saga-common` for DTOs. Other services can consume events using `com.saga.common.dto.SagaEvent` when aligning the contract.
