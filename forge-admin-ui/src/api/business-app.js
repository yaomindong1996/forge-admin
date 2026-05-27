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
