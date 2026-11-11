#!/usr/bin/env python3
"""Convert Forge MySQL 全量初始化SQL.sql into a PostgreSQL-oriented full init script.

Usage:
  python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \\
    --input forge-server/db/全量初始化SQL.sql \\
    --output forge-server/db/postgresql/全量初始化SQL.sql

Skips Flowable engine tables (act_*/flw_*). Keeps framework *_version tables.
Does not claim byte-perfect semantic parity for every MySQL quirk; output is the
repeatable baseline for empty PG installs. Re-run after refreshing the MySQL full dump.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

SKIP_TABLE_PREFIXES = ("act_", "flw_")

IDENT = re.compile(r"`([^`]+)`")
CREATE_START = re.compile(r"^CREATE TABLE `([^`]+)`\s*\(", re.I)
INSERT_START = re.compile(r"^INSERT INTO\s+(?:`([^`]+)`|([A-Za-z0-9_]+))", re.I)
ENGINE_TAIL = re.compile(
    r"\)\s*ENGINE=InnoDB[^;]*COMMENT='((?:\\'|[^'])*)'\s*;\s*$",
    re.I,
)
ENGINE_TAIL_NO_COMMENT = re.compile(r"\)\s*ENGINE=InnoDB[^;]*;\s*$", re.I)
COL_LINE = re.compile(
    r"^\s*`([^`]+)`\s+(.+?)(?:\s+COMMENT\s+'((?:\\'|[^'])*)')?\s*,?\s*$",
    re.I,
)
KEY_LINE = re.compile(
    r"^\s*(PRIMARY KEY|UNIQUE KEY|KEY|UNIQUE INDEX|INDEX)\s*(?:`([^`]+)`)?\s*\((.+)\)\s*,?\s*$",
    re.I,
)


def should_skip_table(name: str) -> bool:
    lower = name.lower()
    return lower.startswith(SKIP_TABLE_PREFIXES)


def unquote_ident(value: str) -> str:
    return value.replace("`", "").strip().lower()


def quote_ident(value: str) -> str:
    return '"' + unquote_ident(value).replace('"', '""') + '"'


def convert_type(mysql_type: str) -> str:
    t = mysql_type.strip()
    # strip unsigned / zerofill
    t = re.sub(r"\bunsigned\b", "", t, flags=re.I)
    t = re.sub(r"\bzerofill\b", "", t, flags=re.I)
    t = re.sub(r"\s+", " ", t).strip()
    lower = t.lower()

    if lower.startswith("tinyint"):
        return "smallint"
    if lower.startswith("int(") or lower == "int":
        return "integer"
    if lower.startswith("bigint"):
        return "bigint"
    if lower.startswith("smallint"):
        return "smallint"
    if lower.startswith("mediumint"):
        return "integer"
    if lower.startswith("double") or lower.startswith("float"):
        return "double precision" if lower.startswith("double") else "real"
    if lower.startswith("decimal") or lower.startswith("numeric"):
        return re.sub(r"^(decimal|numeric)", "numeric", t, count=1, flags=re.I)
    if lower.startswith("datetime") or lower.startswith("timestamp"):
        return "timestamp"
    if lower.startswith("date"):
        return "date"
    if lower.startswith("time"):
        return "time"
    if lower.startswith("longtext") or lower.startswith("mediumtext") or lower.startswith("tinytext") or lower == "text":
        return "text"
    if lower.startswith("longblob") or lower.startswith("mediumblob") or lower.startswith("blob") or lower.startswith("binary") or lower.startswith("varbinary"):
        return "bytea"
    if lower.startswith("json"):
        return "jsonb"
    if lower.startswith("varchar") or lower.startswith("char"):
        return t
    if lower.startswith("enum") or lower.startswith("set"):
        return "varchar(255)"
    if lower.startswith("bit"):
        return "smallint"
    return t


def strip_on_update(rest: str) -> str:
    return re.sub(r"\s+ON UPDATE CURRENT_TIMESTAMP(?:\(\d+\))?", "", rest, flags=re.I)


def strip_auto_increment(rest: str) -> str:
    return re.sub(r"\s+AUTO_INCREMENT\b", "", rest, flags=re.I)


def normalize_default(rest: str, pg_type: str) -> str:
    numeric = bool(re.match(
        r"^(smallint|integer|bigint|numeric|double precision|real)\b",
        pg_type,
        flags=re.I,
    ))

    def repl(match: re.Match[str]) -> str:
        raw = match.group(1)
        upper = raw.upper()
        if upper in {"CURRENT_TIMESTAMP", "CURRENT_DATE", "CURRENT_TIME", "NOW()"}:
            return "DEFAULT CURRENT_TIMESTAMP" if upper == "NOW()" else f"DEFAULT {upper}"
        if numeric and re.fullmatch(r"-?\d+(\.\d+)?", raw):
            return "DEFAULT " + raw
        return "DEFAULT '" + raw.replace("\\'", "''") + "'"

    return re.sub(r"DEFAULT\s+'((?:\\'|[^'])*)'", repl, rest, flags=re.I)


def convert_column_line(line: str) -> tuple[str | None, tuple[str, str] | None]:
    match = COL_LINE.match(line.rstrip().rstrip(","))
    if not match:
        return None, None
    name, type_and_rest, comment = match.group(1), match.group(2), match.group(3)
    rest = strip_auto_increment(strip_on_update(type_and_rest))
    # split type token from constraints
    type_match = re.match(
        r"^([a-zA-Z]+(?:\s*\([^)]*\))?)\s*(.*)$",
        rest.strip(),
    )
    if not type_match:
        pg_type = convert_type(rest)
        constraints = ""
    else:
        pg_type = convert_type(type_match.group(1))
        constraints = type_match.group(2).strip()
    constraints = normalize_default(constraints, pg_type)
    constraints = re.sub(r"\bCHARACTER SET\s+\w+", "", constraints, flags=re.I)
    constraints = re.sub(r"\bCOLLATE\s+\w+", "", constraints, flags=re.I)
    constraints = re.sub(r"\s+", " ", constraints).strip()
    col_sql = f"  {quote_ident(name)} {pg_type}" + (f" {constraints}" if constraints else "")
    comment_pair = (unquote_ident(name), comment.replace("\\'", "'")) if comment else None
    return col_sql, comment_pair


def convert_key_line(table: str, line: str) -> tuple[str | None, str | None]:
    """Returns (inline_constraint_or_none, create_index_sql_or_none)."""
    match = KEY_LINE.match(line.rstrip().rstrip(","))
    if not match:
        return None, None
    kind, key_name, cols = match.group(1).upper(), match.group(2), match.group(3)
    col_list = ", ".join(quote_ident(c.strip()) for c in cols.split(","))
    if kind == "PRIMARY KEY":
        return f"  PRIMARY KEY ({col_list})", None
    index_name = unquote_ident(key_name) if key_name else f"idx_{table}_auto"
    unique = kind.startswith("UNIQUE")
    prefix = "CREATE UNIQUE INDEX" if unique else "CREATE INDEX"
    return None, f"{prefix} {quote_ident(index_name)} ON {quote_ident(table)} ({col_list});"


def convert_insert(line: str) -> str:
    # INSERT INTO t (...) VALUES (...);  or INSERT INTO `t` (...)
    def repl_table(match: re.Match[str]) -> str:
        name = match.group(1) or match.group(2)
        return f"INSERT INTO {quote_ident(name)}"

    line = INSERT_START.sub(repl_table, line, count=1)
    line = IDENT.sub(lambda m: quote_ident(m.group(1)), line)
    # MySQL escaped quotes \' -> ''
    line = line.replace("\\'", "''")
    line = line.replace('\\"', '"')
    return line


def convert(content: str) -> str:
    lines = content.splitlines()
    out: list[str] = []
    out.append("-- Generated by convert-mysql-full-to-pg.py")
    out.append("-- PostgreSQL full init for Forge Admin (empty database).")
    out.append("-- Flowable act_*/flw_* tables are skipped; engine creates them on startup.")
    out.append("SET client_encoding = 'UTF8';")
    out.append("")

    i = 0
    skipped_tables: list[str] = []
    converted_tables: list[str] = []

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if not stripped or stripped.startswith("ALTER DATABASE") or stripped.startswith("SET NAMES"):
            i += 1
            continue

        create = CREATE_START.match(stripped)
        if create:
            table = create.group(1)
            if should_skip_table(table):
                skipped_tables.append(table)
                # skip until ENGINE line ending with ;
                while i < len(lines) and not ENGINE_TAIL.search(lines[i]) and not ENGINE_TAIL_NO_COMMENT.search(lines[i]):
                    i += 1
                i += 1
                continue

            table_l = unquote_ident(table)
            converted_tables.append(table_l)
            body_cols: list[str] = []
            indexes: list[str] = []
            comments: list[tuple[str, str]] = []
            table_comment: str | None = None
            i += 1
            while i < len(lines):
                raw = lines[i]
                engine = ENGINE_TAIL.search(raw) or ENGINE_TAIL_NO_COMMENT.search(raw)
                if engine:
                    if ENGINE_TAIL.search(raw):
                        table_comment = ENGINE_TAIL.search(raw).group(1).replace("\\'", "'")
                    break
                col_sql, comment_pair = convert_column_line(raw)
                if col_sql is not None:
                    body_cols.append(col_sql)
                    if comment_pair:
                        comments.append(comment_pair)
                    i += 1
                    continue
                inline, index_sql = convert_key_line(table_l, raw)
                if inline:
                    body_cols.append(inline)
                if index_sql:
                    indexes.append(index_sql)
                i += 1
            out.append(f"-- {table_l}")
            out.append(f"CREATE TABLE IF NOT EXISTS {quote_ident(table_l)} (")
            # join columns with commas
            for idx, col in enumerate(body_cols):
                suffix = "," if idx < len(body_cols) - 1 else ""
                out.append(col + suffix)
            out.append(");")
            if table_comment:
                out.append(
                    f"COMMENT ON TABLE {quote_ident(table_l)} IS '{table_comment.replace(chr(39), chr(39)+chr(39))}';"
                )
            for col_name, comment in comments:
                out.append(
                    f"COMMENT ON COLUMN {quote_ident(table_l)}.{quote_ident(col_name)} IS '{comment.replace(chr(39), chr(39)+chr(39))}';"
                )
            out.extend(indexes)
            out.append("")
            i += 1
            continue

        insert = INSERT_START.match(stripped)
        if insert:
            table = insert.group(1) or insert.group(2)
            if should_skip_table(table):
                while i < len(lines) and not lines[i].rstrip().endswith(";"):
                    i += 1
                i += 1
                continue
            buf = [line]
            while not lines[i].rstrip().endswith(";"):
                i += 1
                if i >= len(lines):
                    break
                buf.append(lines[i])
            out.append(convert_insert("\n".join(buf)))
            out.append("")
            i += 1
            continue

        # keep comments that are not mysql dump noise
        if stripped.startswith("--") and "definition" not in stripped:
            out.append(stripped)
        i += 1

    out.append("")
    out.append(f"-- converted_tables={len(converted_tables)} skipped_flowable={len(skipped_tables)}")
    return "\n".join(out) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if not args.input.is_file():
        print(f"ERROR: input not found: {args.input}", file=sys.stderr)
        return 1
    text = args.input.read_text(encoding="utf-8", errors="replace")
    result = convert(text)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(result, encoding="utf-8")
    create_count = result.count("CREATE TABLE IF NOT EXISTS")
    print(f"Wrote {args.output} ({len(result)} bytes, create_tables={create_count})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
