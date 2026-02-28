# Detailed Technical Architecture

## Architecture Overview

This document details the technical architecture of the International Bank Transfer system using Orchestrated SAGA.

## Recommended Technology Stack

### Option 1: Java/Spring Boot (Recommended)
- **Framework**: Spring Boot 3.x
- **SAGA**: Custom implementation or Axon Framework
- **Kafka**: Spring Kafka
- **Circuit Breaker**: Resilience4j
- **Database**: Spring Data JPA (PostgreSQL), Spring Data MongoDB
- **API**: Spring WebFlux (Reactive)
- **Service Discovery**: Spring Cloud Consul
- **API Gateway**: Spring Cloud Gateway

### Option 2: Node.js/TypeScript
- **Framework**: NestJS
- **SAGA**: Custom implementation
- **Kafka**: kafkajs
- **Circuit Breaker**: opossum
- **Database**: TypeORM (PostgreSQL), Mongoose (MongoDB)
- **API**: Express/Fastify
- **Service Discovery**: Consul
- **API Gateway**: Kong

## Microservices Structure

```
saga_project/
├── saga-orchestrator/          # Central orchestrator
├── account-service/            # Account service
├── validation-service/         # Validation service
├── currency-service/           # Currency conversion service
├── transaction-service/        # Transaction service
├── notification-service/      # Notification service
├── audit-service/              # Audit service
├── api-gateway/                # API gateway
├── shared/                     # Shared libraries
│   ├── saga-common/            # Contracts and DTOs
│   ├── kafka-common/           # Kafka configuration
│   └── circuit-breaker/        # Circuit breaker config
└── infrastructure/             # Infrastructure
    ├── docker-compose.yml      # Kafka, PostgreSQL, MongoDB, Redis (already configured)
    └── kafka-topics/           # Topic creation scripts
```

**Infrastructure**: The project already has `docker-compose.yml` with PostgreSQL, MongoDB, Kafka (and Kafka UI), pgAdmin, Mongo Express and Redis. Start with `docker-compose up -d` for local development.

## Service Details

### 1. SAGA Orchestrator

**Responsibilities:**
- Manage saga state
- Orchestrate command sequence
- Run compensations
- Implement Circuit Breaker
- Persist saga state

**Technologies:**
- State Machine (Spring State Machine or custom)
- Kafka Producer/Consumer (Kafka in docker-compose)
- PostgreSQL (SQL) for saga state
- Resilience4j for Circuit Breaker

**Endpoints:**
- `POST /api/transfers` - Start transfer
- `GET /api/transfers/{sagaId}` - Query status
- `POST /api/transfers/{sagaId}/compensate` - Manual compensation

### 2. Account Service

**Responsibilities:**
- Validate accounts (origin and destination)
- Check balance
- Execute debits and credits
- Manage balance locks

**Technologies:**
- Spring Data JPA
- PostgreSQL (transactional data: accounts, balances)
- Redis (optional limit cache)
- Kafka Consumer/Producer

**Endpoints:**
- `POST /api/accounts/validate` - Validate account
- `POST /api/accounts/debit` - Debit
- `POST /api/accounts/credit` - Credit
- `POST /api/accounts/compensate` - Compensation

**Kafka Topics:**
- Consumes: `account.validate.origin`, `account.validate.destination`
- Produces: `account.validated`, `account.debited`, `account.credited`

### 3. Validation Service

**Responsibilities:**
- Compliance validation
- Limit checks (daily, monthly)
- AML (Anti-Money Laundering)
- Sanctions checks

**Technologies:**
- Spring Boot
- MongoDB (NoSQL) for compliance logs and events
- Integration with external APIs (sanctions list)

**Endpoints:**
- `POST /api/validation/compliance` - Validate compliance
- `GET /api/validation/limits/{accountId}` - Query limits

**Kafka Topics:**
- Consumes: `compliance.validate`
- Produces: `compliance.approved`, `compliance.rejected`

### 4. Currency Service

**Responsibilities:**
- Query exchange rates
- Convert amounts
- Reserve rate for limited time
- Cancel reservation on compensation

**Technologies:**
- Spring Boot
- Integration with exchange API (e.g. ExchangeRate API)
- Redis for exchange rate cache (TTL e.g. 5 min) and reservations
- Circuit Breaker for external API

**Endpoints:**
- `POST /api/currency/convert` - Convert currency
- `POST /api/currency/reserve` - Reserve rate
- `POST /api/currency/cancel-reservation` - Cancel reservation

**Kafka Topics:**
- Consumes: `currency.convert`
- Produces: `currency.converted`, `currency.reservation-cancelled`

### 5. Transaction Service

**Responsibilities:**
- Record transactions
- Execute debits and credits (coordination)
- Maintain transaction history

**Technologies:**
- Spring Data JPA
- PostgreSQL
- Event Sourcing (optional)

**Endpoints:**
- `POST /api/transactions/execute` - Execute transaction
- `GET /api/transactions/{id}` - Query transaction

### 6. Notification Service

**Responsibilities:**
- Send emails
- Send SMS
- Push notifications
- Retry on failure

**Technologies:**
- Spring Boot
- Integration with email/SMS services
- Circuit Breaker
- Dead Letter Queue

**Endpoints:**
- `POST /api/notifications/send` - Send notification

**Kafka Topics:**
- Consumes: `notification.send`
- Produces: `notification.sent`, `notification.failed`

### 7. Audit Service

**Responsibilities:**
- Record all events
- Store compliance logs
- Generate audit reports
- Data retention per regulation

**Technologies:**
- Spring Boot
- MongoDB (NoSQL) for audit logs and business events
- Kafka Consumer
- Operational logs sent to central tool (Loki or ELK)

**Endpoints:**
- `POST /api/audit/record` - Record event
- `GET /api/audit/reports` - Generate reports

**Kafka Topics:**
- Consumes: `audit.record`
- Produces: `audit.recorded`

## Kafka Configuration

### Required Topics

```bash
# Commands
transfer.commands (partitions: 6, replication: 3)
account.commands (partitions: 3, replication: 3)
compliance.commands (partitions: 3, replication: 3)
currency.commands (partitions: 3, replication: 3)
transaction.commands (partitions: 3, replication: 3)
notification.commands (partitions: 3, replication: 3)

# Events
transfer.events (partitions: 6, replication: 3)
account.events (partitions: 3, replication: 3)
compliance.events (partitions: 3, replication: 3)
currency.events (partitions: 3, replication: 3)
transaction.events (partitions: 3, replication: 3)
notification.events (partitions: 3, replication: 3)

# Dead Letter Queue
transfer.dlq (partitions: 3, replication: 3)
```

### Consumer Group Configuration

- `saga-orchestrator-group` - Orchestrator
- `account-service-group` - Account Service
- `validation-service-group` - Validation Service
- `currency-service-group` - Currency Service
- `transaction-service-group` - Transaction Service
- `notification-service-group` - Notification Service
- `audit-service-group` - Audit Service

## Circuit Breaker Configuration

### Resilience4j (Java)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      accountService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 60s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
      currencyService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
  retry:
    instances:
      accountService:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
      currencyService:
        maxAttempts: 3
        waitDuration: 500ms
  timelimiter:
    instances:
      accountService:
        timeoutDuration: 30s
      currencyService:
        timeoutDuration: 10s
```

## Database and Cache

### PostgreSQL (SQL) — Transactional Data and SAGA State

Use: saga state, accounts, balances, transactions. All operations requiring strong consistency and ACID transactions use PostgreSQL. Available in `docker-compose.yml` (port 5432, db `meubanco`).

**SAGA Schema:**
```sql
CREATE TABLE saga_instance (
    id VARCHAR(255) PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    current_state VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    payload JSONB,
    compensation_data JSONB
);

CREATE TABLE saga_step (
    id VARCHAR(255) PRIMARY KEY,
    saga_id VARCHAR(255) REFERENCES saga_instance(id),
    step_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    executed_at TIMESTAMP,
    compensated_at TIMESTAMP,
    error_message TEXT
);
```

### MongoDB (NoSQL) — Logs and Audit

Use: application logs, regulatory audit, business and compliance events. Asynchronous writes; suitable for high event volume and time-range queries. Available in `docker-compose.yml` (port 27017).

**Collections:**
- `audit_logs` - Audit logs
- `compliance_logs` - Compliance logs
- `transfer_events` - Transfer events
- `application_logs` - Application events (optional; can be only in logging tool)

### Redis — Cache

Available in `docker-compose.yml` (port 6379). Use in flow:

- **Currency Service**: exchange rates (TTL: 5 minutes), rate reservation
- **Account/Validation**: account limits (TTL: 1 hour) to reduce load on PostgreSQL
- Session state when needed

### Logging Tool (recommendation)

For operational logs (troubleshooting, log metrics, correlation ID across services), centralize in:

1. **Grafana Loki** (recommended): lightweight, Grafana integration, LogQL. Can be added to `docker-compose.yml` for local use.
2. **ELK Stack** (Elasticsearch, Logstash, Kibana): for full-text search and long operational log retention.

MongoDB remains the store for **audit and business events**; the logging tool is for **operational logs**.

## Security

### Authentication and Authorization

- **JWT Tokens**: Stateless authentication
- **OAuth2**: For external system integration
- **Role-Based Access Control (RBAC)**: Access control

### Encryption

- **Data in transit**: TLS 1.3
- **Data at rest**: AES-256
- **Sensitive**: Specific fields encrypted

### Rate Limiting

- API Gateway: 100 req/min per client
- Internal services: 1000 req/min

## Observability

### Metrics (Prometheus)

- Transfer success rate
- Latency per step
- Compensation rate
- Circuit breaker states
- Kafka message throughput

### Logs (Grafana Loki recommended or ELK Stack)

- Structured JSON logs sent to Loki or ELK
- Correlation ID (and sagaId) on all requests
- Levels: ERROR, WARN, INFO, DEBUG
- MongoDB for audit/business events; Loki/ELK for operational

### Tracing (Jaeger/Zipkin)

- Distributed tracing
- Span per SAGA step
- Response time per service

## Scalability

### Horizontal Scaling

- Each service can scale independently
- Kafka partitioning enables parallelism
- Load balancer distributes load

### Performance

- Aggressive cache (Redis)
- Connection pooling
- Async processing where possible
- Batch processing for audit

## Testing

### Test Strategy

1. **Unit Tests**: Business logic
2. **Integration Tests**: Kafka and DB integration
3. **Contract Tests**: Inter-service contracts
4. **End-to-End Tests**: Full flow
5. **Chaos Tests**: Resilience tests

### Test Scenarios

- Full happy path
- Failure at each step (compensation test)
- Service timeouts
- Circuit breaker activation
- Kafka downtime
- Database failure

## Deployment

### Containerization

- Docker per service
- Docker Compose for development
- Kubernetes for production

### CI/CD

- GitHub Actions / GitLab CI
- Automated build
- Automated tests
- Deploy to environments (dev, staging, prod)

## Monitoring and Alerts

### Critical Alerts

- Failure rate > 5%
- Latency > 30s
- Circuit breaker open > 5min
- Dead letter queue with messages
- Database connection errors

### Dashboards

- Grafana: Real-time metrics
- Kibana: Log analysis
- Jaeger UI: Request tracing
