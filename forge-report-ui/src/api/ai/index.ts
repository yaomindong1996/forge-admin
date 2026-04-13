import { post } from '@/api/http'
import { AIGenerateRequest, AIGenerateResponse } from './ai.d'

/**
 * AI 生成大屏（调用后端 API）
 */
export async function aiGenerate(data: AIGenerateRequest): Promise<AIGenerateResponse> {
  const res = await post('/goview-api/ai/generate', data) as any
  // 后端返回 RespInfo<String>，需要解析 JSON
  const jsonStr = res?.data || res
  if (typeof jsonStr === 'string') {
    return JSON.parse(jsonStr)
  }
  return jsonStr
}

/**
 * AI 对话流式输出（SSE）
 */
export function aiChatStream(
  content: string,
  agentCode: string,
  sessionId: string,
  onChunk: (chunk: string) => void,
  onDone: () => void,
  onError: (error: Error) => void
): AbortController {
  const abortController = new AbortController()

  fetch('/goview-api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ content, agentCode, sessionId }),
    signal: abortController.signal
  })
    .then(async response => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法获取响应流')
      }

      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留最后一个不完整的行

        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed || trimmed === 'data: [DONE]') continue
          if (trimmed === 'event: done') continue
          if (trimmed.startsWith('event: error')) continue
          if (!trimmed.startsWith('data: ')) continue

          try {
            const chunk = trimmed.slice(6)
            if (chunk === '[DONE]') {
              onDone()
              return
            }
            onChunk(chunk)
          } catch {
            // 忽略解析失败
          }
        }
      }
      onDone()
    })
    .catch(error => {
      if (error.name !== 'AbortError') {
        onError(error)
      }
    })

  return abortController
}
