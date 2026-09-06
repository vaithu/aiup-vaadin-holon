-- V027__create_rfq_table.sql
-- Entity: RFQ (docs/entity_model.md)

CREATE SEQUENCE rfq_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE rfq
(
    id BIGINT DEFAULT nextval('rfq_seq') PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendor(id),
    rfq_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    required_by DATE NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
