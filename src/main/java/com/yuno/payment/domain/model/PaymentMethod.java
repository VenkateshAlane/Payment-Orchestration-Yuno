package com.yuno.payment.domain.model;

/**
 * Supported payment methods.
 * CARD routes to Provider A; UPI routes to Provider B.
 * Adding a new method requires only a new routing strategy — no changes here.
 */
public enum PaymentMethod {
    CARD,
    UPI
}
