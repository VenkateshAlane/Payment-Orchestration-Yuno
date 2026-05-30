package com.yuno.payment.infrastructure.web;

import com.yuno.payment.application.command.CreatePaymentCommand;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.IdempotencyKey;
import com.yuno.payment.infrastructure.web.dto.CreatePaymentRequest;
import com.yuno.payment.infrastructure.web.dto.PaymentHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Maps between HTTP DTOs and application-layer objects.
 * Keeps the controller thin — it handles HTTP concerns only.
 */
@Component
public class PaymentMapper {

    public CreatePaymentCommand toCommand(CreatePaymentRequest request, String idempotencyKeyHeader) {
        return new CreatePaymentCommand(
                request.amount(),
                request.currency(),
                request.method(),
                request.customerId(),
                new IdempotencyKey(idempotencyKeyHeader)
        );
    }

    public PaymentHttpResponse toHttpResponse(PaymentResult result) {
        return new PaymentHttpResponse(
                result.paymentId(),
                result.status(),
                result.method(),
                result.assignedProvider(),
                result.amount(),
                result.currency(),
                result.attemptCount(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
