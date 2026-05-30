package com.yuno.payment.domain.exception;

import com.yuno.payment.domain.model.PaymentId;
import com.yuno.payment.domain.model.PaymentStatus;

/**
 * Thrown when the Payment aggregate is asked to perform an illegal state transition.
 * E.g. transitioning a PENDING payment directly to SUCCESS.
 */
public class IllegalStateTransitionException extends RuntimeException {

    private final PaymentId paymentId;
    private final PaymentStatus from;
    private final PaymentStatus to;

    public IllegalStateTransitionException(PaymentId paymentId, PaymentStatus from, PaymentStatus to) {
        super(String.format(
                "Payment [%s] cannot transition from %s to %s", paymentId, from, to));
        this.paymentId = paymentId;
        this.from = from;
        this.to = to;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public PaymentStatus getFrom()  { return from; }
    public PaymentStatus getTo()    { return to; }
}
