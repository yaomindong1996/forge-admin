import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store/modules/auth'
import { request } from '@/utils'
import { generateUUID } from '@/utils/common'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''
const ENCRYPTED_REQUEST = { encrypt: true }
const PUBLISH_CHECK_TIMEOUT = 60_000
const PUBLISH_EXECUTION_TIMEOUT = 120_000
const DRAFT_SAVE_TIMEOUT = 60_000

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

export function businessApplicationDetailBySlug(portalSlug) {
  return request.get(`/ai/business/application/by-slug/${encodeURIComponent(portalSlug)}`, ENCRYPTED_REQUEST)
}

export function checkBusinessApplicationSlugAvailable(portalSlug, excludeId) {
  return request.get('/ai/business/application/slug-available', encryptedParams({
    portalSlug,
    excludeId,
  }))
}

export function saveBusinessApplicationPortalConfig(id, config) {
  return request.put(`/ai/business/application/${id}/portal-config`, config || {}, ENCRYPTED_REQUEST)
}

export function saveBusinessApplicationAiAssistantConfig(id, config) {
  return request.put(`/ai/business/application/${id}/ai-assistant-config`, {
    aiAssistantConfig: config || {},
  }, ENCRYPTED_REQUEST)
}

export function businessApplicationAiAssistantStatus(id) {
  return request.get(`/ai/business/application/${id}/ai-assistant-status`, ENCRYPTED_REQUEST)
}

export function chatBusinessApplicationAssistant(applicationCodeOrSlug, data) {
  return request.post(
    `/ai/business/application/portal/${encodeURIComponent(applicationCodeOrSlug)}/assistant/chat`,
    data,
    ENCRYPTED_REQUEST,
  )
}

export function distributeBusinessApplicationToWorkbench(id, data) {
  return request.post(`/ai/business/application/${id}/distribute/workbench`, data || {}, ENCRYPTED_REQUEST)
}

export function businessApplicationWorkbench() {
  // needTip:false：工作台首页必调，无 portal 权限（如社区角色）时静默降级为空列表，
  // 避免全局 403 错误弹窗轰炸首页；调用方（home/index.vue）已自带 catch 兑底
  return request.get('/ai/business/application/workbench', { encrypt: true, needTip: false })
}

export function createBusinessApplication(data) {
  return request.post('/ai/business/application', data, ENCRYPTED_REQUEST)
}

export function updateBusinessApplication(data) {
  return request.put('/ai/business/application', data, {
    ...ENCRYPTED_REQUEST,
    timeout: DRAFT_SAVE_TIMEOUT,
  })
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
  return request.put(`/ai/business/application/${id}/objects`, data || [], {
    ...ENCRYPTED_REQUEST,
    timeout: DRAFT_SAVE_TIMEOUT,
  })
}

export function provisionBusinessApplicationFormData(id, data) {
  return request.post(`/ai/business/application/${id}/form-data/provision`, data, {
    ...ENCRYPTED_REQUEST,
    timeout: DRAFT_SAVE_TIMEOUT,
  })
}

export function designBusinessApplicationPage(id, data) {
  return request.post(`/ai/business/application/${id}/design-page`, data, ENCRYPTED_REQUEST)
}

export function initializeBusinessApplicationTemplate(id, data) {
  return request.post(`/ai/business/application/${id}/initialize-template`, data, ENCRYPTED_REQUEST)
}

export function initializeBusinessApplicationAi(id, data) {
  return request.post(`/ai/business/application/${id}/initialize-ai`, data, ENCRYPTED_REQUEST)
}

export function previewBusinessApplicationExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    method: 'post',
    url: '/ai/business/application/excel/preview',
    data: formData,
    encrypt: false,
  })
}

export function initializeBusinessApplicationExcel(id, file, config = {}) {
  const formData = new FormData()
  formData.append('file', file)
  if (config.objectName)
    formData.append('objectName', config.objectName)
  if (config.objectCode)
    formData.append('objectCode', config.objectCode)
  formData.append('fields', JSON.stringify(config.fields || []))
  return request({
    method: 'post',
    url: `/ai/business/application/${id}/import-excel`,
    data: formData,
    encrypt: false,
  })
}

/** 查询可导出的应用页面（应用配置导出） */
export function listBusinessApplicationDebugBundlePages(id) {
  return request.get(`/ai/business/application/${id}/debug-bundle/pages`, ENCRYPTED_REQUEST)
}

/** 导出应用配置（设计态 JSON，不含业务数据）；pageIds 为空则整应用 */
export function exportBusinessApplicationDebugBundle(id, pageIds = []) {
  const params = {}
  if (Array.isArray(pageIds) && pageIds.length)
    params.pageIds = pageIds.join(',')
  return request.get(`/ai/business/application/${id}/debug-bundle/export`, {
    encrypt: false,
    responseType: 'blob',
    rawResponse: true,
    // 避免拦截器把 application/json 附件当成 RespInfo 解析掉
    preserveBlob: true,
    timeout: 120_000,
    params,
  })
}

/** 导入应用配置，重建应用/对象/表结构；autoPublish=true 时导入后尝试发布 */
export function importBusinessApplicationDebugBundle(file, autoPublish = false) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('autoPublish', autoPublish ? 'true' : 'false')
  return request({
    method: 'post',
    url: '/ai/business/application/debug-bundle/import',
    data: formData,
    encrypt: false,
    timeout: 180_000,
  })
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

export function businessApplicationRuntimeByCode(applicationCode) {
  return request.get(
    `/ai/business/application/by-code/${encodeURIComponent(applicationCode)}/runtime`,
    ENCRYPTED_REQUEST,
  )
}

export function businessApplicationRuntimeByCodeOrSlug(identifier) {
  return request.get(
    `/ai/business/application/portal/${encodeURIComponent(identifier)}/runtime`,
    ENCRYPTED_REQUEST,
  )
}

export function businessApplicationPermissionWorkspace(applicationCode) {
  return request.get(
    `/ai/business/application/by-code/${encodeURIComponent(applicationCode)}/permissions`,
    ENCRYPTED_REQUEST,
  )
}

export function businessApplicationRolePermission(applicationCode, roleId) {
  return request.get(
    `/ai/business/application/by-code/${encodeURIComponent(applicationCode)}/permissions/roles/${roleId}`,
    ENCRYPTED_REQUEST,
  )
}

export function saveBusinessApplicationRolePermission(applicationCode, roleId, data) {
  return request.put(
    `/ai/business/application/by-code/${encodeURIComponent(applicationCode)}/permissions/roles/${roleId}`,
    data,
    ENCRYPTED_REQUEST,
  )
}

export function saveBusinessApplicationDataScopeAdapter(applicationCode, objectId, data) {
  return request.put(
    `/ai/business/application/by-code/${encodeURIComponent(applicationCode)}/permissions/objects/${objectId}/data-scope-adapter`,
    data,
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
