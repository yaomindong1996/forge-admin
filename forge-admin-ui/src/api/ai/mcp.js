import { postEncrypt, request } from '@/utils'

export function mcpConnectionList() {
  return request.get('/ai/mcp/connection')
}

export function mcpConnectionCreate(data) {
  return postEncrypt('/ai/mcp/connection', data)
}

export function mcpConnectionUpdate(data) {
  return request.put('/ai/mcp/connection', data, { encrypt: true })
}

export function mcpConnectionSync(id, expectedRowVersion) {
  return request.post(`/ai/mcp/connection/${id}/sync`, { expectedRowVersion })
}

export function mcpConnectionDelete(id, expectedRowVersion) {
  return request.delete(`/ai/mcp/connection/${id}`, { params: { expectedRowVersion } })
}

export function resourceToolsCatalog(params) {
  return request.get('/ai/resource/tools', { params })
}
