import { request } from '@/utils'

export default {
  // ========== 流程任务 ==========

  /**
   * 我的待办任务
   */
  getTodoTasks: (params, config = {}) =>
    request.get('/api/flow/task/todo', { ...config, params }),

  /**
   * 我的已办任务
   */
  getDoneTasks: (params, config = {}) =>
    request.get('/api/flow/task/done', { ...config, params }),

  /**
   * 我发起的流程
   */
  getStartedTasks: (params, config = {}) =>
    request.get('/api/flow/task/started', { ...config, params }),

  /**
   * 候选任务（未签收）
   */
  getCandidateTasks: (params, config = {}) =>
    request.get('/api/flow/task/candidate', { ...config, params }),

  /**
   * 签收任务
   */
  claimTask: (taskId, userId) =>
    request.post('/api/flow/task/claim', null, { params: { taskId, userId } }),

  /**
   * 审批通过
   */
  approveTask: data =>
    request.post('/api/flow/task/approve', data),

  /**
   * 审批驳回
   */
  rejectTask: data =>
    request.post('/api/flow/task/reject', data),

  /**
   * 转办任务
   */
  delegateTask: data =>
    request.post('/api/flow/task/delegate', data),

  /**
   * 退回上一审批节点
   */
  returnTask: data =>
    request.post('/api/flow/task/return', data),

  /**
   * 终结流程
   */
  terminateTask: data =>
    request.post('/api/flow/task/terminate', data),

  /**
   * 撤回流程
   */
  withdrawProcess: data =>
    request.post('/api/flow/task/withdraw', data),

  /**
   * 获取任务详情
   */
  getTaskDetail: taskId =>
    request.get(`/api/flow/task/${taskId}`),

  /**
   * 获取流程图
   */
  getProcessDiagram: processInstanceId =>
    request.get(`/api/flow/task/diagram/${processInstanceId}`, { responseType: 'blob' }),

  /**
   * 获取流程图详情信息（包含节点状态）
   */
  getProcessDiagramInfo: (processInstanceId, params = {}) =>
    request.get(`/api/flow/task/diagram-info/${processInstanceId}`, { params }),

  /**
   * 获取流程审批时间轴
   */
  getProcessHistory: processInstanceId =>
    request.get(`/api/flow/task/history/${processInstanceId}`),

  /**
   * 获取任务表单信息（表单类型、formUrl、流程变量等）
   */
  getTaskFormInfo: taskId =>
    request.get(`/api/flow/task/form/${taskId}`),

  /**
   * 获取流程表单只读信息（已办、抄送、历史查看）
   */
  getProcessFormInfo: params =>
    request.get('/api/flow/task/form', { params }),

  /**
   * 催办任务
   */
  remindTask: taskId =>
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
  getProcessStatus: businessKey =>
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
  getProcessVariables: businessKey =>
    request.get(`/api/flow/instance/variables/${businessKey}`),

  // ========== 流程模型 ==========

  /**
   * 分页查询流程模型
   */
  getModelPage: params =>
    request.get('/api/flow/model/page', { params }),

  /**
   * 获取流程模型状态统计
   */
  getModelStatistics: params =>
    request.get('/api/flow/model/statistics', { params }),

  /**
   * 获取模型详情
   */
  getModelDetail: id =>
    request.get(`/api/flow/model/${id}`),

  /**
   * 创建流程模型
   */
  createModel: data =>
    request.post('/api/flow/model', data),

  /**
   * 更新流程模型
   */
  updateModel: data =>
    request.put('/api/flow/model', data),

  /**
   * 删除流程模型
   */
  deleteModel: id =>
    request.delete(`/api/flow/model/${id}`),

  /**
   * 部署流程模型
   */
  deployModel: id =>
    request.post(`/api/flow/model/${id}/deploy`),

  /**
   * 禁用流程模型
   */
  disableModel: id =>
    request.post(`/api/flow/model/${id}/disable`),

  /**
   * 启用流程模型
   */
  enableModel: id =>
    request.post(`/api/flow/model/${id}/enable`),

  /**
   * 挂起流程模型
   */
  suspendModel: id =>
    request.post(`/api/flow/model/${id}/suspend`),

  /**
   * 激活流程模型
   */
  activateModel: id =>
    request.post(`/api/flow/model/${id}/activate`),

  /**
   * 复制流程模型
   */
  copyModel: (id, newName) =>
    request.post(`/api/flow/model/${id}/copy`, null, {
      params: { newName },
    }),

  /**
   * 获取流程模型列表
   */
  getModelList: params =>
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

  // ========== 流程分类 ==========

  /**
   * 获取启用的分类列表
   */
  getEnabledCategories: () =>
    request.get('/api/flow/category/enabled'),

  /**
   * 获取分类树形列表（用于管理页面）
   */
  getCategoryTree: () =>
    request.get('/api/flow/category/tree'),

  /**
   * 获取分类下拉树（用于选择器）
   * @param onlyLeaf 是否只返回叶子节点
   */
  getCategoryTreeSelect: (onlyLeaf = true) =>
    request.get('/api/flow/category/tree-select', { params: { onlyLeaf } }),

  /**
   * 分页查询分类
   */
  getCategoryPage: params =>
    request.get('/api/flow/category/page', { params }),

  /**
   * 获取分类详情
   */
  getCategoryDetail: id =>
    request.get(`/api/flow/category/${id}`),

  /**
   * 创建分类
   */
  createCategory: data =>
    request.post('/api/flow/category', data),

  /**
   * 更新分类
   */
  updateCategory: data =>
    request.put('/api/flow/category', data),

  /**
   * 删除分类
   */
  deleteCategory: id =>
    request.delete(`/api/flow/category/${id}`),

  /**
   * 启用分类
   */
  enableCategory: id =>
    request.post(`/api/flow/category/${id}/enable`),

  /**
   * 禁用分类
   */
  disableCategory: id =>
    request.post(`/api/flow/category/${id}/disable`),

  // ========== 流程抄送 ==========

  /**
   * 发送抄送
   */
  sendCc: data =>
    request.post('/api/flow/cc/send', data),

  /**
   * 我的抄送
   */
  getMyCc: params =>
    request.get('/api/flow/cc/my', { params }),

  /**
   * 我发送的抄送
   */
  getSentCc: params =>
    request.get('/api/flow/cc/sent', { params }),

  /**
   * 获取抄送关联业务表单
   */
  getCcFormInfo: id =>
    request.get(`/api/flow/cc/form/${id}`),

  /**
   * 标记已读
   */
  markCcRead: id =>
    request.post(`/api/flow/cc/read/${id}`),

  /**
   * 批量标记已读
   */
  batchMarkCcRead: ids =>
    request.post('/api/flow/cc/read/batch', ids),

  /**
   * 获取未读抄送数量
   */
  getUnreadCcCount: userId =>
    request.get('/api/flow/cc/unread/count', { params: { userId } }),

  // ========== 审批意见 ==========

  /**
   * 添加审批意见
   */
  addComment: data =>
    request.post('/api/flow/comment', data),

  /**
   * 获取流程审批意见（审批历史）
   */
  getProcessComments: processInstanceId =>
    request.get(`/api/flow/comment/process/${processInstanceId}`),

  /**
   * 获取任务审批意见
   */
  getTaskComments: taskId =>
    request.get(`/api/flow/comment/task/${taskId}`),

  // ========== 流程模板 ==========

  /**
   * 分页查询流程模板
   */
  getTemplatePage: params =>
    request.get('/api/flow/template/page', { params }),

  /**
   * 获取启用的模板列表
   */
  getEnabledTemplates: category =>
    request.get('/api/flow/template/enabled', { params: { category } }),

  /**
   * 获取模板详情
   */
  getTemplateDetail: id =>
    request.get(`/api/flow/template/${id}`),

  /**
   * 根据Key获取模板详情
   */
  getTemplateByKey: templateKey =>
    request.get(`/api/flow/template/key/${templateKey}`),

  /**
   * 创建模板
   */
  createTemplate: data =>
    request.post('/api/flow/template', data),

  /**
   * 更新模板
   */
  updateTemplate: data =>
    request.put('/api/flow/template', data),

  /**
   * 删除模板
   */
  deleteTemplate: id =>
    request.delete(`/api/flow/template/${id}`),

  /**
   * 启用模板
   */
  enableTemplate: id =>
    request.post(`/api/flow/template/${id}/enable`),

  /**
   * 禁用模板
   */
  disableTemplate: id =>
    request.post(`/api/flow/template/${id}/disable`),

  /**
   * 从模板创建流程模型
   */
  createModelFromTemplate: (templateKey, modelName, modelKey) =>
    request.post(`/api/flow/template/createModel/${templateKey}`, null, {
      params: { modelName, modelKey },
    }),

  /**
   * 复制模板
   */
  copyTemplate: (id, newName) =>
    request.post(`/api/flow/template/copy/${id}`, null, {
      params: { newName },
    }),

  // ========== 表单定义 ==========

  /**
   * 获取表单定义分页列表
   */
  getFormPage: params =>
    request.get('/api/flow/form/page', { params }),

  /**
   * 获取所有启用的表单定义
   */
  getEnabledForms: () =>
    request.get('/api/flow/form/enabled'),

  /**
   * 获取表单定义详情
   */
  getFormById: id =>
    request.get(`/api/flow/form/${id}`),

  /**
   * 根据表单Key获取表单定义
   */
  getFormByKey: formKey =>
    request.get(`/api/flow/form/key/${formKey}`),

  /**
   * 创建表单定义
   */
  createForm: data =>
    request.post('/api/flow/form', data),

  /**
   * 更新表单定义
   */
  updateForm: data =>
    request.put('/api/flow/form', data),

  /**
   * 删除表单定义
   */
  deleteForm: id =>
    request.delete(`/api/flow/form/${id}`),

  /**
   * 启用表单
   */
  enableForm: id =>
    request.post(`/api/flow/form/${id}/enable`),

  /**
   * 禁用表单
   */
  disableForm: id =>
    request.post(`/api/flow/form/${id}/disable`),

  /**
   * 复制表单
   */
  copyForm: (id, newName) =>
    request.post(`/api/flow/form/${id}/copy`, null, {
      params: { newName },
    }),

  /**
   * 检查表单Key是否存在
   */
  checkFormKeyExists: (formKey, excludeId) =>
    request.get('/api/flow/form/checkKey', {
      params: { formKey, excludeId },
    }),

  /**
   * 预览表单（获取表单Schema）
   */
  previewForm: id =>
    request.get(`/api/flow/form/${id}/preview`),

  /**
   * 发布表单版本
   */
  publishForm: id =>
    request.post(`/api/flow/form/${id}/publish`),

  /**
   * 获取表单版本
   */
  getFormVersions: id =>
    request.get(`/api/flow/form/${id}/versions`),

  /**
   * 获取表单字段目录
   */
  getFormFieldCatalog: params =>
    request.get('/api/flow/form/field-catalog', { params }),

  // ========== 流程入口 ==========

  getEntryPage: params =>
    request.get('/api/flow/entry/page', { params }),

  getEntryDetail: id =>
    request.get(`/api/flow/entry/${id}`),

  createEntry: data =>
    request.post('/api/flow/entry', data),

  updateEntry: data =>
    request.put('/api/flow/entry', data),

  deleteEntry: id =>
    request.delete(`/api/flow/entry/${id}`),

  getRuntimeEntry: entryCode =>
    request.get(`/api/flow/runtime/entry/${entryCode}`),

  submitEntryForm: (entryCode, data) =>
    request.post(`/api/flow/runtime/submit/${entryCode}`, data),

  getRuntimeInstance: processInstanceId =>
    request.get(`/api/flow/runtime/instance/${processInstanceId}`),

  // ========== 组织填报批次 ==========

  getFillBatchPage: params =>
    request.get('/api/flow/fill-batch/page', { params }),

  createFillBatch: data =>
    request.post('/api/flow/fill-batch', data),

  updateFillBatch: data =>
    request.put('/api/flow/fill-batch', data),

  publishFillBatch: id =>
    request.post(`/api/flow/fill-batch/${id}/publish`),

  deleteFillBatch: id =>
    request.delete(`/api/flow/fill-batch/${id}`),

  getFillBatchItems: id =>
    request.get(`/api/flow/fill-batch/${id}/items`),
}
