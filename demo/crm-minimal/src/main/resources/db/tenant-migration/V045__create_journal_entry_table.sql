-- V044__create_journal_entry_table.sql
-- Entity: JOURNAL_ENTRY (docs/entity_model.md)

CREATE SEQUENCE journal_entry_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE journal_entry
(
    id BIGINT DEFAULT nextval('journal_entry_seq') PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_period(id),
    entry_number VARCHAR(20) NOT NULL UNIQUE,
    status_code BIGINT NOT NULL REFERENCES reference_code(id),
    posted_at TIMESTAMP NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    entry_hash VARCHAR(64) NOT NULL UNIQUE,
    reverses_entry_id BIGINT REFERENCES journal_entry(id),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
