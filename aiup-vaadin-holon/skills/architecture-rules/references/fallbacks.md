# `@Fallback` — the recorded exception

Every rule in `ArchitectureTest` and `SourceConventionsTest` has an escape
hatch, and that is deliberate. A rule with no escape hatch does not get
obeyed; it gets deleted the first time someone meets a legitimate case it
did not anticipate. Once deleted it protects nothing, and the deletion
happens in a hurry, under deadline, by whoever is least able to judge
whether the rule mattered.

`@Fallback` is the alternative. It keeps the rule, and makes the exception
cost something small but non-zero: a sentence of justification that sits in
the source forever and shows up in every future `git blame` and code review.

```java
@Fallback(reason = "Holon Datastore cannot express the recursive CTE the "
                 + "org-chart report needs; see ADR-007")
public class OrgChartReportService { ... }
```

## What it actually does

`@Fallback` is read by the rules themselves. Both `ArchitectureTest` and
`SourceConventionsTest` exclude annotated types before they assert. It is a
`@Retention(RUNTIME)` annotation on `TYPE` so ArchUnit can see it on the
compiled class and the text scanner can see it in the source.

It does **not** suppress everything. It exempts the annotated class from the
rules it can plausibly violate — a banned import, a layer crossing, a plain
SQL string. It does not exempt it from the traceability sensors, because a
fallback is still part of a use case and still has to be covered by a test.

## When it is legitimate

Three cases, and only three.

**The framework genuinely has no equivalent.** This is the common one. Holon
covers most of what an application needs, but not all of it, and the
honest answer is sometimes raw Vaadin or plain Spring. The `holon-stack.md`
fallback policy already names this case; `@Fallback` is how you record it in
a way a machine can count.

**The Holon equivalent exists but is measurably wrong here.** Rare, and the
reason must contain the measurement. "Slow" is not a reason. "The Datastore
query planner issues N+1 selects for this 40k-row export; measured 18s vs
400ms" is a reason.

**A deliberate, time-boxed migration.** Code being moved onto the stack in
stages. The reason must name the ticket or ADR that ends the exception,
otherwise the time box does not exist.

## When it is not

- To make a failing build green before a demo. The rule is reporting a real
  violation; the fix is the fix.
- Because the Holon idiom was unfamiliar. Read `holon-stack.md` first. Most
  `@Fallback` annotations written in the first week of a project are wrong.
- On a whole package or a base class, to cover many violations at once.
  `@Fallback` is per class, and that is the point — the cost should scale
  with the size of the exception.

## The empty-reason rule

`SourceConventionsTest.everyFallbackGivesAReason` fails on `reason = ""` and on
the usual placeholders — `n/a`, `tbd`, `todo`, `none`, `because`. This looks
petty and is not. Without it, `@Fallback` degrades within a month into
`@SuppressWarnings` — a token people paste to silence a tool, carrying no
information. The check is crude, but it is enough to stop the reflexive paste
and force a sentence.

A good reason answers one question: *what did you try from the Holon stack,
and what specifically happened when you tried it?*

## Reviewing them

`@Fallback` is designed to be greppable:

```sh
git grep -n '@Fallback' src/main/java
```

Two things make that list worth reading periodically.

The **count** is a health signal. A handful in a mature codebase is normal.
Thirty means the ban list is wrong for this project and should be argued
about openly — amend `holon-stack.md`, or record an ADR that changes the
policy — rather than being routed around one class at a time.

The **ages** are a decay signal. A fallback written for a framework gap that
has since been closed is now just a lie about the stack. When Holon ships
the missing capability, the `@Fallback` and the code under it should go
together. Nothing automated can detect this, which is why it belongs in a
human review rather than in a sensor.

## Relationship to the inline comment convention

`holon-stack.md` asks for `// FALLBACK: no Holon equivalent for <thing>` at
the point of use. Keep writing it — it explains the specific line to the
next reader. `@Fallback` is the class-level, machine-readable counterpart:
the comment tells a human why this line is like this, the annotation tells
the build that the exception was a decision rather than an accident.

Where both appear, the annotation's `reason` should be the fuller statement.
