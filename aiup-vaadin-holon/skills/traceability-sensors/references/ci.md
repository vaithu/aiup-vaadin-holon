# CI — the backstop

The fourth layer of the harness. Guides persuade, sensors verify, guards insist
— but all three stop at the edge of a Claude Code session. CI is what binds
everyone else: a colleague on a laptop, a Dependabot bump, a commit made from
the GitHub web UI.

Install this once per project, after `/traceability-sensors` and
`/architecture-rules`. Two files, both under `.github/workflows/`.

## What CI must add over the guards

Nothing about the *sensors* changes in CI — they are the same classes. What
changes is everything the guards deliberately leave out, because it is too slow
to run every turn:

| | Guards (per turn) | CI (per push) |
|---|---|---|
| Sensors | yes, every turn | yes |
| Unit tests | no | yes |
| `*IT` — Testcontainers, Playwright | no | yes |
| Quality gate (`-Pquality`) | no | yes |
| SAST (CodeQL) | no | yes |

The guards are a fast inner loop; CI is the slow outer one. Do not try to move
work from the right column to the left — a sensor group that needs Docker stops
being cheap enough to run every turn, and a guard too expensive to obey gets
skipped. That trade-off is the whole reason the `sensor` tag exists.

## `java-quality.yml`

Ordered cheapest-first so a specification/code mismatch fails in ~30 seconds
rather than after a full Playwright run. Each stage is a separate step so the
GitHub UI names the layer that broke.

```yaml
name: Java quality

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

concurrency:
  group: java-quality-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  verify:
    runs-on: ubuntu-latest
    timeout-minutes: 45

    steps:
      - uses: actions/checkout@v4
        with:
          # The traceability sensors read docs/ from the working tree, not from
          # the index. A shallow checkout is fine; a sparse one is not.
          fetch-depth: 1

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven

      # Layer 2, cheap half. No Docker, no Spring context: seconds.
      # Runs first so that "the docs and the code disagree" is reported
      # before anything slow has started.
      - name: Sensors
        run: mvn -q -B test -Dgroups=sensor

      # Everything that is not a sensor and not an *IT.
      - name: Unit tests
        run: mvn -B test -DexcludedGroups=sensor

      # Failsafe: Testcontainers + Playwright. The expensive half.
      - name: Integration tests and quality gate
        run: mvn -B -Pquality verify -DexcludedGroups=sensor

      - name: Publish test report
        uses: mikepenz/action-junit-report@v4
        if: always()
        with:
          report_paths: '**/target/*-reports/TEST-*.xml'
          detailed_summary: true

      # Report-mode findings are worthless if nobody can see them.
      - name: Upload quality reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: quality-reports
          retention-days: 7
          path: |
            **/target/spotbugs*.xml
            **/target/pmd.xml
            **/target/cpd.xml
            **/target/checkstyle-result.xml
            **/target/site/jacoco/
            **/target/dependency-check-report.*
            **/target/e2e-snapshots-actual/
          if-no-files-found: ignore
```

Four things in there are deliberate:

- **`-Dgroups=sensor` then `-DexcludedGroups=sensor`.** Without the exclusion
  the sensors run three times, and a sensor failure is reported against the
  slowest step instead of the fastest one.
- **`if: always()` on both report steps.** The run you most need the artifacts
  from is the one that failed.
- **`e2e-snapshots-actual/`** is uploaded so a visual-regression failure can be
  inspected without reproducing it locally. See `/playwright-test`.
- **`concurrency` with `cancel-in-progress`.** Playwright and Testcontainers
  runs are long; without this a busy branch queues them.

## `codeql.yml`

`rules/quality-gate.md` lists CodeQL as the SAST half of the gate — FindSecBugs
catches the Java-level patterns, CodeQL the dataflow ones. It is a separate
workflow because it needs `security-events: write` and runs on a schedule, and
because a CodeQL outage should not block a PR that the quality gate passed.

```yaml
name: CodeQL

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    # Catches newly published rules against unchanged code.
    - cron: '0 3 * * 1'

permissions:
  contents: read
  security-events: write

jobs:
  analyze:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven

      - uses: github/codeql-action/init@v3
        with:
          languages: java-kotlin
          queries: security-and-quality

      # Compile only. Tests add minutes and teach CodeQL nothing.
      - name: Build
        run: mvn -q -B -DskipTests package

      - uses: github/codeql-action/analyze@v3
```

## Making the gate hard

`rules/quality-gate.md` ships the quality profile in **report mode** so that
adopting it is not blocked by a flood of pre-existing findings. Once the
baseline is triaged, enforce it in CI *before* enforcing it locally — CI has the
full picture and nobody can skip it:

```yaml
      - name: Integration tests and quality gate
        run: >
          mvn -B -Pquality verify -DexcludedGroups=sensor
          -Dspotbugs.failOnError=true
          -Dpmd.failOnViolation=true
          -Dcheckstyle.failOnViolation=true
          -Djacoco.haltOnFailure=true -Djacoco.line.minimum=0.70
          -Ddependency.check.failBuildOnCVSS=7
```

Flip them one at a time. A step that turns red for six unrelated reasons at once
gets disabled rather than fixed.

## What CI must not do

- **Do not add a workflow step that edits the repository** — no auto-formatting
  commit, no "update the docs" bot. The sensors assert that `docs/` and `src/`
  agree; a job that silently rewrites one of them turns a real failure into a
  green run and destroys the signal.
- **Do not let CI write a `Status:` line.** Raising a status is a claim about
  coverage that only `/coverage-check` is allowed to support.
- **Do not skip the sensors on a documentation-only change.** A path filter that
  skips CI for `docs/**` is exactly backwards: a docs-only edit is the single
  most likely way to break traceability, because renaming a flow heading
  invalidates every annotation naming it.

That last one is worth stating plainly, because the path filter is such a common
reflex:

```yaml
# WRONG — a renamed flow heading now reaches main unchecked.
on:
  pull_request:
    paths-ignore: ['docs/**', '**.md']
```

## Verification

After adding both files:

```sh
# 1. The workflows parse.
python3 -c "import yaml,glob; [yaml.safe_load(open(f)) for f in glob.glob('.github/workflows/*.yml')]"

# 2. The sensor step passes locally, exactly as CI runs it.
mvn -q -B test -Dgroups=sensor

# 3. The exclusion is real: this must NOT run the sensor classes.
mvn -B test -DexcludedGroups=sensor | grep -c 'TraceabilityTest'   # → 0

# 4. Push a branch that renames a flow heading in a UC marked Done
#    without touching its test. CI must fail at the "Sensors" step.
```

Step 4 is the one that matters. A backstop nobody has ever seen fail is a
backstop nobody knows is wired up.
