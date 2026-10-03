# A worked ADR

Two records, to show both the shape of a good one and how a decision is
revised without editing it.

The example is deliberately a real, difficult decision with costs the authors
disliked. Most ADRs written from a template read as press releases; the point
of showing this one is the `Consequences` section.

---

## `ADR-009-the-service-is-the-transaction-boundary.md`

```markdown
# ADR-009: The service is the transaction boundary

**Status:** Accepted
**Date:** 2026-03-02
**Affects:** Process View, Development View
**Amends:** —

## Context

Three layers could own a transaction, and each was argued for during the first
two use cases.

The **view** is where a user action begins, so it is where "this whole button
click succeeds or fails together" is most naturally expressed. It is also
where a wait for user input can appear, and a transaction held open across one
holds a database connection for as long as the user is distracted. We had this
happen once in UC-003 while prototyping: eight concurrent edits exhausted the
pool.

The **service** sits between the view and persistence. One call is one
business operation, which is also the unit a use case specification describes
as a step. It has no access to the UI and cannot wait for a user.

The **persistence layer** is where the writes are, and putting the boundary
there makes every individual write atomic. But a use case step that writes two
aggregates — creating an order and decrementing stock — would then be two
transactions, and the specification describes it as one.

A fourth option, no explicit transactions at all with the database's
autocommit, was considered and discarded as soon as the second multi-write
step appeared.

The decision had to be made before UC-004, which is the first use case with a
step that writes two aggregates.

## Decision

A service method is the transaction boundary. It opens on entry to a public
service method and commits when that method returns.

Views do not open transactions and must not be annotated as transactional.
Persistence methods do not open their own; they join the caller's.

A use case step that must be atomic is implemented as **one** service method.
If a step needs two service calls, that is a signal the service API is wrong,
not a reason to widen the boundary.

## Consequences

A use case step maps one-to-one onto a service method, which is why the
traceability sensors can relate a specification flow to a test of that method.
That alignment was not the reason for the decision but it has turned out to be
its most useful property.

No transaction can be held across user interaction, because the layer that
talks to the user is not permitted to open one.

**It costs us two things, and both are real.**

First, a read-modify-write that spans two service calls is not atomic, and
there is no layer left where it could be made atomic without reintroducing the
problem this ADR exists to avoid. Where that matters, the second write must
detect the conflict itself — an optimistic version check, or a unique
constraint — and the use case needs an alternative flow describing what the
user sees. This is extra specification work on every such use case, forever.

Second, a service method that calls another service method nests, and the inner
transaction silently joins the outer one. That is usually what is wanted and
occasionally is not; the case where it is not is hard to see in review because
nothing in either method's text reveals it. We accept this rather than ban
service-to-service calls, which would push logic back into views.

A third, smaller cost: a long-running read inside a service method holds a
transaction it does not need. Reporting queries must be kept out of
transactional services or marked read-only.

## Alternatives considered

**Transaction in the view.** Rejected: it permits a transaction to span user
interaction, which we observed exhausting the connection pool during UC-003.
Enforcing "do not wait for the user inside one" by convention is exactly the
kind of rule that holds until the first deadline.

**Transaction in the persistence layer.** Rejected: a use case step that writes
two aggregates would be two transactions, contradicting the specification that
describes it as one step. It would have been acceptable if every step wrote a
single aggregate, which was true until UC-004 and would not have stayed true.

**No explicit transactions, relying on autocommit.** Rejected as soon as a
step needed two writes. It also makes every multi-write failure leave partial
data, with no record of which half succeeded.
```

---

## Superseding it

Two years later the project adds an outbox table, and a service method must now
write business data and an outbox row in one transaction — which ADR-009 allows
— but *also* must not hold a transaction across the message broker call, which
ADR-009 says nothing about.

That is a refinement, not a reversal, so it is `Amends`:

```markdown
# ADR-014: A service method must not call a remote system inside its transaction

**Status:** Accepted
**Date:** 2028-06-11
**Affects:** Process View
**Amends:** [ADR-009](ADR-009-the-service-is-the-transaction-boundary.md)

## Context

[ADR-009](ADR-009-the-service-is-the-transaction-boundary.md) placed the
transaction boundary at the service method and justified it by excluding the
one thing that made a view-level boundary dangerous: waiting for something
outside the database while holding a connection.

The outbox work in UC-031 reintroduced exactly that, from the other side. A
service method that writes a row and then publishes to the broker holds its
transaction for the duration of a network call to a system that can be slow or
down. ADR-009 does not forbid it — it only forbids waiting for a *user* — so
the first implementation did it, and a broker outage during load testing held
every connection in the pool until it timed out.

The underlying rule in ADR-009 was never "do not wait for a user". It was "do
not hold a transaction while waiting for something you do not control". That
was stated too narrowly because, at the time, the user was the only such thing.

## Decision

A transactional service method must not call a remote system. It writes an
outbox row and returns; a separate process reads the outbox and performs the
call, outside any transaction.

ADR-009 stands unchanged. This states the general form of the constraint it
expressed narrowly.

## Consequences

Message delivery becomes at-least-once rather than exactly-once, so every
consumer must be idempotent. This is a real and permanent cost, paid by code
that is not ours.

A failed publish is now invisible at the moment of the user action — the user
sees success, and delivery happens later or is retried. Use cases where the
user must know the message was delivered need an alternative flow, and two of
the existing ones (UC-018, UC-022) are now wrong and must be revised.

The outbox table and its reader are new infrastructure to operate and monitor.
A stalled reader is silent by construction; it needs an alarm, which is
operational work the previous design did not require.

## Alternatives considered

**Publish after the transaction commits.** Rejected: the write can succeed and
the publish fail, with nothing recording that it should be retried. This is the
same partial-failure problem ADR-009 rejected autocommit for.

**Publish before the transaction, roll back on failure.** Rejected: the message
is then sent for work that may not have happened.

**Extend the transaction to include the broker.** Rejected: a distributed
transaction requires broker and database support we do not have, and would
reintroduce exactly the long-held-connection failure this ADR exists to
prevent.
```

Then, in `ADR-009`, **only** this line changes — the body is left exactly as it
was:

```markdown
**Amends:** —
```

stays as it is, because ADR-009 amends nothing. ADR-009's `Status` also stays
`Accepted`, because it was not superseded: ADR-014 refines it.

Had ADR-014 *reversed* it — moving the boundary somewhere else — ADR-009's
header would become:

```markdown
**Status:** Superseded by [ADR-014](ADR-014-<slug>.md)
```

and nothing else in the file would be touched.

## What to copy from this

- The **Context is the longest section** and names a specific incident. "Eight
  concurrent edits exhausted the pool" is why the next reader believes the
  decision instead of merely obeying it.
- The **Consequences name three costs**, one of which the authors say they
  dislike. That is what makes the document evidence rather than advertising.
- Each **alternative has a reason it lost**, and one of them says what would
  have had to be true for it to win — which is the sentence a future reader
  checks when conditions change.
- The second ADR is honest that the first one's rule was **stated too
  narrowly**, without editing the first one to pretend otherwise.
