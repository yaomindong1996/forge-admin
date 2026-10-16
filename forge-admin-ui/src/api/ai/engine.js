import { useAuthStore } from '@/store/modules/auth'
import { consumeSSE, generateUUID, request } from '@/utils'
import { agentStreamLifecycle } from './agent-stream-lifecycle'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''

/**
 * AI 引擎 SSE 薄封装：注入鉴权头与防重放头后，委托给通用 {@link consumeSSE}。
 * 返回 AbortController，调用方可 `.abort()` 中断（对话页停止生成即用它）。
 */
function streamEngine(url, data, onEvent, onComplete, onError) {
  const authStore = useAuthStore()
  return consumeSSE(`${BASE_URL}${url}`, {
    headers: {
      'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
      'X-Timestamp': Date.now().toString(),
      'X-Nonce': generateUUID(),
    },
    body: data,
    ...agentStreamLifecycle({ onEvent, onComplete, onError }),
  })
}

export function streamEngineChat(data, onEvent, onComplete, onError) {
  return streamEngine('/ai/engine/stream', data, onEvent, onComplete, onError)
}

export function engineResume(interruptId, confirmed, onEvent, onComplete, onError) {
  return streamEngine('/ai/engine/resume', { interruptId, confirmed }, onEvent, onComplete, onError)
}

export function engineStop(sessionId) {
  return request.post('/ai/engine/stop', { sessionId })
}
