import https from 'node:https'
import { randomUUID } from 'node:crypto'
import { BuildError, ensure } from './errors.mjs'
import { parseStrictJson } from '../forge-plugin/json.mjs'

export function apiBase(value) {
  ensure(typeof value === 'string' && value.length <= 512, 'WORKER_URL_INVALID')
  const url = new URL(value)
  ensure(url.protocol === 'https:' && !url.username && !url.password && !url.search && !url.hash
    && /^(\/[a-zA-Z0-9_-]+)*\/?$/.test(url.pathname)
    && url.href.replace(/\/$/, '') === value.replace(/\/$/, ''), 'WORKER_HTTPS_REQUIRED')
  return url.href.replace(/\/$/, '')
}

// 不继承代理/内部调用头，不跟随重定向，不关闭 TLS 校验；原始响应/凭证不进入错误日志。
export function workerRequest(connection, action, body, options = {}) {
  const { taskId, token, baseUrl } = connection
  ensure(/^[a-f0-9-]{36}$/.test(taskId) && /^[a-f0-9]{64}$/.test(token), 'WORKER_IDENTITY_INVALID')
  ensure(['claim', 'archive', 'heartbeat', 'finish', 'approval-check'].includes(action), 'WORKER_ACTION_INVALID')
  const payload = Buffer.from(JSON.stringify(body))
  ensure(payload.length <= 65536 && !options.signal?.aborted, 'WORKER_REQUEST_INVALID')
  const url = new URL(`${apiBase(baseUrl)}/internal/plugin-build/${taskId}/${action}`)
  const limit = action === 'archive' ? 8 * 1024 * 1024 : 65536
  return new Promise((resolve, reject) => {
    let timer
    const abort = () => request.destroy(new BuildError('WORKER_REQUEST_ABORTED'))
    const request = https.request(url, { method: 'POST', headers: { 'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json', 'Content-Length': payload.length,
      'X-Timestamp': String(Date.now()), 'X-Nonce': randomUUID() }, rejectUnauthorized: true }, (response) => {
      readResponse(response, action, limit).then(resolve, reject)
    })
    const cleanup = () => {
      clearTimeout(timer)
      options.signal?.removeEventListener('abort', abort)
    }
    request.on('error', () => { cleanup(); reject(new BuildError('WORKER_HTTP_FAILED')) })
    request.on('close', cleanup)
    timer = setTimeout(() => request.destroy(new BuildError('WORKER_HTTP_TIMEOUT')), 3000)
    options.signal?.addEventListener('abort', abort, { once: true })
    request.end(payload)
  })
}

export async function readResponse(response, action, limit) {
  const type = response.headers['content-type'] || ''
  const expected = action === 'archive' ? 'application/octet-stream' : 'application/json'
  if (response.statusCode !== 200 || !type.startsWith(expected)) {
    response.destroy()
    throw new BuildError('WORKER_RESPONSE_REJECTED')
  }
  const chunks = []
  let bytes = 0
  try {
    for await (const chunk of response) {
      bytes += chunk.length
      ensure(bytes <= limit, 'WORKER_RESPONSE_SIZE_LIMIT')
      chunks.push(chunk)
    }
    const data = Buffer.concat(chunks)
    if (action === 'archive') {
      ensure(data.length > 0, 'WORKER_PACKAGE_EMPTY')
      return data
    }
    const envelope = parseStrictJson(data.toString('utf8'), limit)
    ensure(envelope?.code === 200 && envelope.data && typeof envelope.data === 'object', 'WORKER_RESPONSE_INVALID')
    return envelope.data
  }
  catch {
    response.destroy()
    throw new BuildError('WORKER_RESPONSE_INVALID')
  }
}
