import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs/promises'
import path from 'node:path'
import { randomUUID } from 'node:crypto'
import { fixture, write } from './fixtures/helpers.mjs'
import { publishRelease } from './vault.mjs'
import { prepareRegistration, readRegistrationConfig } from './registration.mjs'
import { runReleaseCli } from './index.mjs'

async function setup(t) {
  const item = await fixture(t)
  const sealed = await publishRelease(item.config, { reviewed: true })
  const settings = { protocolVersion: 1, repositoryId: item.config.repositoryId, vaultRoot: item.vaultRoot,
    taskId: randomUUID(), revision: 4, reviewId: randomUUID(), serverResultSha256: 'e'.repeat(64) }
  await write(item.root, 'registration.json', JSON.stringify(settings))
  const file = path.join(item.root, 'registration.json')
  return { ...item, sealed, file, settings, registration: await readRegistrationConfig(file) }
}

test('真实文件复验后小型元数据导出不写封存/联网/声明授权', async (t) => {
  const item = await setup(t)
  const receipt = path.join(item.vaultRoot, item.sealed.releaseId, 'receipt.json')
  const before = await fs.readFile(receipt)
  const metadata = await prepareRegistration(item.registration, item.sealed.releaseId)
  assert.equal(metadata.taskId, item.settings.taskId)
  assert.equal(metadata.reviewId, item.settings.reviewId)
  assert.equal(metadata.releaseId, 'rel-' + metadata.manifestSha256)
  assert.equal(metadata.resultSha256, item.config.resultSha256)
  assert.equal(metadata.serverResultSha256, 'e'.repeat(64))
  assert.notEqual(metadata.resultSha256, metadata.serverResultSha256)
  assert.equal(metadata.result.artifactCount, 2)
  assert.equal(metadata.result.success, true)
  assert.ok(Buffer.byteLength(JSON.stringify(metadata)) < 65536)
  assert.doesNotMatch(JSON.stringify(metadata), /vaultRoot|jobRoot|https:|workerId|Token|approvalChecked|deployed/)
  assert.deepEqual(await fs.readFile(receipt), before)
  assert.deepEqual(await fs.readdir(item.vaultRoot), [item.sealed.releaseId])
})

for (const extra of [{ token: 'private' }, { tenantId: 2 }, { apiBaseUrl: 'https://example.invalid' }]) {
  test('配置拒绝凭证/身份/网络目标 ' + Object.keys(extra)[0], async (t) => {
    const item = await setup(t)
    await write(item.root, 'registration.json', JSON.stringify({ ...item.settings, ...extra }))
    await assert.rejects(readRegistrationConfig(item.file), /REGISTRATION_CONFIG_INVALID/)
  })
}

test('配置拒绝旧协议/非整数修订/重复字段/非法审批绑定', async (t) => {
  const item = await setup(t)
  for (const change of [{ protocolVersion: 2 }, { revision: 1.5 }, { reviewId: 'invalid' },
    { serverResultSha256: 1 }, { taskId: '00000000-0000-4000-8000-00000000000A' }]) {
    await write(item.root, 'registration.json', JSON.stringify({ ...item.settings, ...change }))
    await assert.rejects(readRegistrationConfig(item.file))
  }
  await write(item.root, 'registration.json', '{"revision":1,"revision":2}')
  await assert.rejects(readRegistrationConfig(item.file))
})

test('文件被改/取消/不匹配仓库不能导出登记数据', async (t) => {
  const item = await setup(t)
  await assert.rejects(prepareRegistration({ ...item.registration, repositoryId: 'other' }, item.sealed.releaseId))
  await assert.rejects(prepareRegistration(item.registration, item.sealed.releaseId, AbortSignal.abort()),
    /INTERRUPTED/)
  const target = path.join(item.vaultRoot, item.sealed.releaseId, 'artifacts/frontend/index.html')
  await fs.chmod(target, 0o600)
  await fs.writeFile(target, 'changed')
  await fs.chmod(target, 0o400)
  await assert.rejects(prepareRegistration(item.registration, item.sealed.releaseId), /SNAPSHOT_ARTIFACT_MISMATCH/)
})

test('CLI只输出导入协议且错误不泄露本地目录', async (t) => {
  const item = await setup(t)
  const messages = [], errors = []
  const output = { log: value => messages.push(value), error: value => errors.push(value) }
  assert.equal(await runReleaseCli(['prepare-registration', item.file, item.sealed.releaseId], output), 0)
  const value = JSON.parse(messages.pop())
  assert.equal(value.taskId, item.settings.taskId)
  assert.ok(!JSON.stringify(value).includes(item.root))
  assert.equal(await runReleaseCli(['prepare-registration', item.file, item.sealed.releaseId, '--reviewed'], output), 1)
  assert.equal(JSON.parse(errors.pop()).code, 'RELEASE_COMMAND_INVALID')
})
