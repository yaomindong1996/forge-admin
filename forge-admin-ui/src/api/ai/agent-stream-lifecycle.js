// HTTP 正常结束只说明连接结束；必须看到协议终态才算一次 Agent 请求完成。
export function agentStreamLifecycle({ onEvent, onComplete, onError }) {
  let terminal = false
  let waitingConfirm = false
  let failure = null
  let settled = false

  function fail(error) {
    if (!settled) {
      settled = true
      onError?.(error)
    }
  }

  function end(data) {
    terminal = true
    if (!data || typeof data !== 'object' || Array.isArray(data)) {
      failure = new Error('对话完成状态无效，连接可能已中断，请刷新会话查看结果')
    }
    else if (data.error) {
      failure = new Error(data.error)
    }
    else if (data.interrupted && !waitingConfirm) {
      failure = new Error('工具确认信息缺失，无法恢复对话')
    }
  }

  function event(type, data) {
    if (settled) {
      return
    }
    if (['ERROR', 'error'].includes(type)) {
      failure = new Error(data?.message || data?.error || 'Agent 执行失败，请稍后重试')
    }
    if (type === 'MODEL_CALL_END' && data?.error) {
      failure = new Error(data.error)
    }
    if (type === 'EXCEED_MAX_ITERS') {
      failure = new Error('超过最大迭代次数')
    }
    if (type === 'REQUIRE_USER_CONFIRM') {
      waitingConfirm = typeof data?.interruptId === 'string' && Boolean(data.interruptId.trim())
      terminal = waitingConfirm
      if (!waitingConfirm) {
        failure = new Error('工具确认信息缺失，无法恢复对话')
      }
    }
    if (type === 'AGENT_END') {
      end(data)
    }
    onEvent?.(type, data)
  }

  function complete() {
    if (settled) {
      return
    }
    if (failure || !terminal) {
      fail(failure || new Error('对话连接已中断，未收到完成状态，请刷新会话查看结果'))
      return
    }
    settled = true
    onComplete?.()
  }

  return { onEvent: event, onComplete: complete, onError: fail }
}
