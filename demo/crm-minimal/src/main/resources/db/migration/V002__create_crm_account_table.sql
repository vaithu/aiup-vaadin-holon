-- V002__create_crm_account_table.sql
-- Platform-schema table backing CrmAccountCredentialsStore (CON-S-010 exemption)

CREATE SEQUENCE crm_account_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE crm_account
(
    id            BIGINT       DEFAULT nextval('crm_account_seq') PRIMARY KEY,
    tenant_id     VARCHAR(64)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_crm_account_tenant_email UNIQUE (tenant_id, email)
);