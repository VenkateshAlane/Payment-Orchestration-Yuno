package com.yuno.payment.infrastructure.idempotency;

import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.IdempotencyKey;
import com.yuno.payment.domain.port.outbound.IdempotencyPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Null Object pattern: in-memory IdempotencyPort for unit tests.
 * No Redis required — tests run fast and in isolation.
 *
 * Thread-safe atomic operations:
 *   find()  → ConcurrentHashMap.get()       (read, no lock)
 *   store() → ConcurrentHashMap.putIfAbsent() (atomic write — first writer wins)
 */
public class InMemoryIdempotencyAdapter implements IdempotencyPort {

    private final Map<String, PaymentResult> store = new ConcurrentHashMap<>();

    @Override
    public Optional<PaymentResult> find(IdempotencyKey key) {
        return Optional.ofNullable(store.get(key.value()));
    }

    @Override
    public void store(IdempotencyKey key, PaymentResult result) {
        store.putIfAbsent(key.value(), result);
    }

    /** Test helper — check how many results are stored. */
    public int size() {
        return store.size();
    }
}
