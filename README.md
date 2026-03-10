# International Bank Transfer System — Orchestrated SAGA

## Description

Complex financial system for processing international bank transfers using the **Orchestrated SAGA** pattern with **Apache Kafka** messaging, **Circuit Breaker**, and microservices architecture.

## Objective

Implement a robust system that ensures eventual consistency and automatic compensation in distributed financial operations, demonstrating:

- **Orchestrated SAGA**: Centralized coordination of distributed transactions
- **Asynchronous Messaging**: Apache Kafka for inter-service communication
- **Circuit Breaker**: Protection against cascading failures
- **Automatic Compensation**: Rollback of operations on failure
- **Observability**: Logs, metrics, and distributed tracing

## Documentation

- **[EXECUTIVE_SUMMARY.md](./EXECUTIVE_SUMMARY.md)**: Executive summary for quick presentation
- **[INDEX.md](./INDEX.md)**: Full documentation index
- **[DESIGN.md](./DESIGN.md)**: Complete architecture and design document
- **[SAGA_DIAGRAM.md](./SAGA_DIAGRAM.md)**: Detailed SAGA flow diagrams
- **[ARCHITECTURE.md](./ARCHITECTURE.md)**: Detailed technical architecture
- **[REQUIREMENTS.md](./REQUIREMENTS.md)**: Functional and non-functional requirements
- **[OBSERVABILITY.md](./OBSERVABILITY.md)**: Prometheus, Loki and Grafana — access, configuration and examples
- **[docs/JMETER.md](./docs/JMETER.md)**: JMeter installation and load test scenarios
- **[GITHUB_PUBLISHING.md](./GITHUB_PUBLISHING.md)**: Strategy for publishing and maintaining the repo on GitHub

## Architecture

### Main Components

1. **shared/** — Shared modules ([saga-common](./shared), kafka-common, circuit-breaker) — DTOs and Kafka/Resilience4j configuration
2. **SAGA Orchestrator**: Orchestrates all transfer steps
3. **Account Service**: Manages bank accounts and operations
4. **Validation Service**: Compliance and limit validations
5. **Currency Service**: Currency conversion
6. **Transaction Service**: Executes financial transactions
7. **Notification Service**: Sends notifications
8. **Audit Service**: Records audit logs

### Transfer Flow

1. Origin account validation
2. Destination account validation
3. Compliance validation (AML, limits)
4. Currency conversion
5. Debit from origin account
6. Credit to destination account
7. Notifications
8. Audit

### Compensation

If any step fails, the system runs automatic compensations in reverse order.

## Technologies

- **Backend**: Java/Spring Boot or Node.js/TypeScript
- **Messaging**: Apache Kafka (infra in `docker-compose.yml`)
- **SQL Database**: PostgreSQL — SAGA state, accounts, transactions
- **NoSQL Database**: MongoDB — logs and audit
- **Cache**: Redis — exchange rates, limits (infra in `docker-compose.yml`)
- **Circuit Breaker**: Resilience4j (Java) or opossum (Node.js)
- **API Gateway**: Spring Cloud Gateway
- **Logs**: Grafana Loki (recommended) or ELK Stack
- **Observability**: Prometheus, Grafana, Jaeger

## Kafka Topics

### Commands
- `transfer.initiate`
- `account.validate.origin`
- `account.validate.destination`
- `compliance.validate`
- `currency.convert`
- `transaction.debit`
- `transaction.credit`
- `notification.send`
- `audit.record`

### Events
- `transfer.started`
- `account.validated`
- `compliance.approved`
- `currency.converted`
- `transaction.debited`
- `transaction.credited`
- `transfer.completed`
- `transfer.failed`
- `transfer.compensated`

## SAGA States

1. PENDING
2. VALIDATING_ORIGIN
3. VALIDATING_DESTINATION
4. VALIDATING_COMPLIANCE
5. CONVERTING_CURRENCY
6. DEBITING
7. CREDITING
8. NOTIFYING
9. AUDITING
10. COMPLETED
11. COMPENSATING
12. FAILED

## Security

- JWT authentication
- Role-based authorization
- Encryption of sensitive data
- Rate limiting
- Input validation
- Audit logging

## Observability

- Performance metrics
- Structured logs with correlation ID
- Distributed tracing
- Health checks

## Infrastructure and Local Execution

The project includes `docker-compose.yml` with:

- **PostgreSQL** (5432) — SAGA state, accounts, transactions
- **MongoDB** (27017) — logs and audit
- **Redis** (6379) — cache
- **Apache Kafka** (9092 between containers; **29092** for host apps) + Kafka UI (8082)
- **pgAdmin** (8080) and **Mongo Express** (8081) for administration

Start with: `docker-compose up -d`.

**Run and validate the flow locally:** bring up infra with `docker-compose up -d` and then each microservice (order and request examples in **[RUNNING_LOCALLY.md](./RUNNING_LOCALLY.md)**).

### Projects (microservices)

| Project | Port | Description |
|---------|------|-------------|
| [saga-orchestrator](./saga-orchestrator) | 8083 | SAGA orchestrator, transfer API |
| [account-service](./account-service) | 8084 | Accounts, validation, debit/credit (PostgreSQL) |
| [validation-service](./validation-service) | 8085 | Compliance (MongoDB) |
| [currency-service](./currency-service) | 8086 | Currency conversion (Redis cache) |
| [transaction-service](./transaction-service) | 8087 | Transaction records, forward to account (PostgreSQL) |
| [notification-service](./notification-service) | 8088 | Notifications (simulated) |
| [audit-service](./audit-service) | 8089 | Audit (MongoDB) |
| **Observability** | | |
| Prometheus | 9090 | Metrics |
| Loki | 3100 | Operational logs |
| Grafana | 3000 | Dashboards (admin/admin) |

## Maven Build

The project has a **root POM** in the main folder and each microservice is a Maven module.

**Build all services (from repository root):**
```bash
# With Maven installed
mvn clean install

# With Maven Wrapper (Windows)
mvnw.cmd clean install

# With Maven Wrapper (Linux/macOS)
./mvnw clean install
```

**Build or run a single service:**
```bash
cd saga-orchestrator
mvn spring-boot:run
```

To generate the Maven Wrapper JAR (if Maven is not installed), run once at the root: `mvn -N wrapper:wrapper`.

## Next Steps

1. Start infra: `docker-compose up -d`
2. Implement project base structure
3. Configure Kafka topics and consumers
4. Implement SAGA Orchestrator (PostgreSQL)
5. Develop microservices (PostgreSQL/MongoDB/Redis)
6. Implement Circuit Breaker
7. Add logging tool (Loki or ELK)
8. Configure observability

## License

This is an educational/demonstration project.
