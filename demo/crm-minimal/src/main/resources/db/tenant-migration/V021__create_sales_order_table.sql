-- V020__create_sales_order_table.sql
-- Entity: SALES_ORDER (docs/entity_model.md)

CREATE SEQUENCE sales_order_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE sales_order
(
    id BIGINT DEFAULT nextval('sales_order_seq') PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    quote_id BIGINT REFERENCES quote(id),
    order_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    order_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL CHECK (total_amount BETWEEN 0 AND 999999999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
