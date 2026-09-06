-- V030__create_purchase_order_line_table.sql
-- Entity: PURCHASE_ORDER_LINE (docs/entity_model.md)

CREATE SEQUENCE purchase_order_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE purchase_order_line
(
    id BIGINT DEFAULT nextval('purchase_order_line_seq') PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_order(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price BETWEEN 0 AND 9999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
