# Quality Gate Guardrail

A **SonarQube-equivalent quality gate** that every test skill (`/datastore-test`,
`/holon-vaadin-test`, `/playwright-test`) must ensure is present **before it starts
generating or running tests**. It replicates SonarQube's value using only
free/open-source tooling — **no SonarQube or SonarCloud account, server, or license
is required.**

The reference implementation lives in the demo module:
[`demo/crm-minimal`](../../demo/crm-minimal) — see its `pom.xml` `quality` profile,
its [`config/`](../../demo/crm-minimal/config) rulesets, and the
[`java-quality.yml`](../../.github/workflows/java-quality.yml) /
[`codeql.yml`](../../.github/workflows/codeql.yml) workflows.

## Why this is a testing guardrail

Tests answer *"does it work?"*. This gate answers *"is it safe and clean?"* — bugs,
security vulnerabilities, code smells, duplication, coverage, and vulnerable
dependencies. Wiring it into the test skills means the checks run **as part of the
same `verify` phase as the tests**, so quality regressions are caught the moment
testing starts rather than in a later review.

## What the gate is made of

| SonarQube capability | Free replacement | Maven plugin |
|----------------------|------------------|--------------|
| Bugs / correctness (Java) | **SpotBugs** | `spotbugs-maven-plugin` |
| Security vulnerabilities / hotspots (SAST) | **FindSecBugs** (+ **CodeQL** in CI) | `findsecbugs-plugin` |
| Code smells / conventions | **PMD**, **Checkstyle** | `maven-pmd-plugin`, `maven-checkstyle-plugin` |
| Duplicated code | **PMD CPD** | `maven-pmd-plugin` (`cpd-check`) |
| Test coverage | **JaCoCo** | `jacoco-maven-plugin` |
| Vulnerable dependencies (CVEs) | **OWASP Dependency-Check** | `dependency-check-maven` |

All of these are bound to the `verify` phase inside a Maven **`quality` profile**, so
a normal `mvn package` stays fast and the gate is opt-in.

## Guardrail behaviour for a test skill

When a test skill runs, before emitting the test summary it must:

1. **Check** whether the target project's `pom.xml` declares a `quality` profile
   containing the six plugins above, plus the `config/` rulesets
   (`checkstyle.xml`, `pmd-ruleset.xml`, `spotbugs-exclude.xml`).
2. **If missing** — scaffold them by copying the reference implementation from
   [`demo/crm-minimal`](../../demo/crm-minimal) (the `quality` profile block and the
   three `config/` files), adjusting package/source paths to the target project.
   Do **not** invent a bespoke gate; reuse the reference so every project matches.
3. **Run the gate together with the tests:**

   ```sh
   mvn -Pquality verify
   ```

4. **Report** the outcome alongside the test results: bug/smell/duplication counts,
   coverage %, and any CVE findings. Point the user at the generated reports under
   `target/` (`spotbugs*.xml/html`, `pmd.xml`, `cpd.xml`, `checkstyle-result.xml`,
   `site/jacoco/`, `dependency-check-report.*`).

## Report mode vs. enforcing (fail the build)

The gate ships in **report mode** (non-failing) so adopting it is not blocked by a
flood of pre-existing findings. The toggles are properties in the `quality` profile:

| Property | Report-mode default | Enforce by setting |
|----------|--------------------|--------------------|
| `spotbugs.failOnError` | `false` | `true` |
| `pmd.failOnViolation` | `false` | `true` |
| `checkstyle.failOnViolation` | `false` | `true` |
| `jacoco.haltOnFailure` / `jacoco.line.minimum` | `false` / `0.00` | `true` / e.g. `0.70` |
| `dependency.check.failBuildOnCVSS` | `11` (never) | e.g. `7` |

**Recommended progression:** run in report mode, triage the baseline, tune the
rulesets for the Holon/Vaadin idioms, then flip the toggles to make the gate hard.

## Constraints

- **Reuse the reference gate** from `demo/crm-minimal` — do not hand-roll a different
  set of plugins or rulesets per project.
- **No SonarQube/SonarCloud dependency** — the gate must run with only the OSS Maven
  plugins and (in CI) GitHub CodeQL + Code Scanning.
- **Non-failing by default** — never introduce the gate in enforcing mode on a project
  with an un-triaged baseline; start in report mode.
- **Tune, don't disable** — suppress framework false positives via the `config/`
  rulesets/exclude filter, not by dropping whole tools.
