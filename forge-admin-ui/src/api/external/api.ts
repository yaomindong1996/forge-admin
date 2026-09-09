import { request } from '@/utils'

export interface ExternalApi {
  id?: number
  systemId: number
  apiName: string
  apiCode?: string
  executionMode?: string
  apiPath: string
  apiMethod: string
  apiDesc?: string
  requestContentType?: string
  requestHeaders?: string
  requestParams?: string
  requestBodyTemplate?: string
  responseContentType?: string
  responseDataPath?: string
  responseTotalPath?: string
  mockResponseJson?: string
  paramMappingEnabled?: boolean
  paramMappings?: string
  responseTransformEnabled?: boolean
  responseTransformScript?: string
  errorCodePath?: string
  errorMsgPath?: string
  successCodes?: string
  docFileId?: string
  docFileName?: string
  rateLimitEnabled?: boolean
  rateLimitQps?: number
  cacheEnabled?: boolean
  cacheTtl?: number
  cacheKeyTemplate?: string
  permissionCheckEnabled?: boolean
  requiredPermission?: string
  lowcodeQueryEnabled?: boolean
  inputSchemaJson?: string
  outputSchemaJson?: string
  apiStatus?: number
  sortOrder?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ExternalApiVO extends ExternalApi {
  systemName?: string
}

export function getExternalApiList() {
  return request.get('/external/api/list')
}

export function getExternalApiPage(params: { pageNum: number, pageSize: number, systemId?: number, apiCode?: string, apiName?: string, apiMethod?: string, apiStatus?: number }) {
  return request.get('/external/api/page', { params })
}

export function getExternalApiById(id: number) {
  return request.get(`/external/api/${id}`)
}

export function createExternalApi(data: ExternalApi) {
  return request.post('/external/api', data)
}

export function updateExternalApi(data: ExternalApi) {
  return request.put('/external/api', data)
}

export function deleteExternalApi(id: number) {
  return request.delete(`/external/api/${id}`)
}

export function debugExternalApi(id: number, params: Record<string, any>) {
  return request.post(`/external/proxy/debug/${id}`, params)
}

/**
 * 调用已登记的外部接口。请求始终先到 Forge 代理，外部系统凭据不会暴露给浏览器。
 */
export function callExternalApi<T = unknown>(id: number, params: Record<string, unknown> = {}, method = 'GET') {
  const normalizedMethod = String(method || 'GET').toUpperCase()
  if (['GET', 'HEAD'].includes(normalizedMethod)) {
    return request.get<T>(`/external/proxy/${id}`, { params })
  }
  return request.post<T>(`/external/proxy/${id}`, params)
}

export function clearExternalApiLogs(params: { systemId?: number, apiId?: number, callStatus?: number, debugFlag?: boolean }) {
  return request.delete('/external/api/log/clear', { params })
}

export function getExternalApiLogSummary(params: { systemId?: number, apiId?: number, callStatus?: number, debugFlag?: boolean }) {
  return request.get('/external/api/log/summary', { params })
}

export function updateExternalApiDocument(data: ExternalApi) {
  return request.put('/external/api', data)
}
