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

```
Inception          Elaboration                              Construction
─────────────────  ──────────────────────────────────────   ───────────────────────────────────────────────
/requirements  →  /entity-model  →  /use-case-diagram  →  /use-case-spec  →  /flyway-migration
                                                                           ↘  /implement UC-XXX
                                                                           ↘  /implement-from-html <file>
                                                                           ↘  /datastore-test UC-XXX
                                                                           ↘  /holon-vaadin-test UC-XXX
                                                                           ↘  /playwright-test UC-XXX
```

|                       | Inception       | Elaboration                            | Construction                                                                                                                                      |
|-----------------------|-----------------|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **aiup-core**         | `/requirements` | `/entity-model`<br>`/use-case-diagram` | `/use-case-spec`                                                                                                                                  |
| **aiup-vaadin-holon** |                 |                                        | `/flyway-migration`<br>`/implement`<br>`/implement-from-html`<br>`/datastore-test`<br>`/holon-vaadin-test`<br>`/playwright-test` |

---

## LTS Version Pins

| Component | Version | Source of truth |
|-----------|---------|-----------------|
| Java | **25** | — |
| Holon Core (Bean, BeanPropertySet, Datastore, Context, Auth) | **`com.holon-platform.core:10.0.0`** | https://github.com/vaithu/holon-vaadin-flow |
| Holon JDBC Datastore | **`com.holon-platform.jdbc:10.0.0`** | https://github.com/vaithu/holon-vaadin-flow |
| Holon JPA Datastore (optional) | **`com.holon-platform.jpa:10.0.0`** | https://github.com/vaithu/holon-vaadin-flow |
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
   Everything else is wired via Holon `Context` and Holon starters. `@Service`, `@Component`, and
   `@Repository` are allowed **only when a class needs Spring lifecycle** (`@Transactional`,
   `@EventListener`, `@Scheduled`); Holon `Context` remains preferred. `@Autowired` is **banned** —
   inject dependencies through constructors.
4. **`Bean` + `BeanPropertySet`, never `PropertyBox`** — domain objects are plain JavaBeans with
   `@DataPath` / `@Identifier` annotations; property sets are `BeanPropertySet<T>`.
5. **Holon Auth, never Spring Security** — `AuthContext`, `Realm`, `Authenticator`, `@Permitted`;
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
- Maven or Gradle with the Holon per-module BOMs imported (`holon-vaadin-flow-bom:10.0.1`, `holon-datastore-jdbc-bom:10.0.0`)
- Vaadin 25 on the classpath (`holon-vaadin-flow-spring-boot` starter pulls it in)
- Spring Boot 4.1.0 (bootstrap runtime)
- Flyway (managed by Spring Boot parent), PostgreSQL 16+
- `docs/vision.md` at the project root describing product vision and target users

---

## License

Apache-2.0. Derived from [AI-Unified-Process/marketplace](https://github.com/AI-Unified-Process/marketplace).
See [NOTICE](NOTICE) for full attribution.
