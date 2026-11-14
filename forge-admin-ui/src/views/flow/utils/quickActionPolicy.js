// 快捷同意/驳回只看节点审批权限开关；表单填写类限制仍在提交时按业务上下文判断
export function quickActionPolicyError(action, formInfo) {
  if (action === 'approve' && formInfo?.allowApprove === false)
    return '当前节点不允许同意'
  if (action === 'reject' && formInfo?.allowReject === false)
    return '当前节点不允许驳回'
  if (action === 'reject' && formInfo?.allowMultiReturn === true && Array.isArray(formInfo.returnTargets) && formInfo.returnTargets.length)
    return '该流程已开启指定节点驳回，请进入详情选择驳回节点'
  if (action === 'rejectToStart' && formInfo?.allowRejectToStart !== true)
    return '当前节点不允许退回发起人修改'
  if (formInfo?.requireSignature === true)
    return '需要手写签名，请进入详情处理'
  return ''
}

// 打开快捷弹窗时的预检：节点权限 + 流程级动态表单（需进详情填写）；业务应用表单仍留到提交时结合业务上下文判断
export function quickActionPrecheckError(action, formInfo) {
  const policyError = quickActionPolicyError(action, formInfo)
  if (policyError)
    return policyError
  if (action === 'approve' && formInfo?.formType === 'dynamic' && formInfo?.formJson)
    return '需要填写节点表单，请进入详情处理'
  return ''
}
