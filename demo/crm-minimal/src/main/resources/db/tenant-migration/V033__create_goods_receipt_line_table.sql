-- V032__create_goods_receipt_line_table.sql
-- Entity: GOODS_RECEIPT_LINE (docs/entity_model.md)

CREATE SEQUENCE goods_receipt_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE goods_receipt_line
(
    id BIGINT DEFAULT nextval('goods_receipt_line_seq') PRIMARY KEY,
    goods_receipt_id BIGINT NOT NULL REFERENCES goods_receipt(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    bin_id BIGINT NOT NULL REFERENCES bin(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
