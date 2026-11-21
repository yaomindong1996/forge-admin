#!/usr/bin/env node
/**
 * 计算下一个 Flyway 增量版本号（避免多人并行撞号）。
 *
 * 水位取三者最大值再 +1：
 * 1) 本地 db/mysql/migration 文件
 * 2) origin/main（若可访问）同路径文件
 * 3) 可选：forge_schema_history 已执行版本（--jdbc / 环境变量）
 *
 * Usage:
 *   node forge-server/scripts/db/next-migration-version.mjs
 *   node forge-server/scripts/db/next-migration-version.mjs --desc add_foo
 *   node forge-server/scripts/db/next-migration-version.mjs --jdbc 'jdbc:mysql://127.0.0.1:3306/forge_admin_local' --user root --password 'xxx'
 *   FORGE_FLYWAY_JDBC_URL=... FORGE_DB_USERNAME=... FORGE_DB_PASSWORD=... node ... --from-db
 */
import { execFileSync, spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const forgeDir = path.resolve(__dirname, '../..')
const mysqlDir = path.join(forgeDir, 'db/mysql/migration')
const VERSION_RE = /^V(\d+)\.(\d+)\.(\d+)__/

function parseArgs(argv) {
  const out = { desc: '', fromDb: false, jdbc: '', user: '', password: '', create: false }
  for (let i = 0; i < argv.length; i += 1) {
    const a = argv[i]
    if (a === '--desc') out.desc = String(argv[++i] || '').trim()
    else if (a === '--from-db') out.fromDb = true
    else if (a === '--jdbc') out.jdbc = String(argv[++i] || '').trim()
    else if (a === '--user') out.user = String(argv[++i] || '').trim()
    else if (a === '--password') out.password = String(argv[++i] || '')
    else if (a === '--create') out.create = true
    else if (a === '--help' || a === '-h') out.help = true
  }
  return out
}

function parseVersion(name) {
  const m = VERSION_RE.exec(name)
  if (!m) return null
  return [Number(m[1]), Number(m[2]), Number(m[3])]
}

function cmp(a, b) {
  for (let i = 0; i < 3; i += 1) {
    if (a[i] !== b[i]) return a[i] - b[i]
  }
  return 0
}

function formatVersion(v) {
  return `${v[0]}.${v[1]}.${v[2]}`
}

function bump(v) {
  return [v[0], v[1], v[2] + 1]
}

function maxVersion(versions) {
  return versions.reduce((acc, cur) => (cmp(cur, acc) > 0 ? cur : acc))
}

function versionsFromDir(dir) {
  if (!fs.existsSync(dir)) return []
  return fs.readdirSync(dir)
    .map(parseVersion)
    .filter(Boolean)
}

function versionsFromGitRef(ref, relDir) {
  try {
    const out = execFileSync('git', ['ls-tree', '-r', '--name-only', ref, relDir], {
      cwd: path.resolve(forgeDir, '..'),
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    })
    return out.split('\n')
      .map((line) => path.basename(line.trim()))
      .map(parseVersion)
      .filter(Boolean)
  } catch {
    return []
  }
}

function parseJdbcMysql(jdbc) {
  // jdbc:mysql://host:port/db?...
  const m = /^jdbc:mysql:\/\/([^/?]+)\/([^?]+)/.exec(jdbc || '')
  if (!m) return null
  const hostPort = m[1]
  const database = m[2]
  const [host, port = '3306'] = hostPort.split(':')
  return { host, port, database }
}

function versionsFromMysqlHistory({ jdbc, user, password }) {
  const parsed = parseJdbcMysql(jdbc)
  if (!parsed) {
    console.error('无法解析 JDBC（仅支持 jdbc:mysql://host:port/db）')
    process.exit(2)
  }
  const sql = "SELECT version FROM forge_schema_history WHERE version IS NOT NULL AND success = 1"
  const args = [
    `-h${parsed.host}`,
    `-P${parsed.port}`,
    `-u${user}`,
    `-N`,
    `-e`,
    sql,
    parsed.database,
  ]
  const env = { ...process.env }
  if (password != null && password !== '') env.MYSQL_PWD = password
  const result = spawnSync('mysql', args, { encoding: 'utf8', env })
  if (result.status !== 0) {
    console.error(result.stderr || result.stdout || 'mysql 查询失败')
    process.exit(2)
  }
  return String(result.stdout || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((v) => {
      const parts = v.split('.').map(Number)
      if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return null
      return parts
    })
    .filter(Boolean)
}

function snakeDesc(desc) {
  return String(desc || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 80)
}

function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(`Usage:
  node forge-server/scripts/db/next-migration-version.mjs [--desc name] [--create] [--from-db]
  --from-db 读取 FORGE_FLYWAY_JDBC_URL / FORGE_DB_USERNAME / FORGE_DB_PASSWORD
  或显式 --jdbc --user --password`)
    process.exit(0)
  }

  const sources = []
  const local = versionsFromDir(mysqlDir)
  if (local.length) sources.push({ name: 'local-files', max: maxVersion(local) })

  const remote = versionsFromGitRef('origin/main', 'forge-server/db/mysql/migration')
  if (remote.length) sources.push({ name: 'origin/main', max: maxVersion(remote) })

  const jdbc = args.jdbc || process.env.FORGE_FLYWAY_JDBC_URL || process.env.SPRING_DATASOURCE_URL || ''
  const user = args.user || process.env.FORGE_DB_USERNAME || process.env.MYSQL_USER || ''
  const password = args.password != null && args.password !== ''
    ? args.password
    : (process.env.FORGE_DB_PASSWORD || process.env.MYSQL_PWD || '')
  if (args.fromDb || args.jdbc) {
    if (!jdbc || !user) {
      console.error('--from-db / --jdbc 需要 JDBC 与用户名（可用环境变量 FORGE_FLYWAY_JDBC_URL、FORGE_DB_USERNAME）')
      process.exit(2)
    }
    const dbVersions = versionsFromMysqlHistory({ jdbc, user, password })
    if (dbVersions.length) sources.push({ name: 'forge_schema_history', max: maxVersion(dbVersions) })
  }

  if (!sources.length) {
    console.error('未找到任何版本水位（本地 migration / origin/main / DB）')
    process.exit(1)
  }

  const watermark = maxVersion(sources.map((s) => s.max))
  const next = bump(watermark)
  const nextText = formatVersion(next)

  console.log('watermark sources:')
  for (const s of sources) {
    console.log(`  - ${s.name}: ${formatVersion(s.max)}`)
  }
  console.log(`next: ${nextText}`)

  const desc = snakeDesc(args.desc)
  if (desc) {
    const file = `V${nextText}__${desc}.sql`
    console.log(`mysql:       forge-server/db/mysql/migration/${file}`)
    console.log(`postgresql:  forge-server/db/postgresql/migration/${file}`)
    console.log(`sqlserver:   forge-server/db/sqlserver/migration/${file}`)
    if (args.create) {
      const stub = `-- TODO: ${desc}\n`
      for (const dir of [
        path.join(forgeDir, 'db/mysql/migration'),
        path.join(forgeDir, 'db/postgresql/migration'),
        path.join(forgeDir, 'db/sqlserver/migration'),
      ]) {
        fs.mkdirSync(dir, { recursive: true })
        const target = path.join(dir, file)
        if (fs.existsSync(target)) {
          console.error(`已存在: ${target}`)
          process.exit(1)
        }
        fs.writeFileSync(target, stub)
      }
      console.log('created mysql/postgresql/sqlserver stubs')
    }
  }
}

main()
