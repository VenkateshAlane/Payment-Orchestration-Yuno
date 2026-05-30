CREATE TABLE payment_status_history (
    id          UUID            PRIMARY KEY,
    payment_id  UUID            NOT NULL REFERENCES payments(id),
    from_status VARCHAR(20)     NOT NULL,
    to_status   VARCHAR(20)     NOT NULL,
    provider    VARCHAR(50),
    occurred_at TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_psh_payment_id   ON payment_status_history (payment_id);
CREATE INDEX idx_psh_occurred_at  ON payment_status_history (occurred_at DESC);

COMMENT ON TABLE payment_status_history
    IS 'Immutable audit trail of every payment status transition. One row per transition.';
