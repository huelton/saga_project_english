# Running and Validating the Flow Locally

## What the Architecture Allows

**Yes.** The architecture is designed so that **everything** (infrastructure + microservices) can run locally and the SAGA flow can be validated end to end.

---

## Current Situation

### What Already Runs Locally Today

**Infrastructure** is defined in `docker-compose.yml` and starts with one command:

| Component    | Port | Access / UI              | Use in flow                    |
|--------------|------|--------------------------|---------------------------------|
| **PostgreSQL**| 5432 | pgAdmin http://localhost:8080 | SAGA state, accounts, transactions |
| **MongoDB**   | 27017| Mongo Express http://localhost:8081 | Logs and audit              |
| **Redis**     | 6379 | CLI or Redis client     | Cache (rates, limits)           |
| **Kafka**     | 9092 (between containers) / **29092** (host apps) | Kafka UI http://localhost:8082 | Messaging between services   |

**Command:**

```bash
docker-compose up -d
```

Credentials (e.g. PostgreSQL: user `postgres`, password `postgres`, DB `meubanco`; MongoDB: `admin`/`admin123`) are in `docker-compose.yml`.

With this you can:
- Start the full local infra
- Create Kafka topics (Kafka UI or scripts)
- Connect applications (when they exist) to PostgreSQL, MongoDB, Redis and Kafka

### What Is Needed to Validate the Full Flow

The **application microservices** are implemented in the repository. To validate the end-to-end SAGA flow:

1. **Implement** (or generate PoC) for: SAGA Orchestrator, Account, Validation, Currency, Transaction, Notification, Audit (already done in this repo).
2. **Configure** each service to point to local resources (PostgreSQL: `localhost:5432`, MongoDB: `localhost:27017`, Redis: `localhost:6379`, Kafka: see note below).
3. **Run** the services (IDE, `mvn spring-boot:run`, or include them in Docker Compose).

When that is done, **yes, you can run everything locally and validate the flow** (happy path and compensation).

---

## Service Connection to Local Infra

When services run **on your machine** (outside Docker), use:

| Resource   | URL / connection string (host) |
|------------|---------------------------------|
| PostgreSQL | `jdbc:postgresql://localhost:5432/meubanco` (user: `postgres`, password: `postgres`) |
| MongoDB    | `mongodb://admin:admin123@localhost:27017` |
| Redis      | `localhost:6379` (no password in current compose) |
| Kafka      | `localhost:29092` (apps on host; inside Docker use `kafka:9092`) |

**Kafka**: The `docker-compose` exposes two listeners:
- **Inside Docker** (other containers): `kafka:9092`
- **On host** (apps outside Docker): `localhost:29092` — use `bootstrap.servers=localhost:29092` in applications.

---

## pgAdmin Configuration

1. Open pgAdmin at **http://localhost:8080**.
2. **pgAdmin login** (web UI):
   - **Email:** `admin@admin.com`
   - **Password:** `admin123`
3. After login, **add the PostgreSQL server**:
   - Right-click **Servers** → **Register** → **Server**.
   - **General tab:** *Name* e.g. `Postgres Local`.
   - **Connection tab:**
     - **Host name/address:** use **exactly** `postgres` (service name in `docker-compose`).
     - **Port:** `5432`
     - **Maintenance database:** `meubanco`
     - **Username:** `postgres`
     - **Password:** `postgres` (check *Save password* if desired)
   - Click **Save**.

**Important (pgAdmin in Docker):** Do not use `localhost` or `127.0.0.1` for Host. Inside the pgAdmin container, "localhost" is pgAdmin itself. The PostgreSQL server is in another container; on the Docker network it is named `postgres`. So Host must be **`postgres`**.

If pgAdmin is installed **on your machine** (outside Docker), then use Host `localhost`.

Then pgAdmin connects to database `meubanco` (user `postgres`). Tables for saga-orchestrator, account-service and transaction-service appear under **Servers** → **Postgres Local** → **Databases** → **meubanco** → **Schemas** → **public** → **Tables**.

---

## Recommended Order to Start Services

1. **Infrastructure**: `docker-compose up -d` (PostgreSQL, MongoDB, Redis, Kafka). If apps fail with *password authentication failed for user "postgres"*, see **Troubleshooting** below (recreate Postgres volume or use env vars).
2. **Services** (in any order, or in separate terminals):
   - `cd account-service && mvn spring-boot:run` (port 8084)
   - `cd validation-service && mvn spring-boot:run` (port 8085)
   - `cd currency-service && mvn spring-boot:run` (port 8086)
   - `cd transaction-service && mvn spring-boot:run` (port 8087)
   - `cd notification-service && mvn spring-boot:run` (port 8088)
   - `cd audit-service && mvn spring-boot:run` (port 8089)
   - `cd saga-orchestrator && mvn spring-boot:run` (port 8083)

3. **Start a transfer** (example):
   ```bash
   curl -X POST http://localhost:8083/api/transfers \
     -H "Content-Type: application/json" \
     -d "{\"originAccount\":\"BR123456789\",\"destinationAccount\":\"US987654321\",\"amount\":1000,\"originCurrency\":\"BRL\",\"destinationCurrency\":\"USD\",\"clientId\":\"client-001\"}"
   ```
4. **Check status** (use the returned `sagaId`):
   ```bash
   curl http://localhost:8083/api/transfers/{sagaId}
   ```

## Checklist to Validate the Flow Locally

- [ ] Infra up: `docker-compose up -d`
- [ ] Kafka topics created (automatically by saga-orchestrator on startup)
- [ ] Services running (account, validation, currency, transaction, notification, audit, saga-orchestrator)
- [ ] Happy path test: `POST /api/transfers` then `GET /api/transfers/{sagaId}` until status COMPLETED
- [ ] Compensation test: force failure (e.g. non-existent account) and check FAILED state and compensation

---

## Troubleshooting

### Error: "Connection refused" when connecting to PostgreSQL (pgAdmin in Docker)

If you see something like *connection to server at "127.0.0.1", port 5432 failed: Connection refused* when registering the server in pgAdmin, **Host** is set to `localhost` or `127.0.0.1`. With pgAdmin running in Docker, always use **Host = `postgres`** (service name in `docker-compose`). After changing to `postgres`, save and try again.

### Error: "FATAL: password authentication failed for user 'postgres'" (PostgreSQL)

This happens when the **PostgreSQL volume** was first created with different credentials. The `POSTGRES_USER` and `POSTGRES_PASSWORD` in `docker-compose` only apply on **first** initialization; if the volume already exists, the database keeps the old password.

**Recommended fix — run the reset script (at project root):**

```powershell
.\reset-postgres.ps1
```

The script runs `docker-compose down -v` (removes all volumes, including Postgres) and `docker-compose up -d`. Then **wait ~15 seconds** and start the applications again. The database will accept user `postgres` and password `postgres`.

**Manual alternative:**

```powershell
docker-compose down -v
docker-compose up -d
```

Wait ~15 seconds and start the applications (saga-orchestrator, account-service, transaction-service).

**If PostgreSQL is installed on Windows** and using port 5432, stop that service first (or use another host/port), or the Docker container will not bind to that port.

**Alternative (without recreating the volume):** If your Postgres still has old user/password (e.g. `admin`/`admin123`), pass them via environment variables when running the app:

- **PowerShell:**  
  `$env:SPRING_DATASOURCE_USERNAME="admin"; $env:SPRING_DATASOURCE_PASSWORD="admin123"; mvn spring-boot:run`
- **CMD:**  
  `set SPRING_DATASOURCE_USERNAME=admin && set SPRING_DATASOURCE_PASSWORD=admin123 && mvn spring-boot:run`

**Credentials after reset:** user `postgres`, password `postgres`, database `meubanco`. To use another user/password without changing code, set before starting services: `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` (Spring Boot uses these).

---

## Summary

| Question | Answer |
|----------|--------|
| Can the architecture run everything locally and validate the flow? | **Yes.** |
| Does the infra start locally today? | **Yes** — `docker-compose up -d`. |
| Can the full SAGA flow be validated today? | **Yes** — microservices are implemented (saga-orchestrator, account-service, validation-service, currency-service, transaction-service, notification-service, audit-service). |
| How to validate locally? | Start infra, then each service (see order above) and use `POST /api/transfers` and `GET /api/transfers/{sagaId}`. |
