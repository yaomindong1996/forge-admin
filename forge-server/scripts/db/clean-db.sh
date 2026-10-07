#!/usr/bin/env bash
# Forge 模板库清理：只保留超级管理员、默认租户、系统配置（菜单/字典/参数/区划）和定时任务，
# 清空日志、流程、低代码、报表、AI 用户配置等测试与运行数据，并删除低代码自动建的业务表。
# 必须在「全量 SQL + Flyway 增量」都执行完后运行；增量脚本本身会写入演示数据，先清再迁移会被重新写回。
# 兼容 macOS 自带 bash 3.2：不使用关联数组、${var,,}、mapfile。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORGE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
MIGRATION_DIR="$FORGE_DIR/db/migration"

HOST="127.0.0.1"
PORT="3306"
DATABASE="forge_admin"
USER="root"
PASSWORD="${MYSQL_PWD:-}"
TENANT_ID="1"
ADMIN_USERNAME="admin"
KEEP_ORG_ID="1"
KEEP_AGENT_CODES="dashboard_generator"
EXECUTE="false"
ASSUME_YES="false"
DROP_BUSINESS_TABLES="true"
ALLOW_PENDING_MIGRATIONS="false"
PRINT_SQL="false"
BACKUP_FILE=""
# 迁移历史一旦被清空，重启会重跑已执行 SQL；改名工程的插件历史也必须优先保护。
MIGRATION_HISTORY_REGEX='^forge_schema_history$|_plugin_[a-z0-9_]+_history$'
EXTRA_KEEP_TABLES=()
EXTRA_DROP_TABLES=()
EXTRA_SQL_FILES=()

usage() {
  cat <<'USAGE'
Usage: clean-db.sh [options]

把已完成「全量 SQL + Flyway 增量」的库清理成干净模板库。默认只预览（dry-run），加 --execute 才会真正执行。

保留：默认租户、超级管理员（及其角色/根组织）、菜单与权限、字典、系统参数、行政区划、
      客户端配置、消息模板、定时任务配置、内置模板（页面/提示词/公式/编码规则/流程表达式等）、
      主库与插件 Flyway 迁移历史（包括改名工程的 *_plugin_*_history）。
清空：登录/操作/任务等日志、在线用户、消息公告、文件记录、全部流程数据（含 Flowable ACT_*）、
      低代码应用/对象/发布记录及其菜单、报表大屏、AI 模型/供应商/知识库、代码生成记录、
      测试用户/租户/组织/角色/岗位、其它租户数据、逻辑删除残留。
删除：低代码自动建的业务表（非 sys_/ai_/gen_/qrtz_/act_/flw_/forge_ 前缀，且没有 Java 实体或 Mapper 引用）。

Options:
  --host HOST                MySQL host, default 127.0.0.1
  --port PORT                MySQL port, default 3306
  --database DATABASE        Database name, default forge_admin
  --user USER                MySQL user, default root
  --password PASSWORD        MySQL password（也可用环境变量 MYSQL_PWD）
  --tenant-id ID             保留的默认租户 ID, default 1
  --admin-username NAME      保留的超级管理员账号, default admin
  --keep-org-id ID           超级管理员保留的根组织 ID, default 1（不存在时取该租户第一个根组织）
  --keep-agent-codes CODES   保留的内置 AI Agent 编码，逗号分隔, default dashboard_generator
  --keep-table TABLE         额外保护的表（不删不清），可重复
  --drop-table TABLE         额外删除的表，可重复；禁止指定主库或插件迁移历史表
  --keep-business-tables     业务表只清空数据，不 DROP
  --extra-sql FILE           清理末尾追加的自定义 SQL，可重复；需人工审核，不受迁移历史保护规则拦截
                             可用 @tenant_id/@admin_user_id 等变量
  --allow-pending-migrations 允许在增量未全部执行时清理（不推荐）
  --backup-file FILE         执行前用 mysqldump 备份整库到 FILE
  --print-sql                打印将要执行的 SQL
  --execute                  真正执行清理（默认只预览）
  --yes                      执行时跳过输入库名确认
  -h, --help                 Show help
USAGE
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_value() {
  [[ -n "${2:-}" && "${2:-}" != --* ]] || die "$1 requires a value."
}

validate_identifier() {
  [[ "$1" =~ ^[A-Za-z0-9_]+$ ]] || die "$2 只能包含字母、数字和下划线: $1"
}

validate_number() {
  [[ "$1" =~ ^[0-9]+$ ]] || die "$2 必须是数字: $1"
}

lower() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) require_value "$1" "${2:-}"; HOST="$2"; shift 2 ;;
    --port) require_value "$1" "${2:-}"; PORT="$2"; shift 2 ;;
    --database) require_value "$1" "${2:-}"; DATABASE="$2"; shift 2 ;;
    --user) require_value "$1" "${2:-}"; USER="$2"; shift 2 ;;
    --password) require_value "$1" "${2:-}"; PASSWORD="$2"; shift 2 ;;
    --tenant-id) require_value "$1" "${2:-}"; TENANT_ID="$2"; shift 2 ;;
    --admin-username) require_value "$1" "${2:-}"; ADMIN_USERNAME="$2"; shift 2 ;;
    --keep-org-id) require_value "$1" "${2:-}"; KEEP_ORG_ID="$2"; shift 2 ;;
    --keep-agent-codes) require_value "$1" "${2:-}"; KEEP_AGENT_CODES="$2"; shift 2 ;;
    --keep-table) require_value "$1" "${2:-}"; EXTRA_KEEP_TABLES+=("$(lower "$2")"); shift 2 ;;
    --drop-table) require_value "$1" "${2:-}"; EXTRA_DROP_TABLES+=("$(lower "$2")"); shift 2 ;;
    --keep-business-tables) DROP_BUSINESS_TABLES="false"; shift ;;
    --extra-sql) require_value "$1" "${2:-}"; EXTRA_SQL_FILES+=("$2"); shift 2 ;;
    --allow-pending-migrations) ALLOW_PENDING_MIGRATIONS="true"; shift ;;
    --backup-file) require_value "$1" "${2:-}"; BACKUP_FILE="$2"; shift 2 ;;
    --print-sql) PRINT_SQL="true"; shift ;;
    --execute) EXECUTE="true"; shift ;;
    --yes) ASSUME_YES="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

validate_identifier "$DATABASE" "--database"
validate_number "$TENANT_ID" "--tenant-id"
validate_number "$KEEP_ORG_ID" "--keep-org-id"
[[ "$ADMIN_USERNAME" =~ ^[A-Za-z0-9_.@-]+$ ]] || die "--admin-username 含非法字符: $ADMIN_USERNAME"
[[ "$KEEP_AGENT_CODES" =~ ^[A-Za-z0-9_,]*$ ]] || die "--keep-agent-codes 只能是逗号分隔的编码"
if [[ ${#EXTRA_KEEP_TABLES[@]} -gt 0 ]]; then
  for t in "${EXTRA_KEEP_TABLES[@]}"; do validate_identifier "$t" "--keep-table"; done
fi
if [[ ${#EXTRA_DROP_TABLES[@]} -gt 0 ]]; then
  for t in "${EXTRA_DROP_TABLES[@]}"; do
    validate_identifier "$t" "--drop-table"
    if [[ "$t" =~ $MIGRATION_HISTORY_REGEX ]]; then
      die "迁移历史表不能通过 --drop-table 删除: $t"
    fi
  done
fi
if [[ ${#EXTRA_SQL_FILES[@]} -gt 0 ]]; then
  for f in "${EXTRA_SQL_FILES[@]}"; do [[ -r "$f" ]] || die "--extra-sql 文件不可读: $f"; done
fi

command -v mysql >/dev/null 2>&1 || die "需要 mysql 客户端。"

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/forge-clean-db.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT

# 密码写入 600 权限的临时 option 文件，避免出现在进程列表；MYSQL_PWD 在新版客户端已废弃
CLIENT_CNF="$WORK_DIR/client.cnf"
{
  echo "[client]"
  if [[ -n "$PASSWORD" ]]; then
    printf 'password="%s"\n' "$(printf '%s' "$PASSWORD" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')"
  fi
} > "$CLIENT_CNF"
chmod 600 "$CLIENT_CNF"

MYSQL_BASE=(mysql --defaults-extra-file="$CLIENT_CNF" --protocol=tcp --host="$HOST" --port="$PORT" --user="$USER"
  --default-character-set=utf8mb4)

run_query() {
  "${MYSQL_BASE[@]}" --batch --raw --skip-column-names "$DATABASE" --execute="$1"
}

run_sql_file() {
  "${MYSQL_BASE[@]}" "$DATABASE" < "$1"
}
TABLES_FILE="$WORK_DIR/tables.tsv"
COLUMNS_FILE="$WORK_DIR/columns.txt"
CODE_TABLES_FILE="$WORK_DIR/code-tables.txt"
PLAN_FILE="$WORK_DIR/plan.sql"
DROP_FILE="$WORK_DIR/drop.txt"
TRUNCATE_FILE="$WORK_DIR/truncate.txt"
: > "$DROP_FILE"
: > "$TRUNCATE_FILE"

# ---------------------------------------------------------------------------
# 表分类规则（全部按小写表名匹配）
# ---------------------------------------------------------------------------

# 框架表前缀：不会被当作业务表 DROP
FRAMEWORK_REGEX='^(sys_|ai_|gen_|qrtz_|act_|flw_|forge_)|^(worker_node|config_properties)$'

# 保留表结构/内置数据，仍允许后续按租户和逻辑删除过滤；迁移历史由专用规则完整保护。
KEEP_EXACT_REGEX='^(qrtz_locks|act_ge_property|act_id_property|sys_flow_spel_template|'
KEEP_EXACT_REGEX+='sys_flow_template|sys_flow_comment_phrase|ai_business_field_template)$|databasechangelog'

# 框架前缀下的垃圾表（备份/临时副本），直接 DROP
JUNK_REGEX='_(bak|backup|old|copy|tmp)$|_[0-9]{4,8}$|^tmp_'

# 框架表中需要整表清空的数据
TRUNCATE_REGEXES=(
  # 日志、历史、执行记录、投递箱、幂等记录
  '(^|_)logs?$' '_history$' '_record$' '_outbox$' '_inbox$' '_idempotency$' '_repair$'
  # 会话与运行态
  '^sys_auth_online_user$' '^worker_node$' '^qrtz_(fired_triggers|scheduler_state)$'
  '^sys_job_api_token$' '^sys_plugin_task$' '^sys_plugin_build$' '^ai_crud_export_task$' '^ai_chat_' '^ai_agent_event$'
  # 消息、公告、文件
  '^sys_message$' '^sys_message_receiver$' '^sys_notice' '^sys_file_(metadata|group|storage_config)$'
  # 流程（Forge 流程表 + Flowable 运行/历史/部署表）
  '^sys_flow_' '^act_' '^flw_'
  # 低代码应用、业务对象、开放平台、数据审计
  '^ai_business_' '^ai_crud_config' '^ai_lowcode_(model|publish_task)$' '^ai_custom_query_scheme$'
  '^ai_data_audit_' '^ai_application_' '^ai_capability'
  # 报表大屏
  '^ai_report_' '^ai_dashboard_'
  # AI 用户配置（含 API Key）、知识库、技能
  '^ai_(provider|model|model_capability|model_route_policy|model_route_target)$'
  '^ai_knowledge' '^ai_store_instance$' '^ai_skill' '^ai_agent_(skill|tool_config|tool_permission)$'
  # 代码生成记录与数据源（含数据库密码）
  '^gen_(table|table_column|datasource)$'
  # 打印、外部接口、企业协同同步数据
  '^sys_print_' '^sys_external_(system|api)$' '^sys_user_social$'
  '^sys_social_(org_mapping|post_mapping|sync_issue|tag|tag_member|todo_link|callback_event)$'
  # 岗位、员工
  '^sys_post$' '^sys_user_post$' '^sys_employee$'
)

in_list() {
  local needle="$1"; shift
  local item
  for item in "$@"; do
    [[ "$item" == "$needle" ]] && return 0
  done
  return 1
}

matches_truncate() {
  local name="$1" regex
  for regex in "${TRUNCATE_REGEXES[@]}"; do
    [[ "$name" =~ $regex ]] && return 0
  done
  return 1
}

has_table() {
  awk -F '\t' -v t="$1" '$2 == t { found = 1 } END { exit found ? 0 : 1 }' "$TABLES_FILE"
}

has_column() {
  grep -qx "$1 $2" "$COLUMNS_FILE"
}

is_code_table() {
  grep -qx "$1" "$CODE_TABLES_FILE"
}

is_cleared() {
  grep -qx "$1" "$DROP_FILE" || grep -qx "$1" "$TRUNCATE_FILE"
}

# 有 Java 实体（@TableName）或 Mapper XML 引用的表，删表会导致启动或迁移失败，只能清数据
collect_code_tables() {
  {
    grep -rhoE '@TableName\((value[[:space:]]*=[[:space:]]*)?"[A-Za-z0-9_]+"' "$FORGE_DIR" \
      --include='*.java' --exclude-dir=test --exclude-dir=target 2>/dev/null \
      | sed -E 's/.*"([A-Za-z0-9_]+)"/\1/' || true
    grep -rhoiE '(from|join|into|update)[[:space:]]+`?[A-Za-z][A-Za-z0-9_]*' "$FORGE_DIR" \
      --include='*Mapper.xml' --exclude-dir=test --exclude-dir=target 2>/dev/null \
      | awk '{print $2}' | tr -d '`' || true
  } | tr '[:upper:]' '[:lower:]' | sort -u > "$CODE_TABLES_FILE"
}

# ---------------------------------------------------------------------------
# 前置检查
# ---------------------------------------------------------------------------

echo "Checking MySQL connection for database $DATABASE ..."
server_version="$(run_query "SELECT VERSION()")"
[[ "${server_version%%.*}" -ge 8 ]] 2>/dev/null || die "需要 MySQL 8.0+，当前版本: $server_version"

run_query "SELECT table_name, LOWER(table_name) FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' ORDER BY table_name" > "$TABLES_FILE"
run_query "SELECT CONCAT(LOWER(table_name), ' ', LOWER(column_name)) FROM information_schema.columns
  WHERE table_schema = DATABASE()" > "$COLUMNS_FILE"

for required in sys_user sys_tenant sys_role sys_resource; do
  has_table "$required" || die "库 $DATABASE 缺少 ${required}，请先执行全量初始化 SQL。"
done

check_migrations() {
  local pending
  has_table forge_schema_history || die "未找到 forge_schema_history，增量脚本还没执行。请先执行 init-db.sh --migrate 或启动一次 forge-admin-server。"
  run_query "SELECT version FROM forge_schema_history WHERE success = 1 AND version IS NOT NULL" \
    | sort -u > "$WORK_DIR/applied.txt"
  find "$MIGRATION_DIR" -maxdepth 1 -type f -name 'V*__*.sql' \
    | sed -E 's#.*/V([0-9.]+)__.*#\1#' | sort -u > "$WORK_DIR/files.txt"
  pending="$(comm -23 "$WORK_DIR/files.txt" "$WORK_DIR/applied.txt" | grep -vx '1.0.0' || true)"
  if [[ -n "$pending" ]]; then
    if [[ "$ALLOW_PENDING_MIGRATIONS" == "true" ]]; then
      echo "WARN: 以下增量未执行，稍后执行可能重新写入演示数据: $(echo "$pending" | head -5 | tr '\n' ' ')"
    else
      die "以下增量脚本尚未执行（共 $(echo "$pending" | wc -l | tr -d ' ') 个，示例: $(echo "$pending" | head -5 | tr '\n' ' ')）。增量会写入演示数据，必须先执行完再清理。"
    fi
  fi
}
check_migrations

first_value() {
  run_query "$1" | head -1
}

has_table sys_tenant && [[ -n "$(first_value "SELECT id FROM sys_tenant WHERE id = $TENANT_ID")" ]] \
  || die "默认租户 $TENANT_ID 不存在。"

ADMIN_USER_ID="$(first_value "SELECT id FROM sys_user WHERE username = '$ADMIN_USERNAME'
  ORDER BY (del_flag = 0) DESC, (tenant_id = $TENANT_ID) DESC, id LIMIT 1")"
[[ -n "$ADMIN_USER_ID" ]] || die "超级管理员账号 $ADMIN_USERNAME 不存在。"

ADMIN_ROLE_ID="$(first_value "SELECT id FROM sys_role WHERE tenant_id = $TENANT_ID AND role_key = 'admin'
  ORDER BY (del_flag = 0) DESC, id LIMIT 1")"
if [[ -z "$ADMIN_ROLE_ID" ]]; then
  ADMIN_ROLE_ID="$(first_value "SELECT r.id FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id
    WHERE ur.user_id = $ADMIN_USER_ID AND r.is_system = 1 ORDER BY r.id LIMIT 1")"
fi
[[ -n "$ADMIN_ROLE_ID" ]] || die "租户 $TENANT_ID 下找不到超级管理员角色（role_key = 'admin'）。"

if has_table sys_org; then
  resolved_org="$(first_value "SELECT id FROM sys_org WHERE id = $KEEP_ORG_ID AND tenant_id = $TENANT_ID")"
  if [[ -z "$resolved_org" ]]; then
    resolved_org="$(first_value "SELECT id FROM sys_org WHERE tenant_id = $TENANT_ID AND parent_id = 0
      ORDER BY (del_flag = 0) DESC, sort, id LIMIT 1")"
  fi
  KEEP_ORG_ID="$resolved_org"
fi

# ---------------------------------------------------------------------------
# 表分类
# ---------------------------------------------------------------------------

collect_code_tables

while IFS=$'\t' read -r original name; do
  [[ -n "$name" ]] || continue
  [[ "$name" =~ $MIGRATION_HISTORY_REGEX ]] && continue
  if [[ ${#EXTRA_KEEP_TABLES[@]} -gt 0 ]] && in_list "$name" "${EXTRA_KEEP_TABLES[@]}"; then
    continue
  fi
  if [[ ${#EXTRA_DROP_TABLES[@]} -gt 0 ]] && in_list "$name" "${EXTRA_DROP_TABLES[@]}"; then
    echo "$original" >> "$DROP_FILE"
    continue
  fi
  [[ "$name" =~ $KEEP_EXACT_REGEX ]] && continue
  if [[ "$name" =~ $FRAMEWORK_REGEX ]]; then
    if [[ "$name" =~ $JUNK_REGEX ]] && ! is_code_table "$name"; then
      echo "$original" >> "$DROP_FILE"
    elif matches_truncate "$name"; then
      echo "$original" >> "$TRUNCATE_FILE"
    fi
    continue
  fi
  # 非框架前缀：有代码引用的演示表清数据，其余视为低代码/测试自动建表
  if is_code_table "$name" || [[ "$DROP_BUSINESS_TABLES" != "true" ]]; then
    echo "$original" >> "$TRUNCATE_FILE"
  else
    echo "$original" >> "$DROP_FILE"
  fi
done < "$TABLES_FILE"

# ---------------------------------------------------------------------------
# 生成清理 SQL
# ---------------------------------------------------------------------------

emit() {
  printf '%s\n' "$*" >> "$PLAN_FILE"
}

# 表存在才输出语句（兼容不同版本库结构）
emit_if_table() {
  local table="$1"; shift
  has_table "$table" && emit "$@"
  return 0
}

keep_agent_sql="$(printf '%s' "$KEEP_AGENT_CODES" | awk -F ',' '{
  out = ""; for (i = 1; i <= NF; i++) if ($i != "") out = out (out == "" ? "" : ",") "\x27" $i "\x27";
  print (out == "" ? "\x27\x27" : out) }')"

: > "$PLAN_FILE"
emit "-- Forge template cleanup plan for $DATABASE"
emit "SET NAMES utf8mb4;"
emit "SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0;"
emit "SET @tenant_id = $TENANT_ID, @admin_user_id = $ADMIN_USER_ID, @admin_role_id = $ADMIN_ROLE_ID;"
emit "SET @keep_org_id = ${KEEP_ORG_ID:-0};"
emit ""

emit "-- 1. 低代码生成的菜单及其按钮/子菜单（在清空低代码元数据之前执行）"
emit "DROP TEMPORARY TABLE IF EXISTS forge_clean_menu_ids;"
emit "CREATE TEMPORARY TABLE forge_clean_menu_ids (id BIGINT PRIMARY KEY);"
emit "INSERT IGNORE INTO forge_clean_menu_ids
WITH RECURSIVE menu_tree (id, depth) AS (
  SELECT id, 0 FROM sys_resource
  WHERE (path LIKE '/ai/crud-page/%' AND path NOT LIKE '/ai/crud-page/:%')
     OR path LIKE '/app-center/suite-menu/%'
     OR path LIKE '/ai/lowcode-domain/%'
     OR path LIKE '/app/%'
     OR path LIKE '/pages/lowcode-runtime%'
     OR perms LIKE 'ai:business:application:%:page:%'
  UNION ALL
  SELECT child.id, menu_tree.depth + 1 FROM sys_resource child
  JOIN menu_tree ON child.parent_id = menu_tree.id
  WHERE menu_tree.depth < 20
)
SELECT id FROM menu_tree;"
emit_if_table ai_crud_config "INSERT IGNORE INTO forge_clean_menu_ids
SELECT r.id FROM sys_resource r JOIN ai_crud_config c ON c.menu_resource_id = r.id;"
emit "DELETE FROM sys_role_resource WHERE resource_id IN (SELECT id FROM forge_clean_menu_ids);"
emit "DELETE FROM sys_resource WHERE id IN (SELECT id FROM forge_clean_menu_ids);"
emit "DROP TEMPORARY TABLE IF EXISTS forge_clean_menu_ids;"
emit ""

emit "-- 2. 删除低代码自动建表、备份表"
while IFS= read -r table; do
  [[ -n "$table" ]] && emit "DROP TABLE IF EXISTS \`$table\`;"
done < "$DROP_FILE"
emit ""

emit "-- 3. 清空日志、流程、低代码、报表、AI 用户配置等数据"
while IFS= read -r table; do
  [[ -n "$table" ]] && emit "TRUNCATE TABLE \`$table\`;"
done < "$TRUNCATE_FILE"
emit ""

emit "-- 4. 身份数据：只保留默认租户、超级管理员、超级管理员角色和根组织"
emit "DELETE FROM sys_tenant WHERE id <> @tenant_id;"
emit "DELETE FROM sys_user WHERE id <> @admin_user_id;"
admin_updates="tenant_id = @tenant_id"
for column in avatar last_login_time last_login_ip; do
  has_column sys_user "$column" && admin_updates="$admin_updates, $column = NULL"
done
has_column sys_user login_count && admin_updates="$admin_updates, login_count = 0"
emit "UPDATE sys_user SET $admin_updates WHERE id = @admin_user_id;"
if has_column sys_tenant default_business_datasource_id; then
  emit "UPDATE sys_tenant SET default_business_datasource_id = NULL, default_business_datasource_code = NULL WHERE id = @tenant_id;"
fi
emit "DELETE FROM sys_role WHERE id <> @admin_role_id;"
emit "DELETE FROM sys_role_resource WHERE role_id <> @admin_role_id;"
emit "DELETE FROM sys_user_role WHERE NOT (user_id = @admin_user_id AND role_id = @admin_role_id);"
emit "INSERT INTO sys_user_role (tenant_id, user_id, role_id, create_time)
SELECT @tenant_id, @admin_user_id, @admin_role_id, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = @admin_user_id AND role_id = @admin_role_id);"
emit_if_table sys_user_tenant "DELETE FROM sys_user_tenant WHERE NOT (user_id = @admin_user_id AND tenant_id = @tenant_id);"
emit_if_table sys_user_tenant "UPDATE sys_user_tenant SET is_default = 1 WHERE user_id = @admin_user_id;"
emit_if_table sys_role_org "DELETE FROM sys_role_org;"
emit_if_table sys_role_data_scope "DELETE FROM sys_role_data_scope WHERE role_id <> @admin_role_id;"
if has_column sys_role_module_data_scope role_id; then
  emit "DELETE FROM sys_role_module_data_scope WHERE role_id <> @admin_role_id;"
fi
if [[ -n "${KEEP_ORG_ID:-}" ]]; then
  emit "DELETE FROM sys_org WHERE id <> @keep_org_id;"
  emit "UPDATE sys_org SET parent_id = 0, ancestors = '0' WHERE id = @keep_org_id;"
  emit_if_table sys_user_org "DELETE FROM sys_user_org;"
  emit_if_table sys_user_org "INSERT INTO sys_user_org (tenant_id, user_id, org_id, is_main, create_time)
VALUES (@tenant_id, @admin_user_id, @keep_org_id, 1, NOW());"
  emit_if_table sys_user_org_role "DELETE FROM sys_user_org_role;"
  emit_if_table sys_user_org_role "INSERT INTO sys_user_org_role (tenant_id, user_id, org_id, role_id, create_by, create_time)
VALUES (@tenant_id, @admin_user_id, @keep_org_id, @admin_role_id, @admin_user_id, NOW());"
else
  emit_if_table sys_org "DELETE FROM sys_org;"
  emit_if_table sys_user_org "DELETE FROM sys_user_org;"
  emit_if_table sys_user_org_role "DELETE FROM sys_user_org_role;"
fi
emit ""

emit "-- 5. 选择性保留：内置模板保留，用户创建的删除"
emit_if_table sys_flow_template "DELETE FROM sys_flow_template WHERE is_system IS NULL OR is_system <> 1;"
emit_if_table sys_flow_comment_phrase "DELETE FROM sys_flow_comment_phrase WHERE owner_type IS NULL OR owner_type <> 0;"
emit_if_table ai_business_field_template "DELETE FROM ai_business_field_template WHERE suite_code IS NULL OR suite_code <> 'COMMON';"
if has_table ai_lowcode_domain; then
  emit "DELETE FROM ai_lowcode_domain WHERE NOT (domain_code = 'general' AND tenant_id = @tenant_id);"
  emit "UPDATE ai_lowcode_domain SET menu_parent_id = NULL;"
fi
if has_table ai_code_rule; then
  emit "DELETE FROM ai_code_rule WHERE builtin IS NULL OR builtin <> 1 OR source_object_id IS NOT NULL OR source_object_code IS NOT NULL;"
  emit_if_table ai_code_rule_segment "DELETE FROM ai_code_rule_segment WHERE rule_id NOT IN (SELECT id FROM ai_code_rule);"
fi
if has_table ai_formula_function; then
  emit "DELETE FROM ai_formula_function WHERE builtin IS NULL OR builtin <> 1;"
  emit_if_table ai_formula_function_version "DELETE FROM ai_formula_function_version WHERE function_code NOT IN (SELECT function_code FROM ai_formula_function);"
  emit_if_table ai_formula_function_install "DELETE FROM ai_formula_function_install WHERE function_code NOT IN (SELECT function_code FROM ai_formula_function);"
fi
emit_if_table ai_agent "DELETE FROM ai_agent WHERE agent_code NOT IN ($keep_agent_sql);"
emit_if_table sys_id_sequence "DELETE FROM sys_id_sequence WHERE biz_key LIKE '%:%';"
emit ""

# 其它租户数据、逻辑删除残留：只处理没有被整表清空/删除的表
emit "-- 6. 删除其它租户的数据（tenant_id 为 0 的全局数据保留）"
while IFS=$'\t' read -r original name; do
  [[ -n "$name" ]] || continue
  [[ "$name" =~ $MIGRATION_HISTORY_REGEX ]] && continue
  is_cleared "$original" && continue
  [[ "$name" == "sys_tenant" ]] && continue
  has_column "$name" tenant_id && emit "DELETE FROM \`$original\` WHERE tenant_id NOT IN (0, @tenant_id);"
done < "$TABLES_FILE"
emit ""

emit "-- 7. 清除逻辑删除残留"
while IFS=$'\t' read -r original name; do
  [[ -n "$name" ]] || continue
  [[ "$name" =~ $MIGRATION_HISTORY_REGEX ]] && continue
  is_cleared "$original" && continue
  for column in del_flag deleted; do
    has_column "$name" "$column" && emit "DELETE FROM \`$original\` WHERE \`$column\` <> 0;"
  done
done < "$TABLES_FILE"
emit ""

emit "-- 8. 收尾：孤儿授权、默认本地存储"
emit "DELETE FROM sys_role_resource WHERE resource_id NOT IN (SELECT id FROM sys_resource);"
if has_table sys_file_storage_config; then
  emit "INSERT INTO sys_file_storage_config (config_name, storage_type, is_default, enabled, use_https, max_file_size,
  allowed_types, order_num, create_time, update_time, create_by, update_by, del_flag)
SELECT '本地存储', 'local', 1, 1, 0, 100,
  'jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3', 0, NOW(), NOW(), @admin_user_id, @admin_user_id, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_file_storage_config);"
fi
if [[ ${#EXTRA_SQL_FILES[@]} -gt 0 ]]; then
  for extra_sql in "${EXTRA_SQL_FILES[@]}"; do
    emit ""
    emit "-- 9. 项目自定义清理: $extra_sql"
    cat "$extra_sql" >> "$PLAN_FILE"
    emit ""
  done
fi
emit "SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;"

# ---------------------------------------------------------------------------
# 预览
# ---------------------------------------------------------------------------

count_rows_sql() {
  local file="$1" sql="" table
  while IFS= read -r table; do
    [[ -n "$table" ]] || continue
    [[ -n "$sql" ]] && sql="$sql UNION ALL "
    sql="${sql}SELECT '$table', COUNT(*) FROM \`$table\`"
  done < "$file"
  printf '%s' "$sql"
}

print_table_counts() {
  local title="$1" file="$2" sql
  echo "$title: $(grep -c . "$file" || true) 张"
  sql="$(count_rows_sql "$file")"
  [[ -n "$sql" ]] || return 0
  run_query "$sql" | awk -F '\t' '{ printf "  - %-48s %s 行\n", $1, $2 }'
}

echo
echo "== 保留对象 =="
echo "默认租户 ID: $TENANT_ID"
echo "超级管理员: $ADMIN_USERNAME (id=$ADMIN_USER_ID)，角色 id=${ADMIN_ROLE_ID}，根组织 id=${KEEP_ORG_ID:-无}"
echo "保留内置 Agent: ${KEEP_AGENT_CODES:-无}"
echo
print_table_counts "将删除的表（DROP）" "$DROP_FILE"
echo
print_table_counts "将清空的表（TRUNCATE）" "$TRUNCATE_FILE"
echo
echo "另外会删除：低代码菜单、测试用户/租户/角色/组织、其它租户数据、逻辑删除残留，并补齐本地存储默认配置。"

if [[ "$PRINT_SQL" == "true" ]]; then
  echo
  echo "== SQL =="
  cat "$PLAN_FILE"
fi

if [[ "$EXECUTE" != "true" ]]; then
  echo
  echo "当前为预览模式，没有修改数据库。确认无误后加 --execute 执行。"
  exit 0
fi

# ---------------------------------------------------------------------------
# 执行
# ---------------------------------------------------------------------------

if [[ "$ASSUME_YES" != "true" ]]; then
  echo
  echo "清理不可回滚（TRUNCATE/DROP 会隐式提交）。请输入库名 $DATABASE 确认："
  read -r confirm_name
  [[ "$confirm_name" == "$DATABASE" ]] || die "库名不匹配，已取消。"
fi

if [[ -n "$BACKUP_FILE" ]]; then
  command -v mysqldump >/dev/null 2>&1 || die "--backup-file 需要 mysqldump。"
  mkdir -p "$(dirname "$BACKUP_FILE")"
  echo "Backing up $DATABASE to $BACKUP_FILE ..."
  mysqldump --defaults-extra-file="$CLIENT_CNF" --protocol=tcp --host="$HOST" --port="$PORT" --user="$USER" \
    --default-character-set=utf8mb4 --single-transaction --routines --triggers --hex-blob \
    "$DATABASE" > "$BACKUP_FILE"
fi

echo "Running cleanup ..."
run_sql_file "$PLAN_FILE"

echo
echo "== 清理结果 =="
run_query "SELECT '租户', COUNT(*) FROM sys_tenant
  UNION ALL SELECT '用户', COUNT(*) FROM sys_user
  UNION ALL SELECT '角色', COUNT(*) FROM sys_role
  UNION ALL SELECT '菜单/权限', COUNT(*) FROM sys_resource
  UNION ALL SELECT '字典数据', COUNT(*) FROM sys_dict_data" \
  | awk -F '\t' '{ printf "  %-10s %s\n", $1, $2 }'
has_table sys_job_config && run_query "SELECT '定时任务', COUNT(*) FROM sys_job_config" \
  | awk -F '\t' '{ printf "  %-10s %s\n", $1, $2 }'
echo "Cleanup completed for $DATABASE."
