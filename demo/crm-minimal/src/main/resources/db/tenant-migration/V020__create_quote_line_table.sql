-- V019__create_quote_line_table.sql
-- Entity: QUOTE_LINE (docs/entity_model.md)

CREATE SEQUENCE quote_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE quote_line
(
    id BIGINT DEFAULT nextval('quote_line_seq') PRIMARY KEY,
    quote_id BIGINT NOT NULL REFERENCES quote(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity DECIMAL(12,2) NOT NULL CHECK (quantity BETWEEN 0.01 AND 999999.99),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price BETWEEN 0 AND 9999999.99),
    discount_pct DECIMAL(5,2) NOT NULL CHECK (discount_pct BETWEEN 0 AND 100),
    tax_code_id BIGINT NOT NULL REFERENCES tax_code(id),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
