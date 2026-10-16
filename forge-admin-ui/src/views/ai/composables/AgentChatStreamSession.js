import { engineResume, engineStop, streamEngineChat } from '@/api/ai/engine'
import { imageRecordId } from '@/stores/ai/image-job-utils'
import { applyAgentStreamEvent, failAssistantMessage, finishAssistantMessage }
  from '../components/agent-chat/agent-stream-events'

const HITL_TIMEOUT_SEC = 30

// 只负责当前页面的直接流与确认计时，不用断连释放后端 lease，不自动重发业务工具。
export class AgentChatStreamSession {
  constructor(options) {
    this.options = options
    this.controller = null
    this.activeMessage = null
    this.runVersion = 0
    this.hitlTimer = null
    this.hitlInterval = null
  }

  clearHitlTimer() {
    const { hitlCountdown } = this.options
    clearTimeout(this.hitlTimer)
    clearInterval(this.hitlInterval)
    this.hitlTimer = null
    this.hitlInterval = null
    hitlCountdown.value = 0
  }

  waitForConfirm(data) {
    const { pendingConfirm, hitlCountdown, message } = this.options
    pendingConfirm.value = { interruptId: data?.interruptId || '', tool: data?.tool || '', args: data?.args ?? '' }
    this.clearHitlTimer()
    hitlCountdown.value = HITL_TIMEOUT_SEC
    this.hitlInterval = setInterval(() => {
      hitlCountdown.value = Math.max(0, hitlCountdown.value - 1)
      if (!hitlCountdown.value) {
        clearInterval(this.hitlInterval)
      }
    }, 1000)
    this.hitlTimer = setTimeout(() => {
      if (pendingConfirm.value) {
        message.warning('工具确认超时，已自动拒绝')
        this.handleConfirm(false)
      }
    }, HITL_TIMEOUT_SEC * 1000)
  }

  persistMeta(data, assistant) {
    const { messages } = this.options
    if (data?.assistantRecordId != null) {
      assistant.recordId = data.assistantRecordId
    }
    if (data?.userRecordId != null) {
      const index = messages.value.findIndex(item => item.id === assistant.id)
      const user = messages.value.slice(0, index).reverse().find(item => item.role === 'user')
      if (user) {
        user.recordId = data.userRecordId
      }
    }
  }

  prepare(assistant) {
    const { isStreaming, connectionError } = this.options
    this.runVersion += 1
    this.controller?.abort()
    isStreaming.value = true
    connectionError.value = false
    this.activeMessage = assistant
    assistant.status = 'streaming'
    assistant.streaming = true
    assistant.error = ''
    return this.runVersion
  }

  callbacks(assistant, version) {
    const { scheduleRender, flushRender, message, scrollToBottom } = this.options
    const { connectionError, pendingConfirm } = this.options
    const current = () => version === this.runVersion
    const event = (type, data) => {
      if (current()) {
        applyAgentStreamEvent(type, data, assistant, {
          persistMeta: this.persistMeta.bind(this),
          scheduleRender,
          flushRender,
          confirm: data => this.waitForConfirm(data),
          hint: text => message.info(text),
        })
        scrollToBottom()
      }
    }
    const complete = () => {
      if (current()) {
        finishAssistantMessage(assistant)
        flushRender(assistant)
        this.settle()
      }
    }
    const error = (cause) => {
      if (current()) {
        failAssistantMessage(assistant, cause?.message || '对话连接失败')
        finishAssistantMessage(assistant)
        flushRender(assistant)
        connectionError.value = true
        this.clearHitlTimer()
        pendingConfirm.value = null
        this.settle()
        message.error(`对话失败: ${assistant.error}`)
      }
    }
    return [event, complete, error]
  }

  settle() {
    const { isStreaming, scrollToBottom } = this.options
    isStreaming.value = false
    this.activeMessage = null
    this.controller = null
    scrollToBottom()
  }

  startStream(text, assistant, agentCode, images, extra = {}) {
    const { currentAgent, currentSessionId } = this.options
    const version = this.prepare(assistant)
    assistant.firstTokenMs = null
    assistant.totalMs = null
    assistant.tokenUsage = null
    assistant._startedAt = Date.now()
    const payload = {
      agentCode: agentCode || currentAgent.value?.agentCode,
      sessionId: currentSessionId.value,
      message: text,
      ...extra,
    }
    const ids = (images || []).map(imageRecordId).filter(Boolean)
    if (ids.length) {
      payload.imageFileIds = ids
    }
    this.controller = streamEngineChat(payload, ...this.callbacks(assistant, version))
  }

  handleConfirm(confirmed) {
    const { pendingConfirm, messages, message } = this.options
    const confirmation = pendingConfirm.value
    if (!confirmation) {
      return
    }
    const assistant = this.activeMessage || [...messages.value].reverse().find(item => item.role === 'assistant')
    this.clearHitlTimer()
    pendingConfirm.value = null
    if (!confirmation.interruptId || !assistant) {
      message.error('确认信息缺失，无法恢复对话')
      return
    }
    const version = this.prepare(assistant)
    assistant.totalMs = null
    assistant._startedAt ??= Date.now()
    this.controller = engineResume(confirmation.interruptId, confirmed, ...this.callbacks(assistant, version))
  }

  stopChat() {
    const { pendingConfirm, currentSessionId, message, flushRender } = this.options
    if (pendingConfirm.value) {
      this.handleConfirm(false)
      return
    }
    this.runVersion += 1
    this.clearHitlTimer()
    pendingConfirm.value = null
    if (this.controller) {
      if (currentSessionId.value) {
        engineStop(currentSessionId.value).catch(() => message.warning('停止请求未确认，请刷新会话查看状态'))
      }
      this.controller.abort()
      this.controller = null
    }
    if (this.activeMessage) {
      this.activeMessage.status = 'aborted'
      finishAssistantMessage(this.activeMessage)
      flushRender(this.activeMessage)
    }
    this.settle()
  }

  detach() {
    this.runVersion += 1
    this.clearHitlTimer()
    this.controller?.abort()
    this.controller = null
    // 离页只断开本地订阅，不调用 stop，不提前释放后台占用。
  }
}
