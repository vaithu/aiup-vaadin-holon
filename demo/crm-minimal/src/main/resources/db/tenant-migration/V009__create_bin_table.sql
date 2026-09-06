-- V008__create_bin_table.sql
-- Entity: BIN (docs/entity_model.md)

CREATE SEQUENCE bin_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE bin
(
    id BIGINT DEFAULT nextval('bin_seq') PRIMARY KEY,
    warehouse_id BIGINT NOT NULL REFERENCES warehouse(id),
    zone VARCHAR(20) NOT NULL,
    bin_code VARCHAR(20) NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
