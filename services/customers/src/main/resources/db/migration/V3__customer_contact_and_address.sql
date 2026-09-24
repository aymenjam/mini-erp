-- Contacts and addresses are parts of the customer aggregate (spec 1.1, slice 2).
-- They are deleted with their customer, and the app never deletes a customer, so CASCADE is only a safety net.
CREATE TABLE customer_contact (
    id          UUID         PRIMARY KEY,
    version     BIGINT       NOT NULL,
    customer_id UUID         NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(254),
    phone       VARCHAR(50),
    job_title   VARCHAR(100),
    -- "primary" is a reserved word in SQL.
    is_primary  BOOLEAN      NOT NULL
);

CREATE TABLE customer_address (
    id               UUID         PRIMARY KEY,
    version          BIGINT       NOT NULL,
    customer_id      UUID         NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    type             VARCHAR(20)  NOT NULL,
    line1            VARCHAR(200) NOT NULL,
    line2            VARCHAR(200),
    postal_code      VARCHAR(20),
    city             VARCHAR(100) NOT NULL,
    country          VARCHAR(2)   NOT NULL,
    default_for_type BOOLEAN      NOT NULL,
    CONSTRAINT ck_customer_address_type CHECK (type IN ('BILLING', 'SHIPPING')),
    CONSTRAINT ck_customer_address_country CHECK (country ~ '^[A-Z]{2}$')
);

-- At most one primary contact per customer, and one default address per customer and type.
-- These indexes are the last guard behind the domain rules, also for two requests at the same time.
-- They cannot be deferred, so the adapter clears the old flag before it sets the new one (CustomerPersistenceAdapter).
CREATE UNIQUE INDEX ux_customer_contact_primary ON customer_contact (customer_id) WHERE is_primary;
CREATE UNIQUE INDEX ux_customer_address_default ON customer_address (customer_id, type) WHERE default_for_type;

-- Foreign keys are not indexed by PostgreSQL. Loading the children of a customer uses these.
CREATE INDEX ix_customer_contact_customer ON customer_contact (customer_id);
CREATE INDEX ix_customer_address_customer ON customer_address (customer_id);
