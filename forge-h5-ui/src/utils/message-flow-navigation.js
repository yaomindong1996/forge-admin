const FLOW_TODO_BIZ_TYPE = 'FLOW_TODO'

export function isFlowTodoMessage(message = {}) {
  return String(message?.bizType || '').toUpperCase() === FLOW_TODO_BIZ_TYPE
}

export function isFlowTaskRoute(route = '') {
  const path = String(route || '').split('?')[0]
  return path === '/flow/todo'
    || path === '/pages/todo'
    || path === '/pages/todo-detail'
}

export function extractFlowTaskId(route = '') {
  const match = String(route || '').match(/[?&]taskId=([^&#]+)/i)
  if (!match) return ''
  try {
    return String(decodeURIComponent(match[1]))
  }
  catch {
    return String(match[1])
  }
}

export function resolveFlowMessageTaskId(message = {}, route = '') {
  const value = extractFlowTaskId(route)
    || message.taskId
    || message.task_id
    || (isFlowTodoMessage(message) ? message.bizKey : '')
  return value === undefined || value === null ? '' : String(value)
}

export function resolveFlowMessageMode(message = {}) {
  return isFlowTodoMessage(message) && Number(message.readFlag) === 1 ? 'readonly' : 'todo'
}

export function shouldAutoMarkMessageRead(message = {}) {
  return Number(message.readFlag) === 0 && !isFlowTodoMessage(message)
}

export function buildFlowTaskDetailUrl(taskId, mode = 'todo', messageId = '') {
  const normalizedTaskId = taskId === undefined || taskId === null ? '' : String(taskId)
  if (!normalizedTaskId) return ''
  const normalizedMode = mode === 'readonly' ? 'readonly' : 'todo'
  const normalizedMessageId = messageId === undefined || messageId === null ? '' : String(messageId)
  const messageQuery = normalizedMessageId ? `&messageId=${encodeURIComponent(normalizedMessageId)}` : ''
  return `/pages/todo-detail?taskId=${encodeURIComponent(normalizedTaskId)}&mode=${normalizedMode}${messageQuery}`
}
