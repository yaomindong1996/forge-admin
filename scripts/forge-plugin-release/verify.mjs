import fs from 'node:fs/promises'
import { safeTarget } from '../forge-plugin/paths.mjs'
import { collectArtifacts } from '../forge-plugin-builder/artifacts.mjs'
import { sha256 } from '../forge-plugin-builder/files.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'
import { exactKeys, readJson, releasePattern } from './config.mjs'
import { metadataLimit, normalizeManifest, releaseId, serialize, targetProfile } from './manifest.mjs'

export async function verifyRelease(config, id, signal) {
  ensure(typeof id === 'string' && releasePattern.test(id), 'RELEASE_ID_INVALID')
  return verifySnapshot(await safeTarget(config.vaultRoot, id), id, config.repositoryId, signal)
}

export async function verifySnapshot(root, id, repositoryId, signal) {
  ensure(!signal?.aborted, 'INTERRUPTED')
  const entries = await fs.readdir(root)
  ensure(entries.length === 3 && entries.every(name => ['artifacts', 'manifest.json', 'receipt.json'].includes(name)),
    'SNAPSHOT_CONTENT_INVALID')
  const { value, data } = await readJson(await safeTarget(root, 'manifest.json'), metadataLimit)
  const manifest = normalizeManifest(value)
  ensure(releaseId(manifest) === id && sha256(data) === id.slice(4)
    && data.toString('utf8') === serialize(manifest) && manifest.repositoryId === repositoryId,
  'SNAPSHOT_MANIFEST_MISMATCH')
  await verifyReceipt(root, id)
  const actual = await collectArtifacts(await safeTarget(root, 'artifacts'), targetProfile(manifest), signal)
  ensure(JSON.stringify(actual) === JSON.stringify(manifest.artifacts), 'SNAPSHOT_ARTIFACT_MISMATCH')
  await verifyPermissions(root)
  ensure(!signal?.aborted, 'INTERRUPTED')
  return summary(manifest, 'verified')
}

async function verifyReceipt(root, id) {
  const { value } = await readJson(await safeTarget(root, 'receipt.json'))
  exactKeys(value, ['protocolVersion', 'releaseId', 'manifestSha256', 'sealedAt', 'localReviewDeclared',
    'liveTaskApprovalVerified', 'deployed'])
  ensure(value.protocolVersion === 1 && value.releaseId === id && value.manifestSha256 === id.slice(4)
    && value.localReviewDeclared === true && value.liveTaskApprovalVerified === false && value.deployed === false
    && typeof value.sealedAt === 'string' && /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z$/.test(value.sealedAt)
    && new Date(value.sealedAt).toISOString() === value.sealedAt, 'SNAPSHOT_RECEIPT_INVALID')
}

async function verifyPermissions(root) {
  const stat = await fs.lstat(root)
  ensure(stat.isDirectory() && !stat.isSymbolicLink() && stat.uid === process.getuid()
    && (stat.mode & 0o777) === 0o500, 'SNAPSHOT_PERMISSION_INVALID')
  for (const name of await fs.readdir(root)) {
    const target = await safeTarget(root, name)
    const item = await fs.lstat(target)
    if (item.isDirectory()) await verifyPermissions(target)
    else ensure(item.isFile() && item.nlink === 1 && item.uid === process.getuid()
      && (item.mode & 0o777) === 0o400, 'SNAPSHOT_PERMISSION_INVALID')
  }
}

// 固定待办项不是目标环境检测结果；离线核验不能代替当前任务授权或部署验收。
export function summary(manifest, status) {
  return { protocolVersion: 1, status, releaseId: releaseId(manifest), repositoryId: manifest.repositoryId,
    jobId: manifest.jobId, pluginId: manifest.plugin.id, version: manifest.plugin.version,
    manifestSha256: releaseId(manifest).slice(4), artifactManifestSha256: manifest.artifactManifestSha256,
    artifactCount: manifest.artifacts.length,
    artifactBytes: manifest.artifacts.reduce((sum, file) => sum + file.bytes, 0),
    localReviewDeclared: status !== 'checked', liveTaskApprovalVerified: false, deployed: false,
    deploymentPreparation: [
      { code: 'artifact_integrity', status: 'passed' },
      { code: 'live_task_approval', status: 'pending' },
      { code: 'target_and_permissions', status: 'pending' },
      { code: 'backup_and_restore', status: 'pending' },
      { code: 'migrations_and_resources', status: 'pending' },
      { code: 'deploy_and_runtime_health', status: 'pending' },
    ] }
}
