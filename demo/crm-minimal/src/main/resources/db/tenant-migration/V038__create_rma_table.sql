-- V037__create_rma_table.sql
-- Entity: RMA (docs/entity_model.md)

CREATE SEQUENCE rma_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE rma
(
    id BIGINT DEFAULT nextval('rma_seq') PRIMARY KEY,
    sales_order_id BIGINT NOT NULL REFERENCES sales_order(id),
    quality_inspection_id BIGINT REFERENCES quality_inspection(id),
    rma_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    disposition_code BIGINT NOT NULL REFERENCES reference_code(id),
    requested_at DATE NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
