package com.yuno.payment.domain.port.inbound;

import com.yuno.payment.application.command.CreatePaymentCommand;
import com.yuno.payment.application.command.PaymentResult;

/**
 * Inbound (driving) port for creating a payment.
 * ISP: one interface per use case — nothing else bleeds in.
 * Implemented by CreatePaymentService in the application layer.
 */
public interface CreatePaymentUseCase {
    PaymentResult execute(CreatePaymentCommand command);
}
