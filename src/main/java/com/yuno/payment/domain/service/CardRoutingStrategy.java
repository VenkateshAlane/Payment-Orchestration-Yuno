package com.yuno.payment.domain.service;

import com.yuno.payment.domain.model.PaymentMethod;

import java.util.List;

/**
 * Routes CARD payments to Provider A (primary) with Provider B as failover.
 */
public class CardRoutingStrategy implements PaymentRoutingStrategy {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public List<String> providerChain() {
        return List.of("PROVIDER_A", "PROVIDER_B");
    }
}
