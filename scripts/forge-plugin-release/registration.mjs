import { safeTarget } from '../forge-plugin/paths.mjs'
import { reportResult } from '../forge-plugin-builder/worker-result.mjs'
import { sha256 } from '../forge-plugin-builder/files.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'
import { digestPattern, exactKeys, privateDirectory, readJson } from './config.mjs'
import { uuidPattern } from './approval-config.mjs'
import { metadataLimit, normalizeManifest, releaseId } from './manifest.mjs'
import { verifyRelease } from './verify.mjs'

// 只读导出给管理员人工导入；没有凭证/网络调用，不能代替登记事务中的当前审批检查。
export async function readRegistrationConfig(file) {
  const { value } = await readJson(file)
  exactKeys(value, ['protocolVersion', 'repositoryId', 'vaultRoot', 'taskId', 'revision', 'reviewId',
    'serverResultSha256'], 'REGISTRATION_CONFIG_INVALID')
  ensure(value.protocolVersion === 1 && typeof value.repositoryId === 'string'
    && /^[a-z][a-z0-9-]{0,63}$/.test(value.repositoryId)
    && typeof value.taskId === 'string' && uuidPattern.test(value.taskId)
    && typeof value.reviewId === 'string' && uuidPattern.test(value.reviewId)
    && Number.isSafeInteger(value.revision) && value.revision >= 1 && value.revision <= 2147483647
    && typeof value.serverResultSha256 === 'string' && digestPattern.test(value.serverResultSha256),
  'REGISTRATION_CONFIG_INVALID')
  return { ...value, vaultRoot: await privateDirectory(value.vaultRoot) }
}

export async function prepareRegistration(config, id, signal) {
  await verifyRelease(config, id, signal)
  const root = await safeTarget(config.vaultRoot, id)
  const { data, value } = await readJson(await safeTarget(root, 'manifest.json'), metadataLimit)
  const manifest = normalizeManifest(value)
  ensure(sha256(data) === id.slice(4) && releaseId(manifest) === id, 'SNAPSHOT_MANIFEST_MISMATCH')
  const result = reportResult({ status: 'built', deployed: false, jobId: manifest.jobId,
    source: { sha256: manifest.source.sha256 }, artifacts: manifest.artifacts },
  { packageSha256: manifest.packageSha256, commit: manifest.source.commit, image: manifest.image })
  const metadata = { protocolVersion: 1, taskId: config.taskId, reviewId: config.reviewId,
    revision: config.revision, serverResultSha256: config.serverResultSha256,
    releaseId: id, manifestSha256: id.slice(4), repositoryId: manifest.repositoryId,
    resultSha256: manifest.resultSha256, pluginId: manifest.plugin.id, pluginVersion: manifest.plugin.version,
    coreVersion: manifest.plugin.coreVersion, operation: manifest.plugin.operation, result }
  ensure(Buffer.byteLength(JSON.stringify(metadata)) <= 65536, 'REGISTRATION_METADATA_LIMIT')
  await verifyRelease(config, id, signal)
  ensure(!signal?.aborted, 'INTERRUPTED')
  return metadata
}
