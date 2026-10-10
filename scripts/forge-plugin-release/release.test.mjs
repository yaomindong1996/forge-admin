import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs/promises'
import { readdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { fixture, updateResult, write, writable } from './fixtures/helpers.mjs'
import { readReleaseConfig, readJson } from './config.mjs'
import { checkRelease, publishRelease } from './vault.mjs'
import { verifyRelease } from './verify.mjs'
import { runReleaseCli } from './index.mjs'
import { serialize } from './manifest.mjs'
import { copyArtifacts } from './copy.mjs'
import { collectArtifacts } from '../forge-plugin-builder/artifacts.mjs'

const reviewed = { reviewed: true }

test('跨多个64KiB块复制仍核对全部字节，不使用硬链接或整文件缓冲', async (t) => {
  const item = await fixture(t)
  const data = Buffer.alloc(2 * 1024 * 1024 + 7, 0x5a)
  await write(item.artifactRoot, 'frontend/assets/chunks.bin', data)
  const artifacts = await collectArtifacts(item.artifactRoot, item.result.preflight.targets)
  await updateResult(item, result => { result.artifacts = artifacts })
  const sealed = await publishRelease(await readReleaseConfig(item.file), reviewed)
  const copied = path.join(item.vaultRoot, sealed.releaseId, 'artifacts/frontend/assets/chunks.bin')
  assert.deepEqual(await fs.readFile(copied), data)
  assert.equal((await fs.stat(copied)).nlink, 1)
  assert.equal(sealed.artifactCount, 3)
  assert.equal((await verifyRelease(await readReleaseConfig(item.vaultFile, 'verify'), sealed.releaseId)).status,
    'verified')
})

test('check 不写 vault；封存仅产物和元数据，核验只读、幂等且保留原件', async (t) => {
  const item = await fixture(t)
  await write(item.jobRoot, 'source/private.txt', 'synthetic excluded source')
  await write(item.jobRoot, 'control/private.json', 'synthetic excluded config')
  const config = await readReleaseConfig(item.file)
  const checked = await checkRelease(config)
  assert.equal(checked.status, 'checked')
  assert.equal(checked.localReviewDeclared, false)
  assert.deepEqual(await fs.readdir(item.vaultRoot), [])
  const sealed = await publishRelease(config, reviewed)
  assert.equal(sealed.status, 'sealed')
  assert.equal(sealed.releaseId, checked.releaseId)
  assert.equal(sealed.artifactCount, 2)
  assert.equal(sealed.deployed, false)
  assert.equal(sealed.liveTaskApprovalVerified, false)
  assert.deepEqual(sealed.deploymentPreparation.slice(1).map(row => row.status), Array(5).fill('pending'))
  const root = path.join(item.vaultRoot, sealed.releaseId)
  assert.deepEqual((await fs.readdir(root)).sort(), ['artifacts', 'manifest.json', 'receipt.json'])
  assert.equal((await fs.stat(root)).mode & 0o777, 0o500)
  for (const file of item.result.artifacts) {
    const target = path.join(root, 'artifacts', file.path)
    assert.deepEqual(await fs.readFile(target), await fs.readFile(path.join(item.artifactRoot, file.path)))
    const stat = await fs.stat(target)
    assert.equal(stat.mode & 0o777, 0o400)
    assert.equal(stat.nlink, 1)
  }
  const before = await fs.readFile(path.join(root, 'receipt.json'))
  assert.equal((await publishRelease(config, reviewed)).status, 'already_sealed')
  assert.deepEqual(await fs.readFile(path.join(root, 'receipt.json')), before)
  const verified = await verifyRelease(await readReleaseConfig(item.vaultFile, 'verify'), sealed.releaseId)
  assert.equal(verified.status, 'verified')
  assert.equal(verified.releaseId, sealed.releaseId)
  assert.deepEqual(await fs.readdir(item.vaultRoot), [sealed.releaseId])
  assert.doesNotMatch(JSON.stringify(sealed), /private|sourceRoot|jobRoot|vaultRoot/)
})

for (const targets of [{ server: 'server-only' }, { ui: 'ui-only' }]) {
  test(`允许独立 ${Object.keys(targets)[0]} 产物而非强制双端`, async (t) => {
    const item = await fixture(t, targets)
    const sealed = await publishRelease(await readReleaseConfig(item.file), reviewed)
    assert.equal(sealed.artifactCount, 1)
    assert.equal((await verifyRelease(await readReleaseConfig(item.vaultFile, 'verify'), sealed.releaseId)).status,
      'verified')
  })
}

for (const status of ['checked', 'failed', 'building']) {
  test(`${status} 报告不能作为成功制品封存`, async (t) => {
    const item = await fixture(t)
    await updateResult(item, result => { result.status = status })
    await assert.rejects(publishRelease(await readReleaseConfig(item.file), reviewed), /SUCCESSFUL_BUILD_REQUIRED/)
    assert.deepEqual(await fs.readdir(item.vaultRoot), [])
  })
}

test('已部署/原始摘要变化/报告 jobId 不符均拒绝', async (t) => {
  const item = await fixture(t)
  await updateResult(item, result => { result.deployed = true })
  await assert.rejects(checkRelease(item.config), /SUCCESSFUL_BUILD_REQUIRED/)
  await updateResult(item, result => { result.deployed = false; result.jobId = 'job-other' })
  await assert.rejects(checkRelease(item.config), /BUILD_JOB_MISMATCH/)
  await write(item.jobRoot, 'result.json', serialize(item.result) + ' ')
  await assert.rejects(checkRelease(item.config), /BUILD_RESULT_MISMATCH/)
})

for (const kind of ['changed', 'missing', 'extra', 'link', 'hardlink', 'text-jar', 'oversize']) {
  test(`不相信声明摘要：实际文件 ${kind} 时拒绝且不封存`, async (t) => {
    const item = await fixture(t)
    const file = path.join(item.artifactRoot, 'frontend/index.html')
    if (kind === 'changed') await fs.writeFile(file, 'changed synthetic fixture')
    if (kind === 'missing') await fs.unlink(file)
    if (kind === 'extra') await write(item.artifactRoot, 'frontend/extra.js', 'extra')
    if (kind === 'link') await fs.symlink(file, path.join(item.artifactRoot, 'frontend/link'))
    if (kind === 'hardlink') await fs.link(file, path.join(item.artifactRoot, 'frontend/hardlink'))
    if (kind === 'text-jar') await fs.writeFile(path.join(item.artifactRoot, 'backend/admin.jar'), 'not a jar')
    if (kind === 'oversize') await fs.truncate(file, 512 * 1024 * 1024 + 1)
    await assert.rejects(publishRelease(await readReleaseConfig(item.file), reviewed))
    assert.deepEqual(await fs.readdir(item.vaultRoot), [])
  })
}

test('封存必须显式 reviewed；遗留锁不抢占/不删除', async (t) => {
  const item = await fixture(t)
  await assert.rejects(publishRelease(item.config), /REVIEW_REQUIRED/)
  await write(item.vaultRoot, '.publish-lock/owner.json', '{"nonce":"synthetic external owner"}')
  await assert.rejects(publishRelease(item.config, reviewed), /VAULT_BUSY/)
  assert.deepEqual(await fs.readdir(item.vaultRoot), ['.publish-lock'])
  assert.match(await fs.readFile(path.join(item.vaultRoot, '.publish-lock/owner.json'), 'utf8'), /external owner/)
})

test('两个并发发布不覆盖：一个封存，一个锁冲突，重试复验后幂等', async (t) => {
  const item = await fixture(t)
  const config = await readReleaseConfig(item.file)
  const results = await Promise.allSettled([publishRelease(config, reviewed), publishRelease(config, reviewed)])
  assert.equal(results.filter(result => result.status === 'fulfilled').length, 1)
  assert.equal(results.find(result => result.status === 'rejected').reason.code, 'VAULT_BUSY')
  assert.equal((await publishRelease(config, reviewed)).status, 'already_sealed')
  assert.equal((await fs.readdir(item.vaultRoot)).length, 1)
})

test('发布暂存时中止：保留暂存但不暴露最终快照、不泄留自己的锁', async (t) => {
  const item = await fixture(t)
  const signal = { get aborted() { return readdirSync(item.vaultRoot).some(name => name.startsWith('.pending-')) } }
  await assert.rejects(publishRelease(item.config, { reviewed: true, signal }), /INTERRUPTED/)
  const names = await fs.readdir(item.vaultRoot)
  assert.equal(names.length, 1)
  assert.match(names[0], /^\.pending-/)
  const sealed = await publishRelease(item.config, reviewed)
  assert.equal(sealed.status, 'sealed')
  assert.equal((await fs.readdir(item.vaultRoot)).length, 2)
})

test('复制阶段重新计算实际摘要，摘要不符保留诊断暂存', async (t) => {
  const item = await fixture(t)
  const destination = path.join(item.root, 'copy')
  const files = item.result.artifacts.map(file => ({ ...file, sha256: 'f'.repeat(64) }))
  await assert.rejects(copyArtifacts(item.artifactRoot, destination, files), /ARTIFACT_CHANGED/)
  assert.ok((await fs.stat(path.join(destination, files[0].path))).isFile())
})

for (const kind of ['artifact', 'manifest', 'receipt', 'permissions', 'extra', 'link']) {
  test(`已有快照 ${kind} 被改写：verify/发布重试拒绝且不修复覆盖`, async (t) => {
    const item = await fixture(t)
    const config = await readReleaseConfig(item.file)
    const sealed = await publishRelease(config, reviewed)
    const root = path.join(item.vaultRoot, sealed.releaseId)
    if (kind === 'artifact') {
      const file = path.join(root, 'artifacts/frontend/index.html')
      await fs.chmod(file, 0o600)
      await fs.writeFile(file, 'corrupt')
      await fs.chmod(file, 0o400)
    }
    if (kind === 'manifest') {
      const file = path.join(root, 'manifest.json')
      await fs.chmod(file, 0o600)
      await fs.appendFile(file, ' ')
      await fs.chmod(file, 0o400)
    }
    if (kind === 'receipt') {
      const file = path.join(root, 'receipt.json')
      const value = (await readJson(file)).value
      value.liveTaskApprovalVerified = true
      await fs.chmod(file, 0o600)
      await fs.writeFile(file, serialize(value))
      await fs.chmod(file, 0o400)
    }
    if (kind === 'permissions') await fs.chmod(path.join(root, 'artifacts'), 0o700)
    if (kind === 'extra') { await fs.chmod(root, 0o700); await write(root, 'unlisted', 'unlisted') }
    if (kind === 'link') {
      const dir = path.join(root, 'artifacts/frontend')
      await fs.chmod(dir, 0o700)
      await fs.symlink(path.join(dir, 'index.html'), path.join(dir, 'link'))
      await fs.chmod(dir, 0o500)
    }
    await assert.rejects(verifyRelease(await readReleaseConfig(item.vaultFile, 'verify'), sealed.releaseId))
    await assert.rejects(publishRelease(config, reviewed))
    assert.deepEqual(await fs.readdir(item.vaultRoot), [sealed.releaseId])
  })
}

test('离线 verify 不依赖已删除的原工作区，必须匹配仓库和固定 ID', async (t) => {
  const item = await fixture(t)
  const sealed = await publishRelease(await readReleaseConfig(item.file), reviewed)
  // fixture 内原工作区主动移走，验证只读快照而非回读旧产物。
  await fs.rename(item.jobRoot, path.join(item.root, 'original-moved'))
  const config = await readReleaseConfig(item.vaultFile, 'verify')
  assert.equal((await verifyRelease(config, sealed.releaseId)).status, 'verified')
  await assert.rejects(verifyRelease({ ...config, repositoryId: 'other' }, sealed.releaseId),
    /SNAPSHOT_MANIFEST_MISMATCH/)
  await assert.rejects(verifyRelease(config, '../escape'), /RELEASE_ID_INVALID/)
})

test('CLI 严格参数、错误输出不含私有路径，结束后释放 signal listener', async (t) => {
  const item = await fixture(t)
  const output = { logs: [], errors: [],
    log(value) { this.logs.push(value) }, error(value) { this.errors.push(value) } }
  const count = process.listenerCount('SIGINT')
  for (const args of [[], ['run', item.file], ['publish', item.file], ['check', item.file, '--reviewed'],
    ['publish', item.file, '--reviewed', '--reviewed'], ['verify', item.file], ['check', 'relative.json']]) {
    assert.equal(await runReleaseCli(args, output), 1)
  }
  assert.equal(await runReleaseCli(['check', '/private/tmp/synthetic-nonexistent.json'], output), 1)
  assert.ok(output.errors.every(error => !error.includes(item.root) && !error.includes('/private/')))
  assert.equal(await runReleaseCli(['check', item.file], output), 0)
  assert.equal(process.listenerCount('SIGINT'), count)
  const cli = fileURLToPath(new URL('./index.mjs', import.meta.url))
  const run = spawnSync(process.execPath, [cli, 'publish', item.file, '--reviewed'], { encoding: 'utf8' })
  assert.equal(run.status, 0, run.stderr)
  const sealed = JSON.parse(run.stdout)
  const verify = spawnSync(process.execPath, [cli, 'verify', item.vaultFile, sealed.releaseId], { encoding: 'utf8' })
  assert.equal(verify.status, 0, verify.stderr)
  assert.equal(JSON.parse(verify.stdout).deployed, false)
  assert.doesNotMatch(run.stdout + verify.stdout, new RegExp(item.root))
  await writable(item.vaultRoot)
})
