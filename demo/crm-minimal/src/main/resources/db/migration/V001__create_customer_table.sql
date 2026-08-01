-- V001__create_customer_table.sql
-- Customer company. PK backed by a sequence (never SERIAL / IDENTITY).

CREATE SEQUENCE customer_seq START WITH 1 INCREMENT BY 50 CACHE 50;

CREATE TABLE customer
(
    id         BIGINT        DEFAULT nextval('customer_seq') PRIMARY KEY,
    name       VARCHAR(200)  NOT NULL UNIQUE,
    industry   VARCHAR(100),
    status     VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                             CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMP     NOT NULL DEFAULT now()
);
