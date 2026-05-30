package com.yuno.payment.application.command;

import com.yuno.payment.domain.model.PaymentId;

/**
 * Immutable query object for fetching a single payment by ID.
 */
public record GetPaymentQuery(PaymentId paymentId) {}
