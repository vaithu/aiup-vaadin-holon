-- V014__create_stock_transfer_table.sql
-- Entity: STOCK_TRANSFER (docs/entity_model.md)

CREATE SEQUENCE stock_transfer_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE stock_transfer
(
    id BIGINT DEFAULT nextval('stock_transfer_seq') PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    from_bin_id BIGINT NOT NULL REFERENCES bin(id),
    to_bin_id BIGINT NOT NULL REFERENCES bin(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    transferred_at TIMESTAMP NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
