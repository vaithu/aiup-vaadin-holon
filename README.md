# aiup-vaadin-holon

A **Claude Code / GitHub Copilot plugin marketplace** that automates the
[AI Unified Process (AIUP)](https://unifiedprocess.ai) for a **Holon Platform + Vaadin Flow** stack.
It ports the methodology from [AI-Unified-Process/marketplace](https://github.com/AI-Unified-Process/marketplace)
and replaces the construction plugins with a single `aiup-vaadin-holon` plugin that targets
**Holon Platform 10.0.x + Vaadin Flow 25** exclusively.

> **Acknowledgements** — The stack-agnostic `aiup-core` skills and the overall marketplace
> pattern were created by **Simon Martinelli** and **Marc Affolter** at
> [unifiedprocess.ai](https://unifiedprocess.ai) and are reproduced here under their original
> Apache-2.0 license. See [`NOTICE`](NOTICE) for full attribution.

---

## What is the AI Unified Process?

AIUP is a disciplined AI-assisted development methodology where every project starts with a
written vision, proceeds through requirements, an entity model, and use case specifications,
and **only then** enters implementation. Nothing gets built without a use case. Nothing reaches
production without tests traceable to requirements.

## Skill Execution Sequence

Skills must be run in the order shown below. Each skill reads artifacts produced by earlier
skills. **Every skill now includes a `## Prerequisites` guard clause** — if the required
artifacts are missing the skill will stop immediately and tell you exactly which skill to run
first. Do not skip steps.

```
Inception              Elaboration                                      Construction
─────────────────────  ──────────────────────────────────────────────   ────────────────────────────────────────────────────────────────────────
                                                                        ┌── /flyway-migration  (reads entity_model.md → writes V*.sql)
/requirements  ──→  /entity-model  ──→  /use-case-diagram  ──→  /use-case-spec  ──→  /implement UC-XXX  ──→  /datastore-test UC-XXX
                                                                        │                                 ──→  /holon-vaadin-test UC-XXX
                                                                        └── /test-case UC-XXX ...         ──→  /playwright-test TC-XXX
```

**Step-by-step for a standard Vaadin + Spring Boot + Holon feature:**

| # | Skill | Reads | Writes | Required before |
|---|-------|-------|--------|----------------|
| 1 | `/requirements` | `docs/vision.md` | `docs/requirements.md` | everything |
| 2 | `/entity-model` | `docs/requirements.md` | `docs/entity_model.md` | steps 3–8 |
| 3 | `/use-case-diagram` | `docs/requirements.md` | `docs/use_cases.puml` | step 4 |
| 4 | `/use-case-spec UC-XXX` | `docs/requirements.md`, `docs/use_cases.puml` | `docs/use_cases/UC-XXX-*.md` | steps 5–8 |
| 5 | `/flyway-migration` | `docs/entity_model.md` | `src/main/resources/db/migration/V*.sql` | steps 6–8 |
| 6 | `/implement UC-XXX` | use case spec, entity model, Flyway migrations | Java beans, service, Vaadin view, auth guards | steps 7–8 |
| 7 | `/test-case UC-XXX …` | `docs/use_cases/UC-XXX-*.md` | `docs/test_cases/TC-XXX-*.md` | step 8 (Playwright only) |
| 8a | `/datastore-test UC-XXX` | use case spec, service implementation, Flyway migrations | JUnit 5 + Testcontainers tests | — |
| 8b | `/holon-vaadin-test UC-XXX` | use case spec, view implementation, Flyway migrations | Vaadin Browserless tests | — |
| 8c | `/playwright-test TC-XXX` | use case spec, test case doc, view implementation | Playwright E2E tests | — |

> **Why schema-first?** The plugin targets Holon Datastore (not JPA), which means
> `@DataPath` field names on JavaBeans must match the actual column names in the database.
> Flyway migrations must be created from the entity model *before* the Java domain classes are
> written, so field-to-column mapping is unambiguous. JPA `ddl-auto` style code-first generation
> is not applicable here — `jakarta.persistence.*` is banned in this stack.

|                       | Inception       | Elaboration                            | Construction                                                                                                                                                                                                                            |
|-----------------------|-----------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **aiup-core**         | `/requirements` | `/entity-model`<br>`/use-case-diagram` | `/use-case-spec`<br>`/test-case`                                                                                                                                                                                                        |
| **aiup-vaadin-holon** |                 |                                        | `/flyway-migration`<br>`/implement`<br>`/implement-from-html`<br>`/jpa-to-holon-domain`<br>`/jpa-to-holon-views`<br>`/ai-assistant`<br>`/datastore-test`<br>`/holon-vaadin-test`<br>`/playwright-test` |

---

## Skill Reference

### aiup-core skills

| Skill | Phase | Invocation | What it produces |
|-------|-------|-----------|-----------------|
| `/requirements` | Inception | `/requirements` | Requirements catalog from `docs/vision.md` |
| `/entity-model` | Elaboration | `/entity-model` | Mermaid ER diagram in `docs/entity_model.md` |
| `/use-case-diagram` | Elaboration | `/use-case-diagram` | PlantUML use case diagram in `docs/use_case_diagram.puml` |
| `/use-case-spec` | Construction | `/use-case-spec UC-XXX` | Detailed use case specification in `docs/use_cases/UC-XXX-*.md` |
| `/test-case` | Construction | `/test-case UC-XXX UC-YYY` | End-to-end test case document (`docs/test_cases/TC-*.md`) chaining multiple use cases into one user journey; used as input by `/playwright-test` |
| `/reverse-engineer` | Any | `/reverse-engineer` | Recovers use case diagram, specs, and entity model from existing code |

### aiup-vaadin-holon skills

| Skill | Invocation | What it produces |
|-------|-----------|-----------------|
| `/flyway-migration` | `/flyway-migration` | Flyway `V*.sql` migration scripts from `docs/entity_model.md` |
| `/implement` | `/implement UC-XXX` | JavaBean, `BeanPropertySet` model, Holon Datastore service, Vaadin view, and Holon Auth guards for a use case |
| `/implement-from-html` | `/implement-from-html <file>` | Infers entities, roles, and Holon Vaadin components from an HTML mockup file |
| `/jpa-to-holon-domain` | `/jpa-to-holon-domain [EntityName]` | In a single pass: annotates the JPA entity with Holon meta-annotations and Jakarta Validation constraints, creates the `BeanPropertySet` companion `*Model` interface, generates I18N resource bundles, and produces a Spring Data JPA `Repository` (marker only) plus a Holon `BeanDatastoreHelper` `Service` with paginated reads and lazy streaming |
| `/jpa-to-holon-views` | `/jpa-to-holon-views [EntityName]` | Generates Vaadin views using the Two-View Pattern: a `ListingBundle` list view and a `MasterDetailLayout` detail view with responsive desktop/mobile behaviour |
| `/ai-assistant` | `/ai-assistant UC-XXX` | Adds an AI-powered chat assistant to a use case using the **free** `vaadin-ai-core-flow` module: a Vaadin chat view (`MessageList` + `MessageInput`, optional `Upload`), an `AIOrchestrator` bean, an `LLMProvider` (default `SpringAILLMProvider`, config-driven API key), and a Holon `Datastore`-backed custom `AIController` / `DatabaseProvider` exposing the use case's entities read-only. Commercial Grid/Chart/Form AI controllers are not used |
| `/datastore-test` | `/datastore-test UC-XXX` | JUnit 5 + Testcontainers + Flyway + Holon Datastore integration tests |
| `/holon-vaadin-test` | `/holon-vaadin-test UC-XXX` | Server-side Vaadin browserless unit tests (`vaadin-testbench-unit-junit`) |
| `/playwright-test` | `/playwright-test UC-XXX` or `/playwright-test TC-XXX` | Browser end-to-end tests via Playwright; can target a single use case or a full test case journey document |

---



| Component | Version | Source of truth |
|-----------|---------|-----------------|
| Java | **25** | — |
| Holon Core (Bean, BeanPropertySet, Datastore, Context, Auth) | **`com.holon-platform.core:10.0.0`** | https://github.com/vaithu/holon-vaadin-flow |
| Holon Datastore | **`com.holon-platform.jdbc:10.0.0`** | https://github.com/vaithu/holon-vaadin-flow |
| Spring JPA (fallback) | via Spring Boot 4.1.0 | — |
| Holon Vaadin Flow | **`com.holon-platform.vaadin:10.0.1`** | https://github.com/vaithu/holon-vaadin-flow |
| Vaadin Flow | **25.2.1** | — |
| Spring Boot | **4.1.0** | — |
| Flyway | (managed by Spring Boot parent) | — |
| PostgreSQL | **16+** | — |
| JUnit | **5** | — |
| Testcontainers | latest stable | — |
| Playwright | latest stable | — |

---

## The Holon-only Rule

This plugin enforces a strict hierarchy:

1. **Always prefer a Holon API** — `com.holon-platform.*` is the first choice for every concern.
2. **Raw Vaadin as a fallback** — `com.vaadin.*` components are allowed **only** when Holon Vaadin
   Flow has no equivalent. Every such use MUST be justified inline:
   ```java
   // FALLBACK: no Holon equivalent for <thing>
   ```
3. **Spring as bootstrap first** — `@SpringBootApplication` on the main class is always permitted.
   Holon Spring Boot starters **register** the `Datastore`, Auth, and related beans into the Spring
   application context; application code is then **wired via Spring constructor injection**.
   `@Service`, `@Component`, and `@Repository` are allowed **only when a class needs Spring
   lifecycle** (`@Transactional`, `@EventListener`, `@Scheduled`). `@Autowired` is **banned** — inject
   dependencies through constructors — and `Context.get()` is reserved for framework internals, not
   application code.
4. **`Bean` + `BeanPropertySet`, never `PropertyBox`** — domain objects are plain JavaBeans with
   `@DataPath` / `@Identifier` annotations; property sets are `BeanPropertySet<T>`.
5. **Holon Auth, never Spring Security** — `AuthContext`, `Realm`, `Authenticator`, `@Authenticate`, `@RolesAllowed`;
   Spring Security may appear only in filter-chain wiring where Holon Auth requires it.

See [`aiup-vaadin-holon/rules/holon-stack.md`](aiup-vaadin-holon/rules/holon-stack.md) for the
complete allow-list, ban-list, and preferred idioms.

---

## Installation

### GitHub Copilot (VS Code)

Add to `.vscode/mcp.json` in your project:

```jsonc
{
  "inputs": [],
  "servers": {
    "aiup-vaadin-holon": {
      "type": "http",
      "url": "https://github.com/vaithu/aiup-vaadin-holon"
    }
  }
}
```

Then open the Copilot Chat panel and run:

```
/plugin marketplace add vaithu/aiup-vaadin-holon
/plugin install aiup-core
/plugin install aiup-vaadin-holon
```

### Claude Code

In your project directory:

```
/plugin marketplace add vaithu/aiup-vaadin-holon
/plugin install aiup-core
/plugin install aiup-vaadin-holon
```

### Verify installation

Open Claude Code (or Copilot Chat) in your project and run:

```
/requirements
```

If the agent begins reading `docs/vision.md` and proposing a requirements catalog, the skills
are installed correctly.

---

## Repository layout

```
.
├── .claude-plugin/marketplace.json         # marketplace manifest — lists aiup-core + aiup-vaadin-holon
├── README.md                               # this file
├── CLAUDE.md                               # guidance for Claude Code when working in this repo
├── LICENSE                                 # Apache-2.0
├── NOTICE                                  # attribution (Simon Martinelli / Marc Affolter / Holon)
├── aiup-core/                              # copied verbatim from AI-Unified-Process/marketplace
│   ├── .claude-plugin/plugin.json
│   ├── .mcp.json
│   └── skills/
│       ├── requirements/
│       ├── entity-model/
│       ├── use-case-diagram/
│       ├── use-case-spec/
│       ├── test-case/
│       └── reverse-engineer/
└── aiup-vaadin-holon/                      # Holon Platform + Vaadin Flow construction plugin
    ├── .claude-plugin/plugin.json
    ├── .mcp.json
    ├── README.md
    ├── rules/
    │   ├── holon-stack.md                  # dependency allow/ban list + preferred idioms
    │   └── mcp-servers.md
    └── skills/
        ├── flyway-migration/SKILL.md
        ├── implement/
        │   ├── SKILL.md
        │   └── references/
        │       ├── bean-model.md
        │       ├── datastore-patterns.md
        │       ├── holon-vaadin-ui.md
        │       ├── security-patterns.md
        │       └── context-wiring.md
        ├── implement-from-html/
        │   ├── SKILL.md
        │   └── references/
        │       ├── html-mapping.md
        │       ├── css-extraction.md
        │       ├── role-inference.md
        │       └── entity-inference.md
        ├── datastore-test/SKILL.md
        ├── holon-vaadin-test/SKILL.md
        ├── jpa-to-holon-domain/SKILL.md
        ├── jpa-to-holon-views/SKILL.md
        ├── ai-assistant/
        │   ├── SKILL.md
        │   └── references/
        │       ├── orchestrator-setup.md
        │       ├── llm-provider-config.md
        │       ├── holon-datastore-provider.md
        │       └── security-and-guardrails.md
        └── playwright-test/SKILL.md
```

---

## Demo module

A worked, minimal **CRM** application produced with this plugin lives in
[`demo/crm-minimal/`](demo/crm-minimal/). It shows the full AIUP flow — vision → requirements →
entity model → use case specs → Flyway migrations → Holon/Vaadin implementation — and its
[`README`](demo/crm-minimal/README.md) documents every step and command used to create it.

---

## Prerequisites (for projects using this plugin)

- Java 25
- Maven or Gradle with the Holon per-module BOMs imported (`holon-vaadin-flow-bom:10.0.1`; no separate Datastore BOM needed when using `holon-datastore-jpa-spring-boot`)
- Vaadin 25 on the classpath (`holon-vaadin-flow-spring-boot` starter pulls it in)
- Spring Boot 4.1.0 (bootstrap runtime)
- Flyway (managed by Spring Boot parent), PostgreSQL 16+
- `docs/vision.md` at the project root describing product vision and target users

---

## License

Apache-2.0. Derived from [AI-Unified-Process/marketplace](https://github.com/AI-Unified-Process/marketplace).
See [NOTICE](NOTICE) for full attribution.
