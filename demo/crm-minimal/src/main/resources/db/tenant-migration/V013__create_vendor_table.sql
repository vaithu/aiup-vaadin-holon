-- V012__create_vendor_table.sql
-- Entity: VENDOR (docs/entity_model.md)

CREATE SEQUENCE vendor_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE vendor
(
    id BIGINT DEFAULT nextval('vendor_seq') PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    vendor_number VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(500) NOT NULL,
    tax_id VARCHAR(50),
    bank_iban VARCHAR(34) NOT NULL,
    default_currency VARCHAR(3) NOT NULL,
    payment_terms_days INTEGER NOT NULL CHECK (payment_terms_days BETWEEN 0 AND 365),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
