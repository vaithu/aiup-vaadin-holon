-- V005__create_fiscal_period_table.sql
-- Entity: FISCAL_PERIOD (docs/entity_model.md)

CREATE SEQUENCE fiscal_period_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE fiscal_period
(
    id BIGINT DEFAULT nextval('fiscal_period_seq') PRIMARY KEY,
    fiscal_year_id BIGINT NOT NULL REFERENCES fiscal_year(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_closed BOOLEAN NOT NULL,
    closed_at TIMESTAMP,
    closed_by VARCHAR(50),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);

-- closed_by: references holon-saas tenant-users TenantUser.userId (external, not enforced by FK)
