package com.yuno.payment.domain.model;

/**
 * The outcome returned by a payment provider after a charge attempt.
 * Use the static factories to construct — never build directly.
 *
 * Invariant: success=true  → providerTransactionId is set, failureReason is null.
 *            success=false → failureReason is set,          providerTransactionId is null.
 */
public record ProviderResult(
        boolean success,
        String providerTransactionId,
        String failureReason
) {

    // Compact constructor enforces mutual exclusivity
    public ProviderResult {
        if (success && (providerTransactionId == null || providerTransactionId.isBlank())) {
            throw new IllegalArgumentException("Successful ProviderResult must have a providerTransactionId");
        }
        if (!success && (failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException("Failed ProviderResult must have a failureReason");
        }
        if (success && failureReason != null) {
            throw new IllegalArgumentException("Successful ProviderResult must not have a failureReason");
        }
        if (!success && providerTransactionId != null) {
            throw new IllegalArgumentException("Failed ProviderResult must not have a providerTransactionId");
        }
    }

    public static ProviderResult success(String providerTransactionId) {
        return new ProviderResult(true, providerTransactionId, null);
    }

    public static ProviderResult failure(String reason) {
        return new ProviderResult(false, null, reason);
    }
}
