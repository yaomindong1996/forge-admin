import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { randomBytes } from 'node:crypto'
import { Readable } from 'node:stream'
import { executeWorker } from './worker.mjs'
import { executeBuild } from './builder.mjs'
import { apiBase, readResponse } from './worker-http.mjs'
import { maintainLease } from './worker-lease.mjs'
import { runBuilderCli } from './index.mjs'
import { fixture, socketFixture, fakeDocker } from './fixtures/helpers.mjs'
import { write, snapshot } from '../forge-plugin/fixtures/helpers.mjs'
import { BuildError } from './errors.mjs'

const id = '00000000-0000-4000-8000-000000000000'
const wait = ms => new Promise(resolve => setTimeout(resolve, ms))

async function setup(t) {
  const item = await fixture(t)
  const { packageFile, packageSha256, ...config } = item.config
  void packageFile
  await write(item.delivery, 'worker.json', JSON.stringify({ ...config, apiBaseUrl: 'https://worker.example/api' }))
  const previous = process.env.FORGE_PLUGIN_WORKER_TOKEN
  process.env.FORGE_PLUGIN_WORKER_TOKEN = randomBytes(32).toString('hex')
  t.after(() => {
    if (previous === undefined) delete process.env.FORGE_PLUGIN_WORKER_TOKEN
    else process.env.FORGE_PLUGIN_WORKER_TOKEN = previous
  })
  const calls = []
  const request = async (connection, action, body) => {
    calls.push({ connection, action, body })
    if (action === 'archive') return item.data
    return { taskId: id, sha256: packageSha256, revision: 2, leaseSeconds: 90,
      archiveBytes: item.data.length,
      status: action === 'finish' ? (body.result.success ? 'built' : 'build_failed') : 'building' }
  }
  const options = { taskId: id, revision: 1, sha256: packageSha256, reviewed: true }
  return { ...item, file: path.join(item.delivery, 'worker.json'), request, options, calls }
}

function failedBuild() {
  return { status: 'failed', deployed: false, jobId: 'job-fixture',
    failurePhase: 'source_snapshot', failureCode: 'SOURCE_DIRTY' }
}

test('只领取指定的已审查任务，失败结果保留且不执行下一任务/部署', async (t) => {
  const item = await setup(t)
  const result = await executeWorker(item.file, item.options, {
    request: item.request, build: async () => failedBuild(),
  })
  assert.equal(result.status, 'build_failed')
  assert.equal(result.deployed, false)
  assert.deepEqual(item.calls.map(call => call.action), ['claim', 'archive', 'finish'])
  assert.equal(item.calls[0].body.sourceCommit, item.config.commit)
  assert.equal(item.calls[0].body.image, item.config.image)
  assert.ok(!JSON.stringify(result).includes(process.env.FORGE_PLUGIN_WORKER_TOKEN))
  assert.ok(!JSON.stringify(result).includes(item.calls[0].body.leaseToken))
  const root = (await fs.readdir(item.config.workspaceRoot)).find(name => name.startsWith('transfer-'))
  const receipt = await fs.readFile(path.join(item.config.workspaceRoot, root, 'worker-result.json'), 'utf8')
  assert.ok(!receipt.includes(process.env.FORGE_PLUGIN_WORKER_TOKEN))
  assert.equal((await fs.stat(path.join(item.config.workspaceRoot, root, 'lease.json'))).mode & 0o777, 0o600)
})

test('未审查或重复/非法 worker flags 拒绝，正常登录 Token 不作为机器凭证', async (t) => {
  const item = await setup(t)
  await assert.rejects(executeWorker(item.file, { ...item.options, reviewed: false }), /REVIEW/)
  process.env.FORGE_PLUGIN_WORKER_TOKEN = 'user-session-token'
  await assert.rejects(executeWorker(item.file, item.options), /TOKEN_REQUIRED/)
  const output = { log() {}, error() {} }
  assert.equal(await runBuilderCli(['worker', item.file, id, '1', item.options.sha256], output), 1)
  assert.equal(await runBuilderCli(['worker', item.file, id, '1', item.options.sha256,
    '--reviewed', '--reviewed'], output), 1)
  assert.equal(item.calls.length, 0)
})

test('包摘要/领取版本不一致均阻止构建；没有自动重新领取', async (t) => {
  const item = await setup(t)
  let built = false
  const build = async () => { built = true; return failedBuild() }
  const request = async (...args) => args[1] === 'archive' ? Buffer.from('wrong') : item.request(...args)
  const result = await executeWorker(item.file, item.options, { request, build })
  assert.equal(result.status, 'report_pending')
  assert.equal(result.failureCode, 'WORKER_PACKAGE_DIGEST_MISMATCH')
  assert.equal(built, false)
  assert.equal(item.calls.filter(call => call.action === 'claim').length, 1)
  const invalid = await executeWorker(item.file, item.options, {
    request: async () => ({ status: 'built' }), build,
  })
  assert.equal(invalid.status, 'claim_failed')
  assert.equal(built, false)
})

test('领取/回写网络不确定时只保留私有结果，不打印服务端原始错误或重试领取', async (t) => {
  const item = await setup(t)
  const request = async (...args) => {
    if (['claim', 'finish'].includes(args[1])) throw new Error('private response and credentials')
    return item.request(...args)
  }
  const result = await executeWorker(item.file, item.options, { request })
  assert.equal(result.status, 'claim_failed')
  assert.ok(!JSON.stringify(result).includes('private response'))
  const pending = await executeWorker(item.file, item.options, { request: async (...args) => {
    if (args[1] === 'finish') throw new BuildError('WORKER_HTTP_FAILED')
    return item.request(...args)
  }, build: async () => failedBuild() })
  assert.equal(pending.status, 'report_pending')
  assert.equal(pending.result.failureCode, 'SOURCE_DIRTY')
})

test('续期错误/信号失效中止本次构建，成功产物不能被伪装为正常远端完成', async (t) => {
  const item = await setup(t)
  let stopped = false
  const result = await executeWorker(item.file, item.options, {
    interval: 1,
    request: async (...args) => {
      if (args[1] === 'heartbeat') throw new BuildError('WORKER_HTTP_FAILED')
      return item.request(...args)
    },
    build: async (config, options) => {
      await wait(20)
      stopped = options.signal.aborted
      return { ...failedBuild(), status: 'built', artifacts: [{ path: 'backend/admin.jar', bytes: 4,
        sha256: 'a'.repeat(64) }] }
    },
  })
  assert.equal(stopped, true)
  assert.equal(result.status, 'build_failed')
  assert.equal(result.result.failureCode, 'WORKER_LEASE_LOST')
  assert.equal(result.result.artifactCount, null)
})

test('真实 core 构建桥接：Git/ZIP/阶段/实际摘要，无原工程写入或容器凭据（Docker仍是桩）', async (t) => {
  const item = await setup(t)
  await socketFixture(t, item.config.dockerSocket)
  const original = await snapshot(item.host.root)
  const docker = fakeDocker(item.config, { build: async (args) => {
    const mount = args.find(arg => arg.endsWith('dst=/output'))
    const output = mount.slice('type=bind,src='.length, mount.indexOf(',dst='))
    await write(output, 'backend/admin.jar', Buffer.from([0x50, 0x4b, 3, 4]))
    await write(output, 'frontend/index.html', '<title>synthetic fixture</title>')
  } })
  const result = await executeWorker(item.file, item.options, { request: item.request,
    build: (config, options) => executeBuild(config, { ...options, execute: docker.execute }) })
  assert.equal(result.status, 'built')
  assert.equal(result.result.artifactCount, 2)
  assert.match(result.result.artifactManifestSha256, /^[a-f0-9]{64}$/)
  assert.deepEqual(item.calls.filter(call => call.action === 'heartbeat').map(call => call.body.phase),
    ['source_snapshot', 'package_preflight', 'source_preflight', 'container_build', 'artifact_verification'])
  assert.deepEqual(await snapshot(item.host.root), original)
  for (const call of docker.calls) {
    assert.ok(!JSON.stringify(call).includes(process.env.FORGE_PLUGIN_WORKER_TOKEN))
    assert.ok(!JSON.stringify(call).includes(item.calls[0].body.leaseToken))
    assert.ok(!JSON.stringify(call.options.env).includes('FORGE_PLUGIN_WORKER_TOKEN'))
  }
})

test('HTTPS origin 必须规范且无凭据/query/跳转；响应有界且不把错误 JSON 当 ZIP', async () => {
  assert.equal(apiBase('https://worker.example/api/'), 'https://worker.example/api')
  for (const value of ['http://worker.example', 'https://user:secret@worker.example',
    'https://worker.example/api?key=secret', 'https://worker.example/a/../api', 'https://worker.example/%61']) {
    assert.throws(() => apiBase(value))
  }
  function response(data, status = 200, type = 'application/json') {
    const stream = Readable.from([Buffer.from(data)])
    stream.statusCode = status
    stream.headers = { 'content-type': type }
    return stream
  }
  assert.equal((await readResponse(response('{"code":200,"data":{"status":"building"}}'), 'claim', 65536))
    .status, 'building')
  await assert.rejects(readResponse(response('redirect', 302), 'claim', 65536))
  await assert.rejects(readResponse(response('{"code":500}'), 'archive', 8 * 1024 * 1024))
  await assert.rejects(readResponse(response('x'.repeat(30)), 'claim', 10))
  await assert.rejects(readResponse(response('{"code":200,"code":500,"data":{}}'), 'claim', 65536))
})

test('续期串行化，不让旧阶段覆盖新阶段，并在 stop 后不再发请求', async () => {
  const controller = new AbortController()
  const phases = []
  const lease = maintainLease(async (action, body) => {
    phases.push(body.phase)
    await wait(2)
    return { status: 'building', leaseSeconds: 90 }
  }, () => controller.abort(), controller.signal, 1000)
  await Promise.all([lease.advance('package_preflight'), lease.advance('source_preflight')])
  assert.deepEqual(phases, ['source_preflight', 'source_preflight'])
  await lease.stop()
  const count = phases.length
  await wait(5)
  assert.equal(phases.length, count)
})
