# System Requirements

## Functional Requirements

### RF01 - Start International Transfer
**Description**: The system must allow a client to start an international bank transfer.

**Priority**: High

**Acceptance Criteria**:
- Client must provide: origin account, destination account, amount, currencies
- System must validate input data
- System must create SAGA instance
- System must return transfer ID

**Input**:
```json
{
  "originAccount": "BR123456789",
  "destinationAccount": "US987654321",
  "amount": 1000.00,
  "originCurrency": "BRL",
  "destinationCurrency": "USD",
  "clientId": "client-001"
}
```

**Output**:
```json
{
  "sagaId": "saga-12345",
  "transferId": "transfer-12345",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### RF02 - Validate Origin Account
**Description**: System must validate that the origin account exists, is active and has sufficient balance.

**Priority**: High

**Acceptance Criteria**:
- Check account existence
- Check account is active
- Check available balance
- Check daily/monthly limits
- Return error if any validation fails

### RF03 - Validate Destination Account
**Description**: System must validate that the destination account exists and is operational.

**Priority**: High

**Acceptance Criteria**:
- Check account existence
- Validate bank details (SWIFT, IBAN)
- Check destination bank is operational
- Return error if validation fails

### RF04 - Validate Compliance
**Description**: System must validate compliance and regulatory rules.

**Priority**: High

**Acceptance Criteria**:
- Check regulatory limits
- Run AML (Anti-Money Laundering) check
- Check international sanctions list
- Record suspicious attempts
- Block non-compliant transfers

### RF05 - Convert Currency
**Description**: System must convert amount from origin to destination currency.

**Priority**: High

**Acceptance Criteria**:
- Query current exchange rate
- Calculate converted amount
- Reserve rate for limited period (5 minutes)
- Allow reservation cancellation

### RF06 - Execute Debit
**Description**: System must debit the origin account.

**Priority**: High

**Acceptance Criteria**:
- Lock balance on origin account
- Record pending transaction
- Ensure idempotency
- Allow compensation on failure

### RF07 - Execute Credit
**Description**: System must credit the destination account.

**Priority**: High

**Acceptance Criteria**:
- Credit converted amount to destination account
- Confirm transaction
- Ensure idempotency
- Allow compensation on failure

### RF08 - Send Notifications
**Description**: System must send notifications to clients.

**Priority**: Medium

**Acceptance Criteria**:
- Send email to origin client
- Send email to destination client
- Send SMS (optional)
- Notify internal systems
- Retry on failure

### RF09 - Record Audit
**Description**: System must record all events for audit.

**Priority**: High

**Acceptance Criteria**:
- Record all transfer steps
- Store compliance logs
- Retain history per legal period (5 years)
- Support query and reports

### RF10 - Automatic Compensation
**Description**: System must run automatic compensation on failure.

**Priority**: High

**Acceptance Criteria**:
- Detect failure at any step
- Run compensations in reverse order
- Revert all completed operations
- Notify client of failure
- Record failure reason

### RF11 - Query Transfer Status
**Description**: System must allow querying the status of a transfer.

**Priority**: Medium

**Acceptance Criteria**:
- Return current SAGA state
- Return history of executed steps
- Return error details if any
- Support polling or webhook

### RF12 - Circuit Breaker
**Description**: System must implement Circuit Breaker for failure protection.

**Priority**: High

**Acceptance Criteria**:
- Open circuit after 5 consecutive failures
- Block requests when circuit open
- Attempt recovery after timeout
- Return fallback when circuit open
- Record circuit metrics

## Non-Functional Requirements

### RNF01 - Performance
- **Latency**: Full transfer in < 30 seconds (95th percentile)
- **Throughput**: Support 1000 transfers/minute
- **Response time**: API Gateway < 100ms
- **Async processing**: Non-blocking operations

### RNF02 - Availability
- **Uptime**: 99.9% (8.76 hours downtime/year)
- **Redundancy**: Multiple instances per service
- **Failover**: Automatic and transparent
- **Recovery**: Fast after failures

### RNF03 - Scalability
- **Horizontal**: Scale services independently
- **Elastic**: Auto-scaling based on load
- **Partitioning**: Kafka partitioned for parallelism
- **Cache**: Distributed cache to reduce load

### RNF04 - Reliability
- **Idempotency**: Idempotent operations
- **At-least-once delivery**: Kafka guarantees
- **Transactions**: Guaranteed compensation
- **Retry**: Automatic retry with backoff

### RNF05 - Security
- **Authentication**: JWT required
- **Authorization**: RBAC
- **Encryption**: TLS 1.3 in transit, AES-256 at rest
- **Validation**: Strict input validation
- **Rate Limiting**: Abuse protection
- **Logs**: No sensitive data in logs

### RNF06 - Observability
- **Metrics**: Prometheus with custom metrics
- **Logs**: Structured (JSON) with correlation ID
- **Tracing**: Distributed tracing (Jaeger)
- **Alerts**: Proactive alerts for issues
- **Dashboards**: Real-time dashboards

### RNF07 - Maintainability
- **Code**: Clean and documented code
- **Tests**: Coverage > 80%
- **Documentation**: Complete documentation
- **Versioning**: API versioning
- **CI/CD**: Automated pipeline

### RNF08 - Compliance
- **LGPD/GDPR**: Personal data protection
- **PCI-DSS**: Financial data security
- **Regulations**: Banking regulation compliance
- **Audit**: Auditable logs for 5 years
- **Reports**: Compliance reports

### RNF09 - Resilience
- **Circuit Breaker**: Protection against cascading failures
- **Timeout**: Configurable timeouts
- **Fallback**: Fallback strategies
- **Bulkhead**: Resource isolation
- **Chaos Engineering**: Resilience tests

### RNF10 - Usability
- **API**: Intuitive RESTful API
- **Documentation**: Swagger/OpenAPI
- **Errors**: Clear error messages
- **Versioning**: API versioning

## Use Cases

### CU01 - Successful Transfer
**Actor**: Client

**Main Flow**:
1. Client requests international transfer
2. System validates origin account
3. System validates destination account
4. System validates compliance
5. System converts currency
6. System debits origin account
7. System credits destination account
8. System sends notifications
9. System records audit
10. Client receives confirmation

**Result**: Transfer completed successfully

### CU02 - Transfer with Insufficient Balance
**Actor**: Client

**Main Flow**:
1. Client requests international transfer
2. System validates origin account
3. System detects insufficient balance
4. System cancels transfer
5. System notifies client of failure

**Result**: Transfer cancelled, client notified

### CU03 - Transfer with Credit Failure
**Actor**: Client

**Main Flow**:
1. Client requests international transfer
2. System runs all validations
3. System converts currency
4. System debits origin account
5. System attempts to credit destination account
6. System detects credit failure
7. System runs compensation:
   - Reverts debit on origin account
   - Cancels currency reservation
8. System notifies client of failure

**Result**: Transfer cancelled, debit reverted

### CU04 - Transfer Blocked by Compliance
**Actor**: Client

**Main Flow**:
1. Client requests international transfer
2. System validates origin account
3. System validates destination account
4. System validates compliance
5. System detects compliance violation (e.g. sanction)
6. System blocks transfer
7. System records suspicious attempt
8. System notifies client (generic message)
9. System notifies compliance team

**Result**: Transfer blocked, compliance notified

### CU05 - Status Query
**Actor**: Client

**Main Flow**:
1. Client queries transfer status
2. System fetches SAGA state
3. System returns current status and history

**Result**: Client sees transfer status

## Business Rules

### RN01 - Transfer Limits
- Daily limit per account: 50,000 (local currency)
- Monthly limit per account: 500,000 (local currency)
- Per-transaction limit: 10,000 (without additional approval)
- Transactions above 10,000 require manual approval

### RN02 - Exchange Rate
- Rate reserved for 5 minutes
- Rate from external API
- Rate cache for 1 minute
- 2% spread applied

### RN03 - Compensation
- Compensation must run within 1 minute of failure
- All operations must be reversible
- Compensation must be idempotent

### RN04 - Notifications
- Email required for both parties
- SMS optional (client must opt in)
- Notification retry: 3 attempts, 5-minute interval

### RN05 - Audit
- All events must be recorded
- Logs retained for 5 years
- Logs immutable after creation

### RN06 - Compliance
- AML check required for amounts > 3,000 (local currency)
- Sanctions check required
- Automatic block of suspicious transfers

## Infrastructure and Persistence

- **PostgreSQL (SQL)**: SAGA state, accounts, transactions. Used for transactional data and strong consistency.
- **MongoDB (NoSQL)**: Application logs, audit and business events. Retention per compliance policy.
- **Redis**: Exchange rate cache (Currency Service), account limits and sessions. TTL per business rules.
- **Kafka**: Messaging between orchestrator and microservices; infra in `docker-compose.yml`.
- **Centralized logs**: Grafana Loki (recommended) or ELK Stack for operational logs; MongoDB for audit/business events.

## Constraints

### RE01 - Technological
- Java 17+ or Node.js 18+
- Kafka 3.x (docker-compose)
- PostgreSQL 14+ (docker-compose)
- MongoDB 6+ (docker-compose)
- Redis 7+ (docker-compose)

### RE02 - Infrastructure
- Kubernetes for production
- Docker for containerization
- Cloud provider (AWS, Azure, GCP)

### RE03 - Regulatory
- LGPD/GDPR compliance
- Banking regulation compliance
- Data retention for 5 years

### RE04 - Operational
- Deploy only during maintenance windows
- Daily database backups
- 24/7 monitoring
