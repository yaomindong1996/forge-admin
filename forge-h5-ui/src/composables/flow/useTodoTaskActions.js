import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import api from '@/api'
import { TODO_ACTION_LABELS as ACTION_LABELS, useTodoDetailStore } from '@/store'
import { showConfirmDialog } from '@/utils/dialog'
import { createFlowActionCredentials } from '@/utils/flow-action-idempotency'
import { compactObject as compact, resolveApiErrorMessage as resolveErrorMessage } from '@/utils/flow-page'
import { toast } from '@/utils/notify'
import { safeNavigateBack } from '@/utils/route'
import { isConfiguredBusinessTaskForm } from './useFlowBusinessForm'

/**
 * 审批详情的办理动作。审批动作属于不可并行的状态流转，接口返回前统一阻止重复操作。
 * @param {object} options
 * @param {object} options.businessForm reactive(useFlowBusinessForm(...))
 * @param {import('vue').Ref<string>} options.userId
 * @param {() => void} options.refresh
 * @param {() => void} options.scrollToApprovalComment
 */
export function useTodoTaskActions({ businessForm, userId, refresh, scrollToApprovalComment }) {
  const store = useTodoDetailStore()
  const {
    task, formInfo, businessContext, sourceMessageId, comment, signature, approvalPointChecks,
    actionLoading, pendingAction, claimLoading, moreVisible, rejectTargetVisible, selectedReturnTarget,
    delegateVisible, delegateUser, delegateComment, delegateSignature, requireComment, approvalPoints,
    canChooseReturnTarget, requireSignature,
  } = storeToRefs(store)
  const formSaving = ref(false)

  async function claimTask() {
    claimLoading.value = true
    try {
      await api.claimFlowTask(task.value.taskId || task.value.id, userId.value)
      toast('签收成功', { type: 'success' })
      await refresh()
    }
    catch (error) {
      console.error('签收失败:', error)
      toast(resolveErrorMessage(error, '签收失败'), { type: 'error' })
    }
    finally {
      claimLoading.value = false
    }
  }

  function submitMoreAction(action, signatureRef) {
    moreVisible.value = false
    submitAction(action, signatureRef)
  }

  function actionInputs(action, signatureRef) {
    const delegating = action === 'delegate'
    return {
      actionComment: delegating ? delegateComment.value : comment.value,
      actionSignature: delegating ? delegateSignature.value : signature.value,
      signatureRef,
    }
  }

  function validateAction(action, { actionComment, actionSignature, signatureRef }) {
    if (action === 'delegate' && !delegateUser.value) {
      toast('请选择转办人员', { type: 'warning' })
      return false
    }
    if (action === 'return' && canChooseReturnTarget.value && !selectedReturnTarget.value) {
      toast('请选择驳回至哪个已审批节点', { type: 'warning' })
      return false
    }
    if (requireComment.value && !actionComment.trim()) {
      toast('请输入审批意见', { type: 'warning' })
      if (action !== 'delegate') {
        moreVisible.value = false
        rejectTargetVisible.value = false
        scrollToApprovalComment()
      }
      return false
    }
    if (action === 'approve' && store.hasUncheckedRequiredPoints()) {
      toast('请完成全部必审要点', { type: 'warning' })
      return false
    }
    return businessForm.validateRequiredFields() && hasSignature(actionSignature, signatureRef)
  }

  async function submitAction(action, signatureRef) {
    if (actionLoading.value) return
    if (businessForm.blockedReason) return
    if (action === 'reject' && canChooseReturnTarget.value) {
      store.openRejectTarget()
      return
    }
    const inputs = actionInputs(action, signatureRef)
    if (!validateAction(action, inputs)) return
    const confirmed = await showConfirmDialog({
      title: `确认${ACTION_LABELS[action]}`,
      description: action === 'rejectToStart'
        ? '当前流程会保留并退回发起人，修改后可沿原流程重新提交。'
        : '提交后将按当前流程策略执行，不能撤销。',
      confirmText: ACTION_LABELS[action],
      isDestructive: ['reject', 'terminate'].includes(action),
    })
    if (!confirmed) return

    actionLoading.value = true
    pendingAction.value = action
    try {
      await executeAction(action, inputs)
      afterActionSuccess(action)
    }
    catch (error) {
      console.error('提交审批动作失败:', error)
      toast(resolveErrorMessage(error, `${ACTION_LABELS[action] || '提交'}失败`), { type: 'error' })
    }
    finally {
      actionLoading.value = false
      pendingAction.value = ''
    }
  }

  async function executeAction(action, { actionComment, actionSignature, signatureRef }) {
    const resolvedSignature = await resolveSignature(actionSignature, signatureRef)
    if (action === 'delegate') delegateSignature.value = resolvedSignature
    else signature.value = resolvedSignature
    const payload = buildActionPayload(action, actionComment, resolvedSignature)
    Object.assign(payload, await createFlowActionCredentials(action, payload.taskId, buildIdempotencyDigestPayload(payload)))
    if (isConfiguredBusinessTaskForm(businessContext.value) && ['approve', 'reject', 'rejectToStart', 'return'].includes(action)) {
      await api.completeBusinessTaskAction(payload)
    }
    else if (action === 'approve') await api.approveFlowTask(payload)
    else if (action === 'reject') await api.rejectFlowTask(payload)
    else if (action === 'rejectToStart') await api.rejectToStartFlowTask(payload)
    else if (action === 'return') await api.returnFlowTask({ ...payload, targetActivityId: selectedReturnTarget.value || undefined })
    else if (action === 'terminate') await api.terminateFlowTask(payload)
    else await api.delegateFlowTask(payload)
  }

  function afterActionSuccess(action) {
    if (sourceMessageId.value)
      api.markMessageRead(sourceMessageId.value).catch(error => console.warn('来源消息将由流程完成事件同步已读:', error))
    toast(`${ACTION_LABELS[action]}成功`, { type: 'success' })
    delegateVisible.value = false
    rejectTargetVisible.value = false
    // 主操作成功立即返回，不再被消息已读同步阻塞；来源列表 onShow 会立即重查。
    returnAfterSuccessfulAction()
  }

  function buildActionPayload(action, actionComment = comment.value.trim(), actionSignature = signature.value) {
    const info = formInfo.value || {}
    const context = businessContext.value || {}
    return compact({
      action,
      taskId: info.taskId || task.value?.taskId || task.value?.id,
      businessKey: context.businessKey || info.businessKey || task.value?.businessKey,
      processInstanceId: context.processInstanceId || info.processInstanceId || task.value?.processInstanceId,
      processDefKey: context.processDefKey || info.processDefKey || task.value?.processDefKey,
      taskDefKey: context.taskDefKey || info.taskDefKey || task.value?.taskDefKey,
      objectCode: context.objectCode || info.objectCode || task.value?.objectCode,
      recordId: context.recordId || info.recordId || task.value?.recordId,
      formKey: context.formKey || info.formKey,
      userId: userId.value,
      comment: actionComment.trim(),
      signature: actionSignature || undefined,
      targetActivityId: action === 'return' ? selectedReturnTarget.value || undefined : undefined,
      targetUserId: action === 'delegate' ? String(delegateUser.value?.id || '') : undefined,
      variables: { ...(info.variables || {}), ...businessForm.mainData },
      data: isConfiguredBusinessTaskForm(businessContext.value) && businessForm.businessFormHasWritableFields
        ? businessForm.buildCurrentBusinessFormData()
        : undefined,
      approvalPointResults: approvalPoints.value.map(point => ({
        id: point.id,
        content: point.content,
        required: point.required === true,
        checked: Boolean(approvalPointChecks.value?.[point.id]),
      })),
    })
  }

  async function saveBusinessFields() {
    if (formSaving.value || !businessForm.businessFormHasWritableFields || !businessForm.validateRequiredFields()) return
    formSaving.value = true
    try {
      const payload = buildActionPayload('approve')
      const res = await api.saveBusinessTaskFormContext({ ...payload, data: businessForm.buildCurrentBusinessFormData() })
      businessContext.value = res?.data || businessContext.value
      businessForm.applyBusinessContext(businessContext.value)
      toast('修改已暂存', { type: 'success' })
    }
    catch (error) {
      console.error('暂存业务表单失败:', error)
      toast(resolveErrorMessage(error, '暂存修改失败'), { type: 'error' })
    }
    finally {
      formSaving.value = false
    }
  }

  function hasSignature(value, signatureRef) {
    if (!requireSignature.value) return true
    if (String(value || '').trim()) return true
    if (signatureRef?.hasSignature?.()) return true
    toast('请完成手写签名', { type: 'warning' })
    return false
  }

  async function resolveSignature(value, signatureRef) {
    if (!requireSignature.value) return value || ''
    if (String(value || '').trim() && !signatureRef?.hasSignature?.()) return value
    return signatureRef?.upload ? signatureRef.upload() : value || ''
  }

  function returnAfterSuccessfulAction() {
    // 企微卡片 / 直链打开时页面栈常只有本页；H5 上盲目 navigateBack 会 history.back 到同 URL，看起来像刷新。
    // 先看栈深，能退就退；否则落到待办 Tab 或消息页。
    const fromMessage = Boolean(sourceMessageId.value)
    safeNavigateBack({
      fallback: fromMessage ? '/pages/message/index' : '/pages/todo',
      fallbackType: fromMessage ? 'reLaunch' : 'switchTab',
    })
  }

  return { formSaving, claimTask, submitAction, submitMoreAction, saveBusinessFields }
}

function buildIdempotencyDigestPayload(payload) {
  const { action, taskId, comment, signature, variables, data, targetActivityId, targetUserId, approvalPointResults } = payload
  return { action, taskId, comment, signature, variables, data, targetActivityId, targetUserId, approvalPointResults }
}
