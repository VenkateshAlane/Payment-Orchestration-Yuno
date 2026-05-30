package com.yuno.payment.application;

import com.yuno.payment.application.command.CreatePaymentCommand;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.IdempotencyKey;
import com.yuno.payment.domain.model.PaymentMethod;
import com.yuno.payment.domain.model.ProviderResult;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import com.yuno.payment.domain.port.outbound.PaymentRepositoryPort;
import com.yuno.payment.domain.service.CardRoutingStrategy;
import com.yuno.payment.domain.service.ProviderRegistry;
import com.yuno.payment.domain.service.RoutingEngine;
import com.yuno.payment.domain.service.UpiRoutingStrategy;
import com.yuno.payment.infrastructure.idempotency.InMemoryIdempotencyAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies idempotency guarantees under concurrent load.
 *
 * KNOWN LIMITATION — full concurrent idempotency (first-write-wins across
 * threads) requires a distributed lock (Redis SET NX before processing).
 * The current putIfAbsent implementation provides "soft" idempotency:
 *   - Once the cache is populated, all subsequent callers receive the same result.
 *   - Truly concurrent first-callers may each create a payment before any
 *     one of them calls store() — that window is the residual TOCTOU gap.
 *
 * This test verifies the guarantee that IS provided:
 *   After any concurrent race settles, every subsequent caller retrieves
 *   exactly the cached result — no new payments are created.
 */
@DisplayName("Idempotency under concurrent load")
class IdempotencyConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    private PaymentRepositoryPort repository;
    private InMemoryIdempotencyAdapter idempotency;
    private PaymentProviderPort provider;
    private CreatePaymentService service;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentRepositoryPort.class);
        provider   = mock(PaymentProviderPort.class);
        PaymentProviderPort providerB = mock(PaymentProviderPort.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        when(provider.providerId()).thenReturn("PROVIDER_A");
        when(provider.charge(any())).thenReturn(ProviderResult.success("txn-concurrent-001"));
        when(providerB.providerId()).thenReturn("PROVIDER_B");

        idempotency = new InMemoryIdempotencyAdapter();

        ProviderRegistry registry = new ProviderRegistry(
                List.of(provider, providerB),
                List.of(new CardRoutingStrategy(), new UpiRoutingStrategy())
        );
        RoutingEngine routingEngine = new RoutingEngine(registry);
        RetryableProviderExecutor executor = new RetryableProviderExecutor(
                1, 0L, 1.0, 0L, ms -> {}
        );

        service = new CreatePaymentService(repository, idempotency, routingEngine, executor, eventPublisher);
    }

    @Test
    @DisplayName("once the cache is populated, all subsequent callers receive the same cached result without a new charge")
    void subsequentCallersGetCachedResultAfterRaceSettles() throws Exception {
        IdempotencyKey sharedKey = new IdempotencyKey("concurrent-key-001");
        CreatePaymentCommand command = new CreatePaymentCommand(
                new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "cust-concurrent",
                sharedKey
        );

        // Step 1: let the concurrent race happen (some threads may create duplicate payments)
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<PaymentResult>> futures = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                return service.execute(command);
            }));
        }
        startGate.countDown();
        pool.shutdown();

        // drain concurrent results — we don't assert on these
        for (Future<PaymentResult> f : futures) {
            f.get();
        }

        // Step 2: after the race, the idempotency cache must be populated.
        // Subsequent sequential calls must all return the cached result — no new charge.
        int priorChargeCount = org.mockito.Mockito.mockingDetails(provider)
                .getInvocations().size();

        Set<String> subsequentIds = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < THREAD_COUNT; i++) {
            subsequentIds.add(service.execute(command).paymentId());
        }

        // All subsequent callers must get the same (cached) paymentId
        assertThat(subsequentIds)
                .as("all post-race calls must return the single cached paymentId")
                .hasSize(1);

        // No new charges after the cache was populated
        int finalChargeCount = org.mockito.Mockito.mockingDetails(provider)
                .getInvocations().size();
        assertThat(finalChargeCount)
                .as("no new provider charges after idempotency cache is warm")
                .isEqualTo(priorChargeCount);
    }
}
