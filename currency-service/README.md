# Currency Service

**Currency conversion** service in the SAGA flow: receives the conversion request with amount and origin/destination currencies, fetches the rate (with **Redis** cache when configured), calculates the converted amount and publishes the result to the orchestrator.

---

## Role in the SAGA Flow

- **Position:** Invoked after compliance approval (orchestrator state `CONVERTING_CURRENCY`).
- **Input:** Command on topic `currency.convert` with transfer data (transferId, amount, origin currency, destination currency).
- **Output:** Publishes `currency.converted` with the converted amount (and rate used, if applicable) for the orchestrator to proceed to debit.
- **Cache:** Redis for exchange rate cache (configurable TTL), reducing external API calls and improving latency.

This service does not orchestrate; it only converts and notifies the result.

---

## Responsibilities

- **Convert amount:** Calculate value in destination currency from exchange rate (Redis cache or default value).
- **Consume command:** Listen to topic `currency.convert` and extract amount and currencies from payload.
- **Publish result:** Send `currency.converted` with converted amount and data needed for next steps (debit/credit).
- **Rate cache:** Redis configuration to store rates with TTL; fallback to default rate when Redis unavailable or key missing.
- **Extensibility:** Ready for integration with external exchange API and Circuit Breaker (e.g. `circuit-breaker` module or Resilience4j).

---

## Kafka Topics

| Direction | Topic                 | Use                                                |
|-----------|------------------------|----------------------------------------------------|
| Consumes  | `currency.convert`     | Command to convert amount between currencies.     |
| Produces  | `currency.converted`   | Conversion result for the orchestrator.          |

---

## Stack and Dependencies

- **Java 21**, **Spring Boot 3**, **Spring Data Redis**, **Spring Kafka**
- **Redis:** exchange rate cache
- **Apache Kafka:** communication with the orchestrator

Main class: `com.saga.currency.CurrencyServiceApplication`.

---

## Build and Run

```bash
mvn clean install
cd currency-service
mvn spring-boot:run
```

**Default port:** 8086. Requires Redis and Kafka.
