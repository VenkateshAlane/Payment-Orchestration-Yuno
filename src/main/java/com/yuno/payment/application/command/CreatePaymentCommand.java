package com.yuno.payment.application.command;

import com.yuno.payment.domain.model.IdempotencyKey;
import com.yuno.payment.domain.model.PaymentMethod;

import java.math.BigDecimal;

/**
 * Immutable command object carrying all data needed to create a payment.
 * Decouples the REST layer from the application use case.
 * Validated at the REST boundary before construction.
 */
public record CreatePaymentCommand(
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String customerId,
        IdempotencyKey idempotencyKey
) {}
