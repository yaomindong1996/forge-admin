import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..')
const migrationDir = path.join(root, 'forge-server/db/migration')
const migrationName = 'V1.0.208__expand_tenant_theme_config.sql'
const read = file => readFileSync(path.join(root, file), 'utf8')
const sql = read(`forge-server/db/migration/${migrationName}`)

// 只检查脚本契约，不连接 MySQL、不执行迁移；实际 DDL 验收由用户执行。
test('新版本唯一，旧迁移不复用', () => {
  assert.deepEqual(readdirSync(migrationDir).filter(file => /^V1\.0\.208__/.test(file)), [migrationName])
})

test('扩容限定当前库的租户主题列，重复执行与更宽列不降级', () => {
  assert.match(sql, /FROM information_schema\.COLUMNS/)
  assert.match(sql, /TABLE_SCHEMA = DATABASE\(\)/)
  assert.match(sql, /TABLE_NAME = 'sys_tenant'/)
  assert.match(sql, /COLUMN_NAME = 'theme_config'/)
  assert.match(sql, /DATA_TYPE IN \('char', 'varchar', 'tinytext'\)/)
  assert.match(sql, /COALESCE\(@tenant_theme_expand_sql, 'SELECT 1'\)/)
  assert.match(sql, /PREPARE tenant_theme_expand_stmt FROM @tenant_theme_expand_sql;/)
  assert.match(sql, /EXECUTE tenant_theme_expand_stmt;/)
  assert.match(sql, /DEALLOCATE PREPARE tenant_theme_expand_stmt;/)
})

test('文本扩容保留字符集和排序规则，不截断或改写租户数据', () => {
  assert.match(sql, /MODIFY COLUMN `theme_config` TEXT CHARACTER SET/)
  assert.match(sql, /CHARACTER_SET_NAME/)
  assert.match(sql, /' COLLATE ', COLLATION_NAME/)
  assert.match(sql, /NULL COMMENT ''主题配置''/)
  const statements = sql.replace(/^\s*--.*$/gm, '')
  assert.doesNotMatch(statements, /\b(?:UPDATE|INSERT|DELETE|DROP|TRUNCATE|LEFT|SUBSTRING)\b/i)
  assert.doesNotMatch(sql, /\$\{[^}]+\}/)
})

test('全量与 Docker 初始化均使用 TEXT，两个脚本字节一致', () => {
  const full = read('forge-server/db/全量初始化SQL.sql')
  const docker = read('docker-forge-admin/init-sql/01-init.sql')
  assert.match(full, /`theme_config` text DEFAULT NULL COMMENT '主题配置'/)
  assert.doesNotMatch(full, /`theme_config` varchar\(1000\)/)
  assert.equal(docker, full)
})

test('Java DTO 与实体继续使用 String 配置协议，无 1000 字符限制', () => {
  const base = 'forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/'
    + 'com/mdframe/forge/plugin/system/'
  for (const file of ['dto/SysTenantDTO.java', 'entity/SysTenant.java']) {
    assert.match(read(base + file), /private String themeConfig;/)
    assert.doesNotMatch(read(base + file), /@(?:Size|Length)\([^)]*1000[^)]*\)\s*private String themeConfig;/)
  }
})
