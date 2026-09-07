import path from 'node:path'
import { fileURLToPath } from 'node:url'
import Vue from '@vitejs/plugin-vue'
import Unocss from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'

const root = path.dirname(fileURLToPath(import.meta.url))
const project = path.resolve(root, '../../../..')
const ui = path.join(project, 'forge-admin-ui')

export default defineConfig({
  root,
  plugins: [Vue(), Unocss({ configFile: path.join(ui, 'uno.config.js') }), AutoImport({ imports: ['vue', 'vue-router'], dts: false }), Components({ resolvers: [NaiveUiResolver()], dts: false })],
  resolve: { alias: { '@': path.join(ui, 'src'), '~': ui, 'vue-router/auto-routes': path.join(root, 'auto-routes.js') }, dedupe: ['vue', 'pinia', 'vue-router'] },
  define: { global: 'globalThis' },
  css: { preprocessorOptions: { scss: { additionalData: '@use "@/styles/variables.scss";' } } },
  server: { host: '127.0.0.1', port: 5188, strictPort: true, fs: { allow: [project] } },
})
