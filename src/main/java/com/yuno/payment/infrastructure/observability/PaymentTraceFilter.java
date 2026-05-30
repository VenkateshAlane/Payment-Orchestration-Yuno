package com.yuno.payment.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that seeds MDC with payment-specific correlation fields.
 *
 * Every log line in the request thread will automatically carry:
 *   idempotencyKey  — extracted from the Idempotency-Key header
 *
 * Micrometer adds traceId and spanId to MDC automatically when the
 * Brave bridge is on the classpath.
 *
 * The filter runs once per request (OncePerRequestFilter) and clears
 * MDC on completion to prevent leakage across threads.
 */
public class PaymentTraceFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String MDC_IDEMPOTENCY_KEY    = "idempotencyKey";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            MDC.put(MDC_IDEMPOTENCY_KEY, idempotencyKey);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_IDEMPOTENCY_KEY);
        }
    }
}
