---
name: architecture-rules
description: >
  Turns the prose conventions of the Holon stack into ArchUnit rules that fail
  the build — the banned imports, constructor injection, the service as the
  transaction boundary, package-by-feature, and the test-layer naming contract.
  Use when the user asks to "enforce the architecture", "add ArchUnit rules",
  "make the Holon rules executable", "stop the agent using banned imports",
  "check the package structure", "add ArchitectureTest", or mentions ArchUnit,
  architecture tests, dependency rules, layer checks, or enforcing
  holon-stack.md automatically.
---

# Architecture Rules (ArchUnit)

## Prerequisites

| Required artifact | Created by |
|---|---|
| Production code under `src/main/java/` | `/implement` |
| A test tree and ArchUnit on the test classpath | `/traceability-sensors` (same dependency) |

If `src/main/java/` holds no class, **stop**: there is nothing to constrain yet.

## Why this exists

[`rules/holon-stack.md`](../../rules/holon-stack.md) is a ban list written in
prose, and the construction skills are told to honour it. Prose shapes behaviour
probabilistically; it does not stop a commit. One `@Autowired` field added at
11pm, one view that reaches past its service straight into the `Datastore`, and
the convention is gone — nothing errors, and review is the only thing standing
between the project and the drift.

ArchUnit makes the same rules executable. The payoff is not only enforcement:
**these rules bind every contributor and every tool**, not just an agent running
with this plugin installed. They travel with the clone, run in CI, and fail a
build that no AI assistant was involved in.

> **The dividing line, borrowed from the guards:** *if a rule can be checked by
> reading the repository, it is a test.* Only a rule about what happened during
> a session belongs in a hook (`/session-guards`). Prefer a rule here whenever
> the rule can be expressed as one.

## What this installs

Three classes under `src/test/java/<base-package>/architecture/`, plus one
annotation in the production tree:

| File | Lives in | Covers |
|---|---|---|
| `ArchitectureTest.java` | test tree | production code: banned imports, injection, transaction boundary, package-by-feature, naming, logging |
| `TestLayerConventionsTest.java` | test tree | the test tree itself, which `ArchitectureTest` deliberately excludes |
| `SourceConventionsTest.java` | test tree | what bytecode cannot see — SQL written as a string literal, and empty `@Fallback` reasons |
| `Fallback.java` | `<base-package>/shared/` | the annotation that records a deliberate, justified exception |

`SourceConventionsTest` is the odd one out: ArchUnit reads compiled classes, and
the compiler discards the difference between a SQL string and any other string.
That rule therefore scans `src/main/java` as text instead.

The three test classes carry the `sensor` tag (`@ArchTag("sensor")` on the
ArchUnit classes, `@Tag("sensor")` on the plain JUnit one), so they join the
three traceability sensors in the one cheap command:

```sh
mvn -q test -Dgroups=sensor
```

Source is in [references/rules.md](references/rules.md); the `@Fallback`
annotation that makes an exception visible is in
[references/fallbacks.md](references/fallbacks.md).

## The rules, and what each one stops

### Banned dependencies

Each row of the ban list in `holon-stack.md` becomes one `noClasses().should()
.dependOnClassesThat().resideInAnyPackage(...)` rule, with the reason in
`.because(...)` so the failure message tells the reader what to use instead.

| Rule | Stops |
|---|---|
| `noSpringSecurity` | Spring Security creeping in beside Holon Auth, leaving two half-configured security models |
| `noSpringMvcRest` | A REST controller appearing in a project where Vaadin *is* the UI layer |
| `noFieldInjection` | `@Autowired` on fields and setters — dependencies that do not appear in the constructor |
| `noHibernateValidatorConstraints` | Vendor constraints where Jakarta Bean Validation has an equivalent |
| `noVaadinI18nProvider` | A second localization mechanism beside Holon's `LocalizationContext` |
| `noDeprecatedThemeAnnotation` | `@Theme`, removed in Vaadin 25.2 |
| `noPlainSql` | SQL strings outside the `Datastore` — the thing the typed query API exists to prevent |
| `noConsoleOutput` | `System.out` / `System.err` instead of SLF4J |
| `noPersistenceBoxOutsideFallback` | the Holon persistence container used as the domain model instead of a JavaBean + `BeanPropertySet` |
| `noJpaOutsideFallback` | JPA and Spring Data drifting in where `Datastore` would do |

The last two are the ones with a legitimate exception, so they are written as
"…unless the class is annotated `@Fallback`" rather than as an absolute ban.

### Structure and boundaries

| Rule | Stops |
|---|---|
| `viewsDoNotTouchTheDatastore` | a view reaching past its service — the transaction boundary leaking into the UI |
| `onlyServicesAreTransactional` | `@Transactional` on a view or a bean, where it silently does nothing useful |
| `noLayerPackages` | `domain/`, `service/`, `ui/`, `repository/` packages reappearing under a package-by-feature layout |
| `servicesAreNamedService`, `viewsAreNamedView`, `modelsAreNamedModel` | the naming conventions the other skills and these very rules rely on |
| `routesAreViews` | a `@Route` on something that is not a view |
| `beansDoNotDependOnViews` | the domain depending on the UI |

### The test layer

`TestLayerConventionsTest` exists because `ArchitectureTest` excludes the test
tree, so a convention governing tests needs its own home. It asserts the two
dangerous directions only:

- a browserless test must **not** be named `*IT` — Surefire would skip it;
- a Playwright test must **not** be named `*Test` — it would start a browser
  inside the unit-test phase.

A misnamed test is the quietest failure a project has: nothing errors, the wrong
Maven plugin simply never picks the class up, and it reports as passing **by
never running at all**. That is worth a rule rather than a paragraph.

> Note it does *not* assert "every `*Test` extends something" — plenty of test
> classes are neither view tests nor journeys (the sensors themselves), so that
> rule would be false.

## The `@Fallback` escape

`holon-stack.md` allows raw Vaadin, JPA or Spring Data when Holon has no
equivalent, justified inline with a `// FALLBACK:` comment. ArchUnit cannot read
comments, so the skill installs a test-visible marker annotation that says the
same thing in a form a rule can check:

```java
@Fallback(reason = "BeanDatastoreHelper cannot express the recursive CTE this report needs")
class AccountHierarchyQuery { … }
```

Rules that have a legitimate exception are written to skip annotated classes.
This makes every exception **greppable, reviewable and countable** — the
opposite of an exclusion list buried in a build file, which is why
`/traceability-sensors` has none and this skill has exactly one.

`reason()` is mandatory and must name what Holon could not do. A `@Fallback`
with a vague reason is a review finding; the rule cannot judge prose, so this is
the one place where a human still has to look.

## Adoption: report first, then enforce

Installing ten rules into an existing codebase produces a wall of failures and
the instinctive response is to delete the rules. Don't do that.

1. Install both classes with **every rule present**.
2. Run `mvn -q test -Dgroups=sensor` and read the output as a baseline.
3. For each failing rule, choose **once**, and record the choice:
   - *fix the code* — the default, and usually small;
   - *mark the genuine exception* with `@Fallback(reason = "…")`;
   - *freeze the rule* with `.allowEmptyShould(true)` and a `@Disabled`-free
     documented exemption list naming the existing offenders — and an issue to
     burn it down.
4. Never resolve a failure by **deleting the rule**. A deleted rule is a
   convention that silently stopped applying; a frozen one with a named list is
   a debt that is visible.

An exemption list naming classes is acceptable **only** for pre-existing code.
A class added after adoption must satisfy the rule or carry a `@Fallback`.

## Workflow

1. Determine the base package from `src/main/java/` and confirm ArchUnit is on
   the test classpath    (`/traceability-sensors` adds it; see
   `../traceability-sensors/references/build-wiring.md`).
2. Create `src/test/java/<base>/architecture/` and write `ArchitectureTest`,
   `TestLayerConventionsTest`, `SourceConventionsTest` and `Fallback` from
   `references/rules.md`, fixing the `package` line and `BASE_PACKAGE`.
3. If the project's Playwright tests still use the old `*E2ETest` naming, rename
   them to `UC<NNN><Name>IT` / `TC<NNN><Name>IT` first — otherwise the
   test-layer rules fail on every one of them at once and bury the real
   findings.
4. Run `mvn -q test -Dgroups=sensor`.
5. Triage the baseline as described above; report the three buckets (fixed,
   `@Fallback`, frozen) to the user explicitly, with counts.
6. Where a rule and `holon-stack.md` disagree, **the rule is wrong until the
   document is changed** — update both in the same change, never only the rule.

## Constraints

- **Allowed:** `com.tngtech.archunit:archunit-junit5` (test scope), JUnit 5.
- **Banned:** any dependency beyond ArchUnit; any rule that needs a Spring
  context, a database or Docker. These must stay in the seconds-long `sensor`
  group, or the guard that asks for them every turn will be ignored.
- **One rule, one convention, one `.because(...)`.** The reason string is the
  error message a developer sees at 2am; write it as advice ("use
  `BeanDatastoreHelper`"), not as a restatement of the rule.
- **Every rule must cite its source** in a comment: the section of
  `holon-stack.md`, or the ADR it enforces. A rule nobody can trace to a
  decision gets deleted the first time it is inconvenient.
- **`allowEmptyShould(true)` only where a rule legitimately matches nothing
  yet** — otherwise a typo'd package name produces a green rule that checks
  nothing.
- **Never weaken a rule to make a build pass.** Fix the code, mark the
  exception, or freeze it with a named list.
- **No `@Disabled` on an architecture rule.** A disabled rule looks enforced
  and is not, which is worse than an absent one.

## DO NOT

- Delete a rule to turn a build green
- Add `@Fallback` without a reason naming what Holon could not do
- Put architecture rules in the same class as the traceability sensors
- Let these rules analyse the test tree (that is `TestLayerConventionsTest`)
- Enforce a convention here that `holon-stack.md` does not state — change the
  document first
- Introduce a rule that needs Spring, a database or Docker to decide
