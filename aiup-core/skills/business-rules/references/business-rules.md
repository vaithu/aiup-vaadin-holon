# Business Rules

Rules that more than one use case must honour.

A rule that belongs to a single use case stays **in** that use case as
`BR-NNN` — it is part of that use case's story and has no life of its own. A
rule that several use cases share lives **here** as `GR-NNN`, is stated once,
and is referenced by each use case that applies it. The use case keeps its local
`BR-NNN` heading, so tests and the traceability sensors still point at a rule of
that use case; the heading says which shared rule it realizes instead of
restating it.

`BusinessRuleTraceabilityTest` (`./mvnw -q test -Dgroups=sensor`) keeps the two
sides honest: it fails when a use case references a rule that does not exist,
when a link lands on a renamed heading, when the `**Realized by:**` line and the
use cases disagree in either direction, when the summary table drifts from the
rules below it, or when a rule here is realized by fewer than two use cases.

**Why `GR` and not `BR`.** A `BR-NNN` id belongs to the use case that declares
it: `@UseCase(id = "UC-007", businessRules = "BR-014")` means "BR-014 of
UC-007", and the sensor resolves it inside that one file. A rule that no single
use case owns has no place in that namespace. `GR` — global rule — is a second
namespace precisely so a shared rule has an id of its own that needs no use case
to give it meaning.

| Id     | Rule                     | Realized by                                   | From             |
|--------|--------------------------|-----------------------------------------------|------------------|
| GR-001 | [Rule Name]              | UC-XXX BR-004, UC-YYY BR-011                  | FR-005           |
| GR-002 | [Second Rule Name]       | UC-XXX BR-005 (qualifier), UC-ZZZ BR-019 (qualifier) | NFR-001, NFR-002 |

## GR-001: [Rule Name]

[The rule, stated once, in business language. Say what the business permits or
forbids — not how a form, a query, or a constraint enforces it. Name the edges
a test would otherwise have to guess: case sensitivity, whether a bound is
inclusive, whether the rule differs between creation and update.]

**Realized by:** UC-XXX BR-004, UC-YYY BR-011

## GR-002: [Second Rule Name]

[Second rule. When the use cases apply it to different subjects, qualify each
entry on the Realized by line so the reader knows which is which.]

**Realized by:** UC-XXX BR-005 (first subject), UC-ZZZ BR-019 (second subject)

---

## Reference

### What each column of the summary table holds

| Column | Content |
|--------|---------|
| `Id` | `GR-NNN`, allocated from one project-wide sequence, never reused. |
| `Rule` | The heading text of the rule below, character for character. |
| `Realized by` | Every `UC-NNN BR-NNN` pair, comma-separated, matching the rule's own `Realized by:` line. |
| `From` | The `FR-NNN` / `NFR-NNN` / `C-NNN` in `docs/requirements.md` the rule derives from. Several, comma-separated, when it serves more than one. |

### The use case side

The use case keeps its heading and replaces its body with one sentence:

```markdown
### BR-014: Unique Pet Name per Owner

Realizes [GR-006: Unique Pet Name per Owner](../business_rules.md#gr-006-unique-pet-name-per-owner).
```

The heading name should match the global rule's name, and the heading's number
is whatever that use case already had — promoting a rule never renumbers it.
The anchor is the GitHub slug of the `## GR-NNN: Name` heading: lowercase,
spaces replaced by `-`, punctuation (`:`) dropped.

| Heading | Anchor |
|---|---|
| `## GR-006: Unique Pet Name per Owner` | `#gr-006-unique-pet-name-per-owner` |
| `## GR-005: Telephone Format` | `#gr-005-telephone-format` |

### Status of a rule

A rule here has no status line: it is in force the moment a use case realizes
it. A rule that no longer applies is marked by appending ` (Obsolete)` to its
heading and emptying its `Realized by:` line to `**Realized by:** —`, after
every use case has dropped the link. The id is never reused.

### Writing guidelines

| Do | Don't |
|---|---|
| "An owner's telephone number is exactly 10 digits." | "The telephone field has a 10-digit regex validator." |
| "Names are compared case-insensitively." | "The query uses `LOWER(name)`." |
| "A pet type must be chosen when a pet is first recorded. On update it may be left unchanged." | "Type is `@NotNull` on the bean." |
| "Rows are fetched as the user scrolls; there are no page controls." | "The listing uses a lazy `DataProvider` with a 50-row page size." |

The rule describes the business constraint. Where and how it is enforced — the
form, the service, a database constraint, or all three — is an implementation
decision, and belongs in `docs/architecture/`, not here.
