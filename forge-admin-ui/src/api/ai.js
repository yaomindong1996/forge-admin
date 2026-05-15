import { useAuthStore } from '@/store/modules/auth'
import { generateUUID, request } from '@/utils'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''

// ========== 智能体管理 ==========

export function agentPage(params) {
  return request.get('/ai/agent/page', { params })
}

export function agentList() {
  return request.get('/ai/agent/list')
}

export function agentGetById(id) {
  return request.get(`/ai/agent/${id}`)
}

export function agentAdd(data) {
  return request.post('/ai/agent', data)
}

export function agentUpdate(data) {
  return request.put('/ai/agent', data)
}

export function agentDelete(id) {
  return request.delete(`/ai/agent/${id}`)
}

export function streamAgentChat(data, onChunk, onComplete, onError, options = {}) {
  const { maxRetries = 0, retryDelay = 800 } = options
  const controller = new AbortController()
  const authStore = useAuthStore()
  let currentRetry = 0
  let isAborted = false
  let completed = false

  controller.signal.addEventListener('abort', () => {
    isAborted = true
  })

  function completeOnce(data) {
    if (!completed) {
      completed = true
      onComplete(data)
    }
  }

  function parseSseBlock(block) {
    let eventType = 'message'
    const dataLines = []

    for (const rawLine of block.split(/\r?\n/)) {
      const line = rawLine.trimEnd()
      if (!line || line.startsWith(':')) {
        continue
      }

      if (line.startsWith('event:')) {
        eventType = line.slice(6).trim()
      }
      else if (line.startsWith('data:')) {
        const dataLine = line.slice(5)
        dataLines.push(dataLine.startsWith(' ') ? dataLine.slice(1) : dataLine)
      }
    }

    return {
      eventType,
      eventData: dataLines.join('\n'),
    }
  }

  function processSseBlock(block) {
    if (!block.trim() || completed) {
      return
    }

    const { eventType, eventData } = parseSseBlock(block)
    const parsedData = parseEventData(eventData)
    if (eventType === 'done' || eventData === '[DONE]') {
      completeOnce(parsedData)
    }
    else if (eventType === 'complete') {
      completeOnce(parsedData)
    }
    else if (eventType === 'error') {
      completed = true
      onError(parsedData?.message || eventData || '智能体测试失败')
    }
    else if (eventType === 'progress') {
      onChunk({
        event: 'progress',
        data: parsedData || { message: eventData },
      })
    }
    else if (eventData) {
      onChunk({
        event: 'chunk',
        data: parsedData && typeof parsedData === 'object'
          ? parsedData
          : { content: eventData },
      })
    }
  }

  function parseEventData(eventData) {
    if (!eventData || eventData === '[DONE]') {
      return null
    }

    try {
      return JSON.parse(eventData)
    }
    catch {
      return null
    }
  }

  function doFetch() {
    fetch(`${BASE_URL}/ai/client/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
        'X-Timestamp': Date.now().toString(),
        'X-Nonce': generateUUID(),
      },
      body: JSON.stringify(data),
      signal: controller.signal,
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(response.statusText || `HTTP ${response.status}`)
        }

        currentRetry = 0
        if (!response.body) {
          throw new Error('当前浏览器不支持流式响应')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              if (buffer.trim()) {
                processSseBlock(buffer)
              }
              completeOnce()
              return
            }

            buffer += decoder.decode(value, { stream: true })
            const events = buffer.split(/\r?\n\r?\n/)
            buffer = events.pop() ?? ''

            for (const eventStr of events) {
              processSseBlock(eventStr)
            }

            read()
          }).catch((error) => {
            if (error.name !== 'AbortError') {
              handleRetry(error.message)
            }
          })
        }

        read()
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          handleRetry(error.message)
        }
      })
  }

  function handleRetry(errorMessage) {
    if (isAborted) {
      return
    }

    if (currentRetry < maxRetries) {
      currentRetry++
      setTimeout(doFetch, retryDelay)
    }
    else {
      onError(errorMessage || '智能体测试连接失败')
    }
  }

  doFetch()
  return controller
}

// ========== 供应商管理 ==========

/**
 * 分页查询供应商列表
 */
export function providerPage(params) {
  return request.get('/ai/provider/page', { params })
}

/**
 * 查询供应商详情
 */
export function providerGetById(id) {
  return request.get(`/ai/provider/${id}`)
}

/**
 * 新增供应商
 */
export function providerAdd(data) {
  return request.post('/ai/provider', data)
}

/**
 * 修改供应商
 */
export function providerUpdate(data) {
  return request.put('/ai/provider', data)
}

/**
 * 删除供应商
 */
export function providerDelete(id) {
  return request.delete(`/ai/provider/${id}`)
}

/**
 * 测试供应商连接
 */
export function providerTest(data) {
  return request.post('/ai/provider/test', data)
}

/**
 * 设为默认供应商
 */
export function providerSetDefault(id) {
  return request.put(`/ai/provider/${id}/default`)
}

/**
 * 获取供应商模板列表
 */
export function providerTemplates() {
  return request.get('/ai/provider/templates')
}

// ========== 模型管理 ==========

/**
 * 分页查询模型列表
 */
export function modelPage(params) {
  return request.get('/ai/model/page', { params })
}

/**
 * 按供应商查询所有模型（下拉选择用）
 */
export function modelListByProvider(providerId) {
  return request.get('/ai/model/list', { params: { providerId } })
}

/**
 * 查询模型详情
 */
export function modelGetById(id) {
  return request.get(`/ai/model/${id}`)
}

/**
 * 新增模型
 */
export function modelAdd(data) {
  return request.post('/ai/model', data)
}

/**
 * 修改模型
 */
export function modelUpdate(data) {
  return request.put('/ai/model', data)
}

/**
 * 删除模型
 */
export function modelDelete(id) {
  return request.delete(`/ai/model/${id}`)
}

// ========== 会话管理 ==========

export function sessionPage(params) {
  return request.get('/ai/admin/session/page', { params })
}

export function sessionMessages(sessionId) {
  return request.get(`/ai/admin/session/${sessionId}/messages`)
}

export function sessionDelete(sessionId) {
  return request.delete(`/ai/admin/session/${sessionId}`)
}

export function sessionStatistics() {
  return request.get('/ai/admin/session/statistics')
}

// ========== AiClient 通用调用 ==========

export function aiClientCall(data) {
  return request.post('/ai/client/call', data)
}

export function aiClientStream(data) {
  return request.post('/ai/client/stream', data)
}

// ========== 上下文配置管理 ==========

export function contextConfigList(agentCode) {
  return request.get('/ai/context/list', { params: { agentCode } })
}

export function contextConfigAdd(data) {
  return request.post('/ai/context/add', data)
}

export function contextConfigUpdate(data) {
  return request.put('/ai/context/update', data)
}

export function contextConfigDelete(id) {
  return request.delete(`/ai/context/${id}`)
}

// ========== CRUD 配置驱动 ==========

export function crudConfigPage(params) {
  return request.get('/ai/crud-config/page', { params })
}

export function crudConfigGetById(id) {
  return request.get(`/ai/crud-config/${id}`)
}

export function crudConfigGetByKey(configKey) {
  return request.get(`/ai/crud-config/by-key/${configKey}`)
}

export function updateSessionMetadata(sessionId, metadata) {
  return request.put(`/ai/admin/session/${sessionId}/metadata`, metadata)
}

export function crudConfigRender(configKey) {
  return request.get(`/ai/crud-config/render/${configKey}`)
}

export function crudConfigAdd(data) {
  return request.post('/ai/crud-config', data)
}

export function crudConfigUpdate(data) {
  return request.put('/ai/crud-config', data)
}

export function crudConfigDelete(id) {
  return request.delete(`/ai/crud-config/${id}`)
}

export function crudConfigAiGenerate(data) {
  return request.post('/ai/crud-config/ai/generate', data)
}

export function crudConfigAiGenerateFromTable(data) {
  return request.post('/ai/crud-config/ai/generateFromTable', data)
}
