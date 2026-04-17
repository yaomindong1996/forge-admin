import { request } from '@/utils'

// ========== 供应商管理 ==========

/**
 * 分页查询供应商列表
 */
export function providerPage(params) {
  return request.get('/ai/provider/page', { params })
}

/**
 * 查询供应商详情
 */
export function providerGetById(id) {
  return request.get(`/ai/provider/${id}`)
}

/**
 * 新增供应商
 */
export function providerAdd(data) {
  return request.post('/ai/provider', data)
}

/**
 * 修改供应商
 */
export function providerUpdate(data) {
  return request.put('/ai/provider', data)
}

/**
 * 删除供应商
 */
export function providerDelete(id) {
  return request.delete(`/ai/provider/${id}`)
}

/**
 * 测试供应商连接
 */
export function providerTest(data) {
  return request.post('/ai/provider/test', data)
}

/**
 * 设为默认供应商
 */
export function providerSetDefault(id) {
  return request.put(`/ai/provider/${id}/default`)
}

/**
 * 获取供应商模板列表
 */
export function providerTemplates() {
  return request.get('/ai/provider/templates')
}

// ========== 模型管理 ==========

/**
 * 分页查询模型列表
 */
export function modelPage(params) {
  return request.get('/ai/model/page', { params })
}

/**
 * 按供应商查询所有模型（下拉选择用）
 */
export function modelListByProvider(providerId) {
  return request.get('/ai/model/list', { params: { providerId } })
}

/**
 * 查询模型详情
 */
export function modelGetById(id) {
  return request.get(`/ai/model/${id}`)
}

/**
 * 新增模型
 */
export function modelAdd(data) {
  return request.post('/ai/model', data)
}

/**
 * 修改模型
 */
export function modelUpdate(data) {
  return request.put('/ai/model', data)
}

/**
 * 删除模型
 */
export function modelDelete(id) {
  return request.delete(`/ai/model/${id}`)
}
