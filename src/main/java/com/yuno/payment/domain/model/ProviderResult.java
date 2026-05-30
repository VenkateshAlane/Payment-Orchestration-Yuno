package com.yuno.payment.domain.model;

/**
 * The outcome returned by a payment provider after a charge attempt.
 * Use the static factories to construct — never build directly.
 */
public record ProviderResult(
        boolean success,
        String providerTransactionId,
        String failureReason
) {

    public static ProviderResult success(String providerTransactionId) {
        return new ProviderResult(true, providerTransactionId, null);
    }

    public static ProviderResult failure(String reason) {
        return new ProviderResult(false, null, reason);
    }
}
