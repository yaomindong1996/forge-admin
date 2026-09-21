import { request } from '@/utils'

export function crudConfigPage(params) {
  return request.get('/ai/crud-config/page', { params })
}

export function crudConfigGetById(id) {
  return request.get(`/ai/crud-config/${id}`)
}

export function crudConfigGetByKey(configKey) {
  return request.get(`/ai/crud-config/by-key/${configKey}`)
}

export function updateSessionMetadata(sessionId, metadata) {
  return request.put(`/ai/admin/session/${sessionId}/metadata`, metadata)
}

export function crudConfigRender(configKey, designPreview = false, requestOptions = {}) {
  const options = requestOptions && typeof requestOptions === 'object'
    ? { ...requestOptions }
    : { appId: requestOptions }
  const appId = options.appId
  const applicationId = options.applicationId
  const pageId = options.pageId
  delete options.appId
  delete options.applicationId
  delete options.pageId
  const params = { ...(options.params || {}) }
  if (typeof pageId === 'string' && pageId.trim())
    params.pageId = pageId
  if (designPreview)
    params.designPreview = true
  if (appId !== null && appId !== undefined && String(appId).trim())
    params.appId = String(appId)
  if (applicationId !== null && applicationId !== undefined && String(applicationId).trim())
    params.applicationId = String(applicationId)
  return request.get(`/ai/crud-config/render/${configKey}`, {
    ...options,
    params: Object.keys(params).length ? params : undefined,
  })
}

export function crudConfigAdd(data) {
  return request.post('/ai/crud-config', data)
}

export function crudConfigUpdate(data) {
  return request.put('/ai/crud-config', data)
}

export function crudConfigDelete(id) {
  return request.delete(`/ai/crud-config/${id}`)
}

export function crudConfigAiGenerate(data) {
  return request.post('/ai/crud-config/ai/generate', data)
}

export function crudConfigAiGenerateFromTable(data) {
  return request.post('/ai/crud-config/ai/generateFromTable', data)
}

export function customQueryExecute(configKey, data, config = {}) {
  return request.post(`/ai/custom-query/${configKey}/execute`, data, config)
}

export function customQuerySchemeList(configKey) {
  return request.get(`/ai/custom-query/${configKey}/scheme/list`)
}

export function customQuerySchemeGet(configKey, id) {
  return request.get(`/ai/custom-query/${configKey}/scheme/${id}`)
}

export function customQuerySchemeAdd(configKey, data) {
  return request.post(`/ai/custom-query/${configKey}/scheme`, data)
}

export function customQuerySchemeUpdate(configKey, data) {
  return request.put(`/ai/custom-query/${configKey}/scheme`, data)
}

export function customQuerySchemeDelete(configKey, id) {
  return request.delete(`/ai/custom-query/${configKey}/scheme/${id}`)
}
