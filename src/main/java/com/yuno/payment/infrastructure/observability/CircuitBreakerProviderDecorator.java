package com.yuno.payment.infrastructure.observability;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.ProviderResult;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator that wraps any PaymentProviderPort with a Resilience4j circuit breaker.
 *
 * When a provider fails consistently (configurable threshold), the circuit opens
 * and subsequent calls fail immediately without hitting the provider network.
 * After a configurable wait period the circuit transitions to half-open to probe recovery.
 *
 * The circuit breaker name matches the provider ID so configuration in application.yml
 * is per-provider:
 *   resilience4j.circuitbreaker.instances.providerA.*
 *   resilience4j.circuitbreaker.instances.providerB.*
 *
 * Decorator stack: ObservingProviderDecorator → LoggingProviderDecorator
 *                  → CircuitBreakerProviderDecorator → ActualAdapter
 */
public class CircuitBreakerProviderDecorator implements PaymentProviderPort {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerProviderDecorator.class);

    private final PaymentProviderPort delegate;
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerProviderDecorator(PaymentProviderPort delegate,
                                           CircuitBreakerRegistry registry) {
        this.delegate       = delegate;
        this.circuitBreaker = registry.circuitBreaker(delegate.providerId());
    }

    @Override
    public String providerId() {
        return delegate.providerId();
    }

    @Override
    public ProviderResult charge(Payment payment) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.charge(payment));
        } catch (CallNotPermittedException e) {
            log.warn("Circuit open for provider={} payment={} — skipping charge attempt",
                    delegate.providerId(), payment.getId());
            return ProviderResult.failure("Circuit open — provider " + delegate.providerId() + " is unavailable");
        }
    }
}
