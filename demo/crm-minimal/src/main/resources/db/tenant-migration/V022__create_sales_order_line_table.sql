-- V021__create_sales_order_line_table.sql
-- Entity: SALES_ORDER_LINE (docs/entity_model.md)

CREATE SEQUENCE sales_order_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE sales_order_line
(
    id BIGINT DEFAULT nextval('sales_order_line_seq') PRIMARY KEY,
    sales_order_id BIGINT NOT NULL REFERENCES sales_order(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price BETWEEN 0 AND 9999999.99),
    tax_code_id BIGINT NOT NULL REFERENCES tax_code(id),
    allocated_quantity DECIMAL(12,2) NOT NULL CHECK (allocated_quantity BETWEEN 0 AND 999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
