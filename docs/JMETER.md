# Load Testing and Scenarios with Apache JMeter

This document describes step-by-step JMeter installation and test scenarios for validating the SAGA system (transfers, compensation, resilience).

---

## 1. JMeter Installation (Step by Step)

### 1.1 Prerequisites

- **Java (JDK) 11 or higher** installed and `JAVA_HOME` set.
- Verify: `java -version` and `echo %JAVA_HOME%` (Windows) or `echo $JAVA_HOME` (Linux/macOS).

### 1.2 Download

1. Go to: https://jmeter.apache.org/download_jmeter.cgi
2. Download the **Binary** (`.tgz` or `.zip`), e.g.: `apache-jmeter-5.6.3.zip`
3. Extract to a folder, e.g.: `C:\Tools\apache-jmeter-5.6.3` (Windows) or `/opt/apache-jmeter-5.6.3` (Linux/macOS)

### 1.3 Environment Setup (Optional)

- **Windows:** Add the `bin` folder to PATH, e.g.:  
  `C:\Tools\apache-jmeter-5.6.3\bin`
- **Linux/macOS:**  
  `export PATH=$PATH:/opt/apache-jmeter-5.6.3/bin`

### 1.4 Run JMeter

- **GUI:**
  - Windows: `jmeter.bat` (inside `bin`)
  - Linux/macOS: `./jmeter` (inside `bin`)
- **CLI (run test plan):**
  - `jmeter -n -t path/to/plan.jmx -l result.csv -e -o report/`

### 1.5 Verification

When JMeter opens, the main window with the "Test Plan" tree should appear. Installation is correct.

---

## 2. Test Scenarios for Validation

As in **ARCHITECTURE.md** and **REQUIREMENTS.md**, the scenarios below validate the SAGA flow and resilience.

### 2.1 Scenario 1 — Happy path (full flow)

**Goal:** Validate a transfer that completes all steps successfully.

**Steps:**

1. **POST /api/transfers** (saga-orchestrator, port 8083) with JSON body:
   - `originAccountId`, `destinationAccountId`, `amount`, `currency`
2. Get the transfer identifier from the response (e.g. `transferId` or `id`).
3. **GET /api/transfers/{transferId}/status** in a loop (polling) until `status` = `COMPLETED` or timeout.

**Success criterion:** 202 response on POST and then status `COMPLETED` on GET within expected time.

### 2.2 Scenario 2 — Multiple transfers (load)

**Goal:** Measure throughput and latency under load.

**Steps:**

1. Thread Group with N threads (e.g. 10) and R ramp-up (e.g. 10 s).
2. Loop: POST /api/transfers with valid data (existing accounts, e.g. ACC-001, ACC-002).
3. Optional: GET status after each POST to confirm completion.

**Metrics to watch:** Requests per second, response time (avg, p95, p99), error rate.

### 2.3 Scenario 3 — Failure at one step (compensation)

**Goal:** Validate that a failure at one step (e.g. validation or debit) results in compensation and FAILED/COMPENSATING state.

**Steps:**

1. Configure a service to fail (e.g. mock or temporarily stop one microservice).
2. Send POST /api/transfers.
3. Query GET /api/transfers/{transferId}/status until final state (FAILED or COMPENSATING).

**Success criterion:** Orchestrator does not leave the saga in an inconsistent state; final state reflects failure/compensation.

### 2.4 Scenario 4 — Timeout and resilience

**Goal:** Validate behavior with slow or unavailable service.

**Steps:**

1. Increase delay in one service (e.g. validation or currency) or stop one service.
2. Send POST /api/transfers.
3. Verify configured timeouts and that compensation or circuit breaker is triggered per ARCHITECTURE.md.

### 2.5 Scenario 5 — Status query (GET)

**Goal:** Measure performance of the status endpoint.

**Steps:**

1. Thread Group with many threads (e.g. 50).
2. GET /api/transfers/{transferId}/status with one or several valid `transferId`s.
3. Collect latency and error rate.

---

## 3. Sample JMeter Test Plan

The project may include an example JMX file in `docs/jmeter/` (e.g. `saga-transfer-basic.jmx`) with:

- **Thread Group:** 5 users, 1 iteration each (or 10 users, 5 iterations).
- **HTTP Request (POST):**  
  - Server: localhost, Port: 8083, Path: `/api/transfers`  
  - Method: POST, Body: JSON with `originAccountId`, `destinationAccountId`, `amount`, `currency`
- **View Results Tree** and **Summary Report** (or **Backend Listener** for Grafana/Prometheus if desired).

To run in non-GUI mode:

```bash
jmeter -n -t docs/jmeter/saga-transfer-basic.jmx -l result.jtl -e -o report/
```

The HTML report is generated in the `report/` folder.

---

## 4. Validation Checklist

- [ ] JMeter installed and opens correctly.
- [ ] Scenario 1 (happy path) run successfully (202 + COMPLETED).
- [ ] Scenario 2 (load) run; metrics noted (throughput, latency).
- [ ] Scenario 3 (failure/compensation) run; final state consistent.
- [ ] Scenario 4 (timeout/resilience) run; system does not hang.
- [ ] Scenario 5 (GET status) run; latency within expected range.

For full functional and non-functional requirements, see **REQUIREMENTS.md** and **ARCHITECTURE.md**.
