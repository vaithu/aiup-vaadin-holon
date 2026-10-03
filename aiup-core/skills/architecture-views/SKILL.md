---
name: architecture-views
description: >
  Creates the 4+1 architecture views as short, agent-readable markdown in
  docs/architecture/, plus the index that says which document to open for
  which task. Use when the user asks to "document the architecture", "create
  architecture views", "write the logical view", "add a 4+1 model", "set up
  docs/architecture", "explain how the code is organised", or mentions
  Kruchten, logical/process/development/physical views, architecture
  documentation, or an architecture index for an AI agent.
---

# Architecture Views (4+1)

## Prerequisites

| Required artifact | Created by |
|---|---|
| `docs/use_cases/UC-*.md` | `/use-case-spec` |
| `docs/entity_model.md` | `/entity-model` |

Neither is strictly required, but writing the views before any use case exists
produces a description of a system nobody has agreed to build yet.

## Why this exists

A coding agent starts every session knowing nothing about the project. What it
does know is whatever it reads in the first few thousand tokens. Everything
not written there gets **invented** — plausibly, consistently, and differently
each time.

Architecture documentation is usually written for humans joining a team: broad,
complete, full of diagrams, read once. That shape is actively wrong for an
agent. These documents are read *every session*, by a reader with no memory and
a finite context, that cannot see a PNG and will not skim to find the one line
that matters.

So three rules follow, and they are the whole skill:

- **Text, not pictures.** Mermaid or PlantUML, which an agent can read *and
  change*. A rendered image is a fact it can only ignore.
- **In the repository, not beside it.** A wiki drifts from the code the moment
  the code changes. A document in the same pull request as the code does not.
- **Short, not complete.** What is not written gets invented; what is written
  at length gets skimmed. Both failures produce the same result.

## The five views, and the one reordering that matters

Kruchten put the scenarios **last**, as a way to validate the other four. Put
them **first**. In this process a use case is the unit of work: it is what gets
specified, implemented, tested and marked `Done`. The other four views exist to
answer "how does a use case become code?"

| View | Question it answers | File |
|---|---|---|
| **Use Case (+1)** | What should the system do, and for whom? | `docs/use_cases.puml`, `docs/use_cases/`, `docs/test_cases/` |
| **Logical** | Which business building blocks exist? | `architecture/logical.md` + `docs/entity_model.md` |
| **Process** | How does it behave at runtime? | `architecture/process.md` |
| **Development** | How is the code organised? | `architecture/development.md` + `testing.md` |
| **Physical** | Where does it run? | `architecture/physical.md` |

The Use Case View deliberately sits **outside** `docs/architecture/`. It is the
base the architecture rests on, not a part of it.

Templates for all six files are in [references/views.md](references/views.md).

## Two rules that decide every edit

### One home per fact

State every rule **once**, in the view it belongs to. When a second document
needs it, it links.

This is not tidiness. Two documents stating the same rule are one edit away
from contradicting each other, and when they do, nobody can tell which one is
the lie — least of all an agent, which will read both, find them equally
authoritative, and pick one.

It follows that the root `CLAUDE.md` (or `AGENTS.md`) is an **index**, not a
summary. It says which document to open for which task, and holds only what is
needed in literally every turn. The moment it starts restating the Development
View, there are two Development Views.

It also follows that there is no separate "coding guidelines" folder. The
guidelines *are* the Development View.

### Prescriptive, not descriptive

The views say how a use case **is to be built**, not what the code that exists
happens to do.

This is what lets the same document guide the first use case and the fiftieth.
A descriptive view — "the `billing` package contains `BillService`, which…" —
is stale after the next commit, and teaches an agent to imitate whatever is
there, including the mistakes.

So name **patterns and use case ids**, not the classes of whichever use cases
are already implemented. "A query that spans aggregates is built in the
service, not the view" survives refactoring. "`BillService.findOverdue` does
X" does not.

### No business logic in this folder

A rule about bills, orders or patients belongs to the use case that needs it,
or to `docs/business_rules.md` when several use cases share it.

These documents say **where a rule of a given kind is honoured** — in the form,
in the service, in a database constraint. That is an architectural decision and
stays true however the rules themselves change.

## Workflow

**1. Read what exists.** The use cases, the entity model, the stack rules, the
build file. The views describe a system; you cannot describe one you have not
read.

**2. Write `docs/architecture/README.md` first.** The index is the hardest
document and the most valuable. Writing it first forces the decision about
what belongs where, before four documents exist to contradict each other.

It contains:

- the three rules above, stated for this project;
- the five-view table with links;
- a **"what an agent reads, and when"** table — task on the left, document on
  the right. This is the part an agent actually uses;
- the traceability chain: `Requirement → Use Case → View → Code → Test`, with
  one worked example naming real ids;
- a link to `adr/` for decisions.

**3. Write the four views.** Each one answers its question and stops. If a
view runs past roughly 200 lines, something in it belongs to a use case
specification or an ADR.

**4. Add the "what an agent reads" row for every new document.** A document
nobody is sent to is a document nobody reads.

**5. Wire the index into `CLAUDE.md` / `AGENTS.md`.** One line per task,
pointing at the view. Delete anything in the root file that the views now
state — that is the duplication this skill exists to prevent.

**6. Check the links resolve.**

```sh
grep -oE '\]\([^)#]+\.md[^)]*\)' docs/architecture/*.md | sort -u
```

## When a view and the code disagree

There are exactly two honest fixes, and choosing between them is the decision:

1. **The code is wrong.** Change the code.
2. **The view is out of date.** Change the view — and if the question could
   have gone either way, write an ADR saying why it changed.

Quietly loosening a view so the current code complies is neither. It converts
a constraint into a description, and a description constrains nothing.

If the architecture is enforced by ArchUnit rules, the rule and the view must
be changed in the same commit. A rule the view does not mention is folklore; a
view the rules do not enforce is a wish.

## Constraints

**Allowed**

- Markdown under `docs/architecture/`.
- Mermaid and PlantUML diagrams, as source, in the file.
- Links to other documents in the repository.
- Tables. An agent parses a table far more reliably than a paragraph.

**Banned**

- Binary images — PNG, JPG, SVG exported from a drawing tool. An agent cannot
  read them and cannot update them.
- A wiki, Confluence page, or shared drive as the home of any of this.
- Restating in one view what another view already says. Link instead.
- Business rules in `docs/architecture/`. They belong to a use case or to
  `business_rules.md`.
- Naming concrete classes as the definition of a convention.
- A view that documents one feature's implementation. That is a use case
  specification wearing a disguise.

## DO NOT

- **Do not write all five views at length on day one.** Write the index and a
  thin version of each, then grow them when an agent gets something wrong —
  the mistake tells you exactly which sentence was missing.
- **Do not copy the views into `CLAUDE.md`.** That is the duplication failure,
  and it is the most common one, because the root file is what gets read first
  and the temptation to put everything there is strong.
- **Do not let a view become a changelog.** "We used to do X, now we do Y"
  belongs in an ADR. A view states the current rule, with no history.
- **Do not document aspiration as fact.** If the project does not yet have a
  deployment pipeline, the Physical View says so in one line. An agent that
  reads about a pipeline that does not exist will write code for it.
- **Do not skip the "what an agent reads, and when" table.** Five excellent
  documents with no index is five documents that get read in the wrong order,
  or not at all.
