package com.yuno.payment.application;

import com.yuno.payment.application.command.CreatePaymentCommand;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.exception.PaymentFailedException;
import com.yuno.payment.domain.model.*;
import com.yuno.payment.domain.port.outbound.IdempotencyPort;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import com.yuno.payment.domain.port.outbound.PaymentRepositoryPort;
import com.yuno.payment.domain.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CreatePaymentService")
class CreatePaymentServiceTest {

    private PaymentRepositoryPort repository;
    private IdempotencyPort idempotency;
    private PaymentProviderPort providerA;
    private PaymentProviderPort providerB;
    private ApplicationEventPublisher eventPublisher;
    private CreatePaymentService service;

    @BeforeEach
    void setUp() {
        repository     = mock(PaymentRepositoryPort.class);
        idempotency    = mock(IdempotencyPort.class);
        providerA      = mock(PaymentProviderPort.class);
        providerB      = mock(PaymentProviderPort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        when(providerA.providerId()).thenReturn("PROVIDER_A");
        when(providerB.providerId()).thenReturn("PROVIDER_B");
        when(idempotency.find(any())).thenReturn(Optional.empty());

        ProviderRegistry registry = new ProviderRegistry(
                List.of(providerA, providerB),
                List.of(new CardRoutingStrategy(), new UpiRoutingStrategy())
        );
        RoutingEngine routingEngine = new RoutingEngine(registry);
        RetryableProviderExecutor executor = new RetryableProviderExecutor(
                3, 0L, 1.0, 0L, ms -> {}
        );

        service = new CreatePaymentService(repository, idempotency, routingEngine, executor, eventPublisher);
    }

    @Test
    @DisplayName("creates payment and returns SUCCESS result")
    void successfulPayment() {
        when(providerA.charge(any())).thenReturn(ProviderResult.success("txn-001"));

        CreatePaymentCommand command = new CreatePaymentCommand(
                new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "cust-1",
                new IdempotencyKey("key-001")
        );

        PaymentResult result = service.execute(command);

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.method()).isEqualTo("CARD");
        assertThat(result.assignedProvider()).isEqualTo("PROVIDER_A");
        assertThat(result.paymentId()).isNotBlank();
        verify(repository, times(3)).save(any()); // PENDING, PROCESSING, SUCCESS
        verify(idempotency).store(any(), eq(result));
    }

    @Test
    @DisplayName("returns cached result on duplicate idempotency key")
    void idempotencyHitReturnsCached() {
        PaymentResult cached = new PaymentResult(
                "existing-id", "SUCCESS", "CARD", "PROVIDER_A",
                new BigDecimal("100"), "USD", 1, null, null
        );
        when(idempotency.find(new IdempotencyKey("key-dup"))).thenReturn(Optional.of(cached));

        CreatePaymentCommand command = new CreatePaymentCommand(
                new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "cust-1",
                new IdempotencyKey("key-dup")
        );

        PaymentResult result = service.execute(command);

        assertThat(result).isEqualTo(cached);
        verify(repository, never()).save(any()); // no DB writes on idempotency hit
        verify(providerA, never()).charge(any());
    }

    @Test
    @DisplayName("throws PaymentFailedException and marks payment FAILED when all providers exhausted")
    void failsWhenAllProvidersExhausted() {
        when(providerA.charge(any())).thenReturn(ProviderResult.failure("down"));
        when(providerB.charge(any())).thenReturn(ProviderResult.failure("down"));

        CreatePaymentCommand command = new CreatePaymentCommand(
                new BigDecimal("50.00"), "USD", PaymentMethod.CARD, "cust-2",
                new IdempotencyKey("key-002")
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(PaymentFailedException.class);

        // PENDING + PROCESSING + FAILED = 3 saves
        verify(repository, times(3)).save(any());
        // No idempotency stored for failed payments
        verify(idempotency, never()).store(any(), any());
    }

    @Test
    @DisplayName("UPI payment routes to PROVIDER_B primary")
    void upiRoutesToProviderB() {
        when(providerB.charge(any())).thenReturn(ProviderResult.success("txn-upi-001"));

        CreatePaymentCommand command = new CreatePaymentCommand(
                new BigDecimal("200.00"), "INR", PaymentMethod.UPI, "cust-3",
                new IdempotencyKey("key-003")
        );

        PaymentResult result = service.execute(command);

        assertThat(result.assignedProvider()).isEqualTo("PROVIDER_B");
        verify(providerA, never()).charge(any());
    }

    @Test
    @DisplayName("publishes domain events for each state transition")
    void publishesDomainEvents() {
        when(providerA.charge(any())).thenReturn(ProviderResult.success("txn-001"));

        CreatePaymentCommand command = new CreatePaymentCommand(
                new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "cust-4",
                new IdempotencyKey("key-004")
        );

        service.execute(command);

        // PROCESSING event + SUCCESS event
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));
    }
}
