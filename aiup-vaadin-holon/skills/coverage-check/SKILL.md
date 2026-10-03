---
name: coverage-check
description: >
  Audits whether a use case or test case is really covered by tests before its
  Status line is allowed to claim Done, Tested or Automated. Runs the read-only
  uc-coverage agent, which reads every flow, every business rule and every test
  body and reports the gaps. Use when the user asks to "check coverage for
  UC-XXX", "is UC-XXX really done", "audit the tests for this use case",
  "verify before marking Done", "can I set Status to Done", "find untested
  flows", or mentions coverage audit, traceability gaps, weak assertions, or
  unenforced business rules.
---

# Coverage Check

## Prerequisites

| Required artifact | Created by |
|---|---|
| `docs/use_cases/UC-NNN-*.md` or `docs/test_cases/TC-NNN-*.md` | `/use-case-spec`, `/test-case` |
| A test tree with tests annotated `@UseCase` / `@TestCase` | `/traceability-sensors` |

Without the annotations the audit still works — it falls back to searching the
test tree as text — but it will report naming findings on every scenario.

## Why this exists

The traceability sensors answer one question very well: *does a link exist?*
They parse the specifications, read `@UseCase` and `@TestCase` off the
compiled test classes, and fail when the two disagree. That catches the
drift that matters most, cheaply, on every build.

What a sensor cannot do is read a test body. This passes every sensor in the
project:

```java
@Test
@UseCase(id = "UC-007", scenario = "A1 — duplicate pet name")
void rejectsDuplicateName() {
    assertThat(service).isNotNull();
}
```

The link is there. The behaviour is not. And an agent working to close a
sensor failure is under exactly the pressure that produces this test —
the fastest way to make the red go away is to create a method with the right
annotation, not to understand the alternative flow.

So there are two different checks, and they belong in two different places:

| | Sensors | Coverage check |
|---|---|---|
| Asks | Does the link exist? | Does the test do the thing? |
| Reads | Document headings, annotations | Document prose, test bodies, production code |
| Cost | ~5 seconds | A few minutes of agent context |
| Runs | Every build | Before a `Status:` changes |
| Verdict | Pass / fail | A report |

This skill is the second column. It is deliberately not a test: it is a
judgement about whether an assertion is meaningful, and nothing mechanical
can make that judgement.

## What it does

Dispatches the **`uc-coverage`** agent — read-only, `Read`/`Grep`/`Glob`
only — with the id to audit. The agent:

1. reads the specification and lists every flow, every `BR-NNN`, the realized
   `GR-NNN`s, and the current `**Status:**`;
2. finds every test that claims the id, by annotation *and* by text;
3. reads each test body and classifies the scenario as **Covered**,
   **Asserted weakly**, **Mislabelled** or **Absent**;
4. for each business rule, looks for the assertion that would fail if the rule
   were deleted from production code;
5. compares all of that against what the `Status:` line claims, and ends with
   one of three verdicts.

It never edits, never runs a build, and never changes the status itself.
Separating the audit from the fix is the whole point: an agent that can both
find a gap and close it will close it in the cheapest way available.

## Workflow

**1. Run the audit.**

```
/coverage-check UC-007
```

Launch the `uc-coverage` agent with the id. Do not pre-digest the
specification for it — it must read the document itself, or it inherits your
reading of it along with your blind spots.

**2. Read the verdict.**

| Verdict | Then |
|---|---|
| `Status is supported` | The status line may stand or be raised. Nothing to do. |
| `Status claims nothing` | A draft. The report is a work list, not a failure. |
| `Status is not supported` | Fix the gaps **or** lower the status. Those are the only two options. |

**3. Act on the findings — in this order.**

1. **Absent** scenarios first. A missing test is a missing test.
2. **Mislabelled** next. These are actively misleading: the sensor is green,
   the annotation lies, and the flow it claims to cover has nothing.
3. **Unenforced business rules.** For each, decide honestly whether the rule
   is real. If it is, implement and assert it. If it was aspirational, delete
   it from the specification — a rule nobody enforces is a rule that teaches
   readers the document cannot be trusted.
4. **Asserted weakly** last, but do not skip them. Strengthen the assertion so
   that it fails when the behaviour is removed. The test that cannot fail is
   worse than no test, because it occupies the slot where a real one would go.

**4. Re-run the sensors.** Any new test changes the annotation set:

```sh
mvn -q test -Dgroups=sensor
```

**5. Only now, set the `Status:` line.** The status is an assertion about the
repository (see the use-case and test-case templates). This skill is how that
assertion gets earned.

## When to run it

- Before raising a `Status:` to `Done`, `Tested` or `Automated`. If the
  session guards are installed, this is enforced: they refuse an asserting
  status for an id that has not been audited in the current session.
- After a large `/implement` run, on each use case it touched.
- Periodically on use cases marked `Done` long ago. Coverage decays —
  a flow gets refactored away, a rule changes, and the status quietly becomes
  a lie. Nothing warns you about this except looking.

## What a good finding looks like

> `rejectsDuplicateName` asserts the notification text. Assert the pet count
> is unchanged as well — the current test passes if the row is inserted and
> the message is shown.

Specific, names the test, names what the current assertion fails to exclude.

> Coverage for A1 could be improved.

Useless. If the agent produces findings like this, send it back with the
instruction to quote the assertion it is complaining about.

## Constraints

**Allowed**

- The read-only `uc-coverage` agent: `Read`, `Grep`, `Glob`.
- Reading anything under `docs/`, `src/main/java/`, `src/test/java/`.
- Producing a report in the agent's own context.

**Banned**

- Editing any file from inside the audit. The auditor that can fix what it
  finds will fix it the cheap way.
- Running `mvn`, builds, or tests from inside the audit — it is a reading
  task, and a build makes it slow enough that people stop running it.
- Changing a `**Status:**` line as part of the audit.
- Treating the sensors as a substitute. A green sensor run says the links are
  intact and says nothing at all about whether the tests assert anything.
- Auditing an id the user did not name.

## DO NOT

- **Do not run this instead of the sensors.** It is slower, it costs context,
  and it is a judgement rather than a gate. The sensors run on every build;
  this runs at a status change.
- **Do not paste the specification into the agent prompt.** Give it the id.
  Reading the document is step one of the audit.
- **Do not argue with a finding you have not checked.** If the report says a
  rule is unenforced, grep for it before dismissing it.
- **Do not close a finding by deleting the scenario from the document**
  unless the scenario genuinely is not a requirement. Shrinking the
  specification until it matches the tests is how a project ends up with a
  perfectly traceable description of the wrong system.
