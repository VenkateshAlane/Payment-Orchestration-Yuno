package com.yuno.payment.application;

import com.yuno.payment.domain.exception.PaymentFailedException;
import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.ProviderResult;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import com.yuno.payment.application.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Executes a payment against an ordered list of providers with retry and failover.
 *
 * Chain of Responsibility: each provider tries to handle the payment.
 * If exhausted, passes to the next.
 *
 * GRASP Pure Fabrication: no real-world entity — created for cohesion of retry logic.
 * GRASP High Cohesion: only responsible for retry/failover execution.
 *
 * Sleeper is injected so tests run at full speed without real Thread.sleep calls.
 */
public class RetryableProviderExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryableProviderExecutor.class);

    private final int maxAttemptsPerProvider;
    private final long initialBackoffMs;
    private final double backoffMultiplier;
    private final long maxBackoffMs;
    private final Sleeper sleeper;

    public RetryableProviderExecutor(int maxAttemptsPerProvider,
                                     long initialBackoffMs,
                                     double backoffMultiplier,
                                     long maxBackoffMs,
                                     Sleeper sleeper) {
        this.maxAttemptsPerProvider = maxAttemptsPerProvider;
        this.initialBackoffMs       = initialBackoffMs;
        this.backoffMultiplier      = backoffMultiplier;
        this.maxBackoffMs           = maxBackoffMs;
        this.sleeper                = sleeper;
    }

    /**
     * Tries each provider in the chain up to maxAttemptsPerProvider times.
     * Uses exponential backoff between retries within the same provider.
     * Moves to the next provider (failover) when all attempts for one are exhausted.
     *
     * @param payment       the payment to process (aggregate tracks attempt count)
     * @param providerChain ordered list of providers to try
     * @return ProviderResult from the first successful attempt
     * @throws PaymentFailedException if all providers and all attempts are exhausted
     */
    /**
     * Tries each provider in the chain up to maxAttemptsPerProvider times.
     * Uses exponential backoff between retries within the same provider.
     * Moves to the next provider (failover) when all attempts for one are exhausted.
     *
     * @return ExecutionResult containing the winning provider ID and provider transaction ID
     * @throws PaymentFailedException if all providers and all attempts are exhausted
     */
    public ExecutionResult execute(Payment payment, List<PaymentProviderPort> providerChain) {
        for (int providerIndex = 0; providerIndex < providerChain.size(); providerIndex++) {
            PaymentProviderPort provider = providerChain.get(providerIndex);
            boolean isFailover = providerIndex > 0;

            long backoffMs = initialBackoffMs;

            for (int attempt = 1; attempt <= maxAttemptsPerProvider; attempt++) {
                payment.incrementAttemptCount();

                log.info("Charging payment={} provider={} attempt={} failover={}",
                        payment.getId(), provider.providerId(), attempt, isFailover);

                try {
                    ProviderResult result = provider.charge(payment);

                    if (result.success()) {
                        log.info("Payment succeeded payment={} provider={} totalAttempts={}",
                                payment.getId(), provider.providerId(), payment.getAttemptCount());
                        return new ExecutionResult(provider.providerId(), result.providerTransactionId());
                    }

                    log.warn("Payment attempt failed payment={} provider={} attempt={} reason={}",
                            payment.getId(), provider.providerId(), attempt, result.failureReason());

                } catch (Exception e) {
                    log.error("Provider threw exception payment={} provider={} attempt={} error={}",
                            payment.getId(), provider.providerId(), attempt, e.getMessage());
                }

                if (attempt < maxAttemptsPerProvider) {
                    doSleep(backoffMs);
                    backoffMs = Math.min((long) (backoffMs * backoffMultiplier), maxBackoffMs);
                }
            }

            log.warn("Provider exhausted, moving to next payment={} exhaustedProvider={}",
                    payment.getId(), provider.providerId());
        }

        log.error("All providers exhausted for payment={}", payment.getId());
        throw new PaymentFailedException(payment.getId());
    }

    private void doSleep(long ms) {
        if (ms <= 0) return;
        try {
            sleeper.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
