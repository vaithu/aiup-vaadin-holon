# aiup-core

The **stack-agnostic** half of the AI Unified Process: everything from a vision
statement to a specification precise enough to build from and to test against.

It writes only Markdown and PlantUML. It names no language, framework or
database — pair it with a construction plugin such as
[`aiup-vaadin-holon`](../aiup-vaadin-holon) to turn the specifications into
code.

---

## Skills

| Skill | Phase | Invocation | Writes |
|-------|-------|-----------|--------|
| Requirements | Inception | `/requirements` | `docs/requirements.md` — functional (`FR-NNN`), non-functional (`NFR-NNN`), constraints (`CON-NNN`) |
| Entity Model | Elaboration | `/entity-model` | `docs/entity_model.md` — Mermaid ER diagram + attribute tables |
| Use Case Diagram | Elaboration | `/use-case-diagram` | `docs/use_cases.puml` — actors and use cases in PlantUML |
| Architecture Views | Elaboration | `/architecture-views` | `docs/architecture/` — the 4+1 views (logical, process, development, physical, scenarios) plus an index |
| Use Case Spec | Construction | `/use-case-spec UC-XXX` | `docs/use_cases/UC-NNN-*.md` — flows, business rules (`BR-NNN`), a `Status:` line |
| Business Rules | Construction | `/business-rules` | `docs/business_rules.md` — the shared `GR-NNN` catalogue |
| Test Case | Construction | `/test-case UC-XXX UC-YYY` | `docs/test_cases/TC-NNN-*.md` — one journey chaining several use cases |
| ADR | Any | `/adr "<decision>"` | `docs/adr/ADR-NNN-*.md` — one immutable decision record |
| Reverse Engineer | Any | `/reverse-engineer` | All of the above, recovered from a codebase that already exists |

## Order

```
/requirements ──→ /entity-model ──→ /use-case-diagram ──→ /use-case-spec UC-XXX
                        │                                        │
                        └──→ /architecture-views                 ├──→ /business-rules
                                                                 └──→ /test-case UC-XXX …

/adr and /reverse-engineer run at any point.
```

Each skill opens with a `## Prerequisites` guard clause: if an input artifact is
missing it stops and names the skill to run first.

`/business-rules` runs *after* some use cases exist, because it is an
extraction: it finds the `BR-NNN` rules that several use cases state
independently and promotes them to a single `GR-NNN`.

## Two conventions worth knowing

These matter because the construction plugins read these documents **by
machine**, not only by eye.

### `Status:` is an assertion, not a label

Every `UC-NNN` and `TC-NNN` carries a `**Status:**` line. Most values are
descriptive, but a few make a claim the sensors will check:

| Document | Vocabulary | Asserting values | The claim |
|---|---|---|---|
| `UC-NNN` | Draft, Reviewed, Approved, Implemented, Tested, Done, Obsolete | **`Tested`, `Done`** | every flow — main, alternative, exception — has a test |
| `TC-NNN` | Draft, Reviewed, Approved, Automated, Obsolete | **`Automated`** | the whole Flow table runs end to end in one test |

Everything else claims nothing and cannot be contradicted. Note that
`Implemented` is *not* an asserting value: it says the code exists, not that the
flows are covered — so it is the right status for a use case that is built but
not yet fully tested.

Raising the status to an asserting value is what switches coverage checking on,
so raise it last. Lowering it is always an honest answer to a failing check;
deleting a flow from the document to make the check pass is not.

### The identifier namespaces are distinct

| Prefix | Scope | Lives in |
|---|---|---|
| `FR` / `NFR` / `CON` | project | `requirements.md` |
| `UC` | project | `use_cases/` |
| `TC` | project | `test_cases/` |
| `BR` | **owned by one use case** | that use case's spec |
| `GR` | shared, no single owner | `business_rules.md` |
| `ADR` | project, immutable | `adr/` |

All are project-unique. A `BR` belongs to the use case that declares it; a rule
two use cases share has no owner, which is why `GR` exists as a second
namespace rather than being folded into `BR`.

## Headings are an interface

The construction plugins parse these documents from disk — flow headings become
test annotations, and a sensor fails the build when an annotation names a flow
that no longer exists. So:

- Keep the heading text of a flow stable, or expect to update the tests that
  name it. Renaming `A2 — Owner not found` is a code change.
- Keep the `**Status:**` line in the format the template uses.
- Do not delete a flow to make a check pass. Write the test, or lower the
  status.

If you are not using a construction plugin, none of this binds you; the
documents are still ordinary Markdown.

## Installation

```sh
/plugin marketplace add vaithu/aiup-vaadin-holon
/plugin install aiup-core
```

Then confirm the skills are present:

```sh
/help    # the nine skills above should be listed
```

## License

Apache-2.0. See [`LICENSE`](../LICENSE) and [`NOTICE`](../NOTICE).
