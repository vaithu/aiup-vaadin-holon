---
name: flyway-migration
description: >
  Creates versioned Flyway database migration scripts (V*.sql) with sequences,
  tables, constraints, and foreign keys from the entity model. Use when the user
  asks to "create a migration", "generate SQL scripts", "set up database tables",
  "write a Flyway migration", or mentions schema migration, DB migration,
  database versioning, or SQL migration files.
---

# Flyway Migration

## Instructions

Create Flyway database migration scripts based on `docs/entity_model.md`.
Column names are `snake_case` derived from JavaBean field names (e.g. field `totalAmount`
→ column `total_amount`, matching the `@DataPath` convention documented in
[`../implement/references/bean-model.md`](../implement/references/bean-model.md)).
Use sequences for primary keys — never `SERIAL`, `BIGSERIAL`, or `IDENTITY`.

Also emit `V001__auth_schema.sql` (or the next available version number) as a scaffold
for Holon Auth role / permission tables so the `Realm` bootstrap has storage backing.

## Constraints

Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.
Key constraints for this skill:

- **Java 25 / PostgreSQL 16+ / Flyway 10.x**
- Column names MUST be `snake_case` of the JavaBean field name
- PKs use sequences (`CREATE SEQUENCE … nextval(…)`) — not `SERIAL` / `IDENTITY`
- Do NOT drop existing tables without explicit user confirmation

## Pre-Emit Checklist

- [ ] All entities from `docs/entity_model.md` have a migration file
- [ ] Tables created in dependency order (referenced tables before referencing tables)
- [ ] Every PK uses a sequence named `{table_name}_seq`
- [ ] Foreign key constraints reference tables already created in the same or earlier migration
- [ ] Column names are `snake_case` of the corresponding JavaBean field name
- [ ] `V001__auth_schema.sql` (or equivalent) is included for Holon Auth tables

## File Naming Convention

```
V001__create_<table>_table.sql
V002__create_<table>_table.sql
...
V0NN__auth_schema.sql
```

## Example Migration

```sql
-- V001__create_order_table.sql

CREATE SEQUENCE order_seq START WITH 1 INCREMENT BY 50 CACHE 50;

CREATE TABLE "order"
(
    id            BIGINT         DEFAULT nextval('order_seq') PRIMARY KEY,
    customer_name VARCHAR(200)   NOT NULL,
    total_amount  DECIMAL(10,2)  NOT NULL CHECK (total_amount >= 0),
    status        VARCHAR(20)    NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    created_at    TIMESTAMP      NOT NULL DEFAULT now()
);
```

```sql
-- V002__create_order_line_item_table.sql

CREATE SEQUENCE order_line_item_seq START WITH 1 INCREMENT BY 50 CACHE 50;

CREATE TABLE order_line_item
(
    id          BIGINT         DEFAULT nextval('order_line_item_seq') PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES "order"(id),
    product     VARCHAR(200)   NOT NULL,
    quantity    INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price  DECIMAL(10,2)  NOT NULL CHECK (unit_price >= 0)
);
```

```sql
-- V003__auth_schema.sql
-- Holon Auth scaffold: adjust table/column names to match your Realm configuration

CREATE SEQUENCE holon_account_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE holon_account (
    id           BIGINT  DEFAULT nextval('holon_account_seq') PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    enabled      BOOLEAN NOT NULL DEFAULT true
);

CREATE SEQUENCE holon_role_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE holon_role (
    id    BIGINT DEFAULT nextval('holon_role_seq') PRIMARY KEY,
    code  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE holon_account_role (
    account_id BIGINT NOT NULL REFERENCES holon_account(id),
    role_id    BIGINT NOT NULL REFERENCES holon_role(id),
    PRIMARY KEY (account_id, role_id)
);

CREATE SEQUENCE holon_permission_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE holon_permission (
    id    BIGINT DEFAULT nextval('holon_permission_seq') PRIMARY KEY,
    code  VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE holon_role_permission (
    role_id       BIGINT NOT NULL REFERENCES holon_role(id),
    permission_id BIGINT NOT NULL REFERENCES holon_permission(id),
    PRIMARY KEY (role_id, permission_id)
);
```

## Workflow

1. Read `docs/entity_model.md`
2. Read existing migrations to determine the next version number
3. Create sequence definitions for each entity
4. Create table definitions with columns, constraints, and foreign keys
5. Order tables so that referenced tables are created before referencing tables
6. Add `auth_schema.sql` migration with Holon Auth tables scaffold
7. Validate:
    - All entities from the entity model have corresponding tables
    - All foreign keys reference tables created in the same or earlier migration
    - Sequence names follow the pattern `{table_name}_seq`
    - SQL syntax is valid for PostgreSQL 16+
    - Run the Pre-Emit Checklist above before committing
