# Design and Architecture — International Bank Transfer System

## Overview

This project implements an **International Bank Transfer** system using the **Orchestrated SAGA** pattern with **Apache Kafka** messaging, **Circuit Breaker**, and multiple microservices.

## Business Problem

An international bank transfer involves multiple operations that must run in a transactional way:
- Origin account validation
- Destination account validation
- Limit and compliance checks
- Currency conversion
- Debit from origin account
- Credit to destination account
- Notifications
- Audit

If any step fails, all previous operations must be compensated (rollback).

## Orchestrated SAGA Architecture

### Main Components

1. **Orchestrator (SAGA Orchestrator)**
   - Coordinates all transfer steps
   - Manages transaction state
   - Runs compensations on failure
   - Implements Circuit Breaker for resilience

2. **Microservices (Participants)**
   - **Account Service**: Manages bank accounts
   - **Validation Service**: Compliance and limit validations
   - **Currency Service**: Currency conversion
   - **Transaction Service**: Executes debits and credits
   - **Notification Service**: Sends notifications
   - **Audit Service**: Records audit

3. **Messaging (Kafka)**
   - Topics for commands and events
   - Delivery and order guarantees
   - Dead Letter Queue for failed messages
   - *Kafka infrastructure available via project `docker-compose.yml`*

4. **Databases**
   - **PostgreSQL (SQL)**: SAGA state, accounts, transactions and transactional data
   - **MongoDB (NoSQL)**: Application logs, audit and events for analysis

5. **Cache (Redis)**
   - Exchange rates (configurable TTL)
   - Account limits and sessions
   - Reduced load on services

6. **Circuit Breaker**
   - Protection against cascading failures
   - Fallback strategies
   - Timeout and retry policies

## International Transfer Flow

### Main Flow (Happy Path)

1. **Transfer start** — Client request, orchestrator creates saga instance; PostgreSQL: saga state; MongoDB: start log.
2. **Origin account validation** — PostgreSQL/Redis; check account, balance, limits.
3. **Destination account validation** — Existence, SWIFT/IBAN, destination bank operational.
4. **Compliance validation** — Regulatory limits, AML, sanctions.
5. **Currency conversion** — Redis cache for rate; external API if needed; reserve rate.
6. **Debit origin** — PostgreSQL: lock amount, record pending transaction.
7. **Credit destination** — Credit converted amount; confirm transaction.
8. **Debit confirmation** — Confirm debit, release lock.
9. **Notifications** — Notify origin/destination clients and internal systems.
10. **Audit** — MongoDB: record all steps; compliance and application events.

### Compensation Flow (Rollback)

If any step fails, the orchestrator runs compensations in reverse order:
- Credit failure → Revert debit
- Debit failure → Release balance lock
- Conversion failure → Cancel rate reservation
- Compliance failure → Release validations
- Validation failure → Notify client of error

## Technologies

- **Language**: Java/Spring Boot or Node.js/TypeScript
- **Messaging**: Apache Kafka (infra in project `docker-compose.yml`)
- **Circuit Breaker**: Resilience4j (Java) or opossum (Node.js)
- **SQL database**: PostgreSQL — SAGA state, accounts, transactions
- **NoSQL database**: MongoDB — application and audit logs
- **Cache**: Redis — exchange rates, limits, sessions
- **API Gateway**: Spring Cloud Gateway or Kong
- **Service Discovery**: Consul or Eureka
- **Observability**: Prometheus + Grafana; **Logs**: see recommended tool below

## Design Patterns

1. **Orchestrated SAGA**: Central orchestrator manages the flow
2. **Event Sourcing**: Events for traceability
3. **CQRS**: Read/write separation
4. **Circuit Breaker**: Failure protection
5. **Retry Pattern**: Automatic retries
6. **Dead Letter Queue**: Failed messages
7. **Saga State Machine**: State machine for SAGA

## Kafka Topics

### Commands
- `transfer.initiate` — Start transfer
- `account.validate.origin` — Validate origin account
- `account.validate.destination` — Validate destination account
- `compliance.validate` — Validate compliance
- `currency.convert` — Convert currency
- `transaction.debit` — Debit
- `transaction.credit` — Credit
- `notification.send` — Send notification
- `audit.record` — Record audit

### Events
- `transfer.started` — Transfer started
- `account.validated` — Account validated
- `compliance.approved` — Compliance approved
- `currency.converted` — Currency converted
- `transaction.debited` — Debit done
- `transaction.credited` — Credit done
- `transfer.completed` — Transfer complete
- `transfer.failed` — Transfer failed
- `transfer.compensated` — Compensation executed

### Dead Letter Queue
- `transfer.dlq` — Messages that failed after retries

## SAGA States

1. **PENDING** — Waiting to start
2. **VALIDATING_ORIGIN** — Validating origin account
3. **VALIDATING_DESTINATION** — Validating destination account
4. **VALIDATING_COMPLIANCE** — Validating compliance
5. **CONVERTING_CURRENCY** — Converting currency
6. **DEBITING** — Executing debit
7. **CREDITING** — Executing credit
8. **NOTIFYING** — Sending notifications
9. **AUDITING** — Recording audit
10. **COMPLETED** — Transfer complete
11. **COMPENSATING** — Running compensation
12. **FAILED** — Transfer failed

## Circuit Breaker Configuration

- **Failure threshold**: 5 consecutive failures
- **Timeout**: 30 seconds per call
- **Half-open interval**: 60 seconds
- **Retry**: 3 attempts with exponential backoff
- **Fallback**: Return structured error or use cache

## Security

- JWT authentication
- Role-based authorization
- Encryption of sensitive data
- Rate limiting
- Input validation
- Audit logging

## Observability

- **Metrics**: Success rate, latency, throughput (Prometheus + Grafana)
- **Logs**: Structured (JSON) with correlation ID; see *Logging tool* section below
- **Tracing**: Distributed tracing (Jaeger/Zipkin)
- **Health checks**: Health endpoints per service

### Logging Tool (recommendation)

Centralize logs with one of:

1. **Grafana Loki** (recommended for this project)
   - Lightweight, native Grafana integration
   - Structured logs (JSON), correlation ID, labels by service/sagaId
   - LogQL queries, alerts and dashboards with metrics
   - Can be added to `docker-compose.yml` for local use

2. **ELK Stack** (Elasticsearch, Logstash, Kibana)
   - Strong option for full-text search and advanced analysis
   - Suitable for long retention and heavy log compliance
   - Higher resource usage than Loki

Services should send structured logs (JSON) to the chosen tool; MongoDB remains the store for **audit and business events**, while the logging tool is for **operational and troubleshooting**.

## Scalability

- Horizontal scaling of microservices
- Kafka topic partitioning
- **Distributed cache (Redis)**: fewer hits to PostgreSQL and external APIs
- Load balancing

## Compliance Considerations

- LGPD/GDPR: Personal data protection
- PCI-DSS: Card data security
- Banking regulations
- Log retention per legal requirements
- Audit reports
