# Requirements — Minimal CRM

Derived from [`vision.md`](vision.md). IDs are stable and traceable to use cases and tests.

## Functional Requirements

| ID | Requirement | Priority | Traces to |
|----|-------------|----------|-----------|
| FR-001 | A sales rep can register a new customer company (name, industry, status). | Must | UC-001 |
| FR-002 | A sales rep can view a list of all active customers. | Must | UC-001 |
| FR-003 | Customer company names must be unique. | Must | UC-001 |
| FR-004 | A sales rep can add a contact person to an existing customer. | Must | UC-002 |
| FR-005 | A sales rep can view the contacts belonging to a customer. | Must | UC-002 |
| FR-006 | A sales manager can archive a customer, hiding it from the active list. | Should | UC-001 |

## Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-001 | Every route requires an authenticated user (Holon Auth). |
| NFR-002 | Archiving a customer requires the `customers:archive` permission. |
| NFR-003 | Persistence uses PostgreSQL 16+ via the Holon JDBC Datastore. |
| NFR-004 | Database schema is managed exclusively through Flyway migrations. |

## Roles & Permissions

| Role | Permissions |
|------|-------------|
| `SALES_REP` | `customers:view`, `customers:create`, `contacts:view`, `contacts:create` |
| `SALES_MANAGER` | all `SALES_REP` permissions + `customers:archive` |
