import { apiBase } from '../forge-plugin-builder/worker-http.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'
import { digestPattern, exactKeys, privateDirectory, readJson } from './config.mjs'

export const uuidPattern = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/
export const workerPattern = /^[a-z0-9][a-z0-9_-]{0,63}$/

export async function readApprovalConfig(file) {
  const { value } = await readJson(file)
  exactKeys(value, ['protocolVersion', 'repositoryId', 'vaultRoot', 'apiBaseUrl', 'taskId', 'revision',
    'reviewId', 'serverResultSha256', 'workerId'], 'APPROVAL_CONFIG_INVALID')
  ensure(value.protocolVersion === 1 && typeof value.repositoryId === 'string'
    && /^[a-z][a-z0-9-]{0,63}$/.test(value.repositoryId), 'APPROVAL_CONFIG_INVALID')
  ensure(typeof value.taskId === 'string' && uuidPattern.test(value.taskId)
    && typeof value.reviewId === 'string' && uuidPattern.test(value.reviewId)
    && Number.isSafeInteger(value.revision) && value.revision >= 1 && value.revision <= 2147483647
    && typeof value.workerId === 'string' && workerPattern.test(value.workerId)
    && typeof value.serverResultSha256 === 'string' && digestPattern.test(value.serverResultSha256),
  'APPROVAL_BINDING_INVALID')
  return { ...value, apiBaseUrl: apiBase(value.apiBaseUrl), vaultRoot: await privateDirectory(value.vaultRoot) }
}
