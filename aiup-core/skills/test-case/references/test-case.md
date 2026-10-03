# Test Case: [Journey Name]

## Overview

**ID:** TC-XXX   
**Goal:** [In one sentence: who does what across the journey and which outcome is verified end-to-end]   
**Priority:** Critical | High | Medium | Low   
**Status:** Draft | Reviewed | Approved | Automated | Obsolete   
**Process:** [process-name.bpmn](../processes/process-name.bpmn) — [the path through it: Start event → UC-XXX Name → Gateway? answer → UC-YYY Name → End event] *(omit this line when the project has no process models)*

## Roles

- [Role acting in the journey (what they do)]
- [Second role, if the journey spans several]

## Preconditions

- [Data or state that must exist before the journey starts, with its source (e.g. Flyway test data `V900__test_data.sql`)]

## Flow

| Step | Name          | Description                                        | Test Data          | Use Case                                      |
|------|---------------|----------------------------------------------------|--------------------|-----------------------------------------------|
| 1    | [Action name] | [What the role does and what the system shows]     | [Literal values]   | [UC-XXX](../use_cases/UC-XXX-name.md)         |
| 2    | [Verify …]    | [Observable result that anchors the transition]    | -                  | -                                             |
| 3    | [Next action] | [Continues with state created in earlier steps]    | [Literal values]   | [UC-YYY](../use_cases/UC-YYY-name.md)         |

## Validation

1. **[Check name]**: [Cross-cutting end-state expectation, observable through the UI after the flow completes]
2. **[Check name]**: [Second expectation]

## Postconditions

- [Data record the journey creates or changes and leaves behind]
- [Cleanup-order constraint, if any (e.g. "The enrollment must be deleted before the student it belongs to")]

---

## Reference

### Status Values

| Status    | Description                                          |
|-----------|------------------------------------------------------|
| Draft     | Initial version, still being written.                |
| Reviewed  | Complete, awaiting stakeholder review.               |
| Approved  | Reviewed and approved for automation.                |
| Automated | An end-to-end test implements this test case.        |
| Obsolete  | No longer valid, superseded by another test case.    |

### Priority Values

| Priority | Description                                                        |
|----------|--------------------------------------------------------------------|
| Critical | The system's core journey — run on every change.                   |
| High     | Important journey — run in every full test pass.                   |
| Medium   | Secondary journey — run regularly.                                 |
| Low      | Rare or edge journey — run when the affected area changes.         |

### Flow Writing Guidelines

- **Step** numbers run from 1 without gaps; the automated test derives one step method per row.
- **Name** is short and action-oriented — it becomes the step method name in the test.
- **Description** says what the role does and what the system shows — business language, no implementation detail (no HTTP verbs, SQL, class names, or protocol terms; see the use case template's step-writing guidelines).
- **Test Data** holds the literal values the step enters, comma-separated; `-` when the step needs none.
- **Use Case** links the action to its specification with a relative path; verification rows use `-`.

### Postconditions Guidelines

Postconditions inventory the data the journey leaves behind — the automated test derives its cleanup from this list (delete exactly these records, nothing else). State every record the flow creates or changes, identified by the literal test data values, and any deletion-order constraint imposed by business rules (dependent records before their parents).

### Machine-parsed contract

This document is **read by a program**, not only by people.
`TestCaseTraceabilityTest` (installed by the construction plugin's
`/traceability-sensors`) parses the file from disk and compares it with the
`@TestCase(id = …, useCases = {…})` annotation on the journey test class, in
both directions — so the annotation cannot drift away from the document it
automates.

| Element | Pattern (multiline) | Example |
|---|---|---|
| Id | `^\*\*ID:\*\*[ \t]*(TC-\d{3})[ \t]*$` | `**ID:** TC-001` |
| Status | `^\*\*Status:\*\*[ \t]*(.*)$` | `**Status:** Automated` |
| Use case link in the Flow table | `\[(UC-\d{3})\]\(\.\./use_cases/[^)]+\)` | `[UC-004](../use_cases/UC-004-find-owners.md)` |
| Journey test class | `^TC(\d{3})\w*IT$` | `TC001VisitBookedForKnownPetIT` |

Rules the parser depends on:

- **Three digits everywhere** — `TC-001`, not `TC-1`.
- **The set of `UC-NNN` links in the Flow table must equal the `useCases` of
  the annotation**, as a set: a use case linked here but missing from the
  annotation, or named there but not linked here, is a failure. Verification
  rows carry `-` and contribute nothing.
- **Every linked path must resolve** to a file in `docs/use_cases/`.
- **The class name encodes the id**: `TC-001` → `TC001<Name>IT`. The `IT`
  suffix is what makes Failsafe run it in the `verify` phase; a journey named
  `*Test` would start a browser inside the unit-test phase, and
  `TestLayerConventionsTest` fails the build for it.

### The Status line is an assertion

`Automated` is the value that **switches the coverage sensor on**: from that
moment the build requires a `TC<NNN><Name>IT` class annotated
`@TestCase(id = "TC-NNN", useCases = {…})` to exist and to agree with the Flow
table. Every other value (`Draft`, `Reviewed`, `Approved`) only has to satisfy
referential integrity.

So the status is a claim about the repository, not a label on a document. Run
the construction plugin's `/coverage-check TC-XXX` before setting it; where the
session guards are installed (`/session-guards`), an edit that sets it without
a finished audit is refused.

### The `Process:` line

When the project keeps BPMN process models in `docs/processes/`, each test case
names the model it walks and the path it takes through it. One test case per
distinct path: every gateway answer that leads to a different sequence of use
cases is its own `TC`. Activities in the model are named `UC-NNN <Use Case
Name>`, so the path reads as the Flow table's use case column plus the gateway
decisions between them, and a precondition exists for each decision to force
the branch (see the Preconditions section).

Omit the line entirely when the project has no process models — it is optional
structure, and an empty or placeholder value is worse than its absence.
