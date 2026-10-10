import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { runProcess } from './process.mjs'
import { verifyDocker, runContainer, containerArguments } from './docker.mjs'
import { collectArtifacts } from './artifacts.mjs'
import { validateFileNames, readRegular } from './files.mjs'
import { fixture, socketFixture, fakeDocker } from './fixtures/helpers.mjs'
import { temporary, write, zip, pluginFiles } from '../forge-plugin/fixtures/helpers.mjs'
import { executeBuild } from './builder.mjs'
import { sha256 } from './files.mjs'

test('子进程有超时/中断/输出限额，不继承敏感环境，也不把错误输出带入结果', async () => {
  const original = process.env.FORGE_BUILDER_TEST_SECRET
  process.env.FORGE_BUILDER_TEST_SECRET = 'synthetic test value'
  try {
    const result = await runProcess(process.execPath,
      ['-e', 'process.stdout.write(String(process.env.FORGE_BUILDER_TEST_SECRET))'])
    assert.equal(result.toString(), 'undefined')
  }
  finally {
    if (original === undefined) {
      delete process.env.FORGE_BUILDER_TEST_SECRET
    }
    else {
      process.env.FORGE_BUILDER_TEST_SECRET = original
    }
  }
  await assert.rejects(runProcess(process.execPath, ['-e', 'setTimeout(()=>{},10000)'], { timeoutMs: 50 }),
    /COMMAND_TIMEOUT/)
  await assert.rejects(runProcess(process.execPath, ['-e', 'process.stdout.write("x".repeat(10000))'],
    { outputLimit: 100 }), /OUTPUT_LIMIT/)
  await assert.rejects(runProcess(process.execPath, ['-e', 'process.stderr.write("private");process.exit(1)']),
    error => error.message === 'COMMAND_FAILED' && !error.message.includes('private'))
  const controller = new AbortController()
  const pending = runProcess(process.execPath, ['-e', 'setTimeout(()=>{},10000)'], { signal: controller.signal })
  controller.abort()
  await assert.rejects(pending, /INTERRUPTED/)
  await assert.rejects(runProcess('/no-such-forge-builder-binary', []), /EXECUTABLE_UNAVAILABLE/)
})

test('rootful/镜像摘要不符拒绝；Docker 不拿 Web/数据库认证环境', async (t) => {
  const item = await fixture(t)
  await socketFixture(t, item.config.dockerSocket)
  for (const behavior of [{ security: ['name=seccomp'] }, { digests: [] },
    { resources: { CgroupVersion: '1' } }, { resources: { CgroupDriver: 'none' } },
    { resources: { CpuCfsQuota: false } }, { resources: { MemoryLimit: false } }]) {
    const docker = fakeDocker(item.config, behavior)
    await assert.rejects(verifyDocker(item.config, docker.execute))
    assert.ok(!docker.calls.some(call => call.args[2] === 'run'))
  }
  const docker = fakeDocker(item.config)
  await verifyDocker(item.config, docker.execute)
  assert.deepEqual(Object.keys(docker.calls[0].options.env).sort(), ['DOCKER_CONFIG', 'LANG', 'PATH'])
  assert.equal(docker.calls[0].args[1], `unix://${item.config.dockerSocket}`)
})

test('容器只挂本次只读输入和唯一输出，不挂 socket/源码原件，固定资源和网络边界', () => {
  const config = { image: `forge-builder@sha256:${'a'.repeat(64)}` }
  const job = { source: '/isolated/job/source', package: '/isolated/job/package.zip',
    control: '/isolated/job/control', output: '/isolated/job/artifacts', force: true }
  const args = containerArguments(config, job, { name: 'forge-build-owned', nonce: 'owned' })
  for (const value of ['--network=none', '--pull=never', '--read-only', '--cap-drop=ALL',
    '--security-opt=no-new-privileges', '--memory=4g', '--cpus=2', '--pids-limit=256', '--force']) {
    assert.ok(args.includes(value))
  }
  const mounts = args.filter(arg => arg.startsWith('type=bind'))
  assert.equal(mounts.length, 4)
  assert.equal(mounts.filter(arg => arg.endsWith(',readonly')).length, 3)
  assert.ok(!args.some(arg => /docker\.sock|privileged|host-network|sh -c/.test(arg)))
  assert.equal(args.filter(arg => arg === '--env').length, 3)
})

test('失败清理自己容器；所有权冲突不删除其它容器', async (t) => {
  const item = await fixture(t)
  const job = { source: '/source', package: '/package', control: '/control', output: '/output' }
  const failure = fakeDocker(item.config, { build: async () => { throw new Error('fixture failed') } })
  await assert.rejects(runContainer(item.config, job, { execute: failure.execute }), /fixture failed/)
  assert.ok(failure.calls.some(call => call.args[2] === 'rm'))
  const conflict = fakeDocker(item.config, { wrongOwner: true })
  await assert.rejects(runContainer(item.config, job, { execute: conflict.execute }), /CLEANUP_UNVERIFIED/)
  assert.ok(!conflict.calls.some(call => call.args[2] === 'rm'))
  const absent = fakeDocker(item.config, { absent: true })
  await runContainer(item.config, job, { execute: absent.execute })
  assert.ok(!absent.calls.some(call => call.args[2] === 'rm'))
  const rejected = fakeDocker(item.config)
  const failingCleanup = async (executable, args, options) => {
    if (args[2] === 'rm') {
      throw new Error('cleanup fixture')
    }
    return rejected.execute(executable, args, options)
  }
  await assert.rejects(runContainer(item.config, job, { execute: failingCleanup }), /CLEANUP_UNVERIFIED/)
})

for (const kind of ['empty', 'missing-index', 'link', 'hardlink', 'unexpected', 'zero', 'oversize']) {
  test(`实际产物负例 ${kind} 不信任包的成功回执`, async (t) => {
    const root = await temporary(t)
    if (kind !== 'empty') {
      await write(root, 'frontend/index.html', 'fixture')
    }
    if (kind === 'missing-index') {
      await fs.rename(path.join(root, 'frontend/index.html'), path.join(root, 'frontend/other.html'))
    }
    if (kind === 'link') {
      await fs.symlink('/etc/passwd', path.join(root, 'frontend/linked'))
    }
    if (kind === 'hardlink') {
      await fs.link(path.join(root, 'frontend/index.html'), path.join(root, 'frontend/linked'))
    }
    if (kind === 'unexpected') {
      await write(root, 'success.json', '{"success":true}')
    }
    if (kind === 'zero') {
      await write(root, 'frontend/index.html', '')
    }
    if (kind === 'oversize') {
      await fs.truncate(path.join(root, 'frontend/index.html'), 512 * 1024 * 1024 + 1)
    }
    await assert.rejects(collectArtifacts(root, { ui: 'test-ui' }))
  })
}

test('源码/产物路径及普通文件大小有界', async (t) => {
  for (const names of [['a', 'A'], ['é', 'e\u0301'], ['a', 'a/b'], ['../escape'],
    ['a', 'b', 'c']]) {
    assert.throws(() => validateFileNames(names, 2))
  }
  assert.throws(() => validateFileNames(['a/'.repeat(33) + 'b'], 10))
  const root = await temporary(t)
  await write(root, 'file', '12345')
  await assert.rejects(readRegular(path.join(root, 'file'), 4), /FILE_UNSAFE/)
  await fs.symlink(path.join(root, 'file'), path.join(root, 'link'))
  await assert.rejects(readRegular(path.join(root, 'link'), 10))
})

test('离线源包本地配置拒绝；Abort 后预检不能继续成功', async (t) => {
  const item = await fixture(t)
  const files = pluginFiles()
  files.set('ui/.env.local', Buffer.from('fixture'))
  const archive = zip(files)
  await write(item.delivery, 'plugin.zip', archive)
  item.config.packageSha256 = sha256(archive)
  assert.equal((await executeBuild(item.config)).failureCode, 'PACKAGE_LOCAL_CONFIG_OR_OUTPUT')
  const controller = new AbortController()
  controller.abort()
  assert.equal((await executeBuild(item.config, { signal: controller.signal })).failureCode, 'INTERRUPTED')
})

test('后端输出不能是普通文本冒充 JAR', async (t) => {
  const root = await temporary(t)
  await write(root, 'backend/admin.jar', 'not a zip archive')
  await assert.rejects(collectArtifacts(root, { server: 'plugin' }), /BACKEND_JAR_SIGNATURE_INVALID/)
})
