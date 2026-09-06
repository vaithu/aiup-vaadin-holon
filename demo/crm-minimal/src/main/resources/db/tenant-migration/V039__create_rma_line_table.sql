-- V038__create_rma_line_table.sql
-- Entity: RMA_LINE (docs/entity_model.md)

CREATE SEQUENCE rma_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE rma_line
(
    id BIGINT DEFAULT nextval('rma_line_seq') PRIMARY KEY,
    rma_id BIGINT NOT NULL REFERENCES rma(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    defect_notes VARCHAR(1000),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
