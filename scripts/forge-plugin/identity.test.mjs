import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { addPlugin, removePlugin } from './installer.mjs'
import { identityContext } from './identity.mjs'
import { descriptor, plugin, pluginFiles, project, snapshot, temporary, write, zip } from './fixtures/helpers.mjs'

for (const generated of [false, true]) {
  for (const id of ['forge-starter-core', 'forge-plugin-demo', 'forge-admin']) {
    for (const archive of [false, true]) {
      const scenario = `${generated ? 'renamed' : 'template'} / ${archive ? 'ZIP' : 'directory'}`
      test(`stable identity ${id}: ${scenario}`, async t => {
        const host = await project(t, generated)
        const metadata = descriptor({ id, features: ['community.forge-starter-cache', 'com.mdframe.forge'] })
        const files = pluginFiles(metadata)
        const sql = `src/main/resources/db/plugin/${id}/V1.0.0__fixture.sql`
        const code = 'package com.mdframe.forge;\nimport com.mdframe.forge.starter.core.Example;\n'
          + `@RequiresFeature("community.forge-starter-cache")\n@RequestMapping("/plugin/${id}")\n`
          + 'public class Probe { String feature = "com.mdframe.forge"; }\n'
        const ui = `<template><a href="/plugins/${id}">${id}</a></template>\n`
        const binary = Buffer.from([0, 0xff, ...Buffer.from(id)])
        files.set(`server/forge-plugin-demo/${sql}`, Buffer.from(`SELECT '${id}', 'community.forge-starter-cache';\n`))
        files.set('server/forge-plugin-demo/src/main/java/com/mdframe/forge/Probe.java', Buffer.from(code))
        files.set('ui/index.vue', Buffer.from(ui))
        files.set(`ui/assets/${id}/original.bin`, binary)
        const source = await sourceFor(t, files, archive)
        const before = await snapshot(path.dirname(source))
        const result = await addPlugin(host.root, source)
        const module = generated ? 'core-plugin-demo' : 'forge-plugin-demo'
        const target = path.join(host.root, host.server, 'plugins', module)
        assert.equal(result.record.id, id)
        assert.equal(await fs.readFile(path.join(target, sql), 'utf8'),
          `SELECT '${id}', 'community.forge-starter-cache';\n`)
        const pkg = generated ? 'com/acme/app' : 'com/mdframe/forge'
        assert.equal(await fs.readFile(path.join(target, `src/main/java/${pkg}/Probe.java`), 'utf8'),
          generated ? code.replaceAll('com.mdframe.forge;', 'com.acme.app;')
            .replace('import com.mdframe.forge.', 'import com.acme.app.') : code)
        const pom = await fs.readFile(path.join(target, 'pom.xml'), 'utf8')
        assert.ok(pom.includes(`<artifactId>${module}</artifactId>`))
        assert.ok(pom.includes(`<groupId>${generated ? 'com.acme.maven' : 'com.mdframe.forge'}</groupId>`))
        assert.deepEqual(await fs.readFile(path.join(target, 'src/main/resources/META-INF/forge-plugin.json')),
          files.get('server/forge-plugin-demo/src/main/resources/META-INF/forge-plugin.json'))
        const uiTarget = path.join(host.root, host.ui, 'src/views/plugins', id)
        assert.equal(await fs.readFile(path.join(uiTarget, 'index.vue'), 'utf8'), ui)
        assert.deepEqual(await fs.readFile(path.join(uiTarget, `assets/${id}/original.bin`)), binary)
        await removePlugin(host.root, id)
        assert.deepEqual(await snapshot(path.dirname(source)), before)
      })
    }
  }
}

async function sourceFor(t, files, archive) {
  const root = await temporary(t)
  if (archive) {
    await write(root, 'package.zip', zip(files))
    return path.join(root, 'package.zip')
  }
  for (const [relative, data] of files) {
    await write(root, `package/${relative}`, data)
  }
  return path.join(root, 'package')
}

test('UI-only identity remains stable without a server component', async t => {
  const host = await project(t, true)
  const source = await plugin(t, descriptor({ id: 'forge-admin', server: null }))
  await write(source, 'ui/index.vue', '<template><a href="/plugins/forge-admin">forge-admin</a></template>')
  await addPlugin(host.root, source)
  assert.equal(await fs.readFile(path.join(host.root, host.ui, 'src/views/plugins/forge-admin/index.vue'), 'utf8'),
    '<template><a href="/plugins/forge-admin">forge-admin</a></template>')
})

test('identity protection preserves multiline shared rules and complete values, not similarly prefixed text', () => {
  const context = identityContext({ artifactMap: { 'forge-admin': 'core-admin' } },
    descriptor({ id: 'forge-admin', features: ['community.forge-starter-cache'] }))
  const rules = [['cd forge-server\n', 'cd acme-server\n'], ['forge-admin', 'core-admin'],
    ['forge-starter-cache', 'core-starter-cache']]
  const source = 'cd forge-server\nforge-admin|forge-admin-server|community.forge-starter-cache\n'
  assert.equal(context.transformText('instructions.txt', source, rules),
    'cd acme-server\nforge-admin|core-admin-server|community.forge-starter-cache\n')
  assert.equal(context.transformText('pom.xml', '<artifactId>forge-admin</artifactId>', rules),
    '<artifactId>core-admin</artifactId>')
  assert.deepEqual(context.directoryMap, {})
})

test('package declaration followed by feature literal on the same Java line protects only the protocol value', () => {
  const context = identityContext({ artifactMap: {} }, descriptor({ features: ['com.mdframe.forge'] }))
  const source = 'package com.mdframe.forge; @RequiresFeature("com.mdframe.forge") public class Probe {}'
  assert.equal(context.transformText('Probe.java', source, [['com.mdframe.forge', 'com.acme.app']]),
    'package com.acme.app; @RequiresFeature("com.mdframe.forge") public class Probe {}')
})
