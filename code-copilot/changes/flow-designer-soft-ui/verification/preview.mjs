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
const { presetIcons, presetWind3 } = await import(pathToFileURL(require.resolve('unocss')).href)
const collections = {}
for (const name of ['lucide', 'mdi', 'material-symbols']) {
  const data = JSON.parse(await readFile(require.resolve(`@iconify/json/json/${name}.json`), 'utf8'))
  collections[name] = () => data
}
const runtime = path.join(root, 'runtime.js')
const { NODE_PRESENTATION } = await import(pathToFileURL(path.join(frontend,
  'src/components/flow-designer/constants/node-presentation.js')).href)
const server = await createServer({
  configFile: false,
  root,
  plugins: [vue(), unocss({ configFile: false,
    safelist: Object.values(NODE_PRESENTATION).map(item => item.icon),
    presets: [presetWind3(), presetIcons({ collections })] })],
  resolve: { alias: [
    { find: '@/utils/http', replacement: runtime },
    { find: '@/utils/request', replacement: runtime },
    { find: '@/utils/encrypt-request', replacement: runtime },
    { find: /^@\/utils$/, replacement: runtime },
    { find: /^\.\/http$/, replacement: runtime },
    { find: '@/store/modules/auth', replacement: runtime },
    { find: '@/store/modules/user', replacement: runtime },
    { find: /^@\/store$/, replacement: runtime },
    { find: '@', replacement: path.join(frontend, 'src') },
    { find: 'vue', replacement: require.resolve('vue/dist/vue.runtime.esm-bundler.js') },
    { find: 'pinia', replacement: path.join(frontend, 'node_modules/pinia/dist/pinia.mjs') },
    { find: 'naive-ui', replacement: path.join(frontend, 'node_modules/naive-ui/es/index.mjs') },
  ] },
  server: { host: '127.0.0.1', port: 4319, strictPort: true,
    fs: { allow: [root, frontend, path.resolve(frontend, '..')] } },
  cacheDir: '/private/tmp/forge-flow-soft-ui-vite',
})
await server.listen()
process.stdout.write('本地组件验收（无真实接口）：http://127.0.0.1:4319/\n')
