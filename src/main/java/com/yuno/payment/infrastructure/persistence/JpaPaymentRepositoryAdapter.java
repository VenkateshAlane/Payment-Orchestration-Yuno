package com.yuno.payment.infrastructure.persistence;

import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.model.PaymentId;
import com.yuno.payment.domain.port.outbound.PaymentRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Outbound adapter: implements PaymentRepositoryPort using Spring Data JPA.
 *
 * The application layer calls PaymentRepositoryPort — it has no idea this
 * adapter or JPA even exists. Full DIP compliance.
 *
 * Transactions are owned here, not by the service. This keeps each DB write
 * in its own short-lived transaction so provider HTTP calls (with retry
 * backoff) never hold an open DB connection.
 */
@Repository
public class JpaPaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final SpringDataPaymentRepository jpaRepository;
    private final PaymentEntityMapper mapper;

    public JpaPaymentRepositoryAdapter(SpringDataPaymentRepository jpaRepository,
                                       PaymentEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper        = mapper;
    }

    @Override
    @Transactional
    public void save(Payment payment) {
        PaymentEntity entity = mapper.toEntity(payment);
        jpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(PaymentId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
    }
}
