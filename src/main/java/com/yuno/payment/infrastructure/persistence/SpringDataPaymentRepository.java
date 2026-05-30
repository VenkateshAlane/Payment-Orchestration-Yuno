package com.yuno.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for PaymentEntity.
 * Only the infrastructure layer knows this exists.
 */
public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {}
