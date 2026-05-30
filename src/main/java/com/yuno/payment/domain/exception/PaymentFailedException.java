package com.yuno.payment.domain.exception;

import com.yuno.payment.domain.model.PaymentId;

/**
 * Thrown when all providers in the failover chain are exhausted without success.
 */
public class PaymentFailedException extends RuntimeException {

    private final PaymentId paymentId;

    public PaymentFailedException(PaymentId paymentId) {
        super("All providers exhausted for payment: " + paymentId);
        this.paymentId = paymentId;
    }

    public PaymentId getPaymentId() { return paymentId; }
}
