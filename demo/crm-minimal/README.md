# MiniCRM — `aiup-vaadin-holon` demo module

A **minimal CRM** application generated with the [`aiup-vaadin-holon`](../../README.md)
plugin marketplace. It demonstrates the full **AI Unified Process (AIUP)** flow — from a
written vision through requirements, an entity model, and use case specs, and only then into
a **Holon Platform + Vaadin Flow** implementation.

> **Why a `demo/` folder and not `src/`?** The repository root is the plugin marketplace and
> deliberately has no application `src/` ([`CLAUDE.md`](../../CLAUDE.md)). This self-contained
> demo lives under `demo/crm-minimal/` so it illustrates the plugin's *output* without turning
> the marketplace repo itself into an application.

## What it does

Two entities, two use cases, Holon Auth on every route:

| Use case | View | Actor | Guard |
|----------|------|-------|-------|
| [UC-001 Manage Customers](docs/use_cases/UC-001-manage-customers.md) | `/customers` | Sales Rep / Manager | `customers:view` (+ `customers:archive` for archiving) |
| [UC-002 Manage Contacts](docs/use_cases/UC-002-manage-contacts.md) | `/contacts` | Sales Rep | `contacts:view` |

## Module layout

```
demo/crm-minimal/
├── pom.xml                                   # Holon BOM + Vaadin BOM + Spring Boot parent
├── docs/                                     # AIUP artifacts (the "why" before the "how")
│   ├── vision.md
│   ├── requirements.md                       # FR-/NFR- traceable to use cases
│   ├── entity_model.md                       # Mermaid ER diagram + @DataPath mapping
│   └── use_cases/
│       ├── UC-001-manage-customers.md
│       └── UC-002-manage-contacts.md
└── src/main/
    ├── java/com/example/crm/
    │   ├── CrmApplication.java               # @SpringBootApplication (only Spring stereotype)
    │   ├── config/CrmConfig.java             # Holon Context service registration + Realm bootstrap
    │   ├── domain/Customer.java              # JavaBean + @DataPath/@Identifier + BeanPropertySet
    │   ├── domain/Contact.java
    │   ├── service/CustomerService.java      # Datastore-backed, Context-wired (no @Service)
    │   ├── service/ContactService.java
    │   └── ui/
    │       ├── CustomerListView.java         # PropertyListing + @Permitted + AuthContext guard
    │       └── ContactListView.java
    └── resources/
        ├── application.yaml
        └── db/migration/                     # Flyway V*.sql — sequences, never SERIAL/IDENTITY
            ├── V001__create_customer_table.sql
            ├── V002__create_contact_table.sql
            └── V003__auth_schema.sql          # Holon Auth tables + seeded roles/permissions
```

---

## How this module was created (step by step)

Each construction step maps to a plugin skill. In Claude Code or GitHub Copilot Chat — with
`aiup-core` and `aiup-vaadin-holon` installed (see the [root README](../../README.md#installation))
— the module was produced by running the skills below **in order**. The AIUP rule is *nothing
gets built without a use case*, so the elaboration artifacts come first.

### 0. Scaffold the module directory

```sh
mkdir -p demo/crm-minimal/docs/use_cases
mkdir -p demo/crm-minimal/src/main/java/com/example/crm/{config,domain,service,ui}
mkdir -p demo/crm-minimal/src/main/resources/db/migration
```

### 1. Inception — write the vision, then generate requirements

The vision is hand-written; requirements are generated from it.

```
# in Copilot Chat / Claude Code, from the module root
/requirements
```

→ reads `docs/vision.md`, produces `docs/requirements.md` (FR-001…FR-006, NFR-001…NFR-004,
and the role/permission matrix).

### 2. Elaboration — entity model and use case diagram

```
/entity-model
/use-case-diagram
```

→ `/entity-model` produces `docs/entity_model.md` with the Mermaid ER diagram and the
`@DataPath` (snake_case) column mapping for `Customer` and `Contact`.

### 3. Construction — use case specifications

```
/use-case-spec UC-001
/use-case-spec UC-002
```

→ writes `docs/use_cases/UC-001-manage-customers.md` and `UC-002-manage-contacts.md`
(main scenario, alternative flows, business rules `BR-001`…`BR-004`, traceability).

### 4. Construction — database migrations

```
/flyway-migration
```

→ reads `docs/entity_model.md` and emits `V001…V003` under `src/main/resources/db/migration/`.
Primary keys use sequences (`customer_seq`, `contact_seq`) — never `SERIAL`/`IDENTITY` — and
`V003__auth_schema.sql` scaffolds the Holon Auth tables and seeds the `SALES_REP` /
`SALES_MANAGER` roles and their permissions.

### 5. Construction — implement each use case

```
/implement UC-001
/implement UC-002
```

Each `/implement` run produced the three Holon layers, strictly following
[`rules/holon-stack.md`](../../aiup-vaadin-holon/rules/holon-stack.md):

1. **Domain** — `Customer` / `Contact` plain JavaBeans with `@DataPath` + `@Identifier` and a
   `public static final BeanPropertySet<T> PROPERTIES` constant. **No `PropertyBox`.**
2. **Service** — `CustomerService` / `ContactService`: plain classes that fetch the `Datastore`
   from `Context.get()` and expose a `CONTEXT_KEY`. **No `@Service`/`@Autowired`.**
3. **View** — `CustomerListView` / `ContactListView`: Holon `PropertyListing`,
   `Components.input.*`, `@Permitted` route guards, and `AuthContext.require().isPermitted(...)`
   to hide the manager-only Archive button.

Holon `Context` wiring and the Holon Auth `Realm` were added in `config/CrmConfig.java`; the
only Spring stereotype in the whole module is `@SpringBootApplication` on `CrmApplication`.

### 6. (Optional) Generate tests

Not included in this minimal demo, but the same flow continues with:

```
/datastore-test UC-001
/holon-vaadin-test UC-001
/playwright-test UC-001
```

---

## Verifying the Holon-only rule

The plugin bans `PropertyBox`, `@Autowired`, Spring Security, and Spring Data, and restricts
Spring stereotypes to classes that need Spring lifecycle (this minimal demo needs none). Verify the
module obeys the rule (matches below appear only inside explanatory Javadoc comments):

```sh
cd demo/crm-minimal

# No PropertyBox in real code
grep -rn 'PropertyBox' src/

# This minimal demo uses no Spring stereotypes and no @Autowired (constructor injection / Holon Context only)
grep -rn '@Service\|@Component\|@Repository\|@Autowired' src/
grep -rn 'org.springframework.security\|org.springframework.data\|jakarta.persistence' src/
```

---

## Build & run

### Prerequisites

- Java 25 (`java -version` → 25)
- Maven 3.9+
- Docker (for a throwaway PostgreSQL) or a local PostgreSQL 16+

### 1. Start PostgreSQL

```sh
docker run --name minicrm-pg -e POSTGRES_DB=minicrm \
  -e POSTGRES_USER=crm -e POSTGRES_PASSWORD=crm \
  -p 5432:5432 -d postgres:16
```

### 2. Build (Flyway runs the migrations on first start)

```sh
cd demo/crm-minimal
mvn clean package
```

### 3. Run

```sh
mvn spring-boot:run
```

Then open:

- <http://localhost:8080/customers>
- <http://localhost:8080/contacts>

Both routes require an authenticated Holon Auth principal. Insert an account into the
`holon_account` table and map it to a role (`SALES_REP` or `SALES_MANAGER`) in
`holon_account_role` — the roles and their permissions are already seeded by
`V003__auth_schema.sql`.

---

## Security note

The PostgreSQL JDBC driver is pinned to **42.7.11** in `pom.xml`. Earlier 42.7.x releases are
affected by [a SCRAM CPU-exhaustion DoS](https://github.com/advisories) and a channel-binding
fallback issue; 42.7.11 is the patched version. Bump deliberately and re-check advisories when
upgrading.

## Version pins

Matches [`aiup-vaadin-holon/rules/holon-stack.md`](../../aiup-vaadin-holon/rules/holon-stack.md):

| Component | Version |
|-----------|---------|
| Java | 25 |
| Holon Core / Auth / JDBC | 10.0.0 |
| Holon Vaadin Flow | 10.0.1 |
| Vaadin Flow | 25.2.1 |
| Spring Boot | 4.1.0 |
| Flyway | (managed by Spring Boot parent) |
| PostgreSQL JDBC | 42.7.11 |
