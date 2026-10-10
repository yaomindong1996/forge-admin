import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import test from 'node:test'
import { runPluginCli } from './index.mjs'
import { plugin, project, repository, snapshot, temporary, write } from './fixtures/helpers.mjs'

for (const args of [[], ['--help'], ['-h']]) {
  test(`plugin help ${JSON.stringify(args)} reports actual capabilities`, async () => {
    const messages = []
    const output = { log: text => messages.push(text), error: () => assert.fail('help must not fail') }
    assert.equal(await runPluginCli(args, output), 0)
    assert.match(messages[0], /add <目录\|包.zip>/)
    assert.match(messages[0], /不删除数据库/)
  })
}

test('unknown plugin commands fail with usage guidance', async () => {
  const errors = []
  assert.equal(await runPluginCli(['unknown'], { error: text => errors.push(text) }), 1)
  assert.match(errors[0], /未知插件命令/)
})

for (const args of [['add'], ['remove'], ['list', '--force'], ['remove', 'demo', '--force'],
  ['add', './sample', '--oops'], ['add', './sample', '--force', '--force'], ['add', '--dev']]) {
  test(`invalid command ${args.join(' ')} is non-mutating`, async t => {
    const { root } = await project(t)
    const before = await snapshot(root)
    assert.equal(await runPluginCli(args, { error: () => {} }, { root }), 1)
    assert.deepEqual(await snapshot(root), before)
  })
}

test('CLI add/list/remove, relative source and recovery messages', async t => {
  const { root } = await project(t)
  const source = await plugin(t)
  const messages = []
  const output = { log: value => messages.push(value), error: value => assert.fail(value) }
  const environment = { root, cwd: path.dirname(source) }
  assert.equal(await runPluginCli(['add', path.basename(source)], output, environment), 0)
  assert.equal(await runPluginCli(['list'], output, environment), 0)
  assert.match(messages.join('\n'), /demo\t1.0.0\tcopy/)
  assert.equal(await runPluginCli(['remove', 'demo'], output, environment), 0)
  assert.match(messages.join('\n'), /恢复备份/)
  assert.match(messages.join('\n'), /数据库表、数据和迁移历史未删除/)
})

test('copied entry targets its own project, not arbitrary invocation cwd', async t => {
  const { root } = await project(t)
  const cwd = await temporary(t)
  await write(cwd, 'forge.config.json', 'must remain unchanged')
  for (const directory of ['forge-plugin', 'forge-shared']) {
    await fs.cp(path.join(repository, 'scripts', directory), path.join(root, 'scripts', directory), { recursive: true })
  }
  const result = spawnSync(process.execPath, [path.join(root, 'scripts/forge-plugin/index.mjs'), 'list'],
    { cwd, encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr)
  assert.match(result.stdout, /未安装插件/)
  assert.equal(await fs.readFile(path.join(cwd, 'forge.config.json'), 'utf8'), 'must remain unchanged')
})
