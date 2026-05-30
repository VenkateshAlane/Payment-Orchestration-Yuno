# Test Case Documentation

## Classification

| Type | Description |
|---|---|
| **Sanity** | Happy-path: basic functionality works end-to-end |
| **Regression** | Edge cases, state transitions, guard rails |
| **Integration** | Real external systems (PostgreSQL via Testcontainers) |

---

## Domain Model Tests (`PaymentTest`)

### Sanity

| ID | Test | Expected |
|---|---|---|
| D-S-01 | `Payment.create()` starts in PENDING | `status == PENDING` |
| D-S-02 | `Payment.create()` generates non-null ID | `id != null` |
| D-S-03 | `Payment.create()` sets createdAt/updatedAt | timestamps not null |
| D-S-04 | PENDING → PROCESSING transition | `status == PROCESSING` |
| D-S-05 | PROCESSING → SUCCESS transition, records provider | `status == SUCCESS`, `assignedProvider` set |
| D-S-06 | PROCESSING → FAILED transition | `status == FAILED` |

### Regression

| ID | Test | Expected |
|---|---|---|
| D-R-01 | `Payment.create()` with null money | `IllegalArgumentException` |
| D-R-02 | `Payment.create()` with null method | `IllegalArgumentException` |
| D-R-03 | `markProcessing()` twice (PROCESSING → PROCESSING) | `IllegalStateTransitionException` |
| D-R-04 | `markSuccess()` from PENDING (skip PROCESSING) | `IllegalStateTransitionException` |
| D-R-05 | `markSuccess()` from SUCCESS (terminal state) | `IllegalStateTransitionException` |
| D-R-06 | `markFailed()` from PENDING | `IllegalStateTransitionException` |
| D-R-07 | `markFailed()` from FAILED (terminal state) | `IllegalStateTransitionException` |
| D-R-08 | `Money` with zero amount | `IllegalArgumentException` |
| D-R-09 | `Money` with negative amount | `IllegalArgumentException` |
| D-R-10 | `Money` with invalid ISO 4217 currency | `IllegalArgumentException` |
| D-R-11 | `Money` lowercase currency normalised to uppercase | `currency == "USD"` |
| D-R-12 | `IdempotencyKey` with blank string | `IllegalArgumentException` |
| D-R-13 | `IdempotencyKey` with null | `IllegalArgumentException` |
| D-R-14 | `PaymentId.of()` with malformed UUID | `IllegalArgumentException` |
| D-R-15 | `PaymentId.generate()` uniqueness | two generated IDs differ |
| D-R-16 | `incrementAttemptCount()` increments correctly | count tracks calls |
| D-R-17 | `markProcessing()` returns event with correct from/to | event fields match |

---

## Routing Engine Tests (`RoutingEngineTest`)

### Sanity

| ID | Test | Expected |
|---|---|---|
| R-S-01 | CARD resolves to [PROVIDER_A, PROVIDER_B] | chain[0] = A, chain[1] = B |
| R-S-02 | UPI resolves to [PROVIDER_B, PROVIDER_A] | chain[0] = B, chain[1] = A |

### Regression (Negative)

| ID | Test | Expected |
|---|---|---|
| R-N-01 | No strategy registered for method | `UnsupportedPaymentMethodException` |
| R-N-02 | Strategy references provider ID not in registry | `IllegalStateException` |

---

## Retry Executor Tests (`RetryableProviderExecutorTest`)

### Sanity

| ID | Test | Expected |
|---|---|---|
| E-S-01 | Provider succeeds on first attempt | `ExecutionResult` with correct provider/txn |

### Regression

| ID | Test | Expected |
|---|---|---|
| E-R-01 | Retry same provider on failure, succeeds on 2nd | called twice, success |
| E-R-02 | All retries on primary exhausted → failover to secondary | secondary returns success |
| E-R-03 | All providers all retries exhausted | `PaymentFailedException` thrown |
| E-R-04 | Provider throws exception → treated as failure, retries | succeeds on next attempt |
| E-R-05 | Total attempt count tracked across all providers | `payment.attemptCount == 4` (3+1) |

---

## Application Layer Tests (`CreatePaymentServiceTest`)

### Sanity

| ID | Test | Expected |
|---|---|---|
| A-S-01 | Successful CARD payment returns SUCCESS result | `status=SUCCESS`, `provider=PROVIDER_A` |
| A-S-02 | UPI payment routes to PROVIDER_B | `provider=PROVIDER_B`, providerA never called |

### Regression

| ID | Test | Expected |
|---|---|---|
| A-R-01 | Duplicate idempotency key returns cached result | no DB writes, cached result returned |
| A-R-02 | All providers fail → `PaymentFailedException` thrown | 3 saves (PENDING/PROCESSING/FAILED) |
| A-R-03 | Domain events published on each transition | 2 events published (PROCESSING + SUCCESS) |

---

## Provider Adapter Tests (`ProviderAdapterTest`)

### Sanity

| ID | Test | Expected |
|---|---|---|
| P-S-01 | ProviderA providerId() returns "PROVIDER_A" | string match |
| P-S-02 | ProviderA charge succeeds (0% failure rate) | `success=true`, txnId starts with "PROVIDER_A-" |
| P-S-03 | ProviderB providerId() returns "PROVIDER_B" | string match |
| P-S-04 | ProviderB charge succeeds (0% failure rate) | `success=true`, txnId starts with "PROVIDER_B-" |

### Regression (Negative)

| ID | Test | Expected |
|---|---|---|
| P-N-01 | ProviderA charge fails (100% failure rate) | `success=false`, failureReason not blank |
| P-N-02 | ProviderB charge fails (100% failure rate) | `success=false` |
| P-N-03 | Unique txn IDs on successive successes | txn1 ≠ txn2 |

---

## Idempotency Tests (`InMemoryIdempotencyAdapterTest`)

### Sanity

| ID | Test | Expected |
|---|---|---|
| I-S-01 | Store and retrieve result by key | retrieved result == stored result |

### Regression

| ID | Test | Expected |
|---|---|---|
| I-R-01 | Unknown key returns empty | `Optional.empty()` |
| I-R-02 | Different keys are independent | each key returns its own result |
| I-R-03 | Overwriting same key replaces result | latest result returned |

---

## Observability Tests (`ObservabilityDecoratorTest`)

| ID | Test | Expected |
|---|---|---|
| O-S-01 | `LoggingProviderDecorator` delegates to underlying | result unchanged, delegate called once |
| O-S-02 | `LoggingProviderDecorator` propagates provider ID | same ID as delegate |
| O-S-03 | `LoggingProviderDecorator` re-throws exceptions | exception propagates |
| O-S-04 | `ObservingProviderDecorator` delegates to underlying | result unchanged |
| O-S-05 | Full stack (Observing → Logging → Underlying) works | result unchanged, delegate called once |

---

## Controller Tests (`PaymentControllerTest`)

### Sanity

| ID | Test | Expected |
|---|---|---|
| C-S-01 | POST /payments succeeds → 201 with body | JSON contains paymentId, status, provider |
| C-S-02 | GET /payments/{id} found → 200 with body | JSON contains paymentId and status |

### Regression (Negative)

| ID | Test | Expected |
|---|---|---|
| C-N-01 | POST without Idempotency-Key header → 400 | `MISSING_HEADER` error |
| C-N-02 | POST with null amount → 400 | `VALIDATION_ERROR` error |
| C-N-03 | POST with zero amount → 400 | validation error |
| C-N-04 | POST with unknown method enum → 400 | bad request |
| C-N-05 | POST all providers fail → 422 | `PAYMENT_FAILED` error |
| C-N-06 | GET with unknown ID → 404 | `PAYMENT_NOT_FOUND` error |
| C-N-07 | GET with malformed UUID → 400 | `BAD_REQUEST` error |

---

## Integration Tests (`JpaPaymentRepositoryAdapterTest`)  
*Requires Docker — skipped otherwise*

### Sanity

| ID | Test | Expected |
|---|---|---|
| IT-S-01 | Save and find PENDING payment | all fields persisted correctly |

### Regression

| ID | Test | Expected |
|---|---|---|
| IT-R-01 | Persist PROCESSING → SUCCESS transition | status=SUCCESS, provider recorded |
| IT-R-02 | Persist PROCESSING → FAILED transition | status=FAILED |
| IT-R-03 | Persist attempt count | count matches increments |
| IT-R-04 | Find unknown ID → empty | `Optional.empty()` |
| IT-R-05 | Repeated save overwrites (upsert semantics) | latest state reflected |

---

## Test Count Summary

| Layer | Tests | Failures |
|---|---|---|
| Domain model | 26 | 0 |
| Routing engine | 4 | 0 |
| Retry executor | 6 | 0 |
| Application services | 5 | 0 |
| Provider adapters | 7 | 0 |
| Idempotency | 4 | 0 |
| Observability decorators | 5 | 0 |
| Controller (MockMvc) | 9 | 0 |
| Persistence (Testcontainers) | 6 | 0 (skipped without Docker) |
| **Total** | **72** | **0** |
