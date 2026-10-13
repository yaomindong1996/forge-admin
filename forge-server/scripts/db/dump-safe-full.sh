#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

HOST="127.0.0.1"
PORT="3306"
DATABASE="forge_admin_new"
USER="root"
PASSWORD="${MYSQL_PWD:-}"
SSL_MODE=""
OUTPUT=""
OUTPUT_DIR="$FORGE_DIR/db/backup"
GZIP_OUTPUT="false"
INCLUDE_LOG_DATA="false"
INCLUDE_SENSITIVE_DATA="false"
EXCLUDE_DEPENDENT_DATA="true"
DRY_RUN="false"

EXTRA_IGNORE_TABLES=()
EXTRA_IGNORE_REGEXES=()

usage() {
  cat <<'USAGE'
Usage: dump-safe-full.sh [options]

Exports one SQL file containing DDL and safe INSERT data.
DDL and data are skipped for runtime/history tables that are rebuilt or not
needed during normal restore, including act_* Flowable tables and *_version
tables. Data is skipped for log/runtime tables and sensitive configuration
tables by default.

Options:
  --host HOST                    MySQL host, default 127.0.0.1
  --port PORT                    MySQL port, default 3306
  --database DATABASE            Database name, default forge_admin_new
  --user USER                    MySQL user, default root
  --password PASSWORD            MySQL password. Prefer MYSQL_PWD env in CI.
  --ssl-mode MODE                MySQL SSL mode, e.g. REQUIRED for secure connection
  --output FILE                  Output .sql file path
  --output-dir DIR               Output directory when --output is not set
  --gzip                         Compress output to .gz
  --include-log-data             Also dump data from log/runtime tables
  --include-sensitive-data       Also dump data from sensitive config tables
  --no-dependent-exclude         Do not exclude child tables that reference ignored tables
  --ignore-data-table TABLE      Skip INSERT data for a table. Can be repeated.
  --ignore-data-regex REGEX      Skip INSERT data for tables matching regex. Can be repeated.
  --dry-run                      Print planned excluded data tables and commands only
  -h, --help                     Show help

Examples:
  MYSQL_PWD='secret' ./forge-server/scripts/db/dump-safe-full.sh \
    --host 127.0.0.1 --port 3306 --database forge_admin_new --user root --ssl-mode REQUIRED

  ./forge-server/scripts/db/dump-safe-full.sh \
    --database forge_admin_new --ignore-data-table sys_user --gzip
USAGE
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_value() {
  local option="$1"
  local value="${2:-}"
  [[ -n "$value" && "$value" != --* ]] || die "$option requires a value."
}

validate_identifier() {
  local value="$1"
  local label="$2"
  [[ "$value" =~ ^[A-Za-z0-9_]+$ ]] || die "$label must contain only letters, numbers and underscore: $value"
}

escape_sql_literal() {
  printf "%s" "$1" | sed "s/'/''/g"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      require_value "$1" "${2:-}"
      HOST="$2"
      shift 2
      ;;
    --port)
      require_value "$1" "${2:-}"
      PORT="$2"
      shift 2
      ;;
    --database)
      require_value "$1" "${2:-}"
      DATABASE="$2"
      shift 2
      ;;
    --user)
      require_value "$1" "${2:-}"
      USER="$2"
      shift 2
      ;;
    --password)
      require_value "$1" "${2:-}"
      PASSWORD="$2"
      shift 2
      ;;
    --ssl-mode)
      require_value "$1" "${2:-}"
      SSL_MODE="$2"
      shift 2
      ;;
    --output)
      require_value "$1" "${2:-}"
      OUTPUT="$2"
      shift 2
      ;;
    --output-dir)
      require_value "$1" "${2:-}"
      OUTPUT_DIR="$2"
      shift 2
      ;;
    --gzip)
      GZIP_OUTPUT="true"
      shift
      ;;
    --include-log-data)
      INCLUDE_LOG_DATA="true"
      shift
      ;;
    --include-sensitive-data)
      INCLUDE_SENSITIVE_DATA="true"
      shift
      ;;
    --no-dependent-exclude)
      EXCLUDE_DEPENDENT_DATA="false"
      shift
      ;;
    --ignore-data-table)
      require_value "$1" "${2:-}"
      validate_identifier "$2" "--ignore-data-table"
      EXTRA_IGNORE_TABLES+=("$2")
      shift 2
      ;;
    --ignore-data-regex)
      require_value "$1" "${2:-}"
      EXTRA_IGNORE_REGEXES+=("$2")
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

validate_identifier "$DATABASE" "--database"

command -v mysql >/dev/null 2>&1 || die "mysql client is required."
command -v mysqldump >/dev/null 2>&1 || die "mysqldump is required."
if [[ "$GZIP_OUTPUT" == "true" ]]; then
  command -v gzip >/dev/null 2>&1 || die "gzip is required when --gzip is set."
fi

MYSQL_BASE=(mysql --protocol=tcp --host="$HOST" --port="$PORT" --user="$USER" --batch --raw --skip-column-names)
MYSQLDUMP_BASE=(mysqldump --protocol=tcp --host="$HOST" --port="$PORT" --user="$USER" --default-character-set=utf8mb4)

if [[ -n "$SSL_MODE" ]]; then
  MYSQL_BASE+=(--ssl-mode="$SSL_MODE")
  MYSQLDUMP_BASE+=(--ssl-mode="$SSL_MODE")
fi

run_mysql() {
  if [[ -n "$PASSWORD" ]]; then
    MYSQL_PWD="$PASSWORD" "${MYSQL_BASE[@]}" "$@"
  else
    "${MYSQL_BASE[@]}" "$@"
  fi
}

run_mysqldump() {
  if [[ -n "$PASSWORD" ]]; then
    MYSQL_PWD="$PASSWORD" "${MYSQLDUMP_BASE[@]}" "$@"
  else
    "${MYSQLDUMP_BASE[@]}" "$@"
  fi
}

print_command() {
  printf '  '
  printf '%q ' "$@"
  echo
}

COMMON_DUMP_OPTS=(
  --single-transaction
  --skip-lock-tables
  --quick
  --hex-blob
  --complete-insert
)

if mysqldump --help 2>/dev/null | grep -q -- "--set-gtid-purged"; then
  COMMON_DUMP_OPTS+=(--set-gtid-purged=OFF)
fi

if mysqldump --help 2>/dev/null | grep -q -- "--column-statistics"; then
  COMMON_DUMP_OPTS+=(--column-statistics=0)
fi

timestamp="$(date +%Y%m%d_%H%M%S)"
if [[ -z "$OUTPUT" ]]; then
  mkdir -p "$OUTPUT_DIR"
  OUTPUT="$OUTPUT_DIR/${DATABASE}_safe_full_${timestamp}.sql"
fi

if [[ "$GZIP_OUTPUT" == "true" && "$OUTPUT" != *.gz ]]; then
  OUTPUT="${OUTPUT}.gz"
fi

# Flowable 引擎表由应用首次启动创建；社区纯净版/安全全量导出不得带 act_*/flw_*（否则有表无 schema.version → WrongDbException）
full_ignore_regex="^act_|^flw_|(^|_)version$"
log_regex="(^|_)log($|_)|(^|_)logs($|_)|_log$|^log_|_history$|^qrtz_(fired_triggers|scheduler_state|locks)$|^worker_node$|^sys_auth_online_user$|^ai_crud_export_task$|^ai_chat_(record|session)$|^ai_dashboard_generate_record$"
sensitive_tables=(
  ai_business_message_channel
  ai_model
  ai_report_data_connection
  ai_provider
  config_properties
  gen_datasource
  sys_api_config
  sys_email_config
  sys_file_metadata
  sys_file_storage_config
  sys_sms_config
  sys_social_config
  sys_user_social
)

where_parts=()
if [[ "$INCLUDE_LOG_DATA" != "true" ]]; then
  where_parts+=("LOWER(table_name) REGEXP '$(escape_sql_literal "$log_regex")'")
fi

if [[ "$INCLUDE_SENSITIVE_DATA" != "true" ]]; then
  sensitive_in=""
  for table in "${sensitive_tables[@]}"; do
    if [[ -n "$sensitive_in" ]]; then
      sensitive_in+=","
    fi
    sensitive_in+="'$(escape_sql_literal "$table")'"
  done
  where_parts+=("LOWER(table_name) IN ($sensitive_in)")
fi

if [[ ${#EXTRA_IGNORE_TABLES[@]} -gt 0 ]]; then
  for table in "${EXTRA_IGNORE_TABLES[@]}"; do
    where_parts+=("LOWER(table_name) = LOWER('$(escape_sql_literal "$table")')")
  done
fi

if [[ ${#EXTRA_IGNORE_REGEXES[@]} -gt 0 ]]; then
  for regex in "${EXTRA_IGNORE_REGEXES[@]}"; do
    where_parts+=("LOWER(table_name) REGEXP '$(escape_sql_literal "$regex")'")
  done
fi

base_ignore_condition="FALSE"
if [[ ${#where_parts[@]} -gt 0 ]]; then
  base_ignore_condition=""
  for part in "${where_parts[@]}"; do
    if [[ -z "$base_ignore_condition" ]]; then
      base_ignore_condition="($part)"
    else
      base_ignore_condition="$base_ignore_condition OR ($part)"
    fi
  done
fi

if [[ "$EXCLUDE_DEPENDENT_DATA" == "true" ]]; then
  ignore_query="
WITH RECURSIVE ignored(table_name) AS (
  SELECT table_name
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_type = 'BASE TABLE'
    AND ($base_ignore_condition)
  UNION
  SELECT kcu.table_name
  FROM information_schema.key_column_usage kcu
  JOIN ignored i ON i.table_name = kcu.referenced_table_name
  WHERE kcu.table_schema = DATABASE()
    AND kcu.referenced_table_schema = DATABASE()
    AND kcu.referenced_table_name IS NOT NULL
)
SELECT DISTINCT table_name FROM ignored ORDER BY table_name;"
else
  ignore_query="
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND ($base_ignore_condition)
ORDER BY table_name;"
fi

echo "Checking MySQL connection for database $DATABASE ..."
run_mysql "$DATABASE" --execute="SELECT 1" >/dev/null

full_ignore_query="
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND LOWER(table_name) REGEXP '$(escape_sql_literal "$full_ignore_regex")'
ORDER BY table_name;"

full_ignore_tables=()
while IFS= read -r table; do
  [[ -n "$table" ]] && full_ignore_tables+=("$table")
done < <(run_mysql "$DATABASE" --execute="$full_ignore_query")

full_ignore_opts=()
if [[ ${#full_ignore_tables[@]} -gt 0 ]]; then
  for table in "${full_ignore_tables[@]}"; do
    full_ignore_opts+=(--ignore-table="$DATABASE.$table")
  done
fi

ignored_tables=()
while IFS= read -r table; do
  [[ -n "$table" ]] && ignored_tables+=("$table")
done < <(run_mysql "$DATABASE" --execute="$ignore_query")

ignore_opts=()
if [[ ${#ignored_tables[@]} -gt 0 ]]; then
  for table in "${ignored_tables[@]}"; do
    ignore_opts+=(--ignore-table="$DATABASE.$table")
  done
fi

echo "Database: $DATABASE"
echo "Output: $OUTPUT"
echo "Fully skipped tables: ${#full_ignore_tables[@]}"
if [[ ${#full_ignore_tables[@]} -gt 0 ]]; then
  for table in "${full_ignore_tables[@]}"; do
    echo "  - $table"
  done
fi
echo "Data skipped tables: ${#ignored_tables[@]}"
if [[ ${#ignored_tables[@]} -gt 0 ]]; then
  for table in "${ignored_tables[@]}"; do
    echo "  - $table"
  done
fi

if [[ "$DRY_RUN" == "true" ]]; then
  schema_command=("${MYSQLDUMP_BASE[@]}" "${COMMON_DUMP_OPTS[@]}" --routines --events --triggers --no-data)
  data_command=("${MYSQLDUMP_BASE[@]}" "${COMMON_DUMP_OPTS[@]}" --no-create-info --skip-triggers --order-by-primary)
  if [[ ${#full_ignore_opts[@]} -gt 0 ]]; then
    schema_command+=("${full_ignore_opts[@]}")
    data_command+=("${full_ignore_opts[@]}")
  fi
  schema_command+=("$DATABASE")
  if [[ ${#ignore_opts[@]} -gt 0 ]]; then
    data_command+=("${ignore_opts[@]}")
  fi
  data_command+=("$DATABASE")

  echo
  echo "Schema command:"
  print_command "${schema_command[@]}"
  echo "Data command:"
  print_command "${data_command[@]}"
  exit 0
fi

mkdir -p "$(dirname "$OUTPUT")"
tmp_output="${OUTPUT%.gz}.tmp"

cleanup() {
  rm -f "$tmp_output"
}
trap cleanup EXIT

{
  echo "-- Forge safe full dump"
  echo "-- Database: $DATABASE"
  echo "-- Generated at: $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "-- DDL: all tables, routines, events and triggers"
  echo "-- Data: INSERT statements, excluding full-skip, log/runtime and sensitive tables listed below"
  echo "--"
  echo "-- Fully skipped tables:"
  if [[ ${#full_ignore_tables[@]} -eq 0 ]]; then
    echo "--   none"
  else
    for table in "${full_ignore_tables[@]}"; do
      echo "--   $table"
    done
  fi
  echo "--"
  echo "-- Data skipped tables:"
  if [[ ${#ignored_tables[@]} -eq 0 ]]; then
    echo "--   none"
  else
    for table in "${ignored_tables[@]}"; do
      echo "--   $table"
    done
  fi
  echo
  echo "-- Restore with: mysql <options> $DATABASE < this_file.sql"
  echo "SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;"
  echo "SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;"
  echo
  echo "-- Schema"
  schema_dump_args=("${COMMON_DUMP_OPTS[@]}" --routines --events --triggers --no-data)
  if [[ ${#full_ignore_opts[@]} -gt 0 ]]; then
    schema_dump_args+=("${full_ignore_opts[@]}")
  fi
  schema_dump_args+=("$DATABASE")
  run_mysqldump "${schema_dump_args[@]}"
  echo
  echo "-- Data"
  data_dump_args=("${COMMON_DUMP_OPTS[@]}" --no-create-info --skip-triggers --order-by-primary)
  if [[ ${#full_ignore_opts[@]} -gt 0 ]]; then
    data_dump_args+=("${full_ignore_opts[@]}")
  fi
  if [[ ${#ignore_opts[@]} -gt 0 ]]; then
    data_dump_args+=("${ignore_opts[@]}")
  fi
  data_dump_args+=("$DATABASE")
  run_mysqldump "${data_dump_args[@]}"
  echo
  echo "SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;"
  echo "SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;"
} > "$tmp_output"

if [[ "$GZIP_OUTPUT" == "true" ]]; then
  gzip -c "$tmp_output" > "$OUTPUT"
else
  mv "$tmp_output" "$OUTPUT"
fi

trap - EXIT
rm -f "$tmp_output"

echo "Dump completed: $OUTPUT"
