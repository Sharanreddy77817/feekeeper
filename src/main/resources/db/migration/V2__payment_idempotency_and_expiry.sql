ALTER TABLE payments ADD COLUMN idempotency_key varchar(100);
ALTER TABLE payments ADD COLUMN new_expiry_date date;

CREATE UNIQUE INDEX uk_payments_tenant_idempotency
    ON payments(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
