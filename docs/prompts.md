# AI-Assisted Development Prompt Log

This document records the prompts used during vibe coding as required by the assessment.

## Session Overview

Tool: Claude (claude-sonnet-4-6) via Claude Code CLI  
Approach: Phase-by-phase implementation with tests before proceeding. Each phase was reviewed and committed independently.

---

## Phase 1 — Project Setup

**Prompt:**
> Read the PDF and plan. Use best software architecture decisions and make it extensible following SOLID and GRASP. I want to log extensively and be able to trace out the payment as well. Create code phase-by-phase and independently test everything before proceeding to next phase and do self-reviews after every phase.

**What AI generated:**
- pom.xml with Spring Boot 3.2.5, Micrometer tracing, Testcontainers, logstash-logback-encoder
- application.yml with retry/provider/idempotency config
- application-test.yml with zero-backoff overrides
- logback-spring.xml with JSON structured logging
- docker-compose.yml (PostgreSQL 15, Redis 7, Zipkin)
- DB migration V1 (payments table)
- .gitignore, main application class

---

## Phase 2 — Domain Model

**Prompt (continuation):** Continue phase-by-phase.

**What AI generated:**
- `PaymentId`, `Money`, `PaymentMethod`, `PaymentStatus`, `IdempotencyKey`, `ProviderResult` value objects
- `PaymentStatus` enum with `canTransitionTo()` abstract method per state (State pattern)
- `Payment` aggregate with private constructor, `create()` and `reconstitute()` static factories, controlled state transitions returning domain events
- `PaymentStatusChangedEvent` domain event record
- `IllegalStateTransitionException`, `PaymentNotFoundException`, `PaymentFailedException`, `UnsupportedPaymentMethodException`
- 26 unit tests covering all transitions, value object validation, guards

**Key design decision prompted:** Use Java 17 `record` for value objects and events — immutable, self-validating, minimal boilerplate.

---

## Phase 3 — Ports and Commands

**What AI generated:**
- `CreatePaymentCommand`, `GetPaymentQuery` immutable command/query records
- `PaymentResult` shared application-layer DTO
- `CreatePaymentUseCase`, `GetPaymentUseCase` inbound port interfaces (ISP: one per use case)
- `PaymentRepositoryPort`, `PaymentProviderPort`, `IdempotencyPort` outbound port interfaces

---

## Phase 4 — Routing Engine

**What AI generated:**
- `PaymentRoutingStrategy` interface (Strategy pattern)
- `CardRoutingStrategy` → [PROVIDER_A, PROVIDER_B]
- `UpiRoutingStrategy` → [PROVIDER_B, PROVIDER_A]
- `ProviderRegistry` (Registry pattern — maps method to ordered provider chain)
- `RoutingEngine` (pure domain service delegating to registry)
- 4 routing tests including negative cases (unregistered method, missing adapter)

---

## Phase 5 — Application Layer

**What AI generated:**
- `Sleeper` functional interface (injected into `RetryableProviderExecutor` so tests use no-op)
- `ExecutionResult` record (carries winning provider ID + transaction ID)
- `RetryableProviderExecutor` (Chain of Responsibility + exponential backoff)
- `CreatePaymentService` (full orchestration flow with `@Transactional`)
- `GetPaymentService`
- 11 tests: retry scenarios, failover, idempotency hit, domain event publishing

**Bug fixed during testing:**
> Test `publishesDomainEvents` failed: `verify(eventPublisher, times(2)).publishEvent(any())` matched the wrong `ApplicationEventPublisher` overload. Fixed to `any(Object.class)`.

---

## Phase 6 — Persistence Infrastructure

**What AI generated:**
- `PaymentEntity` (JPA entity, separate from domain model)
- `SpringDataPaymentRepository` (Spring Data JPA interface)
- `PaymentEntityMapper` (bidirectional mapping: domain ↔ JPA entity)
- `JpaPaymentRepositoryAdapter` (implements `PaymentRepositoryPort`)
- Flyway dependency added to pom.xml

**Issues resolved:**
- `flyway-database-postgresql` doesn't exist in Flyway 9.x (Spring Boot 3.2.5) — removed, PostgreSQL support is included in `flyway-core`
- Testcontainers tests fail without Docker: added `disabledWithoutDocker = true`; tests skip gracefully

---

## Phase 7 — Provider Infrastructure

**What AI generated:**
- `ProviderSimulator` with configurable failure rates per provider
- `ProviderAAdapter` and `ProviderBAdapter` implementing `PaymentProviderPort`
- 7 tests with 0% and 100% failure rates, unique txnId generation

---

## Phase 8 — Idempotency Infrastructure

**What AI generated:**
- `InMemoryIdempotencyAdapter` (Null Object pattern, `ConcurrentHashMap`)
- `RedisIdempotencyAdapter` (JSON serialisation, configurable TTL)
- 4 idempotency unit tests

---

## Phase 9 — Observability

**What AI generated:**
- `LoggingProviderDecorator` (structured logs: before/after each charge with duration)
- `ObservingProviderDecorator` (Micrometer Observation span per charge: `provider.charge`)
- `PaymentTraceFilter` (seeds MDC with `idempotencyKey` for every request)
- `PaymentEventLogger` (listens to `PaymentStatusChangedEvent`, writes audit log)
- 5 decorator tests using `ObservationRegistry.NOOP`

---

## Phase 10 — Web Layer

**What AI generated:**
- `CreatePaymentRequest` record with Bean Validation annotations
- `PaymentHttpResponse`, `ErrorResponse` response records
- `PaymentMapper` (HTTP DTOs ↔ application commands/results)
- `PaymentController` (POST /payments, GET /payments/{id})
- `GlobalExceptionHandler` with OWASP-compliant error responses (no stack traces)
- `PaymentConfig` — Spring `@Configuration` wiring all domain/application beans
- `JacksonConfig` — `ObjectMapper` with `JavaTimeModule` for Instant serialization
- 9 controller tests with MockMvc

---

## Phase 11 — Documentation

**What AI generated:**
- `README.md` with setup guide, API docs, configuration reference
- `docs/test-cases.md` with all 72 test cases classified by type
- `docs/requirements.md` with functional/non-functional requirements and I/O specs
- `docs/prompts.md` (this file)
