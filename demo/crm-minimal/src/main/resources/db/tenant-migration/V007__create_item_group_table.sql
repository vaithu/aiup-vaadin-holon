-- V006__create_item_group_table.sql
-- Entity: ITEM_GROUP (docs/entity_model.md)

CREATE SEQUENCE item_group_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE item_group
(
    id BIGINT DEFAULT nextval('item_group_seq') PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    variant_axes VARCHAR(200),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
