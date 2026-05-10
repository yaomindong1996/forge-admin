import { del, get, post, put } from '@/api/http'

export interface ExternalSystem {
  id: number | string
  systemName: string
  baseUrl: string
  authType: string
  authConfig: string
  description?: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface ExternalSystemPageResponse {
  records: ExternalSystem[]
  total: number
  size: number
  current: number
}

export const getExternalSystemPageApi = (params?: { pageNum?: number; pageSize?: number; systemName?: string; status?: number }) => {
  return get('/forge-report-api/external/system/page', params) as unknown as Promise<{ code: number; data: ExternalSystemPageResponse; msg: string }>
}

export const getExternalSystemDetailApi = (id: string | number) => {
  return get(`/forge-report-api/external/system/${id}`) as unknown as Promise<{ code: number; data: ExternalSystem; msg: string }>
}

export const createExternalSystemApi = (data: Partial<ExternalSystem>) => {
  return post('/forge-report-api/external/system', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const updateExternalSystemApi = (data: Partial<ExternalSystem>) => {
  return put('/forge-report-api/external/system', data) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const deleteExternalSystemApi = (id: string | number) => {
  return del(`/forge-report-api/external/system/${id}`) as unknown as Promise<{ code: number; data?: any; msg: string }>
}

export const getExternalSystemListApi = () => {
  return get('/forge-report-api/external/system/list') as unknown as Promise<{ code: number; data: ExternalSystem[]; msg: string }>
}