# AI Prompt Log

Documents how I used AI during development, as required by the assessment.

Tool: Claude (claude-sonnet-4-6)

---

## Phase 1 — Understanding the Requirements

**Prompt:**
> Read the PDF completely end to end and once done give me a detailed overview of what needs to be built. I will then verify your understanding and correct any deviations before we write a single line of code

**AI gave back:** summary of the payment orchestration requirements — two providers, retry, failover, idempotency, hexagonal architecture.

**What I corrected:**
- AI assumed a single transaction wrapping the entire payment flow. I flagged this — holding a DB connection open across provider HTTP calls and retry backoff sleeps would exhaust the connection pool under load. Told it each repository.save() needs its own short transaction at the adapter level, not the service
- AI planned to put business rules in the controller. Clarified the architecture: controller translates HTTP, service orchestrates, domain owns rules. Nothing crosses those lines

---

## Phase 2 — Project Setup

**Prompt:**
> Before generating any code tell me what tech choices you are planning to make and why. I want to understand your reasoning first and I will tell you if I agree or disagree

**AI proposed:** Spring Boot 3.2.5, Hibernate DDL, no structured logging, hardcoded config values.

**I pushed back on:**
- Hibernate DDL in prod is dangerous, changed to Flyway for explicit versioned migrations
- Structured JSON logging is non negotiable for anything production grade, added logstash-logback-encoder
- Config values like failure rates must be in application.yml not hardcoded — told it to use @Value

**Prompt after alignment:**
> Now generate the project setup based on what we agreed. Spring Boot, Postgres, Redis, Zipkin, Flyway, structured JSON logs, docker compose, Testcontainers in test scope only. Once done list out every file you created and what each one does and I will review before we move on

**After reviewing the output I changed:**
- Added application-test.yml myself with zero backoff and zero failure rates — tests should not sleep
- AI forgot to scope Testcontainers to test only in pom.xml

---

## Phase 3 — Domain Model

**Prompt:**
> Before writing the domain model explain to me how you plan to implement the payment state machine. I want to see your approach first, tell me the tradeoffs, and I will decide which direction to go

**AI proposed:** switch statement in the Payment class checking current status before each transition.

**I rejected this:**
- Switch statement in Payment means every new status requires touching Payment itself, violates OCP
- Told it to use the State pattern on the enum — each status knows its own valid next states via an abstract method. Open for extension, closed for modification

**Prompt:**
> Now implement the domain model with the enum-based state machine we discussed. Payment aggregate with create() and reconstitute() static factories only, no public constructor. Value objects for Money, PaymentId, IdempotencyKey, ProviderResult. Domain events returned from each transition. Use records wherever it makes sense. Once done walk me through the invariants you enforced and I will check if anything is missing

**What I caught in review:**
- reconstitute() had no null checks — corrupted DB rows would blow up deep in business logic instead of failing at the boundary. Added requireNonNull guards myself
- Money was accepting any string as currency. I told it to validate against ISO 4217 using Currency.getInstance() and normalise to uppercase

---

## Phase 4 — Ports

**Prompt:**
> Define all ports before any infrastructure. Tell me what you think the inbound and outbound boundaries should be, explain why you drew them where you did, and I will validate before we proceed

**Reviewed and adjusted:**
- Documented first-writer-wins semantics on IdempotencyPort.store() explicitly in the Javadoc — AI left the contract ambiguous which would cause bugs when someone implements a new adapter

---

## Phase 5 — Routing Engine

**Prompt:**
> Implement the routing engine. But first tell me what pattern you plan to use and why. I don't want you to just start coding

**AI proposed:** if-else chain in the service.

**I rejected this:**
- Told it Strategy pattern with a registry. Adding a new payment method should not require touching existing code
- Insisted the registry validate that every provider ID referenced in a strategy actually has a registered adapter. AI's version silently returned an empty list — silent failures in payment routing are unacceptable

---

## Phase 6 — Application Layer

**Prompt:**
> Implement the full payment orchestration flow in CreatePaymentService. Walk me through the exact sequence of steps you plan before writing code. I want to catch any logical issues before they end up in the codebase

**AI's planned sequence was missing:** storing the idempotency result on failure. If a payment fails and the client retries with the same key, AI's version would hit the providers again instead of immediately returning FAILED.

**I corrected this before implementation:**
> You are missing idempotency storage on the failure path. A failed payment should be cached so retries with the same key return FAILED immediately without re-attempting the charge. Fix the sequence and show me again before coding

**After implementation I also:**
- Verified the Sleeper injection pattern so tests don't sleep for real
- Confirmed @Transactional is on the adapter methods not the service

---

## Phase 7 — Persistence

**Prompt:**
> Implement the JPA adapter. Before writing show me how you plan to keep the JPA entity separate from the domain model and how the mapping works. I want to make sure there is no JPA leaking into domain

**After reviewing output:**
- Build was failing — flyway-database-postgresql doesn't exist in Flyway 9.x, it's bundled in flyway-core. I caught this and removed it
- Testcontainers tests were failing hard without Docker. Added disabledWithoutDocker = true myself so CI without Docker skips gracefully instead of failing

---

## Phase 8 — Providers

**Prompt:**
> Implement provider adapters with a configurable simulator. Failure rate 0.0 means always succeeds, 1.0 means always fails. I need to test retry and failover deterministically so the simulator must be injectable and configurable per provider via application.yml

---

## Phase 9 — Idempotency

**Prompt:**
> Two adapters, Redis for prod and in-memory for tests. Before implementing explain to me how you will ensure first-writer-wins atomicity. I do not want last-writer-wins

**AI explained it would use put() and set().**

**I stopped it:**
> Those are not atomic. put() in ConcurrentHashMap and set() in Redis both overwrite. You need putIfAbsent() and Redis SET NX EX. Explain the difference and then implement

- Verified after implementation that setIfAbsent return value is checked correctly for null safety

---

## Phase 10 — Observability

**Prompt:**
> Logging decorator and Micrometer observation decorator wrapping the provider port. Don't use Brave or OpenTelemetry APIs directly, go through the Micrometer Observation abstraction. Walk me through the observation lifecycle before writing

**Bug I caught:**
- observation.stop() was inside the try block — exceptions would bypass it, leaving spans dangling. Told it to move stop() to finally and add observation.error(e) in the catch

---

## Phase 11 — Web Layer

**Prompt:**
> Controller, exception handler, DTOs. Show me the exception-to-HTTP mapping you plan first. Error responses must not leak any internal details — no class names, no stack traces, no payment IDs in error messages

**Things I added after review:**
- Max 255 character limit on the Idempotency-Key header — no limit is a DoS vector AI completely missed
- Filter URL pattern was /payments/* only which misses the POST /payments endpoint with no path segment

---

## Phase 12 — Self Review

**Prompt:**
> Stop generating new features. Do a full review of everything built so far and tell me every issue you can find — correctness bugs, production concerns, security gaps. Don't fix anything yet, just list everything and explain why each is a problem. I will decide what to fix

**AI listed issues. I reviewed the list and decided what was worth fixing:**

> Fix all of these except the distributed lock — that is a known limitation I will document in the concurrency test. Don't change anything I haven't explicitly told you to fix

**Fixed:**
- GetPaymentService instantiated with new in PaymentConfig so @Transactional was silently inert — added @Service
- providerTransactionId captured in ExecutionResult but never persisted — critical for reconciliation, added V2 migration
- No optimistic locking — concurrent updates silently overwrite, added @Version and V3 migration
- No circuit breaker — used a decorator not the @CircuitBreaker annotation because the adapters are in a chain and Spring's proxy is never in the call path
- Credentials plaintext in yaml — moved to env vars with local defaults
- No graceful shutdown — in-flight payment requests would die mid-processing on pod restart
- No audit trail of state transitions — added payment_status_history table and a persister

**Prompt:**
> Now add a concurrency test for idempotency. But be honest in the test about what the current implementation actually guarantees versus what it doesn't. I don't want a test that asserts behaviour we haven't actually implemented

Test documents the soft guarantee — once cache is warm all callers get the same result — and explicitly calls out the residual TOCTOU gap and that a distributed lock would be needed to fully close it.
