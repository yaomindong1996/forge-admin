import { archiveLimit, check, jsonLimit, MarketError, parseJson } from './config.mjs'

export function apiUrl(config, route) {
  check(/^\/api\/[A-Za-z0-9/?=&._-]+$/.test(route), 'MARKET_ROUTE_INVALID')
  return `${config.apiBaseUrl}${route}`
}

export async function request(config, route, options = {}) {
  const timer = new AbortController()
  const timeout = setTimeout(() => timer.abort(), options.zip ? 30000 : 3000)
  try {
    const response = await fetch(apiUrl(config, route), {
      method: options.body ? 'POST' : 'GET', redirect: 'error', cache: 'no-store',
      signal: options.signal ? AbortSignal.any([options.signal, timer.signal]) : timer.signal,
      headers: headers(options), body: options.body ? JSON.stringify(options.body) : undefined,
    })
    check(response.ok, response.status === 401 ? 'MARKET_LOGIN_REQUIRED' : 'MARKET_REQUEST_REJECTED')
    const type = response.headers.get('content-type')?.split(';')[0].trim()
    check(type === (options.zip ? 'application/zip' : 'application/json'), 'MARKET_CONTENT_TYPE_INVALID')
    const data = await boundedBody(response, options.zip ? archiveLimit : jsonLimit)
    if (options.zip) return { data, sha256: response.headers.get('x-source-sha256') }
    const result = parseJson(data)
    check(result && [0, 200].includes(result.code) && Object.hasOwn(result, 'data'), 'MARKET_REQUEST_REJECTED')
    return result.data
  }
  catch (error) {
    if (error instanceof MarketError) throw error
    throw new MarketError(options.signal?.aborted ? 'MARKET_INTERRUPTED' : 'MARKET_NETWORK_FAILED')
  }
  finally { clearTimeout(timeout) }
}

function headers(options) {
  const value = { Accept: options.zip ? 'application/zip' : 'application/json' }
  if (options.body) value['Content-Type'] = 'application/json'
  if (options.session) {
    value.Authorization = `Bearer ${options.session.token}`
    value['X-Forge-Docs-Device-Id'] = options.session.deviceId
  }
  return value
}

async function boundedBody(response, limit) {
  const length = response.headers.get('content-length')
  if (length !== null) check(/^\d+$/.test(length) && Number(length) <= limit, 'MARKET_RESPONSE_TOO_LARGE')
  check(response.body, 'MARKET_RESPONSE_EMPTY')
  const reader = response.body.getReader()
  const chunks = []
  let size = 0
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      size += value.length
      check(size <= limit, 'MARKET_RESPONSE_TOO_LARGE')
      chunks.push(value)
    }
    check(length === null || size === Number(length), 'MARKET_RESPONSE_LENGTH_INVALID')
    return Buffer.concat(chunks, size)
  }
  finally { await reader.cancel().catch(() => {}); reader.releaseLock() }
}
