-- V045__create_journal_line_table.sql
-- Entity: JOURNAL_LINE (docs/entity_model.md)

CREATE SEQUENCE journal_line_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE journal_line
(
    id BIGINT DEFAULT nextval('journal_line_seq') PRIMARY KEY,
    journal_entry_id BIGINT NOT NULL REFERENCES journal_entry(id),
    account_id BIGINT NOT NULL REFERENCES chart_of_account(id),
    debit_amount DECIMAL(14,2) NOT NULL CHECK (debit_amount BETWEEN 0 AND 999999999999.99),
    credit_amount DECIMAL(14,2) NOT NULL CHECK (credit_amount BETWEEN 0 AND 999999999999.99),
    narration VARCHAR(500),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
