# Documentation Index — SAGA Project

## Complete Documentation

### 1. [EXECUTIVE_SUMMARY.md](./EXECUTIVE_SUMMARY.md)
Executive summary for a quick project overview.

### 2. [README.md](./README.md)
Project overview, technologies, and next steps.

### 3. [DESIGN.md](./DESIGN.md)
Main design and architecture document:
- Business problem overview
- Orchestrated SAGA architecture
- Main components (Kafka, PostgreSQL, MongoDB, Redis)
- Transfer flow with SQL, NoSQL and cache usage
- Recommended logging tool (Grafana Loki or ELK)
- Kafka topics
- SAGA states
- Circuit Breaker configuration
- Security and observability

### 4. [SAGA_DIAGRAM.md](./SAGA_DIAGRAM.md)
Detailed text-format diagrams:
- Sequence diagram — Main flow
- Sequence diagram — Compensation flow
- SAGA state diagram
- Component architecture
- Kafka message flow
- Circuit Breaker states
- Payload examples

### 5. [ARCHITECTURE.md](./ARCHITECTURE.md)
Detailed technical architecture:
- Technology stack (Java/Spring Boot and Node.js/TypeScript)
- Microservices structure and docker-compose reference
- Per-service details (PostgreSQL, MongoDB, Redis)
- Kafka configuration
- Circuit Breaker configuration
- SQL (PostgreSQL) and NoSQL (MongoDB) databases, Redis (cache)
- Logging tool (Loki/ELK)
- Security, observability, scalability, testing, deployment

### 6. [REQUIREMENTS.md](./REQUIREMENTS.md)
Functional and non-functional requirements:
- Functional requirements (RF01–RF12)
- Non-functional requirements (RNF01–RNF10)
- Use cases (CU01–CU05)
- Business rules (RN01–RN06)
- Constraints (RE01–RE04)

### 7. [RUNNING_LOCALLY.md](./RUNNING_LOCALLY.md)
How to run the stack locally and what is needed to validate the end-to-end SAGA flow.

### 8. [GITHUB_PUBLISHING.md](./GITHUB_PUBLISHING.md)
Strategy for publishing and maintaining the repository on GitHub (English-only, commits, scheduler).

### 9. Mermaid Diagrams
Interactive Mermaid diagrams (viewable on GitHub, GitLab, etc.):

- [diagrams/saga-flow.mermaid](./diagrams/saga-flow.mermaid)
  - Full SAGA flow sequence diagram

- [diagrams/architecture.mermaid](./diagrams/architecture.mermaid)
  - Full system architecture

- [diagrams/saga-states.mermaid](./diagrams/saga-states.mermaid)
  - SAGA state machine

- [diagrams/circuit-breaker.mermaid](./diagrams/circuit-breaker.mermaid)
  - Circuit Breaker states

## Main Concepts

### Orchestrated SAGA
Pattern for managing distributed transactions with automatic compensation. The central orchestrator coordinates all steps.

### Transfer Flow
1. Origin account validation
2. Destination account validation
3. Compliance validation
4. Currency conversion
5. Debit from origin account
6. Credit to destination account
7. Notifications
8. Audit

### Compensation
On failure, the system runs compensations in reverse order to revert all operations.

### Circuit Breaker
Protection against cascading failures; states: CLOSED, OPEN, HALF-OPEN.

## System Components

1. **SAGA Orchestrator** — Orchestrates the full flow (state in PostgreSQL)
2. **Account Service** — Manages accounts (PostgreSQL, Redis cache)
3. **Validation Service** — Compliance validations (MongoDB for logs)
4. **Currency Service** — Currency conversion (Redis cache)
5. **Transaction Service** — Executes transactions (PostgreSQL)
6. **Notification Service** — Sends notifications
7. **Audit Service** — Records audit (MongoDB)

## Technologies and Infrastructure

- **Messaging**: Apache Kafka (docker-compose)
- **SQL database**: PostgreSQL — SAGA, accounts, transactions
- **NoSQL database**: MongoDB — logs and audit
- **Cache**: Redis (docker-compose)
- **Circuit Breaker**: Resilience4j (Java) or opossum (Node.js)
- **Logs**: Grafana Loki (recommended) or ELK Stack
- **Observability**: Prometheus, Grafana, Jaeger

## Next Steps

1. Review documentation
2. Choose technology stack (Java or Node.js)
3. Set up development environment
4. Implement project base structure
5. Configure Kafka and topics
6. Implement SAGA Orchestrator
7. Develop microservices
8. Implement Circuit Breaker
9. Add tests
10. Configure observability

## How to Use This Documentation

1. **Start with EXECUTIVE_SUMMARY.md** for a quick overview
2. **Read README.md** for the big picture
3. **Study DESIGN.md** to understand the architecture
4. **Use SAGA_DIAGRAM.md** to visualize flows
5. **See ARCHITECTURE.md** for technical details
6. **Check REQUIREMENTS.md** for full requirements
7. **View Mermaid diagrams** to understand flows

## Useful Links

- [SAGA Pattern](https://microservices.io/patterns/data/saga.html)
- [Apache Kafka](https://kafka.apache.org/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Resilience4j](https://resilience4j.readme.io/)
- [Spring State Machine](https://spring.io/projects/spring-statemachine)

## Notes

- This documentation is the basis for implementation
- Mermaid diagrams can be viewed in editors such as VS Code (with Mermaid extension)
- All diagrams are also in text form in SAGA_DIAGRAM.md
- Documentation can be extended as needed during implementation
