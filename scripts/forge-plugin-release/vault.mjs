import fs from 'node:fs/promises'
import { constants } from 'node:fs'
import path from 'node:path'
import { randomUUID } from 'node:crypto'
import { safeTarget, statOptional } from '../forge-plugin/paths.mjs'
import { writePrivate } from '../forge-plugin-builder/files.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'
import { privateDirectory, readJson } from './config.mjs'
import { assertResultUnchanged, inspectRelease } from './evidence.mjs'
import { releaseId, serialize } from './manifest.mjs'
import { copyArtifacts, sealTree, syncDirectory } from './copy.mjs'
import { summary, verifySnapshot } from './verify.mjs'

export async function checkRelease(config, signal) {
  return summary(await inspectRelease(config, signal), 'checked')
}

export async function publishRelease(config, options = {}) {
  ensure(options.reviewed === true, 'REVIEW_REQUIRED')
  await privateDirectory(config.vaultRoot)
  const unlock = await acquireLock(config.vaultRoot)
  try {
    const manifest = await inspectRelease(config, options.signal)
    const id = releaseId(manifest)
    const finalRoot = await safeTarget(config.vaultRoot, id)
    if (await statOptional(finalRoot)) {
      await verifySnapshot(finalRoot, id, config.repositoryId, options.signal)
      return summary(manifest, 'already_sealed')
    }
    const staging = await fs.mkdtemp(path.join(config.vaultRoot, '.pending-'))
    await fs.chmod(staging, 0o700)
    await copyArtifacts(await safeTarget(config.jobRoot, 'artifacts'),
      path.join(staging, 'artifacts'), manifest.artifacts, options.signal)
    await writeMetadata(staging, manifest)
    await sealTree(staging)
    await verifySnapshot(staging, id, config.repositoryId, options.signal)
    await assertResultUnchanged(config)
    ensure(!options.signal?.aborted, 'INTERRUPTED')
    // 所有写者须遵循同一独占锁；不更新 latest 或覆盖旧快照。
    ensure(!await statOptional(finalRoot), 'RELEASE_ALREADY_EXISTS')
    await commitSnapshot(staging, finalRoot)
    await verifySnapshot(finalRoot, id, config.repositoryId, options.signal)
    await syncDirectory(config.vaultRoot)
    return summary(manifest, 'sealed')
  }
  finally { await unlock() }
}

async function commitSnapshot(staging, finalRoot) {
  // macOS 重命名目录要求源目录可写；仅短暂解锁顶层，子目录/文件始终只读且 vault 私有。
  // 崩溃遗留目录须人工隔离；后续发布不会自动修复或覆盖。
  await fs.chmod(staging, 0o700)
  let moved = false
  try {
    await fs.rename(staging, finalRoot)
    moved = true
  }
  finally { await fs.chmod(moved ? finalRoot : staging, 0o500) }
  await syncDirectory(finalRoot)
}

async function writeMetadata(root, manifest) {
  const id = releaseId(manifest)
  await writePrivate(root, 'manifest.json', serialize(manifest))
  await writePrivate(root, 'receipt.json', serialize({ protocolVersion: 1, releaseId: id,
    manifestSha256: id.slice(4), sealedAt: new Date().toISOString(), localReviewDeclared: true,
    liveTaskApprovalVerified: false, deployed: false }))
  for (const name of ['manifest.json', 'receipt.json']) {
    const flags = constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK
    const handle = await fs.open(await safeTarget(root, name), flags)
    try { await handle.sync() }
    finally { await handle.close() }
  }
}

async function acquireLock(root) {
  const lock = await safeTarget(root, '.publish-lock')
  try { await fs.mkdir(lock, { mode: 0o700 }) }
  catch (error) {
    if (error.code === 'EEXIST') ensure(false, 'VAULT_BUSY')
    throw error
  }
  const nonce = randomUUID()
  const identity = await fs.lstat(lock)
  await writePrivate(lock, 'owner.json', serialize({ nonce }))
  // 仅移除本进程创建且 nonce/inode 未变化的锁；失联遗留锁不能自动抢占。
  return async () => {
    const current = await fs.lstat(lock)
    const { value } = await readJson(await safeTarget(lock, 'owner.json'))
    ensure(current.ino === identity.ino && current.dev === identity.dev && value.nonce === nonce, 'VAULT_LOCK_CHANGED')
    await fs.unlink(await safeTarget(lock, 'owner.json'))
    await fs.rmdir(lock)
  }
}
