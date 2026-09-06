-- V043__create_chart_of_account_table.sql
-- Entity: CHART_OF_ACCOUNT (docs/entity_model.md)

CREATE SEQUENCE chart_of_account_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE chart_of_account
(
    id BIGINT DEFAULT nextval('chart_of_account_seq') PRIMARY KEY,
    account_code VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(200) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    is_active BOOLEAN NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
