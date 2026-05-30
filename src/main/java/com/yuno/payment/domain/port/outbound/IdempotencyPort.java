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
     * Atomically stores the result under the given key only if the key is absent.
     * If the key already exists (concurrent duplicate request), the existing entry
     * is preserved and this call is a no-op.
     * Implemented via Redis SET NX EX and ConcurrentHashMap.putIfAbsent.
     */
    void store(IdempotencyKey key, PaymentResult result);
}
