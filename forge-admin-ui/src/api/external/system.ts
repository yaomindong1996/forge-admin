import { request } from '@/utils'

export interface ExternalSystem {
  id?: number
  systemName: string
  systemCode?: string
  baseUrl: string
  authType: string
  systemDesc?: string
  basicUsername?: string
  basicPassword?: string
  tokenValue?: string
  tokenHeaderName?: string
  tokenPrefix?: string
  oauth2TokenUrl?: string
  oauth2ClientId?: string
  oauth2ClientSecret?: string
  oauth2GrantType?: string
  oauth2Scope?: string
  apiKeyName?: string
  apiKeyValue?: string
  apiKeyPosition?: string
  customAuthAdapter?: string
  customAuthConfig?: string
  trustedInternal?: boolean
  proxyEnabled?: boolean
  proxyHost?: string
  proxyPort?: number
  proxyUsername?: string
  proxyPassword?: string
  retryEnabled?: boolean
  retryMaxAttempts?: number
  retryBackoffInterval?: number
  connectTimeout?: number
  readTimeout?: number
  writeTimeout?: number
  sslVerifyEnabled?: boolean
  requestLoggingEnabled?: boolean
  systemStatus?: number
  remark?: string
  apiCount?: number
  createTime?: string
  updateTime?: string
}

export function getExternalSystemList() {
  return request.get('/external/system/list')
}

export function getExternalSystemPage(params: { pageNum: number; pageSize: number; systemCode?: string; systemName?: string; authType?: string; systemStatus?: number }) {
  return request.get('/external/system/page', { params })
}

export function getExternalSystemById(id: number) {
  return request.get(`/external/system/${id}`)
}

export function createExternalSystem(data: ExternalSystem) {
  return request.post('/external/system', data)
}

export function updateExternalSystem(data: ExternalSystem) {
  return request.put('/external/system', data)
}

export function deleteExternalSystem(id: number) {
  return request.delete(`/external/system/${id}`)
}
