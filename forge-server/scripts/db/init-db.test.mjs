import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { existsSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const serverDir = path.resolve(scriptDir, '../..')
const source = readFileSync(path.join(scriptDir, 'init-db.sh'), 'utf8')
  .replace(/^SCRIPT_DIR=.*$/m, `SCRIPT_DIR=${JSON.stringify(scriptDir)}`)
  .replace(/^FORGE_DIR=.*$/m, `FORGE_DIR=${JSON.stringify(serverDir)}`)
const sqlPath = '$FORGE_DIR/db/mysql/全量初始化SQL.sql'

// MySQL 调用全部由当前子 Shell 的函数接管，不执行任何真实数据库操作。
function runInit(script = source, args = [], mysqlStatus = 0) {
  return spawnSync('bash', ['--noprofile', '--norc', '-c', `
mysql() { printf 'MYSQL_STUB_CALLED\\n'; return ${mysqlStatus}; }
${script}
`, 'init-db-test', ...args], {
    encoding: 'utf8', timeout: 30000,
    env: { ...process.env, BASH_ENV: '', ENV: '' },
  })
}

function assertRejectedBeforeMysql(result) {
  assert.ifError(result.error)
  assert.equal(result.status, 1, result.stdout + result.stderr)
  assert.doesNotMatch(result.stdout, /MYSQL_STUB_CALLED|initialization completed/)
  assert.match(result.stderr, /初始化 SQL/)
}

test('全量 SQL 缺失时在调用 MySQL 前失败', () => {
  assertRejectedBeforeMysql(runInit(source.replaceAll(sqlPath, '$FORGE_DIR/__missing_init_test__.sql')))
})

test('全量 SQL 为空时在调用 MySQL 前失败', () => {
  assertRejectedBeforeMysql(runInit(source.replaceAll(sqlPath, '/dev/null')))
})

test('显式跳过全量初始化时不要求该文件存在', () => {
  const result = runInit(source.replaceAll(sqlPath, '$FORGE_DIR/__missing_init_test__.sql'), ['--skip-admin-init'])
  assert.ifError(result.error)
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(result.stdout, /initialization completed/)
  assert.doesNotMatch(result.stdout, /Running .*__missing_init_test__/)
})

test('默认流程会将仓库中真实全量 SQL 交给 MySQL 桩', () => {
  const result = runInit()
  assert.ifError(result.error)
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(result.stdout, /Running .*db\/mysql\/全量初始化SQL\.sql/)
  assert.match(result.stdout, /MYSQL_STUB_CALLED/)
})

test('MySQL 执行失败时不输出初始化成功', () => {
  const result = runInit(source, [], 2)
  assert.equal(result.status, 2, result.stdout + result.stderr)
  assert.doesNotMatch(result.stdout, /initialization completed/)
})

test('帮助使用当前 SQL 路径且不连接数据库', () => {
  const result = runInit(source, ['--help'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(result.stdout, /db\/mysql\/全量初始化SQL\.sql/)
  assert.doesNotMatch(result.stdout, /MYSQL_STUB_CALLED|forge-admin-server\/sql/)
})

// 按 SQL 内容应答的 MySQL 桩：记录每次调用，information_schema 表数量由 TABLE_COUNT 控制
function runWithRecorder(args, { tableCount = 0, dir = scriptDir } = {}) {
  const tmp = mkdtempSync(path.join(tmpdir(), 'forge-init-db-'))
  const log = path.join(tmp, 'calls.log')
  const script = source.replace(/^SCRIPT_DIR=.*$/m, `SCRIPT_DIR=${JSON.stringify(dir)}`)
  const result = spawnSync('bash', ['--noprofile', '--norc', '-c', `
mysql() {
  local q="" a
  for a in "$@"; do case "$a" in --execute=*) q="\${a#--execute=}" ;; esac; done
  printf 'MYSQL %s\\n' "$q" | tr '\\n' ' ' >> ${JSON.stringify(log)}; echo >> ${JSON.stringify(log)}
  case "$q" in
    *"VERSION()"*) echo "8.0.36" ;;
    *information_schema.tables*) echo "${tableCount}" ;;
    "") cat > /dev/null ;;
  esac
}
mvn() { printf 'MVN %s\\n' "$*" >> ${JSON.stringify(log)}; }
${script}
`, 'init-db-test', ...args], {
    encoding: 'utf8', timeout: 30000,
    env: { ...process.env, BASH_ENV: '', ENV: '' },
  })
  const calls = existsSync(log) ? readFileSync(log, 'utf8') : ''
  return { result, calls }
}

test('非空库拒绝重复导入全量 SQL', () => {
  const { result, calls } = runWithRecorder([], { tableCount: 12 })
  assert.equal(result.status, 1, result.stdout + result.stderr)
  assert.match(result.stderr, /已有 12 张表.*--recreate/)
  assert.doesNotMatch(calls, /DROP DATABASE/)
  assert.doesNotMatch(result.stdout, /initialization completed/)
})

test('--recreate --yes 先删库再导入', () => {
  const { result, calls } = runWithRecorder(['--recreate', '--yes'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  const dropAt = calls.indexOf('DROP DATABASE IF EXISTS `forge_admin`')
  const createAt = calls.indexOf('CREATE DATABASE IF NOT EXISTS `forge_admin`')
  assert.ok(dropAt >= 0 && createAt > dropAt, calls)
  assert.match(result.stdout, /Running .*db\/mysql\/全量初始化SQL\.sql/)
})

test('废弃的 --with-demo 仅告警且不影响默认初始化', () => {
  const { result, calls } = runWithRecorder(['--with-demo'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(result.stderr, /--with-demo 已废弃/)
  assert.match(calls, /CREATE DATABASE/)
})

test('--migrate 在全量之后调用独立 Flyway 执行器', () => {
  const { result, calls } = runWithRecorder(['--migrate', '--database', 'demo_db', '--port', '3307'])
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(calls, /MVN -q -B -f .*scripts\/db\/flyway\/pom\.xml flyway:migrate/)
  assert.match(calls, /-Dforge\.db\.url=jdbc:mysql:\/\/127\.0\.0\.1:3307\/demo_db\?/)
  assert.match(calls, /-Dforge\.migration\.dir=.*\/db\/mysql\/migration/)
  assert.ok(result.stdout.indexOf('全量初始化SQL') < result.stdout.indexOf('Running Flyway'), result.stdout)
})

test('缺少 Maven 时 --migrate 在连接数据库前给出替代方案', () => {
  const result = spawnSync('bash', ['--noprofile', '--norc', '-c', `
mysql() { printf 'MYSQL_STUB_CALLED\\n'; }
${source}
`, 'init-db-test', '--migrate'], {
    encoding: 'utf8', timeout: 30000,
    env: { ...process.env, BASH_ENV: '', ENV: '', PATH: '/usr/bin:/bin' },
  })
  assert.equal(result.status, 1, result.stdout + result.stderr)
  assert.match(result.stderr, /需要 Maven.*clean-db\.sh --execute/)
  assert.doesNotMatch(result.stdout, /MYSQL_STUB_CALLED/)
})

test('--clean 执行增量后调用 clean-db.sh 并透传参数', () => {
  const fakeDir = mkdtempSync(path.join(tmpdir(), 'forge-fake-scripts-'))
  const cleanLog = path.join(fakeDir, 'clean.log')
  writeFileSync(path.join(fakeDir, 'clean-db.sh'),
    `printf 'CLEAN %s PWD=%s\\n' "$*" "$MYSQL_PWD" > ${JSON.stringify(cleanLog)}\n`)
  const { result, calls } = runWithRecorder(
    ['--clean', '--yes', '--password', 'p@ss', '--clean-arg', '--admin-username', '--clean-arg', 'root_admin'],
    { dir: fakeDir },
  )
  assert.equal(result.status, 0, result.stdout + result.stderr)
  assert.match(calls, /MVN .*flyway:migrate/)
  const cleanCall = readFileSync(cleanLog, 'utf8')
  assert.match(cleanCall, /--database forge_admin/)
  assert.match(cleanCall, /--execute --yes --admin-username root_admin/)
  assert.match(cleanCall, /PWD=p@ss/)
})
