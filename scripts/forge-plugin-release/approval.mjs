import { randomUUID } from 'node:crypto'
import { safeTarget } from '../forge-plugin/paths.mjs'
import { workerRequest } from '../forge-plugin-builder/worker-http.mjs'
import { reportResult } from '../forge-plugin-builder/worker-result.mjs'
import { sha256 } from '../forge-plugin-builder/files.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'
import { exactKeys, readJson } from './config.mjs'
import { metadataLimit, normalizeManifest, releaseId } from './manifest.mjs'
import { verifyRelease } from './verify.mjs'

// 两次文件复验夹住网络往返；只输出时点核验，不改只读回执，也不登记/发布/部署。
export async function verifyApproval(config, id, options = {}, dependencies = {}) {
  const { signal } = options
  const before = await verifyRelease(config, id, signal)
  const root = await safeTarget(config.vaultRoot, id)
  const { data, value } = await readJson(await safeTarget(root, 'manifest.json'), metadataLimit)
  const manifest = normalizeManifest(value)
  ensure(sha256(data) === id.slice(4) && releaseId(manifest) === id, 'SNAPSHOT_MANIFEST_MISMATCH')
  const token = process.env.FORGE_PLUGIN_WORKER_TOKEN
  ensure(typeof token === 'string' && /^[a-f0-9]{64}$/.test(token), 'WORKER_TOKEN_REQUIRED')
  const body = requestBody(config, manifest)
  const startedAt = Date.now()
  ensure(!signal?.aborted, 'INTERRUPTED')
  const call = dependencies.request || workerRequest
  const response = await call({ baseUrl: config.apiBaseUrl, taskId: config.taskId, token },
    'approval-check', body, { signal })
  validateResponse(response, body, config, startedAt)
  await verifyRelease(config, id, signal)
  ensure(!signal?.aborted, 'INTERRUPTED')
  return { ...before, status: 'approval_checked', liveTaskApprovalVerified: true,
    approval: { taskId: config.taskId, revision: config.revision, reviewId: config.reviewId,
      workerId: config.workerId, serverResultSha256: config.serverResultSha256, checkedAt: response.checkedAt,
      validOnlyAtCheck: true, deploymentAuthorized: false, serverArtifactBytesVerified: false },
    deploymentPreparation: before.deploymentPreparation.map(item => item.code === 'live_task_approval'
      ? { ...item, status: 'passed_at_check' } : item) }
}

function requestBody(config, manifest) {
  const result = reportResult({ status: 'built', deployed: false, jobId: manifest.jobId,
    source: { sha256: manifest.source.sha256 }, artifacts: manifest.artifacts },
  { packageSha256: manifest.packageSha256, commit: manifest.source.commit, image: manifest.image })
  return { checkId: randomUUID(), reviewId: config.reviewId, revision: config.revision,
    serverResultSha256: config.serverResultSha256, manifestSha256: releaseId(manifest).slice(4),
    pluginId: manifest.plugin.id, pluginVersion: manifest.plugin.version, coreVersion: manifest.plugin.coreVersion,
    operation: manifest.plugin.operation, result }
}

function validateResponse(value, body, config, startedAt) {
  exactKeys(value, ['protocolVersion', 'checkId', 'taskId', 'revision', 'reviewId', 'workerId',
    'serverResultSha256', 'manifestSha256', 'checkedAt', 'liveTaskApprovalVerified', 'deployed'],
  'APPROVAL_RESPONSE_INVALID')
  ensure(value.protocolVersion === 1 && value.checkId === body.checkId && value.taskId === config.taskId
    && value.revision === config.revision && value.reviewId === config.reviewId && value.workerId === config.workerId
    && value.serverResultSha256 === config.serverResultSha256 && value.manifestSha256 === body.manifestSha256
    && value.liveTaskApprovalVerified === true && value.deployed === false, 'APPROVAL_RESPONSE_MISMATCH')
  ensure(typeof value.checkedAt === 'string'
    && /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d{3})?Z$/.test(value.checkedAt), 'APPROVAL_TIME_INVALID')
  const checkedAt = Date.parse(value.checkedAt)
  ensure(Number.isFinite(checkedAt) && new Date(checkedAt).toISOString().replace('.000Z', 'Z')
    === value.checkedAt.replace('.000Z', 'Z') && checkedAt >= startedAt - 5000 && checkedAt <= Date.now() + 5000,
  'APPROVAL_TIME_INVALID')
}
