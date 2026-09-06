-- V041__create_payment_batch_table.sql
-- Entity: PAYMENT_BATCH (docs/entity_model.md)

CREATE SEQUENCE payment_batch_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE payment_batch
(
    id BIGINT DEFAULT nextval('payment_batch_seq') PRIMARY KEY,
    batch_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    scheduled_date DATE NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL CHECK (total_amount BETWEEN 0 AND 999999999999.99),
    approved_by VARCHAR(50),
    xml_hash VARCHAR(64) NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);

-- approved_by: references holon-saas tenant-users TenantUser.userId (external, not enforced by FK)
