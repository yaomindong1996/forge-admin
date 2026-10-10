import { defineStore } from 'pinia'
import { activeSignRelations, canAddSign, canReduceSign } from '../../utils/flow-sign.js'

export const TODO_ACTION_LABELS = Object.freeze({
  approve: '同意',
  reject: '驳回',
  rejectToStart: '退回发起人修改',
  return: '退回',
  terminate: '终结流程',
  delegate: '转办',
  addSign: '加签',
  reduceSign: '减签',
})

function initialState() {
  return {
    taskId: '',
    sourceMessageId: '',
    pageMode: 'todo',
    task: null,
    formInfo: null,
    businessContext: null,
    businessContextError: '',
    history: [],
    diagramInfo: null,
    loading: true,
    formLoading: true,
    historyLoading: true,
    diagramLoading: true,
    comment: '',
    signature: '',
    approvalPointChecks: {},
    actionLoading: false,
    pendingAction: '',
    claimLoading: false,
    moreVisible: false,
    rejectTargetVisible: false,
    selectedReturnTarget: '',
    delegateVisible: false,
    delegateUser: null,
    delegateComment: '',
    delegateSignature: '',
    currentUserId: '',
    signRelations: [],
    signVisible: false,
    signAction: 'addSign',
    signUser: null,
    signTargetId: '',
    signComment: '',
  }
}

export const useTodoDetailStore = defineStore('todoDetail', {
  state: initialState,
  getters: {
    readonlyMode: state => state.pageMode === 'readonly',
    // 业务表单上下文携带服务端最终策略，Flow 表单快照仅覆盖其中明确返回的属性。
    policy: state => ({ ...(state.businessContext || {}), ...(state.formInfo || {}) }),
    requireComment() { return this.policy.requireComment !== false },
    requireSignature() { return this.policy.requireSignature === true },
    canApprove() { return this.policy.allowApprove !== false },
    canReject() { return this.policy.allowReject !== false },
    canRejectToStart() { return this.policy.allowRejectToStart === true },
    canDelegate() { return this.policy.allowDelegate !== false },
    canTerminate() { return this.policy.allowTerminate === true },
    returnTargetOptions() {
      const targets = Array.isArray(this.policy.returnTargets) ? this.policy.returnTargets : []
      return targets.map(item => ({ label: item.activityName || item.activityId, value: item.activityId }))
    },
    canChooseReturnTarget() {
      return this.policy.allowMultiReturn === true && this.returnTargetOptions.length > 0
    },
    canAddSign() {
      return canAddSign({ task: this.task, userId: this.currentUserId, policy: this.policy, readonly: this.readonlyMode })
    },
    canReduceSign() {
      return canReduceSign({
        task: this.task, userId: this.currentUserId, relations: this.signRelations, readonly: this.readonlyMode,
      })
    },
    activeSignRelations: state => activeSignRelations(state.signRelations),
    hasMoreActions() {
      return this.canRejectToStart || this.canDelegate || this.canTerminate || this.canAddSign || this.canReduceSign
    },
    isCandidateTask: state => Number(state.task?.status) === 0 && !state.task?.assignee,
    responsibilityDescription: state => state.formInfo?.responsibilityDescription || '',
    approvalPoints: state => (Array.isArray(state.formInfo?.approvalPoints) ? state.formInfo.approvalPoints : []),
    processNodes: state => (Array.isArray(state.diagramInfo?.nodes) ? state.diagramInfo.nodes : []),
    actionLoadingText: state => `${TODO_ACTION_LABELS[state.pendingAction] || '提交'}中...`,
    currentTaskId: state => String(state.task?.taskId || state.task?.id || state.taskId || ''),
  },
  actions: {
    init(options = {}, currentUserId = '') {
      this.$reset()
      this.taskId = String(options.taskId || '')
      this.sourceMessageId = String(options.messageId || '')
      this.pageMode = options.mode === 'readonly' ? 'readonly' : 'todo'
      this.currentUserId = String(currentUserId || '')
    },
    seedApprovalPointChecks(info) {
      const points = Array.isArray(info?.approvalPoints) ? info.approvalPoints : []
      this.approvalPointChecks = Object.fromEntries(points.map(point => [point.id, false]))
    },
    toggleApprovalPoint(point) {
      if (this.readonlyMode || !point?.id) return
      this.approvalPointChecks = { ...this.approvalPointChecks, [point.id]: !this.approvalPointChecks[point.id] }
    },
    hasUncheckedRequiredPoints() {
      return this.approvalPoints.some(point => point?.required === true && !this.approvalPointChecks?.[point.id])
    },
    openRejectTarget() {
      this.selectedReturnTarget = ''
      this.rejectTargetVisible = true
    },
    openDelegate() {
      this.moreVisible = false
      this.delegateUser = null
      this.delegateComment = ''
      this.delegateSignature = ''
      this.delegateVisible = true
    },
    openSign(action) {
      this.moreVisible = false
      this.signAction = action === 'reduceSign' ? 'reduceSign' : 'addSign'
      this.signUser = null
      this.signTargetId = ''
      this.signComment = ''
      this.signVisible = true
    },
  },
})
