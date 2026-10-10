import http from 'node:http'
import { createHash, randomBytes, timingSafeEqual } from 'node:crypto'
import { callbackUrl, check, MarketError } from './config.mjs'
import { request } from './http.mjs'

export function loginTransaction(config) {
  const verifier = randomBytes(32).toString('base64url')
  const state = randomBytes(32).toString('base64url')
  const codeChallenge = createHash('sha256').update(verifier).digest('base64url')
  const authorize = new URL(config.authorizeUrl)
  for (const [key, value] of Object.entries({ clientId: config.clientId, redirectUri: callbackUrl,
    state, codeChallenge })) authorize.searchParams.set(key, value)
  return { verifier, state, authorizeUrl: authorize.href }
}

export function callbackCode(req, state) {
  check(req.method === 'GET' && req.headers.host === '127.0.0.1:37329', 'MARKET_CALLBACK_INVALID')
  check(typeof req.url === 'string' && req.url.startsWith('/callback?') && req.url.length <= 1024,
    'MARKET_CALLBACK_INVALID')
  const url = new URL(req.url, callbackUrl)
  check(url.origin === new URL(callbackUrl).origin && url.pathname === '/callback' && !url.hash,
    'MARKET_CALLBACK_INVALID')
  check([...url.searchParams.keys()].length === 2 && url.searchParams.getAll('state').length === 1
    && url.searchParams.getAll('code').length === 1, 'MARKET_CALLBACK_INVALID')
  const received = url.searchParams.get('state') || ''
  check(/^[A-Za-z0-9_-]{43}$/.test(received) && received.length === state.length
    && timingSafeEqual(Buffer.from(received), Buffer.from(state)), 'MARKET_STATE_INVALID')
  const code = url.searchParams.get('code')
  check(/^[A-Za-z0-9_-]{43}$/.test(code), 'MARKET_CALLBACK_INVALID')
  return code
}

// 固定 loopback 回调必须由网站配置精确允许；不得为方便登录放开 wildcard redirect。
export async function customerLogin(config, options = {}) {
  const transaction = loginTransaction(config)
  const grant = await receiveGrant(transaction, options)
  const value = await request(config, '/api/customer-sso/exchange', { signal: options.signal,
    body: { clientId: config.clientId, redirectUri: callbackUrl, state: transaction.state,
      codeVerifier: transaction.verifier, code: grant } })
  check(value && typeof value.token === 'string' && /^[A-Za-z0-9._~+/=-]{1,8192}$/.test(value.token)
    && typeof value.deviceId === 'string' && /^[A-Za-z0-9_-]{8,128}$/.test(value.deviceId),
  'MARKET_SESSION_INVALID')
  return { token: value.token, deviceId: value.deviceId }
}

async function receiveGrant(transaction, options) {
  check(!options.signal?.aborted, 'MARKET_INTERRUPTED')
  let complete
  let rejectGrant
  const grant = new Promise((resolve, reject) => { complete = resolve; rejectGrant = reject })
  const server = http.createServer((req, res) => {
    res.setHeader('Cache-Control', 'no-store')
    res.setHeader('Referrer-Policy', 'no-referrer')
    res.setHeader('Content-Type', 'text/plain; charset=utf-8')
    res.setHeader('X-Content-Type-Options', 'nosniff')
    try {
      const code = callbackCode(req, transaction.state)
      res.end('登录确认已收到，请返回终端。')
      complete(code)
    }
    catch { res.statusCode = 400; res.end('回调无效，请使用当前终端显示的登录地址。') }
  })
  server.headersTimeout = 5000
  server.requestTimeout = 5000
  const timeout = setTimeout(() => rejectGrant(new MarketError('MARKET_LOGIN_TIMEOUT')), options.timeoutMs || 300000)
  const interrupted = () => rejectGrant(new MarketError('MARKET_INTERRUPTED'))
  options.signal?.addEventListener('abort', interrupted, { once: true })
  server.on('error', () => rejectGrant(new MarketError('MARKET_CALLBACK_UNAVAILABLE')))
  try {
    server.listen({ host: '127.0.0.1', port: 37329, exclusive: true }, () => {
      try { options.notify?.(transaction.authorizeUrl) }
      catch { rejectGrant(new MarketError('MARKET_LOGIN_NOTIFICATION_FAILED')) }
    })
    return await grant
  }
  finally {
    clearTimeout(timeout)
    options.signal?.removeEventListener('abort', interrupted)
    server.closeAllConnections()
    await new Promise(resolve => server.close(resolve))
  }
}
