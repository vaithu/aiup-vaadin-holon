-- V002__create_tax_code_table.sql
-- Entity: TAX_CODE (docs/entity_model.md)

CREATE SEQUENCE tax_code_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE tax_code
(
    id BIGINT DEFAULT nextval('tax_code_seq') PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    rate DECIMAL(5,2) NOT NULL CHECK (rate BETWEEN 0 AND 100),
    is_default BOOLEAN NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
