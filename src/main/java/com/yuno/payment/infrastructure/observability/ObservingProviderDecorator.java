package com.yuno.payment.infrastructure.observability;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.ProviderResult;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * Decorator pattern: wraps any PaymentProviderPort with a Micrometer Observation span.
 *
 * Each charge attempt becomes a named span in Zipkin:
 *   Name: provider.charge
 *   Tags: provider.id, payment.method, outcome (success|failure)
 *
 * The Brave bridge (on the classpath) converts these Observations into
 * Zipkin spans automatically — no manual Brave/OpenTelemetry API calls needed.
 *
 * Decorator stack: ObservingProviderDecorator → LoggingProviderDecorator → ActualAdapter
 */
public class ObservingProviderDecorator implements PaymentProviderPort {

    private static final String OBSERVATION_NAME = "provider.charge";

    private final PaymentProviderPort delegate;
    private final ObservationRegistry registry;

    public ObservingProviderDecorator(PaymentProviderPort delegate, ObservationRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public String providerId() {
        return delegate.providerId();
    }

    @Override
    public ProviderResult charge(Payment payment) {
        Observation observation = Observation.createNotStarted(OBSERVATION_NAME, registry)
                .lowCardinalityKeyValue("provider.id", delegate.providerId())
                .lowCardinalityKeyValue("payment.method", payment.getMethod().name());

        return observation.observe(() -> {
            ProviderResult result = delegate.charge(payment);

            // Record outcome as a tag so it's visible in Zipkin
            observation.lowCardinalityKeyValue("outcome", result.success() ? "success" : "failure");
            if (!result.success()) {
                observation.event(Observation.Event.of("charge.failed", result.failureReason()));
            }

            return result;
        });
    }
}
