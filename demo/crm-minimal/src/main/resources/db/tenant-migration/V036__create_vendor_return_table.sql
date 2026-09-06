-- V035__create_vendor_return_table.sql
-- Entity: VENDOR_RETURN (docs/entity_model.md)

CREATE SEQUENCE vendor_return_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE vendor_return
(
    id BIGINT DEFAULT nextval('vendor_return_seq') PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendor(id),
    quality_inspection_id BIGINT REFERENCES quality_inspection(id),
    return_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    disposition_code BIGINT NOT NULL REFERENCES reference_code(id),
    requested_at DATE NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
