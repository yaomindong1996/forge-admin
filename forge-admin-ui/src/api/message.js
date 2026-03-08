import { request } from '@/utils'

export default {
  // ========== 消息管理 ==========
  
  /**
   * 分页查询当前用户消息列表
   */
  getMessagePage: (query, pageNum, pageSize) => 
    request.post(`/api/message/page?pageNum=${pageNum}&pageSize=${pageSize}`, query),
  
  /**
   * 查询消息详情
   */
  getMessageDetail: (messageId) => 
    request.get(`/api/message/${messageId}`),
  
  /**
   * 标记单条消息为已读
   */
  markMessageRead: (messageId) => 
    request.post(`/api/message/${messageId}/read`),
  
  /**
   * 批量标记为已读
   */
  markMessagesReadBatch: (messageIds) => 
    request.post('/api/message/read/batch', messageIds),
  
  /**
   * 全部标记为已读
   */
  markAllMessagesRead: () => 
    request.post('/api/message/read/all'),
  
  /**
   * 查询未读消息统计
   */
  getUnreadCount: () => 
    request.get('/api/message/unread/count'),
  
  /**
   * 发送消息
   */
  sendMessage: (data) => 
    request.post('/api/message/send', data),
  
  // ========== 消息模板管理 ==========
  
  /**
   * 分页查询模板列表
   */
  getTemplatePage: (params) => 
    request.get('/api/message/template/page', { params }),
  
  /**
   * 根据ID查询模板
   */
  getTemplateById: (id) => 
    request.get(`/api/message/template/${id}`),
  
  /**
   * 根据模板编码查询
   */
  getTemplateByCode: (templateCode) => 
    request.get(`/api/message/template/code/${templateCode}`),
  
  /**
   * 创建模板
   */
  createTemplate: (data) => 
    request.post('/api/message/template', data),
  
  /**
   * 更新模板
   */
  updateTemplate: (data) => 
    request.put('/api/message/template', data),
  
  /**
   * 删除模板
   */
  deleteTemplate: (id) => 
    request.delete(`/api/message/template/${id}`),
}
