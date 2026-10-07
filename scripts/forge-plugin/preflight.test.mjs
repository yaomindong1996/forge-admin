import test from 'node:test'
import assert from 'node:assert/strict'
import { inspectPluginInstall, addPlugin } from './installer.mjs'
import { runPluginCli } from './index.mjs'
import { project, plugin, descriptor, snapshot, write } from './fixtures/helpers.mjs'

test('只读预检与真实安装目标一致，不改源码/POM/config 或创建安装事务', async (t) => {
  const host = await project(t, true)
  const source = await plugin(t)
  const before = await snapshot(host.root)
  const preview = await inspectPluginInstall(host.root, source)
  assert.equal(preview.operation, 'install')
  assert.equal(preview.targets.server, 'acme-server/plugins/core-plugin-demo')
  assert.equal(preview.targets.ui, 'acme-admin-ui/src/views/plugins/demo')
  assert.ok(!JSON.stringify(preview).includes(host.root))
  assert.deepEqual(await snapshot(host.root), before)
  const installed = await addPlugin(host.root, source)
  assert.equal(installed.record.id, preview.pluginId)
  assert.equal(installed.record.version, preview.version)
})

test('已登记替换仍需显式 force，预检同样拒绝定制与目录占用', async (t) => {
  const host = await project(t)
  const source = await plugin(t)
  await addPlugin(host.root, source)
  const next = await plugin(t, descriptor({ version: '1.1.0' }))
  await assert.rejects(inspectPluginInstall(host.root, next), /--force/)
  const preview = await inspectPluginInstall(host.root, next, { force: true })
  assert.equal(preview.previousVersion, '1.0.0')
  await write(host.root, `${preview.targets.ui}/custom.vue`, '<template>定制</template>')
  const before = await snapshot(host.root)
  await assert.rejects(inspectPluginInstall(host.root, next, { force: true }), /源码已修改/)
  assert.deepEqual(await snapshot(host.root), before)
})

test('check CLI 只输出相对预览，拒绝 dev 及无关参数', async (t) => {
  const host = await project(t)
  const source = await plugin(t)
  const messages = []
  const output = { log: message => messages.push(message), error: message => messages.push(message) }
  assert.equal(await runPluginCli(['check', source], output, { root: host.root }), 0)
  assert.equal(JSON.parse(messages[0]).pluginId, 'demo')
  assert.equal(await runPluginCli(['check', source, '--dev'], output, { root: host.root }), 1)
})
