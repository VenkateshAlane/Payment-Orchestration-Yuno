package com.yuno.payment.domain.service;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;

import java.util.List;

/**
 * Pure domain service: resolves the provider chain for a given payment.
 *
 * GRASP Pure Fabrication: no real-world counterpart, created for cohesion.
 * GRASP Low Coupling: depends only on ProviderRegistry (also pure domain).
 */
public class RoutingEngine {

    private final ProviderRegistry registry;

    public RoutingEngine(ProviderRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the ordered provider chain to use for this payment.
     * Delegates to ProviderRegistry — RoutingEngine stays thin.
     */
    public List<PaymentProviderPort> resolveChain(Payment payment) {
        return registry.getChainFor(payment.getMethod());
    }
}
