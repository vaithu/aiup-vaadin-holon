-- V002__create_contact_table.sql
-- Contact person belonging to a customer. Created after customer (FK dependency).

CREATE SEQUENCE contact_seq START WITH 1 INCREMENT BY 50 CACHE 50;

CREATE TABLE contact
(
    id          BIGINT       DEFAULT nextval('contact_seq') PRIMARY KEY,
    customer_id BIGINT       NOT NULL REFERENCES customer (id),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(200) NOT NULL,
    phone       VARCHAR(40)
);

CREATE INDEX idx_contact_customer_id ON contact (customer_id);
