-- V003__auth_schema.sql
-- Holon Auth scaffold: account / role / permission tables backing the Realm bootstrap.
-- Adjust table/column names to match your AccountProvider configuration.

CREATE SEQUENCE holon_account_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE holon_account
(
    id       BIGINT       DEFAULT nextval('holon_account_seq') PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled  BOOLEAN      NOT NULL DEFAULT true
);

CREATE SEQUENCE holon_role_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE holon_role
(
    id   BIGINT       DEFAULT nextval('holon_role_seq') PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE holon_account_role
(
    account_id BIGINT NOT NULL REFERENCES holon_account (id),
    role_id    BIGINT NOT NULL REFERENCES holon_role (id),
    PRIMARY KEY (account_id, role_id)
);

CREATE SEQUENCE holon_permission_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE holon_permission
(
    id   BIGINT       DEFAULT nextval('holon_permission_seq') PRIMARY KEY,
    code VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE holon_role_permission
(
    role_id       BIGINT NOT NULL REFERENCES holon_role (id),
    permission_id BIGINT NOT NULL REFERENCES holon_permission (id),
    PRIMARY KEY (role_id, permission_id)
);

-- Seed roles and permissions from docs/requirements.md
INSERT INTO holon_role (code) VALUES ('SALES_REP'), ('SALES_MANAGER');

INSERT INTO holon_permission (code) VALUES
    ('customers:view'), ('customers:create'), ('customers:archive'),
    ('contacts:view'), ('contacts:create');

-- SALES_REP: view/create customers and contacts
INSERT INTO holon_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM holon_role r, holon_permission p
WHERE r.code = 'SALES_REP'
  AND p.code IN ('customers:view', 'customers:create', 'contacts:view', 'contacts:create');

-- SALES_MANAGER: everything SALES_REP has, plus archive
INSERT INTO holon_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM holon_role r, holon_permission p
WHERE r.code = 'SALES_MANAGER'
  AND p.code IN ('customers:view', 'customers:create', 'customers:archive',
                 'contacts:view', 'contacts:create');
