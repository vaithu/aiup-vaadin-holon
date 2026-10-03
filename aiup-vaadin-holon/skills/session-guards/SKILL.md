---
name: session-guards
description: >
  Installs the Claude Code hooks that make the harness hold during a session —
  refusing to end a turn while changed code or specs are unverified, and
  refusing a Status line that claims Done without a coverage audit behind it.
  Use when the user asks to "install the hooks", "add session guards", "stop
  the agent claiming done", "enforce running the sensors", "guard the Status
  line", "set up .claude/settings.json", or mentions Claude Code hooks,
  PreToolUse, Stop hooks, SubagentStop, or stopping an agent from marking work
  complete without evidence.
---

# Session Guards

## Prerequisites

| Required artifact | Created by |
|---|---|
| The six sensor classes tagged `sensor` | `/traceability-sensors`, `/architecture-rules` |
| The `uc-coverage` agent | ships with this plugin, driven by `/coverage-check` |
| `bash`, `git` and `jq` on `PATH` | see *Requirements* below |

Install these **last**. A guard that demands a sensor run before the sensors
exist blocks every turn and teaches the user to delete the hooks.

## Requirements

The hooks are bash. On Windows they run under **Git Bash**, which ships with
Git for Windows — Claude Code already uses it for the `Bash` tool, so no new
install is needed there. `jq` is a separate download and **is** needed:

```sh
bash --version
git --version
jq --version
```

If `jq` is missing, every hook that parses its input exits `0` and the guards
silently do nothing. They fail open by design, which means a missing
dependency is invisible until you run the smoke test. Run it.

## Why this exists

Three of the four layers of this harness live in the repository: the written
guides, the sensors in `src/test/java`, the CI backstop. All three share a
weakness — they are *post hoc*. A sensor catches drift only once somebody runs
it, and the one actor most likely not to run it is an agent that has just
finished a long task and believes it is done.

"The agent ran the sensors before it claimed to be finished" is not a fact
about the source tree. No test can assert it, because by the time a test runs,
it has already happened. It is a fact about the **session**, and hooks are the
only place a fact about a session can be checked.

That gives a clean dividing line, and it is worth stating plainly because it
decides where every future rule goes:

> **If a rule can be checked by reading the repository, it is a test.
> Only a rule about what happened during a session is a hook.**

Hooks are the least portable, least testable and most intrusive layer. Keep
them small, and keep anything that could have been a test out of them.

## What this installs

Seven scripts in `.claude/hooks/` plus `.claude/settings.json`:

| File | Event | Does |
|---|---|---|
| `lib.sh` | — | shared state handling; sourced by all of them |
| `session-start.sh` | `SessionStart` | records HEAD, clears the previous session's markers |
| `record-sensor-run.sh` | `PostToolUse(Bash)` | notes a Maven run whose Surefire reports are fresh and green |
| `require-sensors.sh` | `Stop` | refuses to end the turn while changed code, docs or hooks are unverified |
| `guard-spec-status.sh` | `PreToolUse(Edit\|Write)` | refuses an asserting `Status:` with no audit behind it |
| `check-spec-status.sh` | `PostToolUse(*)` | catches the same change made by `sed`, a script, or any other tool |
| `record-coverage-check.sh` | `SubagentStop` | notes that `uc-coverage` audited a given id |
| `smoke.sh` | — | the hooks' own test; run it after changing any of them |

Source is in [references/hooks.md](references/hooks.md); the smoke test and
what it covers is in [references/smoke.md](references/smoke.md).

## The two principles that make them work

### Evidence is positive, never the absence of failure

The obvious way to know the sensors ran is to look at the console output of
the `Bash` call. That is wrong in a way that matters: a command piped through
`tail`, run in the background, narrowed with `-Dtest=…`, or skipped entirely
all produce output with no failure text in it. "No failure appeared" is not
"it passed".

So the evidence is the **Surefire XML report** each sensor class writes —
`target/surefire-reports/TEST-*.<SensorName>.xml`. Written by the test JVM
itself, one per class, with `tests`, `failures` and `errors` as attributes.
The guard requires, for every sensor:

- a report exists;
- it is newer than the session start **and** newer than every changed file;
- `tests` is not `0`;
- `failures` and `errors` are both `0`.

A narrowed run leaves the other reports stale. A skipped run writes none. A
failed one says so in the file. A background run has not written them yet when
the hook looks. None of these can be talked around, because none of them is
about what the console said.

### A guard reads repository state, not the shape of a tool call

`guard-spec-status.sh` has to inspect the `Edit` payload, because it refuses
*before* the write happens. That makes it defeatable: a `sed -i`, a heredoc,
or a tool that does not exist yet goes straight past it.

So it is paired with `check-spec-status.sh`, which runs after **every** tool,
ignores its input entirely, and reads the specification files off disk. It
keeps a snapshot of every `Status:` line and reports any that now asserts
`Done`/`Tested`/`Automated` without an audit. The pre-hook is a convenience —
it refuses cleanly and explains why. The post-hook is the actual guarantee.

Where you can only have one, make it the one that reads state.

## The status contract

`**Status:** Done` is an assertion, not a label. It switches a coverage sensor
on. That makes setting it by hand either a build break, or — far worse — a
certification of coverage nobody checked.

The guards enforce this sequence:

1. `/coverage-check UC-007` — the read-only audit runs and finishes.
2. `record-coverage-check.sh` notes the id on `SubagentStop`.
3. The `Status:` edit is now allowed.
4. `require-sensors.sh` holds the turn until the sensors run and the one that
   just switched on actually votes.

A downgrade — `Done` → `Draft` — is always allowed, with no audit. Admitting
something is not finished needs no evidence; claiming it is does.

An audit goes stale: `aiup_coverage_checked` requires the marker to be newer
than everything under `src/` that changed in the session. Auditing a use case,
then writing more code, then setting `Done` does not pass.

## Workflow

**1. Copy the files.**

```sh
mkdir -p .claude/hooks
# copy the eight files from references/
chmod +x .claude/hooks/*.sh
```

If `.claude/settings.json` already exists, merge the `hooks` block into it.
Do not overwrite — `permissions` is the project's own.

**2. Check `AIUP_SENSORS` in `lib.sh`** names exactly the classes tagged
`sensor` in this project. This is the one line that must be edited per
project, and getting it wrong is the only way these hooks become unsatisfiable.

**3. Run the smoke test.**

```sh
./.claude/hooks/smoke.sh
```

Expect `all hook checks passed`. If `jq` or `stat` is missing it will fail
here — which is the whole reason to run it before trusting the guards.

**4. Make sure the sensors are green first.**

```sh
mvn -q test -Dgroups=sensor
```

Installing guards on a red build means the first turn after installation is
blocked, and the natural reaction is to blame the guards.

**5. Commit, then start a new session.** `SessionStart` only fires on
`startup` and `clear`, so the guards are inert until then.

## Living with them

**A block is information, not an obstacle.** The message names what changed
and what to run. Run it.

**They block once per turn.** The `Stop` hook checks `stop_hook_active` and
lets the second entry through, so a project that genuinely cannot build does
not loop forever. The guard is a reminder with teeth, not a cage.

**A commit does not clear them.** `session-start.sh` records the base commit,
and the change set is computed against *that*, not against `HEAD`. Committing
unverified work and then finishing the turn does not work.

**They follow a worktree.** State lives in `git rev-parse --absolute-git-dir`,
so a worktree keeps its own markers rather than sharing the parent's.

**They cost a few `git` calls per tool use.** `aiup_changed_files` runs
`git status --porcelain` and a `git diff --name-only` on every invocation, and
`check-spec-status.sh` fires after *every* tool. On a normal checkout that is
tens of milliseconds. On Windows with real-time antivirus scanning the working
tree it can be seconds, and `smoke.sh` — which makes roughly sixty hook
invocations — can take half an hour rather than the few seconds it takes on
Linux. If the session feels sluggish after installing these, measure git
first:

```sh
time git status --porcelain
```

An exclusion for the repository in the antivirus scanner, or
`git config core.fsmonitor true` (git ≥ 2.37), fixes it. The hooks themselves
have no fat to trim — they are already doing the minimum.

**To disable them temporarily**, rename `.claude/settings.json`. Do not delete
individual hooks: the pairs (`guard`/`check`, `record`/`require`) only work
together, and removing one half leaves a guard that cannot be satisfied.

## Constraints

**Allowed**

- `bash`, `git`, `jq`, `awk`, `sed`, `grep`, `comm`, `stat` — POSIX tools and
  the one JSON parser.
- Reading `target/surefire-reports/*.xml` as evidence.
- Writing markers into the git directory (`aiup-*`), which is not tracked.
- Exit `2` with a message on stderr to refuse.

**Banned**

- Any hook that could have been a test. If the rule can be checked by reading
  the repository, it belongs in `src/test/java`.
- Running Maven, Docker, or anything slow from a hook. They fire on every tool
  call; the budget is milliseconds.
- Writing anything into the working tree. Markers go in the git directory, so
  they never show up in `git status` and never enter a commit.
- Trusting console output as evidence of a passing run.
- Exiting non-zero for any reason other than a deliberate refusal. A hook that
  crashes takes the turn with it.
- Hard-coding the project's base package — the report lookup matches on the
  class's simple name.

## DO NOT

- **Do not install these before the sensors exist.** The guard will demand a
  run of classes that are not there, every turn, with no way out.
- **Do not add a rule here because it was easier than writing a test.** Hooks
  are invisible to CI, invisible to anyone not using Claude Code, and tested
  only by a shell script. A test is better in every way except convenience.
- **Do not make a hook exit non-zero on an unexpected payload.** Fail open.
  The cost of a guard that occasionally misses is small; the cost of one that
  breaks every session is that it gets deleted.
- **Do not skip `smoke.sh` after editing a hook.** `require-sensors.sh` will
  block the turn until you run it, which is deliberate — the hooks guard the
  code, and the smoke test is the only thing guarding the hooks.
- **Do not weaken the evidence test to make a workflow convenient.** If
  running the sensors in the background were accepted, the guard would be
  satisfied by a run that had not finished, and the whole layer would be
  theatre.
