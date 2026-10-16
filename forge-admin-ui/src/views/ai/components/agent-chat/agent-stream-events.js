export function failAssistantMessage(msg, error) {
  msg.streaming = false
  msg.status = 'error'
  msg.error = error || '对话失败'
}

function currentTool(msg, data) {
  const last = msg.toolCalls[msg.toolCalls.length - 1]
  if (!data?.toolCallId) {
    return last
  }
  const matching = msg.toolCalls.find(tool => tool.callId === data.toolCallId)
  if (matching) {
    return matching
  }
  // 旧历史记录未保存调用 ID；只接回最后一个未标 ID 的调用，不覆盖其它明确 ID。
  if (last && !last.callId) {
    last.callId = data.toolCallId
    return last
  }
}

function toolEvent(type, data, msg) {
  if (type === 'TOOL_CALL_START') {
    // 未正常结束的旧调用不能在下一调用到来时伪装成功。
    const previous = currentTool(msg)
    if (previous?.status === 'running') {
      previous.status = 'error'
      previous.error = '工具未正常结束'
    }
    msg.toolCalls.push({
      callId: data?.toolCallId,
      name: data?.tool || '工具',
      args: data?.args ?? '',
      result: '',
      status: 'running',
      error: '',
    })
    return
  }
  const tool = currentTool(msg, data)
  if (!tool) {
    return
  }
  if (['TOOL_RESULT_TEXT_DELTA', 'TOOL_RESULT_DATA_DELTA'].includes(type)) {
    tool.result = `${typeof tool.result === 'string' ? tool.result : ''}${data?.content || ''}`
  }
  if (type === 'TOOL_RESULT_END') {
    tool.status = data?.success === false ? 'error' : 'done'
    tool.error = data?.success === false ? '工具执行失败' : ''
  }
  if (type === 'ALL_TOOLS_DENIED') {
    tool.status = 'aborted'
    tool.error = '工具未绑定或当前权限不允许执行'
  }
  if (type === 'USER_CONFIRM_RESULT') {
    tool.status = data?.confirmed ? 'running' : 'aborted'
  }
}

function markFirstToken(msg) {
  if (msg.firstTokenMs == null && msg._startedAt != null) {
    msg.firstTokenMs = Date.now() - msg._startedAt
  }
}

export function finishAssistantMessage(msg) {
  msg.streaming = false
  if (!['error', 'aborted', 'waiting_confirm'].includes(msg.status)) {
    msg.status = 'done'
  }
  for (const tool of msg.toolCalls) {
    if (tool.status === 'running') {
      tool.status = msg.status === 'aborted' ? 'aborted' : 'error'
      tool.error = '工具未正常结束'
    }
  }
  if (msg.status !== 'waiting_confirm' && msg.totalMs == null && msg._startedAt != null) {
    msg.totalMs = Date.now() - msg._startedAt
  }
}

export function applyAgentStreamEvent(type, data, msg, effects) {
  if (type.startsWith('TOOL_') || ['ALL_TOOLS_DENIED', 'USER_CONFIRM_RESULT'].includes(type)) {
    toolEvent(type, data, msg)
    return
  }
  switch (type) {
    case 'PERSIST_META':
      effects.persistMeta(data, msg)
      break
    case 'TEXT_BLOCK_DELTA':
      markFirstToken(msg)
      msg.content += data?.text || ''
      effects.scheduleRender(msg)
      break
    case 'THINKING_BLOCK_DELTA':
      markFirstToken(msg)
      msg.thinking += data?.text || ''
      break
    case 'REQUIRE_USER_CONFIRM': {
      msg.status = 'waiting_confirm'
      const tool = currentTool(msg, data)
      if (tool) {
        tool.status = 'waiting_confirm'
      }
      effects.confirm(data)
      break
    }
    case 'HINT_BLOCK':
      effects.hint(data?.hint || '提示信息')
      break
    case 'MODEL_CALL_END':
      if (data?.error) {
        failAssistantMessage(msg, data.error)
      }
      else if (data?.usage?.totalTokens != null) {
        msg.tokenUsage = data.usage.totalTokens
      }
      break
    case 'EXCEED_MAX_ITERS':
      failAssistantMessage(msg, '超过最大迭代次数')
      break
    case 'AGENT_END':
      if (data?.error) {
        failAssistantMessage(msg, data.error)
      }
      else if (data?.aborted) {
        msg.status = 'aborted'
      }
      finishAssistantMessage(msg)
      effects.flushRender(msg)
      break
    case 'ERROR':
    case 'error':
      failAssistantMessage(msg, data?.message || data?.error || '对话失败')
      break
    default:
      break
  }
}
