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
│   └── skills/                              # requirements, entity-model, use-case-diagram, use-case-spec, business-rules, test-case, architecture-views, adr, reverse-engineer
└── aiup-vaadin-holon/                       # Holon Platform + Vaadin Flow construction plugin
    ├── .claude-plugin/plugin.json
    ├── .mcp.json                            # context7, Vaadin, JavaDocs, Playwright (no jOOQ/Karibu)
    ├── agents/
    │   └── uc-coverage.md                   # read-only coverage auditor (drives /coverage-check)
    ├── rules/
    │   ├── holon-stack.md                   # dependency allow/ban list + idioms
    │   ├── harness.md                       # the four layers + where a new rule belongs
    │   └── mcp-servers.md
    └── skills/
        ├── flyway-migration/
        ├── implement/                       # /implement UC-XXX
        ├── implement-from-html/             # /implement-from-html <file>
        ├── ai-assistant/              # /ai-assistant UC-XXX (free vaadin-ai-core-flow + Holon Datastore)
        ├── traceability-sensors/             # spec ↔ code link, enforced by JUnit
        ├── architecture-rules/               # holon-stack.md enforced by ArchUnit
        ├── coverage-check/                   # audit a UC/TC before its Status claims Done
        ├── session-guards/                   # .claude/hooks — evidence before a turn ends
        ├── datastore-test/
        ├── holon-vaadin-test/
        └── playwright-test/
```

## Plugin Architecture

### Two-layer design

- **aiup-core** — stack-agnostic methodology: from vision to use case specification.
- **aiup-vaadin-holon** — stack-specific construction plugin for Holon Platform + Vaadin Flow.

### The harness

The construction skills form a four-layer harness — **Guides** (prose) →
**Sensors** (JUnit/ArchUnit) → **Guards** (`.claude/hooks`) → **Backstop**
(CI). The dividing line, and where a new rule belongs, is in
`aiup-vaadin-holon/rules/harness.md`:

> If a rule can be checked by reading the repository, it is a test.
> Only a rule about what happened during a session is a hook.

`**Status:** Done` is an executable assertion that switches a coverage sensor
on, not a label. Referential integrity is checked for *every* spec; coverage
only for specs whose status claims completion.

### Marketplace configuration

`marketplace.json` defines two plugins: `aiup-core` and `aiup-vaadin-holon`.

## AI Unified Process Workflow

### Core (stack-agnostic)

| Phase        | Skill              | Description                                      |
|--------------|--------------------|--------------------------------------------------|
| Inception    | `/requirements`    | Generate requirements from `docs/vision.md`      |
| Elaboration  | `/entity-model`    | Create entity model with Mermaid ER diagram      |
| Elaboration  | `/use-case-diagram`| Generate PlantUML use case diagrams              |
| Elaboration  | `/architecture-views` | Write the 4+1 views in `docs/architecture/` + the index |
| Construction | `/use-case-spec`   | Write detailed use case specifications           |
| Construction | `/business-rules`  | Maintain the shared `GR-NNN` rule catalogue (`docs/business_rules.md`) |
| Construction | `/test-case`       | Write an end-to-end test case (TC-*) chaining several use cases |
| Any          | `/adr`             | Record an architecture decision as `ADR-NNN-*.md` |
| Any          | `/reverse-engineer`| Recover use case diagram + specs + entity model from existing code |

### Holon / Vaadin (stack-specific)

| Phase        | Skill                    | Description                                                      |
|--------------|--------------------------|------------------------------------------------------------------|
| Construction | `/flyway-migration`      | Create Flyway V*.sql migrations from `docs/entity_model.md`      |
| Construction | `/implement UC-XXX`      | Implement a use case: JavaBean, BeanPropertySet, Datastore service, Holon Vaadin view, Holon Auth guards |
| Construction | `/implement-from-html`   | Infer entities, roles, Holon Vaadin components from an HTML mockup |
| Construction | `/ai-assistant UC-XXX`   | Add an AI-powered chat assistant (free `vaadin-ai-core-flow`: `AIOrchestrator`, `MessageList`/`MessageInput`, `SpringAILLMProvider`) backed by a Holon `Datastore` custom `AIController`/`DatabaseProvider`; no commercial AI controllers |
| Construction | `/datastore-test UC-XXX` | JUnit 5 + Testcontainers Postgres + Flyway + Holon Datastore integration tests |
| Construction | `/holon-vaadin-test UC-XXX` | Server-side Vaadin Browserless unit tests (vaadin-testbench-unit-junit) |
| Construction | `/playwright-test UC-XXX`| Browser E2E tests via Playwright                                 |
| Construction | `/traceability-sensors`  | Install the JUnit sensors that fail the build when `docs/` and the tests disagree |
| Construction | `/architecture-rules`    | Turn `holon-stack.md` into ArchUnit rules (banned imports, injection, layers, test naming) |
| Transition   | `/coverage-check UC-XXX` | Read-only audit of whether a UC/TC is really covered, before its `Status:` claims `Done` |
| Transition   | `/session-guards`        | Install `.claude/hooks` — no turn ends, and no `Status:` is raised, without evidence |

## The Holon-only Rule (enforced in every construction SKILL.md)

- **Domain:** plain JavaBean with `@DataPath` / `@Identifier` — **never** `PropertyBox`
- **Property set:** `BeanPropertySet<T>` — never raw `PropertySet`
- **Persistence:** Holon `Datastore` JDBC (or JPA only when JDBC cannot express the query, justified inline)
- **UI:** `Components.input.*`, `PropertyListing`, `PropertyForm`, `form.setBean()` / `form.getBean()`
- **Security:** Holon Auth (`AuthContext`, `Realm`, `Authenticator`, `@Authenticate`, `@RolesAllowed`, `Permission`) — not Spring Security
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
# 1. PropertyBox must never be presented as the domain model. It legitimately
#    appears in three kinds of place: the ban lists, the ArchUnit rule that
#    enforces the ban, and the few Datastore APIs that genuinely return one
#    (multi-table joins, PropertyInputGroup.getValue()). Enforcement in a
#    generated project is ArchitectureTest.noDynamicPropertyContainerOutsideFallback;
#    this check only looks for a skill telling the agent to model a domain with it.
git grep -n 'PropertyBox' aiup-vaadin-holon/ \
  | grep -iE 'domain model|instead of (a )?bean|as the (primary )?(data|domain)' \
  | grep -viE 'use (the )?.?Bean|never|banned|instead\.|— use'
# → should return NO hits

# 2. Every construction SKILL.md has a Constraints section
grep -rL 'Constraints' aiup-vaadin-holon/skills/

# 3. marketplace.json is valid JSON
python3 -c "import json; json.load(open('.claude-plugin/marketplace.json'))"

# 4. BeanPropertySet in bean-model.md
grep -l 'BeanPropertySet' aiup-vaadin-holon/skills/implement/references/bean-model.md

# 5. AuthContext in security-patterns.md
grep -l 'AuthContext' aiup-vaadin-holon/skills/implement/references/security-patterns.md

# 6. The commercial Vaadin AI extensions must never be referenced as an allowed dependency
#    (the ai-assistant skill uses the free vaadin-ai-core-flow only)
git grep -n 'vaadin-ai-extensions-flow' aiup-vaadin-holon/ | grep -viE 'BANNED|banned|commercial|MUST NOT|not be used|never|\bno\b'
# → should return NO hits

# 7. Every construction SKILL.md (including ai-assistant) has a Constraints section
grep -rL 'Constraints' aiup-vaadin-holon/skills/*/SKILL.md
# → should return NO hits

# 8. Every reference linked from a SKILL.md actually exists. Links may be
#    relative to a sibling skill (../other-skill/references/x.md), so resolve
#    the path rather than assuming it is local.
for f in aiup-vaadin-holon/skills/*/SKILL.md aiup-core/skills/*/SKILL.md; do
  grep -oE '(\.\./[A-Za-z0-9._-]+/)*references/[A-Za-z0-9._-]+\.md' "$f" | sort -u | while read -r r; do
    [ -f "$(cd "$(dirname "$f")" && cd "$(dirname "$r")" 2>/dev/null && pwd)/$(basename "$r")" ] \
      || echo "MISSING: $f -> $r"
  done
done
# → should return NO hits

# 9. The sensor class list in the hooks matches the classes the skills install
grep -o 'AIUP_SENSORS=.*' aiup-vaadin-holon/skills/session-guards/references/hooks.md
# → must name exactly: ArchitectureTest TestLayerConventionsTest SourceConventionsTest
#   UseCaseTraceabilityTest TestCaseTraceabilityTest BusinessRuleTraceabilityTest

# 10. No skill still emits the old, untraceable test names
git grep -nE 'class [A-Z][A-Za-z0-9]*E2ETest' aiup-vaadin-holon/skills/
# → should return NO hits: E2E tests are *IT, named TC<NNN>… or UC<NNN>…

# 11. The CI backstop is documented and wired. CI must run the sensors, and must
#     never skip a run because only docs/ changed — a renamed flow heading is the
#     most likely way to break traceability, so the only `paths-ignore` in the
#     reference is the one explicitly labelled as wrong.
CI=aiup-vaadin-holon/skills/traceability-sensors/references/ci.md
grep -q 'groups=sensor' "$CI" && echo "ok: CI runs the sensors"
[ "$(grep -c 'paths-ignore' "$CI")" = 1 ] && grep -q '# WRONG' "$CI" \
  && echo "ok: the only paths-ignore is the counter-example"

# 12. The asserting-status vocabulary agrees everywhere it is stated. The real
#     definition is CLAIMS_COMPLETE/CLAIMS_AUTOMATED in sensors.md and
#     aiup_asserting_status in hooks.md; the prose must not invent values.
#     (Values outside the templates' vocabularies are the usual drift.)
grep -n 'Set.of("Done", "Tested")' aiup-vaadin-holon/skills/traceability-sensors/references/sensors.md
grep -n 'Done | Tested | Automated' aiup-vaadin-holon/skills/session-guards/references/hooks.md
git grep -nE '`(Specified|In Progress)`' -- '*.md' \
  | grep -vE 'implement-from-html|implement-master-detail|demo-data'
# → the first two must match; the third must return NO hits
```
