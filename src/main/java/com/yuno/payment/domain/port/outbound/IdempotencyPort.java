package com.yuno.payment.domain.port.outbound;

import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.IdempotencyKey;

import java.util.Optional;

/**
 * Outbound (driven) port for idempotency checking and storage.
 * Backed by Redis in production, by an in-memory map in tests.
 * GRASP Indirection: adds a layer so the application never couples to Redis directly.
 */
public interface IdempotencyPort {

    /**
     * Returns the cached PaymentResult for the given key, if it exists.
     */
    Optional<PaymentResult> find(IdempotencyKey key);

    /**
     * Stores the result under the given key with a configured TTL.
     */
    void store(IdempotencyKey key, PaymentResult result);
}
