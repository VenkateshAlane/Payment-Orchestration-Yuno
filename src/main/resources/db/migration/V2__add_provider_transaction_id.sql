ALTER TABLE payments
    ADD COLUMN provider_transaction_id VARCHAR(100);

COMMENT ON COLUMN payments.provider_transaction_id
    IS 'Transaction reference returned by the external payment provider on success. Null for PENDING/PROCESSING/FAILED payments.';
