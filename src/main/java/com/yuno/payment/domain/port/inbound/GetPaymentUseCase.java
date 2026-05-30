package com.yuno.payment.domain.port.inbound;

import com.yuno.payment.application.command.GetPaymentQuery;
import com.yuno.payment.application.command.PaymentResult;

/**
 * Inbound (driving) port for fetching a payment by ID.
 * Implemented by GetPaymentService in the application layer.
 */
public interface GetPaymentUseCase {
    PaymentResult execute(GetPaymentQuery query);
}
