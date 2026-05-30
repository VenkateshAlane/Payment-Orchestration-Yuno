package com.yuno.payment.infrastructure.web.dto;

import java.time.Instant;

/**
 * Standard error response body.
 * Never leaks stack traces or internal details to the client (OWASP).
 */
public record ErrorResponse(
        String error,
        String message,
        int status,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String message, int status) {
        return new ErrorResponse(error, message, status, Instant.now());
    }
}
