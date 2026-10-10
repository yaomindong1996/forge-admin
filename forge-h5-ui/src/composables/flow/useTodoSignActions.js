import { storeToRefs } from 'pinia'
import api from '@/api'
import { useTodoDetailStore } from '@/store'
import { createFlowActionCredentials } from '@/utils/flow-action-idempotency'
import { compactObject as compact, resolveApiErrorMessage } from '@/utils/flow-page'
import { SIGN_MODE_PARALLEL } from '@/utils/flow-sign'
import { toast } from '@/utils/notify'

/**
 * 审批详情的加签/减签。操作人、50 人上限、节点"允许加签"都由后端校验，这里只做必填提示与重复提交加锁。
 * @param {object} options
 * @param {import('vue').Ref<string>} options.userId
 * @param {() => Promise<void>} options.refresh
 */
export function useTodoSignActions({ userId, refresh }) {
  const store = useTodoDetailStore()
  const {
    currentTaskId, signRelations, signVisible, signAction, signUser, signTargetId, signComment,
    actionLoading, pendingAction,
  } = storeToRefs(store)

  async function loadSignRelations() {
    if (!currentTaskId.value || !userId.value) return
    try {
      const response = await api.getFlowTaskSignRelations(currentTaskId.value, userId.value)
      signRelations.value = Array.isArray(response?.data) ? response.data : []
    }
    catch (error) {
      signRelations.value = []
      console.warn('读取加签记录失败:', error)
    }
  }

  function resolveTargetUserId() {
    if (signAction.value === 'addSign') return String(signUser.value?.id || signUser.value?.userId || '')
    return String(signTargetId.value || '')
  }

  async function submitSign() {
    if (actionLoading.value) return
    const action = signAction.value
    const targetUserId = resolveTargetUserId()
    if (!targetUserId) return toast(action === 'addSign' ? '请选择加签人员' : '请选择要移除的人员', { type: 'warning' })
    const payload = compact({
      taskId: currentTaskId.value,
      userId: userId.value,
      targetUserId,
      comment: signComment.value.trim(),
      signMode: SIGN_MODE_PARALLEL,
    })
    actionLoading.value = true
    pendingAction.value = action
    try {
      Object.assign(payload, await createFlowActionCredentials(action, payload.taskId, { ...payload }))
      if (action === 'addSign') await api.addFlowTaskSign(payload)
      else await api.reduceFlowTaskSign(payload)
      signVisible.value = false
      toast(action === 'addSign' ? '加签成功' : '减签成功', { type: 'success' })
      await refresh()
    }
    catch (error) {
      toast(resolveApiErrorMessage(error, action === 'addSign' ? '加签失败' : '减签失败'), { type: 'error' })
    }
    finally {
      actionLoading.value = false
      pendingAction.value = ''
    }
  }

  return { loadSignRelations, submitSign }
}
