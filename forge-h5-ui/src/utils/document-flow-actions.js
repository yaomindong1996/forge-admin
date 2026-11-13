export const DOCUMENT_FLOW_ACTION = Object.freeze({
  START: 'START_FLOW',
  RESUBMIT: 'RESUBMIT_FLOW',
  WITHDRAW: 'WITHDRAW_FLOW',
  HANDLE: 'HANDLE_TASK',
  VIEW: 'VIEW_FLOW',
  START_PROCESS: 'START_PROCESS',
})

export const NO_START_PERMISSION_HINT = '暂无提交审批权限，请联系管理员'
export const NO_PROCESS_START_PERMISSION_HINT = '暂无发起业务流程权限，请联系管理员'
// 单据页能承载的业务流程动作位置；toolbar 属于列表级操作，不在单据页展示
const PROCESS_ACTION_LISTS = ['rowActions', 'detailActions', 'formActions', 'runtimeActions']

// permission 对应 FLOW_PERMISSIONS 的键；HANDLE_TASK 只跳转待办详情，由待办接口自行校验。
const BUTTONS = {
  [DOCUMENT_FLOW_ACTION.START]: { label: '提交审批', variant: 'primary', permission: 'start' },
  [DOCUMENT_FLOW_ACTION.RESUBMIT]: { label: '重新提交', variant: 'primary', permission: 'start' },
  [DOCUMENT_FLOW_ACTION.WITHDRAW]: { label: '撤回', variant: 'secondary', permission: 'withdraw' },
  [DOCUMENT_FLOW_ACTION.HANDLE]: { label: '去审批', variant: 'primary', permission: '' },
}

export function resolveDocumentFlowActionType(action = {}) {
  return String(action.actionType || action.key || '').trim().toUpperCase()
}

/**
 * 按钮完全由后端运行态 runtimeActions 决定，前端不推导状态。
 * can(permissionKey) 返回当前用户是否有对应权限；没有权限的按钮隐藏并给出提示。
 */
export function buildDocumentFlowButtons(runtime, can = () => true) {
  const buttons = []
  let hint = ''
  for (const action of Array.isArray(runtime?.runtimeActions) ? runtime.runtimeActions : []) {
    if (!action || action.visible === false) continue
    const actionType = resolveDocumentFlowActionType(action)
    // 应用级业务流程按钮由 buildProcessStartButtons 单独生成
    if (isProcessStartAction(action)) continue
    const preset = BUTTONS[actionType]
    if (!preset) continue
    if (preset.permission && !can(preset.permission)) {
      if (preset.permission === 'start') hint = NO_START_PERMISSION_HINT
      continue
    }
    if (buttons.some(button => button.actionType === actionType)) continue
    buttons.push({
      key: actionType,
      actionType,
      label: preset.label,
      variant: preset.variant,
      disabled: action.disabled === true,
      disabledReason: action.disabledReason || '',
    })
  }
  return { buttons, hint }
}

export function isProcessStartAction(action = {}) {
  return resolveDocumentFlowActionType(action) === DOCUMENT_FLOW_ACTION.START_PROCESS
    || String(action.key || '').toUpperCase().startsWith('STARTPROCESS:')
}

/** 渲染配置中投影出的业务流程启动动作（后端 BusinessApplicationRuntimeConfigOverlayService 合并写入）。 */
export function collectProcessStartActions(config = {}) {
  const options = config?.options && typeof config.options === 'object' ? config.options : {}
  return PROCESS_ACTION_LISTS
    .flatMap(name => (Array.isArray(options[name]) ? options[name] : []))
    .filter(action => action && isProcessStartAction(action))
}

/**
 * 业务流程启动按钮：同一记录上同一流程已运行或已发起过就不再展示，
 * 与管理端 shouldHideProcessStartAction 及服务端手工发起幂等语义一致。
 */
export function buildProcessStartButtons(actions = [], processState = {}, { canStart = true, isVisible = () => true } = {}) {
  const started = new Set([
    ...(Array.isArray(processState?.activeProcessCodes) ? processState.activeProcessCodes : []),
    ...(Array.isArray(processState?.startedProcessCodes) ? processState.startedProcessCodes : []),
  ].map(code => String(code).trim()))
  const buttons = []
  let hint = ''
  for (const action of Array.isArray(actions) ? actions : []) {
    if (!action || action.visible === false || !isProcessStartAction(action) || !isVisible(action)) continue
    const applicationCode = String(action.applicationCode || '').trim()
    const processCode = String(action.processCode || '').trim()
    if (!applicationCode || !processCode || started.has(processCode)) continue
    if (buttons.some(button => button.processCode === processCode)) continue
    if (!canStart) {
      hint = NO_PROCESS_START_PERMISSION_HINT
      continue
    }
    buttons.push({
      key: `${DOCUMENT_FLOW_ACTION.START_PROCESS}:${processCode}`,
      actionType: DOCUMENT_FLOW_ACTION.START_PROCESS,
      label: String(action.label || '').trim() || '发起流程',
      variant: 'primary',
      applicationCode,
      processCode,
      objectCode: String(action.objectCode || '').trim(),
      confirmText: String(action.confirmText || '').trim(),
      disabled: false,
      disabledReason: '',
    })
  }
  return { buttons, hint }
}

/**
 * 启动接口在首个节点执行失败时仍返回运行记录（status=FAILED），失败原因记在节点时间线上。
 * 返回空串表示已正常进入流程。
 */
export function resolveProcessRunFailure(run) {
  if (String(run?.status || '').toUpperCase() !== 'FAILED') return ''
  const timeline = Array.isArray(run.timeline) ? run.timeline : []
  const failedNode = timeline.find(item => String(item?.status || '').toUpperCase() === 'FAILED' && item.errorSummary)
  return String(run.errorSummary || failedNode?.errorSummary || '流程执行失败，请联系管理员').trim()
}

export function resolveDocumentStatusText(runtime) {
  if (!runtime) return ''
  return runtime.documentStatusLabel || runtime.message || ''
}

// 只决定标签颜色，文案始终使用后端返回的 documentStatusLabel / message。
const FLOW_STATUS_TONE = {
  STARTED: 'primary',
  RUNNING: 'primary',
  IN_PROCESS: 'primary',
  NEED_MODIFY: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
}

export function resolveDocumentFlowTone(flowStatus) {
  return FLOW_STATUS_TONE[String(flowStatus || '').toUpperCase()] || 'default'
}
