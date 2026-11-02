import { request } from '@/utils'

const BASE = '/crm/biz-form'

/** 录入页运行时方案 */
export function bizFormScheme(objectType) {
  return request.get(`${BASE}/scheme`, { params: { objectType } })
}

/** 配置弹窗：默认模版详情 */
export function bizFormTemplateDefault(objectType) {
  return request.get(`${BASE}/template/${encodeURIComponent(objectType)}/default`)
}

/** 保存默认模版 */
export function saveBizFormTemplateDefault(data) {
  return request.put(`${BASE}/template/default`, data)
}

/** 侧表扩展值（平台 /system 对象） */
export function bizFormExtGet(objectType, bizId) {
  return request.get(`${BASE}/ext`, { params: { objectType, bizId } })
}

export function bizFormExtSave(data) {
  return request.put(`${BASE}/ext`, data)
}
