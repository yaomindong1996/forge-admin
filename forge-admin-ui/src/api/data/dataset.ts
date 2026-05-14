import { request } from '@/utils'

export interface DataDataset {
  id?: number
  datasetCode: string
  datasetName: string
  connectionId: number
  connectionName?: string
  datasetType: string
  tableName?: string
  sqlText?: string
  paramSchemaJson?: string
  defaultOrderJson?: string
  maxRows?: number
  timeoutSeconds?: number
  cacheEnabled?: number
  cacheTtlSeconds?: number
  status?: number
  description?: string
  createTime?: string
  updateTime?: string
}

export interface DataDatasetField {
  id?: number
  fieldName: string
  fieldLabel?: string
  sourceColumn?: string
  dbType?: string
  dataType: string
  fieldRole: string
  defaultAgg?: string
  queryEnabled?: number
  displayEnabled?: number
  sensitiveLevel?: string
  maskRule?: string
  dictType?: string
  sort?: number
  description?: string
}

export interface DataDatasetDetail {
  id: number
  datasetCode: string
  datasetName: string
  connectionId: number
  connectionName?: string
  datasetType: string
  tableName?: string
  sqlText?: string
  paramSchemaJson?: string
  defaultOrderJson?: string
  maxRows: number
  timeoutSeconds: number
  cacheEnabled: number
  cacheTtlSeconds?: number
  status: number
  description?: string
  createTime: string
  updateTime: string
  fields: DataDatasetField[]
}

export interface DataDatasetPreview {
  columns: string[]
  rows: Record<string, any>[]
  total: number
}

export function getDataDatasetPage(params: { pageNum: number, pageSize: number, datasetName?: string, connectionId?: number, datasetType?: string, status?: number }) {
  return request.get('/data/dataset/page', { params })
}

export function getDataDatasetList(connectionId?: number) {
  return request.get('/data/dataset/list', { params: { connectionId } })
}

export function getDataDatasetById(id: number) {
  return request.get(`/data/dataset/${id}`)
}

export function createDataDataset(data: DataDataset & { fields?: DataDatasetField[] }) {
  return request.post('/data/dataset', data)
}

export function updateDataDataset(data: DataDataset & { fields?: DataDatasetField[] }) {
  return request.put('/data/dataset', data)
}

export function deleteDataDataset(id: number) {
  return request.delete(`/data/dataset/${id}`)
}

export function syncDataDatasetFields(id: number) {
  return request.post(`/data/dataset/${id}/sync-fields`)
}

export function saveDataDatasetFields(id: number, fields: DataDatasetField[]) {
  return request.put(`/data/dataset/${id}/fields`, fields)
}

export function previewDataDataset(id: number, params?: Record<string, any>, maxRows?: number) {
  return request.post(`/data/dataset/${id}/preview`, { datasetId: id, params, maxRows })
}

export function getDataDatasetMetadata(id: number) {
  return request.get(`/data/dataset/runtime/${id}/metadata`)
}

export function queryDataDataset(dto: { datasetId: number, params?: Record<string, any>, fields?: string[], pageNum?: number, pageSize?: number, maxRows?: number }) {
  return request.post('/data/dataset/runtime/query', dto)
}
