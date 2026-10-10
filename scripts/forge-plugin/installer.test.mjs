import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { addPlugin, listPlugins, removePlugin } from './installer.mjs'
import { descriptor, plugin, pluginFiles, project, snapshot, temporary, write, zip, git, commit }
  from './fixtures/helpers.mjs'

for (const generated of [false, true]) {
  test(`install/list/force/remove in ${generated ? 'renamed' : 'template'} project`, async t => {
    const host = await project(t, generated)
    const source = await plugin(t)
    const beforeSource = await snapshot(source)
    const result = await addPlugin(host.root, source)
    const module = generated ? 'core-plugin-demo' : 'forge-plugin-demo'
    const target = `${host.server}/plugins/${module}`
    assert.equal(result.record.version, '1.0.0')
    const config = JSON.parse(await fs.readFile(path.join(host.root, 'forge.config.json'), 'utf8'))
    assert.equal(config.projectType, generated ? undefined : 'template')
    if (generated) {
      assert.deepEqual(config.customSetting, { kept: true })
    }
    const pom = await fs.readFile(path.join(host.root, target, 'pom.xml'), 'utf8')
    assert.ok(pom.includes(`<artifactId>${module}</artifactId>`))
    assert.match(pom, /<relativePath>\.\.\/\.\.\/pom.xml<\/relativePath>/)
    assert.ok(pom.includes(`<groupId>${generated ? host.options.groupId : 'com.mdframe.forge'}</groupId>`))
    const pkg = generated ? 'com/acme/app' : 'com/mdframe/forge'
    assert.match(await fs.readFile(path.join(host.root, target, `src/main/java/${pkg}/plugin/demo/Hello.java`), 'utf8'),
      generated ? /package com.acme.app/ : /package com.mdframe.forge/)
    assert.equal((await listPlugins(host.root)).length, 1)
    await write(host.root, `${target}/.flattened-pom.xml`, '<project/>')
    await write(host.root, `${target}/target/classes/Built.class`, 'build artifact')
    await assert.rejects(addPlugin(host.root, source), /--force/)
    const upgraded = await addPlugin(host.root, await plugin(t, descriptor({ version: '1.0.1' })), { force: true })
    assert.ok(upgraded.backup)
    const backup = JSON.parse(await fs.readFile(path.join(upgraded.backup, 'restore.json'), 'utf8'))
    assert.equal(backup.id, 'demo')
    assert.ok(backup.paths.every(item => item.relative && item.backup))
    assert.equal((await listPlugins(host.root))[0].version, '1.0.1')
    const removed = await removePlugin(host.root, 'demo')
    assert.ok(removed.backup)
    assert.equal((await listPlugins(host.root)).length, 0)
    assert.equal(await fs.readFile(path.join(host.root, `${host.server}/pom.xml`), 'utf8'), host.rootPom)
    assert.equal(await fs.readFile(path.join(host.root, `${host.server}/${host.admin}/pom.xml`), 'utf8'), host.adminPom)
    await assert.rejects(fs.lstat(path.join(host.root, target)), { code: 'ENOENT' })
    assert.deepEqual(await snapshot(source), beforeSource)
  })
}

test('ZIP installation, UI-only/server-only and multiple plugins have deterministic registration', async t => {
  const host = await project(t)
  const zipRoot = await temporary(t)
  await write(zipRoot, 'package.zip', zip(pluginFiles(), { method: 8 }))
  await addPlugin(host.root, path.join(zipRoot, 'package.zip'))
  await addPlugin(host.root, await plugin(t, descriptor({ id: 'aaa', server: null })))
  const serverOnly = await plugin(t, descriptor({ id: 'zzz', ui: null, server: { module: 'forge-plugin-zzz' } }))
  await addPlugin(host.root, serverOnly)
  assert.deepEqual((await listPlugins(host.root)).map(item => item.id), ['aaa', 'demo', 'zzz'])
  await removePlugin(host.root, 'demo')
  assert.deepEqual((await listPlugins(host.root)).map(item => item.id), ['aaa', 'zzz'])
})

test('force can change components and module name without orphaning old targets', async t => {
  const host = await project(t)
  await addPlugin(host.root, await plugin(t))
  await addPlugin(host.root, await plugin(t, descriptor({ ui: null, server: { module: 'forge-plugin-next' } })),
    { force: true })
  await assert.rejects(fs.lstat(path.join(host.root, host.ui, 'src/views/plugins/demo')), { code: 'ENOENT' })
  await assert.rejects(fs.lstat(path.join(host.root, host.server, 'plugins/forge-plugin-demo')), { code: 'ENOENT' })
  assert.ok((await fs.lstat(path.join(host.root, host.server, 'plugins/forge-plugin-next'))).isDirectory())
})

test('dev links do not mutate external sources, including remove after external edits', async t => {
  const host = await project(t)
  const source = await plugin(t)
  const before = await snapshot(source)
  await addPlugin(host.root, source, { dev: true })
  assert.equal((await listPlugins(host.root))[0].mode, 'dev')
  assert.ok((await fs.lstat(path.join(host.root, host.server, 'plugins/forge-plugin-demo/src'))).isSymbolicLink())
  assert.deepEqual(await snapshot(source), before)
  await write(source, 'ui/local.vue', 'customization')
  await write(host.root, `${host.server}/plugins/forge-plugin-demo/target/classes/built`, 'build artifact')
  await write(host.root, `${host.server}/plugins/forge-plugin-demo/.flattened-pom.xml`, '<project/>')
  const updated = await snapshot(source)
  await removePlugin(host.root, 'demo')
  assert.deepEqual(await snapshot(source), updated)
})

test('dev in generated project or ZIP source is rejected before writes', async t => {
  const source = await plugin(t)
  const generated = await project(t, true)
  const before = await snapshot(generated.root)
  await assert.rejects(addPlugin(generated.root, source, { dev: true }), /模板/)
  assert.deepEqual(await snapshot(generated.root), before)
  const host = await project(t)
  await write(source, 'package.zip', zip(pluginFiles()))
  await assert.rejects(addPlugin(host.root, path.join(source, 'package.zip'), { dev: true }), /目录来源/)
})

test('no Git or ignored targets refuse modified copy, retain edited bytes', async t => {
  for (const ignored of [false, true]) {
    const host = await project(t)
    if (ignored) {
      git(host.root, ['init', '-q'])
      await write(host.root, '.gitignore', 'forge-server/plugins/\nforge-admin-ui/src/views/plugins/\n.forge-plugin/\n')
      commit(host.root)
    }
    const source = await plugin(t)
    await addPlugin(host.root, source)
    const file = `${host.ui}/src/views/plugins/demo/index.vue`
    await write(host.root, file, 'user edit')
    const before = await snapshot(host.root)
    await assert.rejects(addPlugin(host.root, source, { force: true }), /源码已修改/)
    await assert.rejects(removePlugin(host.root, 'demo'), /源码已修改/)
    assert.deepEqual(await snapshot(host.root), before)
  }
})

test('Git tracked/untracked dirty is refused, committed customization is backed up on force', async t => {
  const host = await project(t)
  git(host.root, ['init', '-q'])
  await write(host.root, '.gitignore', '.forge-plugin/\n')
  const source = await plugin(t)
  await addPlugin(host.root, source)
  commit(host.root)
  const file = `${host.ui}/src/views/plugins/demo/index.vue`
  await write(host.root, file, 'committed customization')
  await assert.rejects(addPlugin(host.root, source, { force: true }), /未提交/)
  commit(host.root)
  const result = await addPlugin(host.root, source, { force: true })
  assert.equal(await fs.readFile(path.join(result.backup, 'old-1/index.vue'), 'utf8'), 'committed customization')
  commit(host.root)
  await write(host.root, `${host.ui}/src/views/plugins/demo/new.vue`, 'untracked')
  await assert.rejects(removePlugin(host.root, 'demo'), /未提交/)
})

for (const scenario of ['target', 'parent-link', 'module', 'version', 'marker', 'runtime', 'pom', 'owner']) {
  test(`preflight ${scenario} failure leaves host files unchanged`, async t => {
    const host = await project(t)
    const source = await plugin(t)
    if (scenario === 'target') {
      await write(host.root, `${host.server}/plugins/forge-plugin-demo/local`, 'mine')
    }
    if (scenario === 'parent-link') {
      await fs.symlink(await temporary(t), path.join(host.root, host.server, 'plugins'))
    }
    if (scenario === 'module') {
      await addPlugin(host.root, await plugin(t, descriptor({ id: 'other' })))
    }
    if (scenario === 'version') {
      await write(source, 'forge-plugin.json', JSON.stringify(descriptor({ requiresCore: '>=9.0.0' })))
    }
    if (scenario === 'marker') {
      await write(host.root, `${host.server}/pom.xml`, host.rootPom.replace('forge-plugins:modules:end', 'broken'))
    }
    if (scenario === 'runtime') {
      await write(source, 'server/forge-plugin-demo/src/main/resources/META-INF/forge-plugin.json', '{}')
    }
    if (scenario === 'pom') {
      await write(source, 'server/forge-plugin-demo/pom.xml', '<project><artifactId>wrong</artifactId></project>')
    }
    if (scenario === 'owner') {
      await addPlugin(host.root, source)
      await fs.unlink(path.join(host.root, host.ui, 'src/views/plugins/demo/.forge-plugin-owned.json'))
    }
    const before = await snapshot(host.root)
    await assert.rejects(addPlugin(host.root, source, { force: true }))
    assert.deepEqual(await snapshot(host.root), before)
    assert.ok(!(await fs.readdir(host.root)).includes('.forge-plugin') || scenario === 'module' || scenario === 'owner')
  })
}

test('configuration/core mismatch and malformed plugin record are rejected', async t => {
  const host = await project(t, true)
  const source = await plugin(t)
  for (const replacement of [{ forgeVersion: '1.1.2' }, { artifactPrefix: '../oops' }, { projectType: 'unknown' },
    { plugins: [{ id: '../outside' }] }]) {
    await write(host.root, 'forge.config.json', JSON.stringify({ ...host.options, ...replacement }))
    const before = await snapshot(host.root)
    await assert.rejects(addPlugin(host.root, source))
    assert.deepEqual(await snapshot(host.root), before)
  }
})

test('empty/null/array existing config cannot be misclassified as unconfigured template', async t => {
  const host = await project(t)
  const source = await plugin(t)
  for (const content of ['', 'null', '[]']) {
    await write(host.root, 'forge.config.json', content)
    const before = await snapshot(host.root)
    await assert.rejects(addPlugin(host.root, source))
    assert.deepEqual(await snapshot(host.root), before)
  }
})

test('generated UI-only preserves undefined text and unknown-extension binary bytes', async t => {
  const host = await project(t, true)
  const source = await plugin(t, descriptor({ server: null }))
  await write(source, 'ui/api.js', 'export const unset = undefined\n')
  const binary = Buffer.from([0, 0xff, 0xfe, 1, 2])
  await write(source, 'ui/data.bin', binary)
  await addPlugin(host.root, source)
  const target = path.join(host.root, host.ui, 'src/views/plugins/demo')
  assert.equal(await fs.readFile(path.join(target, 'api.js'), 'utf8'), 'export const unset = undefined\n')
  assert.deepEqual(await fs.readFile(path.join(target, 'data.bin')), binary)
})

test('dev wrapper refuses local POM edits but permits external source edits', async t => {
  const host = await project(t)
  const source = await plugin(t)
  await addPlugin(host.root, source, { dev: true })
  await write(source, 'server/forge-plugin-demo/src/main/java/External.java', '// external edit')
  const target = `${host.server}/plugins/forge-plugin-demo/pom.xml`
  await write(host.root, target, '<project/>')
  const before = await snapshot(host.root)
  await assert.rejects(removePlugin(host.root, 'demo'), /接入 POM/)
  assert.deepEqual(await snapshot(host.root), before)
})

for (const operation of ['fresh', 'force', 'remove', 'dev']) {
  for (const failAt of [0, 2, 4]) {
    test(`${operation} injected failure after write ${failAt} restores all files`, async t => {
      const host = await project(t)
      const source = await plugin(t)
      if (['force', 'remove'].includes(operation)) {
        await addPlugin(host.root, source)
      }
      const before = await snapshot(host.root)
      const hooks = { afterWrite: ({ index }) => {
        if (index === failAt) {
          throw new Error('injected failure')
        }
      } }
      const action = operation === 'remove' ? removePlugin(host.root, 'demo', hooks)
        : addPlugin(host.root, source, { force: operation === 'force', dev: operation === 'dev' }, hooks)
      await assert.rejects(action, /已恢复原源码/)
      assert.deepEqual(await snapshot(host.root), before)
      await assert.rejects(fs.lstat(path.join(host.root, '.forge-plugin/lock')), { code: 'ENOENT' })
    })
  }
}

test('existing lock is not removed and host mutations after preflight are preserved', async t => {
  const host = await project(t)
  const source = await plugin(t)
  await write(host.root, '.forge-plugin/lock', 'other process')
  await assert.rejects(addPlugin(host.root, source), /安装锁/)
  assert.equal(await fs.readFile(path.join(host.root, '.forge-plugin/lock'), 'utf8'), 'other process')
  await fs.unlink(path.join(host.root, '.forge-plugin/lock'))
  await assert.rejects(addPlugin(host.root, source, {}, { beforeWrite: async ({ relative }) => {
    if (relative === `${host.server}/pom.xml`) {
      await write(host.root, relative, `${host.rootPom}<!-- user edit -->`)
    }
  } }), /写入前发生变化/)
  const updatedPom = await fs.readFile(path.join(host.root, `${host.server}/pom.xml`), 'utf8')
  assert.equal(updatedPom, `${host.rootPom}<!-- user edit -->`)
  await assert.rejects(fs.lstat(path.join(host.root, host.server, 'plugins/forge-plugin-demo')), { code: 'ENOENT' })
})

test('replaced lock is retained with warning; completed install is not rolled back', async t => {
  const host = await project(t)
  const source = await plugin(t)
  const result = await addPlugin(host.root, source, {}, { afterWrite: async ({ index }) => {
    if (index === 4) {
      const lock = path.join(host.root, '.forge-plugin/lock')
      await fs.rename(lock, `${lock}-original`)
      await fs.writeFile(lock, 'other process')
    }
  } })
  assert.equal(result.record.id, 'demo')
  assert.match(result.warnings[0], /未删除他人锁/)
  assert.equal(await fs.readFile(path.join(host.root, '.forge-plugin/lock'), 'utf8'), 'other process')
  assert.equal((await listPlugins(host.root)).length, 1)
})
