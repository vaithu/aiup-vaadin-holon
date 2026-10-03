# JPA Anti-Patterns — Detection & Guardrail

This file is the authoritative catalog of JPA/Hibernate anti-patterns for
`aiup-vaadin-holon`. Every skill that can emit `jakarta.persistence.*` code, a
JPA entity, a repository, a `@Transactional` service, or a JPA-related property
in `application.properties`/`application.yml` MUST read this file **before**
generating and MUST run the **Guardrail Protocol** below before emitting.

> Reminder: in this stack JPA is a **fallback**, not the default. Persistence is
> Holon `Datastore` / `BeanDatastoreHelper` (see [`holon-stack.md`](holon-stack.md)).
> Every anti-pattern below is additionally a signal that the JPA path may be the
> wrong path entirely.

---

## Guardrail Protocol (mandatory)

Run this **immediately before** writing any file that contains JPA code or JPA
configuration, and again after the file is written.

1. **Scan** the code you are about to emit against the Detection Table below.
2. **Classify** every hit by severity:

| Severity | Action |
|---|---|
| 🛑 **BLOCK** | **Do not emit.** Rewrite using the "Correct form" column. If the correct form is not achievable, **stop and ask the developer** — never emit the anti-pattern and continue. |
| ⚠️ **WARN** | Emit only with an inline waiver comment on the line above (format below) **and** list it in the final report. If you cannot justify it in one line, treat it as BLOCK. |

3. **Waiver comment format** (⚠️ only — never valid for 🛑):

```java
// JPA-WAIVER(<id>): <one-line justification of why the correct form cannot be used>
```

e.g. `// JPA-WAIVER(JPA-012): legacy table has no version column; optimistic locking added in V12`

4. **Report** at the end of the task:

```
## JPA anti-pattern guardrail

- Blocked and rewritten: JPA-001 (EAGER on Customer.owner → LAZY), JPA-004 (…)
- Waived (⚠️): JPA-012 — <justification>
- Clean: no other anti-patterns detected
```

If nothing was detected, print `✅ JPA anti-pattern guardrail: clean`.

5. **Never** silently strip an anti-pattern from *pre-existing* code the task did
   not ask you to touch. Report it as a finding instead, and ask before refactoring.

---

## Detection Table

### A. Mapping anti-patterns

| ID | Anti-pattern | Detect | Severity | Correct form |
|---|---|---|---|---|
| JPA-001 | EAGER association (explicit or defaulted `@ManyToOne` / `@OneToOne`) | `FetchType.EAGER`, or `@ManyToOne` / `@OneToOne` with no `fetch =` | 🛑 | `@ManyToOne(fetch = FetchType.LAZY)` — always explicit |
| JPA-002 | `CascadeType.ALL` / `REMOVE` on the *many* side | `@ManyToOne(... cascade = CascadeType.ALL\|REMOVE)` | 🛑 | Cascade only from parent → child on `@OneToMany`; never child → parent |
| JPA-003 | Unidirectional `@OneToMany` without `mappedBy` | `@OneToMany` with no `mappedBy` and no `@JoinColumn` | 🛑 | `@OneToMany(mappedBy = "parent", fetch = LAZY, orphanRemoval = true)` |
| JPA-004 | `List` for `@ManyToMany` (delete-all + re-insert on every change) | `@ManyToMany` + `List<` | 🛑 | `Set<>` — or a real join **entity** when the link carries attributes |
| JPA-005 | `@Enumerated` (either `ORDINAL` or `STRING`) for a domain value | `@Enumerated`, `enum ` inside an entity package | 🛑 | `Long <field>Id` FK to a lookup table + lookup bean/model/service/Flyway migration (`holon-stack.md` Rule 3) |
| JPA-006 | `GenerationType.AUTO` / `TABLE` | `GenerationType.AUTO`, `GenerationType.TABLE` | 🛑 | `IDENTITY`, or `SEQUENCE` with an explicit `@SequenceGenerator` |
| JPA-007 | Sequence `allocationSize` mismatched with the DB `INCREMENT BY` | `@SequenceGenerator` without `allocationSize`, or `allocationSize` ≠ the Flyway `INCREMENT BY` | 🛑 | Keep both sides equal (e.g. `allocationSize = 50` ↔ `INCREMENT BY 50`) |
| JPA-008 | `LocalDateTime` for a stored timestamp | `private LocalDateTime` on an entity | 🛑 | `Instant` ↔ `TIMESTAMPTZ` (`holon-stack.md` timezone rules) |
| JPA-009 | Entity used as the API/DTO/UI carrier across a transaction boundary | entity returned from a view fetch callback while holding uninitialised associations | ⚠️ | Project to the bean fields actually needed via `BeanProjection` / `BeanPropertySet` subsets |
| JPA-010 | Bidirectional association without sync helpers | `@OneToMany` + `@ManyToOne` pair with no `addX()` / `removeX()` | ⚠️ | Add `addChild`/`removeChild` that set both sides |
| JPA-011 | Mutable collection field exposed by its getter | `return this.children;` on a mapped collection | ⚠️ | Return `Collections.unmodifiableCollection(...)`; mutate via sync helpers |
| JPA-012 | No optimistic locking | entity without `@Version` | 🛑 | `@Version private Long version;` (also required by the audit/version bean rules) |
| JPA-013 | `@Transient`-less derived/computed field | computed getter mapped as a column | ⚠️ | `@Transient` on the derived field, plus Holon `@Ignore` |

### B. Identity & Lombok anti-patterns

| ID | Anti-pattern | Detect | Severity | Correct form |
|---|---|---|---|---|
| JPA-020 | Lombok `@Data` / `@EqualsAndHashCode` / `@ToString` on an entity | `@Data`, `@EqualsAndHashCode`, `@ToString` in a file containing `@Entity` | 🛑 | `@Getter`/`@Setter` only; hand-write `equals`/`hashCode`; hand-write `toString()` over scalar fields only |
| JPA-021 | `equals`/`hashCode` derived from the generated `id` | `Objects.hash(id)`, `id.equals(other.id)` | 🛑 | Business key, or `hashCode()` returning a constant per class with `equals` on a natural/business key |
| JPA-022 | `toString()` touching a lazy association | association field referenced inside `toString()` | 🛑 | Scalar fields only |
| JPA-023 | Entity not `Serializable` while used in Vaadin session state | `@Entity` class bound into a view field, no `implements Serializable` | ⚠️ | `implements Serializable` with a `serialVersionUID` |

### C. Query & transaction anti-patterns

| ID | Anti-pattern | Detect | Severity | Correct form |
|---|---|---|---|---|
| JPA-030 | N+1 selects — association dereferenced inside a loop/stream over a result set | `for (…) { x.getChild().getY() }`, `.map(x -> x.getChild()…)` | 🛑 | One query with a join fetch / explicit projection, or a separate batched lookup keyed by FK |
| JPA-031 | `findAll()` inside a listing fetch callback | `.fetch(q -> svc.findAll())` | 🛑 | `svc.findSlice(q.getOffset(), q.getLength(), …)` |
| JPA-032 | Pagination combined with a collection join fetch (Hibernate paginates in memory — `HHH000104`) | `setFirstResult`/`Pageable` + `join fetch` of a collection | 🛑 | Two-step: page the ids, then fetch the collection for those ids |
| JPA-033 | Custom query methods on the repository | any method declaration in a `JpaRepository` interface | 🛑 | Move to the service using `BeanDatastoreHelper` / `Datastore` + the `*Model` property constants |
| JPA-034 | `EntityManager` / Spring Data used for queries when Holon `Datastore` can express them | `EntityManager`, `@Query`, `Specification` | 🛑 | `Datastore` / `BeanDatastoreHelper`; JPA only with a `// FALLBACK:` justification |
| JPA-035 | Lazy `Stream` consumed outside an active transaction | `stream(...)` result returned to a caller with no `@Transactional` | 🛑 | Caller annotated `@Transactional(readOnly = true)`; consume in try-with-resources |
| JPA-036 | Datastore/JPA stream not closed | `stream(...)` outside try-with-resources | 🛑 | `try (Stream<T> s = svc.stream(...)) { … }` |
| JPA-037 | Missing `readOnly = true` on read-only service/class | `@Transactional` with no `readOnly` on a query-only method | ⚠️ | `@Transactional(readOnly = true)` at class level; plain `@Transactional` on writes |
| JPA-038 | `@Transactional` on a Vaadin view / UI class | `@Transactional` in a file containing `@Route` | 🛑 | Transactions belong in the service layer |
| JPA-039 | `save()`/`merge()` called in a loop without batching | repository/helper `save` inside `for`/`forEach` | ⚠️ | Bulk operation, or batch with `hibernate.jdbc.batch_size` + periodic flush/clear |
| JPA-040 | `repository.save()` used for writes | `repository.save(`, `repository.delete(` | 🛑 | `BeanDatastoreHelper.save/insert/update/delete` |
| JPA-041 | Deleting a collection element-by-element via cascade instead of a bulk delete | `children.forEach(repo::delete)` | ⚠️ | `helper.bulkDelete(<filter>)` |

### D. Configuration anti-patterns

| ID | Anti-pattern | Detect | Severity | Correct form |
|---|---|---|---|---|
| JPA-050 | Open Session In View left enabled | `spring.jpa.open-in-view` absent or `true` | 🛑 | `spring.jpa.open-in-view=false` (explicit) |
| JPA-051 | Hibernate owns the schema | `spring.jpa.hibernate.ddl-auto` = `create`/`create-drop`/`update` | 🛑 | `validate` (or `none`) — Flyway owns the schema |
| JPA-052 | `hibernate.enable_lazy_load_no_trans=true` (masks N+1 / LazyInitializationException) | that property present | 🛑 | Remove it; fix the transaction boundary instead |
| JPA-053 | SQL logging left on in a non-dev profile | `spring.jpa.show-sql=true` outside `application-dev*` | ⚠️ | Use `logging.level.org.hibernate.SQL=DEBUG` in the dev profile only |
| JPA-054 | No JDBC batching configured while batch writes exist | `hibernate.jdbc.batch_size` absent | ⚠️ | Set `batch_size`, `order_inserts`, `order_updates` |

---

## Quick verification greps

Run these over the emitted diff (PowerShell: `Select-String`):

```sh
# 🛑 EAGER fetch, cascade misuse, enums, AUTO ids
git grep -nE 'FetchType\.EAGER|CascadeType\.(ALL|REMOVE)|@Enumerated|GenerationType\.(AUTO|TABLE)' src/

# 🛑 Lombok on entities
git grep -lE '@(Data|EqualsAndHashCode|ToString)' src/ | xargs git grep -l '@Entity'

# 🛑 Repository custom methods (any non-comment line inside the interface body)
git grep -nA100 'extends JpaRepository' src/

# 🛑 findAll() inside a fetch callback
git grep -nE '\.fetch\(.*findAll\(\)' src/

# 🛑 Spring Data / EntityManager queries
git grep -nE 'EntityManager|@Query|Specification<' src/

# 🛑 Config
git grep -nE 'open-in-view\s*[:=]\s*true|ddl-auto\s*[:=]\s*(create|create-drop|update)|enable_lazy_load_no_trans' src/main/resources/

# open-in-view must be present AND false
git grep -n 'open-in-view' src/main/resources/
```

Any hit that is not preceded by a `// JPA-WAIVER(<id>):` comment (⚠️ only) is a violation.

---

## Pre-Emit checklist fragment

Skills copy these lines into their own Pre-Emit Checklist:

- [ ] **JPA guardrail run** — emitted code scanned against [`../../rules/jpa-anti-patterns.md`](../../rules/jpa-anti-patterns.md); zero 🛑 findings; every ⚠️ carries a `// JPA-WAIVER(<id>):` comment
- [ ] All associations explicitly `FetchType.LAZY`; no `CascadeType.ALL`/`REMOVE` on `@ManyToOne`
- [ ] No Lombok `@Data`/`@EqualsAndHashCode`/`@ToString` on any `@Entity`; `toString()` touches scalars only
- [ ] No `@Enumerated` / Java enum for a domain value — lookup table FK instead
- [ ] Every entity has `@Version`; no `LocalDateTime` for stored timestamps
- [ ] Repositories have zero custom methods; all writes via `BeanDatastoreHelper`
- [ ] No association dereferenced inside a loop over a result set (N+1); no `findAll()` in a fetch callback
- [ ] Every lazy `Stream` is consumed inside `@Transactional(readOnly = true)` and closed with try-with-resources
- [ ] `spring.jpa.open-in-view=false` and `spring.jpa.hibernate.ddl-auto=validate` present explicitly
- [ ] JPA anti-pattern guardrail report printed in the final summary
