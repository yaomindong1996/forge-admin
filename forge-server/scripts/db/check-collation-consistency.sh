#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
EXPECTED_COLLATION="utf8mb4_0900_ai_ci"

INIT_SCRIPT="$PROJECT_DIR/forge-server/scripts/db/init-db.sh"
COMPOSE_FILE="$PROJECT_DIR/docker-forge-admin/docker-compose.yml"
ACTIVE_SQL_PATHS=(
  "$PROJECT_DIR/forge-server/db/mysql/全量初始化SQL.sql"
  "$PROJECT_DIR/docker-forge-admin/init-sql/01-init.sql"
    "$PROJECT_DIR/forge-server/forge-report-server/sql"
  "$PROJECT_DIR/forge-server/forge-framework"
)
CONFIG_AND_GUIDE_PATHS=(
  "$INIT_SCRIPT"
  "$COMPOSE_FILE"
  "$PROJECT_DIR/AGENTS.md"
  "$PROJECT_DIR/code-copilot/rules/project-context.md"
)

if ! command -v rg >/dev/null 2>&1; then
  echo "ERROR: 排序规则校验需要 ripgrep（rg），请安装后重试。" >&2
  exit 1
fi

failures=0

# 扫描范围是必需输入；目录迁移应更新清单，不能静默跳过缺失路径。
for scan_path in "${CONFIG_AND_GUIDE_PATHS[@]}" "${ACTIVE_SQL_PATHS[@]}"; do
  if [[ ! -e "$scan_path" || ! -r "$scan_path" ]]; then
    echo "ERROR: 校验路径不存在或不可读：$scan_path" >&2
    failures=$((failures + 1))
  fi
done
if ((failures > 0)); then
  echo "Collation consistency check failed with $failures problem(s)." >&2
  exit 1
fi

require_pattern() {
  local pattern="$1"
  local file="$2"
  local description="$3"
  local status=0
  # 不使用 --quiet，避免找到匹配后提前返回而掩盖读取错误。
  rg --no-config --ignore-case -- "$pattern" "$file" >/dev/null || status=$?
  if ((status == 1)); then
    echo "ERROR: $description: $file" >&2
    failures=$((failures + 1))
  elif ((status != 0)); then
    echo "ERROR: rg 校验执行失败（退出码 ${status}）：$file" >&2
    failures=$((failures + 1))
  fi
}

reject_pattern() {
  local description="$1"
  shift
  local status=0
  rg --no-config --line-number --ignore-case "$@" || status=$?
  if ((status == 0)); then
    echo "ERROR: $description" >&2
    failures=$((failures + 1))
  elif ((status != 1)); then
    echo "ERROR: rg 扫描执行失败（退出码 ${status}）：$description" >&2
    failures=$((failures + 1))
  fi
}

require_pattern \
  "CREATE DATABASE IF NOT EXISTS.*CHARACTER SET utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$INIT_SCRIPT" \
  "init-db.sh must create databases with $EXPECTED_COLLATION"
require_pattern \
  "ALTER DATABASE.*CHARACTER SET utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$INIT_SCRIPT" \
  "init-db.sh must align an existing database default with $EXPECTED_COLLATION"
require_pattern \
  "--character-set-server=utf8mb4" \
  "$COMPOSE_FILE" \
  "Docker MySQL must use utf8mb4"
require_pattern \
  "--collation-server=$EXPECTED_COLLATION" \
  "$COMPOSE_FILE" \
  "Docker MySQL must use $EXPECTED_COLLATION"
require_pattern \
  "SET NAMES utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$PROJECT_DIR/forge-server/db/mysql/全量初始化SQL.sql" \
  "the server full initialization SQL must set the expected connection collation"
require_pattern \
  "SET NAMES utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$PROJECT_DIR/docker-forge-admin/init-sql/01-init.sql" \
  "the Docker full initialization SQL must set the expected connection collation"
require_pattern \
  "ALTER DATABASE CHARACTER SET utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$PROJECT_DIR/forge-server/db/mysql/全量初始化SQL.sql" \
  "the server full initialization SQL must align the selected database default"
require_pattern \
  "ALTER DATABASE CHARACTER SET utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$PROJECT_DIR/docker-forge-admin/init-sql/01-init.sql" \
  "the Docker full initialization SQL must align the selected database default"
require_pattern \
  "CREATE DATABASE forge_admin DEFAULT CHARACTER SET utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$PROJECT_DIR/AGENTS.md" \
  "AGENTS.md setup guidance must use $EXPECTED_COLLATION"
require_pattern \
  "CREATE DATABASE forge_admin DEFAULT CHARACTER SET utf8mb4 COLLATE $EXPECTED_COLLATION" \
  "$PROJECT_DIR/code-copilot/rules/project-context.md" \
  "project context setup guidance must use $EXPECTED_COLLATION"

reject_pattern \
  "初始化配置或安装指引包含旧的 utf8mb4 排序规则" \
  -- 'utf8mb4_(unicode_ci|general_ci)' \
  "${CONFIG_AND_GUIDE_PATHS[@]}"

# 历史版本迁移不在活动初始化范围内，保持已发布脚本 checksum 不变。
reject_pattern \
  "活动初始化 SQL 包含旧的 utf8mb4 排序规则" \
  --glob '*.sql' \
  --glob '!**/target/**' \
  -- 'COLLATE[=[:space:]]*utf8mb4_(unicode_ci|general_ci)' \
  "${ACTIVE_SQL_PATHS[@]}"

if ((failures > 0)); then
  echo "Collation consistency check failed with $failures problem(s)." >&2
  exit 1
fi

echo "Collation consistency check passed: utf8mb4 / $EXPECTED_COLLATION"
