package com.yuno.payment.infrastructure.idempotency;

import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.IdempotencyKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryIdempotencyAdapter")
class InMemoryIdempotencyAdapterTest {

    private InMemoryIdempotencyAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryIdempotencyAdapter();
    }

    private PaymentResult sampleResult(String paymentId) {
        return new PaymentResult(
                paymentId, "SUCCESS", "CARD", "PROVIDER_A",
                new BigDecimal("100.00"), "USD", 1,
                Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("returns empty when key not stored")
    void missReturnsEmpty() {
        Optional<PaymentResult> result = adapter.find(new IdempotencyKey("unknown-key"));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns stored result on hit")
    void hitReturnsCachedResult() {
        IdempotencyKey key = new IdempotencyKey("key-001");
        PaymentResult stored = sampleResult("pay-001");
        adapter.store(key, stored);

        Optional<PaymentResult> found = adapter.find(key);
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(stored);
    }

    @Test
    @DisplayName("different keys are independent")
    void differentKeysAreIndependent() {
        adapter.store(new IdempotencyKey("key-A"), sampleResult("pay-A"));
        adapter.store(new IdempotencyKey("key-B"), sampleResult("pay-B"));

        assertThat(adapter.find(new IdempotencyKey("key-A")).get().paymentId()).isEqualTo("pay-A");
        assertThat(adapter.find(new IdempotencyKey("key-B")).get().paymentId()).isEqualTo("pay-B");
    }

    @Test
    @DisplayName("overwriting the same key replaces the result")
    void overwriteReplaces() {
        IdempotencyKey key = new IdempotencyKey("key-overwrite");
        adapter.store(key, sampleResult("pay-old"));
        adapter.store(key, sampleResult("pay-new"));

        assertThat(adapter.find(key).get().paymentId()).isEqualTo("pay-new");
    }
}
