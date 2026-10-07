import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { addPlugin, listPlugins, removePlugin } from './installer.mjs'
import { readBundle } from './bundle.mjs'
import { descriptor, plugin, pluginFiles, project, snapshot, temporary, write, zip, git, commit }
  from './fixtures/helpers.mjs'

const localFiles = ['server/src/main/resources/application-dev.yml', 'ui/.env.local', 'ui/config/.env.production']
for (const generated of [false, true]) {
  for (const mode of ['no-git', 'ignored-directory', 'ignored-file', 'committed-file']) {
    for (const relative of localFiles) {
      test(`local config protected: ${generated ? 'renamed' : 'template'} / ${mode} / ${relative}`, async t => {
        const host = await project(t, generated)
        const source = await plugin(t)
        await addPlugin(host.root, source)
        const module = generated ? 'core-plugin-demo' : 'forge-plugin-demo'
        const file = relative.replace(/^server/, `${host.server}/plugins/${module}`)
          .replace(/^ui/, `${host.ui}/src/views/plugins/demo`)
        if (mode !== 'no-git') {
          git(host.root, ['init', '-q'])
          const ignored = mode === 'ignored-directory'
            ? `${host.server}/plugins/\n${host.ui}/src/views/plugins/\n`
            : mode === 'ignored-file' ? `${file}\n` : ''
          await write(host.root, '.gitignore', `.forge-plugin/\n${ignored}`)
          commit(host.root)
        }
        await write(host.root, file, 'fixture: local-value-must-not-be-logged\n')
        if (mode === 'committed-file') {
          commit(host.root)
        }
        const before = await snapshot(host.root)
        await assert.rejects(addPlugin(host.root, source, { force: true }), /本地配置/)
        await assert.rejects(removePlugin(host.root, 'demo'), /本地配置/)
        assert.deepEqual(await snapshot(host.root), before)
        assert.equal((await listPlugins(host.root))[0].version, '1.0.0')
      })
    }
  }
}

for (const archive of [false, true]) {
  test(`delivery still excludes local configuration: ${archive ? 'ZIP' : 'directory'}`, async t => {
    const root = await temporary(t)
    const clean = pluginFiles()
    const files = new Map(clean)
    files.set('server/forge-plugin-demo/src/main/resources/application-dev.yml', Buffer.from('fixture: local'))
    files.set('ui/.env.local', Buffer.from('FIXTURE=local'))
    files.set('ui/config/.env.production', Buffer.from('FIXTURE=local'))
    for (const [relative, data] of files) {
      await write(root, relative, data)
    }
    await write(root, 'package.zip', zip(files))
    const result = await readBundle(archive ? path.join(root, 'package.zip') : root)
    result.files.delete('package.zip')
    assert.deepEqual(result.files, clean)
  })
}

for (const remove of [false, true]) {
  test(`config added after preflight refuses ${remove ? 'remove' : 'upgrade'} and preserves user file`, async t => {
    const host = await project(t, true)
    const source = await plugin(t)
    await addPlugin(host.root, source)
    const ui = `${host.ui}/src/views/plugins/demo`
    const target = `${ui}/.env.local`
    const before = await snapshot(host.root)
    const hooks = { beforeWrite: async ({ relative }) => {
      // server 操作已经完成，在 UI 写入前新增配置，验证复检和前一个组件的回滚。
      if (relative === ui) {
        await write(host.root, target, 'fixture: added-after-preflight\n')
      }
    } }
    const action = remove ? removePlugin(host.root, 'demo', hooks)
      : addPlugin(host.root, await plugin(t, descriptor({ version: '1.0.1' })), { force: true }, hooks)
    await assert.rejects(action, /本地配置/)
    const expected = { ...before, [target]: Buffer.from('fixture: added-after-preflight\n').toString('base64') }
    assert.deepEqual(await snapshot(host.root), expected)
    await assert.rejects(fs.lstat(path.join(host.root, '.forge-plugin/lock')), { code: 'ENOENT' })
  })
}

test('installed configuration symlink is refused without reading its target', async t => {
  const host = await project(t)
  const source = await plugin(t)
  await addPlugin(host.root, source)
  await fs.symlink('/nonexistent-fixture-config',
    path.join(host.root, host.ui, 'src/views/plugins/demo/.env.local'))
  await assert.rejects(addPlugin(host.root, source, { force: true }), /本地配置/)
  await assert.rejects(removePlugin(host.root, 'demo'), /本地配置/)
})
