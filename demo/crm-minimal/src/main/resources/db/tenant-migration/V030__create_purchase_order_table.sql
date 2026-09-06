-- V029__create_purchase_order_table.sql
-- Entity: PURCHASE_ORDER (docs/entity_model.md)

CREATE SEQUENCE purchase_order_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE purchase_order
(
    id BIGINT DEFAULT nextval('purchase_order_seq') PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendor(id),
    rfq_id BIGINT REFERENCES rfq(id),
    po_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    expected_receipt_date DATE NOT NULL,
    ship_to_warehouse_id BIGINT NOT NULL REFERENCES warehouse(id),
    total_amount DECIMAL(14,2) NOT NULL CHECK (total_amount BETWEEN 0 AND 999999999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
