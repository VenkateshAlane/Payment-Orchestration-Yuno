package com.yuno.payment.domain.model;

/**
 * Value object representing the client-supplied idempotency key.
 * Validated at the REST boundary before entering the application.
 */
public record IdempotencyKey(String value) {

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be null or blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
