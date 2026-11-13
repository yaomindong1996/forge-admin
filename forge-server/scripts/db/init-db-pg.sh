#!/usr/bin/env bash
# Forge PostgreSQL 数据库初始化：建库 → PG 全量 SQL →（可选）Flyway 增量。
# 兼容 macOS 自带 bash 3.2。默认社区安装仍使用 init-db.sh（MySQL）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
HOST="127.0.0.1"
PORT="5432"
DATABASE="forge_admin"
USER="forge"
PASSWORD="${PGPASSWORD:-}"
RECREATE="false"
MIGRATE="false"
ASSUME_YES="false"
SKIP_ADMIN_INIT="false"

usage() {
  cat <<'USAGE'
Usage: init-db-pg.sh [options]

默认流程：创建数据库 → 执行 db/postgresql/全量初始化SQL.sql。
必需基础数据已包含在全量脚本中；增量由 Flyway（或 --migrate）执行。

Options:
  --host HOST              PostgreSQL host, default 127.0.0.1
  --port PORT              PostgreSQL port, default 5432
  --database DATABASE      Database name, default forge_admin
  --user USER              PostgreSQL user, default forge
  --password PASSWORD      PostgreSQL password（也可用环境变量 PGPASSWORD）
  --skip-admin-init        跳过全量 SQL
  --recreate               先 DROP 再重建数据库
  --migrate                导入后用 Flyway 执行 db/postgresql/migration 增量（需要 Maven）
  --yes                    --recreate 时跳过确认
  -h, --help               Show help

Example:
  PGPASSWORD=*** bash forge-server/scripts/db/init-db-pg.sh --database forge_admin --recreate --migrate
USAGE
}

die() { echo "ERROR: $*" >&2; exit 1; }
require_value() { [[ -n "${2:-}" && "${2:-}" != --* ]] || die "$1 requires a value."; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) require_value "$1" "${2:-}"; HOST="$2"; shift 2 ;;
    --port) require_value "$1" "${2:-}"; PORT="$2"; shift 2 ;;
    --database) require_value "$1" "${2:-}"; DATABASE="$2"; shift 2 ;;
    --user) require_value "$1" "${2:-}"; USER="$2"; shift 2 ;;
    --password) require_value "$1" "${2:-}"; PASSWORD="$2"; shift 2 ;;
    --with-demo)
      echo "WARN: --with-demo 已废弃（db/seed 已移除），忽略。" >&2
      shift
      ;;
    --skip-admin-init) SKIP_ADMIN_INIT="true"; shift ;;
    --recreate) RECREATE="true"; shift ;;
    --migrate) MIGRATE="true"; shift ;;
    --yes) ASSUME_YES="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

ADMIN_INIT_SQL="$FORGE_DIR/db/postgresql/全量初始化SQL.sql"
[[ "$DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || die "--database 只能包含字母、数字和下划线: $DATABASE"

if [[ "$SKIP_ADMIN_INIT" != "true" && ( ! -f "$ADMIN_INIT_SQL" || ! -s "$ADMIN_INIT_SQL" ) ]]; then
  die "必需的 PG 全量 SQL 不存在或为空：$ADMIN_INIT_SQL（先运行 convert-mysql-full-to-pg.py）"
fi
if ! command -v psql >/dev/null 2>&1; then
  die "psql client is required. Install PostgreSQL client and retry."
fi
if [[ "$MIGRATE" == "true" ]] && ! command -v mvn >/dev/null 2>&1; then
  die "--migrate requires Maven (mvn)."
fi

export PGPASSWORD="${PASSWORD}"

psql_admin() {
  psql -v ON_ERROR_STOP=1 -h "$HOST" -p "$PORT" -U "$USER" -d postgres "$@"
}

psql_db() {
  psql -v ON_ERROR_STOP=1 -h "$HOST" -p "$PORT" -U "$USER" -d "$DATABASE" "$@"
}

if [[ "$RECREATE" == "true" ]]; then
  if [[ "$ASSUME_YES" != "true" ]]; then
    read -r -p "将 DROP DATABASE $DATABASE，请输入库名确认: " confirm
    [[ "$confirm" == "$DATABASE" ]] || die "确认失败，已中止。"
  fi
  psql_admin -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DATABASE' AND pid <> pg_backend_pid();" || true
  psql_admin -c "DROP DATABASE IF EXISTS $DATABASE;"
fi

psql_admin -tc "SELECT 1 FROM pg_database WHERE datname = '$DATABASE'" | grep -q 1 \
  || psql_admin -c "CREATE DATABASE $DATABASE ENCODING 'UTF8' TEMPLATE template0;"

if [[ "$SKIP_ADMIN_INIT" != "true" ]]; then
  echo "Importing $ADMIN_INIT_SQL ..."
  psql_db -f "$ADMIN_INIT_SQL"
fi

if [[ "$MIGRATE" == "true" ]]; then
  echo "Running Flyway migrations from db/postgresql/migration ..."
  (
    cd "$FORGE_DIR/forge-admin-server"
    mvn -q flyway:migrate \
      -Dflyway.url="jdbc:postgresql://${HOST}:${PORT}/${DATABASE}" \
      -Dflyway.user="$USER" \
      -Dflyway.password="$PASSWORD" \
      -Dflyway.locations="filesystem:$FORGE_DIR/db/postgresql/migration" \
      -Dflyway.table="forge_schema_history" \
      -Dflyway.baselineOnMigrate=true \
      -Dflyway.baselineVersion=1.0.233
  )
fi

echo "PostgreSQL init done: $DATABASE @ $HOST:$PORT"
