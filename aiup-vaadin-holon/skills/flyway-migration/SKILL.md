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

## Prerequisites

Before starting, verify that `docs/entity_model.md` exists in the project.

| Required artifact | Created by |
|---|---|
| `docs/entity_model.md` | `/entity-model` |

If it is missing, **stop** and tell the user:
> "`docs/entity_model.md` not found. Run `/entity-model` first, then re-run `/flyway-migration`."

Do not attempt to infer or recreate the entity model.

## Instructions

Create Flyway database migration scripts based on `docs/entity_model.md`.
Column names are `snake_case` derived from JavaBean field names (e.g. field `totalAmount`
→ column `total_amount`, matching the `@DataPath` convention documented in
[`../implement/references/bean-model.md`](../implement/references/bean-model.md)).

**Primary key strategy — pick the first option supported by the target database:**

| Priority | Mechanism | Supported by |
|---|---|---|
| 1 | `CREATE SEQUENCE` + `DEFAULT nextval(seq)` | PostgreSQL, Oracle, H2, SQL Server, DB2 |
| 2 | `GENERATED ALWAYS AS IDENTITY` | SQL Server 2019+, Oracle 12c+, H2, DB2 |
| 3 | `AUTO_INCREMENT` | MySQL / MariaDB |

If the target database is not specified, use standard SQL sequences (`CREATE SEQUENCE`)
as they work across the widest set of supported databases. Never use `SERIAL`,
`BIGSERIAL`, or `IDENTITY` (non-standard shorthand forms).

Also emit `V001__auth_schema.sql` (or the next available version number) as a scaffold
for Holon Auth role / permission tables so the `Realm` bootstrap has storage backing.

## Constraints

Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.
Key constraints for this skill:

- **Java 25 / Flyway 10.x / any common SQL database (PostgreSQL, MySQL/MariaDB, H2, Oracle, SQL Server)**
- Column names MUST be `snake_case` of the JavaBean field name
- PKs use standard SQL sequences where the target DB supports them; fall back to
  `AUTO_INCREMENT` (MySQL/MariaDB) or `GENERATED ALWAYS AS IDENTITY` (H2, Oracle 12c+,
  SQL Server 2019+) when sequences are not available — never `SERIAL` / `BIGSERIAL`
- Use `CURRENT_TIMESTAMP` (standard SQL) instead of `now()` for default timestamp values
- Do NOT drop existing tables without explicit user confirmation

## Audit & Version Columns

Every **entity table** (i.e. any table that maps to a domain JavaBean) MUST include the
following five columns. Pure join / association tables (e.g. `holon_account_role`) are
**exempt**.

| Column | Type & default | Maps to |
|---|---|---|
| `created_by` | `VARCHAR(100) NOT NULL DEFAULT 'system'` | Spring `@CreatedBy` |
| `created_date` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` | Spring `@CreatedDate` |
| `last_modified_by` | `VARCHAR(100)` | Spring `@LastModifiedBy` |
| `last_modified_date` | `TIMESTAMP` | Spring `@LastModifiedDate` |
| `version` | `BIGINT NOT NULL DEFAULT 0` | Spring `@Version` (optimistic lock) |

Place these columns at the **end** of the `CREATE TABLE` statement, after all business
columns, so they never shift the position of domain columns during schema review.

## Pre-Emit Checklist

- [ ] All entities from `docs/entity_model.md` have a migration file
- [ ] Tables created in dependency order (referenced tables before referencing tables)
- [ ] Every PK uses an appropriate auto-increment strategy for the target DB (sequence by default; `AUTO_INCREMENT` for MySQL/MariaDB; `GENERATED ALWAYS AS IDENTITY` if sequences are unavailable)
- [ ] Foreign key constraints reference tables already created in the same or earlier migration
- [ ] Column names are `snake_case` of the corresponding JavaBean field name
- [ ] `V001__auth_schema.sql` (or equivalent) is included for Holon Auth tables
- [ ] Every entity table (not pure join tables) ends with `created_by`, `created_date`, `last_modified_by`, `last_modified_date`, `version`

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
-- Works on PostgreSQL, H2, Oracle, SQL Server, DB2 (standard SQL sequences)

CREATE SEQUENCE order_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE "order"
(
    id                 BIGINT         DEFAULT nextval('order_seq') PRIMARY KEY,
    customer_name      VARCHAR(200)   NOT NULL,
    total_amount       DECIMAL(10,2)  NOT NULL CHECK (total_amount >= 0),
    status             VARCHAR(20)    NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    -- audit & version columns (mandatory on every entity table)
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0
);
```

```sql
-- V002__create_order_line_item_table.sql

CREATE SEQUENCE order_line_item_seq START WITH 1 INCREMENT BY 50;

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
    enabled      SMALLINT NOT NULL DEFAULT 1
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
    - SQL syntax uses only standard SQL constructs (no database-specific extensions unless the target DB was specified)
    - Run the Pre-Emit Checklist above before committing
