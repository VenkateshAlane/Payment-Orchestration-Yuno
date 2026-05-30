package com.yuno.payment.domain.exception;

import com.yuno.payment.domain.model.PaymentId;

/**
 * Thrown when a payment lookup by ID yields no result.
 */
public class PaymentNotFoundException extends RuntimeException {

    private final PaymentId paymentId;

    public PaymentNotFoundException(PaymentId paymentId) {
        super("Payment not found: " + paymentId);
        this.paymentId = paymentId;
    }

    public PaymentId getPaymentId() { return paymentId; }
}
