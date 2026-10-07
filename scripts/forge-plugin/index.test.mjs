import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import test from 'node:test'
import { runPluginCli } from './index.mjs'

for (const args of [[], ['--help'], ['-h']]) {
  test(`plugin bootstrap help ${JSON.stringify(args)} reports its stage clearly`, () => {
    const messages = []
    const output = { log: text => messages.push(text), error: () => assert.fail('help must not fail') }
    assert.equal(runPluginCli(args, output), 0)
    assert.match(messages[0], /T8 开放/)
  })
}

test('unknown plugin commands fail with usage guidance', () => {
  const errors = []
  assert.equal(runPluginCli(['unknown'], { error: text => errors.push(text) }), 1)
  assert.match(errors[0], /未知插件命令/)
})

for (const args of [['add', './sample', '--force'], ['add', './sample', '--dev'], ['list'], ['remove', 'hello']]) {
  test(`planned command ${args.join(' ')} exits nonzero and cannot mutate the project`, async (t) => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-plugin-bootstrap-'))
    t.after(() => fs.rm(root, { recursive: true, force: true }))
    const config = '{"plugins":[]}'
    await fs.writeFile(path.join(root, 'forge.config.json'), config)
    const script = fileURLToPath(new URL('./index.mjs', import.meta.url))
    const result = spawnSync(process.execPath, [script, ...args], {cwd: root, encoding: 'utf8'})
    assert.equal(result.status, 1)
    assert.match(result.stderr, /尚未开放/)
    assert.deepEqual(await fs.readdir(root), ['forge.config.json'])
    assert.equal(await fs.readFile(path.join(root, 'forge.config.json'), 'utf8'), config)
  })
}
