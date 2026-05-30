package com.yuno.payment.infrastructure.observability;

import com.yuno.payment.domain.model.*;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Observability Decorators")
class ObservabilityDecoratorTest {

    private PaymentProviderPort underlying;
    private Payment payment;

    @BeforeEach
    void setUp() {
        underlying = mock(PaymentProviderPort.class);
        when(underlying.providerId()).thenReturn("PROVIDER_A");

        payment = Payment.create(
                new Money(new BigDecimal("100.00"), "USD"),
                PaymentMethod.CARD
        );
    }

    @Test
    @DisplayName("LoggingProviderDecorator delegates to underlying and returns result unchanged")
    void loggingDecoratorDelegates() {
        when(underlying.charge(payment)).thenReturn(ProviderResult.success("txn-001"));
        LoggingProviderDecorator decorator = new LoggingProviderDecorator(underlying);

        ProviderResult result = decorator.charge(payment);

        assertThat(result.success()).isTrue();
        assertThat(result.providerTransactionId()).isEqualTo("txn-001");
        verify(underlying, times(1)).charge(payment);
    }

    @Test
    @DisplayName("LoggingProviderDecorator propagates provider ID from delegate")
    void loggingDecoratorPropagatesProviderId() {
        LoggingProviderDecorator decorator = new LoggingProviderDecorator(underlying);
        assertThat(decorator.providerId()).isEqualTo("PROVIDER_A");
    }

    @Test
    @DisplayName("LoggingProviderDecorator re-throws exceptions from delegate")
    void loggingDecoratorPropagatesExceptions() {
        when(underlying.charge(payment)).thenThrow(new RuntimeException("boom"));
        LoggingProviderDecorator decorator = new LoggingProviderDecorator(underlying);

        assertThatThrownBy(() -> decorator.charge(payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }

    @Test
    @DisplayName("ObservingProviderDecorator delegates to underlying and returns result unchanged")
    void observingDecoratorDelegates() {
        when(underlying.charge(payment)).thenReturn(ProviderResult.success("txn-002"));

        // Use a no-op ObservationRegistry (won't export to Zipkin but won't fail)
        ObservingProviderDecorator decorator = new ObservingProviderDecorator(
                underlying, ObservationRegistry.NOOP
        );

        ProviderResult result = decorator.charge(payment);

        assertThat(result.success()).isTrue();
        assertThat(result.providerTransactionId()).isEqualTo("txn-002");
        verify(underlying, times(1)).charge(payment);
    }

    @Test
    @DisplayName("Full decorator stack (Observing → Logging → Underlying) works end-to-end")
    void fullDecoratorStack() {
        when(underlying.charge(payment)).thenReturn(ProviderResult.success("txn-stack"));

        PaymentProviderPort stack =
                new ObservingProviderDecorator(
                        new LoggingProviderDecorator(underlying),
                        ObservationRegistry.NOOP
                );

        ProviderResult result = stack.charge(payment);

        assertThat(result.success()).isTrue();
        assertThat(result.providerTransactionId()).isEqualTo("txn-stack");
        verify(underlying, times(1)).charge(payment);
    }
}
