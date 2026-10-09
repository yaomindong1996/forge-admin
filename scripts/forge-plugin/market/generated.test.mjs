import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { temporary, repository } from '../fixtures/helpers.mjs'

test('full generated project keeps market tools byte-for-byte and passes DB stub maintenance tests', async t => {
  const root = await temporary(t)
  const generated = path.join(root, 'delivery-check')
  // 子测试作为独立 Node 进程输出 TAP，不能继承父测试的 IPC 上下文。
  const env = { ...process.env }
  delete env.NODE_TEST_CONTEXT
  const create = spawnSync(process.execPath, ['scripts/forge-create/create-project.mjs', generated,
    '--preset', 'full', '--base-package', 'com.example.delivery', '--display-name', '交付验证',
    '--java-name', 'DeliveryCheck', '--database-name', 'delivery_check'],
  { cwd: repository, env, encoding: 'utf8', timeout: 120000, maxBuffer: 8 * 1024 * 1024 })
  assert.equal(create.status, 0, create.stderr + create.stdout)
  for (const name of ['config', 'http', 'sso', 'catalog', 'source', 'commands']) {
    const relative = `scripts/forge-plugin/market/${name}.mjs`
    const source = await fs.readFile(path.join(repository, relative))
    assert.deepEqual(await fs.readFile(path.join(generated, relative)), source)
  }
  for (const name of ['config', 'document', 'prepare', 'index']) {
    const relative = `scripts/forge-plugin-release/compose/${name}.mjs`
    const source = await fs.readFile(path.join(repository, relative))
    assert.deepEqual(await fs.readFile(path.join(generated, relative)), source)
  }
  for (const relative of ['scripts/forge-plugin-release/cos/config.mjs',
    'scripts/forge-plugin-release/cos/store.mjs', 'scripts/forge-plugin-release/cos/transfer.mjs',
    'scripts/forge-plugin-release/delivery/executor.mjs', 'scripts/forge-plugin-release/delivery/session.mjs',
    'scripts/forge-plugin-release/delivery/runtime.mjs', 'scripts/forge-plugin-release/package.json',
    'scripts/forge-plugin-release/package-lock.json']) {
    assert.deepEqual(await fs.readFile(path.join(generated, relative)), await fs.readFile(path.join(repository, relative)))
  }
  await assert.rejects(fs.stat(path.join(generated, 'scripts/forge-plugin-release/node_modules')), { code: 'ENOENT' })
  await assert.rejects(fs.stat(path.join(generated, 'scripts/forge-plugin-release/cos/fixtures')), { code: 'ENOENT' })
  await assert.rejects(fs.stat(path.join(generated, 'scripts/forge-plugin/market/fixtures')), { code: 'ENOENT' })
  const db = spawnSync(process.execPath, ['--test', 'init-db.test.mjs', 'clean-db.test.mjs'],
    { cwd: path.join(generated, 'delivery-check-server/scripts/db'), env, encoding: 'utf8', timeout: 60000 })
  assert.equal(db.status, 0, db.stderr + db.stdout)
  assert.match(db.stdout, /# fail 0/)
  const help = spawnSync(process.execPath, [path.join(generated, 'scripts/forge-plugin/index.mjs'), '--help'],
    { cwd: root, env, encoding: 'utf8', timeout: 5000 })
  assert.equal(help.status, 0, help.stderr)
  assert.match(help.stdout, /market add/)
})
