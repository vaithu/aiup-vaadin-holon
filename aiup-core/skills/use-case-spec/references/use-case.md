# Use Case: [Use Case Name]

## Overview

**Use Case ID:** UC-XXX   
**Use Case Name:** [Descriptive Name]   
**Primary Actor:** [Role]   
**Goal:** [In one sentence: the observable outcome the actor achieves and why — not "use the system"]   
**Status:** Draft | Reviewed | Approved | Implemented | Tested | Done | Obsolete

## Preconditions

- [Condition that must be true before the use case starts]

## Main Success Scenario

1. [Actor action or system response]
2. [Next step]
3. [Continue until goal is achieved]

## Alternative Flows

### A1: [Alternative Flow Name]

**Trigger:** [Condition that triggers this flow] (step N)
**Flow:**

1. [Step that diverges from main flow]
2. [Continuation]
3. Use case continues at step N. *(or: Use case ends.)*

## Postconditions

### Success Postconditions

- [State of the system after successful completion]

### Failure Postconditions

- [State of the system if the use case fails]

## Business Rules

### BR-001: [Rule Name]

[Description of the business rule that applies to this use case.]

### BR-002: [Shared Rule Name]

Realizes [GR-00N: Shared Rule Name](../business_rules.md#gr-00n-shared-rule-name).

*(`BR-XXX` ids are unique across the project's use cases — see the `/use-case-spec`
numbering rule. A rule several use cases share keeps its local `BR` heading here
and references a `GR-NNN` in `docs/business_rules.md`; see `/business-rules`.)*

---

## Reference

### Status Values

| Status      | Description                                      |
|-------------|--------------------------------------------------|
| Draft       | Initial version, still being written.            |
| Reviewed    | Complete, awaiting stakeholder review.           |
| Approved    | Reviewed and approved for implementation.        |
| Implemented | Implementation complete, pending testing.        |
| Tested      | All tests pass, pending final acceptance.        |
| Done        | Fully implemented, tested, and accepted.         |
| Obsolete    | No longer valid, superseded by another use case. |

### Step Writing Guidelines

| Do                                  | Don't                                         |
|-------------------------------------|-----------------------------------------------|
| "User clicks Save button"           | "User triggers onClick handler"               |
| "System validates the email format" | "System runs regex /^[\w]+@[\w]+$/"           |
| "System displays error message"     | "System throws ValidationException"           |
| "User enters check-in date"         | "User populates dateField component"          |
| "System stores the reservation"     | "System executes INSERT INTO reservations..." |
| "System records the new account"    | "System runs INSERT INTO users / SELECT ..."  |
| "System sends a confirmation email" | "System opens an SMTP connection to sendmail" |
| "System securely stores the password" | "System hashes the password with bcrypt/SHA + salt" |
| "System signs the user in"          | "System issues a JWT / signs a token with expiry" |

Steps describe **what** the actor and system achieve, never **how** it is
implemented. Keep out protocol and infrastructure terms (SMTP, JWT, bcrypt,
hashing, SQL/INSERT/SELECT, HTTP verbs, class and exception names) — those belong
in the implementation, not the specification.

### Machine-parsed contract

This document is **read by a program**, not only by people.
`UseCaseTraceabilityTest` (installed by the construction plugin's
`/traceability-sensors`) parses the file from disk and asserts that every
`@UseCase` annotation in the test tree points at a use case, a scenario and a
business rule that really exist here — and, once the status asserts completion,
that every scenario and rule has a test behind it.

Five lines are therefore structure, not prose. Changing their shape breaks the
sensor rather than merely looking different:

| Element | Pattern (multiline) | Example |
|---|---|---|
| Id | `^\*\*Use Case ID:\*\*[ \t]*(UC-\d{3})[ \t]*$` | `**Use Case ID:** UC-007` |
| Status | `^\*\*Status:\*\*[ \t]*(.*)$` | `**Status:** Done` |
| Alternative flow | `^### (A\d+):[ \t]*(.*)$` | `### A1: Duplicate Pet Name` |
| Business rule | `^### (BR-\d{3}):[ \t]*.+$` | `### BR-001: Unique Pet Name` |
| Shared-rule link | `Realizes [GR-NNN: Name](../business_rules.md#gr-nnn-slug)` | see above |

Rules the parser depends on:

- **Three digits everywhere.** `UC-7` and `BR-1` are not matched; use `UC-007`,
  `BR-001`.
- **`BR-NNN` belongs to this use case** — ids are unique across the project's
  use cases, and the sensor resolves an annotation's `BR` inside the file of the
  `UC` it names. It is what a test names. A rule shared with another use case
  keeps its local `BR` heading and references a global `GR-NNN` in
  [`docs/business_rules.md`](../business_rules.md) — see `/business-rules`.
- **The scenario key a test names** is the main scenario's default name,
  `Main Success Scenario`, or an alternative flow's full
  `"A1: Duplicate Pet Name"` — heading id *and* name. Renaming a flow renames
  the key, and the sensor reports every annotation left pointing at the old one.
- **Markdown hard breaks (trailing spaces) are fine** — the parser collapses
  whitespace — but the `**Key:**` prefix must be exact.

### The Status line is an assertion

`Done` and `Tested` are the two values that **switch the coverage sensor on**:
from that moment the build requires a test behind the main success scenario,
behind every alternative flow, and behind every business rule. Every other
value (`Draft`, `Reviewed`, `Approved`, `Implemented`) only has to satisfy
referential integrity, because specifying before building is the normal
intermediate state, not a violation.

So the status is a claim about the repository, not a label on a document. Run
the construction plugin's `/coverage-check UC-XXX` before setting it; where the
session guards are installed (`/session-guards`), an edit that sets it without
a finished audit is refused.
