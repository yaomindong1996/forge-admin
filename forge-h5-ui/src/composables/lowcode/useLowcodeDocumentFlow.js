import api from '@/api'
import {
  CREATE_SUBMIT_KEY,
  useAuthStore,
  useBadgeStore,
  useDocumentFlowStore,
  useInitiatorSelectStore,
} from '@/store'
import { showConfirmDialog } from '@/utils/dialog'
import { DOCUMENT_FLOW_ACTION } from '@/utils/document-flow-actions'
import { normalizeInitiatorSelectNodes } from '@/utils/initiator-select'
import { buildFlowTaskDetailUrl } from '@/utils/message-flow-navigation'
import { toast } from '@/utils/notify'
import { FLOW_PERMISSIONS } from '@/utils/permission'

/**
 * 低代码单据审批：按钮来自后端运行态 runtimeActions，前端只负责交互编排。
 * getContext 返回 { objectCode, recordId, mode }；persist 复用页面保存逻辑并返回保存结果。
 */
export function useLowcodeDocumentFlow({ getContext, validate, persist, reloadDetail, handleError }) {
  const store = useDocumentFlowStore()
  const authStore = useAuthStore()
  const initiatorStore = useInitiatorSelectStore()
  const badgeStore = useBadgeStore()
  let loadSeq = 0

  // —— 运行态加载 ——
  async function load() {
    const { objectCode, recordId } = getContext()
    const seq = ++loadSeq
    // 先清空，避免切换记录时按钮仍指向上一条单据的流程实例。
    store.setRuntime(null)
    syncPermissions()
    if (!objectCode || !recordId || !authStore.hasPermission(FLOW_PERMISSIONS.runtime)) {
      store.setRuntime(null)
      return null
    }
    try {
      const response = await api.getBusinessDocumentRuntime(objectCode, recordId)
      if (seq === loadSeq) store.setRuntime(response?.data || null)
    }
    catch (error) {
      // 未启用单据模式的对象同样会走到这里，详情页不展示审批区即可。
      if (seq === loadSeq) store.setRuntime(null)
    }
    return store.runtime
  }

  function reset() {
    loadSeq += 1
    store.reset()
  }

  function setCreateSubmit(enabled) {
    store.createSubmit = Boolean(enabled)
    syncPermissions()
  }

  function syncPermissions() {
    store.permissions = {
      start: authStore.hasPermission(FLOW_PERMISSIONS.start),
      withdraw: authStore.hasPermission(FLOW_PERMISSIONS.withdraw),
    }
  }

  // —— 动作分发 ——
  async function run(button) {
    if (!button || button.disabled || store.loadingKey) return
    if (button.actionType === DOCUMENT_FLOW_ACTION.HANDLE) {
      openTask()
      return
    }
    store.loadingKey = button.key
    try {
      if (button.key === CREATE_SUBMIT_KEY) await submitCreated()
      else if (button.actionType === DOCUMENT_FLOW_ACTION.START) await submitExisting()
      else if (button.actionType === DOCUMENT_FLOW_ACTION.RESUBMIT) await resubmit()
      else if (button.actionType === DOCUMENT_FLOW_ACTION.WITHDRAW) await withdraw()
    }
    catch (error) {
      handleError(error)
    }
    finally {
      store.loadingKey = ''
    }
  }

  async function confirm(title, description, isDestructive = false) {
    return showConfirmDialog({ title, description, isDestructive })
  }

  // 新建/编辑态先保存表单，详情态直接使用已保存记录。
  async function saveIfEditing() {
    if (getContext().mode === 'detail') return true
    const saved = await persist({ validate: false, navigate: false, notify: false, requireRecordId: true })
    return Boolean(saved)
  }

  async function submitCreated() {
    if (!validate()) return
    if (!await confirm('提交审批', '将先保存当前填写内容，再提交审批，确认继续吗？')) return
    if (!await saveIfEditing()) return
    const runtime = await load()
    const canStart = (runtime?.runtimeActions || []).some(action =>
      action?.visible !== false && String(action?.actionType || action?.key || '').toUpperCase() === DOCUMENT_FLOW_ACTION.START)
    if (!canStart) {
      toast('已保存，当前单据暂不可提交审批', { type: 'warning' })
      await reloadDetail(getContext().recordId)
      return
    }
    await startFlow()
  }

  async function submitExisting() {
    if (getContext().mode !== 'detail' && !validate()) return
    if (!await confirm('提交审批', '确认提交该单据进入审批吗？')) return
    if (!await saveIfEditing()) return
    await startFlow()
  }

  // 自选审批人节点必须全部选择后才能发起，取消选择则不发起。
  async function startFlow() {
    const { objectCode, recordId } = getContext()
    const configResponse = await api.getBusinessFlowStartConfig(objectCode)
    const nodes = configResponse?.data?.initiatorSelectNodes || []
    let variables = {}
    if (normalizeInitiatorSelectNodes(nodes).length) {
      const selections = await initiatorStore.open(nodes)
      if (!selections) {
        toast('已取消提交，单据已保存', { type: 'info' })
        await reloadDetail(recordId)
        return
      }
      variables = { PROCESS_START_USER: selections }
    }
    await api.startBusinessDocumentFlow({ objectCode, recordId, variables })
    await afterSuccess('已提交审批', recordId)
  }

  async function resubmit() {
    const runtime = store.runtime || {}
    const task = runtime.myTask || {}
    if (!task.taskId) throw new Error('未找到待重新提交的任务，请刷新后重试')
    if (getContext().mode !== 'detail' && !validate()) return
    if (!await confirm('重新提交', '确认按当前内容重新提交审批吗？')) return
    if (!await saveIfEditing()) return
    await api.resubmitBusinessDocumentFlow({
      taskId: task.taskId,
      taskDefKey: task.taskDefKey,
      processInstanceId: task.processInstanceId || runtime.processInstanceId,
      businessKey: runtime.businessKey,
    })
    await afterSuccess('已重新提交', getContext().recordId)
  }

  async function withdraw() {
    const runtime = store.runtime || {}
    const { objectCode, recordId } = getContext()
    if (!await confirm('撤回审批', '撤回后流程将终止，可修改后重新提交，确认撤回吗？', true)) return
    await api.withdrawBusinessDocumentFlow({
      objectCode,
      recordId,
      processInstanceId: runtime.processInstanceId,
      businessKey: runtime.businessKey,
      comment: '申请人撤回',
    })
    await afterSuccess('已撤回', recordId)
  }

  function openTask() {
    const url = buildFlowTaskDetailUrl(store.runtime?.myTask?.taskId)
    if (!url) {
      toast('未找到待办任务，请刷新后重试', { type: 'warning' })
      return
    }
    uni.navigateTo({ url })
  }

  async function afterSuccess(message, recordId) {
    toast(message, { type: 'success' })
    badgeStore.refresh().catch(() => {})
    await reloadDetail(recordId)
  }

  return { load, reset, run, setCreateSubmit }
}
