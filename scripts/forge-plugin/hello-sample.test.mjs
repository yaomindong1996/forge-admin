import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { readBundle, fingerprint } from './bundle.mjs'
import { readDescriptor } from './descriptor.mjs'
import { addPlugin, listPlugins, removePlugin } from './installer.mjs'
import { project, repository, temporary, write, zip } from './fixtures/helpers.mjs'

const sample = path.join(repository, 'plugins-samples/forge-plugin-hello')
const modulePath = 'server/forge-plugin-hello'

test('deliverable hello descriptor and runtime descriptor are identical and compatible', async () => {
  const { files } = await readBundle(sample)
  const descriptor = readDescriptor(files, '1.2.0')
  assert.equal(descriptor.id, 'hello')
  assert.equal(descriptor.edition, 'community')
  assert.equal(descriptor.version, '1.0.0')
  assert.deepEqual(descriptor.features, ['community.hello'])
  assert.deepEqual(files.get('forge-plugin.json'),
    files.get(`${modulePath}/src/main/resources/META-INF/forge-plugin.json`))
  const pom = files.get(`${modulePath}/pom.xml`).toString()
  assert.match(pom, /<version>\$\{revision\}<\/version>/)
  assert.doesNotMatch(pom, /<revision>/)
})

test('sample SQL only seeds non-public resources and is not in host migrations', async () => {
  const { files } = await readBundle(sample)
  const sql = files.get(`${modulePath}/src/main/resources/db/plugin/hello/V1.0.0__add_hello_resources.sql`).toString()
  assert.equal((sql.match(/INSERT INTO sys_resource/g) || []).length, 2)
  assert.equal((sql.match(/NOT EXISTS/g) || []).length, 2)
  assert.doesNotMatch(sql, /\$\{|INSERT INTO sys_role_resource|UPDATE\s|DELETE\s|DROP\s/i)
  assert.match(sql, /'plugins\/hello\/index'/)
  assert.match(sql, /'plugin:hello:info'/)
  const hostPom = await fs.readFile(path.join(repository, 'forge-server/pom.xml'), 'utf8')
  assert.doesNotMatch(hostPom, /<module>plugins\/forge-plugin-hello<\/module>/)
  await assert.rejects(fs.access(path.join(repository, 'forge-admin-ui/src/views/plugins/hello')), { code: 'ENOENT' })
})

for (const generated of [false, true]) {
  for (const zipped of [false, true]) {
    const name = `actual hello ${zipped ? 'ZIP' : 'directory'} install/remove in ${generated ? 'renamed' : 'template'}`
    test(name, async t => {
      const host = await project(t, generated)
      const bundle = await readBundle(sample)
      const original = fingerprint(bundle.files)
      let source = sample
      if (zipped) {
        source = path.join(await temporary(t), 'hello.zip')
        await write(path.dirname(source), 'hello.zip', zip(bundle.files, { method: 8 }))
      }
      await addPlugin(host.root, source)
      const target = path.join(host.root, host.server, 'plugins', `${generated ? 'core' : 'forge'}-plugin-hello`)
      const packagePath = generated ? 'com/acme/app' : 'com/mdframe/forge'
      const controller = await fs.readFile(path.join(target,
        `src/main/java/${packagePath}/plugin/hello/controller/HelloPluginController.java`), 'utf8')
      assert.ok(controller.includes(`package ${packagePath.replaceAll('/', '.')}.plugin.hello.controller;`))
      assert.match(controller, /@SaCheckPermission\("plugin:hello:info"\)/)
      assert.match(controller, /@RequiresFeature\("community.hello"\)/)
      const pom = await fs.readFile(path.join(target, 'pom.xml'), 'utf8')
      assert.ok(pom.includes(`<artifactId>${generated ? 'core' : 'forge'}-starter-plugin</artifactId>`))
      assert.ok(pom.includes(`<groupId>${generated ? host.options.groupId : 'com.mdframe.forge'}</groupId>`))
      assert.match(pom, /<relativePath>\.\.\/\.\.\/pom.xml<\/relativePath>/)
      assert.deepEqual(await fs.readFile(path.join(target, 'src/main/resources/META-INF/forge-plugin.json')),
        bundle.files.get('forge-plugin.json'))
      const ui = path.join(host.root, host.ui, 'src/views/plugins/hello')
      assert.deepEqual(await fs.readFile(path.join(ui, 'index.vue')), bundle.files.get('ui/index.vue'))
      assert.deepEqual(await fs.readFile(path.join(ui, 'api/info.js')), bundle.files.get('ui/api/info.js'))
      assert.equal((await listPlugins(host.root))[0].id, 'hello')
      const removed = await removePlugin(host.root, 'hello')
      assert.ok(removed.backup)
      assert.deepEqual(await listPlugins(host.root), [])
      assert.equal(fingerprint((await readBundle(sample)).files), original)
    })
  }
}

test('actual hello dev adapter links source and removing it preserves external package', async t => {
  const host = await project(t)
  const original = fingerprint((await readBundle(sample)).files)
  await addPlugin(host.root, sample, { dev: true })
  const target = path.join(host.root, host.server, 'plugins/forge-plugin-hello')
  assert.ok((await fs.lstat(path.join(target, 'pom.xml'))).isFile())
  assert.ok((await fs.lstat(path.join(target, 'src'))).isSymbolicLink())
  await removePlugin(host.root, 'hello')
  assert.equal(fingerprint((await readBundle(sample)).files), original)
})
