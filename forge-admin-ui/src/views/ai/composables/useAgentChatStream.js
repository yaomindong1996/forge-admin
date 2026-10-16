import { onBeforeUnmount } from 'vue'
import { AgentChatStreamSession } from './AgentChatStreamSession'

// Vue 只管挂载边界；流回调、请求版本和确认状态由独立生命周期对象编排。
export function useAgentChatStream(options) {
  const session = new AgentChatStreamSession(options)
  onBeforeUnmount(() => session.detach())
  return {
    startStream: session.startStream.bind(session),
    handleConfirm: session.handleConfirm.bind(session),
    stopChat: session.stopChat.bind(session),
    cancelHitl: session.stopChat.bind(session),
    clearHitlTimer: session.clearHitlTimer.bind(session),
  }
}
