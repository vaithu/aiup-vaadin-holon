# Worked Example

A small library-lending project with five use cases. Three rules turned out to
be shared; the rest stayed local.

## Before — the same rule, stated four times

`docs/use_cases/UC-003-register-member.md`:

```markdown
### BR-007: Contact Details Complete

A member record must carry first name, last name, email and telephone. None of
them may be empty.
```

`docs/use_cases/UC-006-update-member.md`:

```markdown
### BR-015: Member Contact Details Required

First name, last name, email and telephone are all mandatory on a member; an
update may not clear any of them.
```

Two statements of one rule, already drifting — only the second mentions update.
A third appeared in `UC-009`, and a fourth in `UC-011` said "email is optional
for a staff member", which turned out to be a genuinely different rule and
stayed local.

## After — `docs/business_rules.md`

```markdown
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
it: `@UseCase(id = "UC-007", businessRules = "BR-017")` means "BR-017 of
UC-007", and the sensor resolves it inside that one file. A rule that no single
use case owns has no place in that namespace. `GR` — global rule — is a second
namespace precisely so a shared rule has an id of its own that needs no use case
to give it meaning.

| Id     | Rule                       | Realized by                                      | From             |
|--------|----------------------------|--------------------------------------------------|------------------|
| GR-001 | Complete Member Contact Details | UC-003 BR-007, UC-006 BR-015, UC-009 BR-022 | FR-004           |
| GR-002 | Deterministic Ordering     | UC-004 BR-009 (titles), UC-007 BR-018 (loans)    | NFR-003          |
| GR-003 | Loan Limit per Member      | UC-007 BR-017, UC-008 BR-020                     | FR-011, C-002    |

## GR-001: Complete Member Contact Details

A member record always carries first name, last name, email and telephone. None
of them may be empty — neither when the member is registered nor after an
update — because the library must be able to reach a member about an overdue
loan.

Staff accounts are not member records and are out of scope for this rule.

**Realized by:** UC-003 BR-007, UC-006 BR-015, UC-009 BR-022

## GR-002: Deterministic Ordering

Every list has a defined order, so the same data is always presented the same
way:

- titles alphabetically by title, then by author,
- a member's loans by due date ascending, the overdue ones first.

**Realized by:** UC-004 BR-009 (titles), UC-007 BR-018 (loans)

## GR-003: Loan Limit per Member

A member may have at most five loans open at any time. A loan counts as open
from the moment it is issued until it is returned; a renewal does not create a
new loan. A reservation is not a loan and does not count towards the limit.

**Realized by:** UC-007 BR-017, UC-008 BR-020
```

## After — the use case side

`docs/use_cases/UC-003-register-member.md` — heading number and name kept, body
replaced:

```markdown
### BR-007: Complete Member Contact Details

Realizes [GR-001: Complete Member Contact Details](../business_rules.md#gr-001-complete-member-contact-details).
```

`docs/use_cases/UC-006-update-member.md`:

```markdown
### BR-015: Complete Member Contact Details

Realizes [GR-001: Complete Member Contact Details](../business_rules.md#gr-001-complete-member-contact-details).
```

`docs/use_cases/UC-007-issue-loan.md` keeps **two** local headings, each
pointing at a different global rule, and a third that stayed local:

```markdown
### BR-017: Loan Limit per Member

Realizes [GR-003: Loan Limit per Member](../business_rules.md#gr-003-loan-limit-per-member).

### BR-018: Deterministic Ordering

Realizes [GR-002: Deterministic Ordering](../business_rules.md#gr-002-deterministic-ordering).

### BR-019: Due Date Is Fourteen Days Out

A loan issued today is due fourteen calendar days later, regardless of opening
hours or holidays.
```

`BR-019` is stated in full because only UC-007 issues loans. Promoting it would
have produced a `GR` with one realizer — which the sensor rejects.

## What the tests then look like

```java
@Test
@UseCase(id = "UC-007", scenario = "Main Success Scenario", businessRules = { "BR-017", "BR-019" })
void issuingALoanSetsTheDueDateFourteenDaysOut() { … }

@Test
@UseCase(id = "UC-007", scenario = "A2: Loan Limit Reached", businessRules = "BR-017")
void aSixthOpenLoanIsRefused() { … }
```

The annotation names the **local** `BR-017`, because that is the rule of that
use case. `BusinessRuleTraceabilityTest` follows `BR-017 → GR-003` and confirms
`UC-007 BR-017` appears on GR-003's `Realized by:` line, so the global rule is
reachable from the test without the test ever naming a `GR` id.

## What the grouping conversation looked like

Three candidates were proposed to the user before anything was edited:

| Candidate | Use cases | Decision |
|---|---|---|
| Complete contact details | UC-003, UC-006, UC-009, **UC-011** | Promoted — but UC-011 excluded: a staff account has no telephone, so its rule is genuinely different and stayed as a local `BR`. The exclusion was written into GR-001's body. |
| Deterministic ordering | UC-004, UC-007 | Promoted. |
| Loan limit | UC-007, UC-008 | Promoted. UC-008 (renew) was nearly missed: renewing does not create a loan, but it must still check the limit. |
| Due date fourteen days out | UC-007 | Left local — one realizer. |

Step 4 of the workflow exists for the UC-011 row. Merging it would have produced
a rule that was wrong for one of its realizers, and no sensor would have caught
it — the links would all have resolved.
