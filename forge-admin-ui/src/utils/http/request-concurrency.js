/**
 * 限制同源并发请求，避免 Chrome 6 连接打满后后续请求一直 Stalled、axios 超时也不触发。
 */

const DEFAULT_LIMIT = 4

const queues = new Map()

function resolveKey(config = {}) {
  const url = String(config.url || '')
  if (url.includes('/ai/crud/') && (url.includes('/page') || url.includes('/tree')))
    return 'ai-crud-page'
  if (url.includes('/ai/crud-config/render/'))
    return 'ai-crud-render'
  if (url.includes('/ai/business/document/') && url.includes('/runtime'))
    return 'ai-document-runtime'
  return ''
}

function getQueue(key) {
  if (!queues.has(key)) {
    queues.set(key, {
      active: 0,
      limit: DEFAULT_LIMIT,
      waiters: [],
    })
  }
  return queues.get(key)
}

function pump(key) {
  const queue = getQueue(key)
  while (queue.active < queue.limit && queue.waiters.length) {
    const next = queue.waiters.shift()
    queue.active += 1
    next()
  }
}

export function acquireRequestSlot(config = {}, waitTimeoutMs = 8000) {
  const key = resolveKey(config)
  if (!key)
    return Promise.resolve(() => {})

  const queue = getQueue(key)
  return new Promise((resolve, reject) => {
    let settled = false
    const grant = () => {
      if (settled)
        return
      settled = true
      clearTimeout(timer)
      let released = false
      resolve(() => {
        if (released)
          return
        released = true
        queue.active = Math.max(0, queue.active - 1)
        pump(key)
      })
    }
    const timer = setTimeout(() => {
      if (settled)
        return
      settled = true
      const idx = queue.waiters.indexOf(grant)
      if (idx >= 0)
        queue.waiters.splice(idx, 1)
      const error = new Error(`timeout of ${waitTimeoutMs}ms exceeded waiting for request slot`)
      error.code = 'ECONNABORTED'
      reject(error)
    }, waitTimeoutMs)

    if (queue.active < queue.limit) {
      queue.active += 1
      grant()
      return
    }
    queue.waiters.push(grant)
  })
}
