package com.yuno.payment.domain.model;

/**
 * Payment lifecycle states.
 *
 * Valid transitions (enforced by the Payment aggregate):
 *   PENDING → PROCESSING → SUCCESS
 *                        → FAILED
 *
 * Terminal states (SUCCESS, FAILED) accept no further transitions.
 */
public enum PaymentStatus {

    PENDING {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return next == PROCESSING;
        }
    },

    PROCESSING {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return next == SUCCESS || next == FAILED;
        }
    },

    SUCCESS {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return false;
        }
    },

    FAILED {
        @Override
        public boolean canTransitionTo(PaymentStatus next) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(PaymentStatus next);
}
