package com.yuno.payment.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * HTTP response body for both POST /payments and GET /payments/{id}.
 */
public record PaymentHttpResponse(
        String paymentId,
        String status,
        String method,
        String assignedProvider,
        String providerTransactionId,
        BigDecimal amount,
        String currency,
        int attemptCount,
        Instant createdAt,
        Instant updatedAt
) {}
