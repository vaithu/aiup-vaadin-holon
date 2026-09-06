-- V007__create_warehouse_table.sql
-- Entity: WAREHOUSE (docs/entity_model.md)

CREATE SEQUENCE warehouse_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE warehouse
(
    id BIGINT DEFAULT nextval('warehouse_seq') PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(500) NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
