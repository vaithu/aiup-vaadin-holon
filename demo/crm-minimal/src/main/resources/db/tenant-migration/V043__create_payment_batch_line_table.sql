-- V042__create_payment_batch_line_table.sql
-- Entity: PAYMENT_BATCH_LINE (docs/entity_model.md)

CREATE SEQUENCE payment_batch_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE payment_batch_line
(
    id BIGINT DEFAULT nextval('payment_batch_line_seq') PRIMARY KEY,
    payment_batch_id BIGINT NOT NULL REFERENCES payment_batch(id),
    bill_id BIGINT NOT NULL REFERENCES bill(id),
    amount DECIMAL(14,2) NOT NULL CHECK (amount BETWEEN 0.01 AND 999999999999.99),
    early_pay_discount DECIMAL(10,2),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
