-- V036__create_vendor_return_line_table.sql
-- Entity: VENDOR_RETURN_LINE (docs/entity_model.md)

CREATE SEQUENCE vendor_return_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE vendor_return_line
(
    id BIGINT DEFAULT nextval('vendor_return_line_seq') PRIMARY KEY,
    vendor_return_id BIGINT NOT NULL REFERENCES vendor_return(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    defect_notes VARCHAR(1000),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
