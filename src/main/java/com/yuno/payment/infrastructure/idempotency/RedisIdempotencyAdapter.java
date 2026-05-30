package com.yuno.payment.infrastructure.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.IdempotencyKey;
import com.yuno.payment.domain.port.outbound.IdempotencyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed IdempotencyPort.
 *
 * Key format:  idempotency:{idempotencyKey}
 * Value:       JSON-serialised PaymentResult
 * TTL:         configurable, defaults to 24 hours
 *
 * GRASP Indirection: the application layer calls IdempotencyPort and has
 * zero knowledge of Redis, JSON serialisation, or TTL configuration.
 *
 * Atomicity: store() uses setIfAbsent (Redis SET NX EX) so the first writer
 * wins. Concurrent duplicate requests that both pass find() will both process
 * the payment, but only the first completion writes the cached result.
 */
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyAdapter.class);
    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper,
                                   Duration ttl) {
        this.redisTemplate  = redisTemplate;
        this.objectMapper   = objectMapper;
        this.ttl            = ttl;
    }

    @Override
    public Optional<PaymentResult> find(IdempotencyKey key) {
        String redisKey = toRedisKey(key);
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            log.debug("Idempotency miss key={}", key);
            return Optional.empty();
        }
        try {
            PaymentResult result = objectMapper.readValue(json, PaymentResult.class);
            log.info("Idempotency hit key={}", key);
            return Optional.of(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialise idempotency value for key={} error={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void store(IdempotencyKey key, PaymentResult result) {
        String redisKey = toRedisKey(key);
        try {
            String json = objectMapper.writeValueAsString(result);
            Boolean stored = redisTemplate.opsForValue().setIfAbsent(redisKey, json, ttl);
            if (Boolean.TRUE.equals(stored)) {
                log.debug("Stored idempotency key={} ttl={}", key, ttl);
            } else {
                log.debug("Idempotency key already present, skipping store key={}", key);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise PaymentResult for idempotency key={} error={}", key, e.getMessage());
        }
    }

    private String toRedisKey(IdempotencyKey key) {
        return KEY_PREFIX + key.value();
    }
}
