package com.yuno.payment.infrastructure.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

/**
 * Simulates external payment provider behaviour.
 *
 * Each provider has a configurable failure rate (0.0 = always succeeds, 1.0 = always fails).
 * Failure rates are set in application.yml and overridden to 0.0 in test profile,
 * making tests deterministic.
 *
 * In production, replace adapters with real HTTP client calls — the simulator is
 * swapped out without touching any other class.
 */
public class ProviderSimulator {

    private static final Logger log = LoggerFactory.getLogger(ProviderSimulator.class);

    private final double providerAFailureRate;
    private final double providerBFailureRate;
    private final Random random;

    public ProviderSimulator(double providerAFailureRate, double providerBFailureRate) {
        this.providerAFailureRate = providerAFailureRate;
        this.providerBFailureRate = providerBFailureRate;
        this.random = new Random();
    }

    /**
     * Simulates a charge attempt for Provider A (CARD).
     *
     * @return a success result with a generated transaction ID, or a failure result
     */
    public SimulatorResult chargeViaProviderA() {
        return simulate("PROVIDER_A", providerAFailureRate);
    }

    /**
     * Simulates a charge attempt for Provider B (UPI).
     *
     * @return a success result with a generated transaction ID, or a failure result
     */
    public SimulatorResult chargeViaProviderB() {
        return simulate("PROVIDER_B", providerBFailureRate);
    }

    private SimulatorResult simulate(String providerId, double failureRate) {
        boolean fails = random.nextDouble() < failureRate;
        if (fails) {
            log.debug("Simulator: {} returning failure (failureRate={})", providerId, failureRate);
            return SimulatorResult.failure("Simulated provider failure");
        }
        String txnId = providerId + "-" + UUID.randomUUID();
        log.debug("Simulator: {} returning success txnId={}", providerId, txnId);
        return SimulatorResult.success(txnId);
    }

    /** Simple result type internal to the infrastructure provider package. */
    public record SimulatorResult(boolean success, String transactionId, String failureReason) {
        public static SimulatorResult success(String txnId) {
            return new SimulatorResult(true, txnId, null);
        }
        public static SimulatorResult failure(String reason) {
            return new SimulatorResult(false, null, reason);
        }
    }
}
