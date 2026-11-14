#!/usr/bin/env python3
"""Convert Forge MySQL 全量初始化SQL.sql into a PostgreSQL-oriented full init script.

Usage:
  python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \\
    --input forge-server/db/mysql/全量初始化SQL.sql \\
    --output forge-server/db/postgresql/全量初始化SQL.sql

  # Pure template (DDL + foundation seed; omit clean-db truncate-class rows):
  python3 forge-server/scripts/db/convert-mysql-full-to-pg.py \\
    --input forge-server/db/mysql/全量初始化SQL.sql \\
    --output forge-server/db/postgresql/全量初始化SQL.纯净版.sql \\
    --clean-seed

Skips Flowable engine tables (act_*/flw_*). Keeps framework *_version tables.
Does not claim byte-perfect semantic parity for every MySQL quirk; output is the
repeatable baseline for empty PG installs. Re-run after refreshing the MySQL full dump.
"""
from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path

# Flowable + Quartz runtime tables: engine/scheduler recreate or re-register.
SKIP_TABLE_PREFIXES = ("act_", "flw_", "qrtz_")
# PostgreSQL NAMEDATALEN-1
PG_IDENT_MAX = 63

# Align with clean-db.sh TRUNCATE_REGEXES: drop seed rows for a "pure" template dump.
# DDL is always kept; only INSERT payloads for these tables are omitted.
CLEAN_SEED_TRUNCATE_REGEXES = (
    r"(^|_)logs?$",
    r"_history$",
    r"_record$",
    r"_outbox$",
    r"_inbox$",
    r"_idempotency$",
    r"_repair$",
    r"^sys_auth_online_user$",
    r"^worker_node$",
    r"^qrtz_(fired_triggers|scheduler_state)$",
    r"^sys_job_api_token$",
    r"^sys_plugin_task$",
    r"^sys_plugin_build$",
    r"^sys_plugin_task_review$",
    r"^sys_plugin_artifact_registration$",
    r"^sys_plugin_delivery$",
    r"^sys_plugin_delivery_target$",
    r"^ai_crud_export_task$",
    r"^ai_chat_",
    r"^ai_agent_event$",
    r"^sys_message$",
    r"^sys_message_receiver$",
    r"^sys_notice",
    r"^sys_file_(metadata|group|storage_config)$",
    r"^sys_flow_",
    r"^act_",
    r"^flw_",
    r"^ai_business_",
    r"^ai_crud_config",
    r"^ai_lowcode_(model|publish_task)$",
    r"^ai_custom_query_scheme$",
    r"^ai_data_audit_",
    r"^ai_application_",
    r"^ai_capability",
    r"^ai_report_",
    r"^ai_dashboard_",
    r"^ai_(provider|model|model_capability|model_route_policy|model_route_target)$",
    r"^ai_knowledge",
    r"^ai_store_instance$",
    r"^ai_skill",
    r"^ai_agent_(skill|tool_config|tool_permission)$",
    r"^gen_(table|table_column|datasource)$",
    r"^sys_print_",
    r"^sys_external_(system|api)$",
    r"^sys_user_social$",
    r"^sys_social_(org_mapping|post_mapping|sync_issue|tag|tag_member|todo_link|callback_event)$",
    r"^sys_post$",
    r"^sys_user_post$",
    r"^sys_employee$",
)
_CLEAN_SEED_TRUNCATE = [re.compile(p, re.I) for p in CLEAN_SEED_TRUNCATE_REGEXES]


def should_omit_seed(table: str) -> bool:
    name = table.lower()
    return any(rx.search(name) for rx in _CLEAN_SEED_TRUNCATE)


def is_mysql_dump_noise_comment(stripped: str) -> bool:
    if not stripped.startswith("--"):
        return False
    noise = (
        "MySQL dump",
        "Host:",
        "Server version",
        "Table structure for table",
        "Dumping data for table",
        "Current Database:",
        "Dump completed",
        "Temporary table structure",
        "POSITIONING",
    )
    return any(token in stripped for token in noise)

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
SET_VAR_START = re.compile(r"^SET\s+@([A-Za-z0-9_]+)\s*=\s*(.*)$", re.I)


def shorten_pg_ident(name: str, used: set[str]) -> str:
    """Keep identifiers <=63 chars and unique within the generated script."""
    candidate = name if len(name) <= PG_IDENT_MAX else name[:PG_IDENT_MAX]
    if candidate not in used and len(name) <= PG_IDENT_MAX:
        used.add(candidate)
        return candidate
    digest = hashlib.md5(name.encode("utf-8")).hexdigest()[:8]
    prefix_len = PG_IDENT_MAX - 1 - len(digest)
    base = name[:prefix_len].rstrip("_")
    candidate = f"{base}_{digest}"
    # Extremely unlikely collision loop if truncated+hash still clashes
    n = 0
    while candidate in used:
        n += 1
        suffix = hashlib.md5(f"{name}:{n}".encode("utf-8")).hexdigest()[:8]
        candidate = f"{base}_{suffix}"
    used.add(candidate)
    return candidate


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
        # Full-dump seed data often contains '' / non-JSON fragments that reject on jsonb.
        # Store as text for init compatibility; app layer still reads JSON strings.
        return "text"
    if lower.startswith("varchar") or lower.startswith("char"):
        return t
    if lower.startswith("enum") or lower.startswith("set"):
        return "varchar(255)"
    if lower.startswith("bit"):
        return "smallint"
    return t


def strip_on_update(rest: str) -> str:
    return re.sub(r"\s+ON UPDATE CURRENT_TIMESTAMP(?:\(\d+\))?", "", rest, flags=re.I)


def strip_auto_increment(rest: str) -> tuple[str, bool]:
    """Remove AUTO_INCREMENT marker and report whether it was present."""
    if re.search(r"\bAUTO_INCREMENT\b", rest, flags=re.I):
        return re.sub(r"\s+AUTO_INCREMENT\b", "", rest, flags=re.I), True
    return rest, False


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


def convert_column_line(
    line: str,
) -> tuple[str | None, tuple[str, str] | None, str | None]:
    """Returns (col_sql, comment_pair, identity_col_name_or_none)."""
    match = COL_LINE.match(line.rstrip().rstrip(","))
    if not match:
        return None, None, None
    name, type_and_rest, comment = match.group(1), match.group(2), match.group(3)
    rest, is_identity = strip_auto_increment(strip_on_update(type_and_rest))
    # split type token from constraints (include optional unsigned/zerofill in type)
    type_match = re.match(
        r"^([a-zA-Z]+(?:\s*\([^)]*\))?(?:\s+unsigned|\s+zerofill)*)\s*(.*)$",
        rest.strip(),
        flags=re.I,
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
    # safety net if unsigned leaked into constraints
    constraints = re.sub(r"\bunsigned\b", "", constraints, flags=re.I)
    constraints = re.sub(r"\bzerofill\b", "", constraints, flags=re.I)
    constraints = re.sub(r"\s+", " ", constraints).strip()
    identity_col = unquote_ident(name) if is_identity else None
    if is_identity:
        # BY DEFAULT allows seed dumps that still supply explicit primary keys.
        constraints = (constraints + " GENERATED BY DEFAULT AS IDENTITY").strip()
    col_sql = f"  {quote_ident(name)} {pg_type}" + (f" {constraints}" if constraints else "")
    comment_pair = (unquote_ident(name), comment.replace("\\'", "'")) if comment else None
    return col_sql, comment_pair, identity_col


def setval_sql(table: str, identity_col: str) -> str:
    # Keep sequence ahead of explicit seed PKs so later INSERT...SELECT without id works.
    # Empty table: setval(..., 1, false) => next nextval is 1 (0 is out of bounds).
    # Populated: setval(..., max, true) => next nextval is max+1.
    qtable = quote_ident(table)
    qcol = quote_ident(identity_col)
    return (
        "SELECT CASE\n"
        f"  WHEN (SELECT MAX({qcol}) FROM {qtable}) IS NULL\n"
        f"    THEN setval(pg_get_serial_sequence('{table}', '{identity_col}'), 1, false)\n"
        f"  ELSE setval(pg_get_serial_sequence('{table}', '{identity_col}'),\n"
        f"              (SELECT MAX({qcol}) FROM {qtable}), true)\n"
        "END;"
    )


def convert_key_line(
    table: str, line: str, used_index_names: set[str]
) -> tuple[str | None, str | None]:
    """Returns (inline_constraint_or_none, create_index_sql_or_none)."""
    match = KEY_LINE.match(line.rstrip().rstrip(","))
    if not match:
        return None, None
    kind, key_name, cols = match.group(1).upper(), match.group(2), match.group(3)

    def normalize_index_col(raw: str) -> str:
        # MySQL: `col`(255) [ASC|DESC] / col DESC — strip prefix length, keep order
        c = raw.strip()
        order = ""
        order_match = re.search(r"\s+(ASC|DESC)\s*$", c, flags=re.I)
        if order_match:
            order = " " + order_match.group(1).upper()
            c = c[: order_match.start()].strip()
        c = re.sub(r"^`([^`]+)`(?:\s*\(\d+\))?$", r"\1", c)
        c = re.sub(r"^([A-Za-z0-9_]+)(?:\s*\(\d+\))?$", r"\1", c)
        return quote_ident(c) + order

    col_list = ", ".join(normalize_index_col(c) for c in cols.split(","))
    if kind == "PRIMARY KEY":
        return f"  PRIMARY KEY ({col_list})", None
    # PostgreSQL index names are schema-global (unlike MySQL per-table). Always
    # prefix with the table name so duplicated KEY names across tables stay unique.
    raw_name = unquote_ident(key_name) if key_name else "idx"
    if raw_name.startswith(f"{table}_"):
        full_name = raw_name
    else:
        full_name = f"{table}_{raw_name}"
    index_name = shorten_pg_ident(full_name, used_index_names)
    unique = kind.startswith("UNIQUE")
    prefix = "CREATE UNIQUE INDEX" if unique else "CREATE INDEX"
    return None, f"{prefix} {quote_ident(index_name)} ON {quote_ident(table)} ({col_list});"


def convert_mysql_expr(expr: str) -> str:
    """Lightweight expression cleanup for SET @var subqueries reused in PG."""
    expr = IDENT.sub(lambda m: quote_ident(m.group(1)), expr.strip())
    expr = expr.replace("\\'", "''")
    # LIMIT inside scalar subquery is fine in PG
    return expr.rstrip().rstrip(";")


def convert_insert(line: str, mysql_vars: dict[str, str] | None = None) -> str:
    # INSERT INTO t (...) VALUES (...);  or INSERT INTO `t` (...)
    def repl_table(match: re.Match[str]) -> str:
        name = match.group(1) or match.group(2)
        return f"INSERT INTO {quote_ident(name)}"

    # Only rewrite backticks in the INSERT header (table/column list), never inside
    # VALUES payloads — chat/seed content often contains markdown code fences.
    values_at = re.search(r"\bVALUES\b", line, flags=re.I)
    if values_at:
        head, tail = line[: values_at.start()], line[values_at.start() :]
        head = INSERT_START.sub(repl_table, head, count=1)
        head = IDENT.sub(lambda m: quote_ident(m.group(1)), head)
        line = head + tail
    else:
        # INSERT...SELECT seed scripts: rewrite idents globally except inside quotes is hard;
        # these scripts are hand-written without backticks in values.
        line = INSERT_START.sub(repl_table, line, count=1)
        line = IDENT.sub(lambda m: quote_ident(m.group(1)), line)
    # MySQL escaped quotes \' -> ''
    line = line.replace("\\'", "''")
    line = line.replace('\\"', '"')
    if mysql_vars:
        # PG treats @x as absolute-value of column x; inline captured SET @x expressions.
        for var_name, var_expr in sorted(mysql_vars.items(), key=lambda kv: -len(kv[0])):
            line = re.sub(rf"@{var_name}\b", f"({var_expr})", line)
    return line


def statement_ends_at(lines: list[str], start: int) -> int:
    """Return index of the line that finishes the SQL statement starting at start.

    Seed dumps embed example SQL (lines ending with ';') inside string literals;
    only treat a semicolon outside single quotes as the real terminator.
    """
    in_string = False
    i = start
    while i < len(lines):
        line = lines[i]
        j = 0
        while j < len(line):
            ch = line[j]
            if in_string:
                if ch == "\\" and j + 1 < len(line):
                    # MySQL dump escape — skip next char inside strings
                    j += 2
                    continue
                if ch == "'":
                    # MySQL '' escaped quote inside strings
                    if j + 1 < len(line) and line[j + 1] == "'":
                        j += 2
                        continue
                    in_string = False
                j += 1
                continue
            if ch == "'":
                in_string = True
                j += 1
                continue
            if ch == ";":
                return i
            j += 1
        i += 1
    return len(lines) - 1


def convert(content: str, *, clean_seed: bool = False) -> str:
    lines = content.splitlines()
    out: list[str] = []
    out.append("-- Generated by convert-mysql-full-to-pg.py")
    if clean_seed:
        out.append("-- PostgreSQL PURE template init (DDL + essential seed only).")
        out.append("-- Seed rows omitted for clean-db.sh truncate-class tables (logs/flow/lowcode/demo…).")
        out.append("-- Still includes admin/tenant/dict/resource/config style foundation data from the MySQL dump.")
    else:
        out.append("-- PostgreSQL full init for Forge Admin (empty database).")
    out.append("-- Flowable act_*/flw_* tables are skipped; engine creates them on startup.")
    out.append("SET client_encoding = 'UTF8';")
    out.append("")

    i = 0
    skipped_tables: list[str] = []
    converted_tables: list[str] = []
    omitted_seed_tables: set[str] = set()
    used_index_names: set[str] = set()
    identity_cols: dict[str, str] = {}
    mysql_vars: dict[str, str] = {}

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if not stripped or stripped.startswith("ALTER DATABASE") or stripped.startswith("SET NAMES"):
            i += 1
            continue
        if is_mysql_dump_noise_comment(stripped):
            i += 1
            continue

        set_var = SET_VAR_START.match(stripped)
        if set_var:
            var_name = set_var.group(1)
            end_i = statement_ends_at(lines, i)
            block = "\n".join(lines[i : end_i + 1]).strip()
            # SET @name = <expr>;
            assign = re.match(
                rf"^SET\s+@{re.escape(var_name)}\s*=\s*(.*);\s*$",
                block,
                flags=re.I | re.S,
            )
            if assign:
                mysql_vars[var_name] = convert_mysql_expr(assign.group(1))
            i = end_i + 1
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
            table_identity: str | None = None
            i += 1
            while i < len(lines):
                raw = lines[i]
                engine = ENGINE_TAIL.search(raw) or ENGINE_TAIL_NO_COMMENT.search(raw)
                if engine:
                    if ENGINE_TAIL.search(raw):
                        table_comment = ENGINE_TAIL.search(raw).group(1).replace("\\'", "'")
                    break
                col_sql, comment_pair, identity_col = convert_column_line(raw)
                if col_sql is not None:
                    body_cols.append(col_sql)
                    if comment_pair:
                        comments.append(comment_pair)
                    if identity_col and table_identity is None:
                        table_identity = identity_col
                    i += 1
                    continue
                inline, index_sql = convert_key_line(table_l, raw, used_index_names)
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
            if table_identity:
                identity_cols[table_l] = table_identity
            i += 1
            continue

        insert = INSERT_START.match(stripped)
        if insert:
            table = insert.group(1) or insert.group(2)
            end_i = statement_ends_at(lines, i)
            if should_skip_table(table):
                i = end_i + 1
                continue
            table_l = unquote_ident(table)
            if clean_seed and should_omit_seed(table_l):
                omitted_seed_tables.add(table_l)
                i = end_i + 1
                continue
            identity_col = identity_cols.get(table_l)
            # Sync sequence before inserts that omit the identity column.
            if identity_col:
                out.append(setval_sql(table_l, identity_col))
            buf = lines[i : end_i + 1]
            out.append(convert_insert("\n".join(buf), mysql_vars))
            if identity_col:
                out.append(setval_sql(table_l, identity_col))
            out.append("")
            i = end_i + 1
            continue

        # keep non-noise comments
        if stripped.startswith("--") and "definition" not in stripped:
            out.append(stripped)
        i += 1

    out.append("")
    summary = (
        f"-- converted_tables={len(converted_tables)} "
        f"skipped_flowable={len(skipped_tables)}"
    )
    if clean_seed:
        summary += f" omitted_seed_tables={len(omitted_seed_tables)}"
    out.append(summary)
    return "\n".join(out) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert Forge MySQL full init SQL into PostgreSQL. "
        "Use --clean-seed for a pure template (DDL + foundation seed only)."
    )
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument(
        "--clean-seed",
        action="store_true",
        help="Omit INSERT for clean-db truncate-class tables (logs/flow/lowcode/demo…)",
    )
    args = parser.parse_args()
    if not args.input.is_file():
        print(f"ERROR: input not found: {args.input}", file=sys.stderr)
        return 1
    text = args.input.read_text(encoding="utf-8", errors="replace")
    result = convert(text, clean_seed=args.clean_seed)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(result, encoding="utf-8")
    create_count = result.count("CREATE TABLE IF NOT EXISTS")
    insert_count = result.count("\nINSERT INTO ")
    print(
        f"Wrote {args.output} ({len(result)} bytes, "
        f"create_tables={create_count}, inserts≈{insert_count}, clean_seed={args.clean_seed})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
