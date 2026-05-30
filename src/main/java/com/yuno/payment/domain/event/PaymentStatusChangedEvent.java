package com.yuno.payment.domain.event;

import com.yuno.payment.domain.model.PaymentId;
import com.yuno.payment.domain.model.PaymentStatus;

import java.time.Instant;

/**
 * Domain event emitted every time a Payment transitions between statuses.
 * Consumers (logging, audit, future webhooks) subscribe via Spring's ApplicationEventPublisher.
 */
public record PaymentStatusChangedEvent(
        PaymentId paymentId,
        PaymentStatus from,
        PaymentStatus to,
        String provider,
        Instant occurredAt
) {}
