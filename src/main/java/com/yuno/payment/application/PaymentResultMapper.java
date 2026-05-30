package com.yuno.payment.application;

import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.Payment;

/**
 * Maps a Payment aggregate to the PaymentResult application DTO.
 *
 * Extracted to eliminate the duplication between CreatePaymentService and
 * GetPaymentService, both of which need the same projection.
 * Package-private — only application-layer classes use this.
 */
final class PaymentResultMapper {

    private PaymentResultMapper() {}

    static PaymentResult toResult(Payment payment) {
        return new PaymentResult(
                payment.getId().toString(),
                payment.getStatus().name(),
                payment.getMethod().name(),
                payment.getAssignedProvider(),
                payment.getProviderTransactionId(),
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getAttemptCount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
