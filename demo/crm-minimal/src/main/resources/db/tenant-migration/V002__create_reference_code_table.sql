-- V001__create_reference_code_table.sql
-- Entity: REFERENCE_CODE (docs/entity_model.md)

CREATE SEQUENCE reference_code_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE reference_code
(
    id BIGINT DEFAULT nextval('reference_code_seq') PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    code VARCHAR(30) NOT NULL,
    label VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL CHECK (sort_order BETWEEN 0 AND 999),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
