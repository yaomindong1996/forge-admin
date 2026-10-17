import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = path.dirname(fileURLToPath(import.meta.url))
const frontend = fileURLToPath(new URL('../../../../forge-admin-ui/', import.meta.url))
const require = createRequire(path.join(frontend, 'package.json'))
const { createServer } = await import(pathToFileURL(require.resolve('vite')).href)
const { default: vue } = await import(pathToFileURL(require.resolve('@vitejs/plugin-vue')).href)
const { default: unocss } = await import(pathToFileURL(require.resolve('unocss/vite')).href)
const { presetIcons } = await import(pathToFileURL(require.resolve('unocss')).href)
const lucide = JSON.parse(await readFile(require.resolve('@iconify/json/json/lucide.json'), 'utf8'))
const runtime = path.join(root, 'runtime.js')
let mode = 'normal'
const records = [
  { id: 'fixture-1', fileId: 'fixture-report', fileName: '采购订单_20261017.xlsx',
    fileSize: 2457600, source: 'export', createTime: new Date().toISOString() },
  { id: 'fixture-2', fileId: 'fixture-contract', fileName: '设备采购合同.pdf',
    fileSize: 1843200, source: 'file', createTime: new Date().toISOString() },
  { id: 'fixture-3', fileId: 'fixture-config', fileName: '应用配置_业务工作台.zip',
    fileSize: 8704000, source: 'file', createTime: '2026-10-15T16:38:00' },
  { id: 'fixture-4', fileName: '审批记录导出.xlsx', fileSize: 350208,
    source: 'export', createTime: '2026-10-14T09:42:00' },
]
const json = (response, data) => {
  response.setHeader('Content-Type', 'application/json')
  response.end(JSON.stringify(data))
}

function fixtureMiddleware(request, response, next) {
  const url = new URL(request.url, 'http://127.0.0.1')
  if (url.pathname === '/__fixture/mode') {
    mode = url.searchParams.get('value') || 'normal'
    return json(response, { code: 200 })
  }
  if (url.pathname === '/system/file/download-log/page') {
    return json(response, { code: mode === 'error' ? 500 : 200,
      data: { records: mode === 'empty' ? [] : records, total: mode === 'empty' ? 0 : records.length } })
  }
  if (url.pathname.startsWith('/system/dict/data/type/')) {
    return json(response, { code: 200, data: [{ dictValue: 'file', dictLabel: '文件' },
      { dictValue: 'export', dictLabel: '导出' }] })
  }
  if (url.pathname.startsWith('/api/file/url/'))
    return json(response, { code: 200, data: `/api/file/download/${url.pathname.split('/').pop()}` })
  if (url.pathname.startsWith('/api/file/download/')) {
    if (mode === 'failed') {
      response.statusCode = 503
      return response.end('local fixture failure')
    }
    response.writeHead(200, { 'Content-Type': 'text/plain', 'Content-Length': '307200',
      'Content-Disposition': 'attachment; filename="fixture-download.txt"' })
    let sent = 0
    const timer = setInterval(() => {
      response.write(Buffer.alloc(10240, 65))
      sent++
      if (sent === 30) {
        clearInterval(timer)
        response.end()
      }
    }, 300)
    response.on('close', () => clearInterval(timer))
    return
  }
  next()
}

const server = await createServer({
  configFile: false,
  root,
  plugins: [vue(), unocss({ configFile: false, presets: [presetIcons({ collections: { lucide: () => lucide } })] }),
    { name: 'local-download-fixtures', configureServer(server) {
      server.middlewares.use(fixtureMiddleware)
    } }],
  resolve: { alias: [
    { find: '@/store/modules/auth', replacement: runtime },
    { find: '@/store/modules/user', replacement: runtime },
    { find: '@/store', replacement: runtime },
    { find: '@/utils/http', replacement: runtime },
    { find: /^@\/utils$/, replacement: runtime },
    { find: /^\.\/http$/, replacement: runtime },
    { find: '@', replacement: path.join(frontend, 'src') },
    { find: 'vue', replacement: require.resolve('vue/dist/vue.runtime.esm-bundler.js') },
    { find: 'pinia', replacement: path.join(frontend, 'node_modules/pinia/dist/pinia.mjs') },
    { find: 'naive-ui', replacement: path.join(frontend, 'node_modules/naive-ui/es/index.mjs') },
  ] },
  server: { host: '127.0.0.1', port: 3041, strictPort: true,
    fs: { allow: [root, frontend, path.resolve(frontend, '..')] } },
  cacheDir: '/private/tmp/forge-downloads-verification-vite',
})
await server.listen()
process.stdout.write('Isolated production component verification: http://127.0.0.1:3041/\n')
