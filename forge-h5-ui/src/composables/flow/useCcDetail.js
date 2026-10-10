import { computed, reactive, ref } from 'vue'
import api from '@/api'
import { useCcStore } from '@/store'
import { isCcUnread } from '@/utils/flow-cc'
import { compactObject as compact, parseNestedJson as parseJson, resolveApiErrorMessage } from '@/utils/flow-page'
import { useFlowBusinessForm } from './useFlowBusinessForm'

/**
 * 抄送详情加载：抄送表单快照 → 只读业务上下文 → 流程进度/历史，并在进入时标记已读。
 * 抄送人不在任务可见范围内，不能复用按 taskId 加载的待办详情，只能走抄送和流程级接口。
 */
export function useCcDetail() {
  const ccStore = useCcStore()
  const ccId = ref('')
  const cc = ref(null)
  const formInfo = ref(null)
  const businessContext = ref(null)
  const businessContextError = ref('')
  const history = ref([])
  const diagramInfo = ref(null)
  const loading = ref(true)
  const formLoading = ref(true)
  const traceLoading = ref(true)
  const notFound = ref(false)

  const businessForm = reactive(useFlowBusinessForm({
    businessContext,
    formInfo,
    businessContextError,
    isReadonly: () => true,
  }))
  const processNodes = computed(() => (Array.isArray(diagramInfo.value?.nodes) ? diagramInfo.value.nodes : []))

  async function load(options = {}) {
    ccId.value = String(options.id || '')
    const cached = ccStore.current
    cc.value = cached && String(cached.id) === ccId.value ? cached : null
    if (!ccId.value) {
      notFound.value = true
      loading.value = false
      return
    }
    try {
      const response = await api.getCcFormInfo(ccId.value)
      formInfo.value = response?.data || null
    }
    catch (error) {
      console.error('加载抄送详情失败:', error)
      notFound.value = !cc.value
    }
    finally {
      loading.value = false
    }
    const processInstanceId = options.processInstanceId || cc.value?.processInstanceId || formInfo.value?.processInstanceId
    markRead()
    await Promise.all([loadTrace(processInstanceId), loadBusinessContext(processInstanceId)])
  }

  async function loadTrace(processInstanceId) {
    traceLoading.value = true
    if (processInstanceId) {
      const [historyResult, diagramResult] = await Promise.allSettled([
        api.getFlowTaskHistory(processInstanceId),
        api.getFlowDiagramInfo(processInstanceId),
      ])
      if (historyResult.status === 'fulfilled')
        history.value = Array.isArray(historyResult.value?.data) ? historyResult.value.data : []
      if (diagramResult.status === 'fulfilled') diagramInfo.value = diagramResult.value?.data || null
    }
    traceLoading.value = false
  }

  async function loadBusinessContext(processInstanceId) {
    formLoading.value = true
    const query = buildContextQuery(processInstanceId)
    try {
      if (!query.processInstanceId && !query.businessKey && !(query.objectCode && query.recordId)) return
      const response = await api.getBusinessTaskReadonlyContext(query)
      businessContext.value = response?.data || null
      businessForm.applyBusinessContext(businessContext.value)
      await businessForm.loadDictOptions()
    }
    catch (error) {
      // 只读上下文按流程可见性放行；抄送人被拒绝时降级为 PC 端查看提示，不打断页面。
      businessContext.value = null
      businessContextError.value = resolveApiErrorMessage(error, '业务表单暂不可在移动端查看')
      console.warn('抄送业务表单不可用:', businessContextError.value)
    }
    finally {
      formLoading.value = false
    }
  }

  function buildContextQuery(processInstanceId) {
    const info = formInfo.value || {}
    const record = cc.value || {}
    const rawRef = parseJson(info.formJson, {})
    const formRef = info.formRef || (rawRef && !Array.isArray(rawRef) ? rawRef.formRef || rawRef : {})
    return compact({
      taskId: info.taskId || record.taskId,
      processInstanceId,
      businessKey: info.businessKey || record.businessKey,
      processDefKey: info.processDefKey || record.processDefKey,
      taskDefKey: info.taskDefKey,
      objectCode: info.objectCode || formRef.objectCode || record.objectCode,
      recordId: info.recordId || formRef.recordId || record.recordId,
      formKey: info.formKey || formRef.formKey,
    })
  }

  // 列表进入时按缓存判断是否未读；消息等外部入口没有缓存，直接标记后重新拉取未读数。
  function markRead() {
    const request = id => api.markCcRead(id)
    const task = cc.value
      ? (isCcUnread(cc.value, ccStore.readIds) ? ccStore.markRead(ccId.value, request) : null)
      : request(ccId.value).then(() => ccStore.loadUnreadCount(api.getCcUnreadCount))
    task?.catch(error => console.warn('标记抄送已读失败:', error))
  }

  return {
    cc, formInfo, businessForm, businessContextError, history, processNodes,
    loading, formLoading, traceLoading, notFound, load,
  }
}
