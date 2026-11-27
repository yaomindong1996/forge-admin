#!/usr/bin/env python3
"""Scan Mapper XML for MySQL-only SQL that lacks a PostgreSQL _databaseId branch.

Usage:
  python3 scripts/check-mapper-dialect.py
  python3 scripts/check-mapper-dialect.py --root forge-server
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# Patterns that must not appear outside a choose that already has a postgresql branch.
FORBIDDEN = [
    (re.compile(r"\bFIND_IN_SET\s*\(", re.I), "FIND_IN_SET"),
    (re.compile(r"\bJSON_LENGTH\s*\(", re.I), "JSON_LENGTH"),
    (re.compile(r"\bTIMESTAMPDIFF\s*\(", re.I), "TIMESTAMPDIFF"),
    (re.compile(r"(?<![A-Za-z0-9_])DIV(?![A-Za-z0-9_])"), "DIV"),
    (re.compile(r"CAST\s*\([^)]*?AS\s+CHAR\b", re.I), "CAST(... AS CHAR)"),
    (re.compile(r"jsonb\s*#>>?"), "jsonb #> / #>> (MyBatis # conflict)"),
    (re.compile(r"UPDATE\s+\w+\s+JOIN\b", re.I), "UPDATE ... JOIN (MySQL multi-table UPDATE)"),
]

CHOOSE_WITH_PG = re.compile(
    r"<choose\b[^>]*>.*?_databaseId\s*==\s*['\"]postgresql['\"].*?</choose>",
    re.I | re.S,
)
XML_COMMENT = re.compile(r"<!--.*?-->", re.S)


def scrub(text: str) -> str:
    text = XML_COMMENT.sub("", text)
    # Drop choose blocks that already provide a postgresql branch (MySQL otherwise is fine).
    prev = None
    while prev != text:
        prev = text
        text = CHOOSE_WITH_PG.sub(" ", text)
    return text


def scan_file(path: Path) -> list[str]:
    raw = path.read_text(encoding="utf-8")
    body = scrub(raw)
    hits: list[str] = []
    for pattern, label in FORBIDDEN:
        if pattern.search(body):
            hits.append(label)
    return hits


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "forge-server",
        help="Directory to scan for *Mapper.xml",
    )
    args = parser.parse_args()
    root: Path = args.root
    if not root.is_dir():
        print(f"root not found: {root}", file=sys.stderr)
        return 2

    failures: list[tuple[Path, list[str]]] = []
    skip_parts = {"target", "build", "node_modules", ".git"}
    for path in sorted(root.rglob("*Mapper.xml")):
        if any(part in skip_parts for part in path.parts):
            continue
        hits = scan_file(path)
        if hits:
            failures.append((path, hits))

    if not failures:
        print(f"OK: no naked MySQL-only dialect in Mapper XML under {root}")
        return 0

    print(f"FAIL: {len(failures)} Mapper XML file(s) need _databaseId postgresql branch:")
    for path, hits in failures:
        rel = path
        try:
            rel = path.relative_to(root)
        except ValueError:
            pass
        print(f"  - {rel}: {', '.join(hits)}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
