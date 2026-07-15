export function isFlowApprovalMessage(message) {
  return message?.bizType === 'FLOW_TODO'
}

export function isPendingFlowApprovalMessage(message) {
  return isFlowApprovalMessage(message) && Number(message?.readFlag) === 0
}
