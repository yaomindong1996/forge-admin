# Quality Gates

This repository runs a regression-aware quality gate on every PR. Legacy debt recorded in `.quality-baseline.json` is allowed to stay; any change that adds new complexity, duplication, or dead code -- or makes a baselined finding worse -- fails CI.

## Commands

Same commands locally and in CI (`package.json` scripts):

| Command | What it does |
|---------|--------------|
| `pnpm quality:report` / `python3 scripts/quality-gate.py report` | Show all findings: new, worsened, stale, and baselined. Add `--changed-only` to scope to files changed since the merge-base |
| `pnpm quality:check` / `python3 scripts/quality-gate.py check` | Exit 1 on new or worsened findings, stale baseline entries, or an unreviewed baseline. Run before marking work done |
| `pnpm quality:prune` / `python3 scripts/quality-gate.py baseline --prune` | Remove baseline entries for debt that has been fixed and tighten improved metrics. Safe for anyone to run; commit the result |

## Checks

| Check | Tool | What it catches | Threshold / scope |
|-------|------|-----------------|-------------------|
| `complexity` | [lizard](http://www.lizard.ws) via `scripts/quality-gate-adapter.py` | Cyclomatic complexity per function | CCN > 15; `forge-server/forge-framework`, `forge-admin-server` main, `forge-admin-ui/src` |
| `duplication` | [jscpd](https://github.com/kucherenko/jscpd) | Duplicated blocks | min 80 tokens; framework + admin-ui src |
| `dead-code` | — | **暂未启用** | knip 在 Vue 动态导出上误报偏高；Java 侧待接入 PMD |

Configuration lives in `.quality-gate.json`.

## When the gate fails

1. Read the output. Each finding names the file, the rule, and whether the file is part of this change.
2. Fix the code. Reduce the function, extract the duplicate, delete the dead export.
3. Re-run `pnpm quality:check` until it passes.

Do **not** add findings to the baseline to get past the gate. The baseline records debt that existed before the gate was installed; it is not an escape hatch.

## Stale entries

If `check` reports stale entries, someone fixed debt and the baseline has not caught up. Run `pnpm quality:prune` and commit `.quality-baseline.json`. The baseline only ever shrinks this way.

## Extending the baseline (humans only)

Rarely, accepting new debt is the right call. That is a reviewed decision:

1. `python3 scripts/quality-gate.py baseline --reason "<why>" --dry-run`
2. On confirmation (human): run without `--dry-run` (writes **unreviewed** baseline)
3. Reviewer runs `python3 scripts/quality-gate.py baseline --approve --reviewed-by "<name>"`
4. `.quality-baseline.json` is CODEOWNERS-protected

Agents may run `report`, `check`, and `baseline --prune`. Agents must never create, extend, or approve the baseline.
