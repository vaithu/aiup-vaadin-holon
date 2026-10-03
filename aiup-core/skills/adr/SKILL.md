---
name: adr
description: >
  Writes Architecture Decision Records — short, numbered, immutable documents
  recording a decision that could have gone the other way, why it went this
  way, and what it costs. Use when the user asks to "write an ADR", "record
  this decision", "document why we chose X", "add an architecture decision
  record", "supersede ADR-00N", "why did we do it this way", or mentions ADRs,
  decision records, or capturing a design rationale.
---

# Architecture Decision Records

## Prerequisites

| Required artifact | Created by |
|---|---|
| `docs/architecture/` and its `README.md` | `/architecture-views` |

ADRs live at `docs/architecture/adr/ADR-NNN-<kebab-slug>.md` and are listed in
the architecture index.

## Why this exists

The views say **what** the architecture is. They are rewritten whenever it
changes, so they only ever hold the current state. That is the right shape for
a document an agent reads every session — and it means the reasoning is lost.

An agent, or a new contributor, that meets a convention with no visible reason
does one of two things: obeys it as a ritual, or decides it is arbitrary and
works around it. Both are bad, and the second one is worse because it looks
like initiative.

An ADR is the counterweight. It records a decision **that could have gone the
other way**, the alternatives that were actually considered, and what the
choice costs. Someone facing the same question again reads it and decides the
way the team decided, instead of deciding anew — or decides differently, on
purpose, with a new ADR saying why.

The division of labour is clean:

| | Views | ADRs |
|---|---|---|
| Hold | the current rule | the reasoning behind it |
| When written | whenever the rule changes | once, when the decision is made |
| Edited later | constantly | never — superseded instead |
| Read | every session | when the rule is questioned |

## What deserves an ADR

The test is simple and worth applying honestly: **could a competent team have
chosen otherwise?**

Write one for:

- a choice between real alternatives — a persistence approach, a UI model, a
  transaction boundary, where validation lives;
- a decision whose *absence* would be a decision — "no authentication",
  "no service layer", "no caching";
- anything that constrains future work in a way that is not obvious from the
  code;
- a reversal of a previous ADR;
- a rule that reviewers keep re-litigating. The repeated argument is the
  signal.

Do **not** write one for:

- a decision with no alternative — using the language the project is written
  in;
- a coding convention with no trade-off. That is a line in the Development
  View;
- a business rule. That belongs to a use case or the shared rule catalogue;
- a plan, a task list, or a status update. An ADR records a decision, not an
  intention.

## The format

```markdown
# ADR-007: Specification traceability is enforced by tests, not by review

**Status:** Accepted
**Date:** 2026-09-14
**Affects:** Development View, Use Case View
**Amends:** —

## Context

<The forces. What situation made this a question? What constraints were real?
Written so that someone who was not there understands why it was hard. This is
the section that ages well and the one most often written too short.>

## Decision

<What was decided, in the present tense and the active voice. "The repository
is the transaction boundary." Not "we decided that we should probably…".>

## Consequences

<What this costs, what it makes easy, what it makes hard. Include the
consequences you dislike — an ADR with only benefits is marketing, and a
reader learns nothing from it.>

## Alternatives considered

<Each one, with the reason it lost. "Rejected" is not a reason.>
```

The header fields:

| Field | Means |
|---|---|
| `Status` | `Proposed`, `Accepted`, `Superseded by ADR-NNN`, `Deprecated` |
| `Date` | when it was accepted, ISO format |
| `Affects` | which views this constrains — so a change to a view can find its ADRs |
| `Amends` | the earlier ADR this refines without replacing, or `—` |

A full worked example is in [references/example.md](references/example.md).

## The rule that makes them worth keeping

**An ADR is immutable.** Once accepted, the text does not change. Only the
`Status` line changes, and only to point at the ADR that replaced it.

This is the whole discipline, and it is constantly tempting to break. When a
decision is revisited, the instinct is to edit the ADR so it reads correctly
now. Do not. An edited ADR loses the only thing it had — a truthful record of
what was known and believed at the time. A reader who cannot tell the original
reasoning from a later patch cannot trust any of it, and the corpus stops being
evidence and becomes just another set of documents that might be stale.

To change a decision:

1. Write a new ADR. It gets the next number.
2. Its `Context` names the old one and says what changed — new information, a
   new constraint, a cost that turned out higher than expected.
3. Set the old one's status to `Superseded by ADR-NNN`, with a link. Change
   nothing else in it.
4. Update the view that states the rule, and the index table.

`Amends` is the lighter form, for an ADR that sharpens an earlier one without
reversing it — the earlier decision stands and stays `Accepted`.

## Writing them after the fact

A project that adopts ADRs part-way through has decisions already embodied in
the code and nowhere else. Reconstructing them is worth doing, and honest if
labelled: say in the index that `ADR-001` to `ADR-00N` were written
retrospectively.

Reconstruct the reasoning, not a justification. If nobody remembers why, say
that — "the reason is not recorded; the cost of changing it now is X" is
genuinely useful, and far better than inventing a rationale that will be
quoted back as though someone meant it.

From the point of adoption onward, a decision gets its ADR when it is made.

## Workflow

**1. Check it is not already answered.**

```sh
grep -ril "<keyword>" docs/architecture/adr/
```

A second ADR on a settled question is how a corpus starts contradicting itself.

**2. Take the next number.** Numbers are never reused, including by superseded
ADRs.

```sh
ls docs/architecture/adr/ | tail -1
```

**3. Write it — `Context` first, and at length.** The decision is usually one
sentence; the forces are the part worth recording. If the Context does not make
the decision feel difficult, either it is under-written or the decision did not
need an ADR.

**4. Fill in `Alternatives considered` truthfully.** Including the one that
was nearly chosen, and what would have had to be true for it to win.

**5. Update the view it affects.** The ADR explains; the view states the rule.
A decision that never reaches a view is a decision nobody will follow.

**6. Add it to the index table** in `docs/architecture/README.md`.

**7. If it changes an enforced rule**, change the rule in the same commit.

## Constraints

**Allowed**

- One markdown file per decision at `docs/architecture/adr/ADR-NNN-<slug>.md`.
- The four header fields, then Context, Decision, Consequences, Alternatives.
- Links to views, use cases, other ADRs, and to code.

**Banned**

- Editing an accepted ADR other than its `Status` line.
- Reusing a number.
- More than one decision per ADR. Two decisions mean two records, so that one
  can be superseded without the other.
- An ADR with no alternatives. If there was no alternative, there was no
  decision.
- Business rules, task lists, or status reports in `adr/`.
- Deleting a superseded ADR. The record of a decision that was reversed is
  more valuable than one that was not.

## DO NOT

- **Do not write the Consequences section as a list of benefits.** The costs
  are why the next reader needs the record. An ADR that claims a free lunch is
  one nobody will believe when it matters.
- **Do not use an ADR to announce a plan.** "We will migrate to X" is not a
  decision record; it is a ticket. The ADR is written when the decision is
  made, and it describes the state from then on.
- **Do not let ADRs become the place rules live.** A reader looking for the
  current convention must find it in a view. If they have to reconstruct it by
  reading eleven ADRs in order, the views have failed.
- **Do not supersede by editing.** It is the one rule that makes the whole
  corpus trustworthy, and it is the one that gets broken first.
- **Do not number by date or by feature.** A flat, monotonic integer. Anything
  cleverer breaks the moment two people write one on the same day.
