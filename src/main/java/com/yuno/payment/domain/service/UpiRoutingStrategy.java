package com.yuno.payment.domain.service;

import com.yuno.payment.domain.model.PaymentMethod;

import java.util.List;

/**
 * Routes UPI payments to Provider B (primary) with Provider A as failover.
 */
public class UpiRoutingStrategy implements PaymentRoutingStrategy {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    public List<String> providerChain() {
        return List.of("PROVIDER_B", "PROVIDER_A");
    }
}
