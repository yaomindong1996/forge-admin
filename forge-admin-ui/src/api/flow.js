import { request } from '@/utils'

export default {
  // ========== 流程任务 ==========

  /**
   * 我的待办任务
   */
  getTodoTasks: (params) =>
    request.get('/api/flow/task/todo', { params }),

  /**
   * 我的已办任务
   */
  getDoneTasks: (params) =>
    request.get('/api/flow/task/done', { params }),

  /**
   * 我发起的流程
   */
  getStartedTasks: (params) =>
    request.get('/api/flow/task/started', { params }),

  /**
   * 候选任务（未签收）
   */
  getCandidateTasks: (params) =>
    request.get('/api/flow/task/candidate', { params }),

  /**
   * 签收任务
   */
  claimTask: (taskId, userId) =>
    request.post('/api/flow/task/claim', null, { params: { taskId, userId } }),

  /**
   * 审批通过
   */
  approveTask: (data) =>
    request.post('/api/flow/task/approve', data),

  /**
   * 审批驳回
   */
  rejectTask: (data) =>
    request.post('/api/flow/task/reject', data),

  /**
   * 转办任务
   */
  delegateTask: (data) =>
    request.post('/api/flow/task/delegate', data),

  /**
   * 撤回流程
   */
  withdrawProcess: (data) =>
    request.post('/api/flow/task/withdraw', data),

  /**
   * 获取任务详情
   */
  getTaskDetail: (taskId) =>
    request.get(`/api/flow/task/${taskId}`),

  /**
   * 获取流程图
   */
  getProcessDiagram: (processInstanceId) =>
    request.get(`/api/flow/task/diagram/${processInstanceId}`, { responseType: 'blob' }),

  /**
   * 催办任务
   */
  remindTask: (taskId) =>
    request.post('/api/flow/task/remind', null, { params: { taskId } }),

  // ========== 流程实例 ==========

  /**
   * 发起流程
   */
  startProcess: (modelKey, data) =>
    request.post(`/api/flow/instance/start/${modelKey}`, data),

  /**
   * 获取流程状态
   */
  getProcessStatus: (businessKey) =>
    request.get(`/api/flow/instance/status/${businessKey}`),

  /**
   * 终止流程
   */
  terminateProcess: (businessKey, data) =>
    request.post(`/api/flow/instance/terminate/${businessKey}`, data),

  /**
   * 删除流程实例
   */
  deleteProcess: (businessKey, userId) =>
    request.delete(`/api/flow/instance/${businessKey}`, { params: { userId } }),

  /**
   * 获取流程变量
   */
  getProcessVariables: (businessKey) =>
    request.get(`/api/flow/instance/variables/${businessKey}`),

  // ========== 流程模型 ==========

  /**
   * 分页查询流程模型
   */
  getModelPage: (params) =>
    request.get('/api/flow/model/page', { params }),

  /**
   * 获取模型详情
   */
  getModelDetail: (id) =>
    request.get(`/api/flow/model/${id}`),

  /**
   * 创建流程模型
   */
  createModel: (data) =>
    request.post('/api/flow/model', data),

  /**
   * 更新流程模型
   */
  updateModel: (data) =>
    request.put('/api/flow/model', data),

  /**
   * 删除流程模型
   */
  deleteModel: (id) =>
    request.delete(`/api/flow/model/${id}`),

  /**
   * 部署流程模型
   */
  deployModel: (id) =>
    request.post(`/api/flow/model/${id}/deploy`),

  /**
   * 禁用流程模型
   */
  disableModel: (id) =>
    request.post(`/api/flow/model/${id}/disable`),

  /**
   * 启用流程模型
   */
  enableModel: (id) =>
    request.post(`/api/flow/model/${id}/enable`),

  /**
   * 获取流程模型列表
   */
  getModelList: (params) =>
    request.get('/api/flow/model/list', { params }),

  // ========== 流程模板 ==========

  /**
   * 获取流程模板列表
   */
  getTemplateList: () =>
    request.get('/api/flow/template/list'),

  /**
   * 从模板创建流程模型
   */
  createFromTemplate: (templateKey, data) =>
    request.post(`/api/flow/template/create/${templateKey}`, data),

  /**
   * 获取模板详情
   */
  getTemplateDetail: (templateKey) =>
    request.get(`/api/flow/template/${templateKey}`),

  // ========== 流程分类 ==========

  /**
   * 获取启用的分类列表
   */
  getEnabledCategories: () =>
    request.get('/api/flow/category/enabled'),

  /**
   * 分页查询分类
   */
  getCategoryPage: (params) =>
    request.get('/api/flow/category/page', { params }),

  /**
   * 获取分类详情
   */
  getCategoryDetail: (id) =>
    request.get(`/api/flow/category/${id}`),

  /**
   * 创建分类
   */
  createCategory: (data) =>
    request.post('/api/flow/category', data),

  /**
   * 更新分类
   */
  updateCategory: (data) =>
    request.put('/api/flow/category', data),

  /**
   * 删除分类
   */
  deleteCategory: (id) =>
    request.delete(`/api/flow/category/${id}`),

  /**
   * 启用分类
   */
  enableCategory: (id) =>
    request.post(`/api/flow/category/${id}/enable`),

  /**
   * 禁用分类
   */
  disableCategory: (id) =>
    request.post(`/api/flow/category/${id}/disable`),

  // ========== 流程抄送 ==========

  /**
   * 发送抄送
   */
  sendCc: (data) =>
    request.post('/api/flow/cc/send', data),

  /**
   * 我的抄送
   */
  getMyCc: (params) =>
    request.get('/api/flow/cc/my', { params }),

  /**
   * 我发送的抄送
   */
  getSentCc: (params) =>
    request.get('/api/flow/cc/sent', { params }),

  /**
   * 标记已读
   */
  markCcRead: (id) =>
    request.post(`/api/flow/cc/read/${id}`),

  /**
   * 批量标记已读
   */
  batchMarkCcRead: (ids) =>
    request.post('/api/flow/cc/read/batch', ids),

  /**
   * 获取未读抄送数量
   */
  getUnreadCcCount: (userId) =>
    request.get('/api/flow/cc/unread/count', { params: { userId } }),

  // ========== 审批意见 ==========

  /**
   * 添加审批意见
   */
  addComment: (data) =>
    request.post('/api/flow/comment', data),

  /**
   * 获取流程审批意见（审批历史）
   */
  getProcessComments: (processInstanceId) =>
    request.get(`/api/flow/comment/process/${processInstanceId}`),

  /**
   * 获取任务审批意见
   */
  getTaskComments: (taskId) =>
    request.get(`/api/flow/comment/task/${taskId}`),
}