import { normalizeRecordSelectorConfig } from '@/components/ai-form/record-selector-utils'
import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store/modules/auth'
import { request } from '@/utils'
import { generateUUID } from '@/utils/common'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''
const ENCRYPTED_REQUEST = { encrypt: true }

function encryptedParams(params) {
  return { params, encrypt: true }
}

export function businessSuitePage(params) {
  return request.get('/ai/business/suite/page', encryptedParams(params))
}

export function businessSuiteList(params) {
  return request.get('/ai/business/suite/list', encryptedParams(params))
}

export function businessSuiteSummary(params) {
  return request.get('/ai/business/suite/summary', encryptedParams(params))
}

export function businessSuiteDetail(id) {
  return request.get(`/ai/business/suite/${id}`, ENCRYPTED_REQUEST)
}

export function createBusinessSuite(data) {
  return request.post('/ai/business/suite', data, ENCRYPTED_REQUEST)
}

export function updateBusinessSuite(data) {
  return request.put('/ai/business/suite', data, ENCRYPTED_REQUEST)
}

export function updateBusinessSuiteStatus(id, status) {
  return request.put(`/ai/business/suite/${id}/status`, null, encryptedParams({ status }))
}

export function deleteBusinessSuite(id) {
  return request.delete(`/ai/business/suite/${id}`, ENCRYPTED_REQUEST)
}

export function syncLowcodeDomains() {
  return request.post('/ai/business/suite/sync-lowcode-domains', null, ENCRYPTED_REQUEST)
}

export function businessObjectPage(params) {
  return request.get('/ai/business/object/page', encryptedParams(params))
}

export function businessObjectList(params) {
  return request.get('/ai/business/object/list', encryptedParams(params))
}

export function businessObjectDetail(id) {
  return request.get(`/ai/business/object/${id}`, ENCRYPTED_REQUEST)
}

export function createBusinessObject(data) {
  return request.post('/ai/business/object', data, ENCRYPTED_REQUEST)
}

export function genDatasourceEnabled(usageScope) {
  return request.get('/generator/datasource/enabled', {
    params: usageScope ? { usageScope } : undefined,
  })
}

export function genDatasourceTables(datasourceId) {
  return request.get(`/generator/datasource/${datasourceId}/tables`)
}

export function genDatasourceTableColumns(datasourceId, tableName) {
  return request.get(`/generator/datasource/${datasourceId}/tables/${encodeURIComponent(tableName)}/columns`)
}

export function updateBusinessObject(data) {
  return request.put('/ai/business/object', data, ENCRYPTED_REQUEST)
}

export function updateBusinessObjectStatus(id, status) {
  return request.put(`/ai/business/object/${id}/status`, null, encryptedParams({ status }))
}

export function deleteBusinessObject(id) {
  return request.delete(`/ai/business/object/${id}`, ENCRYPTED_REQUEST)
}

export function businessObjectRuntimeInfo(id) {
  return request.get(`/ai/business/object/${id}/runtime-info`, ENCRYPTED_REQUEST)
}

export function businessObjectRelations(objectId) {
  return request.get(`/ai/business/object/${objectId}/relations`, ENCRYPTED_REQUEST)
}

export function saveBusinessObjectRelations(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/relations`, data, ENCRYPTED_REQUEST)
}

export function syncLowcodeModels() {
  return request.post('/ai/business/object/sync-lowcode-models', null, ENCRYPTED_REQUEST)
}

export function businessAppPage(params) {
  return request.get('/ai/business/app/page', encryptedParams(params))
}

export function businessAppList(params) {
  return request.get('/ai/business/app/list', encryptedParams(params))
}

export function businessAppDetail(id) {
  return request.get(`/ai/business/app/${id}`, ENCRYPTED_REQUEST)
}

export function createBusinessApp(data) {
  return request.post('/ai/business/app', data, ENCRYPTED_REQUEST)
}

export function updateBusinessApp(data) {
  return request.put('/ai/business/app', data, ENCRYPTED_REQUEST)
}

export function updateBusinessAppStatus(id, status) {
  return request.put(`/ai/business/app/${id}/status`, null, encryptedParams({ status }))
}

export function deleteBusinessApp(id) {
  return request.delete(`/ai/business/app/${id}`, ENCRYPTED_REQUEST)
}

export function businessAppOpenInfo(id) {
  return request.get(`/ai/business/app/${id}/open-info`, ENCRYPTED_REQUEST)
}

export function syncPublishedCrudConfigs() {
  return syncPublishedApps()
}

export function syncPublishedApps() {
  return request.post('/ai/business/app/sync-published-apps', null, ENCRYPTED_REQUEST)
}

export function businessAppCodePreview(id, params) {
  const query = {
    ...(params || {}),
    stripTablePrefixes: Array.isArray(params?.stripTablePrefixes)
      ? params.stripTablePrefixes.join(',')
      : params?.stripTablePrefixes,
  }
  return request.get(`/ai/business/app/${id}/code/preview`, encryptedParams(query))
}

export function businessAppCodeOptions(id) {
  return request.get(`/ai/business/app/${id}/code/options`, ENCRYPTED_REQUEST)
}

export function businessSaveAppCodeOptions(id, data) {
  return request.put(`/ai/business/app/${id}/code/options`, data, ENCRYPTED_REQUEST)
}

export async function businessDownloadAppCode(id, params = {}) {
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
  const resp = await managedFetch(`${BASE_URL}/ai/business/app/${id}/code/download${query ? `?${query}` : ''}`, {
    method: 'GET',
    headers: {
      'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
      'X-Timestamp': Date.now().toString(),
      'X-Nonce': generateUUID(),
    },
  }, {
    globalLoadingType: 'download',
    globalLoadingText: '文件下载处理中，请稍候...',
  })
  if (!resp.ok)
    throw new Error(await resp.text() || resp.statusText)
  return resp.blob()
}

export function businessBindingList(params) {
  return request.get('/ai/business/binding/list', { params })
}

export function businessFlowModelBindings(modelKey) {
  return request.get(`/ai/business/flow/model/${modelKey}/business-bindings`)
}

export function createBusinessBinding(data) {
  return request.post('/ai/business/binding', data)
}

export function updateBusinessBinding(data) {
  return request.put('/ai/business/binding', data)
}

export function deleteBusinessBinding(id) {
  return request.delete(`/ai/business/binding/${id}`)
}

export function saveBusinessBindings(data) {
  return request.post('/ai/business/binding/batch-save', data)
}

export function dynamicCrudImportTemplate(configKey) {
  return request({
    method: 'get',
    url: `/ai/crud/${configKey}/import-template`,
    responseType: 'blob',
    rawResponse: true,
    encrypt: false,
  })
}

export function dynamicCrudExport(configKey, data = {}) {
  return request({
    method: 'post',
    url: `/ai/crud/${configKey}/export`,
    data,
    responseType: 'blob',
    rawResponse: true,
    encrypt: false,
  })
}

export function dynamicCrudImport(configKey, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    method: 'post',
    url: `/ai/crud/${configKey}/import`,
    data: formData,
    encrypt: false,
  })
}

export function businessObjectReadiness(id) {
  return request.get(`/ai/business/object/${id}/readiness`)
}

export function businessSuiteAcceptance(suiteCode) {
  return request.get(`/ai/business/suite/${suiteCode}/acceptance`)
}

export function businessObjectRelationRuntime(objectId) {
  return request.get(`/ai/business/object/${objectId}/relations/runtime`)
}

export function businessObjectDesigner(objectId) {
  return request.get(`/ai/business/object/${objectId}/designer`)
}

export function saveBusinessObjectDesigner(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/designer`, data || {}, { encrypt: true })
}

export function businessObjectFields(objectId) {
  return request.get(`/ai/business/object/${objectId}/fields`)
}

export function createBusinessObjectField(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/fields`, data || {}, { encrypt: true })
}

export function sortBusinessObjectFields(objectId, fieldCodes) {
  return request.put(`/ai/business/object/${objectId}/fields/sort`, fieldCodes || [], { encrypt: true })
}

export function updateBusinessObjectField(objectId, fieldCode, data) {
  return request.put(`/ai/business/object/${objectId}/fields/${fieldCode}`, data || {}, { encrypt: true })
}

export function deleteBusinessObjectField(objectId, fieldCode) {
  return request.delete(`/ai/business/object/${objectId}/fields/${fieldCode}`)
}

export function businessObjectLayout(objectId, layoutKey) {
  return request.get(`/ai/business/object/${objectId}/layout/${layoutKey}`)
}

export function saveBusinessObjectFormLayout(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/layout/form`, data || {}, { encrypt: true })
}

export function saveBusinessObjectListLayout(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/layout/list`, data || {}, { encrypt: true })
}

export function saveBusinessObjectDetailLayout(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/layout/detail`, data || {}, { encrypt: true })
}

export function previewBusinessObjectLayout(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/layout/preview`, data || {}, { encrypt: true })
}

export function businessObjectActions(objectId) {
  return request.get(`/ai/business/object/${objectId}/actions`)
}

export function saveBusinessObjectActions(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/actions`, data || [], { encrypt: true })
}

export function businessObjectPermissionSummary(objectId) {
  return request.get(`/ai/business/object/${objectId}/permission-summary`)
}

export function businessObjectPublishCheck(objectId) {
  return request.get(`/ai/business/object/${objectId}/publish-check`)
}

export function publishBusinessObject(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/publish`, data || {}, { encrypt: true })
}

export function businessObjectDesignVersions(objectId) {
  return request.get(`/ai/business/object/${objectId}/versions`)
}

export function rollbackBusinessObjectDesignVersion(objectId, versionId) {
  return request.post(`/ai/business/object/${objectId}/versions/${versionId}/rollback`)
}

// ==================== 触发器管理 ====================

export function businessTriggerPage(params) {
  return request.get('/ai/business/trigger/page', { params })
}

export function businessTriggerDetail(id) {
  return request.get(`/ai/business/trigger/${id}`)
}

export function createBusinessTrigger(data) {
  return request.post('/ai/business/trigger', data)
}

export function updateBusinessTrigger(data) {
  return request.put('/ai/business/trigger', data)
}

export function deleteBusinessTrigger(id) {
  return request.delete(`/ai/business/trigger/${id}`)
}

export function updateBusinessTriggerStatus(id, status) {
  return request.put(`/ai/business/trigger/${id}/status/${status}`)
}

export function businessTriggerLogs(params) {
  return request.get('/ai/business/trigger/logs', { params })
}

export function businessTriggerScenarioTemplates() {
  return request.get('/ai/business/trigger/scenario-templates')
}

// ==================== 通用业务动作 ====================

export function executeBusinessAction(data) {
  return request.post('/ai/business/action/execute', data || {})
}

export function previewBusinessAction(data) {
  return request.post('/ai/business/action/preview', data || {})
}

export function businessActionLogs(params) {
  return request.get('/ai/business/action/logs', { params })
}

// ==================== 通用记录选择器 ====================

export function queryBusinessRecordSelector(data, params = {}) {
  const payload = data && typeof data === 'object' ? { ...data } : {}
  const normalized = normalizeRecordSelectorConfig(payload)
  const objectCode = normalized.objectCode
  if (!objectCode) {
    console.error('[BusinessRecordSelector] 阻止缺少业务对象编码的查询', {
      payload,
      params,
      stack: new Error('BusinessRecordSelector missing objectCode').stack,
    })
    return Promise.resolve({ data: { records: [], columns: [], total: 0, current: 1, size: 0 } })
  }
  payload.objectCode = objectCode
  payload.businessObjectCode = payload.businessObjectCode || normalized.businessObjectCode || objectCode
  payload.targetObjectCode = payload.targetObjectCode || normalized.targetObjectCode || objectCode
  return request.post('/ai/business/selector/query', payload, { params })
}

// ==================== 通用数量台账查询 ====================

export function queryBusinessQuantityBalance(data) {
  return request.post('/ai/business/quantity/query/balance', data || {})
}

export function queryBusinessQuantityLedger(data) {
  return request.post('/ai/business/quantity/query/ledger', data || {})
}

export function queryBusinessQuantityLock(data) {
  return request.post('/ai/business/quantity/query/lock', data || {})
}

// ==================== 单据与流程 ====================

export function businessDocumentConfig(objectId) {
  return request.get(`/ai/business/document/config/${objectId}`)
}

export function saveBusinessDocumentConfig(objectId, data) {
  return request.put(`/ai/business/document/config/${objectId}`, data)
}

export function businessDocumentNoRuleTokens() {
  return request.get('/ai/business/document/no-rule/tokens')
}

export function previewBusinessDocumentNoRule(data) {
  return request.post('/ai/business/document/no-rule/preview', data)
}

export function businessDocumentRuntime(objectCode, recordId) {
  return request.get(`/ai/business/document/${objectCode}/${recordId}/runtime`)
}

export function businessDocumentRuntimeBatch(objectCode, recordIds = []) {
  return request.post(`/ai/business/document/${objectCode}/runtime/batch`, { recordIds })
}

export function codeRulePage(params) {
  return request.get('/ai/code-rule/page', { params })
}

export function codeRuleList(params = {}) {
  return request.get('/ai/code-rule/list', { params })
}

export function codeRuleTokens() {
  return request.get('/ai/code-rule/tokens')
}

export function previewCodeRule(data) {
  return request.post('/ai/code-rule/preview', data)
}

export function generateCodeRule(data) {
  return request.post('/ai/code-rule/generate', data)
}

export function businessFlowBinding(objectCode) {
  return request.get(`/ai/business/flow/binding/${objectCode}`)
}

export function saveBusinessFlowBinding(objectCode, data) {
  return request.put(`/ai/business/flow/binding/${objectCode}`, data)
}

export function businessFlowAppConfig(objectCode) {
  return request.get(`/ai/business/flow-app/config/${objectCode}`)
}

export function saveBusinessFlowAppConfig(objectCode, data) {
  return request.put(`/ai/business/flow-app/config/${objectCode}`, data)
}

export function businessFlowVariables(modelKey, params = {}) {
  return request.get(`/ai/business/flow/model/${modelKey}/variables`, { params })
}

export function businessFlowFormAssets(objectCode, params = {}) {
  return request.get(`/ai/business/flow/form-assets/${objectCode}`, { params })
}

export function businessTaskFormContext(params = {}) {
  return request.get('/ai/business/flow/task-form-context', { params })
}

export function businessTaskFormReadonlyContext(params = {}) {
  return request.get('/ai/business/flow/task-form-context/readonly', { params })
}

export function saveBusinessTaskFormContext(data) {
  return request.put('/ai/business/flow/task-form-context', data)
}

export function completeBusinessTaskAction(data) {
  return request.post('/ai/business/flow/task-action', data)
}

export function startBusinessDocumentFlow(data) {
  return request.post('/ai/business/flow/start', data)
}

export function resubmitBusinessDocumentFlow(data) {
  return request.post('/ai/business/flow/resubmit', data)
}

export function businessFlowStatus(objectCode, recordId) {
  return request.get(`/ai/business/flow/status/${objectCode}/${recordId}`)
}

// ==================== 流程集成（历史兼容） ====================

export function startBusinessFlow(data) {
  return request.post('/ai/business/trigger/flow/start', data)
}

export function getFlowBinding(objectCode) {
  return request.get(`/ai/business/trigger/flow/binding/${objectCode}`)
}

export function saveFlowBinding(objectCode, data) {
  return request.post(`/ai/business/trigger/flow/binding/${objectCode}`, data)
}

export function getFlowStatus(objectCode, recordId) {
  return request.get(`/ai/business/trigger/flow/status/${objectCode}/${recordId}`)
}

// ==================== 统计接口 ====================

export function businessStatsOverview(configKey) {
  return request.get(`/ai/business/stats/${configKey}/overview`)
}

export function businessStatsGroup(configKey, field) {
  return request.get(`/ai/business/stats/${configKey}/group`, { params: { field } })
}

export function businessStatsTrend(configKey, params) {
  return request.get(`/ai/business/stats/${configKey}/trend`, { params })
}

export function businessStatsMetrics(configKey, params) {
  const searchParams = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '')
      return
    if (Array.isArray(value)) {
      value.forEach(item => searchParams.append(key, item))
      return
    }
    searchParams.append(key, value)
  })
  return request.get(`/ai/business/stats/${configKey}/metrics`, { params: searchParams })
}

// ==================== 权限接口 ====================

export function getPermissionOverview(objectCode) {
  return request.get(`/ai/business/trigger/permission/${objectCode}`)
}

export function businessObjectPermissionActions(objectId) {
  return request.get(`/ai/business/object/${objectId}/permission-actions`)
}

export function enabledApiConfigs() {
  return request.get('/apiConfig/getAllEnabled')
}
