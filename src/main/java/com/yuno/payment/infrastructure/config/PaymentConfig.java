package com.yuno.payment.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuno.payment.application.RetryableProviderExecutor;
import com.yuno.payment.domain.port.outbound.IdempotencyPort;
import com.yuno.payment.domain.port.outbound.PaymentProviderPort;
import com.yuno.payment.domain.service.*;
import com.yuno.payment.infrastructure.idempotency.RedisIdempotencyAdapter;
import com.yuno.payment.infrastructure.observability.CircuitBreakerProviderDecorator;
import com.yuno.payment.infrastructure.observability.LoggingProviderDecorator;
import com.yuno.payment.infrastructure.observability.ObservingProviderDecorator;
import com.yuno.payment.infrastructure.observability.PaymentTraceFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.yuno.payment.infrastructure.provider.ProviderAAdapter;
import com.yuno.payment.infrastructure.provider.ProviderBAdapter;
import com.yuno.payment.infrastructure.provider.ProviderSimulator;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Spring configuration: wires together the hexagonal architecture.
 *
 * Domain and application classes are instantiated here — they have no
 * @Component annotations, keeping them free of Spring coupling.
 *
 * Infrastructure adapters are @Repository / @Component so Spring
 * can find them via component scan; they're injected via constructor args.
 */
@Configuration
public class PaymentConfig {

    // ── Provider Simulator ────────────────────────────────────────────────────

    @Bean
    public ProviderSimulator providerSimulator(
            @Value("${provider.a.failure-rate:0.3}") double providerAFailureRate,
            @Value("${provider.b.failure-rate:0.2}") double providerBFailureRate) {
        return new ProviderSimulator(providerAFailureRate, providerBFailureRate);
    }

    // ── Provider Adapters wrapped in decorator stack ───────────────────────────
    //    ObservingProviderDecorator → LoggingProviderDecorator
    //    → CircuitBreakerProviderDecorator → ActualAdapter

    @Bean("providerA")
    public PaymentProviderPort providerAAdapter(ProviderSimulator simulator,
                                                ObservationRegistry observationRegistry,
                                                CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ObservingProviderDecorator(
                new LoggingProviderDecorator(
                        new CircuitBreakerProviderDecorator(
                                new ProviderAAdapter(simulator), circuitBreakerRegistry)),
                observationRegistry
        );
    }

    @Bean("providerB")
    public PaymentProviderPort providerBAdapter(ProviderSimulator simulator,
                                                ObservationRegistry observationRegistry,
                                                CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ObservingProviderDecorator(
                new LoggingProviderDecorator(
                        new CircuitBreakerProviderDecorator(
                                new ProviderBAdapter(simulator), circuitBreakerRegistry)),
                observationRegistry
        );
    }

    // ── Routing ───────────────────────────────────────────────────────────────

    @Bean
    public CardRoutingStrategy cardRoutingStrategy() {
        return new CardRoutingStrategy();
    }

    @Bean
    public UpiRoutingStrategy upiRoutingStrategy() {
        return new UpiRoutingStrategy();
    }

    @Bean
    public ProviderRegistry providerRegistry(List<PaymentProviderPort> providers,
                                             List<PaymentRoutingStrategy> strategies) {
        return new ProviderRegistry(providers, strategies);
    }

    @Bean
    public RoutingEngine routingEngine(ProviderRegistry registry) {
        return new RoutingEngine(registry);
    }

    // ── Retry executor ────────────────────────────────────────────────────────

    @Bean
    public RetryableProviderExecutor retryableProviderExecutor(
            @Value("${payment.retry.max-attempts-per-provider:3}") int maxAttempts,
            @Value("${payment.retry.initial-backoff-ms:100}")      long initialBackoff,
            @Value("${payment.retry.backoff-multiplier:2.0}")      double multiplier,
            @Value("${payment.retry.max-backoff-ms:2000}")         long maxBackoff) {
        return new RetryableProviderExecutor(
                maxAttempts, initialBackoff, multiplier, maxBackoff,
                Thread::sleep   // real sleep in production
        );
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Bean
    public IdempotencyPort idempotencyPort(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${idempotency.ttl-hours:24}") long ttlHours) {
        return new RedisIdempotencyAdapter(redisTemplate, objectMapper, Duration.ofHours(ttlHours));
    }

    // ── Observability ─────────────────────────────────────────────────────────

    @Bean
    public FilterRegistrationBean<PaymentTraceFilter> paymentTraceFilter() {
        FilterRegistrationBean<PaymentTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PaymentTraceFilter());
        // "/payments" covers POST /payments; "/payments/*" covers GET /payments/{id}
        registration.addUrlPatterns("/payments", "/payments/*");
        registration.setOrder(1);
        return registration;
    }
}
