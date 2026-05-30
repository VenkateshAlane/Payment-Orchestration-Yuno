package com.yuno.payment.infrastructure.provider;

import com.yuno.payment.domain.model.*;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Provider Adapters")
class ProviderAdapterTest {

    private Payment buildPayment(PaymentMethod method) {
        return Payment.create(new Money(new BigDecimal("100.00"), "USD"), method);
    }

    @Nested
    @DisplayName("ProviderAAdapter")
    class ProviderATests {

        @Test
        @DisplayName("providerId returns PROVIDER_A")
        void providerId() {
            ProviderAAdapter adapter = new ProviderAAdapter(new ProviderSimulator(0.0, 0.0));
            assertThat(adapter.providerId()).isEqualTo("PROVIDER_A");
        }

        @Test
        @DisplayName("charge returns SUCCESS when simulator succeeds (0% failure rate)")
        void chargeSucceeds() {
            ProviderAAdapter adapter = new ProviderAAdapter(new ProviderSimulator(0.0, 0.0));
            ProviderResult result = adapter.charge(buildPayment(PaymentMethod.CARD));
            assertThat(result.success()).isTrue();
            assertThat(result.providerTransactionId()).startsWith("PROVIDER_A-");
        }

        @Test
        @DisplayName("charge returns FAILURE when simulator always fails (100% failure rate)")
        void chargeFails() {
            ProviderAAdapter adapter = new ProviderAAdapter(new ProviderSimulator(1.0, 1.0));
            ProviderResult result = adapter.charge(buildPayment(PaymentMethod.CARD));
            assertThat(result.success()).isFalse();
            assertThat(result.failureReason()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("ProviderBAdapter")
    class ProviderBTests {

        @Test
        @DisplayName("providerId returns PROVIDER_B")
        void providerId() {
            ProviderBAdapter adapter = new ProviderBAdapter(new ProviderSimulator(0.0, 0.0));
            assertThat(adapter.providerId()).isEqualTo("PROVIDER_B");
        }

        @Test
        @DisplayName("charge returns SUCCESS when simulator succeeds (0% failure rate)")
        void chargeSucceeds() {
            ProviderBAdapter adapter = new ProviderBAdapter(new ProviderSimulator(0.0, 0.0));
            ProviderResult result = adapter.charge(buildPayment(PaymentMethod.UPI));
            assertThat(result.success()).isTrue();
            assertThat(result.providerTransactionId()).startsWith("PROVIDER_B-");
        }

        @Test
        @DisplayName("charge returns FAILURE when simulator always fails (100% failure rate)")
        void chargeFails() {
            ProviderBAdapter adapter = new ProviderBAdapter(new ProviderSimulator(1.0, 1.0));
            ProviderResult result = adapter.charge(buildPayment(PaymentMethod.UPI));
            assertThat(result.success()).isFalse();
        }
    }

    @Nested
    @DisplayName("ProviderSimulator")
    class SimulatorTests {

        @Test
        @DisplayName("generates unique transaction IDs on each success")
        void uniqueTransactionIds() {
            ProviderSimulator simulator = new ProviderSimulator(0.0, 0.0);
            PaymentProviderPort adapter = new ProviderAAdapter(simulator);
            Payment payment = buildPayment(PaymentMethod.CARD);

            String txn1 = adapter.charge(payment).providerTransactionId();
            String txn2 = adapter.charge(payment).providerTransactionId();

            assertThat(txn1).isNotEqualTo(txn2);
        }
    }
}
