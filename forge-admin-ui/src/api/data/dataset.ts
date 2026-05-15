import { request } from '@/utils'

export interface DataDataset {
  id?: number
  datasetCode: string
  datasetName: string
  connectionId: number
  categoryId?: number | null
  connectionName?: string
  categoryCode?: string
  categoryName?: string
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
  publishStatus?: number
  accessMode?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export interface DataDatasetCategory {
  id?: number
  parentId?: number | null
  level?: number
  ancestors?: string
  categoryCode: string
  categoryName: string
  sortOrder?: number
  status?: number
  description?: string
  children?: DataDatasetCategory[]
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
  dateFormat?: string
  dataUnit?: string
  dimensionId?: number | null
  dimensionCode?: string
  dimensionName?: string
  sort?: number
  description?: string
}

export interface DataDatasetAcl {
  id?: number
  subjectType: string
  subjectId: number
  accessLevel?: string
}

export interface DataDatasetRowScope {
  id?: number
  enabled?: number
  scopeMode?: string
  tenantColumn?: string
  orgColumn?: string
  userColumn?: string
  regionColumn?: string
  regionStrategy?: string
  remark?: string
}

export interface DataDatasetDetail {
  id: number
  datasetCode: string
  datasetName: string
  connectionId: number
  categoryId?: number | null
  connectionName?: string
  categoryCode?: string
  categoryName?: string
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
  publishStatus: number
  accessMode?: string
  description?: string
  createTime: string
  updateTime: string
  fields: DataDatasetField[]
  aclItems?: DataDatasetAcl[]
  rowScope?: DataDatasetRowScope
}

export interface DataDatasetPreview {
  columns: string[]
  rows: Record<string, any>[]
  total: number
}

export function getDataDatasetPage(params: {
  pageNum: number
  pageSize: number
  datasetName?: string
  connectionId?: number
  categoryId?: number
  uncategorized?: boolean
  datasetType?: string
  status?: number
  publishStatus?: number
}) {
  return request.get('/data/dataset/page', { params })
}

export function getDataDatasetList(connectionId?: number) {
  return request.get('/data/dataset/list', { params: { connectionId } })
}

export function getDataDatasetById(id: number) {
  return request.get(`/data/dataset/${id}`)
}

export function createDataDataset(data: DataDataset & { fields?: DataDatasetField[], aclItems?: DataDatasetAcl[], rowScope?: DataDatasetRowScope }) {
  return request.post('/data/dataset', data)
}

export function updateDataDataset(data: DataDataset & { fields?: DataDatasetField[], aclItems?: DataDatasetAcl[], rowScope?: DataDatasetRowScope }) {
  return request.put('/data/dataset', data)
}

export function deleteDataDataset(id: number) {
  return request.delete(`/data/dataset/${id}`)
}

export function publishDataDataset(id: number) {
  return request.post(`/data/dataset/${id}/publish`)
}

export function offlineDataDataset(id: number) {
  return request.post(`/data/dataset/${id}/offline`)
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

export function getDataDatasetCategoryTree() {
  return request.get('/data/dataset/category/tree')
}

export function createDataDatasetCategory(data: DataDatasetCategory) {
  return request.post('/data/dataset/category', data)
}

export function updateDataDatasetCategory(data: DataDatasetCategory) {
  return request.put('/data/dataset/category', data)
}

export function deleteDataDatasetCategory(id: number) {
  return request.delete(`/data/dataset/category/${id}`)
}

export function queryDataDataset(dto: { datasetId: number, params?: Record<string, any>, fields?: string[], pageNum?: number, pageSize?: number, maxRows?: number }) {
  return request.post('/data/dataset/runtime/query', dto)
}
