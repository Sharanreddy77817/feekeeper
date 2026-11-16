CREATE TABLE tenants (
    id uuid PRIMARY KEY,
    business_name varchar(150) NOT NULL,
    owner_name varchar(120) NOT NULL,
    phone varchar(13) NOT NULL,
    upi_id varchar(100),
    created_at timestamptz NOT NULL
);

CREATE TABLE users (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenants(id),
    email varchar(254) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    role varchar(20) NOT NULL CHECK (role IN ('OWNER', 'STAFF')),
    created_at timestamptz NOT NULL
);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);

CREATE TABLE members (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenants(id),
    full_name varchar(120) NOT NULL,
    phone varchar(13) NOT NULL,
    plan_name varchar(100) NOT NULL,
    plan_amount numeric(12,2) NOT NULL,
    plan_duration_days integer NOT NULL CHECK (plan_duration_days IN (30, 90, 180)),
    join_date date NOT NULL,
    expiry_date date NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRING_SOON', 'OVERDUE')),
    last_reminder_sent_date date,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_members_tenant_phone UNIQUE (tenant_id, phone)
);
CREATE INDEX idx_members_tenant_status ON members(tenant_id, status);
CREATE INDEX idx_members_tenant_expiry ON members(tenant_id, expiry_date);
CREATE INDEX idx_members_tenant_phone ON members(tenant_id, phone);

CREATE TABLE payments (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL REFERENCES tenants(id),
    member_id uuid NOT NULL REFERENCES members(id),
    amount_paid numeric(12,2) NOT NULL,
    payment_mode varchar(10) NOT NULL CHECK (payment_mode IN ('UPI', 'CASH')),
    payment_date date NOT NULL,
    receipt_number varchar(50) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT uk_payments_tenant_receipt UNIQUE (tenant_id, receipt_number)
);
CREATE INDEX idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX idx_payments_tenant_member ON payments(tenant_id, member_id);

CREATE TABLE receipt_sequences (
    tenant_id uuid NOT NULL REFERENCES tenants(id),
    year integer NOT NULL,
    last_value bigint NOT NULL,
    PRIMARY KEY (tenant_id, year)
);
CREATE INDEX idx_receipt_sequences_tenant_id ON receipt_sequences(tenant_id);
