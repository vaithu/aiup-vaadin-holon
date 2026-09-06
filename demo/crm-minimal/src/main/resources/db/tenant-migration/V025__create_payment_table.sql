-- V024__create_payment_table.sql
-- Entity: PAYMENT (docs/entity_model.md)

CREATE SEQUENCE payment_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE payment
(
    id BIGINT DEFAULT nextval('payment_seq') PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoice(id),
    payment_number VARCHAR(20) NOT NULL UNIQUE,
    method_code BIGINT NOT NULL REFERENCES reference_code(id),
    received_at TIMESTAMP NOT NULL,
    amount DECIMAL(14,2) NOT NULL CHECK (amount BETWEEN 0.01 AND 999999999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
