import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { readUiPluginManifest } from '../../../forge-admin-ui/build/plugin-ui-manifest.js'
import { addPlugin } from '../../../scripts/forge-plugin/installer.mjs'
import { descriptor, plugin, project, repository } from '../../../scripts/forge-plugin/fixtures/helpers.mjs'

for (const generated of [false, true]) {
  test(`UI manifest reflects actual owned components, generated=${generated}`, async (t) => {
    const host = await project(t, generated)
    const source = await plugin(t)
    await addPlugin(host.root, source)
    const snapshot = await readUiPluginManifest(host.root)
    assert.equal(snapshot.coreVersion, '1.2.0')
    assert.deepEqual(snapshot.plugins, [{ id: 'demo', name: '测试插件', version: '1.0.0', edition: 'community' }])
    assert.ok(!JSON.stringify(snapshot).includes(source))
    await fs.writeFile(path.join(host.root, host.ui, 'src/views/plugins/demo/.forge-plugin-owned.json'), '{}')
    await assert.rejects(() => readUiPluginManifest(host.root), /所有权/)
  })
}

test('dev UI manifest validates actual link and source metadata without requiring copy marker', async (t) => {
  const host = await project(t)
  const source = await plugin(t)
  await addPlugin(host.root, source, { dev: true })
  assert.equal((await readUiPluginManifest(host.root)).plugins[0].id, 'demo')
  await fs.writeFile(path.join(source, 'forge-plugin.json'), JSON.stringify(descriptor({ version: '1.1.0' })))
  await assert.rejects(() => readUiPluginManifest(host.root), /不一致|已变化/)
})

test('workbench persists bounded private packages, additive RBAC/dictionaries and no process execution', async () => {
  const migration = await fs.readFile(path.join(repository,
    'forge-server/db/migration/V1.0.211__add_plugin_install_workbench.sql'), 'utf8')
  assert.match(migration, /CREATE TABLE IF NOT EXISTS sys_plugin_task/)
  assert.match(migration, /uk_sys_plugin_task_active/)
  assert.match(migration, /archive_bytes BETWEEN 1 AND 8388608/)
  assert.match(migration, /sys_plugin_task_status/)
  assert.match(migration, /'system:plugin:snapshot', 'GET', '\/system\/plugin\/snapshot'/)
  assert.doesNotMatch(migration, /sys_role_resource|DELETE FROM|\$\{/)
  const directory = path.join(repository,
    'forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system')
  for (const file of await fs.readdir(path.join(directory, 'service/plugin'))) {
    const text = await fs.readFile(path.join(directory, 'service/plugin', file), 'utf8')
    assert.doesNotMatch(text, /ProcessBuilder|Runtime\.getRuntime|Files\.|@RequestBody Map/)
  }
  const xml = await fs.readFile(path.join(repository,
    'forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysPluginTaskMapper.xml'),
  'utf8')
  assert.ok(!xml.includes('${'))
  assert.doesNotMatch(xml, /DELETE FROM/)
  assert.match(xml, /task_status = #\{expectedStatus\} AND revision = #\{expectedRevision\}/)
})

test('review is an additive platform permission with immutable audit, not a deployment path', async () => {
  const migration = await fs.readFile(path.join(repository,
    'forge-server/db/migration/V1.0.213__add_plugin_task_review.sql'), 'utf8')
  assert.match(migration, /CREATE TABLE IF NOT EXISTS sys_plugin_task_review/)
  assert.match(migration, /'system:plugin:review'/)
  assert.match(migration, /'POST' AS method/)
  assert.match(migration, /'\/system\/plugin-task\/\*\/review' AS url/)
  assert.match(migration, /sys_plugin_review_decision/)
  assert.match(migration, /'release_ready'/)
  assert.match(migration, /'closed'/)
  assert.doesNotMatch(migration, /sys_role_resource|DELETE FROM|\$\{/)
  const ui = await fs.readFile(path.join(repository, 'forge-admin-ui/src/stores/plugin/reviewStore.js'), 'utf8')
  assert.match(ui, /Object\.freeze/)
  assert.doesNotMatch(ui, /localStorage|sessionStorage/)
  const xml = await fs.readFile(path.join(repository,
    'forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/'
      + 'SysPluginTaskReviewMapper.xml'), 'utf8')
  assert.doesNotMatch(xml, /DELETE FROM|\$\{/)
  assert.match(xml, /tenant_id = #\{tenantId\}/)
  assert.match(xml, /del_flag = 0/)
})
