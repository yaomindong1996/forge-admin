import { request } from '@/utils'

export interface ExternalApi {
  id?: number
  systemId: number
  apiName: string
  apiCode?: string
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

export function getExternalApiPage(params: { pageNum: number; pageSize: number; systemId?: number; apiCode?: string; apiName?: string; apiMethod?: string; apiStatus?: number }) {
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

export function clearExternalApiLogs(params: { systemId?: number; apiId?: number; callStatus?: number; debugFlag?: boolean }) {
  return request.delete('/external/api/log/clear', { params })
}

export function getExternalApiLogSummary(params: { systemId?: number; apiId?: number; callStatus?: number; debugFlag?: boolean }) {
  return request.get('/external/api/log/summary', { params })
}

export function updateExternalApiDocument(data: ExternalApi) {
  return request.put('/external/api', data)
}
