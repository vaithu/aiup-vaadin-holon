-- V034__create_inspection_line_table.sql
-- Entity: INSPECTION_LINE (docs/entity_model.md)

CREATE SEQUENCE inspection_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE inspection_line
(
    id BIGINT DEFAULT nextval('inspection_line_seq') PRIMARY KEY,
    quality_inspection_id BIGINT NOT NULL REFERENCES quality_inspection(id),
    unit_number INTEGER NOT NULL CHECK (unit_number BETWEEN 1 AND 9999),
    result_code BIGINT NOT NULL REFERENCES reference_code(id),
    defect_type VARCHAR(100),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
