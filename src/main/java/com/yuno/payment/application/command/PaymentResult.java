package com.yuno.payment.application.command;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable result returned by use cases and stored in the idempotency cache.
 * Lives in the application layer — not a JPA entity, not an HTTP DTO.
 * The web layer maps this to its own HTTP response shape.
 */
public record PaymentResult(
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
