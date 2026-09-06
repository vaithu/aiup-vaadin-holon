-- V011__create_contact_table.sql
-- Entity: CONTACT (docs/entity_model.md)

CREATE SEQUENCE contact_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE contact
(
    id BIGINT DEFAULT nextval('contact_seq') PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(30),
    job_title VARCHAR(100),
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
