#!/usr/bin/env python3
"""Basic sanity checks for SQL Server full init scripts."""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


def check(path: Path) -> list[str]:
    errors: list[str] = []
    text = path.read_text(encoding="utf-8", errors="replace")
    if "convert-mysql-full-to-sqlserver.py" not in text.splitlines()[0]:
        errors.append("missing generated-by header")
    if "CREATE TABLE [" not in text:
        errors.append("no CREATE TABLE found")
    create_count = len(re.findall(r"^CREATE TABLE \[", text, flags=re.M))
    if create_count < 50:
        errors.append(f"too few CREATE TABLE ({create_count})")
    if "ENGINE=InnoDB" in text:
        errors.append("residual ENGINE=InnoDB")
    # Residual MySQL identifier quoting in DDL (seed payloads may contain markdown `code`)
    residual_ddl_bt = re.findall(
        r"(?mi)^(?:IF\s+OBJECT_ID|CREATE\s+TABLE|CREATE\s+(?:UNIQUE\s+)?INDEX|ALTER\s+TABLE|PRIMARY\s+KEY).*(`[^`]+`)",
        text,
    )
    if residual_ddl_bt:
        errors.append(f"residual MySQL backticks in DDL ({len(residual_ddl_bt)})")
    if re.search(r"(?mi)^CREATE\s+TABLE\s+`", text):
        errors.append("CREATE TABLE still uses MySQL backticks")
    if "\nGO\n" not in text and not text.endswith("\nGO\n"):
        errors.append("missing GO batch separators")
    if "act_" in text.lower() and re.search(r"CREATE TABLE \[act_", text, re.I):
        errors.append("Flowable act_* tables should be skipped")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        default=[
            Path("forge-server/db/sqlserver/全量初始化SQL.sql"),
            Path("forge-server/db/sqlserver/全量初始化SQL.纯净版.sql"),
        ],
    )
    args = parser.parse_args()
    failed = False
    for path in args.paths:
        if not path.is_file():
            print(f"ERROR: missing {path}", file=sys.stderr)
            failed = True
            continue
        errs = check(path)
        if errs:
            failed = True
            print(f"FAIL {path}:")
            for e in errs:
                print(f"  - {e}")
        else:
            create_count = len(
                re.findall(
                    r"^CREATE TABLE \[",
                    path.read_text(encoding="utf-8", errors="replace"),
                    flags=re.M,
                )
            )
            print(f"OK {path} (create_tables={create_count})")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
