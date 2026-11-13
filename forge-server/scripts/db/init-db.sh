#!/usr/bin/env bash
# Forge 数据库初始化：建库 → MySQL 全量 SQL →（可选）Flyway 增量 →（可选）清理成干净模板库。
# 兼容 macOS 自带 bash 3.2。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
HOST="127.0.0.1"
PORT="3306"
DATABASE="forge_admin"
USER="root"
PASSWORD="${MYSQL_PWD:-}"
WITH_MODULE="false"
SKIP_ADMIN_INIT="false"
RECREATE="false"
MIGRATE="false"
CLEAN="false"
ASSUME_YES="false"
CLEAN_ARGS=()

usage() {
  cat <<'USAGE'
Usage: init-db.sh [options]

默认流程：创建数据库 → 执行 db/mysql/全量初始化SQL.sql。
必需基础数据已包含在全量脚本中；增量由 Flyway（或 --migrate）执行。

Options:
  --host HOST              MySQL host, default 127.0.0.1
  --port PORT              MySQL port, default 3306
  --database DATABASE      Database name, default forge_admin
  --user USER              MySQL user, default root
  --password PASSWORD      MySQL password（也可用环境变量 MYSQL_PWD）
  --with-module            Import module SQL from db/module（若存在）
  --skip-admin-init        跳过 db/mysql/全量初始化SQL.sql（仅用于已完成基础初始化的库）
  --recreate               先 DROP 再重建数据库（全量 SQL 只能导入空库）
  --migrate                导入后用 Flyway 执行 db/mysql/migration 增量（需要 Maven，与后端启动时的迁移等价）
  --clean                  增量执行完后调用 clean-db.sh 清理成干净模板库（隐含 --migrate）
  --clean-arg ARG          透传给 clean-db.sh 的参数，可重复，例如 --clean-arg --admin-username --clean-arg admin
  --yes                    --recreate / --clean 时跳过确认
  -h, --help               Show help

初始化干净模板库（推荐）：
  MYSQL_PWD=*** bash forge-server/scripts/db/init-db.sh --database my_project --recreate --clean
USAGE
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_value() {
  [[ -n "${2:-}" && "${2:-}" != --* ]] || die "$1 requires a value."
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) require_value "$1" "${2:-}"; HOST="$2"; shift 2 ;;
    --port) require_value "$1" "${2:-}"; PORT="$2"; shift 2 ;;
    --database) require_value "$1" "${2:-}"; DATABASE="$2"; shift 2 ;;
    --user) require_value "$1" "${2:-}"; USER="$2"; shift 2 ;;
    --password) require_value "$1" "${2:-}"; PASSWORD="$2"; shift 2 ;;
    --with-demo|--with-optional)
      echo "WARN: $1 已废弃（db/seed 已移除），忽略。" >&2
      shift
      ;;
    --with-module) WITH_MODULE="true"; shift ;;
    --skip-admin-init) SKIP_ADMIN_INIT="true"; shift ;;
    --recreate) RECREATE="true"; shift ;;
    --migrate) MIGRATE="true"; shift ;;
    --clean) CLEAN="true"; MIGRATE="true"; shift ;;
    --clean-arg) [[ -n "${2:-}" ]] || die "--clean-arg requires a value."; CLEAN_ARGS+=("$2"); shift 2 ;;
    --yes) ASSUME_YES="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

ADMIN_INIT_SQL="$FORGE_DIR/db/mysql/全量初始化SQL.sql"
if [[ "$SKIP_ADMIN_INIT" != "true" && ( ! -f "$ADMIN_INIT_SQL" || ! -r "$ADMIN_INIT_SQL" || ! -s "$ADMIN_INIT_SQL" ) ]]; then
  echo "ERROR: 必需的初始化 SQL 不存在、不可读或为空：$ADMIN_INIT_SQL" >&2
  exit 1
fi

[[ "$DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || die "--database 只能包含字母、数字和下划线: $DATABASE"

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql client is required. Install MySQL client and retry." >&2
  exit 1
fi
if [[ "$MIGRATE" == "true" ]] && ! command -v mvn >/dev/null 2>&1; then
  die "--migrate/--clean 需要 Maven（mvn）。没有 Maven 时先去掉这两个参数，启动一次 forge-admin-server 执行增量，再运行 clean-db.sh --execute。"
fi

# 密码写入 600 权限的临时 option 文件，避免出现在进程列表；MYSQL_PWD 在新版客户端已废弃
CLIENT_CNF="$(mktemp "${TMPDIR:-/tmp}/forge-mysql.XXXXXX")"
trap 'rm -f "$CLIENT_CNF"' EXIT
chmod 600 "$CLIENT_CNF"
{
  echo "[client]"
  if [[ -n "$PASSWORD" ]]; then
    printf 'password="%s"\n' "$(printf '%s' "$PASSWORD" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')"
  fi
} > "$CLIENT_CNF"

MYSQL=(mysql --defaults-extra-file="$CLIENT_CNF" --protocol=tcp --host="$HOST" --port="$PORT" --user="$USER"
  --default-character-set=utf8mb4)

mysql_exec() {
  "${MYSQL[@]}" "$@"
}

confirm() {
  [[ "$ASSUME_YES" == "true" ]] && return 0
  echo "$1 请输入库名 $DATABASE 确认："
  local answer
  read -r answer
  [[ "$answer" == "$DATABASE" ]] || die "库名不匹配，已取消。"
}

server_version="$(mysql_exec --batch --skip-column-names --execute="SELECT VERSION()")"
if [[ "$server_version" =~ ^[0-9]+ ]] && [[ "${BASH_REMATCH[0]}" -lt 8 ]]; then
  die "需要 MySQL 8.0+（全量 SQL 使用 utf8mb4_0900_ai_ci），当前版本: $server_version"
fi

if [[ "$RECREATE" == "true" ]]; then
  confirm "将 DROP 并重建数据库 ${DATABASE}，原数据全部丢失。"
  mysql_exec --execute="DROP DATABASE IF EXISTS \`$DATABASE\`;"
fi

mysql_exec --execute="
CREATE DATABASE IF NOT EXISTS \`$DATABASE\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER DATABASE \`$DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
"

# 全量 SQL 是不带 IF NOT EXISTS 的建表语句，重复导入会报 Table already exists
if [[ "$SKIP_ADMIN_INIT" != "true" ]]; then
  table_count="$(mysql_exec --batch --skip-column-names --execute="
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DATABASE'")"
  if [[ "$table_count" =~ ^[0-9]+$ && "$table_count" -gt 0 ]]; then
    die "库 $DATABASE 已有 $table_count 张表，全量 SQL 只能导入空库。用 --recreate 重建，或用 --skip-admin-init 跳过全量。"
  fi
fi

run_sql_file() {
  local file="$1"
  if [[ -s "$file" ]]; then
    echo "Running $file"
    mysql_exec "$DATABASE" < "$file"
  fi
}

run_sql_dir() {
  local dir="$1" file
  if [[ -d "$dir" ]]; then
    while IFS= read -r file; do
      run_sql_file "$file"
    done < <(find "$dir" -maxdepth 1 -type f -name '*.sql' | sort)
  fi
}

run_migrations() {
  local url="jdbc:mysql://$HOST:$PORT/$DATABASE?useUnicode=true&characterEncoding=utf8"
  url="$url&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&useSSL=false"
  echo "Running Flyway migrations from $FORGE_DIR/db/mysql/migration"
  FORGE_DB_PASSWORD="$PASSWORD" mvn -q -B -f "$SCRIPT_DIR/flyway/pom.xml" flyway:migrate \
    -Dforge.db.url="$url" \
    -Dforge.db.user="$USER" \
    -Dforge.migration.dir="$FORGE_DIR/db/mysql/migration"
}

if [[ "$SKIP_ADMIN_INIT" != "true" ]]; then
  run_sql_file "$ADMIN_INIT_SQL"
fi

if [[ "$WITH_MODULE" == "true" ]]; then
  run_sql_dir "$FORGE_DIR/db/module"
fi

if [[ "$MIGRATE" == "true" ]]; then
  run_migrations
fi

if [[ "$CLEAN" == "true" ]]; then
  confirm "将清理测试数据，只保留超级管理员和默认租户。"
  clean_cmd=(bash "$SCRIPT_DIR/clean-db.sh" --host "$HOST" --port "$PORT" --database "$DATABASE"
    --user "$USER" --execute --yes)
  if [[ ${#CLEAN_ARGS[@]} -gt 0 ]]; then
    clean_cmd+=("${CLEAN_ARGS[@]}")
  fi
  MYSQL_PWD="$PASSWORD" "${clean_cmd[@]}"
fi

echo "Database initialization completed for $DATABASE."
if [[ "$MIGRATE" != "true" ]]; then
  echo "增量脚本会在 forge-admin-server 启动时由 Flyway 执行；也可以加 --migrate 立即执行。"
fi
