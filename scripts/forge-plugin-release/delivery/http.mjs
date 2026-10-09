import https from 'node:https'
import { randomUUID } from 'node:crypto'
import { apiBase, readResponse } from '../../forge-plugin-builder/worker-http.mjs'
import { ensure, BuildError } from '../../forge-plugin-builder/errors.mjs'

export function deliveryRequest(connection, action, body, options = {}) {
  ensure(/^[a-f0-9-]{36}$/.test(connection.taskId) && /^[a-f0-9]{64}$/.test(connection.token)
    && ['claim', 'authorize', 'heartbeat', 'finish', 'recovery'].includes(action), 'DELIVERY_REQUEST_INVALID')
  const payload = Buffer.from(JSON.stringify(body))
  ensure(payload.length <= 65536 && !options.signal?.aborted, 'INTERRUPTED')
  const url = apiBase(connection.baseUrl) + '/internal/plugin-delivery/' + connection.taskId + '/' + action
  return new Promise((resolve, reject) => {
    const request = https.request(url, { method: 'POST', rejectUnauthorized: true, headers: {
      Authorization: 'Bearer ' + connection.token, 'Content-Type': 'application/json',
      'Content-Length': payload.length, 'X-Timestamp': String(Date.now()), 'X-Nonce': randomUUID(),
    } }, response => readResponse(response, 'finish', 65536).then(resolve, reject))
    const abort = () => request.destroy(new BuildError('DELIVERY_REQUEST_ABORTED'))
    const timer = setTimeout(abort, 3000)
    options.signal?.addEventListener('abort', abort, { once: true })
    request.on('error', () => reject(new BuildError('DELIVERY_HTTP_FAILED')))
    request.on('close', () => {
      clearTimeout(timer)
      options.signal?.removeEventListener('abort', abort)
    })
    request.end(payload)
  })
}
