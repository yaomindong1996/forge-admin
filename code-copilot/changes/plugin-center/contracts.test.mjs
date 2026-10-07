import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import test from 'node:test'

const root = resolve(import.meta.dirname, '../../..')
const read = path => readFile(resolve(root, path), 'utf8')

test('全部 JAR 业务插件拥有稳定构建声明，聚合 POM 不伪装成插件', async () => {
  const catalog = JSON.parse(await read('scripts/forge-create/module-catalog.json'))
  let count = 0
  for (const [id, module] of Object.entries(catalog.modules)) {
    if (!['plugin', 'plugin-child'].includes(module.type) || id === 'plugin-capability-parent')
      continue
    const descriptor = JSON.parse(await read(`${module.path}/src/main/resources/META-INF/forge-module.json`))
    assert.equal(descriptor.id, id)
    assert.equal(descriptor.version, '${project.version}')
    assert.equal(descriptor.module, '${project.artifactId}')
    assert.ok(descriptor.name.length)
    count++
  }
  assert.equal(count, 15)
  const pom = await read('forge-server/forge-framework/forge-plugin-parent/pom.xml')
  assert.match(pom, /resources combine.self="override"/)
  assert.match(pom, /<exclude>META-INF\/forge-module.json<\/exclude>/)
  assert.match(pom, /<include>META-INF\/forge-module.json<\/include>/)
})

test('资源/字典只追加，四类字典与页面协议一致，未放开普通角色权限', async () => {
  const sql = await read('forge-server/db/migration/V1.0.210__add_plugin_center.sql')
  assert.doesNotMatch(sql, /\$\{[^}]+\}|DELETE\s+FROM|INSERT\s+INTO\s+sys_role_resource|UPDATE\s+sys_resource/i)
  assert.equal((sql.match(/NOT EXISTS/g) || []).length, 4)
  for (const code of ['origin', 'edition', 'load_state', 'feature_state'])
    assert.ok(sql.includes(`sys_plugin_${code}`))
  for (const code of ['builtin', 'external', 'backend_loaded', 'metadata_only', 'community'])
    assert.ok(sql.includes(`'${code}'`))
  assert.match(sql, /'system\/plugin'/)
  assert.match(sql, /'pc', 0, 0/)
  assert.match(sql, /resource_name = '平台管理'/)
})

test('页面没有未实现的安装操作，详情不是可独立访问的假业务路由', async () => {
  const page = await read('forge-admin-ui/src/views/system/plugin.vue')
  assert.match(page, /plugin\/components\/PluginDetail.vue/)
  assert.doesNotMatch(page, /v-html|Runtime\.exec|ProcessBuilder/)
  const api = await read('forge-admin-ui/src/api/system/plugin.js')
  assert.match(api, /encodeURIComponent\(id\)/)
  assert.match(api, /request.get\('\/system\/plugin\/page'/)
  assert.doesNotMatch(api, /request\.(post|put|delete)/)
})
