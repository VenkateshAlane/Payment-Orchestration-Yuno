package com.yuno.payment.infrastructure.observability;

import com.yuno.payment.domain.event.PaymentStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for PaymentStatusChangedEvent domain events and writes a structured audit log.
 *
 * This gives a persistent, searchable log of every status transition:
 *   paymentId, from, to, provider, timestamp — all in one line, in the MDC context.
 *
 * SRP: this class only logs domain events.
 * Low Coupling: it depends on the event record, not on the Payment aggregate or any service.
 */
@Component
public class PaymentEventLogger {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventLogger.class);

    @EventListener
    public void onStatusChanged(PaymentStatusChangedEvent event) {
        MDC.put("paymentId", event.paymentId().toString());
        try {
            log.info("Payment status changed paymentId={} from={} to={} provider={} at={}",
                    event.paymentId(),
                    event.from(),
                    event.to(),
                    event.provider() != null ? event.provider() : "n/a",
                    event.occurredAt());
        } finally {
            MDC.remove("paymentId");
        }
    }
}
