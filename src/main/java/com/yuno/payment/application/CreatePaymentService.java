package com.yuno.payment.application;

import com.yuno.payment.application.command.CreatePaymentCommand;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.exception.PaymentFailedException;
import com.yuno.payment.domain.model.Money;
import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.port.inbound.CreatePaymentUseCase;
import com.yuno.payment.domain.port.outbound.IdempotencyPort;
import com.yuno.payment.domain.port.outbound.PaymentRepositoryPort;
import com.yuno.payment.domain.service.RoutingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates the full payment creation flow.
 *
 * Flow:
 *   1. Idempotency check — return cached result if key already processed
 *   2. Create Payment aggregate (PENDING)
 *   3. Persist (PENDING)
 *   4. Resolve provider chain via RoutingEngine
 *   5. Transition aggregate to PROCESSING, persist
 *   6. Execute via RetryableProviderExecutor (retry + failover)
 *   7. Transition to SUCCESS or FAILED, persist
 *   8. Publish domain event for each transition
 *   9. Store idempotency result and return
 *
 * Transaction ownership: each repository.save() call carries its own @Transactional
 * on the adapter, so the DB connection is held only for the duration of the SQL
 * statement — never across provider HTTP calls or retry backoff sleeps.
 */
@Service
public class CreatePaymentService implements CreatePaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePaymentService.class);

    private final PaymentRepositoryPort repository;
    private final IdempotencyPort idempotency;
    private final RoutingEngine routingEngine;
    private final RetryableProviderExecutor executor;
    private final ApplicationEventPublisher eventPublisher;

    public CreatePaymentService(PaymentRepositoryPort repository,
                                IdempotencyPort idempotency,
                                RoutingEngine routingEngine,
                                RetryableProviderExecutor executor,
                                ApplicationEventPublisher eventPublisher) {
        this.repository     = repository;
        this.idempotency    = idempotency;
        this.routingEngine  = routingEngine;
        this.executor       = executor;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentResult execute(CreatePaymentCommand command) {

        // ── 1. Idempotency check ──────────────────────────────────────────────
        Optional<PaymentResult> cached = idempotency.find(command.idempotencyKey());
        if (cached.isPresent()) {
            log.info("Idempotency hit key={}", command.idempotencyKey());
            return cached.get();
        }

        // ── 2. Build aggregate ────────────────────────────────────────────────
        Money money     = new Money(command.amount(), command.currency());
        Payment payment = Payment.create(money, command.method());
        log.info("Creating payment id={} method={} amount={} {}",
                payment.getId(), payment.getMethod(), money.amount(), money.currency());

        // ── 3. Persist PENDING ────────────────────────────────────────────────
        repository.save(payment);

        // ── 4. Resolve provider chain ─────────────────────────────────────────
        var chain = routingEngine.resolveChain(payment);
        log.info("Resolved chain payment={} providers={}",
                payment.getId(), chain.stream().map(p -> p.providerId()).toList());

        // ── 5. Transition PENDING → PROCESSING ───────────────────────────────
        eventPublisher.publishEvent(payment.markProcessing());
        repository.save(payment);

        // ── 6–7. Execute with retry/failover; transition to SUCCESS or FAILED ─
        try {
            ExecutionResult result = executor.execute(payment, chain);

            eventPublisher.publishEvent(payment.markSuccess(result.winningProviderId(), result.providerTransactionId()));
            repository.save(payment);

        } catch (PaymentFailedException e) {
            eventPublisher.publishEvent(payment.markFailed());
            repository.save(payment);
            // Cache the failure so a retry with the same key returns FAILED
            // immediately instead of re-attempting the charge.
            // Client must use a new Idempotency-Key to retry the payment.
            idempotency.store(command.idempotencyKey(), toResult(payment));
            throw e;
        }

        // ── 8. Build, store, and return result ────────────────────────────────
        PaymentResult paymentResult = toResult(payment);
        idempotency.store(command.idempotencyKey(), paymentResult);
        return paymentResult;
    }

    private PaymentResult toResult(Payment payment) {
        return PaymentResultMapper.toResult(payment);
    }
}
