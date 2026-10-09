import { ensure, BuildError } from '../../forge-plugin-builder/errors.mjs'
import { writePrivate } from '../../forge-plugin-builder/files.mjs'
import { safeTarget, statOptional } from '../../forge-plugin/paths.mjs'
import { readJson } from '../config.mjs'
import { normalizeManifest, metadataLimit } from '../manifest.mjs'
import { verifyRelease } from '../verify.mjs'
import { prepareCompose, verifyCompose } from '../compose/prepare.mjs'
import { PrivateCosStore, cosClient } from '../cos/store.mjs'
import { fetchCos, publishCos } from '../cos/transfer.mjs'
import { session, targetLock } from './session.mjs'
import { verifyRuntime } from './runtime.mjs'
import { verifyTarget, assertCurrent, switchCompose, verifyContainers } from './docker.mjs'

export async function executeDelivery(config, id, options = {}, dependencies = {}) {
  ensure(options.reviewed === true && /^[a-f0-9-]{36}$/.test(id), 'DELIVERY_REVIEW_REQUIRED')
  const controller = new AbortController()
  const abort = () => controller.abort()
  options.signal?.addEventListener('abort', abort, { once: true })
  const context = { signal: controller.signal, switched: false, cosVerified: false, runtimeVerified: false }
  const { lease, request } = session(config, id, controller.signal, dependencies.request)
  const unlock = await targetLock(config, id)
  let claim, timer, heartbeat, finished = false, releaseLock = false
  try {
    claim = await request('claim')
    validateClaim(claim, config, id, lease)
    context.authorize = async () => {
      ensure(!context.signal.aborted, 'INTERRUPTED')
      const value = await request('authorize')
      validateClaim(value, config, id, lease)
      ensure(value.releaseId === claim.releaseId && value.metadata.manifestSha256 === claim.metadata.manifestSha256
        && value.previousReleaseId === claim.previousReleaseId
        && value.unverifiedReleaseId === claim.unverifiedReleaseId, 'DELIVERY_APPROVAL_CHANGED')
    }
    timer = setInterval(() => {
      if (heartbeat) return
      heartbeat = context.authorize().catch(() => controller.abort()).finally(() => { heartbeat = null })
    }, 30000)
    context.store = dependencies.store || new PrivateCosStore(config.cos, await cosClient())
    const result = await perform(config, claim, context, dependencies)
    clearInterval(timer)
    await heartbeat
    ensure(!context.signal.aborted, 'INTERRUPTED')
    const receipt = await request('finish', finish(claim, context, 'succeeded'))
    ensure(receipt.id === id && receipt.status === 'succeeded', 'DELIVERY_FINISH_UNCONFIRMED')
    finished = true
    releaseLock = true
    await writePrivate(config.stateRoot, id + '.json', JSON.stringify({ id, targetId: config.targetId,
      releaseId: claim.releaseId, status: 'succeeded', deployed: claim.action !== 'publish',
      cosVerified: true, runtimeVerified: context.runtimeVerified }))
    return result
  }
  catch (failure) {
    clearInterval(timer)
    controller.abort()
    await heartbeat
    const code = /^[A-Z][A-Z0-9_]{0,95}$/.test(failure.code || '') ? failure.code : 'DELIVERY_FAILED'
    // 已确认成功之后本地审计写失败，不得把控制面终态改写成unknown。
    if (claim && !finished) {
      try {
        // Compose发起过切换后任何失败都为unknown；不自动执行down或假设旧版本还在。
        const status = context.switched ? 'uncertain' : 'failed'
        const receipt = await request('finish', finish(claim, context, status, code))
        finished = receipt.id === id && receipt.status === status
        releaseLock = finished && status === 'failed'
      }
      catch { finished = false }
    }
    throw new BuildError(finished ? code : 'DELIVERY_RESULT_UNCONFIRMED')
  }
  finally {
    clearInterval(timer)
    options.signal?.removeEventListener('abort', abort)
    if (releaseLock) await unlock()
    // unknown即使已收到回执也保留本地锁；人工关闭后通过recover-lock认证归档。
  }
}

async function perform(config, claim, context, dependencies) {
  if (claim.action === 'publish') {
    await boundManifest(config, claim, context.signal)
    const result = await publishCos(config.cos, claim.releaseId, context)
    context.cosVerified = true
    return result
  }
  await fetchCos(config.cos, claim.releaseId, context)
  const manifest = await boundManifest(config, claim, context.signal)
  // 即使本地已有制品，部署前仍必须读取COS实际字节，不能拿旧发布回执代替。
  await publishCosReadback(config, claim.releaseId, manifest, context)
  context.cosVerified = true
  const output = await safeTarget(config.compose.outputRoot, claim.releaseId)
  if (!await statOptional(output)) await prepareCompose(config.compose, claim.releaseId, { reviewed: true })
  await verifyCompose(config.compose, claim.releaseId, context.signal)
  const client = await verifyTarget(config, manifest, dependencies.execute)
  await assertCurrent(client, config, claim, manifest)
  await context.authorize()
  await switchCompose(client, config, claim.releaseId, context)
  await verifyContainers(client, config, claim.releaseId, manifest)
  await verifyRuntime(config.compose, manifest, { fetch: dependencies.fetch, signal: context.signal })
  context.runtimeVerified = true
  return { releaseId: claim.releaseId, deployed: true, runtimeVerified: true }
}
async function boundManifest(config, claim, signal) {
  await verifyRelease(config.cos, claim.releaseId, signal)
  const { value } = await readJson(await safeTarget(config.cos.vaultRoot, claim.releaseId + '/manifest.json'),
    metadataLimit)
  const manifest = normalizeManifest(value)
  const metadata = claim.metadata
  ensure(manifest.plugin.id === metadata.pluginId && manifest.plugin.version === metadata.pluginVersion
    && manifest.plugin.coreVersion === metadata.coreVersion && manifest.packageSha256 === metadata.result.packageSha256
    && manifest.source.commit === metadata.result.sourceCommit && manifest.image === metadata.result.image
    && manifest.source.sha256 === metadata.result.sourceSha256
    && manifest.artifactManifestSha256 === metadata.result.artifactManifestSha256
    && manifest.resultSha256 === metadata.resultSha256 && manifest.jobId === metadata.result.jobId
    && manifest.artifacts.length === metadata.result.artifactCount
    && manifest.artifacts.reduce((sum, item) => sum + item.bytes, 0) === metadata.result.artifactBytes,
  'DELIVERY_MANIFEST_BINDING_MISMATCH')
  return manifest
}
async function publishCosReadback(config, id, manifest, context) {
  await context.store.privacy()
  const prefix = config.cos.prefix + config.cos.repositoryId + '/' + id + '/'
  for (const item of manifest.artifacts) {
    ensure(!context.signal.aborted, 'INTERRUPTED')
    await context.store.read(prefix + 'artifacts/' + item.path, item)
  }
  const { data } = await readJson(await safeTarget(config.cos.vaultRoot, id + '/manifest.json'), metadataLimit)
  await context.store.read(prefix + 'manifest.json', { bytes: data.length, sha256: id.slice(4) })
}
function validateClaim(value, config, id, lease) {
  ensure(value?.id === id && value.targetId === config.targetId && value.status === 'running'
    && value.workerId === config.workerId && value.lease === lease
    && ['publish', 'deploy', 'restore'].includes(value.action)
    && value.metadata?.repositoryId === config.cos.repositoryId && value.releaseId === value.metadata.releaseId
    && value.releaseId === 'rel-' + value.metadata.manifestSha256, 'DELIVERY_CLAIM_INVALID')
}
function finish(claim, context, status, failureCode) {
  return { status, artifactManifestSha256: claim.metadata.result.artifactManifestSha256,
    cosVerified: context.cosVerified, runtimeVerified: status === 'succeeded' && context.runtimeVerified,
    failureCode: failureCode || null }
}
