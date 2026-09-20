import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const uniPlugin = require('@dcloudio/vite-plugin-uni')

import { defineConfig, loadEnv } from 'vite'
import Unocss from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import path from 'node:path'
import { pluginIcons, pluginPagePathes } from './build/plugin-isme/index.js'

const uni = uniPlugin.default

export default defineConfig(({ mode }) => {
  const viteEnv = loadEnv(mode, process.cwd())
  const { VITE_HTTP_PORT, VITE_REQUEST_PREFIX, VITE_PUBLIC_PATH, VITE_HTTP_PROXY_TARGET, VITE_FLOW_PROXY_TARGET } = viteEnv
  const requestPrefix = VITE_REQUEST_PREFIX || '/dev-api'
  const proxyTarget = VITE_HTTP_PROXY_TARGET || 'http://127.0.0.1:8581/'
  const flowProxyTarget = VITE_FLOW_PROXY_TARGET || proxyTarget

  return {
    base: VITE_PUBLIC_PATH || '/',
    plugins: [
      uni(),
      Unocss(),
      AutoImport({
        imports: ['vue'],
        dts: false,
      }),
      Components({
        dts: false,
      }),
      pluginPagePathes(),
      pluginIcons(),
    ],
    resolve: {
      alias: {
        '@': path.resolve(process.cwd(), 'src'),
        '~': path.resolve(process.cwd()),
      },
    },
    define: {
      global: 'window',
    },
    optimizeDeps: {
      // Wot Design Uni publishes source Vue/TS. Excluding it avoids H5 dev
      // pre-bundling a second reactive runtime and keeps locale/theme state shared.
      exclude: ['wot-design-uni'],
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@import "@/styles/variables.scss";`,
        },
      },
    },
    server: {
      port: VITE_HTTP_PORT || 3009,
      open: false,
      host: '0.0.0.0',
      proxy: {
        // Flowable 流程服务代理：待办、审批、流程历史等接口默认走独立 flow-server。
        [`${requestPrefix}/api/flow`]: {
          target: flowProxyTarget,
          changeOrigin: true,
          secure: false,
          rewrite: path => path.replace(new RegExp(`^${requestPrefix}`), ''),
          configure: (proxy, options) => {
            proxy.on('proxyRes', (proxyRes, req) => {
              proxyRes.headers['x-real-url'] = new URL(req.url || '', options.target)?.href || ''
            })
          },
        },
        // 业务待办表单/动作由 flow-server 内的 BusinessFlowController 提供。
        // 该规则必须在通用 app-server 代理之前，否则会被转发到 8583 并返回 404。
        [`${requestPrefix}/ai/business/flow`]: {
          target: flowProxyTarget,
          changeOrigin: true,
          secure: false,
          rewrite: path => path.replace(new RegExp(`^${requestPrefix}`), ''),
          configure: (proxy, options) => {
            proxy.on('proxyRes', (proxyRes, req) => {
              proxyRes.headers['x-real-url'] = new URL(req.url || '', options.target)?.href || ''
            })
          },
        },
        // Forge App 服务代理：H5 登录、用户信息、验证码等基础接口默认走 app-server。
        [requestPrefix]: {
          target: proxyTarget,
          changeOrigin: true,
          secure: false,
          rewrite: path => path.replace(new RegExp(`^${requestPrefix}`), ''),
          configure: (proxy, options) => {
            proxy.on('proxyRes', (proxyRes, req) => {
              proxyRes.headers['x-real-url'] = new URL(req.url || '', options.target)?.href || ''
            })
          },
        },
        // WebSocket
        '/ws': {
          target: proxyTarget,
          changeOrigin: true,
          ws: true,
          secure: false,
        },
      },
    },
    build: {
      chunkSizeWarningLimit: 1024,
    },
  }
})
