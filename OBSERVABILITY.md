# Observability — Prometheus, Loki and Grafana

This document describes access, configuration and usage of the SAGA project observability tools, as in **ARCHITECTURE.md**.

## Overview

| Tool        | Port | Use |
|------------|------|-----|
| **Prometheus** | 9090 | Metrics (success rate, latency, circuit breaker, Kafka throughput) |
| **Loki**       | 3100 | Operational logs (JSON, correlation ID, sagaId) |
| **Grafana**    | 3000 | Dashboards and queries (Prometheus + Loki) |

**Rule:** MongoDB is used for **audit and business events**; **Loki** (or ELK) for **operational logs** (troubleshooting, cross-service correlation).

---

## 1. Access and Starting the Environment

### 1.1 Start the Observability Stack

```bash
# From project root
docker-compose up -d
```

This starts PostgreSQL, MongoDB, Redis, Kafka, pgAdmin, Mongo Express, Kafka UI, **Prometheus**, **Loki** and **Grafana**.

### 1.2 Access URLs

| Service     | URL | Credentials (local) |
|------------|-----|----------------------|
| **Grafana**   | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | — |
| **Loki** (API) | http://localhost:3100 | — |

On first login Grafana may ask to change the `admin` user password.

---

## 2. Configuration

### 2.1 Prometheus

- **File:** `infrastructure/prometheus/prometheus.yml`
- **Role:** Defines scrape jobs for each microservice at `host.docker.internal:8083` … `8089`.
- **Service requirement:** Expose metrics on Spring Boot Actuator:
  - `management.endpoints.web.exposure.include=prometheus,health,info`
  - `management.metrics.export.prometheus.enabled=true`

Example in a service `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2.2 Loki

- **File:** `infrastructure/loki/loki-config.yml`
- **Role:** Loki server for log ingestion and querying (storage in volume `loki_data`).
- **Sending logs:** Applications can send logs to Loki via:
  - **Promtail** (reading log files), or
  - **HTTP Push:** `POST http://localhost:3100/loki/api/v1/push` (Loki format).

Recommendation: use **logback** with an appender that sends structured JSON (including `correlationId` and `sagaId`) to an agent (Promtail) or to Loki’s push API.

### 2.3 Grafana

- **Datasources:** Provisioned in `infrastructure/grafana/datasources/datasources.yml`.
- **Prometheus:** Internal URL `http://prometheus:9090` (default).
- **Loki:** Internal URL `http://loki:3100`.

No manual datasource setup is required after first start.

---

## 3. Metrics (Prometheus) — ARCHITECTURE.md

Expected system metrics:

- Transfer success rate
- Latency per step (validation, compliance, conversion, debit, credit)
- Compensation rate
- Circuit breaker states (Resilience4j)
- Kafka message throughput

Spring Boot services expose default metrics (JVM, HTTP, Kafka consumer/producer) at `/actuator/prometheus`. Business metrics (e.g. completed transfers, compensations) should be implemented with **Micrometer** in the services and will appear in Prometheus.

---

## 4. Logs (Loki) — ARCHITECTURE.md

- **Format:** Structured logs in **JSON** sent to Loki.
- **Required fields:** **Correlation ID** and **sagaId** (or `transferId`) on all requests.
- **Levels:** ERROR, WARN, INFO, DEBUG.

Example log line (conceptual):

```json
{"timestamp":"2024-01-15T10:00:00Z","level":"INFO","service":"saga-orchestrator","correlationId":"abc-123","sagaId":"T-001","message":"Transfer started"}
```

Queries in Grafana (LogQL) use the Loki datasource.

---

## 5. Usage Examples

### 5.1 Basic — View Targets in Prometheus

1. Open http://localhost:9090.
2. **Status → Targets**.
3. Check jobs `saga-orchestrator`, `account-service`, etc. (for services running on the host).

### 5.2 Basic — Query Prometheus in Grafana

1. Log in at http://localhost:3000 (admin / admin).
2. **Explore** (compass icon) → **Prometheus** datasource.
3. Example query: `jvm_memory_used_bytes{job="saga-orchestrator"}`.
4. Run and view in real time.

### 5.3 Basic — Query Loki in Grafana

1. **Explore** → **Loki** datasource.
2. Simple query: `{job="saga-orchestrator"}` (after configuring job/labels in Promtail or push).
3. Or by level: `{job="saga-orchestrator"} |= "level=\"ERROR\""`.

### 5.4 Intermediate — HTTP Request Rate Dashboard

1. **Dashboards → New → Import**.
2. Use an existing dashboard (e.g. **Spring Boot 2.1 Statistics**, ID 10280) or create a new one.
3. Add panels with Prometheus queries, e.g.:
   - `rate(http_server_requests_seconds_count{job="saga-orchestrator"}[5m])`

### 5.5 Advanced — Log Correlation by sagaId in Loki

1. If logs have label or field `sagaId` (or `transferId`), use LogQL:
   - `{job=~"saga-orchestrator|account-service"} | json | sagaId="T-001"`
2. This lets you follow a single transfer flow across all services.

### 5.6 Advanced — Alerts in Prometheus/Grafana

1. In Prometheus, define rules in `prometheus.yml` (e.g. `rule_files` section) or use Grafana to create alerts.
2. Example: alert when `up{job="saga-orchestrator"} == 0` (service down).
3. Configure a **Contact point** in Grafana (email, Slack, etc.) for notifications.

---

## 6. Port Summary (observability)

| Port | Service     |
|------|-------------|
| 3000 | Grafana     |
| 3100 | Loki (API and ingestion) |
| 9090 | Prometheus  |

For more architecture details, see **ARCHITECTURE.md** (Observability, Database and Cache, and Logging tool sections).
