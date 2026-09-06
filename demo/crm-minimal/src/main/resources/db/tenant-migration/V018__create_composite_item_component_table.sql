-- V017__create_composite_item_component_table.sql
-- Entity: COMPOSITE_ITEM_COMPONENT (docs/entity_model.md)

CREATE SEQUENCE composite_item_component_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE composite_item_component
(
    id BIGINT DEFAULT nextval('composite_item_component_seq') PRIMARY KEY,
    composite_item_id BIGINT NOT NULL REFERENCES composite_item(id),
    component_product_id BIGINT NOT NULL REFERENCES product(id),
    quantity_required DECIMAL(12,2) NOT NULL CHECK (quantity_required BETWEEN 0.01 AND 999999.99),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
