# aiup-vaadin-holon

AI Unified Process construction plugin for the **Holon Platform + Vaadin Flow** stack.

Pair with `aiup-core` for the full AIUP workflow: vision → requirements → entity model →
use case diagram → use case spec → **flyway migration → implement → test**.

---

## Skill Execution Sequence

Skills must be run in order. Each skill includes a **`## Prerequisites` guard clause** — if
required artifacts are missing, the skill stops immediately and tells you which skill to run
first.

```
/requirements  ──→  /entity-model  ──→  /use-case-diagram  ──→  /use-case-spec UC-XXX
                                                                         │
                                                              ┌──────────┘
                                                              │
                                                    /flyway-migration          (reads entity_model.md)
                                                              │
                                                    /implement UC-XXX          (reads use case spec + entity model + V*.sql)
                                                              │
                                              ┌───────────────┼───────────────┐
                                              │               │               │
                                  /datastore-test UC-XXX     │    /holon-vaadin-test UC-XXX
                                                             │
                                              /test-case UC-XXX …
                                                             │
                                              /playwright-test TC-XXX
```

| # | Skill | Key prerequisite |
|---|-------|-----------------|
| 1 | `/requirements` | `docs/vision.md` |
| 2 | `/entity-model` | `docs/requirements.md` |
| 3 | `/use-case-diagram` | `docs/requirements.md` |
| 4 | `/use-case-spec UC-XXX` | `docs/requirements.md` + `docs/use_cases.puml` |
| 5 | `/flyway-migration` | `docs/entity_model.md` |
| 6 | `/implement UC-XXX` | use case spec + entity model + Flyway migrations |
| 7 | `/test-case UC-XXX …` | `docs/use_cases/UC-XXX-*.md` |
| 8 | `/datastore-test`, `/holon-vaadin-test`, `/playwright-test` | implemented service/view + Flyway migrations |

> **Why schema-first?** `@DataPath` field names on JavaBeans must match the exact column names
> produced by the Flyway migrations. Migrations are generated from the entity model *before*
> Java code is written, keeping field-to-column mapping unambiguous. JPA `ddl-auto` is not
> applicable (`ddl-auto=none`; Flyway owns the schema). Domain beans stay plain JavaBeans:
> `jakarta.persistence.*` is banned in application code, with a narrow exception for the
> `holon-saas` entities and wiring named in `rules/holon-stack.md`.

---

## Skills

| Skill | Slash command | Reads | Writes |
|-------|--------------|-------|--------|
| Flyway Migration | `/flyway-migration` | `docs/entity_model.md` | `src/main/resources/db/migration/V*.sql` |
| Implement | `/implement UC-XXX` | use case spec + entity model | JavaBean, BeanPropertySet, Datastore service, Holon Vaadin view, Holon Auth guards |
| Implement from HTML | `/implement-from-html <file>` | HTML mockup | same outputs as `/implement` (inferred) |
| Datastore Test | `/datastore-test UC-XXX` | use case spec | JUnit 5 + Testcontainers integration tests |
| Holon Vaadin Test | `/holon-vaadin-test UC-XXX` | use case spec | Vaadin Browserless server-side tests |
| Playwright Test | `/playwright-test UC-XXX` | use case spec | Browser E2E tests |
| Traceability Sensors | `/traceability-sensors` | `docs/use_cases/`, `docs/test_cases/`, `docs/business_rules.md` | `@UseCase` / `@TestCase` annotations + three JUnit sensors that read `docs/` from disk |
| Architecture Rules | `/architecture-rules` | `rules/holon-stack.md` | `ArchitectureTest` (ArchUnit) + test-layer and source-level convention sensors, plus `@Fallback` |
| Coverage Check | `/coverage-check UC-XXX` | use case spec + its tests | An audit verdict per flow — run before raising `Status:` to `Done` |
| Session Guards | `/session-guards` | the installed sensor classes | `.claude/hooks/` + `.claude/settings.json` (needs Git Bash + `jq`) |

---

## The harness

The skills above fall into four layers, each answering a different question:

| Layer | Question | Parts |
|-------|----------|-------|
| **Guides** | What should the agent do? | `CLAUDE.md`, `docs/`, the 4+1 views, the ADRs, `rules/holon-stack.md` |
| **Sensors** | Does the repo still agree with the guides? | `/traceability-sensors`, `/architecture-rules` |
| **Guards** | Did this session actually run the sensors? | `/session-guards` |
| **Backstop** | Does it hold for everybody else? | `mvn verify` in CI (`traceability-sensors/references/ci.md`) |

The dividing line, and where a new rule belongs, is in **`rules/harness.md`**:

> If a rule can be checked by reading the repository, it is a test.
> Only a rule about what happened during a session is a hook.

**`**Status:** Done` is an executable assertion**, not a label — it switches the
coverage sensor on for that use case. Referential integrity is checked for every
spec; coverage only for specs claiming completion.

**Test names carry traceability:** `UC<NNN><Name>Test` (Surefire),
`UC<NNN><Name>IT` and `TC<NNN><Name>IT` (Failsafe). `mvn -q test -Dgroups=sensor`
runs the sensors in seconds, with no Docker.

---

## Prerequisites

Your project must have:

- **Java 25**
- **Maven or Gradle** with the Holon Platform BOMs imported:
  ```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.holon-platform.vaadin</groupId>
        <artifactId>holon-vaadin-flow-bom</artifactId>
        <version>12.0.1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin-bom</artifactId>
        <version>25.3.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  ```
  > **Note:** Holon 12.x is built from the fork at https://github.com/vaithu/holon-vaadin-flow.
  > Run `mvn install` on the fork before using these artifacts locally.
- **Vaadin 25.3** (pulled in via `holon-vaadin-flow-spring-boot` starter)
- **Spring Boot 4.1.x** (bootstrap runtime)
- **Flyway 10.x**, **PostgreSQL 16+**
- `docs/vision.md` at the project root

---

## Stack version pins

| Component | Version | Source of truth |
|-----------|---------|-----------------|
| Java | 25 | — |
| Holon Vaadin Flow BOM | `com.holon-platform.vaadin:holon-vaadin-flow-bom:12.0.1` | https://github.com/vaithu/holon-vaadin-flow |
| Holon Core | 12.0.0 | https://github.com/vaithu/holon-vaadin-flow |
| Holon JPA Datastore (`BeanDatastore`, `BeanDatastoreHelper`) | `holon-datastore-jpa-spring-boot:12.0.0` | https://github.com/vaithu/holon-vaadin-flow |
| Spring JPA (fallback) | via Spring Boot 4.1.1 | — |
| Holon Vaadin Flow | 12.0.1 | https://github.com/vaithu/holon-vaadin-flow |
| Vaadin Flow | 25.3.0 | — |
| Spring Boot | 4.1.1 | — |
| Flyway | 10.x | — |
| PostgreSQL | 16+ | — |
| JUnit | 5 | — |
| Testcontainers | latest stable | — |
| Playwright | latest stable | — |

See [`rules/holon-stack.md`](rules/holon-stack.md) for the full allow/ban list.

---

## Allow / Ban summary

### Allowed

- `com.holon-platform.*` — all Holon modules
- `com.vaadin.*` — **BANNED** for UI components; if no Holon equivalent exists, stop and ask the developer
- `org.springframework.boot:spring-boot-starter` + `holon-spring-boot-*` starters
- `org.springframework.stereotype.{Service,Component,Repository}` — only when Spring lifecycle is required; Holon `Context` preferred; constructor injection
- `org.flywaydb.*`, `org.postgresql.*`, `org.junit.jupiter.*`, `org.testcontainers.*`, `com.microsoft.playwright.*`

### Banned (skills refuse to emit)

- `com.holonplatform.core.property.PropertyBox` — **use `Bean` + `BeanPropertySet` exclusively**
- `jakarta.persistence.*` / `javax.persistence.*` unless Holon JPA Datastore is explicitly required
- `org.springframework.data.jpa.*`, `org.springframework.data.repository.*`
- `org.springframework.web.bind.annotation.*` (no Spring MVC)
- `org.springframework.beans.factory.annotation.Autowired` — use constructor injection (Holon `Context` preferred)
- `org.springframework.security.*` for auth (use Holon Auth)

---

## Worked example: `/implement-from-html bills.html`

Given `bills.html` — an Accounts Payable master-detail mockup with 3-way match,
approval chain, and roles: **AP Reviewer**, **Finance Director**, **Receiver** —
running `/implement-from-html bills.html` produces:

```
src/main/java/com/example/ap/
├── domain/
│   ├── Bill.java                        # @DataPath("bill") JavaBean
│   ├── BillLineItem.java                # @DataPath("bill_line_item") JavaBean
│   ├── PurchaseOrder.java               # @DataPath("purchase_order") JavaBean
│   └── GoodsReceipt.java                # @DataPath("goods_receipt") JavaBean
├── service/
│   ├── BillService.java                 # Datastore-backed, Context-wired
│   └── ApprovalService.java             # Holon Auth permission checks
├── ui/
│   ├── BillListView.java                # @Route, PropertyListing<Bill>
│   └── BillDetailView.java              # PropertyForm<Bill>, approval buttons
└── security/
    └── ApRealmConfig.java               # Realm bootstrap: AP_REVIEWER, FINANCE_DIRECTOR, RECEIVER roles
src/main/resources/db/migration/                # platform schema (holon-saas): tenant tables, login account
├── V001__create_tenant_tables.sql
src/main/resources/db/tenant-migration/         # every company's schema, V002 and up (V1 is the framework's)
├── V002__create_bill_table.sql
└── V003__create_bill_line_table.sql
# A Holon Auth scaffold (V0NN__auth_schema.sql) is only for a project that does NOT use holon-saas
```

---

## Project structure (skills write here)

```
docs/
├── requirements.md
├── entity_model.md
├── business_rules.md         ← GR-NNN shared rules (/business-rules)
├── use_cases/UC-NNN-*.md
├── test_cases/TC-NNN-*.md
├── architecture/             ← 4+1 views (/architecture-views)
└── adr/ADR-NNN-*.md          ← decisions (/adr)

.claude/
├── settings.json             ← wires the hooks (/session-guards)
└── hooks/                    ← bash guards + smoke.sh

src/
├── main/
│   ├── java/com/example/
│   │   ├── domain/          ← JavaBeans + BeanPropertySet constants
│   │   ├── service/         ← Datastore-backed services (Context-wired)
│   │   ├── ui/              ← Holon Vaadin Flow views (@Route)
│   │   └── security/        ← Realm / auth bootstrap
│   └── resources/
│       └── db/migration/    ← Flyway V*.sql
└── test/
    ├── java/com/example/
    │   ├── traceability/    ← @UseCase, @TestCase, SpecDocuments + 3 sensors
    │   ├── architecture/    ← ArchitectureTest + convention sensors, @Fallback
    │   ├── datastore/       ← JUnit 5 + Testcontainers integration tests
    │   ├── ui/              ← Vaadin Browserless tests (UC<NNN>…Test)
    │   └── e2e/             ← Playwright tests (TC<NNN>…IT)
    └── resources/
        └── db/migration/    ← test-only Flyway seed migrations
```
