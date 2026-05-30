package com.yuno.payment.domain;

import com.yuno.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yuno.payment.domain.model.*;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import com.yuno.payment.domain.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RoutingEngine")
class RoutingEngineTest {

    private PaymentProviderPort providerA;
    private PaymentProviderPort providerB;
    private RoutingEngine routingEngine;

    @BeforeEach
    void setUp() {
        providerA = mock(PaymentProviderPort.class);
        providerB = mock(PaymentProviderPort.class);
        when(providerA.providerId()).thenReturn("PROVIDER_A");
        when(providerB.providerId()).thenReturn("PROVIDER_B");

        ProviderRegistry registry = new ProviderRegistry(
                List.of(providerA, providerB),
                List.of(new CardRoutingStrategy(), new UpiRoutingStrategy())
        );
        routingEngine = new RoutingEngine(registry);
    }

    @Test
    @DisplayName("CARD payment resolves to [PROVIDER_A, PROVIDER_B]")
    void cardResolvesToProviderA() {
        Payment payment = Payment.create(new Money(new BigDecimal("100"), "USD"), PaymentMethod.CARD);
        List<PaymentProviderPort> chain = routingEngine.resolveChain(payment);
        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).providerId()).isEqualTo("PROVIDER_A");
        assertThat(chain.get(1).providerId()).isEqualTo("PROVIDER_B");
    }

    @Test
    @DisplayName("UPI payment resolves to [PROVIDER_B, PROVIDER_A]")
    void upiResolvesToProviderB() {
        Payment payment = Payment.create(new Money(new BigDecimal("200"), "INR"), PaymentMethod.UPI);
        List<PaymentProviderPort> chain = routingEngine.resolveChain(payment);
        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).providerId()).isEqualTo("PROVIDER_B");
        assertThat(chain.get(1).providerId()).isEqualTo("PROVIDER_A");
    }

    @Test
    @DisplayName("throws UnsupportedPaymentMethodException when no strategy registered")
    void throwsOnUnknownMethod() {
        ProviderRegistry emptyRegistry = new ProviderRegistry(
                List.of(providerA, providerB),
                List.of() // no strategies registered
        );
        RoutingEngine engine = new RoutingEngine(emptyRegistry);
        Payment payment = Payment.create(new Money(new BigDecimal("100"), "USD"), PaymentMethod.CARD);

        assertThatThrownBy(() -> engine.resolveChain(payment))
                .isInstanceOf(UnsupportedPaymentMethodException.class)
                .hasMessageContaining("CARD");
    }

    @Test
    @DisplayName("throws when a provider ID in the strategy has no registered adapter")
    void throwsOnMissingAdapter() {
        // Strategy says PROVIDER_A but we only register PROVIDER_B
        ProviderRegistry badRegistry = new ProviderRegistry(
                List.of(providerB),                    // PROVIDER_A is missing
                List.of(new CardRoutingStrategy())     // CARD → [PROVIDER_A, PROVIDER_B]
        );
        RoutingEngine engine = new RoutingEngine(badRegistry);
        Payment payment = Payment.create(new Money(new BigDecimal("100"), "USD"), PaymentMethod.CARD);

        assertThatThrownBy(() -> engine.resolveChain(payment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROVIDER_A");
    }
}
