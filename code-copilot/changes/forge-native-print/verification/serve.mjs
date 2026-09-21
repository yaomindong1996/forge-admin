import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const workspace = process.argv.includes('--workspace')
const persistence = process.argv.includes('--persistence') || workspace
const { printMockPlugin } = await import('./m3b-server.mjs')
const root = path.dirname(fileURLToPath(import.meta.url))
const repo = path.resolve(root, '../../../..')
const ui = path.join(repo, 'forge-admin-ui')
const require = createRequire(path.join(ui, 'package.json'))
const { createServer, build } = await import(pathToFileURL(require.resolve('vite')).href)
const { default: vue } = await import(pathToFileURL(require.resolve('@vitejs/plugin-vue')).href)
const workspaceEntryPlugin = {
  name: 'forge-print-workspace-entry',
  configureServer(server) {
    server.middlewares.use((request, response, next) => {
      const url = new URL(request.url || '/', 'http://127.0.0.1')
      if (/^\/app-center\/application\/[^/]+$/.test(url.pathname) || /^\/print\/(?:designer|preview)$/.test(url.pathname))
        request.url = `/workspace.html${url.search}`
      next()
    })
  },
}
const config = {
  configFile: false,
  root,
  cacheDir: '/private/tmp/forge-print-verification-cache',
  plugins: [...(workspace ? [workspaceEntryPlugin] : []), vue(), ...(persistence ? [printMockPlugin(root, workspace ? { applicationId: '2', sourceType: 'LOWCODE', pageId: 'page_purchase', formKey: null, objectCode: 'purchase' } : null)] : [])],
  resolve: { alias: { ...(persistence ? { '@/api/print': path.join(root, 'm3b-api.js'), '@/store': path.join(root, 'm3b-mocks.js'), '@/composables/useDict': path.join(root, 'm3b-mocks.js'), 'vue-router': path.join(ui, 'node_modules/vue-router/dist/vue-router.mjs') } : {}), '@': path.join(ui, 'src'), pinia: path.join(ui, 'node_modules/pinia/dist/pinia.mjs'), vue: path.join(ui, 'node_modules/vue/dist/vue.runtime.esm-bundler.js'), 'naive-ui': path.join(ui, 'node_modules/naive-ui/es/index.mjs') } },
  server: { host: '127.0.0.1', port: 4318, strictPort: true, fs: { allow: [repo] } },
  build: { outDir: '/private/tmp/forge-print-verification-dist', ...(persistence ? { rollupOptions: { input: path.join(root, workspace ? 'workspace.html' : 'persistence.html') } } : {}), emptyOutDir: true },
}
if (process.argv.includes('--build')) {
  await build(config)
}
else {
  const server = await createServer(config)
  await server.listen()
  server.printUrls()
}
