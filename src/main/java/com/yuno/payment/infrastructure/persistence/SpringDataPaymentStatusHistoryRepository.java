package com.yuno.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for payment status history.
 * Used by PaymentStatusHistoryPersister to append audit entries.
 */
public interface SpringDataPaymentStatusHistoryRepository
        extends JpaRepository<PaymentStatusHistoryEntity, UUID> {

    List<PaymentStatusHistoryEntity> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}
