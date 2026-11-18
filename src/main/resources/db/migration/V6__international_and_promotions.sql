ALTER TABLE tenants ADD COLUMN currency_code varchar(3) NOT NULL DEFAULT 'INR';
ALTER TABLE tenants ADD COLUMN currency_symbol varchar(8) NOT NULL DEFAULT U&'\20B9';
ALTER TABLE tenants ADD COLUMN time_zone varchar(50) NOT NULL DEFAULT 'Asia/Kolkata';

CREATE TABLE promotions (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenants(id),
    code varchar(50) NOT NULL,
    name varchar(100) NOT NULL,
    discount_percent numeric(5,2),
    flat_discount numeric(12,2),
    valid_from date NOT NULL,
    valid_to date NOT NULL,
    active boolean NOT NULL DEFAULT true,
    applicable_plan_days integer,
    CONSTRAINT uk_promotions_tenant_code UNIQUE(tenant_id, code),
    CONSTRAINT ck_promotions_discount CHECK ((discount_percent IS NOT NULL) <> (flat_discount IS NOT NULL)),
    CONSTRAINT ck_promotions_dates CHECK (valid_from <= valid_to)
);
CREATE INDEX idx_promotions_tenant_active ON promotions(tenant_id, active);

ALTER TABLE members DROP CONSTRAINT members_plan_duration_days_check;
ALTER TABLE members ADD CONSTRAINT ck_members_plan_duration_days CHECK (plan_duration_days BETWEEN 1 AND 3650);
ALTER TABLE members ADD COLUMN discount_amount numeric(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE members ADD COLUMN applied_promotion_id uuid REFERENCES promotions(id);

ALTER TABLE payments ADD COLUMN currency_code varchar(3) NOT NULL DEFAULT 'INR';
ALTER TABLE payments ADD COLUMN discount_amount numeric(12,2) NOT NULL DEFAULT 0.00;
