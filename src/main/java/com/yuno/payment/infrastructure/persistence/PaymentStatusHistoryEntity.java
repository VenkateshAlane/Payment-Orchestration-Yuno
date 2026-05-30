package com.yuno.payment.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the payment_status_history table.
 * Immutable by design — status transitions are never updated, only appended.
 */
@Entity
@Table(name = "payment_status_history")
public class PaymentStatusHistoryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "from_status", nullable = false, length = 20, updatable = false)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20, updatable = false)
    private String toStatus;

    @Column(name = "provider", length = 50, updatable = false)
    private String provider;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected PaymentStatusHistoryEntity() {}

    public PaymentStatusHistoryEntity(UUID id,
                                      UUID paymentId,
                                      String fromStatus,
                                      String toStatus,
                                      String provider,
                                      Instant occurredAt) {
        this.id          = id;
        this.paymentId   = paymentId;
        this.fromStatus  = fromStatus;
        this.toStatus    = toStatus;
        this.provider    = provider;
        this.occurredAt  = occurredAt;
    }

    public UUID    getId()         { return id; }
    public UUID    getPaymentId()  { return paymentId; }
    public String  getFromStatus() { return fromStatus; }
    public String  getToStatus()   { return toStatus; }
    public String  getProvider()   { return provider; }
    public Instant getOccurredAt() { return occurredAt; }
}
