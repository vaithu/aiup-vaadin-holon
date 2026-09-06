-- V046__create_fx_rate_table.sql
-- Entity: FX_RATE (docs/entity_model.md)

CREATE SEQUENCE fx_rate_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE fx_rate
(
    id BIGINT DEFAULT nextval('fx_rate_seq') PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate_date DATE NOT NULL,
    rate DECIMAL(18,8) NOT NULL CHECK (rate BETWEEN 0.00000001 AND 999999.99999999),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
