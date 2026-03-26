/**
 * useFlow - 业务侧流程集成 Composable
 *
 * 设计目标：业务组件只需关心业务逻辑，无需关心流程引擎细节
 *
 * 使用示例：
 *   const { startFlow, flowStatus, approvalHistory, isRunning, canWithdraw } = useFlow('leave_apply', businessKey)
 *   await startFlow({ title: '张三请假申请', variables: { days: 3 } })
 */
import { ref, computed, readonly, watch } from 'vue'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'

/**
 * 业务流程集成
 * @param {string} processKey - 流程模型 Key（对应 FlowModel.modelKey）
 * @param {import('vue').Ref<string>|string} businessKeyRef - 业务唯一标识（可传 ref 或字符串）
 */
export function useFlow(processKey, businessKeyRef) {
  const userStore = useUserStore()

  // ======= 状态 =======
  const statusData = ref(null)    // FlowBusiness 业务状态数据
  const loading = ref(false)
  const submitting = ref(false)

  // ======= 计算属性 =======
  const flowStatus = computed(() => statusData.value?.status || 'none')

  /** 流程是否在运行中 */
  const isRunning = computed(() => flowStatus.value === 'running')

  /** 流程是否已结束（通过/驳回/取消） */
  const isFinished = computed(() =>
    ['approved', 'rejected', 'canceled'].includes(flowStatus.value)
  )

  /** 是否可以发起（未发起或已结束才能重新发起） */
  const canStart = computed(() =>
    flowStatus.value === 'none' || flowStatus.value === 'canceled'
  )

  /** 发起人是否可以撤回（运行中才能撤回） */
  const canWithdraw = computed(() =>
    isRunning.value && statusData.value?.applyUserId === userStore.userId
  )

  /** 状态文本 */
  const statusText = computed(() => {
    const map = {
      none: '未发起',
      draft: '草稿',
      running: '审批中',
      approved: '已通过',
      rejected: '已驳回',
      canceled: '已取消'
    }
    return map[flowStatus.value] || '未知'
  })

  /** 状态徽标类型（对应 Naive UI NTag type） */
  const statusTagType = computed(() => {
    const map = {
      none: 'default',
      draft: 'default',
      running: 'warning',
      approved: 'success',
      rejected: 'error',
      canceled: 'default'
    }
    return map[flowStatus.value] || 'default'
  })

  // ======= 获取业务 Key =======
  function getBusinessKey() {
    if (typeof businessKeyRef === 'string') return businessKeyRef
    if (businessKeyRef?.value) return businessKeyRef.value
    return null
  }

  // ======= 核心方法 =======

  /**
   * 刷新流程状态
   */
  async function refreshStatus() {
    const businessKey = getBusinessKey()
    if (!businessKey) return
    loading.value = true
    try {
      const res = await flowApi.getProcessStatus(businessKey)
      if (res.code === 200 && res.data) {
        statusData.value = res.data
      } else {
        statusData.value = null
      }
    } catch (e) {
      statusData.value = null
    } finally {
      loading.value = false
    }
  }

  /**
   * 发起流程
   * @param {object} options
   * @param {string} options.title        流程标题（必填）
   * @param {string} [options.businessType]  业务类型（可选，用于分类过滤）
   * @param {object} [options.variables]  流程变量（可选）
   * @returns {Promise<{success: boolean, processInstanceId: string}>}
   */
  async function startFlow({ title, businessType, variables = {} } = {}) {
    const businessKey = getBusinessKey()
    if (!businessKey) throw new Error('businessKey 不能为空')
    if (!title) throw new Error('流程标题 title 不能为空')

    submitting.value = true
    try {
      const res = await flowApi.startProcess(processKey, {
        businessKey,
        businessType: businessType || processKey,
        title,
        variables,
        userId: userStore.userId,
        userName: userStore.userInfo?.nickName || userStore.userInfo?.userName,
        deptId: userStore.userInfo?.deptId,
        deptName: userStore.userInfo?.deptName
      })
      if (res.code === 200) {
        await refreshStatus()
        return { success: true, processInstanceId: res.data }
      }
      return { success: false, message: res.message }
    } finally {
      submitting.value = false
    }
  }

  /**
   * 撤回流程（发起人在运行中撤回）
   * @param {string} [reason] 撤回原因
   */
  async function withdrawFlow(reason = '') {
    const businessKey = getBusinessKey()
    if (!businessKey) return
    submitting.value = true
    try {
      const res = await flowApi.withdrawProcess({
        processInstanceId: statusData.value?.processInstanceId,
        userId: userStore.userId,
        reason
      })
      if (res.code === 200) {
        await refreshStatus()
        return { success: true }
      }
      return { success: false, message: res.message }
    } finally {
      submitting.value = false
    }
  }

  /**
   * 终止流程（管理员强制终止）
   * @param {string} reason 终止原因
   */
  async function terminateFlow(reason = '') {
    const businessKey = getBusinessKey()
    if (!businessKey) return
    submitting.value = true
    try {
      const res = await flowApi.terminateProcess(businessKey, {
        userId: userStore.userId,
        reason
      })
      if (res.code === 200) {
        await refreshStatus()
        return { success: true }
      }
      return { success: false, message: res.message }
    } finally {
      submitting.value = false
    }
  }

  /**
   * 获取流程审批历史
   * @returns {Promise<Array>}
   */
  async function getApprovalHistory() {
    if (!statusData.value?.processInstanceId) return []
    try {
      const res = await flowApi.getProcessComments(statusData.value.processInstanceId)
      if (res.code === 200) return res.data || []
    } catch (e) {
      console.error('获取审批历史失败:', e)
    }
    return []
  }

  /**
   * 获取流程图信息（节点高亮）
   * @returns {Promise<object|null>}
   */
  async function getDiagramInfo() {
    if (!statusData.value?.processInstanceId) return null
    try {
      const res = await flowApi.getProcessDiagramInfo(statusData.value.processInstanceId)
      if (res.code === 200) return res.data
    } catch (e) {
      console.error('获取流程图失败:', e)
    }
    return null
  }

  // businessKey 变化时自动刷新状态
  if (businessKeyRef && typeof businessKeyRef !== 'string') {
    watch(businessKeyRef, (newKey) => {
      if (newKey) refreshStatus()
    }, { immediate: true })
  } else {
    // 字符串形式，立即初始化
    const bk = getBusinessKey()
    if (bk) refreshStatus()
  }

  return {
    // 状态
    statusData: readonly(statusData),
    flowStatus,
    statusText,
    statusTagType,
    loading: readonly(loading),
    submitting: readonly(submitting),

    // 计算属性
    isRunning,
    isFinished,
    canStart,
    canWithdraw,

    // 方法
    startFlow,
    withdrawFlow,
    terminateFlow,
    refreshStatus,
    getApprovalHistory,
    getDiagramInfo
  }
}

/**
 * 用于审批页面（任务处理人视角）的 composable
 * 封装通过/驳回/转办等操作
 */
export function useFlowTask() {
  const userStore = useUserStore()
  const processing = ref(false)

  /**
   * 审批通过
   */
  async function approve(taskId, comment, variables = {}) {
    processing.value = true
    try {
      const res = await flowApi.approveTask({
        taskId,
        userId: userStore.userId,
        comment,
        variables
      })
      return { success: res.code === 200, message: res.message }
    } catch (e) {
      return { success: false, message: e.message }
    } finally {
      processing.value = false
    }
  }

  /**
   * 审批驳回
   */
  async function reject(taskId, comment) {
    processing.value = true
    try {
      const res = await flowApi.rejectTask({
        taskId,
        userId: userStore.userId,
        comment
      })
      return { success: res.code === 200, message: res.message }
    } catch (e) {
      return { success: false, message: e.message }
    } finally {
      processing.value = false
    }
  }

  /**
   * 转办
   */
  async function delegate(taskId, targetUserId, comment = '') {
    processing.value = true
    try {
      const res = await flowApi.delegateTask({
        taskId,
        userId: userStore.userId,
        targetUserId,
        comment
      })
      return { success: res.code === 200, message: res.message }
    } catch (e) {
      return { success: false, message: e.message }
    } finally {
      processing.value = false
    }
  }

  /**
   * 签收候选任务
   */
  async function claim(taskId) {
    processing.value = true
    try {
      const res = await flowApi.claimTask(taskId, userStore.userId)
      return { success: res.code === 200, message: res.message }
    } catch (e) {
      return { success: false, message: e.message }
    } finally {
      processing.value = false
    }
  }

  return {
    processing: readonly(processing),
    approve,
    reject,
    delegate,
    claim
  }
}
