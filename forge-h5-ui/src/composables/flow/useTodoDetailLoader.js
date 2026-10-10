import { storeToRefs } from 'pinia'
import api from '@/api'
import { useTodoDetailStore } from '@/store'
import { compactObject as compact, parseNestedJson as parseJson, resolveApiErrorMessage as resolveErrorMessage } from '@/utils/flow-page'
import { toast } from '@/utils/notify'

/**
 * 审批详情加载：任务摘要 → 业务表单上下文 → Flow 表单快照 → 流程进度/历史。
 * @param {object} businessForm reactive(useFlowBusinessForm(...))
 */
export function useTodoDetailLoader(businessForm) {
  const store = useTodoDetailStore()
  const {
    taskId, task, formInfo, businessContext, businessContextError, history, diagramInfo,
    loading, formLoading, historyLoading, diagramLoading, readonlyMode,
  } = storeToRefs(store)

  function resetLoadingState() {
    loading.value = true
    formLoading.value = true
    historyLoading.value = true
    diagramLoading.value = true
    formInfo.value = null
    businessContext.value = null
    businessContextError.value = ''
    history.value = []
    diagramInfo.value = null
    businessForm.resetBusinessData()
  }

  async function refresh() {
    if (!taskId.value) { loading.value = false; formLoading.value = false; return }
    resetLoadingState()
    try {
      await loadTaskSummary()
      if (!task.value) return
      const currentTaskId = task.value.taskId || task.value.id || taskId.value
      const tracePromise = loadTrace(task.value.processInstanceId)
      const context = readonlyMode.value
        ? await loadReadonlyBusinessContext({ taskId: currentTaskId })
        : await loadBusinessContext({ taskId: currentTaskId })
      // 与 PC 的 loadTaskFormBundle 保持一致：优先复用业务上下文随响应携带的
      // taskFormInfo JSON 快照，避免再次请求 Flow 后拿到不完整旧协议而渲染空白。
      if (context?.taskFormInfo && typeof context.taskFormInfo === 'object')
        formInfo.value = context.taskFormInfo
      if (!formInfo.value) await loadFlowFormInfo(currentTaskId)
      store.seedApprovalPointChecks(formInfo.value)
      await businessForm.loadDictOptions()
      applyTrace(await tracePromise)
    }
    catch (error) {
      console.error('加载审批详情失败:', error)
      toast(resolveErrorMessage(error, '审批详情加载失败'), { type: 'error' })
    }
    finally {
      loading.value = false
      formLoading.value = false
      historyLoading.value = false
      diagramLoading.value = false
    }
  }

  async function loadTaskSummary() {
    task.value = readCachedTask(taskId.value)
    try {
      const detail = await api.getFlowTaskDetail(taskId.value)
      task.value = detail?.data || task.value
    }
    catch (error) {
      if (!task.value) throw error
      console.warn('读取运行中任务详情失败，改用列表摘要:', error)
    }
  }

  function loadTrace(processInstanceId) {
    return Promise.allSettled([
      processInstanceId ? api.getFlowTaskHistory(processInstanceId) : Promise.resolve({ data: [] }),
      processInstanceId ? api.getFlowDiagramInfo(processInstanceId) : Promise.resolve({ data: null }),
    ])
  }

  function applyTrace([historyResult, diagramResult]) {
    if (historyResult.status === 'fulfilled')
      history.value = Array.isArray(historyResult.value?.data) ? historyResult.value.data : []
    if (diagramResult.status === 'fulfilled') diagramInfo.value = diagramResult.value?.data || null
  }

  async function loadFlowFormInfo(currentTaskId) {
    try {
      const response = readonlyMode.value
        ? await api.getFlowProcessForm(compact({
          taskId: currentTaskId,
          processInstanceId: task.value.processInstanceId,
          businessKey: task.value.businessKey,
          processDefKey: task.value.processDefKey || task.value.processDefinitionKey,
          taskDefKey: task.value.taskDefKey || task.value.taskDefinitionKey,
        }))
        : await api.getFlowTaskForm(currentTaskId)
      formInfo.value = response?.data || null
    }
    catch (error) { console.warn('读取流程任务表单失败:', error) }
  }

  async function loadReadonlyBusinessContext(overrides = {}) {
    const query = buildBusinessContextQuery(overrides)
    if (!hasBusinessContextQuery(query)) return null
    try {
      const res = await api.getBusinessTaskReadonlyContext(query)
      businessContext.value = res?.data || null
      businessForm.applyBusinessContext(businessContext.value)
      return businessContext.value
    }
    catch (error) {
      console.error('加载只读业务表单失败:', error)
      businessContext.value = null
      businessContextError.value = resolveErrorMessage(error, '接口未返回业务表单')
      return null
    }
  }

  async function loadBusinessContext(overrides = {}) {
    const query = buildBusinessContextQuery(overrides)
    if (!hasBusinessContextQuery(query)) return null
    try {
      const res = await api.getBusinessTaskFormContext(query)
      businessContext.value = res?.data || null
      businessForm.applyBusinessContext(businessContext.value)
      return businessContext.value
    }
    catch (error) {
      // 审批业务表单只能信任 task-form-context 返回的记录、字段和节点权限。
      // 接口失败时禁止用表单资产或流程变量拼装可编辑表单，避免展示错误数据。
      businessContext.value = null
      businessContextError.value = resolveErrorMessage(error, '接口未返回业务表单')
      console.warn('业务表单上下文不可用，已停止渲染动态表单:', businessContextError.value)
      return null
    }
  }

  function buildBusinessContextQuery(overrides = {}) {
    const info = formInfo.value || {}
    const rawRef = parseJson(info.formJson, {})
    const formRef = info.formRef || (rawRef && !Array.isArray(rawRef) ? rawRef.formRef || rawRef : {})
    return compact({
      taskId: overrides.taskId || info.taskId || task.value?.taskId || taskId.value,
      businessKey: info.businessKey || task.value?.businessKey,
      processInstanceId: info.processInstanceId || task.value?.processInstanceId,
      processDefKey: info.processDefKey || task.value?.processDefKey || task.value?.processDefinitionKey,
      taskDefKey: info.taskDefKey || task.value?.taskDefKey || task.value?.taskDefinitionKey,
      objectCode: info.objectCode || formRef.objectCode || task.value?.objectCode,
      recordId: info.recordId || formRef.recordId || task.value?.recordId,
      formKey: info.formKey || formRef.formKey,
    })
  }

  return { refresh }
}

function hasBusinessContextQuery(query) {
  return Boolean(query.taskId || query.businessKey || query.processInstanceId || (query.objectCode && query.recordId))
}

function readCachedTask(id) {
  try { return uni.getStorageSync(`flow-task:${id}`) || null }
  catch { return null }
}
