-- Customers of the company that uses the ERP. Contacts and addresses come in V3 (spec 1.1, slice 2).
CREATE TABLE customer (
    id         UUID         PRIMARY KEY,
    version    BIGINT       NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(254),
    phone      VARCHAR(50),
    vat_number VARCHAR(50),
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_customer_type CHECK (type IN ('COMPANY', 'INDIVIDUAL')),
    CONSTRAINT ck_customer_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    -- Same rule as in the domain. The database is the last guard.
    CONSTRAINT ck_customer_vat_only_company CHECK (vat_number IS NULL OR type = 'COMPANY')
);

-- Two requests that create the same VAT number at the same time: the database lets only one pass.
-- The application maps this index name to "409 duplicate VAT number" (CustomerPersistenceAdapter).
CREATE UNIQUE INDEX ux_customer_vat_number ON customer (vat_number) WHERE vat_number IS NOT NULL;

-- The list is sorted by name and by creation time (slice 3).
CREATE INDEX ix_customer_name ON customer (name);
