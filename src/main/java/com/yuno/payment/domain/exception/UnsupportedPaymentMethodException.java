package com.yuno.payment.domain.exception;

import com.yuno.payment.domain.model.PaymentMethod;

/**
 * Thrown when no routing strategy is registered for the given payment method.
 */
public class UnsupportedPaymentMethodException extends RuntimeException {

    private final PaymentMethod method;

    public UnsupportedPaymentMethodException(PaymentMethod method) {
        super("No routing strategy registered for payment method: " + method);
        this.method = method;
    }

    public PaymentMethod getMethod() { return method; }
}
