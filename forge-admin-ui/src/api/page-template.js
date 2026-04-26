import { request } from '@/utils'

/**
 * AI 页面模板 API
 */

/** 查询所有启用的模板（用于生成器选择） */
export function listEnabledTemplates() {
  return request.get('/ai/page-template/list')
}

/** 分页查询（管理页面用） */
export function pageTemplates(params) {
  return request.get('/ai/page-template/page', { params })
}

/** 根据 templateKey 查询 */
export function getTemplateByKey(templateKey) {
  return request.get(`/ai/page-template/by-key/${templateKey}`)
}

/** 根据 ID 查询 */
export function getTemplateById(id) {
  return request.get(`/ai/page-template/${id}`)
}

/** 新增模板 */
export function createTemplate(data) {
  return request.post('/ai/page-template', data)
}

/** 更新模板 */
export function updateTemplate(data) {
  return request.put('/ai/page-template', data)
}

/** 删除模板 */
export function deleteTemplate(id) {
  return request.delete(`/ai/page-template/${id}`)
}
