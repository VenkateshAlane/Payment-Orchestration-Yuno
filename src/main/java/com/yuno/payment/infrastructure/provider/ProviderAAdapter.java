package com.yuno.payment.infrastructure.provider;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.ProviderResult;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;

/**
 * Outbound adapter for Provider A (handles CARD payments as primary).
 * Implements PaymentProviderPort — the domain never knows this class exists.
 *
 * In a real system this would call an external HTTP API.
 * Here it delegates to ProviderSimulator which produces configurable outcomes.
 */
public class ProviderAAdapter implements PaymentProviderPort {

    private static final String PROVIDER_ID = "PROVIDER_A";

    private final ProviderSimulator simulator;

    public ProviderAAdapter(ProviderSimulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public ProviderResult charge(Payment payment) {
        ProviderSimulator.SimulatorResult result = simulator.chargeViaProviderA();
        if (result.success()) {
            return ProviderResult.success(result.transactionId());
        }
        return ProviderResult.failure(result.failureReason());
    }
}
