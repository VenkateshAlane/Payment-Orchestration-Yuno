package com.yuno.payment.domain;

import com.yuno.payment.domain.event.PaymentStatusChangedEvent;
import com.yuno.payment.domain.exception.IllegalStateTransitionException;
import com.yuno.payment.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Payment aggregate.
 * No Spring context — pure domain logic only.
 */
@DisplayName("Payment Aggregate")
class PaymentTest {

    private static final Money VALID_MONEY  = new Money(new BigDecimal("100.00"), "USD");
    private static final Money VALID_MONEY2 = new Money(new BigDecimal("50.00"), "EUR");

    // ── Creation ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates payment in PENDING status")
        void createStartsPending() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("generates a non-null ID")
        void createGeneratesId() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            assertThat(payment.getId()).isNotNull();
            assertThat(payment.getId().value()).isNotNull();
        }

        @Test
        @DisplayName("sets timestamps")
        void createSetsTimestamps() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            assertThat(payment.getCreatedAt()).isNotNull();
            assertThat(payment.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("starts with zero attempt count")
        void createStartsWithZeroAttempts() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            assertThat(payment.getAttemptCount()).isZero();
        }

        @Test
        @DisplayName("rejects null money")
        void createRejectsNullMoney() {
            assertThatThrownBy(() -> Payment.create(null, PaymentMethod.CARD))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null payment method")
        void createRejectsNullMethod() {
            assertThatThrownBy(() -> Payment.create(VALID_MONEY, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── State transitions ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("markProcessing()")
    class MarkProcessing {

        @Test
        @DisplayName("transitions PENDING → PROCESSING")
        void pendingToProcessing() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("returns domain event with correct statuses")
        void returnsEvent() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            PaymentStatusChangedEvent event = payment.markProcessing();
            assertThat(event.from()).isEqualTo(PaymentStatus.PENDING);
            assertThat(event.to()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(event.paymentId()).isEqualTo(payment.getId());
        }

        @Test
        @DisplayName("throws on illegal transition: PROCESSING → PROCESSING")
        void throwsOnIllegalTransition() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            assertThatThrownBy(payment::markProcessing)
                    .isInstanceOf(IllegalStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("markSuccess()")
    class MarkSuccess {

        @Test
        @DisplayName("transitions PROCESSING → SUCCESS")
        void processingToSuccess() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            payment.markSuccess("PROVIDER_A");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("records the winning provider")
        void recordsProvider() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            payment.markSuccess("PROVIDER_A");
            assertThat(payment.getAssignedProvider()).isEqualTo("PROVIDER_A");
        }

        @Test
        @DisplayName("returns domain event")
        void returnsEvent() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            PaymentStatusChangedEvent event = payment.markSuccess("PROVIDER_A");
            assertThat(event.from()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(event.to()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("throws on illegal transition: PENDING → SUCCESS")
        void throwsFromPending() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            assertThatThrownBy(() -> payment.markSuccess("PROVIDER_A"))
                    .isInstanceOf(IllegalStateTransitionException.class);
        }

        @Test
        @DisplayName("throws on re-transition: SUCCESS → SUCCESS")
        void throwsFromSuccess() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            payment.markSuccess("PROVIDER_A");
            assertThatThrownBy(() -> payment.markSuccess("PROVIDER_A"))
                    .isInstanceOf(IllegalStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("markFailed()")
    class MarkFailed {

        @Test
        @DisplayName("transitions PROCESSING → FAILED")
        void processingToFailed() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            payment.markFailed();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("throws on illegal transition: PENDING → FAILED")
        void throwsFromPending() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            assertThatThrownBy(payment::markFailed)
                    .isInstanceOf(IllegalStateTransitionException.class);
        }

        @Test
        @DisplayName("throws on re-transition: FAILED → FAILED")
        void throwsFromFailed() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.markProcessing();
            payment.markFailed();
            assertThatThrownBy(payment::markFailed)
                    .isInstanceOf(IllegalStateTransitionException.class);
        }
    }

    // ── Attempt count ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("incrementAttemptCount()")
    class AttemptCount {

        @Test
        @DisplayName("increments on each call")
        void increments() {
            Payment payment = Payment.create(VALID_MONEY, PaymentMethod.CARD);
            payment.incrementAttemptCount();
            payment.incrementAttemptCount();
            assertThat(payment.getAttemptCount()).isEqualTo(2);
        }
    }

    // ── Value objects ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Money")
    class MoneyTests {

        @Test
        @DisplayName("rejects zero amount")
        void rejectsZero() {
            assertThatThrownBy(() -> new Money(BigDecimal.ZERO, "USD"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects negative amount")
        void rejectsNegative() {
            assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "USD"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects invalid currency code")
        void rejectsInvalidCurrency() {
            assertThatThrownBy(() -> new Money(new BigDecimal("100"), "XYZ"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("normalises currency to uppercase")
        void normalisesUppercase() {
            Money money = new Money(new BigDecimal("100"), "usd");
            assertThat(money.currency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("IdempotencyKey")
    class IdempotencyKeyTests {

        @Test
        @DisplayName("rejects blank key")
        void rejectsBlank() {
            assertThatThrownBy(() -> new IdempotencyKey("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null key")
        void rejectsNull() {
            assertThatThrownBy(() -> new IdempotencyKey(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("PaymentId")
    class PaymentIdTests {

        @Test
        @DisplayName("of() rejects malformed UUID string")
        void rejectsMalformed() {
            assertThatThrownBy(() -> PaymentId.of("not-a-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("generate() produces unique IDs")
        void generateIsUnique() {
            PaymentId a = PaymentId.generate();
            PaymentId b = PaymentId.generate();
            assertThat(a).isNotEqualTo(b);
        }
    }
}
