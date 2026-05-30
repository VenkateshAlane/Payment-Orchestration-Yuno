package com.yuno.payment.infrastructure.provider;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.ProviderResult;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;

/**
 * Outbound adapter for Provider B (handles UPI payments as primary, CARD as failover).
 * Implements PaymentProviderPort — the domain never knows this class exists.
 */
public class ProviderBAdapter implements PaymentProviderPort {

    private static final String PROVIDER_ID = "PROVIDER_B";

    private final ProviderSimulator simulator;

    public ProviderBAdapter(ProviderSimulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public ProviderResult charge(Payment payment) {
        ProviderSimulator.SimulatorResult result = simulator.chargeViaProviderB();
        if (result.success()) {
            return ProviderResult.success(result.transactionId());
        }
        return ProviderResult.failure(result.failureReason());
    }
}
