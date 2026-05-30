import { request } from '@/utils'

export function businessSuitePage(params) {
  return request.get('/ai/business/suite/page', { params })
}

export function businessSuiteList(params) {
  return request.get('/ai/business/suite/list', { params })
}

export function businessSuiteSummary(params) {
  return request.get('/ai/business/suite/summary', { params })
}

export function businessSuiteDetail(id) {
  return request.get(`/ai/business/suite/${id}`)
}

export function createBusinessSuite(data) {
  return request.post('/ai/business/suite', data)
}

export function updateBusinessSuite(data) {
  return request.put('/ai/business/suite', data)
}

export function syncLowcodeDomains() {
  return request.post('/ai/business/suite/sync-lowcode-domains')
}

export function businessObjectPage(params) {
  return request.get('/ai/business/object/page', { params })
}

export function businessObjectList(params) {
  return request.get('/ai/business/object/list', { params })
}

export function businessObjectDetail(id) {
  return request.get(`/ai/business/object/${id}`)
}

export function createBusinessObject(data) {
  return request.post('/ai/business/object', data)
}

export function updateBusinessObject(data) {
  return request.put('/ai/business/object', data)
}

export function updateBusinessObjectStatus(id, status) {
  return request.put(`/ai/business/object/${id}/status`, null, { params: { status } })
}

export function deleteBusinessObject(id) {
  return request.delete(`/ai/business/object/${id}`)
}

export function businessObjectRuntimeInfo(id) {
  return request.get(`/ai/business/object/${id}/runtime-info`)
}

export function businessObjectRelations(objectId) {
  return request.get(`/ai/business/object/${objectId}/relations`)
}

export function saveBusinessObjectRelations(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/relations`, data)
}

export function syncLowcodeModels() {
  return request.post('/ai/business/object/sync-lowcode-models')
}

export function businessAppPage(params) {
  return request.get('/ai/business/app/page', { params })
}

export function businessAppList(params) {
  return request.get('/ai/business/app/list', { params })
}

export function businessAppDetail(id) {
  return request.get(`/ai/business/app/${id}`)
}

export function createBusinessApp(data) {
  return request.post('/ai/business/app', data)
}

export function updateBusinessApp(data) {
  return request.put('/ai/business/app', data)
}

export function updateBusinessAppStatus(id, status) {
  return request.put(`/ai/business/app/${id}/status`, null, { params: { status } })
}

export function deleteBusinessApp(id) {
  return request.delete(`/ai/business/app/${id}`)
}

export function businessAppOpenInfo(id) {
  return request.get(`/ai/business/app/${id}/open-info`)
}

export function syncPublishedCrudConfigs() {
  return request.post('/ai/business/app/sync-published-crud-configs')
}

export function businessBindingList(params) {
  return request.get('/ai/business/binding/list', { params })
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
  return request.put(`/ai/business/object/${objectId}/designer`, data)
}

export function businessObjectFields(objectId) {
  return request.get(`/ai/business/object/${objectId}/fields`)
}

export function createBusinessObjectField(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/fields`, data)
}

export function sortBusinessObjectFields(objectId, fieldCodes) {
  return request.put(`/ai/business/object/${objectId}/fields/sort`, fieldCodes)
}

export function updateBusinessObjectField(objectId, fieldCode, data) {
  return request.put(`/ai/business/object/${objectId}/fields/${fieldCode}`, data)
}

export function deleteBusinessObjectField(objectId, fieldCode) {
  return request.delete(`/ai/business/object/${objectId}/fields/${fieldCode}`)
}

export function businessObjectLayout(objectId, layoutKey) {
  return request.get(`/ai/business/object/${objectId}/layout/${layoutKey}`)
}

export function saveBusinessObjectFormLayout(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/layout/form`, data)
}

export function saveBusinessObjectListLayout(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/layout/list`, data)
}

export function saveBusinessObjectDetailLayout(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/layout/detail`, data)
}

export function previewBusinessObjectLayout(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/layout/preview`, data)
}

export function businessObjectActions(objectId) {
  return request.get(`/ai/business/object/${objectId}/actions`)
}

export function saveBusinessObjectActions(objectId, data) {
  return request.put(`/ai/business/object/${objectId}/actions`, data)
}

export function businessObjectPermissionSummary(objectId) {
  return request.get(`/ai/business/object/${objectId}/permission-summary`)
}

export function businessObjectPublishCheck(objectId) {
  return request.get(`/ai/business/object/${objectId}/publish-check`)
}

export function publishBusinessObject(objectId, data) {
  return request.post(`/ai/business/object/${objectId}/publish`, data)
}

export function businessObjectDesignVersions(objectId) {
  return request.get(`/ai/business/object/${objectId}/versions`)
}

export function rollbackBusinessObjectDesignVersion(objectId, versionId) {
  return request.post(`/ai/business/object/${objectId}/versions/${versionId}/rollback`)
}
