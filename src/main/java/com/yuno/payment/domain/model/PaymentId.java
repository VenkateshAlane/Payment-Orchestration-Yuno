package com.yuno.payment.domain.model;

import java.util.UUID;

/**
 * Value object wrapping the payment's unique identifier.
 * Self-validating: construction always produces a valid ID.
 */
public record PaymentId(UUID value) {

    public PaymentId {
        if (value == null) {
            throw new IllegalArgumentException("PaymentId value must not be null");
        }
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId of(String raw) {
        try {
            return new PaymentId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PaymentId format: " + raw);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
