import { createHash, randomUUID } from 'node:crypto'
import { ensure } from '../../forge-plugin-builder/errors.mjs'
import { parseStrictJson } from '../../forge-plugin/json.mjs'
import { setTimeout as pause } from 'node:timers/promises'

export async function verifyRuntime(config, manifest, dependencies = {}) {
  const fetcher = readyFetcher(dependencies)
  if (manifest.targets.includes('server')) {
    const token = process.env.FORGE_RUNTIME_PROBE_TOKEN
    ensure(typeof token === 'string' && /^[a-f0-9]{64}$/.test(token), 'RUNTIME_PROBE_TOKEN_REQUIRED')
    const nonce = randomUUID()
    const response = await fetcher('http://127.0.0.1:' + config.serverPort + '/internal/plugin-runtime/probe', {
      headers: { Authorization: 'Bearer ' + token, 'X-Forge-Probe-Nonce': nonce,
        'X-Timestamp': String(Date.now()), 'X-Nonce': randomUUID() },
      redirect: 'error', cache: 'no-store', signal: AbortSignal.timeout(10000),
    })
    const data = await readHttp(response, { bytes: 65536, capture: true, exact: false })
    const envelope = parseStrictJson(data.data.toString('utf8'), 65536)
    const value = envelope?.data
    ensure(envelope?.code === 200 && value?.nonce === nonce && value.coreVersion === manifest.plugin.coreVersion,
      'RUNTIME_PROBE_MISMATCH')
    const jar = manifest.artifacts.find(item => item.path === 'backend/admin.jar')
    ensure(jar && value.jarSha256 === jar.sha256 && Array.isArray(value.plugins)
      && value.plugins.some(item => item.id === manifest.plugin.id && item.version === manifest.plugin.version
        && item.backendLoaded === true), 'RUNTIME_PLUGIN_MISMATCH')
  }
  for (const item of manifest.artifacts.filter(file => file.path.startsWith('frontend/'))) {
    const relative = item.path.slice('frontend/'.length).split('/').map(encodeURIComponent).join('/')
    const response = await fetcher('http://127.0.0.1:' + config.uiPort + '/' + relative, {
      redirect: 'error', cache: 'no-store', signal: AbortSignal.timeout(30000),
      headers: { 'Accept-Encoding': 'identity' },
    })
    await readHttp(response, item)
  }
  return true
}
function readyFetcher(dependencies) {
  const fetcher = dependencies.fetch || fetch
  const deadline = Date.now() + (dependencies.startupTimeoutMs ?? 120000)
  return async (url, options) => {
    while (true) {
      ensure(!dependencies.signal?.aborted, 'INTERRUPTED')
      try {
        const response = await fetcher(url, { ...options,
          signal: AbortSignal.any([options.signal, ...(dependencies.signal ? [dependencies.signal] : [])]) })
        // 仅启动暂不可用允许重试；401、摘要/版本不一致不重试。
        if (![502, 503, 504].includes(response.status)) return response
        await response.body?.cancel()
      }
      catch (failure) {
        if (!(failure instanceof TypeError) && failure.name !== 'TimeoutError') throw failure
      }
      ensure(Date.now() < deadline, 'RUNTIME_STARTUP_TIMEOUT')
      await pause(Math.min(1000, Math.max(1, deadline - Date.now())), undefined,
        { signal: dependencies.signal })
    }
  }
}
export async function readHttp(response, expected) {
  ensure(response.status === 200 && response.body, 'RUNTIME_HTTP_REJECTED')
  const hash = createHash('sha256')
  let bytes = 0
  const chunks = []
  try {
    for await (const chunk of response.body) {
      bytes += chunk.length
      ensure(bytes <= expected.bytes, 'RUNTIME_SIZE_LIMIT')
      hash.update(chunk)
      if (expected.capture) chunks.push(Buffer.from(chunk))
    }
    ensure(expected.exact === false || bytes === expected.bytes, 'RUNTIME_SIZE_MISMATCH')
    ensure(!expected.sha256 || hash.digest('hex') === expected.sha256, 'RUNTIME_DIGEST_MISMATCH')
    return { bytes, data: expected.capture ? Buffer.concat(chunks) : undefined }
  }
  catch (failure) {
    await response.body.cancel?.().catch(() => {})
    throw failure
  }
}
