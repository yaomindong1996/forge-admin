import { ref } from 'vue'

export function useLowcodeFlowRuntime({ api, getInteraction, getTaskContext, confirmAction, promptActionInput, notify, reload }) {
  const flowHistory = ref([])
  const flowHistoryLoading = ref(false)

  async function loadFlowHistoryIfNeeded() {
    const context = getTaskContext()
    if (!getInteraction().timeline.enabled || !context.processInstanceId) {
      flowHistory.value = []
      return
    }
    flowHistoryLoading.value = true
    try {
      const response = await api.getFlowTaskHistory(context.processInstanceId)
      const data = response?.data || []
      flowHistory.value = Array.isArray(data) ? data : data.records || data.list || data.rows || []
    }
    catch (error) {
      flowHistory.value = []
      console.warn('[lowcode h5] flow history failed', error?.message || error)
    }
    finally {
      flowHistoryLoading.value = false
    }
  }

  async function runFlowAction(action) {
    const context = getTaskContext()
    if (!context.taskId) {
      notify('当前记录没有可办理的流程任务', { type: 'warning' })
      return
    }
    const operation = String(action.operation || 'approve')
    if (!await confirmAction(action)) return
    const comment = await promptActionInput({ label: '审批意见', name: 'comment', placeholder: '可填写审批意见' })
    if (comment === null) return
    let targetUserId = ''
    if (operation === 'delegate') {
      targetUserId = await promptActionInput({ label: '委派用户 ID', name: 'targetUserId', placeholder: '请输入目标用户 ID' })
      if (!targetUserId) return
    }
    const operationApi = {
      approve: api.approveFlowTask,
      reject: api.rejectFlowTask,
      return: api.returnFlowTask,
      delegate: api.delegateFlowTask,
    }[operation]
    if (!operationApi) {
      notify('不支持的流程操作', { type: 'warning' })
      return
    }
    await operationApi({
      action: operation,
      taskId: context.taskId,
      processInstanceId: context.processInstanceId || undefined,
      taskDefKey: context.taskDefKey || undefined,
      objectCode: context.objectCode || undefined,
      recordId: context.recordId || undefined,
      comment: String(comment || '').trim(),
      targetUserId: targetUserId || undefined,
    })
    notify(`${action.label || '流程操作'}成功`, { type: 'success' })
    if (context.recordId) await reload(context.recordId)
  }

  return { flowHistory, flowHistoryLoading, loadFlowHistoryIfNeeded, runFlowAction }
}
