import test from 'node:test'
import assert from 'node:assert/strict'
import https from 'node:https'
import { EventEmitter } from 'node:events'
import { Readable } from 'node:stream'
import { randomBytes } from 'node:crypto'
import { workerRequest } from './worker-http.mjs'

const connection = { taskId: '00000000-0000-4000-8000-000000000000',
  token: randomBytes(32).toString('hex'), baseUrl: 'https://worker.example/api' }

function client(callback, respond = true) {
  const request = new EventEmitter()
  request.destroy = (error) => { request.emit('error', error); request.emit('close') }
  request.end = (body) => {
    request.body = body
    if (!respond) return
    const response = Readable.from([Buffer.from('{"code":200,"data":{"status":"building"}}')])
    response.statusCode = 200
    response.headers = { 'content-type': 'application/json' }
    response.on('end', () => request.emit('close'))
    callback(response)
  }
  return request
}

test('客户端仅固定 HTTPS POST，验证证书/防重放参数，不发送内部调用头或地址凭据', async (t) => {
  let observed
  t.mock.method(https, 'request', (url, options, callback) => {
    observed = { url, options, request: client(callback) }
    return observed.request
  })
  const value = await workerRequest(connection, 'claim', { leaseToken: randomBytes(32).toString('hex') })
  assert.equal(value.status, 'building')
  assert.equal(observed.url.href, 'https://worker.example/api/internal/plugin-build/'
    + connection.taskId + '/claim')
  assert.equal(observed.options.rejectUnauthorized, true)
  assert.equal(observed.options.method, 'POST')
  assert.equal(observed.options.headers.Authorization, 'Bearer ' + connection.token)
  assert.equal(observed.options.headers['Content-Length'], observed.request.body.length)
  assert.match(observed.options.headers['X-Nonce'], /^[a-f0-9-]{36}$/)
  assert.ok(Number(observed.options.headers['X-Timestamp']) > 0)
  assert.equal(observed.options.headers['X-Inner-Call'], undefined)
})

test('中止及三秒超时会销毁请求，不把连接错误/凭据原文传给上层', async (t) => {
  let destroyed = false
  t.mock.method(https, 'request', (url, options, callback) => {
    const request = client(callback, false)
    const original = request.destroy
    request.destroy = (error) => { destroyed = true; original(error) }
    return request
  })
  const controller = new AbortController()
  const pending = workerRequest(connection, 'heartbeat', {}, { signal: controller.signal })
  controller.abort()
  await assert.rejects(pending, /WORKER_HTTP_FAILED/)
  assert.equal(destroyed, true)
  destroyed = false
  const start = Date.now()
  await assert.rejects(workerRequest(connection, 'heartbeat', {}), /WORKER_HTTP_FAILED/)
  assert.ok(Date.now() - start >= 2900 && Date.now() - start < 6000)
  assert.equal(destroyed, true)
})
