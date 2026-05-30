package com.yuno.payment.domain.service;

import com.yuno.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yuno.payment.domain.model.PaymentMethod;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry pattern: maps each PaymentMethod to an ordered chain of providers.
 *
 * Populated at startup from Spring config — no code change needed to add providers.
 * GRASP Indirection: consumers ask the registry, never resolve providers themselves.
 */
public class ProviderRegistry {

    private final Map<String, PaymentProviderPort> providerById;
    private final Map<PaymentMethod, PaymentRoutingStrategy> strategyByMethod;

    public ProviderRegistry(List<PaymentProviderPort> providers,
                            List<PaymentRoutingStrategy> strategies) {
        this.providerById = new HashMap<>();
        for (PaymentProviderPort p : providers) {
            providerById.put(p.providerId(), p);
        }

        this.strategyByMethod = new HashMap<>();
        for (PaymentRoutingStrategy s : strategies) {
            strategyByMethod.put(s.supportedMethod(), s);
        }
    }

    /**
     * Returns the ordered provider chain for the given payment method.
     * First element is primary; subsequent elements are failover targets.
     *
     * @throws UnsupportedPaymentMethodException if no strategy is registered for the method
     * @throws IllegalStateException             if a provider ID in the chain has no matching adapter
     */
    public List<PaymentProviderPort> getChainFor(PaymentMethod method) {
        PaymentRoutingStrategy strategy = strategyByMethod.get(method);
        if (strategy == null) {
            throw new UnsupportedPaymentMethodException(method);
        }

        List<PaymentProviderPort> chain = new ArrayList<>();
        for (String id : strategy.providerChain()) {
            PaymentProviderPort provider = providerById.get(id);
            if (provider == null) {
                throw new IllegalStateException(
                        "Provider ID '" + id + "' is listed in routing strategy but has no registered adapter");
            }
            chain.add(provider);
        }
        return chain;
    }
}
