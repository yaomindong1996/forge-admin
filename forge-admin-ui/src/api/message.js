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
  
  // ========== 消息管理（管理员） ==========
  
  /**
   * 分页查询所有消息列表（管理员）
   */
  getMessageManagePage: (query, pageNum, pageSize) => 
    request.post(`/api/message/manage/page?pageNum=${pageNum}&pageSize=${pageSize}`, query),
  
  /**
   * 查询消息详细信息（包含发送记录和接收人）
   */
  getMessageManageDetail: (messageId) => 
    request.get(`/api/message/manage/${messageId}/detail`),
  
  // ========== 消息业务类型管理 ==========
  
  /**
   * 分页查询业务类型
   */
  getBizTypePage: (params) => 
    request.get('/api/message/bizType/page', { params }),
  
  /**
   * 查询所有启用的业务类型
   */
  getBizTypeListEnabled: () => 
    request.get('/api/message/bizType/list/enabled'),
  
  /**
   * 根据ID查询业务类型
   */
  getBizTypeById: (id) => 
    request.get(`/api/message/bizType/${id}`),
  
  /**
   * 新增业务类型
   */
  createBizType: (data) => 
    request.post('/api/message/bizType', data),
  
  /**
   * 修改业务类型
   */
  updateBizType: (data) => 
    request.put('/api/message/bizType', data),
  
  /**
   * 删除业务类型
   */
  deleteBizType: (id) => 
    request.delete(`/api/message/bizType/${id}`)
}
