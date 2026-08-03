import { postEncrypt, request } from '@/utils'

// ========== 能力目录 ==========

export function getCapabilityPage(params) {
  return request.get('/ai/capability/page', { params })
}

export function getCapabilityById(id) {
  return request.post('/ai/capability/getById', null, { params: { id } })
}

export function getCapabilityVersionDraft(id) {
  return request.get(`/ai/capability/${id}/version-draft`)
}

export function publishCapability(data) {
  return postEncrypt('/ai/capability/publish', data)
}

export function publishBusinessActionCapability(data) {
  return postEncrypt('/ai/capability/business-action/publish', data)
}

export function getBusinessActionRegistrationSource(params) {
  return request.get('/ai/capability/business-action/registration-source', { params })
}

export function publishFlowActionCapability(data) {
  return postEncrypt('/ai/capability/flow-action/publish', data)
}

export function getFlowActionRegistrationSource(params) {
  return request.get('/ai/capability/flow-action/registration-source', { params })
}

export function getSystemServiceRegistrationSources() {
  return request.get('/ai/capability/system-service/registration-source')
}

export function publishSystemServiceCapability(data) {
  return postEncrypt('/ai/capability/system-service/publish', data)
}

export function disableCapability(id) {
  return request.post(`/ai/capability/disable/${id}`)
}

export function enableCapability(id) {
  return request.post(`/ai/capability/enable/${id}`)
}

export function downloadCapabilityOpenApi(id) {
  return request({
    method: 'get',
    url: `/ai/capability/${id}/openapi`,
    responseType: 'blob',
    rawResponse: true,
    preserveBlob: true,
    encrypt: false,
  })
}

export function downloadCapabilityMarkdown(id) {
  return request({
    method: 'get',
    url: `/ai/capability/${id}/document`,
    responseType: 'blob',
    rawResponse: true,
    preserveBlob: true,
    encrypt: false,
  })
}

export function getCapabilityCallGuide(id, clientId) {
  return request.get(`/ai/capability/${id}/call-guide`, { params: { clientId } })
}

export function getCapabilityCallGuideClients() {
  return request.get('/ai/capability/call-guide/clients')
}

// ========== 机器客户端 ==========

export function getCapabilityClientPage(params) {
  return request.get('/ai/capability/client/page', { params })
}

export function addCapabilityClient(data) {
  return postEncrypt('/ai/capability/client/add', data)
}

export function rotateCapabilityClientSecret(id) {
  return postEncrypt(`/ai/capability/client/rotate/${id}`)
}

export function rotateCapabilityClientSigningKey(id) {
  return postEncrypt(`/ai/capability/client/signing-key/rotate/${id}`)
}

export function revokeCapabilityClient(id) {
  return request.post(`/ai/capability/client/revoke/${id}`)
}

export function getClientUserAssertionConfig(id) {
  return request.get(`/ai/capability/client/${id}/user-assertion`)
}

export function rotateClientUserAssertionKey(id) {
  return postEncrypt(`/ai/capability/client/${id}/user-assertion/key/rotate`)
}

export function disableClientUserAssertion(id) {
  return request.post(`/ai/capability/client/${id}/user-assertion/disable`)
}

export function addClientUserAssertionMapping(id, data) {
  return postEncrypt(`/ai/capability/client/${id}/user-assertion/mapping`, data)
}

export function removeClientUserAssertionMapping(id, mappingId) {
  return request.delete(`/ai/capability/client/${id}/user-assertion/mapping/${mappingId}`)
}

// ========== 能力授权 ==========

export function getCapabilityGrantPage(params) {
  return request.get('/ai/capability/grant/page', { params })
}

export function getCapabilityGrantOptions() {
  return request.get('/ai/capability/grant/options')
}

export function addCapabilityGrant(data) {
  return postEncrypt('/ai/capability/grant/add', data)
}

export function updateCapabilityGrant(id, data) {
  return postEncrypt(`/ai/capability/grant/update/${id}`, data)
}

export function useCurrentCapabilityGrantVersion(id) {
  return request.post(`/ai/capability/grant/use-current-version/${id}`)
}

export function revokeCapabilityGrant(id) {
  return request.post(`/ai/capability/grant/revoke/${id}`)
}

// ========== 调用日志 ==========

export function getCapabilityInvocationPage(params) {
  return request.get('/ai/capability/invocation/page', { params })
}
