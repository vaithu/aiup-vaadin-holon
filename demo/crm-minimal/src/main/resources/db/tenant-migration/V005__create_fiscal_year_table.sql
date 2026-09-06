-- V004__create_fiscal_year_table.sql
-- Entity: FISCAL_YEAR (docs/entity_model.md)

CREATE SEQUENCE fiscal_year_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE fiscal_year
(
    id BIGINT DEFAULT nextval('fiscal_year_seq') PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    label VARCHAR(20) NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
