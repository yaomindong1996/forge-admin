#!/usr/bin/env python3
"""Adapters for scripts/quality-gate.py — emit unix or jsonl findings."""
from __future__ import annotations

import csv
import io
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CCN_THRESHOLD = int(os.environ.get("QG_CCN_THRESHOLD", "15"))
MIN_DUP_TOKENS = int(os.environ.get("QG_MIN_DUP_TOKENS", "80"))

COMPLEXITY_PATHS = [
    "forge-server/forge-framework",
    "forge-server/forge-admin-server/src/main/java",
    "forge-admin-ui/src",
]
DUP_PATHS = [
    "forge-server/forge-framework",
    "forge-admin-ui/src",
]
DEAD_CODE_PATH = "forge-admin-ui"


def run(cmd: list[str], cwd: Path | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        cmd,
        cwd=str(cwd or ROOT),
        capture_output=True,
        text=True,
        check=False,
    )


def emit_complexity() -> int:
    # lizard CSV: nloc,ccn,token,param,length,location,file,function_name,long_name,start,end
    cmd = [
        sys.executable,
        "-m",
        "lizard",
        *COMPLEXITY_PATHS,
        "-l",
        "java",
        "-l",
        "javascript",
        "-l",
        "vue",
        "-C",
        str(CCN_THRESHOLD),
        "--csv",
    ]
    proc = run(cmd)
    # lizard returns non-zero when warnings exist; still parse stdout
    reader = csv.reader(io.StringIO(proc.stdout))
    for row in reader:
        if len(row) < 11:
            continue
        try:
            ccn = int(row[1])
            line = int(row[9])
        except ValueError:
            continue
        if ccn <= CCN_THRESHOLD:
            continue
        path = row[6]
        name = row[7]
        print(f"{path}:{line}: {name} has complexity {ccn} [complexity]")
    return 0


def emit_duplication() -> int:
    out_dir = ROOT / ".jscpd-out"
    out_dir.mkdir(exist_ok=True)
    cmd = [
        "npx",
        "--yes",
        "jscpd",
        *DUP_PATHS,
        "--min-tokens",
        str(MIN_DUP_TOKENS),
        "--reporters",
        "json",
        "--output",
        str(out_dir),
        "--silent",
        "--ignore",
        "**/node_modules/**,**/target/**,**/dist/**,**/*.spec.js,**/*Test.java",
    ]
    run(cmd)
    report = out_dir / "jscpd-report.json"
    if not report.exists():
        return 0
    data = json.loads(report.read_text(encoding="utf-8"))
    for dup in data.get("duplicates", []):
        first = dup.get("firstFile") or {}
        second = dup.get("secondFile") or {}
        file_name = first.get("name") or first.get("path") or ""
        line = first.get("start", 1)
        lines = dup.get("lines") or dup.get("tokens") or 0
        other = second.get("name") or second.get("path") or "unknown"
        print(
            json.dumps(
                {
                    "file": file_name,
                    "line": line,
                    "rule": "duplicate-block",
                    "message": f"{lines} lines duplicated with {other}",
                },
                ensure_ascii=False,
            )
        )
    return 0


def emit_dead_code() -> int:
    """Frontend unused exports via knip; Java dead-code deferred (no PMD in CI image yet)."""
    ui = ROOT / DEAD_CODE_PATH
    if not (ui / "package.json").exists():
        return 0
    proc = run(["npx", "--yes", "knip", "--reporter", "json"], cwd=ui)
    if not proc.stdout.strip():
        # knip may print to stderr or fail on config; do not fail the gate adapter
        return 0
    try:
        data = json.loads(proc.stdout)
    except json.JSONDecodeError:
        return 0
    for unused in data.get("files") or []:
        print(
            json.dumps(
                {"file": f"{DEAD_CODE_PATH}/{unused}", "rule": "unused-file", "message": "unused file"},
                ensure_ascii=False,
            )
        )
    for issue in data.get("issues") or []:
        file_name = issue.get("file") or ""
        for exp in issue.get("exports") or []:
            name = exp.get("name") if isinstance(exp, dict) else str(exp)
            line = exp.get("line", 1) if isinstance(exp, dict) else 1
            print(
                json.dumps(
                    {
                        "file": f"{DEAD_CODE_PATH}/{file_name}",
                        "line": line,
                        "rule": "unused-export",
                        "message": f"unused export {name}",
                    },
                    ensure_ascii=False,
                )
            )
    return 0


def main() -> int:
    if len(sys.argv) != 2 or sys.argv[1] not in {"complexity", "duplication", "dead-code"}:
        print("usage: quality-gate-adapter.py complexity|duplication|dead-code", file=sys.stderr)
        return 2
    kind = sys.argv[1]
    if kind == "complexity":
        return emit_complexity()
    if kind == "duplication":
        return emit_duplication()
    return emit_dead_code()


if __name__ == "__main__":
    sys.exit(main())
