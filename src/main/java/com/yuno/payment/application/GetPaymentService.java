package com.yuno.payment.application;

import com.yuno.payment.application.command.GetPaymentQuery;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.exception.PaymentNotFoundException;
import com.yuno.payment.domain.model.Payment;
import com.yuno.payment.domain.port.inbound.GetPaymentUseCase;
import com.yuno.payment.domain.port.outbound.PaymentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrieves a payment by ID and maps it to PaymentResult.
 * Read-only transaction: hints the JDBC driver and connection pool that
 * no writes occur, enabling optimisations (e.g. replica routing).
 */
@Service
public class GetPaymentService implements GetPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPaymentService.class);

    private final PaymentRepositoryPort repository;

    public GetPaymentService(PaymentRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResult execute(GetPaymentQuery query) {
        log.info("Fetching payment id={}", query.paymentId());

        Payment payment = repository.findById(query.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(query.paymentId()));

        return PaymentResultMapper.toResult(payment);
    }
}
