package com.yuno.payment.infrastructure.web.dto;

import com.yuno.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * HTTP request body for POST /payments.
 * Bean Validation ensures bad input is rejected at the controller boundary
 * before it reaches the domain.
 */
public record CreatePaymentRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency,

        @NotNull(message = "method is required")
        PaymentMethod method,

        @NotBlank(message = "customerId is required")
        String customerId
) {}
