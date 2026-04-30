import { request } from '@/utils/request'

/**
 * 获取行政区划树形结构
 */
export function getRegionTree() {
  return request.get('/system/region/tree')
}

/**
 * 根据code获取详情
 */
export function getRegionByCode(code) {
  return request.get(`/system/region/${code}`)
}

/**
 * 新增行政区划
 */
export function addRegion(data) {
  return request.post('/system/region/', data)
}

/**
 * 更新行政区划
 */
export function updateRegion(data) {
  return request.put('/system/region/', data)
}

/**
 * 删除行政区划
 */
export function deleteRegion(code) {
  return request.delete(`/system/region/${code}`)
}

/**
 * 获取子级列表
 */
export function getRegionChildren(parentCode) {
  return request.get(`/system/region/children/${parentCode}`)
}

/**
 * 搜索行政区划
 */
export function searchRegion(name) {
  return request.get('/system/region/search', { params: { name } })
}
