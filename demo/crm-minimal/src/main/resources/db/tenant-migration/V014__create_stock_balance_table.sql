-- V013__create_stock_balance_table.sql
-- Entity: STOCK_BALANCE (docs/entity_model.md)

CREATE SEQUENCE stock_balance_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE stock_balance
(
    id BIGINT DEFAULT nextval('stock_balance_seq') PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    bin_id BIGINT NOT NULL REFERENCES bin(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0 AND 9999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
