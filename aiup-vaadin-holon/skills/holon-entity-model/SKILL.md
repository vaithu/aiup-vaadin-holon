---
name: holon-entity-model
description: >
  Creates or updates docs/entity_model.md for a Holon Platform + holon-saas project without rewriting
  it: reads the existing model, the requirements, the use case specifications and the ADRs, adds or
  changes only what they require, applies the stack conventions (schema per tenant, platform entities,
  framework-provided entities, audit columns, lookup tables, reserved words), and validates the result
  with a script. Use when the user asks to "update the entity model", "add an entity", "add attributes
  to the entity model", "extend the data model for UC-XXX", "keep the entity model in sync with the
  specs", "add a lookup", or mentions entity_model.md together with holon-saas, Holon, or the generated
  migrations. Once docs/entity_model.md exists, use this instead of /entity-model.
argument-hint: "[what changed, e.g. UC-074, FR-110 or ADR-002]"
---

# Holon Entity Model

`/entity-model` (aiup-core) builds a model **from the requirements** and, as written, rewrites the whole
file. On a model that has grown past the requirements, that destroys work. This skill **updates in
place**, takes its input from the specifications as well as the requirements, and applies the Holon +
holon-saas conventions that the generic skill cannot know.

| Situation | Use |
|---|---|
| No `docs/entity_model.md` yet | this skill creates it, in the same format as `/entity-model` |
| `docs/entity_model.md` exists | this skill. **Do not run `/entity-model` on an existing model.** |

## Prerequisites

| Needed | Why |
|---|---|
| `docs/requirements.md` | the source of the first model |
| `docs/entity_model.md` | optional; when present the skill is in update mode |
| the file is protected | the project is under git with `docs/entity_model.md` committed, **or** you first copy it to `docs/entity_model.md.bak`. If neither is true, **stop and ask** |

Do not proceed without protection: an edit that goes wrong must be recoverable.

## Read first

1. `docs/entity_model.md` completely: every entity name, and the conventions it already follows.
2. `docs/requirements.md`, and the requirement or requirements named in `$ARGUMENTS`.
3. `docs/use_cases/UC-*.md`: attributes hide in main flows ("System records the reason"), in business
   rules (limits, statuses, "history is kept") and in postconditions.
4. `docs/business_rules.md` (`GR-NNN`) and `docs/architecture/adr/` for decisions about where data lives.
5. `rules/holon-stack.md` and `rules/country-variation.md` of this plugin.

## Workflow

1. **Protect** the file (see above).
2. **Find what the change needs.** From `$ARGUMENTS`, or by scanning the specifications for data the model
   does not hold. Write the list: *entity or attribute, which spec or ADR needs it*. Anything not clearly
   required by a specification is **asked, not added**.
3. **Edit in place.** Add an entity, add or change an attribute, add a relationship line. Every
   section you do not touch stays exactly as it was. Never delete or rename an entity or an
   attribute without asking: other documents, migrations and code refer to them by name.
4. **Apply the conventions** below to everything you add.
5. **Validate**, and fix until there are no errors:

   ```
   node <plugin>/skills/holon-entity-model/references/validate-entity-model.js docs/entity_model.md
   ```

6. **Check the migrations would still generate** (no files are written):

   ```
   node <plugin>/skills/flyway-migration/references/generate-migrations.js --dry-run
   ```

7. **Report** what you added and changed, the assumptions you made, and the documents that are now
   out of step (below). Say what you did not do.

## Conventions

The format is `/entity-model`'s: a `### UPPER_SNAKE` heading, one description sentence, a table with
exactly the columns `Attribute | Description | Data Type | Length/Precision | Validation Rules`, the closed
vocabulary for types (`Long`, `String`, `Integer`, `Decimal`, `Boolean`, `Date`, `DateTime`) and rules
(`Primary Key, Sequence`, `Not Null`, `Not Null, Unique`, `Not Null, Foreign Key (TABLE.id)`, `Optional`,
`Not Null, Min: X, Max: Y`, `Not Null, Values: …`, `Not Null, Format: Email`), and a Mermaid diagram of
relationships only. On top of that, for this stack:

| Rule | Detail |
|---|---|
| **Key** | `id`, `Long`, `19`, `Primary Key, Sequence` on every entity |
| **Audit columns** | every entity except framework-provided ones ends with exactly: `created_by` String 100 Not Null, `created_date` DateTime Not Null, `last_modified_by` String 100 Optional, `last_modified_date` DateTime Optional, `version` Long 19 Not Null |
| **Tenant** | schema per tenant: **no** `tenant_id` and **no** country column on a tenant entity. The operating country and every company-wide setting (limits, thresholds, switches, hours) are holon-saas tenant settings, never a column or an entity |
| **Framework-provided entities** | users (`APP_USER`, the table `tenant_users`), invitations, the audit log and billing belong to holon-saas. Document one only so that foreign keys have a target: a description saying it is provided by the framework, the framework's own columns, no audit columns, no migration. List it in the generator's `--external` option |
| **Platform schema** | an entity that must be read **before a tenant is known** (login account, sign-in lock, sign-up and recovery codes) gets a line `**Schema:** platform` right after its description. A platform entity never has a foreign key to a tenant entity, nor the reverse |
| **Lookups** | a status, type, stage, reason or category is its own entity: `id`, `code` String 30 Unique, `name` String 100, `sort_order` Integer, `active` Boolean, plus the audit columns. **Never** an enum, never a `Values:` rule on a string |
| **Optional foreign keys** | the rule is `Optional` and the description contains `References ENTITY` in exactly that form (`; References PAYMENT_TERM`). The migration generator reads that sentence, and without it **no constraint is created**. A polymorphic `reference_id` is exempt |
| **Numbers** | money `Decimal` `15,2`, quantity `15,3`, percentage `5,2` |
| **Dates** | `DateTime` is an `Instant` in the bean and `TIMESTAMP WITH TIME ZONE` in the table |
| **Names** | attributes `lower_snake_case`; table = entity name lower-cased. A name that is a reserved word in H2 or PostgreSQL (`year`, `value`, `level`, `user`, `order`, `key`, …) is renamed, not quoted. A table name reserved by holon-saas (`tenant_users`, `audit_entries`, `user_invitation`, …) is not used |
| **Country policy** | registration identifiers, tax codes, payment methods, chart of accounts are **rows in lookup tables**, never columns on a business entity |
| **Tax and totals** | a document line keeps a snapshot (`tax_code_id`, rate, amount); the document total is the sum of its lines, stated under Constraints |
| **Immutable records** | movements, history, issued and posted documents: say so under `**Constraints:**` |
| **Constraints line** | what the vocabulary cannot say (composite uniqueness, cross-column rules, state transitions) goes in a `**Constraints:**` paragraph after the table. Composite unique constraints are also passed to the generator with `--unique` |

## What the validator checks

Format (headings, five columns, closed vocabularies, bare lengths, a diagram with relationships only, every
entity in the diagram and the reverse, no duplicates) and stack rules (the `id` key, the five audit columns,
no `tenant_id`, no reserved words, no `Values:` rule, no foreign key across the platform and tenant schemas,
no unknown foreign key target). It **warns** about an optional foreign key without `References ENTITY` and
about a decimal precision outside `15,2`, `15,3`, `5,2`. Errors fail the run; fix them, do not edit the script.

## DO NOT

- Run `/entity-model` on an existing model.
- Rewrite the file, reorder entities, or "tidy" sections you were not asked to change.
- Delete or rename an entity or attribute without asking.
- Invent an entity the specifications do not need.
- Put a company setting, a country, or a tenant id on a business entity.
- Model a category as a string with fixed values.
- Weaken the validator to make a run pass.

## After the update

An updated model leaves other documents behind. Name them in the report:

| Document | What to do |
|---|---|
| migrations | `flyway-migration` generates a new table with the generator. Do not regenerate tables that have hand-tuned indexes; add the new files with the next version number |
| `docs/architecture/process.md`, `logical.md` | a new platform entity, a new lock, a new scheduled job or a new dependency belongs there, or in an ADR |
| use case specs | attribute names used in prose should match the model |
| seed data | every new lookup needs its values in a data migration |
| shared rules | rules that now appear in several specs are candidates for `/business-rules` |

## Example

UC-018 and UC-074 describe an adjustment that can be "held" and later approved. The model has no record
of an adjustment, so the skill adds `STOCK_ADJUSTMENT` (product, warehouse, reason, status, signed quantity,
requester, decision fields, optional movement) and the lookup `ADJUSTMENT_STATUS`, adds eight relationship
lines to the diagram, writes the rules the table cannot say (a held adjustment changes nothing; decided
once; a rejection needs a reason) under `**Constraints:**`, states that the limits are tenant settings and
not columns, runs the validator, and lists the migrations, the process view and the seed data as still to do.
