package com.yuno.payment.domain.port.outbound;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.ProviderResult;

/**
 * Outbound (driven) port representing a single payment provider.
 * GRASP Polymorphism: behaviour varies per provider without if/switch.
 * Each provider adapter implements this interface.
 * Decorators (logging, observability) wrap it without touching the adapter.
 */
public interface PaymentProviderPort {

    /**
     * Unique identifier for this provider, e.g. "PROVIDER_A".
     * Used by ProviderRegistry and for logging / tracing.
     */
    String providerId();

    /**
     * Attempt to charge the given payment through this provider.
     *
     * @param payment the payment to process (read-only here — state changes happen in the aggregate)
     * @return ProviderResult indicating success or failure with reason
     */
    ProviderResult charge(Payment payment);
}
