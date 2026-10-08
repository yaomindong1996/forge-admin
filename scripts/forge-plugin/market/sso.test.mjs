import test from 'node:test'
import assert from 'node:assert/strict'
import http from 'node:http'
import { createHash } from 'node:crypto'
import { callbackCode, customerLogin, loginTransaction } from './sso.mjs'
import { marketFixture, session } from './fixtures/helpers.mjs'

const code = 'c'.repeat(43)

test('PKCE uses random transaction and SHA256, callback rejects host/path/method/duplicates/wrong state', () => {
  const config = { authorizeUrl: 'https://example.invalid/account/sso', clientId: 'forge-cli' }
  const transaction = loginTransaction(config)
  const other = loginTransaction(config)
  const url = new URL(transaction.authorizeUrl)
  assert.equal(url.searchParams.get('codeChallenge'),
    createHash('sha256').update(transaction.verifier).digest('base64url'))
  assert.notEqual(transaction.state, other.state)
  assert.ok(!transaction.authorizeUrl.includes(transaction.verifier))
  const req = { method: 'GET', headers: { host: '127.0.0.1:37329' },
    url: `/callback?code=${code}&state=${transaction.state}` }
  assert.equal(callbackCode(req, transaction.state), code)
  const invalid = [{ method: 'POST' }, { headers: { host: 'evil.example.invalid' } },
    { url: req.url + '&code=' + code }, { url: req.url + '&extra=x' },
    { url: req.url.replace('/callback', '/other') }, { url: req.url.replace(transaction.state, other.state) },
    { url: 'http://evil.example.invalid/callback?' + url.searchParams }]
  for (const value of invalid) assert.throws(() => callbackCode({ ...req, ...value }, transaction.state))
})

test('actual loopback callback exchanges code with exact verifier, closes listener and clears timeout', async t => {
  let authorize
  const fixture = await marketFixture(t, { handler: (req, res, route) => {
    if (route !== '/api/customer-sso/exchange') return false
    const chunks = []
    req.on('data', data => chunks.push(data))
    req.on('end', () => {
      const body = JSON.parse(Buffer.concat(chunks))
      assert.equal(body.code, code)
      assert.equal(body.clientId, 'forge-cli')
      assert.equal(body.redirectUri, 'http://127.0.0.1:37329/callback')
      assert.equal(createHash('sha256').update(body.codeVerifier).digest('base64url'),
        authorize.searchParams.get('codeChallenge'))
      assert.equal(body.state, authorize.searchParams.get('state'))
      res.setHeader('Content-Type', 'application/json')
      res.end(JSON.stringify({ code: 200, data: session() }))
    })
    return true
  } })
  let callback
  const result = await customerLogin(fixture.config, { notify: value => {
    authorize = new URL(value)
    const url = new URL(authorize.searchParams.get('redirectUri'))
    url.search = new URLSearchParams({ code, state: authorize.searchParams.get('state') })
    callback = fetch(url).then(res => res.text())
  } })
  assert.deepEqual(result, session())
  assert.match(await callback, /返回终端/)
  await assert.rejects(fetch('http://127.0.0.1:37329/callback'))
})

test('loopback timeout, occupied port and abort produce fixed codes and no leaked listener', async t => {
  const fixture = await marketFixture(t)
  await assert.rejects(customerLogin(fixture.config, { timeoutMs: 20 }), /MARKET_LOGIN_TIMEOUT/)
  const server = http.createServer()
  await new Promise(resolve => server.listen(37329, '127.0.0.1', resolve))
  try { await assert.rejects(customerLogin(fixture.config), /MARKET_CALLBACK_UNAVAILABLE/) }
  finally { await new Promise(resolve => server.close(resolve)) }
  const controller = new AbortController()
  await assert.rejects(customerLogin(fixture.config, { signal: controller.signal,
    notify: () => controller.abort() }), /MARKET_INTERRUPTED/)
  await assert.rejects(fetch('http://127.0.0.1:37329/callback'))
})
