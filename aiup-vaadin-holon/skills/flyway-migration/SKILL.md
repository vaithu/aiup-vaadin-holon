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

## Where the migrations go (holon-saas, schema per tenant)

Every project in this stack uses holon-saas (see `holon-stack.md`), which keeps **one platform
schema** and **one schema per company**. They are migrated from two different folders:

| Folder | Runs against | Contains | Version numbers |
|---|---|---|---|
| `src/main/resources/db/tenant-migration/` | each company's own schema, once per company, applied by holon-saas when it provisions the company | every entity of the entity model **except** those marked `**Schema:** platform` | **V2 / V002 and up**. holon-saas reserves `V1` and `V1.x` for its own scripts |
| `src/main/resources/db/migration/` | the one platform schema, applied by the application's own Flyway (`spring.flyway.locations=classpath:db/migration`) | the `tenant`, `tenant_features` and `tenant_attributes` tables holon-saas expects the application to create, plus every entity marked `**Schema:** platform` (for example the login account) | `V001` and up |

Rules:

- A table in the wrong folder is created in every company's schema, or not at all. Decide from the entity model's `**Schema:**` line; an entity without one is a tenant table.
- **Never create a table whose name is reserved by the framework**: `tenant_users`, `audit_entries`, `user_invitation`, `tenant_permissions`, `tenant_custom_roles`, `tenant_billing_*`, `whatsapp_*`, `tenant_setting_override`, `tenant_plan_setting_default`, `tenant_settings`, `tenant_feature_flags`, `tenant_audit_log` (the full list is `FrameworkReservedTableNames.ALL` in `tenant-core`).
- An entity documented as provided by the framework (such as `APP_USER`, which is `tenant_users`) gets **no migration**; foreign keys to it reference `tenant_users(id)`.
- **Do not emit the Holon Auth scaffold** (`holon_account`, `holon_role`, …). Users, invitations and audit are framework tables, and login credentials live in the platform schema behind the application's `AccountCredentialsStore`.
- The platform `tenant` table mirrors `TenantDetails` exactly (`tenant_id`, `name`, `plan`, `status`, `locale`, `timezone`, `theme`, plus the `tenant_features` and `tenant_attributes` element collections). Read the entity in `holon-saas/tenant-core` of the version in use before writing it.

## Generating the migrations (the generator)

Do not hand-write 100 tables. Run the generator, which reads `docs/entity_model.md` and writes one
file per table in dependency order:

```
node <plugin>/skills/flyway-migration/references/generate-migrations.js \
     --model docs/entity_model.md --out src/main/resources/db [--unique unique.json] [--dry-run]
```

| Option | Meaning |
|---|---|
| `--model` | the entity model (default `docs/entity_model.md`) |
| `--out` | the `db` folder; writes `tenant-migration/` and `migration/` under it (default `src/main/resources/db`) |
| `--tenant-start`, `--platform-start` | first version numbers (defaults 2 and 1) |
| `--unique` | a JSON file of composite unique constraints, `{ "ENTITY": ["col1", "col2"] }` |
| `--external` | entities provided by the framework, which get no migration (default `APP_USER=tenant_users`) |
| `--no-tenant-tables` | skip the platform `tenant`, `tenant_features`, `tenant_attributes` file |
| `--dry-run`, `--force` | report only; overwrite existing generated files |

What it does: maps the entity model's types and rules to SQL (`DateTime` becomes
`TIMESTAMP WITH TIME ZONE`, an optional foreign key is read from "References X" in the description),
puts a table in `db/migration` when its section has `**Schema:** platform`, adds the audit and version
defaults, a sequence with `INCREMENT BY 50`, `CHECK` constraints from `Min`/`Max`, and an index on every
foreign key.

What it checks, and stops without writing anything: a table or column that is a reserved word, a table
name reserved by holon-saas, a foreign key to a missing entity or across the platform and tenant
schemas, a `Values:` rule (a categorical string that belongs in a lookup table), an identifier longer
than PostgreSQL's 63 characters, and a dependency cycle. It refuses to overwrite existing
migrations unless `--force`, because they may have been edited.

What it cannot do, so do it by hand afterwards and say so in the report:

- **Composite and cross-column rules** written as `**Constraints:**` prose. Only the composite unique
  constraints you list in `--unique` are generated.
- **Extra composite indexes** for a particular query, such as a time-window count.
- **Lookup and seed data**, and any data migration.
- **The platform `tenant` tables** follow the holon-saas version in the script's template; compare them with
  `TenantDetails` of the version in use.

A migration the generator wrote and a person then edited is the reason for the overwrite guard: regenerate
into an empty folder and compare, or edit the entity model and `--unique` and regenerate.

## Constraints

Read [`../../rules/holon-stack.md`](../../rules/holon-stack.md) before generating.
Key constraints for this skill:

- **Java 25 / Flyway 10.x / any common SQL database (PostgreSQL, MySQL/MariaDB, H2, Oracle, SQL Server)**
- Column names MUST be `snake_case` of the JavaBean field name
- PKs use standard SQL sequences where the target DB supports them; fall back to
  `AUTO_INCREMENT` (MySQL/MariaDB) or `GENERATED ALWAYS AS IDENTITY` (H2, Oracle 12c+,
  SQL Server 2019+) when sequences are not available — never `SERIAL` / `BIGSERIAL`
- Use `CURRENT_TIMESTAMP` (standard SQL) instead of `now()` for default timestamp values
- A `DateTime` attribute is `TIMESTAMP WITH TIME ZONE` (PostgreSQL `TIMESTAMPTZ`), never a plain `TIMESTAMP`: the bean field is an `Instant` (`JPA-008`)
- Check every column name against the reserved words of H2 and PostgreSQL (`year`, `value`, `level`, `user`, `order`, `key`, …). Rename the attribute in the entity model instead of quoting the column
- Create an index on every foreign key column (PostgreSQL does not)
- Composite uniqueness and cross-column rules written as `**Constraints:**` prose in the entity model are not machine-readable: add the obvious composite `UNIQUE` constraints by hand and list the rest as enforced in the service
- Do NOT drop existing tables without explicit user confirmation
- **JPA schema anti-patterns** — see [`../../rules/jpa-anti-patterns.md`](../../rules/jpa-anti-patterns.md).
  Flyway owns the schema, so:
  - `JPA-051` — the app must run with `spring.jpa.hibernate.ddl-auto=validate` (or `none`);
    if the config says `create` / `create-drop` / `update`, flag it and fix it as part of this task.
  - `JPA-007` — a sequence's `INCREMENT BY` MUST equal the entity's
    `@SequenceGenerator(allocationSize = …)`. Mismatched values silently corrupt id allocation.
  - `JPA-008` — timestamp columns backing an `Instant` field use `TIMESTAMPTZ`
    (`TIMESTAMP WITH TIME ZONE`), never a naive `TIMESTAMP`.
  - `JPA-005` — every categorical/status value gets its own lookup table + FK; never a
    `VARCHAR` mirroring a Java enum.

## Audit & Version Columns

Every **entity table** (i.e. any table that maps to a domain JavaBean) MUST include the
following five columns. Pure join / association tables (e.g. `holon_account_role`) are
**exempt**.

| Column | Type & default | Maps to |
|---|---|---|
| `created_by` | `VARCHAR(100) NOT NULL DEFAULT 'system'` | Spring `@CreatedBy` |
| `created_date` | `TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP` | Spring `@CreatedDate` (`Instant`) |
| `last_modified_by` | `VARCHAR(100)` | Spring `@LastModifiedBy` |
| `last_modified_date` | `TIMESTAMP WITH TIME ZONE` | Spring `@LastModifiedDate` (`Instant`) |
| `version` | `BIGINT NOT NULL DEFAULT 0` | Spring `@Version` (optimistic lock) |

Place these columns at the **end** of the `CREATE TABLE` statement, after all business
columns, so they never shift the position of domain columns during schema review.

## Pre-Emit Checklist

- [ ] All entities from `docs/entity_model.md` have a migration file
- [ ] Tables created in dependency order (referenced tables before referencing tables)
- [ ] Every PK uses an appropriate auto-increment strategy for the target DB (sequence by default; `AUTO_INCREMENT` for MySQL/MariaDB; `GENERATED ALWAYS AS IDENTITY` if sequences are unavailable)
- [ ] Foreign key constraints reference tables already created in the same or earlier migration
- [ ] Column names are `snake_case` of the corresponding JavaBean field name
- [ ] Each table is in the right folder (`db/tenant-migration` from V002, or `db/migration` for `**Schema:** platform`); no framework-reserved table name; no Holon Auth scaffold
- [ ] Every entity table (not pure join tables) ends with `created_by`, `created_date`, `last_modified_by`, `last_modified_date`, `version`
- [ ] Sequence `INCREMENT BY` matches the entity's `@SequenceGenerator(allocationSize = …)` (`JPA-007`)
- [ ] Every `DateTime` column, including the audit ones, is `TIMESTAMP WITH TIME ZONE` (`JPA-008`)
- [ ] Every foreign key column has an index; no column is a reserved word in H2 or PostgreSQL
- [ ] The scripts were loaded into H2 in version order and ran without error (see the last workflow step)
- [ ] Every categorical value has a lookup table + FK — no enum-mirroring `VARCHAR` columns (`JPA-005`)
- [ ] `spring.jpa.hibernate.ddl-auto` is `validate` or `none` — Flyway owns the schema (`JPA-051`)

## File Naming Convention

```
V002__create_<table>_table.sql      (tenant-migration: V002 and up, one table per file, dependency order)
V003__create_<table>_table.sql
...
V001__create_tenant_tables.sql      (migration: the platform schema, V001 and up)
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
    status_id          BIGINT         NOT NULL REFERENCES order_status(id),   -- lookup table, never a CHECK (...) over enum values (JPA-005)
    -- audit & version columns (mandatory on every entity table)
    created_by         VARCHAR(100)   NOT NULL DEFAULT 'system',
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP WITH TIME ZONE,
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
-- Only for a project that does NOT use holon-saas. In this stack, skip it (see "Where the migrations go").
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
3. Run the generator (see "Generating the migrations"); it creates the sequences, the tables with their columns, constraints and foreign keys, and orders the tables so that referenced tables come first. Without it, create these by hand
4. Review what it cannot generate: composite constraints, query-specific indexes, seed data
5. Add anything the model states only as prose
6. The generator already wrote the platform schema migrations (`tenant` tables and any `**Schema:** platform` entity). Add the Holon Auth scaffold only if the project does not use holon-saas
7. Validate:
    - All entities from the entity model have corresponding tables
    - All foreign keys reference tables created in the same or earlier migration
    - Sequence names follow the pattern `{table_name}_seq`
    - SQL syntax uses only standard SQL constructs (no database-specific extensions unless the target DB was specified)
    - Run the Pre-Emit Checklist above before committing
8. **Load the scripts into H2 before reporting.** Concatenate the framework's tenant scripts (`V1.1__tenant_framework_module_tables.sql` from `holon-saas`) and then every file of `db/tenant-migration` in version order, and run them with `java -cp h2.jar org.h2.tools.RunScript -url jdbc:h2:mem:t -script all.sql`; do the same for `db/migration`. A script that fails here fails when a company is provisioned. This does not prove PostgreSQL compatibility; say so in the report.
