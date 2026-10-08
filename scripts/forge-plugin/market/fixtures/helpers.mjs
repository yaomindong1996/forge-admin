import http from 'node:http'
import { createHash } from 'node:crypto'
import path from 'node:path'
import { descriptor, pluginFiles, project, write, zip } from '../../fixtures/helpers.mjs'

export const sha256 = data => createHash('sha256').update(data).digest('hex')
export const session = () => ({ token: 'fixture-customer-token', deviceId: 'fixture-device-id' })

export async function marketFixture(t, options = {}) {
  const host = await project(t, options.generated)
  const data = zip(pluginFiles(options.descriptor || descriptor()))
  const release = { id: '9223372036854775807', version: '1.0.0', edition: 'community',
    requiresCore: '>=1.2.0 <2.0.0', sha256: sha256(data), archiveBytes: data.length, status: 'PUBLISHED' }
  const requests = []
  const routes = {
    '/api/plugins/mine': { records: [{ pluginId: 'demo', name: '测试插件', downloadable: true }], total: 1 },
    '/api/plugins/demo/access': true,
    '/api/plugins/demo/versions': { records: [release], total: 1 },
  }
  const server = http.createServer((req, res) => {
    requests.push({ url: req.url, token: req.headers.authorization, device: req.headers['x-forge-docs-device-id'] })
    const route = new URL(req.url, 'http://localhost').pathname
    if (options.handler?.(req, res, route)) return
    if (route === `/api/plugins/versions/${release.id}/download`) {
      res.setHeader('Content-Type', 'application/zip')
      res.setHeader('X-Source-SHA256', release.sha256)
      res.end(options.download || data)
      return
    }
    res.setHeader('Content-Type', 'application/json')
    res.end(JSON.stringify({ code: 200, data: routes[route] }))
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  t.after(async () => { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)) })
  const base = `http://127.0.0.1:${server.address().port}`
  const config = { protocolVersion: 1, apiBaseUrl: base, authorizeUrl: `${base}/account/sso`, clientId: 'forge-cli' }
  const file = path.join(host.root, 'market.json')
  await write(host.root, 'market.json', JSON.stringify(config))
  const logs = []
  const errors = []
  return { ...host, config, file, data, release, routes, requests, logs, errors,
    output: { log: value => logs.push(value), error: value => errors.push(value) },
    environment: { root: host.root, login: async () => session() } }
}
