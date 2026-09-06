-- V026__create_subscription_table.sql
-- Entity: SUBSCRIPTION (docs/entity_model.md)

CREATE SEQUENCE subscription_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE subscription
(
    id BIGINT DEFAULT nextval('subscription_seq') PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    billing_cycle VARCHAR(20) NOT NULL CHECK (billing_cycle IN ('MONTHLY', 'QUARTERLY', 'ANNUAL')),
    next_invoice_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount BETWEEN 0.01 AND 9999999.99),
    is_active BOOLEAN NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
