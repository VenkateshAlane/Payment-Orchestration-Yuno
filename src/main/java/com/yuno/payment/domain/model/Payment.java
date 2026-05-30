package com.yuno.payment.domain.model;

import com.yuno.payment.domain.event.PaymentStatusChangedEvent;
import com.yuno.payment.domain.exception.IllegalStateTransitionException;

import java.time.Instant;

/**
 * Payment aggregate root.
 *
 * Owns all state transitions — nothing outside this class may set the status directly.
 * Constructed via static factories:
 *   - Payment.create()         → for new payments (generates ID, starts PENDING)
 *   - Payment.reconstitute()   → for reloading from persistence
 *
 * GRASP Information Expert: the aggregate has all the data, so it owns all rules.
 */
public class Payment {

    private final PaymentId id;
    private final Money amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private String assignedProvider;
    private int attemptCount;
    private final Instant createdAt;
    private Instant updatedAt;

    // ── Private constructor — all construction goes through static factories ──

    private Payment(PaymentId id,
                    Money amount,
                    PaymentMethod method,
                    PaymentStatus status,
                    String assignedProvider,
                    int attemptCount,
                    Instant createdAt,
                    Instant updatedAt) {
        this.id               = id;
        this.amount           = amount;
        this.method           = method;
        this.status           = status;
        this.assignedProvider = assignedProvider;
        this.attemptCount     = attemptCount;
        this.createdAt        = createdAt;
        this.updatedAt        = updatedAt;
    }

    // ── Factory: new payment ──

    /**
     * Creates a brand-new payment in PENDING state with a generated ID.
     * GRASP Creator: Payment creates itself.
     */
    public static Payment create(Money amount, PaymentMethod method) {
        if (amount == null)  throw new IllegalArgumentException("Amount must not be null");
        if (method == null)  throw new IllegalArgumentException("Method must not be null");

        Instant now = Instant.now();
        return new Payment(
                PaymentId.generate(),
                amount,
                method,
                PaymentStatus.PENDING,
                null,
                0,
                now,
                now
        );
    }

    // ── Factory: reconstitute from persistence ──

    /**
     * Rebuilds a Payment from stored values. No validation — the DB is the source of truth.
     */
    public static Payment reconstitute(PaymentId id,
                                       Money amount,
                                       PaymentMethod method,
                                       PaymentStatus status,
                                       String assignedProvider,
                                       int attemptCount,
                                       Instant createdAt,
                                       Instant updatedAt) {
        return new Payment(id, amount, method, status, assignedProvider, attemptCount, createdAt, updatedAt);
    }

    // ── State transitions (each returns a domain event) ──

    /**
     * Marks the payment as PROCESSING.
     * Called before handing off to the provider chain.
     */
    public PaymentStatusChangedEvent markProcessing() {
        PaymentStatus previous = this.status;
        validateTransition(PaymentStatus.PROCESSING);
        this.status    = PaymentStatus.PROCESSING;
        this.updatedAt = Instant.now();
        return new PaymentStatusChangedEvent(this.id, previous, this.status, this.assignedProvider, this.updatedAt);
    }

    /**
     * Marks the payment as SUCCESS and records which provider processed it.
     */
    public PaymentStatusChangedEvent markSuccess(String provider) {
        PaymentStatus previous = this.status;
        validateTransition(PaymentStatus.SUCCESS);
        this.status           = PaymentStatus.SUCCESS;
        this.assignedProvider = provider;
        this.updatedAt        = Instant.now();
        return new PaymentStatusChangedEvent(this.id, previous, this.status, provider, this.updatedAt);
    }

    /**
     * Marks the payment as FAILED after all providers are exhausted.
     */
    public PaymentStatusChangedEvent markFailed() {
        PaymentStatus previous = this.status;
        validateTransition(PaymentStatus.FAILED);
        this.status    = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
        return new PaymentStatusChangedEvent(this.id, previous, this.status, this.assignedProvider, this.updatedAt);
    }

    /**
     * Tracks how many total provider attempts were made across retry and failover.
     */
    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    // ── Guard ──

    private void validateTransition(PaymentStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateTransitionException(this.id, this.status, next);
        }
    }

    // ── Getters (no setters — mutations go through state-transition methods) ──

    public PaymentId      getId()               { return id; }
    public Money          getAmount()            { return amount; }
    public PaymentMethod  getMethod()            { return method; }
    public PaymentStatus  getStatus()            { return status; }
    public String         getAssignedProvider()  { return assignedProvider; }
    public int            getAttemptCount()      { return attemptCount; }
    public Instant        getCreatedAt()         { return createdAt; }
    public Instant        getUpdatedAt()         { return updatedAt; }
}
