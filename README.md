# Payment Orchestration System

A production-grade payment orchestration backend built for the Yuno Backend Developer assessment.

## Architecture

Hexagonal (Ports & Adapters) architecture with three concentric zones:

```
INFRASTRUCTURE  (Spring, JPA, Redis, Micrometer/Zipkin)
  APPLICATION   (use cases, retry/failover orchestration)
    DOMAIN      (Payment aggregate, routing, ports — pure Java, zero framework)
```

Design patterns applied: Strategy (routing), Chain of Responsibility (failover), State (payment lifecycle), Command (use case inputs), Decorator (logging + observability), Registry (provider lookup), Domain Events, Factory Method, Value Objects.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker + Docker Compose

### Run dependencies

```bash
docker-compose up -d
```

This starts PostgreSQL 15, Redis 7, and Zipkin (trace UI at http://localhost:9411).

### Run the application

```bash
mvn spring-boot:run
```

Application starts on `http://localhost:8080`.

### Run tests

```bash
mvn test
```

Unit and controller tests run without Docker.
Persistence integration tests (Testcontainers) require Docker — they skip gracefully if Docker is unavailable.

## API

### Create a Payment

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "amount": 100.00,
    "currency": "USD",
    "method": "CARD",
    "customerId": "cust-123"
  }'
```

Response `201 Created`:
```json
{
  "paymentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "SUCCESS",
  "method": "CARD",
  "assignedProvider": "PROVIDER_A",
  "amount": 100.00,
  "currency": "USD",
  "attemptCount": 1,
  "createdAt": "2026-04-11T10:00:00Z",
  "updatedAt": "2026-04-11T10:00:00Z"
}
```

### Fetch a Payment

```bash
curl http://localhost:8080/payments/{paymentId}
```

Response `200 OK` — same shape as above.

### Error Responses

| HTTP Status | Error Code | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Invalid request body field |
| 400 | `MISSING_HEADER` | `Idempotency-Key` header absent |
| 400 | `BAD_REQUEST` | Malformed payment ID or currency |
| 404 | `PAYMENT_NOT_FOUND` | Unknown payment ID |
| 422 | `PAYMENT_FAILED` | All providers exhausted |
| 500 | `INTERNAL_ERROR` | Unexpected error |

## Routing

| Payment Method | Primary Provider | Failover Provider |
|---|---|---|
| CARD | PROVIDER_A | PROVIDER_B |
| UPI | PROVIDER_B | PROVIDER_A |

## Configuration

`application.yml` key settings:

```yaml
payment:
  retry:
    max-attempts-per-provider: 3      # attempts before failover
    initial-backoff-ms: 100           # first backoff delay
    backoff-multiplier: 2.0           # exponential multiplier
    max-backoff-ms: 2000              # cap on backoff

provider:
  a.failure-rate: 0.3                 # 0.0 = always succeeds, 1.0 = always fails
  b.failure-rate: 0.2

idempotency:
  ttl-hours: 24                       # idempotency key retention
```

## Observability

- **Structured JSON logs** — every log line includes `traceId`, `spanId`, `idempotencyKey`, `paymentId`
- **Zipkin traces** — open http://localhost:9411 to see full payment traces
- **Micrometer spans** — each provider charge attempt is a named span (`provider.charge`)

## Project Structure

```
src/main/java/com/yuno/payment/
├── domain/          # Pure Java — no framework imports
│   ├── model/       # Payment aggregate, value objects
│   ├── event/       # Domain events
│   ├── exception/   # Domain exceptions
│   ├── port/        # Inbound + outbound port interfaces
│   └── service/     # RoutingEngine, ProviderRegistry, routing strategies
├── application/     # Use cases, retry executor — depends only on domain
└── infrastructure/  # Spring wiring, JPA, Redis, HTTP adapters
    ├── config/      # Spring @Configuration classes
    ├── idempotency/ # Redis + in-memory adapters
    ├── observability/ # Decorators, trace filter, event logger
    ├── persistence/ # JPA entity, repository adapter
    ├── provider/    # Provider adapters + simulator
    └── web/         # Controller, DTOs, exception handler
```
