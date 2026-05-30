package com.yuno.payment.infrastructure.observability;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.ProviderResult;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator pattern: wraps any PaymentProviderPort and adds structured logging.
 *
 * Logs before and after every charge attempt with:
 *   paymentId, provider, method, amount, currency, outcome, durationMs
 *
 * SRP: this class only logs — it has no routing, retry, or business logic.
 * The decorated provider never knows it is being observed.
 */
public class LoggingProviderDecorator implements PaymentProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingProviderDecorator.class);

    private final PaymentProviderPort delegate;

    public LoggingProviderDecorator(PaymentProviderPort delegate) {
        this.delegate = delegate;
    }

    @Override
    public String providerId() {
        return delegate.providerId();
    }

    @Override
    public ProviderResult charge(Payment payment) {
        long startMs = System.currentTimeMillis();

        log.info("Provider charge start paymentId={} provider={} method={} amount={} currency={}",
                payment.getId(),
                delegate.providerId(),
                payment.getMethod(),
                payment.getAmount().amount(),
                payment.getAmount().currency());

        try {
            ProviderResult result = delegate.charge(payment);
            long durationMs = System.currentTimeMillis() - startMs;

            if (result.success()) {
                log.info("Provider charge success paymentId={} provider={} txnId={} durationMs={}",
                        payment.getId(),
                        delegate.providerId(),
                        result.providerTransactionId(),
                        durationMs);
            } else {
                log.warn("Provider charge failed paymentId={} provider={} reason={} durationMs={}",
                        payment.getId(),
                        delegate.providerId(),
                        result.failureReason(),
                        durationMs);
            }

            return result;

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("Provider charge exception paymentId={} provider={} error={} durationMs={}",
                    payment.getId(),
                    delegate.providerId(),
                    e.getMessage(),
                    durationMs);
            throw e;
        }
    }
}
