package com.yuno.payment.application;

import com.yuno.payment.domain.exception.PaymentFailedException;
import com.yuno.payment.domain.model.*;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RetryableProviderExecutor")
class RetryableProviderExecutorTest {

    private PaymentProviderPort providerA;
    private PaymentProviderPort providerB;
    private RetryableProviderExecutor executor;
    private Payment payment;

    @BeforeEach
    void setUp() {
        providerA = mock(PaymentProviderPort.class);
        providerB = mock(PaymentProviderPort.class);
        when(providerA.providerId()).thenReturn("PROVIDER_A");
        when(providerB.providerId()).thenReturn("PROVIDER_B");

        // Sleeper is a no-op in tests — no real delays
        executor = new RetryableProviderExecutor(3, 0L, 1.0, 0L, ms -> {});

        payment = Payment.create(
                new Money(new BigDecimal("100.00"), "USD"),
                PaymentMethod.CARD
        );
    }

    @Test
    @DisplayName("returns ExecutionResult with winning provider on first attempt")
    void succeedsOnFirstAttempt() {
        when(providerA.charge(payment))
                .thenReturn(ProviderResult.success("txn-001"));

        ExecutionResult result = executor.execute(payment, List.of(providerA, providerB));

        assertThat(result.winningProviderId()).isEqualTo("PROVIDER_A");
        assertThat(result.providerTransactionId()).isEqualTo("txn-001");
        verify(providerA, times(1)).charge(payment);
        verify(providerB, never()).charge(payment);
    }

    @Test
    @DisplayName("retries same provider on failure then succeeds")
    void retriesOnFailureThenSucceeds() {
        when(providerA.charge(payment))
                .thenReturn(ProviderResult.failure("timeout"))
                .thenReturn(ProviderResult.success("txn-002"));

        ExecutionResult result = executor.execute(payment, List.of(providerA, providerB));

        assertThat(result.winningProviderId()).isEqualTo("PROVIDER_A");
        verify(providerA, times(2)).charge(payment);
        verify(providerB, never()).charge(payment);
    }

    @Test
    @DisplayName("fails over to second provider when first is exhausted")
    void failsOverToSecondProvider() {
        when(providerA.charge(payment)).thenReturn(ProviderResult.failure("down"));
        when(providerB.charge(payment)).thenReturn(ProviderResult.success("txn-003"));

        ExecutionResult result = executor.execute(payment, List.of(providerA, providerB));

        assertThat(result.winningProviderId()).isEqualTo("PROVIDER_B");
        verify(providerA, times(3)).charge(payment); // exhausted all 3 attempts
        verify(providerB, times(1)).charge(payment);
    }

    @Test
    @DisplayName("throws PaymentFailedException when all providers exhausted")
    void throwsWhenAllExhausted() {
        when(providerA.charge(payment)).thenReturn(ProviderResult.failure("down"));
        when(providerB.charge(payment)).thenReturn(ProviderResult.failure("down"));

        assertThatThrownBy(() -> executor.execute(payment, List.of(providerA, providerB)))
                .isInstanceOf(PaymentFailedException.class);

        verify(providerA, times(3)).charge(payment);
        verify(providerB, times(3)).charge(payment);
    }

    @Test
    @DisplayName("handles provider exception as a failed attempt and retries")
    void treatsExceptionAsFailure() {
        when(providerA.charge(payment))
                .thenThrow(new RuntimeException("connection reset"))
                .thenReturn(ProviderResult.success("txn-004"));

        ExecutionResult result = executor.execute(payment, List.of(providerA, providerB));

        assertThat(result.winningProviderId()).isEqualTo("PROVIDER_A");
        verify(providerA, times(2)).charge(payment);
    }

    @Test
    @DisplayName("tracks total attempt count across all providers")
    void tracksAttemptCount() {
        when(providerA.charge(payment)).thenReturn(ProviderResult.failure("down"));
        when(providerB.charge(payment)).thenReturn(ProviderResult.success("txn-005"));

        executor.execute(payment, List.of(providerA, providerB));

        // 3 attempts on providerA + 1 on providerB = 4
        assertThat(payment.getAttemptCount()).isEqualTo(4);
    }
}
