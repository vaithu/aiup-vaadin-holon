-- V003__create_numbering_pattern_table.sql
-- Entity: NUMBERING_PATTERN (docs/entity_model.md)

CREATE SEQUENCE numbering_pattern_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE numbering_pattern
(
    id BIGINT DEFAULT nextval('numbering_pattern_seq') PRIMARY KEY,
    document_type VARCHAR(10) NOT NULL CHECK (document_type IN ('SO', 'INV', 'QU', 'PO', 'BIL', 'PAY', 'REC', 'JE', 'RMA')),
    prefix VARCHAR(10) NOT NULL,
    padding_digits INTEGER NOT NULL CHECK (padding_digits BETWEEN 1 AND 10),
    year_reset BOOLEAN NOT NULL,
    current_value BIGINT NOT NULL,
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
