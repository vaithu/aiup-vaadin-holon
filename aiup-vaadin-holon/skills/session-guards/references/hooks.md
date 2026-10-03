# The hook scripts

Eight files. Seven live in `.claude/hooks/`; the eighth is
`.claude/settings.json`, which wires them to events.

`chmod +x .claude/hooks/*.sh` after copying. On Windows, Git sets the
executable bit through `core.fileMode` — if the hooks do not fire, check that
`git ls-files -s .claude/hooks` shows mode `100755`.

Every hook follows two rules, and breaking either one is how a harness turns
into an obstacle:

1. **Fail open.** A hook that cannot answer exits `0`. A `jq` that chokes, a
   missing git dir, an unexpected payload — none of these may take the
   session down. The only non-zero exits are deliberate refusals.
2. **Read the repository, not the tool call.** A guard that inspects the
   shape of an `Edit` is defeated by a `sed`. Where a guard must look at a
   tool call (to refuse *before* the write), a second hook reads the files
   afterwards and catches whatever went around it.

---

## `lib.sh`

```bash
#!/usr/bin/env bash
#
# Shared by the hooks in this directory. Nothing here may take a session down:
# a function that cannot answer returns non-zero, and the caller exits 0.
#
# The hooks keep their state in the git directory, so they follow a worktree
# rather than the shared .git of its parent:
#
#   aiup-session-base       the commit the session started on; its mtime is
#                           when the session started (session-start.sh)
#   aiup-sensors-ran        the change set the last green sensor run saw
#                           (record-sensor-run.sh); its mtime is when
#   aiup-smoke-ran          the hook change set the last green smoke.sh saw
#   aiup-spec-status        every UC/TC Status: line as last seen
#                           (check-spec-status.sh)
#   aiup-coverage-checked/  one file per UC/TC id whose coverage was audited
#                           in this session (record-coverage-check.sh)
#
# Two principles:
#   - a guard reads the state of the repository, never the shape of a tool
#     call, so it holds whichever tool made the change;
#   - evidence is positive — a Surefire report, an audit that finished — and
#     never the absence of failure text on a console.

export LC_ALL=C

aiup_root() {
    printf '%s' "${CLAUDE_PROJECT_DIR:-$(pwd)}"
}

aiup_git_dir() {
    git -C "$(aiup_root)" rev-parse --absolute-git-dir 2>/dev/null
}

# _aiup_changed <gitdir> <paths...>: every path under <paths> that differs
# from what the session started on: the working tree and the index against
# HEAD, plus whatever was committed since the session's base commit — a commit
# must not clear a guard. One path per line, sorted, unique, relative to the
# repository root. A path that no longer exists is suffixed "(deleted)", so a
# file removed after the last run differs from the one that run saw; whether
# it is staged is not a change.
_aiup_changed() {
    local gitdir="$1" root base
    shift
    root=$(aiup_root)
    {
        git -C "$root" -c core.quotePath=false status --porcelain --untracked-files=all -- "$@" 2>/dev/null \
            | sed 's/^...//; s/^.* -> //'
        if [ -f "$gitdir/aiup-session-base" ]; then
            base=$(cat "$gitdir/aiup-session-base")
            if git -C "$root" cat-file -e "$base^{commit}" 2>/dev/null; then
                git -C "$root" -c core.quotePath=false diff --name-only "$base" HEAD -- "$@" 2>/dev/null
            fi
        fi
    } | sed '/^$/d' | sort -u | while IFS= read -r path; do
        if [ -e "$root/$path" ]; then printf '%s\n' "$path"; else printf '%s (deleted)\n' "$path"; fi
    done
}

# What the sensors judge: code, specifications, and the build that runs them.
aiup_changed_files() {
    _aiup_changed "$1" src docs pom.xml
}

# What smoke.sh judges.
aiup_changed_hooks() {
    _aiup_changed "$1" .claude/hooks .claude/settings.json
}

# Fractional seconds where the platform offers them: an edit and the build that
# follows it can easily land in the same whole second. 0 for a missing file.
# GNU stat first: BSD stat rejects -c without printing anything, whereas GNU
# stat reads -f as "file system status" and prints that before failing.
aiup_mtime() {
    stat -c %.9Y "$1" 2>/dev/null || stat -f %Fm "$1" 2>/dev/null || echo 0
}

aiup_newer() {
    awk -v a="$1" -v b="$2" 'BEGIN { exit !(a > b) }'
}

# Newest mtime among the paths on stdin (relative to the root) that still
# exist; 0 when none does.
aiup_newest_mtime() {
    local root newest=0 path m
    root=$(aiup_root)
    while IFS= read -r path; do
        [ -f "$root/$path" ] || continue
        m=$(aiup_mtime "$root/$path")
        if aiup_newer "$m" "$newest"; then newest=$m; fi
    done
    printf '%s' "$newest"
}

# aiup_unverified <marker> <changed> <subject>: why the run recorded in
# <marker> does not cover <changed>. Prints nothing when it does.
aiup_unverified() {
    local marker="$1" changed="$2" subject="$3" unseen stamp file
    if [ ! -f "$marker" ]; then
        echo "$subject has not run in this session."
        return 0
    fi
    # Changes the last run did not see: added, deleted, or committed since.
    unseen=$(comm -23 <(printf '%s\n' "$changed") <(sort -u "$marker") || true)
    if [ -n "$unseen" ]; then
        printf 'these changes happened after the last run:\n%s\n' "$(sed 's/^/    /' <<<"$unseen")"
        return 0
    fi
    # Files the run did see, edited again since.
    stamp=$(aiup_mtime "$marker")
    while IFS= read -r file; do
        [ -f "$(aiup_root)/$file" ] || continue
        if aiup_newer "$(aiup_mtime "$(aiup_root)/$file")" "$stamp"; then
            echo "$file was changed after the last run."
            return 0
        fi
    done <<<"$changed"
}

# The classes whose Surefire reports prove that the sensors ran. All carry the
# JUnit tag "sensor", so `mvn test -Dgroups=sensor` runs exactly these.
AIUP_SENSORS="ArchitectureTest TestLayerConventionsTest SourceConventionsTest UseCaseTraceabilityTest TestCaseTraceabilityTest BusinessRuleTraceabilityTest"

# aiup_sensor_reports_ok <since>: every sensor's Surefire report exists, is
# newer than <since>, ran at least one test, and had no failure or error.
# Prints why not, and returns 1, otherwise. The report is the evidence: it is
# written by the test JVM itself, per class, whatever the console showed.
#
# The report is matched by simple name, not by a hard-coded package, so the
# sensors may live in <base>.traceability and <base>.architecture without this
# file knowing the project's base package.
aiup_sensor_reports_ok() {
    local since="$1" dir name report head tests failures errors
    dir="$(aiup_root)/target/surefire-reports"
    for name in $AIUP_SENSORS; do
        report=$(ls -1 "$dir"/TEST-*."$name".xml 2>/dev/null | head -1)
        if [ -z "$report" ]; then
            echo "there is no Surefire report for $name"
            return 1
        fi
        if ! aiup_newer "$(aiup_mtime "$report")" "$since"; then
            echo "the Surefire report for $name predates the change"
            return 1
        fi
        head=$(grep -m1 -o '<testsuite[^>]*' "$report" || true)
        tests=$(sed -nE 's/.*[[:space:]]tests="([0-9]+)".*/\1/p' <<<"$head")
        failures=$(sed -nE 's/.*[[:space:]]failures="([0-9]+)".*/\1/p' <<<"$head")
        errors=$(sed -nE 's/.*[[:space:]]errors="([0-9]+)".*/\1/p' <<<"$head")
        if [ -z "$tests" ] || [ "$tests" = 0 ]; then
            echo "the Surefire report for $name shows no test"
            return 1
        fi
        if [ "${failures:-1}" != 0 ] || [ "${errors:-1}" != 0 ]; then
            echo "$name failed"
            return 1
        fi
    done
    return 0
}

# The value after "Status:**" on a line, trailing blanks removed. Tolerates a
# line fragment that starts mid-way, as an Edit's old_string may.
aiup_status_value() {
    sed -E 's/.*Status:\*\*[[:space:]]*//; s/[[:space:]]+$//' <<<"$1"
}

# A status that switches a traceability sensor on: Done / Tested on a use
# case, Automated on a test case.
aiup_asserting_status() {
    case "$1" in
        Done | Tested | Automated) return 0 ;;
        *) return 1 ;;
    esac
}

# UC-NNN / TC-NNN from a specification's path.
aiup_spec_id() {
    basename "$1" | grep -oE '^(UC|TC)-[0-9]{3}'
}

# Every UC/TC specification in the working tree and the value of its Status:
# line, "path<TAB>value" per line, sorted.
aiup_status_lines() {
    local path value
    (
        cd "$(aiup_root)" || exit 0
        for path in docs/use_cases/UC-*.md docs/test_cases/TC-*.md; do
            [ -f "$path" ] || continue
            value=$(aiup_status_value "$(grep -m1 'Status:\*\*' "$path" || true)")
            printf '%s\t%s\n' "$path" "$value"
        done
    ) | sort
}

# aiup_coverage_checked <gitdir> <id>: a coverage audit of <id> finished in
# this session, and nothing under src/ — the code and tests it judged — has
# changed since.
aiup_coverage_checked() {
    local gitdir="$1" id="$2" marker newest
    marker="$gitdir/aiup-coverage-checked/$id"
    [ -f "$marker" ] || return 1
    newest=$(aiup_changed_files "$gitdir" | grep '^src/' | aiup_newest_mtime)
    aiup_newer "$(aiup_mtime "$marker")" "$newest"
}
```

---

## `session-start.sh`

```bash
#!/usr/bin/env bash
#
# SessionStart — remember where the session began.
#
# require-sensors.sh diffs the working tree against HEAD, which a commit would
# silently clear. Recording HEAD here lets it also diff HEAD against this
# commit, so "changed in this session" survives a `git commit`. The file's
# mtime is when the session started: a Surefire report older than that was
# written by another session and proves nothing about this one — so the
# markers of the previous session go, and the Status: lines are read once so
# that check-spec-status.sh has something to compare against.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

gitdir=$(aiup_git_dir) || exit 0
head=$(git -C "$(aiup_root)" rev-parse HEAD 2>/dev/null) || exit 0
printf '%s\n' "$head" > "$gitdir/aiup-session-base"
rm -f "$gitdir/aiup-sensors-ran" "$gitdir/aiup-smoke-ran"
rm -rf "$gitdir/aiup-coverage-checked"
aiup_status_lines > "$gitdir/aiup-spec-status" || true
```

---

## `record-sensor-run.sh`

```bash
#!/usr/bin/env bash
#
# PostToolUse(Bash) — remember that the sensors ran, and were green.
#
# Writes the marker require-sensors.sh reads: the change set this run saw, in a
# file whose mtime is when it saw it. The evidence is the Surefire report each
# sensor class writes (target/surefire-reports/TEST-*.xml), never the console:
# a report has to exist for every sensor, be newer than the session start and
# than every changed file, count tests, and count no failure or error. So a
# narrowed run (-Dtest=…) leaves the other reports stale, a skipped run writes
# none, a failed one says so in the file, and a run in the background or piped
# through tail has not written them when this hook looks. Output text that
# merely looks clean is not consulted.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)
command=$(jq -r '.tool_input.command // ""' <<<"$input" 2>/dev/null) || exit 0

# Only a Maven test or verify can have written reports; nothing else is worth
# a stat. `mvn -q clean test`, `./mvnw -B verify …` — but not `mvn test-compile`.
grep -Eq '(\./)?mvnw?([[:space:]]|$)' <<<"$command" || exit 0
grep -Eq '[[:space:]](test|verify)([[:space:]]|$)' <<<"$command" || exit 0
[ "$(jq -r '.tool_response.interrupted // false' <<<"$input" 2>/dev/null)" = "true" ] && exit 0

gitdir=$(aiup_git_dir) || exit 0
changed=$(aiup_changed_files "$gitdir")
since=$(printf '%s\n' "$changed" | aiup_newest_mtime)
started=$(aiup_mtime "$gitdir/aiup-session-base")
if aiup_newer "$started" "$since"; then since=$started; fi
aiup_sensor_reports_ok "$since" >/dev/null || exit 0
printf '%s\n' "$changed" > "$gitdir/aiup-sensors-ran"
```

---

## `require-sensors.sh`

```bash
#!/usr/bin/env bash
#
# Stop — refuse to end the turn while changed code, specs or hooks are unverified.
#
# The sensors in src/test/java are post-hoc: they can only catch drift once
# someone runs them. Nothing in the repository can express "the agent ran them
# before it claimed to be done", because that is a property of the session, not
# of the source tree. This hook is that property — for the sensors over src/,
# docs/ and pom.xml, and for smoke.sh over the hooks themselves.
#
# It blocks once per turn. After a block the agent continues, runs what is
# asked, and re-enters Stop with stop_hook_active set; letting that second Stop
# through is what keeps a session that truly cannot build from looping forever.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)

active=$(jq -r '.stop_hook_active // false' <<<"$input" 2>/dev/null) || exit 0
[ "$active" = "true" ] && exit 0

cd "$(aiup_root)"
gitdir=$(aiup_git_dir) || exit 0
message=""

changed=$(aiup_changed_files "$gitdir") || exit 0
if [ -n "$changed" ]; then
    reason=$(aiup_unverified "$gitdir/aiup-sensors-ran" "$changed" "the sensors have")
    if [ -z "$reason" ]; then
        # The marker was written on the strength of the reports; a `clean`
        # since has taken the evidence away.
        since=$(printf '%s\n' "$changed" | aiup_newest_mtime)
        proof=$(aiup_sensor_reports_ok "$since") || reason="the evidence of that run is gone: $proof."
    fi
    if [ -n "$reason" ]; then
        message+="This session changed code, specifications or the build, and $reason

Changed in this session:
$(sed 's/^/  /' <<<"$changed")

Run the sensors before finishing:

  mvn -q test -Dgroups=sensor

That is ArchitectureTest, TestLayerConventionsTest, SourceConventionsTest and
the three traceability sensors that hold docs/ and the code together — seconds,
no Docker. A full mvn test or verify counts too. What counts is the fresh,
green Surefire report of every sensor, so a narrowed run (-Dtest=…), a skipped
one, a failed one, or one still running in the background does not — and
neither does output that merely looks clean. Run it in the foreground and let
it finish.

"
    fi
fi

hooks=$(aiup_changed_hooks "$gitdir") || exit 0
if [ -n "$hooks" ]; then
    reason=$(aiup_unverified "$gitdir/aiup-smoke-ran" "$hooks" "their smoke test has")
    if [ -n "$reason" ]; then
        message+="This session changed the hooks, and $reason

Changed in this session:
$(sed 's/^/  /' <<<"$hooks")

Run their smoke test before finishing:

  ./.claude/hooks/smoke.sh

"
    fi
fi

[ -n "$message" ] || exit 0
printf '%s' "$message" >&2
exit 2
```

---

## `guard-spec-status.sh`

```bash
#!/usr/bin/env bash
#
# PreToolUse(Edit|Write) — a Status: line is an assertion, not a label.
#
# `Status: Done` / `Tested` / `Automated` switches a traceability sensor on, so
# setting one by hand either breaks the build or, worse, certifies coverage that
# was never checked. This refuses the edit unless a coverage audit of that
# specification finished in this session after the code and tests last changed
# (record-coverage-check.sh writes that evidence). An edit made through Bash
# never passes here; check-spec-status.sh reads the file itself afterwards.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)

# A hook must never take the session down with it: anything unexpected on
# stdin means this guard has nothing to say, not that the call should fail.
file=$(jq -r '.tool_input.file_path // ""' <<<"$input" 2>/dev/null) || exit 0

case "$file" in
    */docs/use_cases/UC-*.md | */docs/test_cases/TC-*.md) ;;
    *) exit 0 ;;
esac

# No line anchor: an Edit's old_string may start mid-line.
status_line() { grep -m1 'Status:\*\*' <<<"$1" || true; }

# Edit carries new_string, Write carries the whole file; either may hold it.
after=$(status_line "$(jq -r '.tool_input.new_string // .tool_input.content // ""' <<<"$input" 2>/dev/null)")
[ -n "$after" ] || exit 0

# An Edit says what it replaces; a Write replaces the file, so the file on
# disk is the "before" — rewriting a Done specification is not a new claim.
if jq -e '.tool_input | has("old_string")' <<<"$input" >/dev/null 2>&1; then
    before=$(status_line "$(jq -r '.tool_input.old_string' <<<"$input" 2>/dev/null)")
elif [ -f "$file" ]; then
    before=$(status_line "$(cat "$file")")
else
    before=""
fi
value=$(aiup_status_value "$after")
[ "$(aiup_status_value "$before")" = "$value" ] && exit 0

# Draft, Reviewed, Approved, Implemented, a downgrade: no sensor switches on,
# nothing to prove. Note Implemented is deliberately not asserting.
aiup_asserting_status "$value" || exit 0

id=$(aiup_spec_id "$file") || exit 0
gitdir=$(aiup_git_dir) || exit 0
aiup_coverage_checked "$gitdir" "$id" && exit 0

cat >&2 <<MSG
Refused: $(basename "$file") would read "**Status:** $value".

That line is an assertion the traceability sensors act on, not a label, and no
coverage audit of $id has finished in this session since the code and tests
last changed. Run

  aiup-vaadin-holon:coverage-check $id

first. When it reports no gaps, make this edit again and then run the sensors
(mvn -q test -Dgroups=sensor) so the one it switches on actually votes. When it
reports gaps, close them or leave the status as it is.
MSG
exit 2
```

---

## `check-spec-status.sh`

```bash
#!/usr/bin/env bash
#
# PostToolUse(every tool) — notice a Status: line that changed, whoever changed it.
#
# guard-spec-status.sh sees an Edit or a Write before it happens; a sed, a
# heredoc or a script goes past it. So this one does not look at the tool call
# at all: after every call it reads the specifications themselves, compares
# each Status: with the value last seen, and reports one that now asserts
# Done / Tested / Automated without a coverage audit behind it. It reports
# once per change — the new value becomes the one last seen either way — and
# the Stop hook then holds the turn until the sensors have judged the claim.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

cat >/dev/null # the input does not matter; the state of the repository does

gitdir=$(aiup_git_dir) || exit 0
snapshot="$gitdir/aiup-spec-status"

current=$(aiup_status_lines) || exit 0
if [ -f "$snapshot" ]; then
    previous=$(cat "$snapshot")
    seen=true
else
    previous=""
    seen=false # first sight — nothing to compare against
fi
printf '%s\n' "$current" > "$snapshot"
[ "$seen" = true ] || exit 0

flagged=""
while IFS=$'\t' read -r path value; do
    [ -n "$path" ] || continue
    aiup_asserting_status "$value" || continue
    old=$(awk -F'\t' -v p="$path" '$1 == p { print $2 }' <<<"$previous")
    [ "$old" = "$value" ] && continue
    id=$(aiup_spec_id "$path") || continue
    aiup_coverage_checked "$gitdir" "$id" && continue
    flagged="$flagged  $path: \"${old:-<new file>}\" -> \"$value\""$'\n'
done <<<"$current"
[ -n "$flagged" ] || exit 0

cat >&2 <<MSG
A Status: line changed without a coverage audit behind it:

$flagged
That line is an assertion the traceability sensors act on, not a label. Either
put the previous value back, or run

  aiup-vaadin-holon:coverage-check <id>

and set the status again once it reports no gaps. Then run the sensors
(mvn -q test -Dgroups=sensor) so the one it switched on actually votes.
MSG
exit 2
```

---

## `record-coverage-check.sh`

```bash
#!/usr/bin/env bash
#
# SubagentStop(uc-coverage) — remember that a coverage audit finished.
#
# guard-spec-status.sh and check-spec-status.sh accept an asserting Status:
# only after aiup-vaadin-holon:coverage-check has audited that specification,
# and the audit runs in the plugin's read-only uc-coverage agent. Its stop is
# the one moment the session knows the audit happened, so this writes a marker
# per id the agent was asked about. The marker says an audit finished, not that
# it found no gaps: the sensors judge the claim itself on the next run.
set -euo pipefail
. "$(dirname "$0")/lib.sh"

input=$(cat)

case "$(jq -r '.agent_type // ""' <<<"$input" 2>/dev/null)" in
    *uc-coverage) ;;
    *) exit 0 ;;
esac
# An agent cut off by its token limit has audited nothing.
[ "$(jq -r '.stop_reason // "end_turn"' <<<"$input" 2>/dev/null)" = "end_turn" ] || exit 0

# UC-001, uc001, TC 1 … → UC-001 / TC-001, one per line.
ids() {
    grep -oiE '(UC|TC)[- ]?[0-9]{1,3}' | tr '[:lower:]' '[:upper:]' \
        | sed -E 's/^(UC|TC)[- ]?0*([0-9]+)$/\1 \2/' \
        | awk '{ printf "%s-%03d\n", $1, $2 }' | sort -u
}

# The assignment is the first user message of the agent's own transcript. The
# report's heading names the same id and stands in when the transcript lags.
transcript=$(jq -r '.agent_transcript_path // ""' <<<"$input" 2>/dev/null) || transcript=""
transcript="${transcript/#\~/$HOME}"
found=""
if [ -n "$transcript" ] && [ -f "$transcript" ]; then
    found=$(jq -c 'select(.type == "user") | .message.content
                   | if type == "string" then . else map(.text? // "") | join(" ") end' \
                "$transcript" 2>/dev/null | head -1 | ids || true)
fi
if [ -z "$found" ]; then
    found=$(jq -r '.last_assistant_message // ""' <<<"$input" 2>/dev/null \
                | grep -E '^## (UC|TC)-[0-9]{3}' | ids || true)
fi
[ -n "$found" ] || exit 0

gitdir=$(aiup_git_dir) || exit 0
mkdir -p "$gitdir/aiup-coverage-checked"
while IFS= read -r id; do
    [ -n "$id" ] && touch "$gitdir/aiup-coverage-checked/$id"
done <<<"$found"
exit 0
```

---

## `settings.json`

Goes at `.claude/settings.json`, not in `hooks/`. If the project already has
one, merge the `hooks` block into it rather than overwriting — `permissions`
and anything else there are the project's own.

```json
{
  "permissions": {
    "allow": [
      "Bash(mvn:*)",
      "Bash(./mvnw:*)",
      "Bash(./.claude/hooks/smoke.sh)",
      "Bash(git status:*)",
      "Bash(git diff:*)",
      "Bash(git log:*)",
      "Bash(git show:*)",
      "Bash(git branch:*)"
    ],
    "deny": [
      "Edit(target/**)"
    ]
  },
  "hooks": {
    "SessionStart": [
      {
        "matcher": "startup|clear",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/session-start.sh\"",
            "timeout": 10
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/guard-spec-status.sh\"",
            "timeout": 10
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/record-sensor-run.sh\"",
            "timeout": 10
          }
        ]
      },
      {
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/check-spec-status.sh\"",
            "timeout": 10
          }
        ]
      }
    ],
    "SubagentStop": [
      {
        "matcher": "aiup-vaadin-holon:uc-coverage$",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/record-coverage-check.sh\"",
            "timeout": 10
          }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/require-sensors.sh\"",
            "timeout": 15
          }
        ]
      }
    ]
  }
}
```

The `PostToolUse` block has **two** entries on purpose. The first has a
`matcher` of `Bash`, because only a Bash call can have run Maven. The second
has **no matcher**, so it fires after every tool — that is what catches a
`Status:` line changed by a `sed`, a script, or any tool added to the product
after these hooks were written.

## Notes on adapting

- **Base package.** `aiup_sensor_reports_ok` matches `TEST-*.<SimpleName>.xml`,
  so nothing here needs to know the project's package. If two classes in
  different packages share a simple name, the first match wins — which is a
  reason not to do that.
- **Sensor list.** `AIUP_SENSORS` must name exactly the classes tagged
  `sensor`. Add a sensor without adding it here and the guard stops asking for
  it; remove one without removing it here and the guard can never be satisfied.
  The smoke test writes a report per name in the list, so it catches the
  second mistake but not the first.
- **`mvnw`.** The scripts accept `mvn` and `./mvnw` equally. If the project
  has a wrapper, prefer it in the refusal messages so the agent uses the
  pinned Maven version.
