-- V010__create_customer_table.sql
-- Entity: CUSTOMER (docs/entity_model.md)

CREATE SEQUENCE customer_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE customer
(
    id BIGINT DEFAULT nextval('customer_seq') PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    customer_number VARCHAR(20) NOT NULL UNIQUE,
    billing_address VARCHAR(500) NOT NULL,
    tax_id VARCHAR(50),
    default_currency VARCHAR(3) NOT NULL,
    default_tax_code_id BIGINT NOT NULL REFERENCES tax_code(id),
    payment_terms_days INTEGER NOT NULL CHECK (payment_terms_days BETWEEN 0 AND 365),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
