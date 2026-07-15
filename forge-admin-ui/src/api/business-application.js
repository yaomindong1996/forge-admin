import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store/modules/auth'
import { request } from '@/utils'
import { generateUUID } from '@/utils/common'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''
const ENCRYPTED_REQUEST = { encrypt: true }
const PUBLISH_CHECK_TIMEOUT = 60_000
const PUBLISH_EXECUTION_TIMEOUT = 120_000

function encryptedParams(params) {
  return { params, encrypt: true }
}

export function businessApplicationPage(params) {
  return request.get('/ai/business/application/page', encryptedParams(params))
}

export function businessApplicationList(params) {
  return request.get('/ai/business/application/list', encryptedParams(params))
}

export function businessApplicationDetail(id) {
  return request.get(`/ai/business/application/${id}`, ENCRYPTED_REQUEST)
}

export function businessApplicationDetailByCode(applicationCode) {
  return request.get(`/ai/business/application/by-code/${encodeURIComponent(applicationCode)}`, ENCRYPTED_REQUEST)
}

export function createBusinessApplication(data) {
  return request.post('/ai/business/application', data, ENCRYPTED_REQUEST)
}

export function updateBusinessApplication(data) {
  return request.put('/ai/business/application', data, ENCRYPTED_REQUEST)
}

export function updateBusinessApplicationStatus(id, status) {
  return request.put(`/ai/business/application/${id}/status`, null, encryptedParams({ status }))
}

export function deleteBusinessApplication(id) {
  return request.delete(`/ai/business/application/${id}`, ENCRYPTED_REQUEST)
}

export function businessApplicationObjects(id) {
  return request.get(`/ai/business/application/${id}/objects`, ENCRYPTED_REQUEST)
}

export function saveBusinessApplicationObjects(id, data) {
  return request.put(`/ai/business/application/${id}/objects`, data || [], ENCRYPTED_REQUEST)
}

export function initializeBusinessApplicationTemplate(id, data) {
  return request.post(`/ai/business/application/${id}/initialize-template`, data, ENCRYPTED_REQUEST)
}

export function businessApplicationWorkspace(id) {
  return request.get(`/ai/business/application/${id}/workspace`, ENCRYPTED_REQUEST)
}

export function businessApplicationWorkspaceByCode(applicationCode) {
  return request.get(
    `/ai/business/application/by-code/${encodeURIComponent(applicationCode)}/workspace`,
    ENCRYPTED_REQUEST,
  )
}

export function businessApplicationReadiness(id) {
  return request.get(`/ai/business/application/${id}/readiness`, ENCRYPTED_REQUEST)
}

export function businessApplicationCodeOptions(id) {
  return request.get(`/ai/business/application/${id}/code/options`, ENCRYPTED_REQUEST)
}

export function saveBusinessApplicationCodeOptions(id, data) {
  return request.put(`/ai/business/application/${id}/code/options`, data, ENCRYPTED_REQUEST)
}

export function previewBusinessApplicationCode(id, params) {
  const query = {
    ...(params || {}),
    objectIds: Array.isArray(params?.objectIds) ? params.objectIds.join(',') : params?.objectIds,
    stripTablePrefixes: Array.isArray(params?.stripTablePrefixes)
      ? params.stripTablePrefixes.join(',')
      : params?.stripTablePrefixes,
  }
  return request.get(`/ai/business/application/${id}/code/preview`, encryptedParams(query))
}

export async function downloadBusinessApplicationCode(id, params = {}) {
  const authStore = useAuthStore()
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      if (value.length || key === 'stripTablePrefixes')
        search.append(key, value.join(','))
      return
    }
    if (value !== undefined && value !== null && (value !== '' || key === 'entityPrefix'))
      search.append(key, value)
  })
  const query = search.toString()
  const response = await managedFetch(
    `${BASE_URL}/ai/business/application/${id}/code/download${query ? `?${query}` : ''}`,
    {
      method: 'GET',
      headers: {
        'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': generateUUID(),
      },
    },
    {
      globalLoadingType: 'download',
      globalLoadingText: '正在打包应用代码，请稍候...',
    },
  )
  if (!response.ok)
    throw new Error(await response.text() || response.statusText)
  return response.blob()
}

export function checkBusinessApplicationPublish(id, data = {}) {
  return request.post(`/ai/business/application/${id}/publish/check`, data, {
    ...ENCRYPTED_REQUEST,
    timeout: PUBLISH_CHECK_TIMEOUT,
  })
}

export function publishBusinessApplication(id, data, idempotencyKey) {
  return request.post(`/ai/business/application/${id}/publish`, data || {}, {
    ...ENCRYPTED_REQUEST,
    headers: { 'Idempotency-Key': idempotencyKey },
    timeout: PUBLISH_EXECUTION_TIMEOUT,
    needTip: false,
    globalLoading: true,
    globalLoadingDelay: 0,
    globalLoadingText: '正在执行发布检查并发布应用，请稍候...',
  })
}

export function businessApplicationVersions(id) {
  return request.get(`/ai/business/application/${id}/versions`, ENCRYPTED_REQUEST)
}

export function businessApplicationVersionDetail(id, versionNo) {
  return request.get(`/ai/business/application/${id}/versions/${versionNo}`, ENCRYPTED_REQUEST)
}

export function businessApplicationPublishRuns(id) {
  return request.get(`/ai/business/application/${id}/publish-runs`, ENCRYPTED_REQUEST)
}

export function recoverBusinessApplicationPublish(id, runId) {
  return request.post(`/ai/business/application/${id}/publish-runs/${runId}/recover`, {}, {
    ...ENCRYPTED_REQUEST,
    timeout: PUBLISH_EXECUTION_TIMEOUT,
  })
}

export function rollbackBusinessApplication(id, versionNo, data, idempotencyKey) {
  return request.post(`/ai/business/application/${id}/versions/${versionNo}/rollback`, data || {}, {
    ...ENCRYPTED_REQUEST,
    headers: { 'Idempotency-Key': idempotencyKey },
    timeout: PUBLISH_EXECUTION_TIMEOUT,
  })
}

export function businessObjectTableMapping(objectId) {
  return request.get(`/ai/business/object/${objectId}/table-mapping`, ENCRYPTED_REQUEST)
}

export function previewBusinessObjectDatabaseDiff(objectId, designVersion) {
  return request.post(`/ai/business/object/${objectId}/database-diff`, { designVersion }, ENCRYPTED_REQUEST)
}

export function syncBusinessObjectDatabase(objectId, designVersion) {
  return request.post(`/ai/business/object/${objectId}/database-sync`, {
    designVersion,
    confirmOnlineDdl: true,
  }, ENCRYPTED_REQUEST)
}
