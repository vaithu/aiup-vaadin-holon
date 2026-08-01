# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Overview

`aiup-vaadin-holon` is a **Claude Code / GitHub Copilot plugin marketplace** that automates the
AI Unified Process (AIUP) for projects using the **Holon Platform + Vaadin Flow** stack.

This repository **IS the plugin marketplace**. It contains no application source code in `src/`.
All illustrative code lives inside `SKILL.md` / `references/` files as fenced snippets.

## Repository Structure

```
.
├── .claude-plugin/marketplace.json          # marketplace manifest
├── aiup-core/                               # stack-agnostic core (copied verbatim from upstream)
│   ├── .claude-plugin/plugin.json
│   ├── .mcp.json                            # context7 only
│   └── skills/                              # requirements, entity-model, use-case-diagram, use-case-spec, test-case, reverse-engineer
└── aiup-vaadin-holon/                       # Holon Platform + Vaadin Flow construction plugin
    ├── .claude-plugin/plugin.json
    ├── .mcp.json                            # context7, Vaadin, JavaDocs, Playwright (no jOOQ/Karibu)
    ├── rules/
    │   ├── holon-stack.md                   # dependency allow/ban list + idioms
    │   └── mcp-servers.md
    └── skills/
        ├── flyway-migration/
        ├── implement/                       # /implement UC-XXX
        ├── implement-from-html/             # /implement-from-html <file>
        ├── datastore-test/
        ├── holon-vaadin-test/
        └── playwright-test/
```

## Plugin Architecture

### Two-layer design

- **aiup-core** — stack-agnostic methodology: from vision to use case specification.
- **aiup-vaadin-holon** — stack-specific construction plugin for Holon Platform + Vaadin Flow.

### Marketplace configuration

`marketplace.json` defines two plugins: `aiup-core` and `aiup-vaadin-holon`.

## AI Unified Process Workflow

### Core (stack-agnostic)

| Phase        | Skill              | Description                                      |
|--------------|--------------------|--------------------------------------------------|
| Inception    | `/requirements`    | Generate requirements from `docs/vision.md`      |
| Elaboration  | `/entity-model`    | Create entity model with Mermaid ER diagram      |
| Elaboration  | `/use-case-diagram`| Generate PlantUML use case diagrams              |
| Construction | `/use-case-spec`   | Write detailed use case specifications           |
| Construction | `/test-case`       | Write an end-to-end test case (TC-*) chaining several use cases |
| Any          | `/reverse-engineer`| Recover use case diagram + specs + entity model from existing code |

### Holon / Vaadin (stack-specific)

| Phase        | Skill                    | Description                                                      |
|--------------|--------------------------|------------------------------------------------------------------|
| Construction | `/flyway-migration`      | Create Flyway V*.sql migrations from `docs/entity_model.md`      |
| Construction | `/implement UC-XXX`      | Implement a use case: JavaBean, BeanPropertySet, Datastore service, Holon Vaadin view, Holon Auth guards |
| Construction | `/implement-from-html`   | Infer entities, roles, Holon Vaadin components from an HTML mockup |
| Construction | `/datastore-test UC-XXX` | JUnit 5 + Testcontainers Postgres + Flyway + Holon Datastore integration tests |
| Construction | `/holon-vaadin-test UC-XXX` | Server-side Vaadin Browserless unit tests (vaadin-testbench-unit-junit) |
| Construction | `/playwright-test UC-XXX`| Browser E2E tests via Playwright                                 |

## The Holon-only Rule (enforced in every construction SKILL.md)

- **Domain:** plain JavaBean with `@DataPath` / `@Identifier` — **never** `PropertyBox`
- **Property set:** `BeanPropertySet<T>` — never raw `PropertySet`
- **Persistence:** Holon `Datastore` JDBC (or JPA only when JDBC cannot express the query, justified inline)
- **UI:** `Components.input.*`, `PropertyListing`, `PropertyForm`, `form.setBean()` / `form.getBean()`
- **Security:** Holon Auth (`AuthContext`, `Realm`, `Authenticator`, `@Permitted`, `Permission`) — not Spring Security
- **DI:** prefer Holon `Context`; `@Autowired` is banned — inject via constructors
- **Spring stereotype:** `@SpringBootApplication` always permitted; `@Service` / `@Component` / `@Repository` allowed only when a class needs Spring lifecycle (`@Transactional`, `@EventListener`, `@Scheduled`)
- **Fallback policy:** raw Vaadin or Spring allowed only when Holon has no equivalent, justified inline with `// FALLBACK: no Holon equivalent for <thing>`

## Contribution Guidelines

- Do **not** add an `src/` directory — this repo is the plugin, not an application.
- When adding a new skill, mirror the YAML frontmatter format: `name` + `description` for auto-triggering.
- Every construction `SKILL.md` must contain a **Constraints** section listing the allow/ban list.
- Bump `aiup-vaadin-holon/.claude-plugin/plugin.json` version when making skill changes.

## Verification Checklist

Run these checks after any structural change:

```sh
# 1. PropertyBox must appear only in the ban list inside holon-stack.md
git grep -n 'PropertyBox' aiup-vaadin-holon/ | grep -v 'holon-stack.md'
# → should return NO hits

# 2. Every construction SKILL.md has a Constraints section
grep -rL 'Constraints' aiup-vaadin-holon/skills/

# 3. marketplace.json is valid JSON
python3 -c "import json; json.load(open('.claude-plugin/marketplace.json'))"

# 4. BeanPropertySet in bean-model.md
grep -l 'BeanPropertySet' aiup-vaadin-holon/skills/implement/references/bean-model.md

# 5. AuthContext in security-patterns.md
grep -l 'AuthContext' aiup-vaadin-holon/skills/implement/references/security-patterns.md
```
