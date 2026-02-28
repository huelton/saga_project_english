# Executive Summary — International Bank Transfer System

## Project Objective

Build a complex financial system for **international bank transfers** using the **Orchestrated SAGA** pattern with **Apache Kafka** messaging and **Circuit Breaker**, demonstrating a robust, resilient microservices architecture.

## Why This Project?

### Justified Complexity
An international bank transfer involves multiple critical operations that must run in a transactional way:
- Origin and destination account validation
- Compliance and regulatory checks
- Currency conversion with volatile rates
- Debits and credits across different systems
- Notifications and audit

### Need for SAGA
- **Distributed transactions**: Operations across multiple services
- **Compensation required**: If any step fails, all previous steps must be rolled back
- **Eventual consistency**: Ensure consistency without distributed ACID transactions
- **Resilience**: System must keep working despite partial failures

## 3-Layer Architecture

```
┌─────────────────────────────────────────┐
│         API Gateway (Entry)            │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│    SAGA Orchestrator (Coordination)     │
│  • Manages full flow                    │
│  • Runs compensations                   │
│  • Circuit Breaker                      │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│      Microservices (Execution)         │
│  • Account Service                      │
│  • Validation Service                   │
│  • Currency Service                     │
│  • Transaction Service                  │
│  • Notification Service                 │
│  • Audit Service                        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│   Kafka + Data (Persistence/Cache)      │
│  • Apache Kafka (Messaging)             │
│  • PostgreSQL SQL (SAGA state, accounts,│
│    transactions)                       │
│  • MongoDB NoSQL (Logs, audit)          │
│  • Redis (Cache: rates, limits)         │
│  • Centralized logs: Loki or ELK        │
└─────────────────────────────────────────┘
```
**Infrastructure**: Kafka, PostgreSQL, MongoDB and Redis are configured in the project's `docker-compose.yml`.

## Simplified Flow

### Happy Path (Success)
1. Client requests transfer → Orchestrator creates SAGA
2. Validate origin account
3. Validate destination account
4. Validate compliance
5. Convert currency
6. Debit origin account
7. Credit destination account
8. Send notifications
9. Record audit
10. **Transfer complete**

### Compensation (Failure)
If **any** step fails:
- Orchestrator detects failure
- Runs compensations in reverse order
- Reverts all completed operations
- Notifies client of failure
- **System returns to initial state**

## Circuit Breaker

Protection against cascading failures:

```
CLOSED (Normal) → OPEN (Blocked) → HALF-OPEN (Testing) → CLOSED
     ↑                                                         │
     └─────────────────────────────────────────────────────────┘
```

- **CLOSED**: Requests pass normally
- **OPEN**: Service in trouble; requests blocked
- **HALF-OPEN**: Testing if service recovered

## Main Technologies

| Component | Technology |
|-----------|------------|
| **Backend** | Java/Spring Boot or Node.js/TypeScript |
| **Messaging** | Apache Kafka (docker-compose) |
| **SQL DB** | PostgreSQL — SAGA state, accounts, transactions |
| **NoSQL DB** | MongoDB — logs and audit |
| **Cache** | Redis — rates, limits, sessions (docker-compose) |
| **Circuit Breaker** | Resilience4j (Java) or opossum (Node.js) |
| **API Gateway** | Spring Cloud Gateway |
| **Logs** | Grafana Loki (recommended) or ELK Stack |
| **Observability** | Prometheus, Grafana, Jaeger |

## Architecture Benefits

### Reliability
- Automatic compensation ensures consistency
- Circuit Breaker prevents cascading failures
- Automatic retry with exponential backoff

### Scalability
- Each service scales independently
- Kafka enables parallel processing
- Cache reduces load on services

### Observability
- Real-time metrics
- Structured logs with correlation ID
- Distributed tracing for debugging

### Maintainability
- Decoupled services
- Easy to add new services
- Isolated tests per service

## Concepts Demonstrated

1. **Orchestrated SAGA**: Centralized coordination of distributed transactions
2. **Event-Driven Architecture**: Asynchronous communication via events
3. **Circuit Breaker Pattern**: Failure protection
4. **Compensating Transactions**: Rollback in distributed systems
5. **Idempotency**: Safe retry of operations
6. **Event Sourcing**: Full traceability
7. **CQRS**: Read/write separation
8. **Microservices**: Independent service architecture

## Documentation Created

1. **README.md** — Project overview
2. **DESIGN.md** — Full architecture and design
3. **SAGA_DIAGRAM.md** — Detailed diagrams
4. **ARCHITECTURE.md** — Implementation technical details
5. **REQUIREMENTS.md** — Functional and non-functional requirements
6. **INDEX.md** — Full documentation index
7. **diagrams/** — Interactive Mermaid diagrams

## Next Steps

1. Documentation created (DONE)
2. Infrastructure: `docker-compose up -d` (PostgreSQL, MongoDB, Kafka, Redis, UIs)
3. Choose technology stack (Java or Node.js)
4. Implement base structure
5. Configure Kafka topics and consumers
6. Implement SAGA Orchestrator (state in PostgreSQL)
7. Develop microservices (PostgreSQL/MongoDB/Redis per design)
8. Implement Circuit Breaker
9. Add logging tool (Loki or ELK)
10. Configure observability

## Real-World Use Cases

This pattern is used in:
- **Banks**: Transfers, payments
- **E-commerce**: Order processing
- **Reservations**: Hotels, flights, cars
- **SaaS**: Subscriptions, upgrades

## Project Highlights

- **Real complexity**: Financial scenario with multiple validations
- **Full SAGA**: Orchestrated with compensation
- **Resilience**: Circuit Breaker and retry
- **Observability**: Metrics, logs, tracing
- **Complete documentation**: Ready for implementation

---

**Status**: Documentation complete  
**Next step**: Code implementation
