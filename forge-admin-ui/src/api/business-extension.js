import { request } from '@/utils'

const ENCRYPTED_REQUEST = { encrypt: true }

function encryptedParams(params) {
  return { params, encrypt: true }
}

export function businessExtensionPage(params) {
  return request.get('/ai/business/extension/page', encryptedParams(params))
}

export function businessExtensionDetail(id) {
  return request.get(`/ai/business/extension/${id}`, ENCRYPTED_REQUEST)
}

export function createBusinessExtension(data) {
  return request.post('/ai/business/extension', data, ENCRYPTED_REQUEST)
}

export function updateBusinessExtension(data) {
  return request.put('/ai/business/extension', data, ENCRYPTED_REQUEST)
}

export function deleteBusinessExtension(id) {
  return request.delete(`/ai/business/extension/${id}`, ENCRYPTED_REQUEST)
}

export function businessExtensionVersions(id) {
  return request.get(`/ai/business/extension/${id}/versions`, ENCRYPTED_REQUEST)
}

export function saveBusinessExtensionDraft(id, data) {
  return request.post(`/ai/business/extension/${id}/versions`, data, ENCRYPTED_REQUEST)
}

export function businessExtensionDiff(id, params) {
  return request.get(`/ai/business/extension/${id}/versions/diff`, encryptedParams(params))
}

export function rollbackBusinessExtension(id, versionNo, lockToken) {
  return request.post(
    `/ai/business/extension/${id}/versions/${versionNo}/rollback`,
    null,
    encryptedParams({ lockToken }),
  )
}

export function acquireBusinessExtensionLock(id) {
  return request.post(`/ai/business/extension/${id}/lock`, null, ENCRYPTED_REQUEST)
}

export function renewBusinessExtensionLock(id, lockToken) {
  return request.put(`/ai/business/extension/${id}/lock`, null, encryptedParams({ lockToken }))
}

export function releaseBusinessExtensionLock(id, lockToken) {
  return request.delete(`/ai/business/extension/${id}/lock`, encryptedParams({ lockToken }))
}

export function validateBusinessExtension(id) {
  return request.post(`/ai/business/extension/${id}/validate`, null, ENCRYPTED_REQUEST)
}

export function testBusinessExtension(id, data) {
  return request.post(`/ai/business/extension/${id}/test`, data || {}, ENCRYPTED_REQUEST)
}

export function updateBusinessExtensionStatus(id, status) {
  return request.put(`/ai/business/extension/${id}/status`, null, encryptedParams({ status }))
}

export function businessExtensionServerHandlers() {
  return request.get('/ai/business/extension/server-handlers', ENCRYPTED_REQUEST)
}
