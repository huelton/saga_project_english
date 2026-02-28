# SAGA Flow Diagram — International Bank Transfer

## Use of Databases and Cache in the Flow

- **PostgreSQL (SQL)**: Saga state, accounts, transactions (debit/credit). All transactional writes and consistency queries use PostgreSQL.
- **MongoDB (NoSQL)**: Application logs, audit and business events (including compliance). Asynchronous write after each relevant step.
- **Redis**: Exchange rate cache (Currency Service), account limits and sessions. Read/write in validation and conversion flow.
- **Kafka**: Messaging between orchestrator and services; infra available in project `docker-compose.yml`.

## Sequence Diagram — Main Flow

```
┌─────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│ Client  │    │ Orchestrator │    │   Account   │    │  Validation  │    │  Currency   │
│         │    │              │    │   Service   │    │   Service   │    │   Service   │
└────┬────┘    └──────┬───────┘    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
     │                │                    │                   │                   │
     │ 1. Initiate    │                    │                   │                   │
     │───────────────>│                    │                   │                   │
     │                │                    │                   │                   │
     │                │ 2. Validate Origin │                   │                   │
     │                │───────────────────>│                   │                   │
     │                │                    │                   │                   │
     │                │ 3. Origin Validated│                   │                   │
     │                │<───────────────────│                   │                   │
     │                │                    │                   │                   │
     │                │ 4. Validate Dest   │                   │                   │
     │                │───────────────────>│                   │                   │
     │                │                    │                   │                   │
     │                │ 5. Dest Validated  │                   │                   │
     │                │<───────────────────│                   │                   │
     │                │                    │                   │                   │
     │                │ 6. Validate Compliance                   │                   │
     │                │─────────────────────────────────────────>│                   │
     │                │                    │                   │                   │
     │                │ 7. Compliance OK   │                   │                   │
     │                │<─────────────────────────────────────────│                   │
     │                │                    │                   │                   │
     │                │ 8. Convert Currency│                   │                   │
     │                │───────────────────────────────────────────────────────────>│
     │                │                    │                   │                   │
     │                │ 9. Currency Converted                   │                   │
     │                │<───────────────────────────────────────────────────────────│
     │                │                    │                   │                   │
     │                │ 10. Debit Origin   │                   │                   │
     │                │───────────────────>│                   │                   │
     │                │                    │                   │                   │
     │                │ 11. Debit Confirmed│                   │                   │
     │                │<───────────────────│                   │                   │
     │                │                    │                   │                   │
     │                │ 12. Credit Dest   │                   │                   │
     │                │───────────────────>│                   │                   │
     │                │                    │                   │                   │
     │                │ 13. Credit Confirmed                    │                   │
     │                │<───────────────────│                   │                   │
     │                │                    │                   │                   │
     │                │ 14. Complete       │                   │                   │
     │                │                    │                   │                   │
     │ 15. Success    │                    │                   │                   │
     │<───────────────│                    │                   │                   │
```

## Sequence Diagram — Compensation Flow

```
┌──────────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────┐
│ Orchestrator │    │  Currency   │    │ Transaction │    │   Account    │
│              │    │   Service   │    │   Service   │    │   Service    │
└──────┬───────┘    └──────┬──────┘    └──────┬──────┘    └──────┬───────┘
       │                   │                   │                   │
       │ 1. Credit Failed  │                   │                   │
       │                   │                   │                   │
       │ 2. Compensate     │                   │                   │
       │    Credit         │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │ 3. Compensate     │                   │                   │
       │    Debit          │                   │                   │
       │──────────────────────────────────────>│                   │
       │                   │                   │                   │
       │ 4. Release        │                   │                   │
       │    Currency       │                   │                   │
       │──────────────────>│                   │                   │
       │                   │                   │                   │
       │ 5. Unlock Balance │                   │                   │
       │──────────────────────────────────────────────────────────>│
       │                   │                   │                   │
       │ 6. Transfer       │                   │                   │
       │    Failed         │                   │                   │
       │                   │                   │                   │
```

## SAGA State Diagram

```
                    ┌──────────┐
                    │  PENDING │
                    └─────┬────┘
                          │
                          ▼
              ┌───────────────────────┐
              │ VALIDATING_ORIGIN     │
              └───────┬───────────────┘
                      │
                      ▼
          ┌───────────────────────────┐
          │ VALIDATING_DESTINATION    │
          └───────┬───────────────────┘
                  │
                  ▼
      ┌───────────────────────────────┐
      │ VALIDATING_COMPLIANCE         │
      └───────┬───────────────────────┘
              │
              ▼
  ┌───────────────────────────────────┐
  │ CONVERTING_CURRENCY              │
  └───────┬──────────────────────────┘
          │
          ▼
  ┌───────────────────────────────────┐
  │ DEBITING                          │
  └───────┬──────────────────────────┘
          │
          ▼
  ┌───────────────────────────────────┐
  │ CREDITING                         │
  └───────┬──────────────────────────┘
          │
          ▼
  ┌───────────────────────────────────┐
  │ NOTIFYING                         │
  └───────┬──────────────────────────┘
          │
          ▼
  ┌───────────────────────────────────┐
  │ AUDITING                          │
  └───────┬───────────────────────────┘
          │
          ▼
      ┌──────────┐
      │COMPLETED │
      └──────────┘

  [On failure at any step]
          │
          ▼
  ┌───────────────────────────────────┐
  │ COMPENSATING                      │
  └───────┬──────────────────────────┘
          │
          ▼
      ┌────────┐
      │ FAILED │
      └────────┘
```

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway                              │
│                    (Spring Cloud Gateway)                        │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SAGA Orchestrator                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  State Machine                                            │  │
│  │  - Manages states                                         │  │
│  │  - Runs compensations                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Circuit Breaker (Resilience4j)                           │  │
│  │  - Failure protection                                     │  │
│  │  - Retry with backoff                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              │ Kafka Topics
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   Account     │    │  Validation   │    │   Currency    │
│   Service     │    │   Service     │    │   Service     │
│               │    │               │    │               │
│ - Validate    │    │ - Compliance  │    │ - Convert     │
│ - Debit       │    │ - Limits      │    │ - Reserve     │
│ - Credit      │    │ - AML         │    │               │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Transaction   │    │ Notification  │    │    Audit      │
│   Service     │    │   Service     │    │   Service     │
│               │    │               │    │               │
│ - Execute     │    │ - Email       │    │ - Log Events  │
│ - Record      │    │ - SMS         │    │ - Compliance  │
│               │    │ - Push        │    │               │
└───────────────┘    └───────────────┘    └───────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         Apache Kafka                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Topics:                                                 │  │
│  │  - transfer.commands                                     │  │
│  │  - transfer.events                                       │  │
│  │  - transfer.dlq                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Data and Infrastructure                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ PostgreSQL (SQL) │  │ MongoDB (NoSQL)  │  │ Redis        │  │
│  │ • SAGA state     │  │ • App logs        │  │ • Cache      │  │
│  │ • Accounts       │  │ • Audit           │  │ • Rates      │  │
│  │ • Transactions   │  │ • Business events │  │ • Limits     │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│  Centralized logs: Grafana Loki (recommended) or ELK Stack       │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow: PostgreSQL, MongoDB and Redis

```
Orchestrator                    Account Service              Currency Service
     │                                 │                            │
     │ 1. Start                        │                            │
     │── Persist state ───────────────>│ PostgreSQL (saga_instance)  │
     │   Log event ──────────────────────────────────────────────>│ MongoDB (audit_logs)
     │                                 │                            │
     │ 2. Account Validation           │                            │
     │                                 │<── Read account/balance ────│ PostgreSQL
     │                                 │<── Limit cache (optional)  │ Redis
     │                                 │                            │
     │ 3. Currency Conversion         │                            │
     │                                 │                            │<── Rate cache ── Redis
     │                                 │                            │── Write reservation ── Redis
     │                                 │                            │
     │ 4. Debit/Credit                 │                            │
     │                                 │── Transaction/lock ────────>│ PostgreSQL
     │                                 │                            │
     │ 5. Audit                        │                            │
     │── Audit event ──────────────────────────────────────────────>│ MongoDB (audit_logs)
     │                                 │                            │
  (Operational logs: send to Grafana Loki or ELK)
```

## Kafka Message Flow

```
┌──────────────┐
│ Orchestrator │
└──────┬───────┘
       │
       │ Publish: transfer.initiate
       ▼
┌─────────────────────────────────────┐
│     Kafka Topic: transfer.commands   │
└──────┬───────────────────────────────┘
       │
       │ Subscribe
       ▼
┌──────────────┐
│Account Service│
└──────┬───────┘
       │
       │ Publish: account.validated
       ▼
┌─────────────────────────────────────┐
│     Kafka Topic: transfer.events     │
└──────┬───────────────────────────────┘
       │
       │ Subscribe
       ▼
┌──────────────┐
│ Orchestrator │
└──────────────┘

[On failure after retries]
       │
       │ Publish: transfer.failed
       ▼
┌─────────────────────────────────────┐
│     Kafka Topic: transfer.dlq        │
│     (Dead Letter Queue)              │
└─────────────────────────────────────┘
```

## Circuit Breaker States

```
                    ┌──────────────┐
                    │     CLOSED   │
                    │  (Normal)    │
                    └──────┬───────┘
                           │
                    [Failures > Threshold]
                           │
                           ▼
                    ┌──────────────┐
                    │     OPEN     │
                    │  (Blocking)  │
                    └──────┬───────┘
                           │
                    [Timeout]
                           │
                           ▼
                    ┌──────────────┐
                    │  HALF-OPEN   │
                    │  (Testing)   │
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   [Success]          [Failure]          [Timeout]
        │                  │                  │
        ▼                  ▼                  ▼
   ┌────────┐         ┌────────┐         ┌────────┐
   │ CLOSED │         │  OPEN  │         │  OPEN  │
   └────────┘         └────────┘         └────────┘
```

## Message Payload Examples

### Command: Initiate Transfer
```json
{
  "sagaId": "saga-12345",
  "command": "transfer.initiate",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "originAccount": "BR123456789",
    "destinationAccount": "US987654321",
    "amount": 1000.00,
    "originCurrency": "BRL",
    "destinationCurrency": "USD",
    "clientId": "client-001"
  }
}
```

### Event: Transfer Completed
```json
{
  "sagaId": "saga-12345",
  "event": "transfer.completed",
  "timestamp": "2024-01-15T10:35:00Z",
  "data": {
    "transferId": "transfer-12345",
    "convertedAmount": 200.00,
    "exchangeRate": 5.0,
    "status": "COMPLETED"
  }
}
```

### Compensation: Compensate Debit
```json
{
  "sagaId": "saga-12345",
  "command": "transaction.compensate.debit",
  "timestamp": "2024-01-15T10:33:00Z",
  "data": {
    "accountId": "BR123456789",
    "amount": 1000.00,
    "reason": "Credit failed",
    "originalTransactionId": "txn-001"
  }
}
```
