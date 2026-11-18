#!/usr/bin/env bash
# Forge SQL Server 数据库初始化：建库 → 全量 SQL（sqlcmd）→（可选）Flyway 增量。
# 兼容 macOS 自带 bash 3.2。默认社区安装仍使用 init-db.sh（MySQL）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
HOST="127.0.0.1"
PORT="1433"
DATABASE="forge_admin"
USER="sa"
PASSWORD="${SQLCMDPASSWORD:-${MSSQL_SA_PASSWORD:-}}"
RECREATE="false"
MIGRATE="false"
ASSUME_YES="false"
SKIP_ADMIN_INIT="false"
ADMIN_INIT_SQL=""
ENCRYPT="false"

usage() {
  cat <<'USAGE'
Usage: init-db-sqlserver.sh [options]

默认流程：创建数据库 → 执行 db/sqlserver/全量初始化SQL.sql。
必需基础数据已包含在全量脚本中；增量由 Flyway（或 --migrate）执行。

Options:
  --host HOST              SQL Server host, default 127.0.0.1
  --port PORT              SQL Server port, default 1433
  --database DATABASE      Database name, default forge_admin
  --user USER              SQL Server user, default sa
  --password PASSWORD      Password（也可用 SQLCMDPASSWORD / MSSQL_SA_PASSWORD）
  --encrypt true|false     sqlcmd -C/-N 加密，default false（本机开发常用）
  --skip-admin-init        跳过全量 SQL
  --init-sql FILE          指定全量 SQL（默认 db/sqlserver/全量初始化SQL.sql）
  --pure                   使用纯净版全量（db/sqlserver/全量初始化SQL.纯净版.sql）
  --recreate               先 DROP 再重建数据库
  --migrate                导入后用 Flyway 执行 db/sqlserver/migration 增量（需要 Maven）
  --yes                    --recreate 时跳过确认
  -h, --help               Show help

Example:
  SQLCMDPASSWORD=*** bash forge-server/scripts/db/init-db-sqlserver.sh --database forge_admin --recreate --migrate
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
    --encrypt) require_value "$1" "${2:-}"; ENCRYPT="$2"; shift 2 ;;
    --skip-admin-init) SKIP_ADMIN_INIT="true"; shift ;;
    --init-sql) require_value "$1" "${2:-}"; ADMIN_INIT_SQL="$2"; shift 2 ;;
    --pure) ADMIN_INIT_SQL="$FORGE_DIR/db/sqlserver/全量初始化SQL.纯净版.sql"; shift ;;
    --recreate) RECREATE="true"; shift ;;
    --migrate) MIGRATE="true"; shift ;;
    --yes) ASSUME_YES="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

ADMIN_INIT_SQL="${ADMIN_INIT_SQL:-$FORGE_DIR/db/sqlserver/全量初始化SQL.sql}"
[[ "$DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || die "--database 只能包含字母、数字和下划线: $DATABASE"
[[ -n "$PASSWORD" ]] || die "需要密码：--password 或环境变量 SQLCMDPASSWORD / MSSQL_SA_PASSWORD"

if [[ "$SKIP_ADMIN_INIT" != "true" && ( ! -f "$ADMIN_INIT_SQL" || ! -s "$ADMIN_INIT_SQL" ) ]]; then
  die "必需的 SQL Server 全量 SQL 不存在或为空：$ADMIN_INIT_SQL（先运行 convert-mysql-full-to-sqlserver.py）"
fi
if ! command -v sqlcmd >/dev/null 2>&1; then
  die "sqlcmd is required. Install SQL Server command-line tools and retry."
fi
if [[ "$MIGRATE" == "true" ]] && ! command -v mvn >/dev/null 2>&1; then
  die "--migrate requires Maven (mvn)."
fi

export SQLCMDPASSWORD="$PASSWORD"

SQLCMD_COMMON=(-S "${HOST},${PORT}" -U "$USER" -P "$PASSWORD" -b)
if [[ "$ENCRYPT" == "false" || "$ENCRYPT" == "0" ]]; then
  # Prefer TrustServerCertificate for local/dev when encrypt is off-ish
  SQLCMD_COMMON+=(-C)
fi

sqlcmd_master() {
  sqlcmd "${SQLCMD_COMMON[@]}" -d master "$@"
}

sqlcmd_db() {
  sqlcmd "${SQLCMD_COMMON[@]}" -d "$DATABASE" "$@"
}

if [[ "$RECREATE" == "true" ]]; then
  if [[ "$ASSUME_YES" != "true" ]]; then
    read -r -p "将 DROP DATABASE [$DATABASE]，请输入库名确认: " confirm
    [[ "$confirm" == "$DATABASE" ]] || die "确认失败，已中止。"
  fi
  sqlcmd_master -Q "IF DB_ID(N'$DATABASE') IS NOT NULL BEGIN ALTER DATABASE [$DATABASE] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [$DATABASE]; END"
fi

sqlcmd_master -Q "IF DB_ID(N'$DATABASE') IS NULL CREATE DATABASE [$DATABASE];"

if [[ "$SKIP_ADMIN_INIT" != "true" ]]; then
  echo "Importing $ADMIN_INIT_SQL ..."
  sqlcmd_db -i "$ADMIN_INIT_SQL"
fi

if [[ "$MIGRATE" == "true" ]]; then
  echo "Running Flyway migrations from db/sqlserver/migration ..."
  (
    cd "$FORGE_DIR/forge-admin-server"
    mvn -q flyway:migrate \
      -Dflyway.url="jdbc:sqlserver://${HOST}:${PORT};databaseName=${DATABASE};encrypt=false" \
      -Dflyway.user="$USER" \
      -Dflyway.password="$PASSWORD" \
      -Dflyway.locations="filesystem:$FORGE_DIR/db/sqlserver/migration" \
      -Dflyway.table="forge_schema_history" \
      -Dflyway.baselineOnMigrate=true \
      -Dflyway.baselineVersion=1.0.234
  )
fi

echo "SQL Server init done: $DATABASE @ $HOST:$PORT"
