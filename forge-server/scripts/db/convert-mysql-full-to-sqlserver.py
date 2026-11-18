#!/usr/bin/env python3
"""Convert Forge MySQL 全量/纯净 dump into a SQL Server-oriented full init script.

Usage:
  python3 forge-server/scripts/db/convert-mysql-full-to-sqlserver.py \\
    --input forge-server/db/backup/ForgeAdmin-纯净版-latest.sql \\
    --output forge-server/db/sqlserver/全量初始化SQL.纯净版.sql

Skips Flowable engine tables (act_*/flw_*); keeps Quartz qrtz_* DDL.
Uses GO batch separators for sqlcmd. IDENTITY tables wrap INSERT with IDENTITY_INSERT.
"""
from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path

SKIP_TABLE_PREFIXES = ("act_", "flw_")
MSSQL_IDENT_MAX = 128

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


def should_skip_table(name: str) -> bool:
    lower = name.lower()
    return lower.startswith(SKIP_TABLE_PREFIXES)


def unquote_ident(value: str) -> str:
    return value.replace("`", "").strip()


def quote_ident(value: str) -> str:
    name = unquote_ident(value)
    return "[" + name.replace("]", "]]") + "]"


def shorten_ident(name: str, used: set[str]) -> str:
    candidate = name if len(name) <= MSSQL_IDENT_MAX else name[:MSSQL_IDENT_MAX]
    if candidate not in used and len(name) <= MSSQL_IDENT_MAX:
        used.add(candidate)
        return candidate
    digest = hashlib.md5(name.encode("utf-8")).hexdigest()[:8]
    prefix_len = MSSQL_IDENT_MAX - 1 - len(digest)
    base = name[:prefix_len].rstrip("_")
    candidate = f"{base}_{digest}"
    n = 0
    while candidate in used:
        n += 1
        suffix = hashlib.md5(f"{name}:{n}".encode("utf-8")).hexdigest()[:8]
        candidate = f"{base}_{suffix}"
    used.add(candidate)
    return candidate


def convert_type(mysql_type: str) -> str:
    t = mysql_type.strip()
    t = re.sub(r"\bunsigned\b", "", t, flags=re.I)
    t = re.sub(r"\bzerofill\b", "", t, flags=re.I)
    t = re.sub(r"\s+", " ", t).strip()
    lower = t.lower()

    if lower.startswith("tinyint") or lower.startswith("bit"):
        return "tinyint"
    if lower.startswith("int(") or lower == "int":
        return "int"
    if lower.startswith("bigint"):
        return "bigint"
    if lower.startswith("smallint"):
        return "smallint"
    if lower.startswith("mediumint"):
        return "int"
    if lower.startswith("double"):
        return "float"
    if lower.startswith("float"):
        return "real"
    if lower.startswith("decimal") or lower.startswith("numeric"):
        return re.sub(r"^(decimal|numeric)", "decimal", t, count=1, flags=re.I)
    if lower.startswith("datetime") or lower.startswith("timestamp"):
        return "datetime2"
    if lower.startswith("date"):
        return "date"
    if lower.startswith("time"):
        return "time"
    if (
        lower.startswith("longtext")
        or lower.startswith("mediumtext")
        or lower.startswith("tinytext")
        or lower == "text"
        or lower.startswith("json")
    ):
        return "nvarchar(max)"
    if (
        lower.startswith("longblob")
        or lower.startswith("mediumblob")
        or lower.startswith("blob")
        or lower.startswith("binary")
        or lower.startswith("varbinary")
    ):
        return "varbinary(max)"
    if lower.startswith("varchar"):
        m = re.match(r"varchar\s*\((\d+)\)", lower)
        if m:
            n = int(m.group(1))
            return "nvarchar(max)" if n > 4000 else f"nvarchar({n})"
        return "nvarchar(255)"
    if lower.startswith("char"):
        m = re.match(r"char\s*\((\d+)\)", lower)
        if m:
            n = int(m.group(1))
            return "nchar(max)" if n > 4000 else f"nchar({n})"
        return "nchar(1)"
    if lower.startswith("enum") or lower.startswith("set"):
        return "nvarchar(255)"
    return t


def strip_on_update(rest: str) -> str:
    return re.sub(r"\s+ON UPDATE CURRENT_TIMESTAMP(?:\(\d+\))?", "", rest, flags=re.I)


def strip_auto_increment(rest: str) -> tuple[str, bool]:
    if re.search(r"\bAUTO_INCREMENT\b", rest, flags=re.I):
        return re.sub(r"\s+AUTO_INCREMENT\b", "", rest, flags=re.I), True
    return rest, False


def normalize_default(rest: str, mssql_type: str) -> str:
    numeric = bool(
        re.match(
            r"^(tinyint|smallint|int|bigint|decimal|float|real)\b",
            mssql_type,
            flags=re.I,
        )
    )

    def repl(match: re.Match[str]) -> str:
        raw = match.group(1)
        upper = raw.upper()
        if upper in {"CURRENT_TIMESTAMP", "CURRENT_DATE", "CURRENT_TIME", "NOW()"}:
            return "DEFAULT SYSDATETIME()" if upper in {"NOW()", "CURRENT_TIMESTAMP"} else f"DEFAULT {upper}"
        if numeric and re.fullmatch(r"-?\d+(\.\d+)?", raw):
            return "DEFAULT " + raw
        if numeric and raw.lower() in {"true", "false"}:
            return "DEFAULT " + ("1" if raw.lower() == "true" else "0")
        return "DEFAULT N'" + raw.replace("\\'", "''").replace("'", "''") + "'"

    return re.sub(r"DEFAULT\s+'((?:\\'|[^'])*)'", repl, rest, flags=re.I)


def convert_column_line(line: str) -> tuple[str | None, bool]:
    """Returns (col_sql, is_identity)."""
    match = COL_LINE.match(line.rstrip().rstrip(","))
    if not match:
        return None, False
    name, type_and_rest, _comment = match.group(1), match.group(2), match.group(3)
    rest, is_identity = strip_auto_increment(strip_on_update(type_and_rest))
    type_match = re.match(
        r"^([a-zA-Z]+(?:\s*\([^)]*\))?(?:\s+unsigned|\s+zerofill)*)\s*(.*)$",
        rest.strip(),
        flags=re.I,
    )
    if not type_match:
        mssql_type = convert_type(rest)
        constraints = ""
    else:
        mssql_type = convert_type(type_match.group(1))
        constraints = type_match.group(2).strip()
    constraints = normalize_default(constraints, mssql_type)
    constraints = re.sub(r"\bCHARACTER SET\s+\w+", "", constraints, flags=re.I)
    constraints = re.sub(r"\bCOLLATE\s+\w+", "", constraints, flags=re.I)
    constraints = re.sub(r"\bunsigned\b", "", constraints, flags=re.I)
    constraints = re.sub(r"\bzerofill\b", "", constraints, flags=re.I)
    constraints = re.sub(r"\s+", " ", constraints).strip()
    if is_identity:
        # IDENTITY + NOT NULL; seed INSERTs use IDENTITY_INSERT
        constraints = re.sub(r"\bDEFAULT\b[^,]*", "", constraints, flags=re.I).strip()
        if "NOT NULL" not in constraints.upper():
            constraints = (constraints + " NOT NULL").strip()
        constraints = (constraints + " IDENTITY(1,1)").strip()
        constraints = re.sub(r"\s+", " ", constraints).strip()
    col_sql = f"  {quote_ident(name)} {mssql_type}" + (f" {constraints}" if constraints else "")
    return col_sql, is_identity


def convert_key_line(
    table: str, line: str, used_index_names: set[str]
) -> tuple[str | None, str | None]:
    match = KEY_LINE.match(line.rstrip().rstrip(","))
    if not match:
        return None, None
    kind, key_name, cols = match.group(1).upper(), match.group(2), match.group(3)

    def normalize_index_col(raw: str) -> str:
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
    raw_name = unquote_ident(key_name) if key_name else "idx"
    if raw_name.lower().startswith(f"{table.lower()}_"):
        full_name = raw_name
    else:
        full_name = f"{table}_{raw_name}"
    index_name = shorten_ident(full_name, used_index_names)
    unique = kind.startswith("UNIQUE")
    prefix = "CREATE UNIQUE INDEX" if unique else "CREATE INDEX"
    return None, f"{prefix} {quote_ident(index_name)} ON {quote_ident(table)} ({col_list});"


def convert_mysql_expr(expr: str) -> str:
    expr = IDENT.sub(lambda m: quote_ident(m.group(1)), expr.strip())
    expr = expr.replace("\\'", "''")
    return expr.rstrip().rstrip(";")


def convert_mysql_hex_blobs(sql: str) -> str:
    """Keep MySQL 0xABCD hex as SQL Server binary literals (same 0x form)."""
    # SQL Server accepts 0x... binary literals; X'...' is not needed.
    out: list[str] = []
    i = 0
    n = len(sql)
    in_string = False
    while i < n:
        ch = sql[i]
        if in_string:
            if ch == "\\" and i + 1 < n:
                out.append(ch)
                out.append(sql[i + 1])
                i += 2
                continue
            if ch == "'":
                if i + 1 < n and sql[i + 1] == "'":
                    out.append("''")
                    i += 2
                    continue
                in_string = False
                out.append(ch)
                i += 1
                continue
            out.append(ch)
            i += 1
            continue
        if ch == "'":
            if i > 0 and sql[i - 1] in "Xx" and (i < 2 or not (sql[i - 2].isalnum() or sql[i - 2] == "_")):
                j = i + 1
                while j < n and sql[j] != "'":
                    j += 1
                if j < n:
                    hex_body = sql[i + 1 : j]
                    if re.fullmatch(r"[0-9A-Fa-f]*", hex_body or ""):
                        if out and out[-1] in "Xx":
                            out.pop()
                        out.append(f"0x{hex_body}" if hex_body else "0x")
                        i = j + 1
                        continue
            in_string = True
            out.append(ch)
            i += 1
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def convert_insert(line: str, mysql_vars: dict[str, str] | None = None) -> str:
    def repl_table(match: re.Match[str]) -> str:
        name = match.group(1) or match.group(2)
        return f"INSERT INTO {quote_ident(name)}"

    values_at = re.search(r"\bVALUES\b", line, flags=re.I)
    if values_at:
        head, tail = line[: values_at.start()], line[values_at.start() :]
        head = INSERT_START.sub(repl_table, head, count=1)
        head = IDENT.sub(lambda m: quote_ident(m.group(1)), head)
        line = head + tail
    else:
        line = INSERT_START.sub(repl_table, line, count=1)
        line = IDENT.sub(lambda m: quote_ident(m.group(1)), line)
    line = line.replace("\\'", "''")
    line = line.replace('\\"', '"')
    if mysql_vars:
        for var_name, var_expr in sorted(mysql_vars.items(), key=lambda kv: -len(kv[0])):
            line = re.sub(rf"@{var_name}\b", f"({var_expr})", line)
    line = convert_mysql_hex_blobs(line)
    return line


def statement_ends_at(lines: list[str], start: int) -> int:
    in_string = False
    i = start
    while i < len(lines):
        line = lines[i]
        j = 0
        while j < len(line):
            ch = line[j]
            if in_string:
                if ch == "\\" and j + 1 < len(line):
                    j += 2
                    continue
                if ch == "'":
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


def emit_go(out: list[str]) -> None:
    if out and out[-1] != "GO":
        out.append("GO")


def convert(content: str, *, clean_seed: bool = False) -> str:
    lines = content.splitlines()
    out: list[str] = []
    out.append("-- Generated by convert-mysql-full-to-sqlserver.py")
    if clean_seed:
        out.append("-- SQL Server PURE template init (DDL + essential seed only).")
    else:
        out.append("-- SQL Server full init for Forge Admin (empty database).")
    out.append("-- Flowable act_*/flw_* tables are skipped; engine creates them on startup.")
    out.append("-- Quartz qrtz_* DDL is kept (same as MySQL full/pure).")
    out.append("-- Run with sqlcmd; batches separated by GO.")
    out.append("SET NOCOUNT ON;")
    emit_go(out)
    out.append("")

    i = 0
    skipped_tables: list[str] = []
    converted_tables: list[str] = []
    omitted_seed_tables: set[str] = set()
    used_index_names: set[str] = set()
    identity_tables: set[str] = set()
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
                while i < len(lines) and not ENGINE_TAIL.search(lines[i]) and not ENGINE_TAIL_NO_COMMENT.search(lines[i]):
                    i += 1
                i += 1
                continue

            table_l = unquote_ident(table)
            converted_tables.append(table_l)
            body_cols: list[str] = []
            indexes: list[str] = []
            has_identity = False
            i += 1
            while i < len(lines):
                raw = lines[i]
                if ENGINE_TAIL.search(raw) or ENGINE_TAIL_NO_COMMENT.search(raw):
                    break
                col_sql, is_identity = convert_column_line(raw)
                if col_sql is not None:
                    body_cols.append(col_sql)
                    if is_identity:
                        has_identity = True
                    i += 1
                    continue
                inline, index_sql = convert_key_line(table_l, raw, used_index_names)
                if inline:
                    body_cols.append(inline)
                if index_sql:
                    indexes.append(index_sql)
                i += 1
            out.append(f"-- {table_l}")
            out.append(f"IF OBJECT_ID(N'{table_l}', N'U') IS NULL")
            out.append("BEGIN")
            out.append(f"CREATE TABLE {quote_ident(table_l)} (")
            for idx, col in enumerate(body_cols):
                suffix = "," if idx < len(body_cols) - 1 else ""
                out.append(col + suffix)
            out.append(");")
            out.append("END;")
            emit_go(out)
            for index_sql in indexes:
                out.append(index_sql)
                emit_go(out)
            out.append("")
            if has_identity:
                identity_tables.add(table_l)
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
            buf = lines[i : end_i + 1]
            insert_sql = convert_insert("\n".join(buf), mysql_vars)
            if table_l in identity_tables:
                out.append(f"SET IDENTITY_INSERT {quote_ident(table_l)} ON;")
                out.append(insert_sql)
                out.append(f"SET IDENTITY_INSERT {quote_ident(table_l)} OFF;")
            else:
                out.append(insert_sql)
            emit_go(out)
            out.append("")
            i = end_i + 1
            continue

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
        description="Convert Forge MySQL full/pure init SQL into SQL Server."
    )
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument(
        "--clean-seed",
        action="store_true",
        help="Omit INSERT for clean-db truncate-class tables",
    )
    args = parser.parse_args()
    if not args.input.is_file():
        print(f"ERROR: input not found: {args.input}", file=sys.stderr)
        return 1
    text = args.input.read_text(encoding="utf-8", errors="replace")
    result = convert(text, clean_seed=args.clean_seed)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(result, encoding="utf-8")
    create_count = result.count("CREATE TABLE [")
    insert_count = result.count("\nINSERT INTO ")
    print(
        f"Wrote {args.output} ({len(result)} bytes, "
        f"create_tables={create_count}, inserts≈{insert_count}, clean_seed={args.clean_seed})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
