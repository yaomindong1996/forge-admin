import { request } from '@/utils'

export function skillPackagePage(params) {
  return request.get('/ai/skill/package/page', { params })
}

export function skillPackageVersions(packageId) {
  return request.get('/ai/skill/package/versions', { params: { packageId } })
}

export function skillPackageVersionDetail(versionId) {
  return request.get(`/ai/skill/package/versions/${versionId}`)
}

export function skillPackagePreview(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/ai/skill/package/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function skillPackageImport(file, expectedArchiveHash) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/ai/skill/package/import', formData, {
    params: { expectedArchiveHash },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function skillPackageActivate(versionId, payload) {
  return request.post(`/ai/skill/package/versions/${versionId}/activate`, payload)
}

export function skillPackageRetire(versionId, payload) {
  return request.post(`/ai/skill/package/versions/${versionId}/retire`, payload)
}

export function agentSkillPackages(agentId) {
  return request.get(`/ai/skill/package/agent/${agentId}`)
}

export function bindAgentSkillPackage(data) {
  return request.post('/ai/skill/package/bind', data)
}

export function unbindAgentSkillPackage(agentId, versionId) {
  return request.delete(`/ai/skill/package/agent/${agentId}/${versionId}`)
}
