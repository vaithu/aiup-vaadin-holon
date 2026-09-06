-- V039__create_bill_table.sql
-- Entity: BILL (docs/entity_model.md)

CREATE SEQUENCE bill_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE bill
(
    id BIGINT DEFAULT nextval('bill_seq') PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendor(id),
    goods_receipt_id BIGINT NOT NULL REFERENCES goods_receipt(id),
    bill_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL CHECK (total_amount BETWEEN 0 AND 999999999999.99),
    is_match_blocked BOOLEAN NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
