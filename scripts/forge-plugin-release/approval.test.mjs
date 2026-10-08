import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs/promises'
import path from 'node:path'
import { randomBytes, randomUUID } from 'node:crypto'
import https from 'node:https'
import { EventEmitter } from 'node:events'
import { Readable } from 'node:stream'
import { fixture, write } from './fixtures/helpers.mjs'
import { publishRelease } from './vault.mjs'
import { verifyRelease } from './verify.mjs'
import { readApprovalConfig } from './approval-config.mjs'
import { verifyApproval } from './approval.mjs'
import { runReleaseCli } from './index.mjs'
import { reportResult } from '../forge-plugin-builder/worker-result.mjs'

const taskId = '00000000-0000-4000-8000-000000000000'

async function setup(t) {
  const item = await fixture(t)
  const sealed = await publishRelease(item.config, { reviewed: true })
  const settings = { protocolVersion: 1, repositoryId: item.config.repositoryId, vaultRoot: item.vaultRoot,
    apiBaseUrl: 'https://worker.example/api', taskId, revision: 4, reviewId: randomUUID(),
    serverResultSha256: 'e'.repeat(64), workerId: 'original-worker' }
  const file = path.join(item.root, 'approval.json')
  await write(item.root, 'approval.json', JSON.stringify(settings))
  const previous = process.env.FORGE_PLUGIN_WORKER_TOKEN
  const token = randomBytes(32).toString('hex')
  process.env.FORGE_PLUGIN_WORKER_TOKEN = token
  t.after(() => {
    if (previous === undefined) delete process.env.FORGE_PLUGIN_WORKER_TOKEN
    else process.env.FORGE_PLUGIN_WORKER_TOKEN = previous
  })
  return { ...item, rawResultSha256: item.config.resultSha256,
    settings, file, token, sealed, config: await readApprovalConfig(file) }
}

function response(item, body) {
  return { protocolVersion: 1, checkId: body.checkId, taskId, revision: item.config.revision,
    reviewId: item.config.reviewId, workerId: item.config.workerId,
    serverResultSha256: item.config.serverResultSha256, manifestSha256: body.manifestSha256,
    checkedAt: new Date().toISOString(), liveTaskApprovalVerified: true, deployed: false }
}

test('逐项报告桥接、摘要类型分离、核验有时点而不改封存收据/工作区', async (t) => {
  const item = await setup(t)
  const receiptFile = path.join(item.vaultRoot, item.sealed.releaseId, 'receipt.json')
  const before = await fs.readFile(receiptFile)
  let calls = 0
  const value = await verifyApproval(item.config, item.sealed.releaseId, {}, {
    request: async (connection, action, body) => {
      calls++
      assert.equal(action, 'approval-check')
      assert.equal(connection.taskId, taskId)
      assert.equal(connection.token, item.token)
      assert.equal(body.serverResultSha256, item.settings.serverResultSha256)
      assert.notEqual(body.serverResultSha256, item.sealed.manifestSha256)
      assert.notEqual(body.serverResultSha256, item.rawResultSha256)
      assert.deepEqual(body.result, reportResult(item.result, { packageSha256: item.result.packageSha256,
        commit: item.result.source.commit, image: item.result.image }))
      assert.doesNotMatch(JSON.stringify(body), /vaultRoot|jobRoot|\/private|Authorization|resultJson|artifacts"/)
      return response(item, body)
    },
  })
  assert.equal(calls, 1)
  assert.equal(value.status, 'approval_checked')
  assert.equal(value.liveTaskApprovalVerified, true)
  assert.equal(value.deployed, false)
  assert.equal(value.approval.validOnlyAtCheck, true)
  assert.equal(value.approval.deploymentAuthorized, false)
  assert.equal(value.approval.serverArtifactBytesVerified, false)
  assert.equal(value.deploymentPreparation[1].status, 'passed_at_check')
  assert.deepEqual(value.deploymentPreparation.slice(2).map(item => item.status), Array(4).fill('pending'))
  assert.deepEqual(await fs.readFile(receiptFile), before)
  assert.deepEqual(await fs.readdir(item.vaultRoot), [item.sealed.releaseId])
  assert.equal((await verifyRelease(item.config, item.sealed.releaseId)).liveTaskApprovalVerified, false)
  assert.ok(!JSON.stringify(value).includes(item.token))
})

for (const field of ['protocolVersion', 'checkId', 'taskId', 'revision', 'reviewId', 'workerId',
  'serverResultSha256', 'manifestSha256', 'liveTaskApprovalVerified', 'deployed']) {
  test(`服务端返回绑定 ${field} 不一致失败关闭`, async (t) => {
    const item = await setup(t)
    await assert.rejects(verifyApproval(item.config, item.sealed.releaseId, {}, {
      request: async (connection, action, body) => ({ ...response(item, body), [field]: 'wrong' }),
    }), /APPROVAL_RESPONSE_MISMATCH/)
    assert.equal((await verifyRelease(item.config, item.sealed.releaseId)).liveTaskApprovalVerified, false)
  })
}

for (const checkedAt of ['2020-01-01T00:00:00.000Z', '2099-01-01T00:00:00.000Z',
  '2026-02-30T00:00:00.000Z', 'invalid', 1]) {
  test(`陈旧/未来/无效时间 ${checkedAt} 不充当实时认证`, async (t) => {
    const item = await setup(t)
    await assert.rejects(verifyApproval(item.config, item.sealed.releaseId, {}, {
      request: async (connection, action, body) => ({ ...response(item, body), checkedAt }),
    }), /APPROVAL_TIME_INVALID/)
  })
}

test('未知响应字段/空响应/拒绝不自动重试、不落本地成功收据', async (t) => {
  const item = await setup(t)
  let calls = 0
  const rejected = async () => { calls++; throw new Error('synthetic拒绝，不能记为成功') }
  await assert.rejects(verifyApproval(item.config, item.sealed.releaseId, {}, { request: rejected }))
  assert.equal(calls, 1)
  for (const kind of ['unknown', 'empty']) {
    await assert.rejects(verifyApproval(item.config, item.sealed.releaseId, {}, {
      request: async (connection, action, body) => kind === 'empty' ? null : { ...response(item, body), command: 'x' },
    }), /APPROVAL_RESPONSE_INVALID/)
  }
  assert.deepEqual((await fs.readdir(path.join(item.vaultRoot, item.sealed.releaseId))).sort(),
    ['artifacts', 'manifest.json', 'receipt.json'])
})

test('网络往返中实际文件被改写则拒绝成功返回', async (t) => {
  const item = await setup(t)
  const target = path.join(item.vaultRoot, item.sealed.releaseId, 'artifacts/frontend/index.html')
  await assert.rejects(verifyApproval(item.config, item.sealed.releaseId, {}, {
    request: async (connection, action, body) => {
      await fs.chmod(target, 0o600)
      await fs.writeFile(target, 'changed during synthetic network')
      await fs.chmod(target, 0o400)
      return response(item, body)
    },
  }), /SNAPSHOT_ARTIFACT_MISMATCH/)
})

test('中止/缺凭证/原本损坏的候选均不继续网络或输出通过', async (t) => {
  const item = await setup(t)
  const controller = new AbortController()
  const dependencies = { request: async (connection, action, body) => {
    controller.abort()
    return response(item, body)
  } }
  await assert.rejects(verifyApproval(item.config, item.sealed.releaseId,
    { signal: controller.signal }, dependencies), /INTERRUPTED/)
  let called = false
  const noCall = { request: async () => { called = true } }
  await assert.rejects(verifyApproval(item.config, item.sealed.releaseId,
    { signal: controller.signal }, noCall), /INTERRUPTED/)
  delete process.env.FORGE_PLUGIN_WORKER_TOKEN
  await assert.rejects(verifyApproval(item.config, item.sealed.releaseId, {}, noCall), /WORKER_TOKEN_REQUIRED/)
  assert.equal(called, false)
})

for (const change of [{ token: 'forbidden' }, { tenantId: 2 }, { command: 'forbidden' }, { revision: 0 },
  { revision: 2147483648 }, { revision: '4' }, { taskId: '../wrong' }, { reviewId: null },
  { serverResultSha256: null }, { workerId: '' }, { apiBaseUrl: 'http://worker.example' },
  { apiBaseUrl: 'https://worker.example/api?redirect=x' }, { apiBaseUrl: 'https://name:secret@worker.example' }]) {
  test(`配置拒绝 ${JSON.stringify(change)} 不扩展身份/网络/命令`, async (t) => {
    const item = await setup(t)
    await write(item.root, 'approval.json', JSON.stringify({ ...item.settings, ...change }))
    await assert.rejects(readApprovalConfig(item.file))
  })
}

// 实际CLI及HTTPS适配器，只替代传输，不把模拟响应称作真服务验收。
function mockTransport(t, respond, http = {}) {
  let observed
  t.mock.method(https, 'request', (url, options, callback) => {
    const request = new EventEmitter()
    request.destroy = error => { request.emit('error', error); request.emit('close') }
    request.end = payload => {
      observed = { url, options, payload: JSON.parse(payload.toString('utf8')) }
      const response = Readable.from([Buffer.from(respond(observed.payload))])
      response.statusCode = http.statusCode ?? 200
      response.headers = { 'content-type': http.type ?? 'application/json' }
      response.on('end', () => request.emit('close'))
      callback(response)
    }
    return request
  })
  return () => observed
}

test('实际CLI固定HTTPS/JSON/nonce单次调用，输出时点而不输出凭证/路径', async (t) => {
  const item = await setup(t)
  const observed = mockTransport(t, body => JSON.stringify({ code: 200, data: response(item, body) }))
  const logs = []
  const result = await runReleaseCli(['verify-approval', item.file, item.sealed.releaseId],
    { log: value => logs.push(value), error: value => logs.push(value) })
  assert.equal(result, 0)
  const value = observed()
  assert.equal(value.url.href, `${item.config.apiBaseUrl}/internal/plugin-build/${taskId}/approval-check`)
  assert.equal(value.options.method, 'POST')
  assert.equal(value.options.rejectUnauthorized, true)
  assert.equal(value.options.headers['X-Inner-Call'], undefined)
  assert.equal(value.options.headers.Authorization, `Bearer ${item.token}`)
  assert.match(value.options.headers['X-Nonce'], /^[a-f0-9-]{36}$/)
  assert.equal(JSON.parse(logs[0]).approval.validOnlyAtCheck, true)
  assert.ok(!logs.join('').includes(item.token) && !logs.join('').includes(item.root))
})

test('CLI拒绝重复键/超限响应、错误命令及原始网络错误，不输出成功', async (t) => {
  const item = await setup(t)
  for (const reply of ['{"code":200,"code":200,"data":{}}', 'x'.repeat(65537)]) {
    t.mock.reset()
    mockTransport(t, () => reply)
    const logs = []
    assert.equal(await runReleaseCli(['verify-approval', item.file, item.sealed.releaseId],
      { log: value => logs.push(value), error: value => logs.push(value) }), 1)
    assert.equal(logs.length, 1)
    assert.equal(JSON.parse(logs[0]).deployed, false)
    assert.ok(!logs[0].includes(item.root) && !logs[0].includes(item.token))
  }
  const errors = []
  assert.equal(await runReleaseCli(['verify-approval', item.file, item.sealed.releaseId, '--force'],
    { log: () => assert.fail('unexpected success'), error: value => errors.push(value) }), 1)
  assert.match(errors[0], /RELEASE_COMMAND_INVALID/)
})

for (const http of [{ statusCode: 302 }, { statusCode: 401 }, { statusCode: 409 },
  { type: 'text/html' }]) {
  test(`认证核验不跟随重定向或吞掉拒绝 ${JSON.stringify(http)}`, async (t) => {
    const item = await setup(t)
    mockTransport(t, body => JSON.stringify({ code: 200, data: response(item, body) }), http)
    const errors = []
    assert.equal(await runReleaseCli(['verify-approval', item.file, item.sealed.releaseId],
      { log: () => assert.fail('unexpected success'), error: value => errors.push(value) }), 1)
    assert.match(errors[0], /WORKER_RESPONSE_REJECTED/)
  })
}
