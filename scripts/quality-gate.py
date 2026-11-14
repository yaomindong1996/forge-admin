#!/usr/bin/env python3
"""Regression-aware quality gate.

Wraps the project's existing analysis tools (complexity, duplication, dead
code, lint) and enforces one rule: legacy debt recorded in the baseline may
stay, but no change may add new debt or make baselined debt worse. When debt
is removed, the stale baseline entry must be pruned so the gate only ever
tightens.

Three commands, same locally and in CI:

  quality-gate.py report   [--changed-only]        show findings, grouped by new / baselined / stale
  quality-gate.py check    [--changed-only] [--allow-stale]
                                                   exit 1 on new or worsened debt, stale entries,
                                                   or an unreviewed baseline
  quality-gate.py baseline --reason TEXT           create a baseline (refuses if one exists)
  quality-gate.py baseline --extend --reason TEXT  add current un-baselined findings (needs review)
  quality-gate.py baseline --prune                 remove stale entries / tighten metrics (safe)
  quality-gate.py baseline --approve --reviewed-by NAME
                                                   mark the baseline as human-reviewed

Config: .quality-gate.json      (checks to run, base ref, baseline path)
Baseline: .quality-baseline.json (fingerprinted findings; committed; CODEOWNERS-protected)

Finding formats a check command may emit, one finding per line:
  unix   path:line[:col]: message            (rule id as trailing [rule] or (rule), else check name)
  jsonl  {"file": ..., "line": ..., "rule": ..., "message": ...}

Identity ignores line numbers and numeric values, so editing unrelated lines
does not create "new" findings. The first integer in a message (complexity
value, duplicated-line count) is tracked as a metric: a higher value than the
baseline counts as worsened. Set "metric": false on a check to disable that.

Python 3.8+, stdlib only.
"""

from __future__ import annotations

import argparse
import datetime as _dt
import hashlib
import json
import os
import re
import subprocess
import sys
from collections import defaultdict
from typing import Any, Dict, List, Optional, Tuple

CONFIG_DEFAULT = ".quality-gate.json"
UNIX_RE = re.compile(r"^(?P<file>[^:\s][^:\n]*?):(?P<line>\d+)(?::(?P<col>\d+))?:\s*(?P<message>.+)$")
RULE_TAIL_RE = re.compile(r"\s*[\[(]([A-Za-z0-9_./:-]+)[\])]\s*$")
INT_RE = re.compile(r"\d+")


def now() -> str:
    return _dt.datetime.now(_dt.timezone.utc).replace(microsecond=0).isoformat()


def die(msg: str, code: int = 2) -> None:
    print(f"quality-gate: {msg}", file=sys.stderr)
    sys.exit(code)


# ─── Config and baseline ─────────────────────────────────────────────────────


def load_json(path: str) -> Optional[Dict[str, Any]]:
    if not os.path.exists(path):
        return None
    with open(path, "r", encoding="utf-8") as fh:
        try:
            return json.load(fh)
        except json.JSONDecodeError as exc:
            die(f"{path} is not valid JSON: {exc}")
    return None


def save_json(path: str, data: Dict[str, Any]) -> None:
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, sort_keys=False)
        fh.write("\n")


def load_config(path: str) -> Dict[str, Any]:
    cfg = load_json(path)
    if cfg is None:
        die(f"no config at {path}")
    if cfg.get("version") != 1:
        die(f"unsupported config version {cfg.get('version')!r}")
    if not cfg.get("checks"):
        die("config has no checks")
    cfg.setdefault("base_ref", "origin/main")
    cfg.setdefault("baseline", ".quality-baseline.json")
    return cfg


def empty_baseline() -> Dict[str, Any]:
    return {"version": 1, "reviewed": False, "review": None, "entries": {}}


# ─── Findings ────────────────────────────────────────────────────────────────


class Finding:
    __slots__ = ("check", "rule", "file", "line", "message", "metric", "key")

    def __init__(self, check: str, rule: str, file: str, line: Optional[int], message: str, track_metric: bool):
        self.check = check
        self.rule = rule
        self.file = os.path.normpath(file).replace(os.sep, "/").lstrip("./") or file
        self.line = line
        self.message = message.strip()
        m = INT_RE.search(self.message) if track_metric else None
        self.metric = int(m.group(0)) if m else None
        ident = "|".join([check, rule, self.file, re.sub(r"\s+", " ", INT_RE.sub("#", self.message))])
        self.key = hashlib.sha1(ident.encode("utf-8")).hexdigest()[:16]


def parse_output(check: str, spec: Dict[str, Any], output: str) -> List[Finding]:
    fmt = spec.get("format", "unix")
    track_metric = spec.get("metric", True) is not False
    default_rule = spec.get("rule", check)
    findings: List[Finding] = []
    for raw in output.splitlines():
        line = raw.strip()
        if not line:
            continue
        if fmt == "jsonl":
            if not line.startswith("{"):
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue
            if "file" not in obj or "message" not in obj:
                continue
            ln = obj.get("line")
            findings.append(
                Finding(check, str(obj.get("rule") or default_rule), str(obj["file"]),
                        int(ln) if isinstance(ln, int) or (isinstance(ln, str) and ln.isdigit()) else None,
                        str(obj["message"]), track_metric)
            )
        else:
            m = UNIX_RE.match(line)
            if not m:
                continue
            message = m.group("message")
            rule = default_rule
            tail = RULE_TAIL_RE.search(message)
            if tail:
                rule = tail.group(1)
                message = message[: tail.start()]
            findings.append(Finding(check, rule, m.group("file"), int(m.group("line")), message, track_metric))
    return findings


def run_checks(cfg: Dict[str, Any], cwd: str, only: Optional[List[str]] = None) -> Tuple[List[Finding], List[str]]:
    findings: List[Finding] = []
    errors: List[str] = []
    for name, spec in cfg["checks"].items():
        if only and name not in only:
            continue
        try:
            proc = subprocess.run(
                spec["command"], shell=True, cwd=cwd, text=True,
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=spec.get("timeout", 1800),
            )
        except subprocess.TimeoutExpired:
            errors.append(f"{name}: timed out")
            continue
        parsed = parse_output(name, spec, proc.stdout)
        # A non-zero exit with no parseable findings means the tool itself failed.
        if proc.returncode != 0 and not parsed and spec.get("fail_on_error", True):
            tail = (proc.stderr or proc.stdout).strip().splitlines()[-5:]
            errors.append(f"{name}: exited {proc.returncode} with no findings parsed: " + " / ".join(tail))
            continue
        findings.extend(parsed)
    return findings, errors


# ─── Git ─────────────────────────────────────────────────────────────────────


def git(cwd: str, *args: str) -> Optional[str]:
    proc = subprocess.run(["git", *args], cwd=cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return proc.stdout.strip() if proc.returncode == 0 else None


def changed_files(cwd: str, base_ref: str) -> Optional[set]:
    """Files changed since the merge-base with base_ref, plus uncommitted changes."""
    mb = git(cwd, "merge-base", base_ref, "HEAD")
    if mb is None:
        return None
    committed = git(cwd, "diff", "--name-only", mb) or ""
    untracked = git(cwd, "ls-files", "--others", "--exclude-standard") or ""
    return {p.replace(os.sep, "/") for p in (committed + "\n" + untracked).splitlines() if p.strip()}


# ─── Comparison ──────────────────────────────────────────────────────────────


def aggregate(findings: List[Finding]) -> Dict[str, Dict[str, Any]]:
    agg: Dict[str, Dict[str, Any]] = {}
    for f in findings:
        e = agg.setdefault(f.key, {"check": f.check, "rule": f.rule, "file": f.file, "count": 0, "metric": None, "sample": f.message, "lines": []})
        e["count"] += 1
        if f.metric is not None and (e["metric"] is None or f.metric > e["metric"]):
            e["metric"] = f.metric
        if f.line is not None:
            e["lines"].append(f.line)
    return agg


def compare(current: Dict[str, Dict[str, Any]], baseline: Dict[str, Any]) -> Dict[str, List[Dict[str, Any]]]:
    entries = baseline.get("entries", {})
    out: Dict[str, List[Dict[str, Any]]] = {"new": [], "worsened": [], "baselined": [], "stale": [], "improved": []}
    for key, cur in current.items():
        base = entries.get(key)
        if base is None:
            out["new"].append({"key": key, **cur})
        elif cur["count"] > base["count"]:
            out["worsened"].append({"key": key, **cur, "baseline_count": base["count"], "why": f"count {base['count']} -> {cur['count']}"})
        elif cur["metric"] is not None and base.get("metric") is not None and cur["metric"] > base["metric"]:
            out["worsened"].append({"key": key, **cur, "baseline_metric": base["metric"], "why": f"metric {base['metric']} -> {cur['metric']}"})
        else:
            out["baselined"].append({"key": key, **cur})
            if cur["count"] < base["count"] or (cur["metric"] is not None and base.get("metric") is not None and cur["metric"] < base["metric"]):
                out["improved"].append({"key": key, **cur, "baseline_count": base["count"], "baseline_metric": base.get("metric")})
    for key, base in entries.items():
        if key not in current:
            out["stale"].append({"key": key, **base})
    return out


# ─── Output ──────────────────────────────────────────────────────────────────


def annotate(level: str, e: Dict[str, Any], title: str) -> None:
    """Emit GitHub Actions annotations so findings land on the PR diff."""
    if not os.environ.get("GITHUB_ACTIONS"):
        return
    line = e.get("lines", [None])[0] if e.get("lines") else None
    loc = f"file={e['file']}" + (f",line={line}" if line else "")
    print(f"::{level} {loc},title={title}::{e['check']}/{e['rule']}: {e['sample']}")


def describe(e: Dict[str, Any]) -> str:
    lines = ",".join(str(l) for l in e.get("lines", [])[:5]) if e.get("lines") else "?"
    extra = f" ({e['why']})" if e.get("why") else ""
    return f"{e['file']}:{lines}  [{e['check']}/{e['rule']}] {e['sample']}{extra}"


def print_report(cmp: Dict[str, List[Dict[str, Any]]], changed: Optional[set], errors: List[str], verbose: bool) -> None:
    def in_change(e: Dict[str, Any]) -> str:
        if changed is None:
            return ""
        return "  <- in this change" if e["file"] in changed else "  (outside this change)"

    for section, label in (("new", "NEW debt (not in baseline)"), ("worsened", "WORSENED debt"), ("stale", "STALE baseline entries (debt no longer found)")):
        items = cmp[section]
        if items:
            print(f"\n== {label}: {len(items)}")
            for e in items:
                print("  " + describe(e) + in_change(e))
    if cmp["improved"]:
        print(f"\n== IMPROVED (baseline can be tightened): {len(cmp['improved'])}")
        for e in cmp["improved"]:
            print("  " + describe(e))
    if verbose and cmp["baselined"]:
        print(f"\n== BASELINED (legacy debt, allowed): {len(cmp['baselined'])}")
        for e in cmp["baselined"]:
            print("  " + describe(e))
    else:
        print(f"\n== baselined legacy findings: {len(cmp['baselined'])}")
    for err in errors:
        print(f"\n!! check error: {err}")


def review_summary(entries: Dict[str, Dict[str, Any]]) -> str:
    by_rule: Dict[str, int] = defaultdict(int)
    by_file: Dict[str, int] = defaultdict(int)
    for e in entries.values():
        by_rule[f"{e['check']}/{e['rule']}"] += e["count"]
        by_file[e["file"]] += e["count"]
    lines = [f"  findings: {sum(by_rule.values())} across {len(by_file)} files"]
    lines.append("  by rule:")
    lines += [f"    {n:5d}  {r}" for r, n in sorted(by_rule.items(), key=lambda x: -x[1])]
    lines.append("  top files:")
    lines += [f"    {n:5d}  {f}" for f, n in sorted(by_file.items(), key=lambda x: -x[1])[:10]]
    return "\n".join(lines)


# ─── Commands ────────────────────────────────────────────────────────────────


def gather(args: argparse.Namespace) -> Tuple[Dict[str, Any], Dict[str, Any], Dict[str, Dict[str, Any]], Optional[set], List[str]]:
    cfg = load_config(args.config)
    baseline_path = os.path.join(args.cwd, args.baseline or cfg["baseline"])
    baseline = load_json(baseline_path) or empty_baseline()
    findings, errors = run_checks(cfg, args.cwd, args.only)
    changed = changed_files(args.cwd, cfg["base_ref"])
    if changed is None and getattr(args, "changed_only", False):
        print(f"quality-gate: cannot resolve merge-base with {cfg['base_ref']}; --changed-only ignored", file=sys.stderr)
    if getattr(args, "changed_only", False) and changed is not None:
        findings = [f for f in findings if f.file in changed]
    return cfg, baseline, aggregate(findings), changed, errors


def cmd_report(args: argparse.Namespace) -> int:
    cfg, baseline, current, changed, errors = gather(args)
    cmp = compare(current, baseline)
    if args.changed_only:
        cmp["stale"] = []  # stale detection needs the full run
    print_report(cmp, changed, errors, verbose=True)
    return 0


def cmd_check(args: argparse.Namespace) -> int:
    cfg, baseline, current, changed, errors = gather(args)
    baseline_path = args.baseline or cfg["baseline"]
    cmp = compare(current, baseline)
    if args.changed_only:
        cmp["stale"] = []
    print_report(cmp, changed, errors, verbose=args.verbose)
    for e in cmp["new"]:
        annotate("error", e, "New quality debt")
    for e in cmp["worsened"]:
        annotate("error", e, "Worsened quality debt")

    failed = False
    if baseline.get("entries") and not baseline.get("reviewed"):
        print(f"\n✗ {baseline_path} has entries that have not been human-reviewed. "
              "A reviewer must run: quality-gate.py baseline --approve --reviewed-by NAME")
        failed = True
    if cmp["new"] or cmp["worsened"]:
        print(f"\n✗ {len(cmp['new'])} new and {len(cmp['worsened'])} worsened finding(s). Fix the code; "
              "do not add them to the baseline. (Legacy debt may only be baselined by a human with a reason.)")
        failed = True
    if cmp["stale"] and not args.allow_stale:
        print(f"\n✗ {len(cmp['stale'])} stale baseline entr{'y' if len(cmp['stale']) == 1 else 'ies'}. "
              "Debt was fixed -- lock it in: quality-gate.py baseline --prune")
        failed = True
    if errors and not args.allow_errors:
        print(f"\n✗ {len(errors)} check(s) failed to run.")
        failed = True
    if not failed:
        print("\n✓ quality gate passed" + (f" ({len(cmp['improved'])} improvement(s) available to prune)" if cmp["improved"] else ""))
    return 1 if failed else 0


def cmd_baseline(args: argparse.Namespace) -> int:
    cfg = load_config(args.config)
    baseline_path = os.path.join(args.cwd, args.baseline or cfg["baseline"])
    existing = load_json(baseline_path)

    if args.approve:
        if existing is None:
            die("no baseline to approve")
        if not args.reviewed_by:
            die("--approve requires --reviewed-by NAME")
        existing["reviewed"] = True
        existing["review"] = {"by": args.reviewed_by, "at": now(), "entries": len(existing["entries"])}
        save_json(baseline_path, existing)
        print(f"quality-gate: baseline approved by {args.reviewed_by} ({len(existing['entries'])} entries)")
        return 0

    if args.prune:
        if existing is None:
            die("no baseline to prune")
        findings, errors = run_checks(cfg, args.cwd, args.only)
        if errors:
            die("refusing to prune while checks fail to run: " + "; ".join(errors))
        cmp = compare(aggregate(findings), existing)
        removed = 0
        tightened = 0
        for e in cmp["stale"]:
            del existing["entries"][e["key"]]
            removed += 1
        for e in cmp["improved"]:
            entry = existing["entries"][e["key"]]
            entry["count"] = e["count"]
            if e["metric"] is not None:
                entry["metric"] = e["metric"]
            tightened += 1
        existing.setdefault("history", []).append({"at": now(), "event": "prune", "removed": removed, "tightened": tightened})
        save_json(baseline_path, existing)
        print(f"quality-gate: pruned {removed} stale entr{'y' if removed == 1 else 'ies'}, tightened {tightened}")
        return 0

    # Create or extend: this blesses debt, so it is gated behind a reason and human review.
    if os.environ.get("CI") and not args.force:
        die("refusing to create or extend a baseline in CI; baselines are created locally and reviewed in a PR")
    if not args.reason or not args.reason.strip():
        die("--reason is required when creating or extending a baseline (why is this debt being accepted rather than fixed?)")
    if existing and not args.extend:
        die(f"{baseline_path} already exists; use --extend to add current un-baselined findings, or --prune")

    findings, errors = run_checks(cfg, args.cwd, args.only)
    if errors:
        die("refusing to baseline while checks fail to run: " + "; ".join(errors))
    current = aggregate(findings)
    data = existing or empty_baseline()
    added: Dict[str, Dict[str, Any]] = {}
    for key, cur in current.items():
        if key not in data["entries"]:
            data["entries"][key] = {"check": cur["check"], "rule": cur["rule"], "file": cur["file"],
                                    "count": cur["count"], "metric": cur["metric"], "sample": cur["sample"],
                                    "added_at": now(), "reason": args.reason.strip()}
            added[key] = cur
        elif cur["count"] > data["entries"][key]["count"] or (
            cur["metric"] is not None and data["entries"][key].get("metric") is not None and cur["metric"] > data["entries"][key]["metric"]
        ):
            data["entries"][key].update({"count": cur["count"], "metric": cur["metric"], "reason": args.reason.strip(), "added_at": now()})
            added[key] = cur
    if not added:
        print("quality-gate: nothing to add; baseline already covers all current findings")
        return 0
    if args.dry_run:
        print("quality-gate: baseline candidate (dry run, nothing written):\n" + review_summary(added))
        return 0
    data["reviewed"] = False
    data["review"] = None
    data.setdefault("history", []).append({"at": now(), "event": "extend" if existing else "create", "added": len(added), "reason": args.reason.strip()})
    save_json(baseline_path, data)
    print(f"quality-gate: wrote {len(added)} entr{'y' if len(added) == 1 else 'ies'} to {os.path.relpath(baseline_path)}\n" + review_summary(added))
    print("\nThis baseline is NOT yet approved. Open a PR so a reviewer can inspect the accepted debt, then run:\n"
          "  quality-gate.py baseline --approve --reviewed-by NAME")
    return 0


# ─── CLI ─────────────────────────────────────────────────────────────────────


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="quality-gate.py", description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--config", default=None, help=f"config path (default: ./{CONFIG_DEFAULT})")
    p.add_argument("--baseline", default=None, help="override baseline path from config")
    p.add_argument("--only", nargs="*", default=None, help="run only these checks")
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("report", help="show all findings grouped by new / worsened / stale / baselined")
    s.add_argument("--changed-only", action="store_true", help="only findings in files changed since merge-base")
    s.set_defaults(fn=cmd_report)

    s = sub.add_parser("check", help="fail on new or worsened debt, stale entries, or unreviewed baseline")
    s.add_argument("--changed-only", action="store_true")
    s.add_argument("--allow-stale", action="store_true", help="do not fail on stale baseline entries")
    s.add_argument("--allow-errors", action="store_true", help="do not fail when a check cannot run")
    s.add_argument("--verbose", action="store_true", help="also list baselined findings")
    s.set_defaults(fn=cmd_check)

    s = sub.add_parser("baseline", help="create, extend, prune, or approve the baseline")
    s.add_argument("--reason", default=None, help="why this debt is accepted (required to create/extend)")
    s.add_argument("--extend", action="store_true", help="add current un-baselined findings to an existing baseline")
    s.add_argument("--prune", action="store_true", help="remove stale entries and tighten improved metrics")
    s.add_argument("--approve", action="store_true", help="mark the baseline as human-reviewed")
    s.add_argument("--reviewed-by", default=None)
    s.add_argument("--dry-run", action="store_true", help="show what would be baselined without writing")
    s.add_argument("--force", action="store_true", help="allow create/extend under CI=1 (not recommended)")
    s.set_defaults(fn=cmd_baseline)
    return p


def main(argv: Optional[List[str]] = None) -> int:
    args = build_parser().parse_args(argv)
    args.config = os.path.abspath(args.config or CONFIG_DEFAULT)
    args.cwd = os.path.dirname(args.config)
    return args.fn(args)


if __name__ == "__main__":
    sys.exit(main())
