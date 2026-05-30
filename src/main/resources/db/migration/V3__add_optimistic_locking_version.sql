ALTER TABLE payments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN payments.version
    IS 'JPA optimistic locking version. Incremented on every UPDATE. Prevents silent concurrent overwrites.';
