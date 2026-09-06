-- V016__create_composite_item_table.sql
-- Entity: COMPOSITE_ITEM (docs/entity_model.md)

CREATE SEQUENCE composite_item_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE composite_item
(
    id BIGINT DEFAULT nextval('composite_item_seq') PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product(id),
    built_quantity_per_run INTEGER NOT NULL CHECK (built_quantity_per_run BETWEEN 1 AND 9999),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
