export const DOCUMENT_FLOW_ACTION = Object.freeze({
  START: 'START_FLOW',
  RESUBMIT: 'RESUBMIT_FLOW',
  WITHDRAW: 'WITHDRAW_FLOW',
  HANDLE: 'HANDLE_TASK',
  VIEW: 'VIEW_FLOW',
  START_PROCESS: 'START_PROCESS',
})

export const NO_START_PERMISSION_HINT = '暂无提交审批权限，请联系管理员'
export const PC_ONLY_PROCESS_HINT = '该流程请在 PC 端发起'

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
    if (actionType === DOCUMENT_FLOW_ACTION.START_PROCESS) {
      hint = hint || PC_ONLY_PROCESS_HINT
      continue
    }
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
