import { request } from '@/utils'
import { generateUUID } from '@/utils/common'
import { useAuthStore } from '@/store/modules/auth'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''

/**
 * SSE 流式生成，支持自动重试
 * @param {Object} data - 请求数据
 * @param {Function} onChunk - 收到数据块回调
 * @param {Function} onComplete - 完成回调
 * @param {Function} onError - 错误回调
 * @param {Object} options - 配置选项
 * @param {number} options.maxRetries - 最大重试次数，默认2
 * @param {number} options.retryDelay - 重试延迟(ms)，默认1000
 * @returns {AbortController} 用于取消请求
 */
export function streamGenerate(data, onChunk, onComplete, onError, options = {}) {
  const { maxRetries = 2, retryDelay = 1000 } = options
  const controller = new AbortController()
  const authStore = useAuthStore()
  let currentRetry = 0
  let isAborted = false

  // 监听abort事件
  controller.signal.addEventListener('abort', () => {
    isAborted = true
  })

  function doFetch() {
    fetch(BASE_URL + '/ai/crud-generator/stream-generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': generateUUID(),
      },
      body: JSON.stringify(data),
      signal: controller.signal,
    })
      .then(response => {
        // 请求成功，重置重试计数
        currentRetry = 0
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let hasReceivedData = false

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              // 如果流结束但没有收到任何数据，可能是异常
              if (!hasReceivedData) {
                handleRetry('服务器未返回数据')
              }
              return
            }
            hasReceivedData = true
            buffer += decoder.decode(value, { stream: true })
            
            const events = buffer.split('\n\n')
            buffer = events.pop() || ''

            for (const eventStr of events) {
              if (!eventStr.trim()) continue
              
              const lines = eventStr.split('\n')
              let eventType = 'message'
              let eventData = ''
              
              for (const line of lines) {
                if (line.startsWith('event:')) {
                  eventType = line.substring(6).trim()
                } else if (line.startsWith('data:')) {
                  eventData = line.substring(5).trim()
                }
              }
              
              if (eventData) {
                try {
                  const parsed = JSON.parse(eventData)
                  if (eventType === 'progress' || eventType === 'chunk' || eventType === 'meta') {
                    onChunk({ event: eventType, data: parsed })
                  } else if (eventType === 'complete') {
                    onComplete(parsed)
                  } else if (eventType === 'error') {
                    onError(parsed.message)
                  }
                } catch (e) {
                  console.warn('JSON parse error:', e)
                }
              }
            }
            read()
          }).catch(err => {
            if (err.name !== 'AbortError') {
              handleRetry(err.message)
            }
          })
        }
        read()
      })
      .catch(err => {
        if (err.name !== 'AbortError') {
          handleRetry(err.message)
        }
      })
  }

  function handleRetry(errorMessage) {
    if (isAborted) return
    
    if (currentRetry < maxRetries) {
      currentRetry++
      console.warn(`[streamGenerate] 第${currentRetry}次重试，原因: ${errorMessage}`)
      // 通知用户正在重试
      onChunk({ 
        event: 'progress', 
        data: { 
          stage: 'retrying', 
          message: `连接中断，正在重试 (${currentRetry}/${maxRetries})...` 
        } 
      })
      setTimeout(doFetch, retryDelay)
    } else {
      onError(`连接失败: ${errorMessage} (已重试${maxRetries}次)`)
    }
  }

  // 首次执行
  doFetch()

  return controller
}

export function crudGeneratorSessionList(params) {
  return request.get('/ai/admin/session/page', { params })
}

export function crudGeneratorSessionMessages(sessionId) {
  return request.get(`/ai/admin/session/${sessionId}/messages`)
}

export function crudGeneratorSessionDelete(sessionId) {
  return request.delete(`/ai/admin/session/${sessionId}`)
}