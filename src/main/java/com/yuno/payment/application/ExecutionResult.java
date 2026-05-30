package com.yuno.payment.application;

/**
 * Carries the outcome of a RetryableProviderExecutor run.
 * Includes which provider actually succeeded so the aggregate can record it.
 */
public record ExecutionResult(String winningProviderId, String providerTransactionId) {}
