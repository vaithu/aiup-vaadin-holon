# CLAUDE.md

This file provides guidance for Claude Code (claude.ai/code) when working with this repository.

## Overview

`aiup-vaadin-holon` is a **Claude Code / GitHub Copilot plugin marketplace** that automates the
AI Unified Process (AIUP) for projects using the **Holon Platform + Vaadin Flow** stack.

This repository **IS the plugin marketplace**. It contains no application source code in `src/`.
All illustrative code lives inside `SKILL.md` / `references/` files as fenced snippets.

## Repository Structure

```
.
├── .claude-plugin/marketplace.json          # marketplace manifest
├── docs/                                    # methodology/process documentation
│   └── AIUP-UI-UX-WORKFLOW.md              # first-class UI/UX workflow
├── aiup-core/                               # stack-agnostic core
│   ├── .claude-plugin/plugin.json
│   ├── .mcp.json
│   └── skills/
└── aiup-vaadin-holon/                       # Holon Platform + Vaadin Flow construction plugin
    ├── .claude-plugin/plugin.json
    ├── .mcp.json
    ├── rules/
    └── skills/
        ├── ui-specification/                # first-class UI/UX model
        ├── flyway-migration/
        ├── implement/
        ├── implement-from-html/
        ├── ai-assistant/
        ├── datastore-test/
        ├── holon-vaadin-test/
        └── playwright-test/
```

## Plugin Architecture

### Two-layer design

- **aiup-core** — stack-agnostic methodology: from vision to use case specification.
- **aiup-vaadin-holon** — stack-specific construction plugin for Holon Platform + Vaadin Flow,
  including the first-class UI/UX specification workflow.

### UI/UX is a first-class AIUP artifact

For Vaadin applications, UI/UX is elaborated in parallel with the entity model after
requirements. It defines the design system, application shell, screen specifications,
interaction/state rules, responsive behavior, accessibility expectations, and approved
HTML/CSS visual contracts.

The UI model does **not** replace requirements, the entity model, or use cases. Business
behavior remains authoritative in requirements/use-case artifacts.

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
| Elaboration  | `/ui-specification`      | Define design system, app shell, screen specs, states, responsive behavior, accessibility, and HTML/CSS mockups |
| Construction | `/flyway-migration`      | Create Flyway V*.sql migrations from `docs/entity_model.md`      |
| Construction | `/implement UC-XXX`      | Implement a use case: JavaBean, BeanPropertySet, Datastore service, Holon Vaadin view, Holon Auth guards |
| Construction | `/implement-from-html`   | Implement an approved HTML mockup as Holon/Vaadin; UI specs and use cases remain authoritative |
| Construction | `/ai-assistant UC-XXX`   | Add an AI-powered chat assistant backed by Holon Datastore |
| Construction | `/datastore-test UC-XXX` | JUnit 5 + Testcontainers Postgres + Flyway + Holon Datastore integration tests |
| Construction | `/holon-vaadin-test UC-XXX` | Server-side Vaadin Browserless unit tests |
| Construction | `/playwright-test UC-XXX`| Browser E2E tests via Playwright |

### Recommended feature flow

```text
Vision
  ↓
Requirements
  ├──────────────→ Entity Model
  └──────────────→ UX/UI Model
                       ├── Design System
                       ├── App Shell
                       ├── Screen Specifications
                       ├── Interaction & State Rules
                       └── HTML/CSS Mockups
                              ↓
                         Use Cases
                              ↓
                       Use Case Specs
                              ↓
                 Flyway + Implementation
                              ↓
                            Tests
```

### UI traceability rule

Every important interactive UI element should trace to a requirement or use-case behavior.
For example:

```text
FR-005 → UC-001 → UI-004 → pos.html → Vaadin POS View → E2E test
```

A screenshot is a visual reference. The UI specification is the durable contract for
layout, interaction, states, and responsive behavior.

## The Holon-only Rule

- **Domain:** plain JavaBean with `@DataPath` / `@Identifier` — **never** `PropertyBox`
- **Property set:** `BeanPropertySet<T>` — never raw `PropertySet`
- **Persistence:** Holon `Datastore` JDBC (or JPA only when JDBC cannot express the query, justified inline)
- **UI:** prefer Holon Vaadin Flow components and patterns defined by the construction skills
- **Security:** Holon Auth (`AuthContext`, `Realm`, `Authenticator`, `@Authenticate`, `@RolesAllowed`, `Permission`) — not Spring Security
- **DI:** prefer Holon `Context`; `@Autowired` is banned — inject via constructors
- **Spring stereotype:** `@SpringBootApplication` always permitted; `@Service` / `@Component` / `@Repository` allowed only when a class needs Spring lifecycle (`@Transactional`, `@EventListener`, `@Scheduled`)
- **Fallback policy:** raw Vaadin or Spring allowed only when Holon has no equivalent, justified inline with `// FALLBACK: no Holon equivalent for <thing>`

## Contribution Guidelines

- Do **not** add an `src/` directory — this repo is the plugin, not an application.
- When adding a new skill, mirror the YAML frontmatter format: `name` + `description` for auto-triggering.
- Every construction `SKILL.md` must contain a **Constraints** section listing the allow/ban list.
- UI specification skills must keep requirements/use-case traceability explicit.
- Bump `aiup-vaadin-holon/.claude-plugin/plugin.json` version when making skill changes.

## Verification Checklist

Run these checks after any structural change:

```sh
# 1. PropertyBox must appear only in the ban list inside holon-stack.md
git grep -n 'PropertyBox' aiup-vaadin-holon/ | grep -v 'holon-stack.md'

# 2. Every construction SKILL.md has a Constraints section
grep -rL 'Constraints' aiup-vaadin-holon/skills/

# 3. marketplace.json is valid JSON
python3 -c "import json; json.load(open('.claude-plugin/marketplace.json'))"

# 4. BeanPropertySet in bean-model.md
grep -l 'BeanPropertySet' aiup-vaadin-holon/skills/implement/references/bean-model.md

# 5. AuthContext in security-patterns.md
grep -l 'AuthContext' aiup-vaadin-holon/skills/implement/references/security-patterns.md

# 6. The commercial Vaadin AI extensions must never be referenced as an allowed dependency
git grep -n 'vaadin-ai-extensions-flow' aiup-vaadin-holon/ | grep -viE 'BANNED|banned|commercial|MUST NOT|not be used|never'

# 7. Every construction SKILL.md (including ai-assistant) has a Constraints section
grep -rL 'Constraints' aiup-vaadin-holon/skills/*/SKILL.md

# 8. UI specification skill is present
test -f aiup-vaadin-holon/skills/ui-specification/SKILL.md
```
