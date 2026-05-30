package com.yuno.payment.infrastructure.persistence;

import com.yuno.payment.domain.model.*;
import org.springframework.stereotype.Component;

/**
 * Translates between the Payment domain model and PaymentEntity (JPA).
 * Neither domain model nor JPA entity references the other — this mapper bridges them.
 */
@Component
public class PaymentEntityMapper {

    /**
     * Converts a Payment domain object into a new or updated PaymentEntity.
     * Always builds a fresh entity reflecting the current aggregate state.
     */
    public PaymentEntity toEntity(Payment payment) {
        return new PaymentEntity(
                payment.getId().value(),
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getAssignedProvider(),
                payment.getProviderTransactionId(),
                payment.getAttemptCount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    /**
     * Reconstitutes a Payment domain object from a stored PaymentEntity.
     */
    public Payment toDomain(PaymentEntity entity) {
        return Payment.reconstitute(
                new PaymentId(entity.getId()),
                new Money(entity.getAmount(), entity.getCurrency()),
                PaymentMethod.valueOf(entity.getMethod()),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getAssignedProvider(),
                entity.getProviderTransactionId(),
                entity.getAttemptCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
