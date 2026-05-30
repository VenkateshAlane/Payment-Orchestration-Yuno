CREATE TABLE IF NOT EXISTS payments (
    id              UUID            PRIMARY KEY,
    amount          NUMERIC(19, 4)  NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    method          VARCHAR(20)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    assigned_provider VARCHAR(50),
    attempt_count   INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at DESC);
