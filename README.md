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

This marketplace adds the part that makes those two sentences enforceable rather
than aspirational: a **harness** of JUnit sensors that read `docs/` from disk and
fail the build when the specifications and the code disagree, session hooks that
refuse to end a turn without a green sensor run, and a CI backstop that applies
the same checks to everyone who is not using an AI agent at all.

## Skill Execution Sequence

Skills must be run in the order shown below. Each skill reads artifacts produced by earlier
skills. **Every skill now includes a `## Prerequisites` guard clause** — if the required
artifacts are missing the skill will stop immediately and tell you exactly which skill to run
first. Do not skip steps.

```
/requirements ──→ /entity-model ──→ /use-case-diagram ──→ /use-case-spec UC-XXX
   Inception          Elaboration         Elaboration              │   Construction
                        │                                          │
                        │                                          ├──→ /business-rules
                        ├──→ /architecture-views                   └──→ /test-case UC-XXX …
                        │
                        └──→ /flyway-migration ──→ /implement UC-XXX ──→ /datastore-test UC-XXX
                                                                     ──→ /holon-vaadin-test UC-XXX
                                                                     ──→ /playwright-test TC-XXX
                                                                                  │
                        ── the harness ───────────────────────────────────────────┤
                                                                                  ▼
                                              /traceability-sensors ──→ /architecture-rules
                                                                                  │
                                                                                  ▼
                                              /coverage-check UC-XXX ──→ /session-guards

/adr "<decision>"  and  /reverse-engineer  run at any point.
```

The last stage is the **harness** — the part that keeps the specifications and
the code from drifting apart once both exist. See [The harness](#the-harness).

**Step-by-step for a standard Vaadin + Spring Boot + Holon feature:**

| # | Skill | Reads | Writes | Required before |
|---|-------|-------|--------|----------------|
| 1 | `/requirements` | `docs/vision.md` | `docs/requirements.md` | everything |
| 2 | `/entity-model` | `docs/requirements.md` | `docs/entity_model.md` | steps 3–8 |
| 3 | `/use-case-diagram` | `docs/requirements.md` | `docs/use_cases.puml` | step 4 |
| 4 | `/use-case-spec UC-XXX` | `docs/requirements.md`, `docs/use_cases.puml` | `docs/use_cases/UC-XXX-*.md` | steps 5–8 |
| 4b | `/business-rules` | `docs/use_cases/*.md` | `docs/business_rules.md` (`GR-NNN`) | — |
| 4c | `/architecture-views` | requirements, entity model, use cases | `docs/architecture/*.md` (4+1 views) | — |
| 4d | `/adr "<decision>"` | the decision at hand | `docs/adr/ADR-NNN-*.md` | — |
| 5 | `/flyway-migration` | `docs/entity_model.md` | `src/main/resources/db/migration/V*.sql` | steps 6–8 |
| 6 | `/implement UC-XXX` | use case spec, entity model, Flyway migrations | Java beans, service, Vaadin view, auth guards | steps 7–8 |
| 7 | `/test-case UC-XXX …` | `docs/use_cases/UC-XXX-*.md` | `docs/test_cases/TC-XXX-*.md` | step 8 (Playwright only) |
| 8a | `/datastore-test UC-XXX` | use case spec, service implementation, Flyway migrations | JUnit 5 + Testcontainers tests | — |
| 8b | `/holon-vaadin-test UC-XXX` | use case spec, view implementation, Flyway migrations | Vaadin Browserless tests | — |
| 8c | `/playwright-test TC-XXX` | use case spec, test case doc, view implementation | Playwright E2E tests | — |
| 9a | `/traceability-sensors` | use cases, test cases, business rules | `@UseCase`/`@TestCase` + three JUnit sensors | step 10 |
| 9b | `/architecture-rules` | `rules/holon-stack.md` | `ArchitectureTest` + convention sensors | step 10 |
| 9c | `/coverage-check UC-XXX` | use case spec + its tests | an audit verdict, before `Status:` claims `Done` | — |
| 10 | `/session-guards` | the installed sensor classes | `.claude/hooks/` + `.claude/settings.json` | — |

> **Why schema-first?** The plugin targets Holon Datastore (not JPA), which means
> `@DataPath` field names on JavaBeans must match the actual column names in the database.
> Flyway migrations must be created from the entity model *before* the Java domain classes are
> written, so field-to-column mapping is unambiguous. JPA `ddl-auto` style code-first generation
> is not applicable here — `jakarta.persistence.*` is banned in this stack.

|                       | Inception       | Elaboration                            | Construction                                                                                                                                                                                                                            |
|-----------------------|-----------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **aiup-core**         | `/requirements` | `/entity-model`<br>`/use-case-diagram`<br>`/architecture-views`<br>`/adr` | `/use-case-spec`<br>`/business-rules`<br>`/test-case`                                                                                                                                                   |
| **aiup-vaadin-holon** |                 |                                        | `/flyway-migration`<br>`/implement`<br>`/implement-from-html`<br>`/jpa-to-holon-domain`<br>`/jpa-to-holon-views`<br>`/ai-assistant`<br>`/datastore-test`<br>`/holon-vaadin-test`<br>`/playwright-test`<br>`/traceability-sensors`<br>`/architecture-rules`<br>`/coverage-check`<br>`/session-guards` |

---

## The harness

Specifications only help if the code is forced to keep agreeing with them. The
construction skills install four layers that do the forcing:

| Layer | Question | Installed by | Binds |
|-------|----------|--------------|-------|
| **Guides** | What should the agent do? | `CLAUDE.md`, `docs/`, `/architecture-views`, `/adr`, `rules/holon-stack.md` | whoever reads them |
| **Sensors** | Does the repo still agree with the guides? | `/traceability-sensors`, `/architecture-rules` | everyone, CI included |
| **Guards** | Did this session actually run the sensors? | `/session-guards` | one Claude Code session |
| **Backstop** | Does it hold for everybody else? | `mvn verify` in CI — workflows in `traceability-sensors/references/ci.md` | every push |

The dividing line, documented in `aiup-vaadin-holon/rules/harness.md`:

> **If a rule can be checked by reading the repository, it is a test.
> Only a rule about what happened during a session is a hook.**

Two consequences worth knowing before you run the skills:

- **`**Status:** Done` is an executable assertion**, not a label. `Done` and
  `Tested` on a use case, and `Automated` on a test case, switch a coverage
  sensor on: every flow must now have a test annotated with its name. `Draft`,
  `Reviewed`, `Approved` and `Implemented` claim nothing. Referential integrity
  is checked for *every* spec; coverage only for the ones claiming completion —
  because writing the spec first makes an unimplemented use case a normal
  intermediate state.
- **Test names carry traceability.** `UC<NNN><Name>Test` runs under Surefire,
  `UC<NNN><Name>IT` and `TC<NNN><Name>IT` under Failsafe. The sensors key on it,
  so `/holon-vaadin-test` and `/playwright-test` emit these names.

Run `mvn -q test -Dgroups=sensor` any time — the sensors need no Docker and take
seconds, which is the only property that makes them worth having.

---

## Skill Reference

### aiup-core skills

| Skill | Phase | Invocation | What it produces |
|-------|-------|-----------|-----------------|
| `/requirements` | Inception | `/requirements` | Requirements catalog from `docs/vision.md` |
| `/entity-model` | Elaboration | `/entity-model` | Mermaid ER diagram in `docs/entity_model.md` |
| `/use-case-diagram` | Elaboration | `/use-case-diagram` | PlantUML use case diagram in `docs/use_case_diagram.puml` |
| `/use-case-spec` | Construction | `/use-case-spec UC-XXX` | Detailed use case specification in `docs/use_cases/UC-XXX-*.md` |
| `/business-rules` | Construction | `/business-rules` | Catalogue of cross-cutting rules (`GR-NNN`) in `docs/business_rules.md`, extracted from the per-use-case `BR-NNN` rules |
| `/architecture-views` | Elaboration | `/architecture-views` | The 4+1 architecture views in `docs/architecture/` — logical, process, development, physical and scenarios, plus an index |
| `/adr` | Any | `/adr "<decision>"` | An immutable architecture decision record in `docs/adr/ADR-NNN-*.md` |
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
| `/traceability-sensors` | `/traceability-sensors` | Installs the `@UseCase` / `@TestCase` annotations and three JUnit sensors that parse `docs/` from disk: every annotation must name a real use case, flow or rule, and every spec claiming `Done` must have a test for each flow |
| `/architecture-rules` | `/architecture-rules` | Turns `rules/holon-stack.md` into executable ArchUnit rules — banned imports, constructor injection, layer boundaries, test naming — plus source-level checks for what bytecode cannot see. `@Fallback` documents a justified exception |
| `/coverage-check` | `/coverage-check UC-XXX` | Runs the read-only `uc-coverage` auditor over a use case or test case and its tests, classifying each flow Covered / Asserted weakly / Mislabelled / Absent. Run it before raising `Status:` to `Done` |
| `/session-guards` | `/session-guards` | Installs `.claude/hooks/` and `.claude/settings.json`: a turn cannot end while code changed after the last green sensor run, and `Status:` cannot claim coverage without a fresh audit. Requires Git Bash + `jq` |

---

## Stack version pins

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

Once the harness skills have been run, verify it is actually wired:

```sh
mvn -q test -Dgroups=sensor     # seconds, no Docker — must pass
./.claude/hooks/smoke.sh        # exercises the guards; expect EXIT=0
```

A green `smoke.sh` is the only evidence the hooks are live — `settings.json`
being present proves nothing, because a hook that cannot read its input exits
quietly by design.

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
│       ├── business-rules/
│       ├── architecture-views/
│       ├── adr/
│       ├── test-case/
│       └── reverse-engineer/
└── aiup-vaadin-holon/                      # Holon Platform + Vaadin Flow construction plugin
    ├── .claude-plugin/plugin.json
    ├── .mcp.json
    ├── README.md
    ├── agents/
    │   └── uc-coverage.md                  # read-only coverage auditor, drives /coverage-check
    ├── rules/
    │   ├── holon-stack.md                  # dependency allow/ban list + preferred idioms
    │   ├── harness.md                      # the four layers + where a new rule belongs
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
        ├── playwright-test/SKILL.md
        ├── traceability-sensors/
        │   ├── SKILL.md
        │   └── references/
        │       ├── sensors.md               # @UseCase, @TestCase, SpecDocuments + 3 sensors
        │       ├── build-wiring.md          # Surefire/Failsafe + the `sensor` tag
        │       └── ci.md                    # java-quality.yml + codeql.yml — the backstop
        ├── architecture-rules/
        │   ├── SKILL.md
        │   └── references/
        │       ├── rules.md                 # ArchitectureTest + convention sensors
        │       └── fallbacks.md             # @Fallback: justifying an exception
        ├── coverage-check/SKILL.md
        └── session-guards/
            ├── SKILL.md
            └── references/
                ├── hooks.md                 # 7 bash hooks + .claude/settings.json
                └── smoke.md                 # smoke.sh — 55 checks over the hooks
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

For the harness skills specifically:

- `/traceability-sensors` and `/architecture-rules` — **ArchUnit** on the test
  classpath (the skill adds it) and **Failsafe** bound, so `*IT` classes run in
  `verify` rather than being silently skipped.
- `/session-guards` — **Git Bash** and **`jq`**. Git Bash ships with Git for
  Windows; `jq` does **not**, so install it separately and put it on `PATH`.
  The hooks run `git status` on every turn, so if a corporate virus scanner
  makes that slow, enable `core.fsmonitor` — the skill documents the
  `time git status --porcelain` diagnostic.

---

## License

Apache-2.0. Derived from [AI-Unified-Process/marketplace](https://github.com/AI-Unified-Process/marketplace).
See [NOTICE](NOTICE) for full attribution.
