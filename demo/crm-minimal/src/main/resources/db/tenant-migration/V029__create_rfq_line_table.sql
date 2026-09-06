-- V028__create_rfq_line_table.sql
-- Entity: RFQ_LINE (docs/entity_model.md)

CREATE SEQUENCE rfq_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE rfq_line
(
    id BIGINT DEFAULT nextval('rfq_line_seq') PRIMARY KEY,
    rfq_id BIGINT NOT NULL REFERENCES rfq(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    quoted_price DECIMAL(10,2),
    lead_time_days INTEGER NOT NULL CHECK (lead_time_days BETWEEN 0 AND 365),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
