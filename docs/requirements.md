# Requirements

## Functional Requirements

| ID | Requirement | Implementation |
|---|---|---|
| FR-01 | Create Payment API | `POST /payments` — accepts amount, currency, method, customerId |
| FR-02 | Fetch Payment API | `GET /payments/{id}` — returns current status and metadata |
| FR-03 | Routing: CARD → Provider A | `CardRoutingStrategy` maps CARD to [PROVIDER_A, PROVIDER_B] |
| FR-04 | Routing: UPI → Provider B | `UpiRoutingStrategy` maps UPI to [PROVIDER_B, PROVIDER_A] |
| FR-05 | Retry on provider failure | `RetryableProviderExecutor` retries up to 3 times with exponential backoff |
| FR-06 | Failover to secondary provider | Chain of Responsibility: exhausted primary → try secondary |
| FR-07 | Idempotency | `Idempotency-Key` header; Redis-backed with 24h TTL |
| FR-08 | Payment status tracking | State machine: PENDING → PROCESSING → SUCCESS / FAILED |

## Non-Functional Requirements

| ID | Requirement | Implementation |
|---|---|---|
| NFR-01 | Extensible routing | Strategy pattern: new method = one class, zero code changes |
| NFR-02 | Extensible providers | Port + adapter: new provider = implement `PaymentProviderPort`, register in config |
| NFR-03 | Structured logging | Logback JSON encoder; every line carries traceId, spanId, idempotencyKey |
| NFR-04 | Distributed tracing | Micrometer Observation + Brave bridge → Zipkin spans per provider call |
| NFR-05 | Testability | Domain has zero framework deps; `InMemoryIdempotencyAdapter` for unit tests |
| NFR-06 | OWASP compliance | No stack traces in responses; validation at boundary; SQL via JPA parameterisation |
| NFR-07 | Idempotency correctness | Redis TTL ensures no stale keys; duplicate requests return identical response |
| NFR-08 | Performance | Zero-backoff test profile; retry backoff capped at 2s in production |

## Integration Points

| System | Direction | Purpose |
|---|---|---|
| PostgreSQL | Outbound | Persistent payment state |
| Redis | Outbound | Idempotency key/response cache |
| Zipkin | Outbound | Distributed trace collection |
| Provider A | Outbound | CARD payment processing (simulated) |
| Provider B | Outbound | UPI payment processing (simulated) |

## Input / Output Parameters

### POST /payments

**Input:**
| Field | Type | Validation |
|---|---|---|
| `Idempotency-Key` header | String | Required, non-blank |
| `amount` | BigDecimal | Required, > 0.00 |
| `currency` | String (ISO 4217) | Required, valid currency code |
| `method` | Enum: CARD / UPI | Required |
| `customerId` | String | Required, non-blank |

**Output (201):**
| Field | Type | Notes |
|---|---|---|
| `paymentId` | UUID string | Globally unique |
| `status` | String | PENDING / PROCESSING / SUCCESS / FAILED |
| `method` | String | CARD / UPI |
| `assignedProvider` | String | PROVIDER_A / PROVIDER_B / null |
| `amount` | BigDecimal | |
| `currency` | String | |
| `attemptCount` | Integer | Total attempts across all providers |
| `createdAt` | ISO-8601 timestamp | |
| `updatedAt` | ISO-8601 timestamp | |

### GET /payments/{id}

**Input:** `id` — UUID string path parameter  
**Output (200):** same shape as POST response
