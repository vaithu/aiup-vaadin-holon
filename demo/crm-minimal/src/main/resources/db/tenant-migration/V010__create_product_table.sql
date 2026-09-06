-- V009__create_product_table.sql
-- Entity: PRODUCT (docs/entity_model.md)

CREATE SEQUENCE product_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE product
(
    id BIGINT DEFAULT nextval('product_seq') PRIMARY KEY,
    item_group_id BIGINT REFERENCES item_group(id),
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    is_composite BOOLEAN NOT NULL,
    unit_cost DECIMAL(10,2) NOT NULL CHECK (unit_cost BETWEEN 0 AND 9999999.99),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price BETWEEN 0 AND 9999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
