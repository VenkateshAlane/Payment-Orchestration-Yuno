package com.yuno.payment.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the payments table.
 * Deliberately separate from the Payment domain model.
 * The domain knows nothing about JPA annotations.
 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "assigned_provider", length = 50)
    private String assignedProvider;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // JPA requires a no-arg constructor
    protected PaymentEntity() {}

    public PaymentEntity(UUID id,
                         BigDecimal amount,
                         String currency,
                         String method,
                         String status,
                         String assignedProvider,
                         String providerTransactionId,
                         int attemptCount,
                         Instant createdAt,
                         Instant updatedAt) {
        this.id                    = id;
        this.amount                = amount;
        this.currency              = currency;
        this.method                = method;
        this.status                = status;
        this.assignedProvider      = assignedProvider;
        this.providerTransactionId = providerTransactionId;
        this.attemptCount          = attemptCount;
        this.createdAt             = createdAt;
        this.updatedAt             = updatedAt;
    }

    public UUID       getId()                    { return id; }
    public BigDecimal getAmount()                { return amount; }
    public String     getCurrency()              { return currency; }
    public String     getMethod()                { return method; }
    public String     getStatus()                { return status; }
    public String     getAssignedProvider()      { return assignedProvider; }
    public String     getProviderTransactionId() { return providerTransactionId; }
    public int        getAttemptCount()          { return attemptCount; }
    public Instant    getCreatedAt()             { return createdAt; }
    public Instant    getUpdatedAt()             { return updatedAt; }

    // Setters needed by the adapter when updating mutable fields
    public void setStatus(String status)                                   { this.status = status; }
    public void setAssignedProvider(String assignedProvider)               { this.assignedProvider = assignedProvider; }
    public void setProviderTransactionId(String providerTransactionId)     { this.providerTransactionId = providerTransactionId; }
    public void setAttemptCount(int attemptCount)                          { this.attemptCount = attemptCount; }
    public void setUpdatedAt(Instant updatedAt)                            { this.updatedAt = updatedAt; }
}
