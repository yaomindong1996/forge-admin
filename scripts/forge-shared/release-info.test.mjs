import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
import { test } from 'node:test'
import {
  createReleaseInfo, frontendVersionChanges, pluginReleaseInfo, readReleaseProject, releaseNotes,
} from './release-info.mjs'
import { buildTime, localBuildInfo, versionDiagnostics, versionResponse, versionWarning } from './version-view.mjs'

async function fixture(t, renamed = false) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-version-test-'))
  t.after(() => fs.rm(root, { recursive: true, force: true }))
  const project = renamed ? 'demo' : 'forge'
  const artifact = renamed ? 'demo-core' : 'forge'
  if (renamed) {
    await fs.writeFile(path.join(root, 'forge.config.json'), JSON.stringify({
      projectName: project, artifactPrefix: artifact,
    }))
  }
  await fs.mkdir(path.join(root, `${artifact}-server`))
  await fs.writeFile(path.join(root, `${artifact}-server/pom.xml`),
    '<project><properties><revision>1.2.0</revision></properties></project>')
  await fs.mkdir(path.join(root, `${project}-admin-ui`))
  await fs.writeFile(path.join(root, `${project}-admin-ui/package.json`),
    JSON.stringify({ name: 'test-ui', version: '0.0.0' }))
  return root
}

test('从根 revision 读取版本，检测漂移；改名工程和缺省可选端均支持', async (t) => {
  for (const renamed of [false, true]) {
    const root = await fixture(t, renamed)
    assert.equal((await readReleaseProject(root)).version, '1.2.0')
    const changes = await frontendVersionChanges(root)
    assert.equal(changes.length, 1)
    assert.equal(changes[0].expected, '1.2.0')
    await assert.rejects(() => pluginReleaseInfo(root, 'admin-ui').config(), /version:sync/)
    await fs.writeFile(changes[0].file, JSON.stringify({ ...changes[0].manifest, version: '1.2.0' }))
    assert.deepEqual(await frontendVersionChanges(root), [])
    const plugin = pluginReleaseInfo(root, 'admin-ui')
    const configured = await plugin.config()
    assert.equal(JSON.parse(configured.define.__FORGE_BUILD_INFO__).version, '1.2.0')
    let emitted
    plugin.generateBundle.call({ emitFile: file => { emitted = file } })
    assert.equal(emitted.fileName, 'version.json')
    assert.deepEqual(JSON.parse(emitted.source), JSON.parse(configured.define.__FORGE_BUILD_INFO__))
  }
})

test('CLI 校验失败后可显式同步，保留其它 package 属性', async (t) => {
  const root = await fixture(t)
  const shared = path.join(root, 'scripts/forge-shared')
  await fs.mkdir(shared, { recursive: true })
  for (const file of ['version-cli.mjs', 'version.mjs', 'release-info.mjs']) {
    await fs.copyFile(new URL(file, import.meta.url), path.join(shared, file))
  }
  const cli = path.join(shared, 'version-cli.mjs')
  assert.throws(() => execFileSync(process.execPath, [cli, '--check'], { stdio: 'pipe' }), /不一致/)
  execFileSync(process.execPath, [cli, '--sync'])
  execFileSync(process.execPath, [cli, '--check'])
  const manifest = JSON.parse(await fs.readFile(path.join(root, 'forge-admin-ui/package.json')))
  assert.deepEqual(manifest, { name: 'test-ui', version: '1.2.0' })
})

test('非法目录前缀与非法 revision 明确报错', async (t) => {
  const root = await fixture(t)
  await fs.writeFile(path.join(root, 'forge.config.json'), '{"projectName":"../escape"}')
  await assert.rejects(() => readReleaseProject(root), /非法/)
  await fs.writeFile(path.join(root, 'forge.config.json'), 'null')
  await assert.rejects(() => readReleaseProject(root), /格式非法/)
  await fs.writeFile(path.join(root, 'forge.config.json'), '{}')
  await fs.writeFile(path.join(root, 'forge-server/pom.xml'), '<properties><revision>unknown</revision></properties>')
  await assert.rejects(() => readReleaseProject(root), /合法/)
})

test('无 Git 或缺少对应日志时不伪造数据，只收录精确版本说明', async (t) => {
  const root = await fixture(t)
  const info = await createReleaseInfo(root, 'h5-ui')
  assert.equal(info.commit, null)
  assert.equal(info.notes, null)
  assert.equal(info.version, '1.2.0')
  assert.ok(!Number.isNaN(Date.parse(info.builtAt)))
  const notes = '## [Unreleased]\n待发布\n## [1.2.0] - 基线\n<script>x</script>\n## [1.1.0]\n旧版'
  assert.equal(releaseNotes(notes, '1.2.0'), '## [1.2.0] - 基线\n<script>x</script>')
  assert.equal(releaseNotes(notes, '1.3.0'), null)
})

test('展示层未知信息不冒充后端；复制字段白名单不包含额外配置', () => {
  assert.deepEqual(localBuildInfo(), {})
  const local = { version: '1.2.0', client: 'h5-ui' }
  const backend = versionResponse({ code: 200, data: {
    version: null, coreVersion: '1.2.0', secret: 'private-secret', build: { host: 'private-host' },
  } })
  const text = versionDiagnostics(local, backend)
  assert.match(text, /发行版本：未生成构建信息/)
  assert.match(text, /框架核心：1.2.0/)
  assert.doesNotMatch(text, /private/)
  assert.match(versionDiagnostics(local, null), /后端运行信息\n发行版本：未获取/)
  assert.equal(buildTime('invalid'), '未提供')
  assert.doesNotMatch(buildTime('2026-10-10T10:00:00Z'), /T/)
  assert.throws(() => versionResponse({ code: 500 }), /响应无效/)
})

test('只比较已知版本；同版本不同提交提示核对；兼容提交短标识', () => {
  const local = { version: '1.2.0', commit: 'abc123456789' }
  assert.equal(versionWarning(local, null), '')
  assert.equal(versionWarning(local, { coreVersion: '1.3.0' }), '')
  assert.match(versionWarning(local, { version: '1.1.0' }), /不一致/)
  assert.match(versionWarning(local, { version: '1.2.0', build: { commit: 'def12345' } }), /提交不同/)
  assert.equal(versionWarning(local, { version: '1.2.0', build: { commit: 'ABC1234' } }), '')
})
