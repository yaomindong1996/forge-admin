import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { executeBuild } from './builder.mjs'
import { readConfig } from './config.mjs'
import { runBuilderCli } from './index.mjs'
import { fixture, socketFixture, fakeDocker } from './fixtures/helpers.mjs'
import { snapshot, write, git, commit, descriptor, plugin, temporary } from '../forge-plugin/fixtures/helpers.mjs'
import { addPlugin } from '../forge-plugin/installer.mjs'
import { loadProject } from '../forge-plugin/project.mjs'
import { buildCommands } from './container-entry.mjs'
import { sha256 } from './files.mjs'

test('真实 Git/ZIP 的 check 保存固定摘要，不改原工程、不调用 Docker', async (t) => {
  const item = await fixture(t)
  const before = await snapshot(item.host.root)
  const config = await readConfig(item.file)
  const result = await executeBuild(config)
  assert.equal(result.status, 'checked')
  assert.equal(result.deployed, false)
  assert.equal(result.preflight.targets.server, 'acme-server/plugins/core-plugin-demo')
  assert.equal(result.source.commit, config.commit)
  assert.match(result.source.sha256, /^[a-f0-9]{64}$/)
  assert.ok(!JSON.stringify(result).includes(item.host.root))
  assert.deepEqual(await snapshot(item.host.root), before)
  const persisted = JSON.parse(await fs.readFile(path.join(config.workspaceRoot, result.jobId, 'result.json')))
  assert.deepEqual(persisted, result)
  assert.equal(await fs.access(path.join(config.workspaceRoot, result.jobId, 'source/.git')).then(() => true,
    () => false), false)
})

test('run 未审查拒绝，Docker 不可用绝不在宿主安装/构建', async (t) => {
  const item = await fixture(t)
  await assert.rejects(executeBuild(item.config, { run: true }), /REVIEW_REQUIRED/)
  const before = await snapshot(item.host.root)
  const result = await executeBuild(item.config, { run: true, reviewed: true })
  assert.equal(result.status, 'failed')
  assert.deepEqual(await snapshot(item.host.root), before)
  assert.equal(await fs.access(path.join(item.config.workspaceRoot, result.jobId, 'source/acme-server/plugins'))
    .then(() => true, () => false), false)
})

test('固定 Admin 视图不带其它前端/历史 ZIP，不提高快照限额', async (t) => {
  const item = await fixture(t)
  await write(item.host.root, 'acme-report-ui/src/font.ttf', 'other frontend fixture')
  await write(item.host.root, `${item.host.ui}/dist.zip`, 'legacy output fixture')
  commit(item.host.root)
  item.config.commit = git(item.host.root, ['rev-parse', 'HEAD']).trim()
  const result = await executeBuild(item.config)
  assert.equal(result.status, 'checked')
  assert.equal(result.source.scope, 'admin-build')
  assert.equal(result.source.excludedFileCount, 2)
  const root = path.join(item.config.workspaceRoot, result.jobId, 'source')
  await assert.rejects(fs.access(path.join(root, 'acme-report-ui')))
  await assert.rejects(fs.access(path.join(root, item.host.ui, 'dist.zip')))
})

test('桩容器通过后只校验实际产物，不宣称部署；原工程和包不变', async (t) => {
  const item = await fixture(t)
  await socketFixture(t, item.config.dockerSocket)
  const before = await snapshot(item.host.root)
  const docker = fakeDocker(item.config, { build: async (args) => {
    const mount = args.find(arg => arg.endsWith('dst=/output'))
    const output = mount.slice('type=bind,src='.length, mount.indexOf(',dst='))
    await write(output, 'backend/admin.jar', 'PK\x03\x04synthetic jar')
    await write(output, 'frontend/index.html', '<!doctype html><title>fixture</title>')
  } })
  const result = await executeBuild(item.config, { run: true, reviewed: true, execute: docker.execute })
  assert.equal(result.status, 'built')
  assert.equal(result.deployed, false)
  assert.equal(result.artifacts.length, 2)
  assert.equal(result.artifacts[0].sha256, sha256(Buffer.from('PK\x03\x04synthetic jar')))
  assert.deepEqual(await snapshot(item.host.root), before)
  assert.deepEqual(await fs.readFile(item.config.packageFile), item.data)
  assert.ok(docker.calls.some(call => call.args[2] === 'rm'))
})

test('即使已提交客户定制也阻断自动替换；普通无定制升级可预检', async (t) => {
  const item = await fixture(t, descriptor({ version: '1.1.0' }))
  const source = await plugin(t)
  await addPlugin(item.host.root, source)
  commit(item.host.root)
  item.config.commit = git(item.host.root, ['rev-parse', 'HEAD']).trim()
  const noForce = await executeBuild(item.config)
  assert.equal(noForce.status, 'failed')
  assert.equal((await executeBuild(item.config, { force: true })).status, 'checked')
  await write(item.host.root, `${item.host.ui}/src/views/plugins/demo/customer.vue`, '<template>定制</template>')
  commit(item.host.root)
  item.config.commit = git(item.host.root, ['rev-parse', 'HEAD']).trim()
  assert.equal((await executeBuild(item.config, { force: true })).status, 'failed')
})

test('忽略的插件本地配置不进快照，但仍阻断原安装所有权检查', async (t) => {
  const item = await fixture(t, descriptor({ version: '1.1.0' }))
  await addPlugin(item.host.root, await plugin(t))
  await write(item.host.root, '.gitignore', '.env.local\n.forge-plugin/\n')
  commit(item.host.root)
  item.config.commit = git(item.host.root, ['rev-parse', 'HEAD']).trim()
  await write(item.host.root, `${item.host.ui}/src/views/plugins/demo/.env.local`, 'configuration fixture')
  const result = await executeBuild(item.config, { force: true })
  assert.equal(result.status, 'failed')
  assert.equal(result.failurePhase, 'source_preflight')
  assert.equal(await fs.readFile(path.join(item.host.root,
    `${item.host.ui}/src/views/plugins/demo/.env.local`), 'utf8'), 'configuration fixture')
})

test('社区执行器拒绝商业包和降级，不把 CLI 的 force 当版本校验绕过', async (t) => {
  const commercial = await fixture(t, descriptor({ edition: 'ee', features: ['ee.demo'] }))
  assert.equal((await executeBuild(commercial.config)).failureCode, 'COMMUNITY_PACKAGE_REQUIRED')
  const downgrade = await fixture(t)
  await addPlugin(downgrade.host.root, await plugin(t, descriptor({ version: '1.1.0' })))
  commit(downgrade.host.root)
  downgrade.config.commit = git(downgrade.host.root, ['rev-parse', 'HEAD']).trim()
  assert.equal((await executeBuild(downgrade.config, { force: true })).failureCode, 'PLUGIN_DOWNGRADE')
})

for (const kind of ['digest', 'dirty', 'commit', 'untracked', 'symlink', 'local']) {
  test(`源码/包负例 ${kind} 不写原工程`, async (t) => {
    const item = await fixture(t)
    if (kind === 'digest') {
      item.config.packageSha256 = 'b'.repeat(64)
    }
    if (kind === 'dirty') {
      await write(item.host.root, `${item.host.ui}/src/main.js`, '// modified')
    }
    if (kind === 'commit') {
      item.config.commit = 'b'.repeat(40)
    }
    if (kind === 'untracked') {
      await write(item.host.root, 'extra.txt', 'untracked')
    }
    if (kind === 'symlink') {
      await fs.symlink('/etc/passwd', path.join(item.host.root, 'linked'))
      commit(item.host.root)
    }
    if (kind === 'local') {
      await write(item.host.root, 'application-dev.yml', 'configuration fixture')
      commit(item.host.root)
    }
    if (['symlink', 'local'].includes(kind)) {
      item.config.commit = git(item.host.root, ['rev-parse', 'HEAD']).trim()
    }
    const before = await snapshot(item.host.root)
    const result = await executeBuild(item.config)
    assert.equal(result.status, 'failed')
    assert.deepEqual(await snapshot(item.host.root), before)
  })
}

test('配置白名单/不可变镜像/私有工作区/目录分离；不接收命令或 credentials', async (t) => {
  const item = await fixture(t)
  for (const change of [{ command: 'anything' }, { image: 'forge:latest' }, { commit: 'HEAD' },
    { workspaceRoot: item.host.root }, { dockerSocket: 'tcp://remote:2375' }]) {
    await write(item.delivery, 'invalid.json', JSON.stringify({ ...item.config, ...change }))
    await assert.rejects(readConfig(path.join(item.delivery, 'invalid.json')))
  }
  const publicDirectory = await temporary(t)
  await fs.chmod(publicDirectory, 0o755)
  await write(item.delivery, 'invalid.json', JSON.stringify({ ...item.config, workspaceRoot: publicDirectory }))
  await assert.rejects(readConfig(path.join(item.delivery, 'invalid.json')), /WORKSPACE_NOT_PRIVATE/)
})

test('固定构建使用宿主真实映射、离线锁文件；不运行测试、迁移或任意脚本', async (t) => {
  const item = await fixture(t)
  const commands = buildCommands(await loadProject(item.host.root))
  assert.ok(commands.backend[0].args.includes('core-framework/core-dependencies/pom.xml'))
  assert.ok(commands.backend[1].args.includes('core-admin-server'))
  assert.ok(commands.backend.every(command => command.args.includes('-o') && command.args.includes('-DskipTests')))
  assert.ok(commands.frontend[0].args.includes('--offline'))
  assert.ok(commands.frontend[0].args.includes('--ignore-scripts'))
  assert.ok(commands.frontend[0].args.includes('--frozen-lockfile'))
  assert.equal(commands.frontend[1].args[0], 'node_modules/vite/bin/vite.js')
})

test('CLI check 可执行/失败码明确；拒绝未知/重复/run 未确认参数', async (t) => {
  const item = await fixture(t)
  const messages = []
  const output = { log: value => messages.push(value), error: value => messages.push(value) }
  assert.equal(await runBuilderCli(['check', item.file], output), 0)
  assert.equal(JSON.parse(messages[0]).status, 'checked')
  for (const args of [['run', item.file], ['check', item.file, '--reviewed'],
    ['check', item.file, '--force', '--force'], ['arbitrary', item.file]]) {
    assert.equal(await runBuilderCli(args, output), 1)
  }
  assert.ok(messages.every(message => !message.includes(item.host.root)))
})
