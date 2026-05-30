package com.yuno.payment.infrastructure.persistence;

import com.yuno.payment.domain.model.*;
import com.yuno.payment.domain.port.outbound.PaymentRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the persistence adapter.
 * Uses a real PostgreSQL container — no H2 mocking.
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaPaymentRepositoryAdapter.class, PaymentEntityMapper.class})
@Sql(scripts = {
    "classpath:db/migration/V1__create_payments_table.sql",
    "classpath:db/migration/V2__add_provider_transaction_id.sql",
    "classpath:db/migration/V3__add_optimistic_locking_version.sql",
    "classpath:db/migration/V4__create_payment_status_history.sql"
})
@DisplayName("JpaPaymentRepositoryAdapter")
class JpaPaymentRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private PaymentRepositoryPort repository;

    private Payment buildCardPayment() {
        return Payment.create(new Money(new BigDecimal("150.00"), "USD"), PaymentMethod.CARD);
    }

    @Test
    @DisplayName("saves and retrieves a PENDING payment")
    void saveAndFindPending() {
        Payment payment = buildCardPayment();
        repository.save(payment);

        Optional<Payment> found = repository.findById(payment.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(payment.getId());
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.get().getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(found.get().getAmount().amount()).isEqualByComparingTo("150.00");
        assertThat(found.get().getAmount().currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("persists status transition to SUCCESS")
    void persistsSuccessTransition() {
        Payment payment = buildCardPayment();
        repository.save(payment);

        payment.markProcessing();
        repository.save(payment);
        payment.markSuccess("PROVIDER_A", "txn-001");
        repository.save(payment);

        Payment found = repository.findById(payment.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(found.getAssignedProvider()).isEqualTo("PROVIDER_A");
    }

    @Test
    @DisplayName("persists status transition to FAILED")
    void persistsFailedTransition() {
        Payment payment = buildCardPayment();
        repository.save(payment);
        payment.markProcessing();
        repository.save(payment);
        payment.markFailed();
        repository.save(payment);

        Payment found = repository.findById(payment.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("persists attempt count increments")
    void persistsAttemptCount() {
        Payment payment = buildCardPayment();
        payment.incrementAttemptCount();
        payment.incrementAttemptCount();
        repository.save(payment);

        Payment found = repository.findById(payment.getId()).orElseThrow();
        assertThat(found.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("returns empty Optional for unknown ID")
    void returnsEmptyForUnknownId() {
        Optional<Payment> found = repository.findById(PaymentId.generate());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("overwrites existing record on repeated save (upsert semantics)")
    void overwritesOnRepeatSave() {
        Payment payment = buildCardPayment();
        repository.save(payment);

        payment.markProcessing();
        payment.incrementAttemptCount();
        repository.save(payment);

        Payment found = repository.findById(payment.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(found.getAttemptCount()).isEqualTo(1);
    }
}
