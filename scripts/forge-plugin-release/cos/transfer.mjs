import fs from 'node:fs/promises'
import path from 'node:path'
import { constants } from 'node:fs'
import { safeTarget, statOptional } from '../../forge-plugin/paths.mjs'
import { ensure } from '../../forge-plugin-builder/errors.mjs'
import { writePrivate, sha256 } from '../../forge-plugin-builder/files.mjs'
import { collectArtifacts } from '../../forge-plugin-builder/artifacts.mjs'
import { parseStrictJson } from '../../forge-plugin/json.mjs'
import { readJson } from '../config.mjs'
import { normalizeManifest, metadataLimit, serialize, targetProfile, releaseId } from '../manifest.mjs'
import { verifyRelease } from '../verify.mjs'
import { sealTree, syncDirectory } from '../copy.mjs'
import { acquireLock } from '../vault.mjs'

const key = (config, id, name) => config.prefix + config.repositoryId + '/' + id + '/' + name

export async function publishCos(config, id, context) {
  ensure(typeof context.authorize === 'function', 'DEPLOY_AUTHORIZATION_REQUIRED')
  const summary = await verifyRelease(config, id, context.signal)
  const root = await safeTarget(config.vaultRoot, id)
  const { data, value } = await readJson(await safeTarget(root, 'manifest.json'), metadataLimit)
  const manifest = normalizeManifest(value)
  ensure(releaseId(manifest) === id, 'COS_MANIFEST_INVALID')
  await context.store.privacy()
  for (const item of manifest.artifacts) {
    ensure(!context.signal?.aborted, 'INTERRUPTED')
    const file = await safeTarget(root, 'artifacts/' + item.path)
    const handle = await fs.open(file, constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK)
    try {
      const stat = await handle.stat()
      ensure(stat.isFile() && stat.nlink === 1 && stat.size === item.bytes, 'ARTIFACT_CHANGED')
      const stream = handle.createReadStream({ autoClose: false })
      try { await context.store.put(key(config, id, 'artifacts/' + item.path), stream, item.bytes, context.authorize) }
      finally { stream.destroy() }
      await context.store.read(key(config, id, 'artifacts/' + item.path), item)
    }
    finally { await handle.close() }
  }
  await verifyRelease(config, id, context.signal)
  // 清单最后发布，相当于提交标记；中断留下的半包不能取回。
  await context.store.put(key(config, id, 'manifest.json'), data, data.length, context.authorize)
  await context.store.read(key(config, id, 'manifest.json'), { bytes: data.length, sha256: sha256(data) })
  await context.store.privacy()
  return { ...summary, published: true, cosReadbackVerified: true, deployed: false,
    artifactManifestSha256: manifest.artifactManifestSha256 }
}

export async function fetchCos(config, id, context) {
  const unlock = await acquireLock(config.vaultRoot)
  try { return await fetchSnapshot(config, id, context) }
  finally { await unlock() }
}
async function fetchSnapshot(config, id, context) {
  ensure(/^rel-[a-f0-9]{64}$/.test(id), 'RELEASE_ID_INVALID')
  const existing = await statOptional(await safeTarget(config.vaultRoot, id))
  if (existing) return verifyRelease(config, id, context.signal)
  await context.store.privacy()
  // manifest允许不同长度，以有界sink获取后再检查内容寻址；head长度不作为摘要证据。
  const head = await context.store.call('headObject', context.store.params(key(config, id, 'manifest.json')))
  const bytes = Number(head.headers?.['content-length'])
  ensure(Number.isSafeInteger(bytes) && bytes > 0 && bytes <= metadataLimit, 'COS_MANIFEST_SIZE_INVALID')
  const received = await context.store.read(key(config, id, 'manifest.json'),
    { bytes, sha256: id.slice(4), capture: true })
  const manifest = normalizeManifest(parseStrictJson(received.data.toString('utf8'), metadataLimit))
  ensure(releaseId(manifest) === id && manifest.repositoryId === config.repositoryId
    && received.data.toString('utf8') === serialize(manifest), 'COS_MANIFEST_INVALID')
  const staging = await fs.mkdtemp(path.join(config.vaultRoot, '.pending-cos-'))
  await fs.chmod(staging, 0o700)
  const artifacts = path.join(staging, 'artifacts')
  await fs.mkdir(artifacts, { mode: 0o700 })
  for (const item of manifest.artifacts) {
    ensure(!context.signal?.aborted, 'INTERRUPTED')
    const file = await safeTarget(artifacts, item.path)
    await fs.mkdir(path.dirname(file), { recursive: true, mode: 0o700 })
    const handle = await fs.open(file, 'wx', 0o600)
    try {
      await context.store.read(key(config, id, 'artifacts/' + item.path), item, chunk => write(handle, chunk))
      await handle.sync()
    }
    finally { await handle.close() }
  }
  const actual = await collectArtifacts(artifacts, targetProfile(manifest), context.signal)
  ensure(JSON.stringify(actual) === JSON.stringify(manifest.artifacts), 'COS_ARTIFACT_INVALID')
  await writePrivate(staging, 'manifest.json', received.data)
  const receipt = { protocolVersion: 1, releaseId: id, manifestSha256: id.slice(4),
    sealedAt: new Date().toISOString(), localReviewDeclared: true,
    liveTaskApprovalVerified: false, deployed: false }
  await writePrivate(staging, 'receipt.json', serialize(receipt))
  await sealTree(staging)
  const final = await safeTarget(config.vaultRoot, id)
  ensure(!await statOptional(final), 'RELEASE_EXISTS')
  await fs.chmod(staging, 0o700)
  await fs.rename(staging, final)
  await fs.chmod(final, 0o500)
  await syncDirectory(config.vaultRoot)
  return verifyRelease(config, id, context.signal)
}
async function write(handle, chunk) {
  let offset = 0
  while (offset < chunk.length) {
    const { bytesWritten } = await handle.write(chunk, offset, chunk.length - offset)
    ensure(bytesWritten > 0, 'COS_FILE_WRITE_FAILED')
    offset += bytesWritten
  }
}
