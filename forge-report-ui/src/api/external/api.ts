import { del, get, post, put } from '@/api/http'

export interface ExternalApi {
  id: number | string
  apiName: string
  systemId: number
  apiPath: string
  method: string
  adapterType: string
  adapterConfig: string
  description?: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface ExternalApiPageResponse {
  records: ExternalApi[]
  total: number
  size: number
  current: number
}

export const getExternalApiPageApi = (params?: { pageNum?: number; pageSize?: number; apiName?: string; systemId?: number; status?: number }) => {
  return get('/forge-report-api/external/api/page', params) as unknown as Promise<{ code: number; data: ExternalApiPageResponse; msg: string }>
}

export const getExternalApiDetailApi = (id: string | number) => {
  return get(`/forge-report-api/external/api/${id}`) as unknown as Promise<{ code: number; data: ExternalApi; msg: string }>
}

export const createExternalApiApi = (data: Partial<ExternalApi>) => {
  return post('/forge-report-api/external/api', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const updateExternalApiApi = (data: Partial<ExternalApi>) => {
  return put('/forge-report-api/external/api', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const deleteExternalApiApi = (id: string | number) => {
  return del(`/forge-report-api/external/api/${id}`) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const getExternalApiListApi = (systemId?: number) => {
  return get('/forge-report-api/external/api/list', { systemId }) as unknown as Promise<{ code: number; data: ExternalApi[]; msg: string }>
}