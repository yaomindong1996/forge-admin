#!/usr/bin/env bash
# Self-test for the quality gate. Proves, against the installed configuration:
#   1. unchanged legacy debt passes          (clean tree, real baseline copy)
#   2. new / worsened debt fails             (synthetic fixture over the threshold)
#   3. stale baseline entries are removed    (injected entry -> check fails -> prune -> passes)
#
# Works on a temporary copy of the baseline so the real one is never touched.
# The copy is marked reviewed: the real baseline's review state is enforced by
# `check` in CI, while this test isolates the comparison behaviour, so it can
# run before a reviewer approves the baseline.
# Fill in the three placeholders for the project, then wire this into the test
# command and CI.
#
#   FIXTURE_PATH    a source path the complexity check scans (must not already exist)
#   FIXTURE_BODY    one function over the complexity threshold, in the project's language
#                   (see references/quality-gates-pattern.md, "Debt Fixture Snippets")
#   GATE            how the gate is invoked

set -euo pipefail

GATE="${GATE:-python3 scripts/quality-gate.py}"
FIXTURE_PATH="${FIXTURE_PATH:-forge-server/forge-framework/forge-starter-parent/forge-starter-orm/src/main/java/com/mdframe/forge/starter/orm/support/QualityGateFixture.java}"
FIXTURE_BODY="${FIXTURE_BODY:-package com.mdframe.forge.starter.orm.support;
public final class QualityGateFixture {
  private QualityGateFixture() {}
  public static int qgFixture(int n) {
    if (n == 1) return 1; if (n == 2) return 2; if (n == 3) return 3; if (n == 4) return 4;
    if (n == 5) return 5; if (n == 6) return 6; if (n == 7) return 7; if (n == 8) return 8;
    if (n == 9) return 9; if (n == 10) return 10; if (n == 11) return 11; if (n == 12) return 12;
    if (n == 13) return 13; if (n == 14) return 14; if (n == 15) return 15; if (n == 16) return 16;
    if (n == 17) return 17; return 0;
  }
}}"
CONFIG=".quality-gate.json"
REAL_BASELINE="$(python3 -c 'import json; print(json.load(open(".quality-gate.json")).get("baseline", ".quality-baseline.json"))')"

if [[ "$FIXTURE_PATH" == "[FIXTURE_PATH]" || "$FIXTURE_BODY" == "[FIXTURE_BODY]" ]]; then
  echo "quality-gate-test: fill in FIXTURE_PATH and FIXTURE_BODY first" >&2
  exit 2
fi
if [[ -e "$FIXTURE_PATH" ]]; then
  echo "quality-gate-test: $FIXTURE_PATH already exists; choose a fixture path that does not" >&2
  exit 2
fi
if [[ ! -f "$REAL_BASELINE" ]]; then
  echo "quality-gate-test: $REAL_BASELINE does not exist; create the baseline first (quality-gate.py baseline --reason ...)" >&2
  exit 2
fi

TMP_BASELINE="$(mktemp -t quality-baseline.XXXXXX.json)"
cleanup() { rm -f "$FIXTURE_PATH" "$TMP_BASELINE"; }
trap cleanup EXIT
cp "$REAL_BASELINE" "$TMP_BASELINE"
python3 - "$TMP_BASELINE" <<'PY'
import json, sys
p = sys.argv[1]; d = json.load(open(p)); d["reviewed"] = True; json.dump(d, open(p, "w"), indent=2)
PY

run_gate() { $GATE --baseline "$TMP_BASELINE" "$@"; }
pass=0; fail=0
ok()   { echo "  ✓ $1"; pass=$((pass+1)); }
bad()  { echo "  ✗ $1"; fail=$((fail+1)); }

echo "quality-gate self-test"

# 1. legacy debt passes
if run_gate check >/dev/null 2>&1; then ok "clean tree passes with the current baseline"; else bad "clean tree should pass (is the baseline reviewed and pruned?)"; fi

# 2. new debt fails
printf '%s\n' "$FIXTURE_BODY" > "$FIXTURE_PATH"
if out="$(run_gate check 2>&1)"; then
  bad "synthetic debt at $FIXTURE_PATH should fail check"
else
  if grep -q "$FIXTURE_PATH" <<<"$out"; then ok "new debt fails and names $FIXTURE_PATH"; else bad "check failed but did not name the fixture file"; fi
fi
rm -f "$FIXTURE_PATH"

# 3. stale entries are removed
python3 - "$TMP_BASELINE" <<'PY'
import json, sys
p = sys.argv[1]; d = json.load(open(p))
d["entries"]["deadbeefdeadbeef"] = {"check": "complexity", "rule": "complexity", "file": "src/__no_such_file__",
                                    "count": 1, "metric": 42, "sample": "stale fixture", "added_at": "1970-01-01T00:00:00+00:00", "reason": "self-test"}
json.dump(d, open(p, "w"), indent=2)
PY
if run_gate check >/dev/null 2>&1; then bad "stale entry should fail check"; else ok "stale entry fails check"; fi
run_gate baseline --prune >/dev/null 2>&1 || true
if python3 -c 'import json,sys; sys.exit(0 if "deadbeefdeadbeef" not in json.load(open(sys.argv[1]))["entries"] else 1)' "$TMP_BASELINE"; then
  ok "prune removed the stale entry"
else
  bad "prune did not remove the stale entry"
fi
if run_gate check >/dev/null 2>&1; then ok "check passes after prune"; else bad "check should pass after prune"; fi

echo "quality-gate self-test: $pass passed, $fail failed"
[[ "$fail" -eq 0 ]]
