import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { existsSync, mkdtempSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const serverDir = path.resolve(scriptDir, '../..')
const migrationDir = path.join(serverDir, 'db/migration')
const source = readFileSync(path.join(scriptDir, 'clean-db.sh'), 'utf8')
  .replace(/^SCRIPT_DIR=.*$/m, `SCRIPT_DIR=${JSON.stringify(scriptDir)}`)
  .replace(/^FORGE_DIR=.*$/m, `FORGE_DIR=${JSON.stringify(serverDir)}`)

// 真实库结构：全量 SQL + 增量新建的表 + Flowable 启动时建的表
function realTables() {
  const names = new Set()
  const fullSql = readFileSync(path.join(serverDir, 'db/全量初始化SQL.sql'), 'utf8')
  for (const match of fullSql.matchAll(/^CREATE TABLE `([^`]+)`/gm)) names.add(match[1])
  for (const file of readdirSync(migrationDir)) {
    const sql = readFileSync(path.join(migrationDir, file), 'utf8')
    for (const match of sql.matchAll(/CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?([A-Za-z0-9_]+)/gi)) names.add(match[1])
  }
  for (const name of ['ACT_RU_TASK', 'ACT_HI_PROCINST', 'ACT_RE_DEPLOYMENT', 'ACT_GE_BYTEARRAY',
    'ACT_GE_PROPERTY', 'ACT_ID_PROPERTY', 'FLW_EV_DATABASECHANGELOG', 'tmp_manual_fix', 'sys_user_bak']) {
    names.add(name)
  }
  return [...names].sort()
}

// forge:create 按 preset 裁剪模块后，演示表可能已没有代码引用，此时 DROP 才是正确结果
function codeSources(dir, out = { java: [], mapper: [] }) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (['node_modules', 'target', 'test', 'db', 'scripts', '.git'].includes(entry.name)) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) codeSources(full, out)
    else if (entry.name.endsWith('Mapper.xml')) out.mapper.push(readFileSync(full, 'utf8'))
    else if (entry.name.endsWith('.java')) out.java.push(readFileSync(full, 'utf8'))
  }
  return out
}
const sources = codeSources(serverDir)
const javaText = sources.java.join('\n')
const mapperText = sources.mapper.join('\n')
const codeBacked = name => new RegExp(`@TableName\\((value\\s*=\\s*)?"${name}"`).test(javaText)
  || new RegExp(`\\b${name}\\b`, 'i').test(mapperText)
const CODE_BACKED_DEMO = ['sample_purchase_order', 'biz_leave_request', 'business_datasource_demo']
  .filter(codeBacked)

// 主历史表名称随 forge:create 替换，插件还需支持不属于框架前缀的自定义工程名。
const MAIN_HISTORY_TABLE = 'forge_schema_history'
const PLUGIN_HISTORY_TABLES = [
  `${MAIN_HISTORY_TABLE.replace(/_schema_history$/, '')}_plugin_hello_history`,
  'forge_plugin_api_2_history', 'acme_plugin_hello_history', 'Acme_Plugin_Order_2_History',
  'tmp_project_plugin_demo_history',
]

const COLUMNS = {
  sys_user: ['id', 'tenant_id', 'username', 'avatar', 'last_login_time', 'last_login_ip', 'login_count', 'del_flag'],
  sys_tenant: ['id', 'default_business_datasource_id', 'default_business_datasource_code', 'del_flag'],
  sys_role: ['id', 'tenant_id', 'role_key', 'is_system', 'del_flag'],
  sys_dict_data: ['dict_code', 'tenant_id', 'del_flag'],
  sys_resource: ['id', 'tenant_id', 'parent_id', 'path', 'perms', 'del_flag'],
  sys_job_config: ['id', 'del_flag'],
  sys_flow_spel_template: ['id', 'tenant_id', 'deleted'],
  sys_role_module_data_scope: ['id', 'tenant_id', 'role_id'],
}

function allVersions() {
  return readdirSync(migrationDir)
    .map(file => file.match(/^V([0-9.]+)__/)?.[1])
    .filter(Boolean)
}

function runClean(args = [], {
  applied = allVersions(), adminFound = true, tables = realTables(), columns = COLUMNS,
} = {}) {
  const tmp = mkdtempSync(path.join(tmpdir(), 'forge-clean-db-'))
  const files = {
    tables: path.join(tmp, 'tables.tsv'),
    columns: path.join(tmp, 'columns.txt'),
    applied: path.join(tmp, 'applied.txt'),
    executed: path.join(tmp, 'executed.sql'),
    calls: path.join(tmp, 'mysql-calls.txt'),
  }
  writeFileSync(files.tables, tables.map(name => `${name}\t${name.toLowerCase()}`).join('\n') + '\n')
  writeFileSync(files.columns, Object.entries(columns)
    .flatMap(([table, fields]) => fields.map(column => `${table.toLowerCase()} ${column}`)).join('\n') + '\n')
  writeFileSync(files.applied, applied.join('\n') + '\n')
  // 不依赖 PATH 中可能安装的新版 bash，确保 macOS 系统自带的 3.2 也能执行保护逻辑。
  const result = spawnSync('/bin/bash', ['--noprofile', '--norc', '-c', `
mysql() {
  printf 'mysql\\n' >> ${JSON.stringify(files.calls)}
  local q="" a
  for a in "$@"; do case "$a" in --execute=*) q="\${a#--execute=}" ;; esac; done
  if [ -z "$q" ]; then cat > ${JSON.stringify(files.executed)}; return 0; fi
  case "$q" in
    *"VERSION()"*) echo "8.0.36" ;;
    *information_schema.tables*) cat ${JSON.stringify(files.tables)} ;;
    *information_schema.columns*) cat ${JSON.stringify(files.columns)} ;;
    *forge_schema_history*) cat ${JSON.stringify(files.applied)} ;;
    *"FROM sys_user WHERE username"*) ${adminFound ? 'echo 1' : ':'} ;;
    *"FROM sys_tenant WHERE id"*|*"FROM sys_role WHERE tenant_id"*|*"FROM sys_org WHERE id"*) echo 1 ;;
    *"COUNT(*)"*) printf '%s\\n' "$q" | grep -o "SELECT '[^']*'" | sed "s/SELECT '//; s/'\\$//" | awk '{ print $0 "\\t3" }' ;;
  esac
}
${source}
`, 'clean-db-test', ...args], {
    encoding: 'utf8', timeout: 60000,
    env: { ...process.env, BASH_ENV: '', ENV: '' },
  })
  const executed = existsSync(files.executed) ? readFileSync(files.executed, 'utf8') : ''
  const mysqlCalls = existsSync(files.calls) ? readFileSync(files.calls, 'utf8').trim().split('\n').length : 0
  return { result, executed, mysqlCalls }
}

function section(stdout, title) {
  const start = stdout.indexOf(title)
  assert.ok(start >= 0, `missing section ${title}\n${stdout}`)
  const end = stdout.indexOf('\n\n', start)
  return stdout.slice(start, end < 0 ? undefined : end)
}

test('预览模式按规则分类表且不执行任何写入', () => {
  assert.ok(codeBacked('sys_user'), 'code scan found no entity sources')
  const { result, executed } = runClean()
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.equal(executed, '')
  assert.match(result.stdout, /当前为预览模式/)

  const dropped = section(result.stdout, '将删除的表（DROP）')
  for (const name of ['crm_customer', 'pw_purchase_order', 'tf_f_order', 'ps_presale_order', 'sys_resource_0607',
    'tmp_manual_fix', 'sys_user_bak']) {
    assert.match(dropped, new RegExp(`- ${name}\\s`), `${name} should be dropped`)
  }
  // 有 Java 实体/Mapper 引用的演示表、框架表不能 DROP
  for (const name of [...CODE_BACKED_DEMO, 'sys_user', 'sys_dict_data', 'forge_schema_history', 'worker_node']) {
    assert.doesNotMatch(dropped, new RegExp(`- ${name}\\s`), `${name} must not be dropped`)
  }

  const truncated = section(result.stdout, '将清空的表（TRUNCATE）')
  for (const name of [...CODE_BACKED_DEMO, 'sys_login_log', 'sys_operation_log',
    'sys_job_log', 'sys_flow_model', 'sys_flow_task', 'ACT_RU_TASK', 'ACT_GE_BYTEARRAY', 'ai_crud_config',
    'ai_business_object', 'ai_business_process', 'ai_report_project', 'ai_provider', 'gen_datasource',
    'sys_file_storage_config', 'sys_message', 'sys_auth_online_user', 'sys_post', 'sys_user_password_history']) {
    assert.match(truncated, new RegExp(`- ${name}\\s`), `${name} should be truncated`)
  }
  for (const name of ['sys_user', 'sys_tenant', 'sys_role', 'sys_resource', 'sys_dict_data', 'sys_dict_type',
    'sys_region_code', 'sys_config', 'sys_job_config', 'sys_client', 'sys_message_template', 'forge_schema_history',
    'sys_flow_template', 'sys_flow_spel_template', 'sys_flow_comment_phrase', 'ai_business_field_template',
    'ai_page_template', 'ai_prompt_template', 'ai_provider_template', 'ACT_GE_PROPERTY', 'ACT_ID_PROPERTY',
    'FLW_EV_DATABASECHANGELOG', 'qrtz_locks', 'QRTZ_JOB_DETAILS']) {
    assert.doesNotMatch(truncated, new RegExp(`- ${name}\\s`), `${name} must be kept`)
  }
})

test('SQL 计划保留超级管理员、默认租户并清理低代码菜单', () => {
  const { result } = runClean(['--print-sql'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  const sql = result.stdout.slice(result.stdout.indexOf('== SQL =='))
  assert.match(sql, /SET @tenant_id = 1, @admin_user_id = 1, @admin_role_id = 1;/)
  assert.match(sql, /DELETE FROM sys_tenant WHERE id <> @tenant_id;/)
  assert.match(sql, /DELETE FROM sys_user WHERE id <> @admin_user_id;/)
  assert.match(sql, /UPDATE sys_user SET tenant_id = @tenant_id, avatar = NULL, last_login_time = NULL/)
  assert.match(sql, /DELETE FROM sys_role WHERE id <> @admin_role_id;/)
  assert.match(sql, /path LIKE '\/ai\/crud-page\/%' AND path NOT LIKE '\/ai\/crud-page\/:%'/)
  assert.match(sql, /perms LIKE 'ai:business:application:%:page:%'/)
  assert.match(sql, /DELETE FROM `sys_dict_data` WHERE tenant_id NOT IN \(0, @tenant_id\);/)
  assert.match(sql, /DELETE FROM `sys_job_config` WHERE `del_flag` <> 0;/)
  assert.match(sql, /DELETE FROM `sys_flow_spel_template` WHERE `deleted` <> 0;/)
  assert.match(sql, /DELETE FROM sys_role_module_data_scope WHERE role_id <> @admin_role_id;/)
  assert.match(sql, /DELETE FROM ai_agent WHERE agent_code NOT IN \('dashboard_generator'\);/)
  assert.match(sql, /SELECT '本地存储', 'local', 1, 1/)
  assert.doesNotMatch(sql, /TRUNCATE TABLE `(sys_job_config|sys_dict_data|sys_resource|sys_user)`;/)
  assert.doesNotMatch(sql, /DROP TABLE IF EXISTS `(sys_user|sys_resource|forge_schema_history)`;/)
  for (const name of CODE_BACKED_DEMO) assert.doesNotMatch(sql, new RegExp(`DROP TABLE IF EXISTS \`${name}\`;`))
  // 菜单必须在低代码元数据被清空之前清理
  assert.ok(sql.indexOf('forge_clean_menu_ids') < sql.indexOf('TRUNCATE TABLE `ai_crud_config`;'))
})

test('增量脚本未执行完时拒绝清理', () => {
  const { result, executed } = runClean(['--execute', '--yes'], { applied: allVersions().slice(0, 10) })
  assert.equal(result.status, 1, result.stdout + result.stderr)
  assert.match(result.stderr, /增量脚本尚未执行/)
  assert.equal(executed, '')
})

test('找不到超级管理员时拒绝清理', () => {
  const { result, executed } = runClean(['--execute', '--yes'], { adminFound: false })
  assert.equal(result.status, 1, result.stdout + result.stderr)
  assert.match(result.stderr, /超级管理员账号 admin 不存在/)
  assert.equal(executed, '')
})

test('--execute --yes 把计划交给 MySQL 执行', () => {
  const { result, executed } = runClean(['--execute', '--yes'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(executed, /^-- Forge template cleanup plan for forge_admin/)
  assert.match(executed, /SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;\n$/)
  assert.match(result.stdout, /Cleanup completed for forge_admin/)
})

test('--keep-table / --keep-business-tables / --extra-sql 可调整清理范围', () => {
  const extra = path.join(mkdtempSync(path.join(tmpdir(), 'forge-extra-sql-')), 'extra.sql')
  writeFileSync(extra, "DELETE FROM sys_resource WHERE path = '/pages/test';\n")
  const { result } = runClean(['--keep-table', 'crm_customer', '--keep-business-tables', '--extra-sql', extra,
    '--print-sql'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.doesNotMatch(result.stdout, /`crm_customer`/)
  assert.match(result.stdout, /TRUNCATE TABLE `pw_purchase_order`;/)
  assert.doesNotMatch(result.stdout, /DROP TABLE IF EXISTS `pw_purchase_order`;/)
  assert.match(result.stdout, /-- 9\. 项目自定义清理[\s\S]*DELETE FROM sys_resource WHERE path = '\/pages\/test';/)
})

function historyFixture() {
  const names = [MAIN_HISTORY_TABLE, ...PLUGIN_HISTORY_TABLES]
  return {
    names,
    tables: [...new Set([...realTables(), ...names])],
    // Flyway 默认没有这些列；模拟扩展列，防止通用行级规则成为清理保护的旁路。
    columns: { ...COLUMNS, ...Object.fromEntries(names.map(name => [name, ['tenant_id', 'del_flag', 'deleted']])) },
  }
}

function assertHistoryUntouched(sql, names) {
  for (const name of names) {
    assert.doesNotMatch(sql, new RegExp(`\\b${name}\\b`, 'i'), `${name} must not occur in the cleanup SQL`)
  }
}

test('默认预览与 SQL 计划完整保护主库和插件迁移历史，兼容任意前缀与大小写', () => {
  const fixture = historyFixture()
  const { result, executed } = runClean(['--print-sql'], fixture)
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.equal(executed, '')
  for (const title of ['将删除的表（DROP）', '将清空的表（TRUNCATE）']) {
    assertHistoryUntouched(section(result.stdout, title), fixture.names)
  }
  assertHistoryUntouched(result.stdout.slice(result.stdout.indexOf('== SQL ==')), fixture.names)
  // 保护不能误伤普通历史或原有租户/逻辑删除规则。
  assert.match(result.stdout, /TRUNCATE TABLE `sys_user_password_history`;/)
  assert.match(result.stdout, /DELETE FROM `sys_dict_data` WHERE tenant_id NOT IN \(0, @tenant_id\);/)
  assert.match(result.stdout, /DELETE FROM `sys_job_config` WHERE `del_flag` <> 0;/)
})

test('执行模式交给 MySQL 的计划同样不写入迁移历史', () => {
  const fixture = historyFixture()
  const { result, executed, mysqlCalls } = runClean(['--execute', '--yes'], fixture)
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.ok(mysqlCalls > 0)
  assert.ok(executed.length > 0)
  assertHistoryUntouched(executed, fixture.names)
  assert.match(executed, /TRUNCATE TABLE `sys_login_log`;/)
  assert.match(executed, /SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;\n$/)
})

for (const name of [MAIN_HISTORY_TABLE, MAIN_HISTORY_TABLE.toUpperCase(), ...PLUGIN_HISTORY_TABLES]) {
  test(`显式删除 ${name} 在连接 MySQL 前拒绝，keep 参数不能掩盖非法 drop`, () => {
    for (const keep of [[], ['--keep-table', name]]) {
      const { result, executed, mysqlCalls } = runClean([
        '--execute', '--yes', ...keep, '--drop-table', 'sys_job_log', '--drop-table', name,
      ])
      assert.equal(result.status, 1, result.stdout + result.stderr)
      assert.match(result.stderr, /迁移历史表不能通过 --drop-table 删除/)
      assert.match(result.stderr, new RegExp(name.toLowerCase()))
      assert.equal(executed, '')
      assert.equal(mysqlCalls, 0)
    }
  })
}

test('插件工作台任务及私有ZIP不会留在清理后的模板库', () => {
  const { result } = runClean(['--print-sql'], { tables: [...realTables(), 'sys_plugin_task'] })
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(result.stdout, /TRUNCATE TABLE `sys_plugin_task`;/)
})

test('迁移历史保护不扩大到普通历史与备份副本', () => {
  const dropped = ['acme_plugin_hello_history_bak', 'forge_plugin_hello_history_tmp',
    'forge_plugin_hello_history_20261007', 'business_approval_history']
  const truncated = ['sys_user_password_history', 'forge_business_history', 'forge_plugin_history']
  const { result } = runClean(['--print-sql'], { tables: [...new Set([...realTables(), ...dropped, ...truncated])] })
  assert.equal(result.status, 0, result.stdout + result.stderr)
  for (const name of dropped) assert.match(result.stdout, new RegExp(`DROP TABLE IF EXISTS \`${name}\`;`))
  for (const name of truncated) assert.match(result.stdout, new RegExp(`TRUNCATE TABLE \`${name}\`;`))
})

test('保留业务表模式仍保护迁移历史，普通表显式 drop 继续生效', () => {
  const fixture = historyFixture()
  const { result } = runClean(['--keep-business-tables', '--drop-table', 'sys_job_log', '--print-sql'], fixture)
  assert.equal(result.status, 0, result.stdout + result.stderr)
  const sql = result.stdout.slice(result.stdout.indexOf('== SQL =='))
  assertHistoryUntouched(sql, fixture.names)
  assert.match(sql, /DROP TABLE IF EXISTS `sys_job_log`;/)
  assert.doesNotMatch(sql, /TRUNCATE TABLE `sys_job_log`;/)
  assert.match(sql, /TRUNCATE TABLE `crm_customer`;/)
})

test('帮助文本说明迁移历史保护及自定义 SQL 的人工审核边界', () => {
  const { result, executed, mysqlCalls } = runClean(['--help'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(result.stdout, /主库与插件 Flyway 迁移历史/)
  assert.match(result.stdout, /禁止指定主库或插件迁移历史表/)
  assert.match(result.stdout, /需人工审核，不受迁移历史保护规则拦截/)
  assert.equal(executed, '')
  assert.equal(mysqlCalls, 0)
})
