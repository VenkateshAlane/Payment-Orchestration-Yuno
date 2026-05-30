package com.yuno.payment.infrastructure.persistence;

import com.yuno.payment.domain.event.PaymentStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Listens for PaymentStatusChangedEvent and writes an audit row to
 * payment_status_history. Each state transition becomes one immutable record.
 *
 * SRP: this class only persists history — logging is left to PaymentEventLogger.
 *
 * @Transactional: each insert is its own short transaction, consistent with
 * the adapter-layer transaction ownership pattern used throughout the project.
 */
@Component
public class PaymentStatusHistoryPersister {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusHistoryPersister.class);

    private final SpringDataPaymentStatusHistoryRepository historyRepository;

    public PaymentStatusHistoryPersister(SpringDataPaymentStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @EventListener
    @Transactional
    public void onStatusChanged(PaymentStatusChangedEvent event) {
        PaymentStatusHistoryEntity entry = new PaymentStatusHistoryEntity(
                UUID.randomUUID(),
                event.paymentId().value(),
                event.from().name(),
                event.to().name(),
                event.provider(),
                event.occurredAt()
        );
        historyRepository.save(entry);
        log.debug("Persisted status history paymentId={} {}→{}",
                event.paymentId(), event.from(), event.to());
    }
}
