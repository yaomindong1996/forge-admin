#!/usr/bin/env python3
"""Fail when the PostgreSQL full init drops a table or column the pure init has.

MySQL installs apply Flyway after db/mysql/全量初始化SQL.sql. PostgreSQL baselines
at the pure-init freeze and does not replay those migrations, so the PG full
script has to already contain every object in 全量初始化SQL.纯净版.sql.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FULL = ROOT / "db" / "postgresql" / "全量初始化SQL.sql"
PURE = ROOT / "db" / "postgresql" / "全量初始化SQL.纯净版.sql"
CREATE = re.compile(r'CREATE TABLE IF NOT EXISTS "([^"]+)" \((.*?)\n\);', re.S)
COLUMN = re.compile(r'^"([^"]+)"\s+(.*)$')


def columns(sql: str) -> dict[str, dict[str, str]]:
    found: dict[str, dict[str, str]] = {}
    for match in CREATE.finditer(sql):
        table_columns: dict[str, str] = {}
        for line in match.group(2).splitlines():
            column = COLUMN.match(line.strip().rstrip(","))
            if column:
                table_columns[column.group(1)] = column.group(2)
        found[match.group(1)] = table_columns
    return found


def main() -> int:
    full = columns(FULL.read_text(encoding="utf-8"))
    pure = columns(PURE.read_text(encoding="utf-8"))
    errors: list[str] = []
    for table, pure_columns in sorted(pure.items()):
        if table not in full:
            errors.append(f"missing table {table}")
            continue
        for name, definition in pure_columns.items():
            if name not in full[table]:
                errors.append(f"missing column {table}.{name}")
            elif full[table][name] != definition:
                errors.append(
                    f"column type changed {table}.{name}: full={full[table][name]!r} pure={definition!r}"
                )
    if errors:
        print(f"{len(errors)} PostgreSQL full-init gaps:", file=sys.stderr)
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"PostgreSQL full init covers all {len(pure)} pure-init tables")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
