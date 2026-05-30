package com.yuno.payment.domain.port.outbound;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.PaymentId;

import java.util.Optional;

/**
 * Outbound (driven) port for persisting and retrieving payments.
 * The domain defines this interface; the infrastructure implements it.
 * Dependency inversion: application layer depends on this abstraction, never on JPA.
 */
public interface PaymentRepositoryPort {
    void save(Payment payment);
    Optional<Payment> findById(PaymentId id);
}
