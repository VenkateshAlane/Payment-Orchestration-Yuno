package com.yuno.payment.domain.service;

import com.yuno.payment.domain.model.PaymentMethod;

import java.util.List;

/**
 * Strategy pattern: each payment method gets its own routing strategy.
 *
 * OCP: adding a new payment method = implement this interface + register in config.
 * No existing code changes.
 *
 * Returns provider IDs in priority order — first is primary, rest are failover.
 */
public interface PaymentRoutingStrategy {
    PaymentMethod supportedMethod();
    List<String> providerChain();
}
