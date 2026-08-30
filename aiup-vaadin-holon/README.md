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
> applicable — `jakarta.persistence.*` is banned.

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
        <version>10.0.1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin-bom</artifactId>
        <version>25.2.1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  ```
  > **Note:** Holon 10.x is built from the fork at https://github.com/vaithu/holon-vaadin-flow.
  > Run `mvn install` on the fork before using these artifacts locally.
- **Vaadin 25.2** (pulled in via `holon-vaadin-flow-spring-boot` starter)
- **Spring Boot 4.1.x** (bootstrap runtime)
- **Flyway 10.x**, **PostgreSQL 16+**
- `docs/vision.md` at the project root

---

## Stack version pins

| Component | Version | Source of truth |
|-----------|---------|-----------------|
| Java | 25 | — |
| Holon Vaadin Flow BOM | `com.holon-platform.vaadin:holon-vaadin-flow-bom:10.0.1` | https://github.com/vaithu/holon-vaadin-flow |
| Holon Core | 10.0.0 | https://github.com/vaithu/holon-vaadin-flow |
| Holon Datastore | 10.0.0 | https://github.com/vaithu/holon-vaadin-flow |
| Spring JPA (fallback) | via Spring Boot 4.1.0 | — |
| Holon Vaadin Flow | 10.0.1 | https://github.com/vaithu/holon-vaadin-flow |
| Vaadin Flow | 25.2.1 | — |
| Spring Boot | 4.1.0 | — |
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
src/main/resources/db/migration/
├── V001__create_bill_tables.sql
└── V002__auth_schema.sql               # Holon Auth role/permission tables scaffold
```

---

## Project structure (skills write here)

```
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
    │   ├── datastore/       ← JUnit 5 + Testcontainers integration tests
    │   ├── ui/              ← Vaadin Browserless tests
    │   └── e2e/             ← Playwright tests
    └── resources/
        └── db/migration/    ← test-only Flyway seed migrations
```
