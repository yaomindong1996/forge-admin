import path from 'node:path'
import Vue from '@vitejs/plugin-vue'
import VueJsx from '@vitejs/plugin-vue-jsx'
import Unocss from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import Components from 'unplugin-vue-components/vite'
import VueRouter from 'unplugin-vue-router/vite'
import { defineConfig, loadEnv } from 'vite'
import removeNoMatch from 'vite-plugin-router-warn'
import VueDevTools from 'vite-plugin-vue-devtools'
import { pluginIcons, pluginPagePathes } from './build/plugin-isme'

export default defineConfig(({ mode, command }) => {
  const viteEnv = loadEnv(mode, process.cwd())
  const { VITE_HTTP_PORT, VITE_REQUEST_PREFIX, VITE_PUBLIC_PATH, VITE_HTTP_PROXY_TARGET, VITE_FLOW_PROXY_TARGET } = viteEnv
  const isServe = command === 'serve'

  return {
    base: VITE_PUBLIC_PATH || '/',
    plugins: [
      // unplugin-vue-router 必须在 Vue 之前
      VueRouter({
        routesFolder: [
          {
            src: 'src/views',
            path: '',
            // 排除不需要生成路由的文件
            exclude: ['**/components/**', '**/api/**', '**/workspace/**'],
          },
        ],
        dts: false,
        watch: isServe,
      }),
      Vue(),
      VueJsx(),
      ...(isServe ? [VueDevTools()] : []),
      Unocss(),
      AutoImport({
        imports: ['vue', 'vue-router'],
        dts: false,
      }),
      Components({
        resolvers: [NaiveUiResolver()],
        dts: false,
      }),
      // 自定义插件，用于生成页面文件的path，并添加到虚拟模块
      pluginPagePathes(),
      // 自定义插件，用于生成自定义icon，并添加到虚拟模块
      pluginIcons(),
      // 移除非必要的vue-router动态路由警告: No match found for location with path
      removeNoMatch(),
    ],
    resolve: {
      alias: {
        '@': path.resolve(process.cwd(), 'src'),
        '~': path.resolve(process.cwd()),
      },
    },
    define: {
      // 为某些依赖（例如 browser-crypto）提供全局对象
      // globalThis 同时兼容主页面、Web Worker 和 SSR 构建；window 会让 module Worker
      // 在 Vite 注入 env.mjs 时直接抛出 ReferenceError。
      global: 'globalThis',
    },
    optimizeDeps: {
      include: [
        '@arco-design/color',
        '@codemirror/lang-java',
        '@codemirror/lang-javascript',
        'codemirror',
        '@codemirror/lang-sql',
        '@codemirror/lang-xml',
        '@codemirror/theme-one-dark',
        '@form-create/designer',
        // 表单设计器通过懒加载页面引入该语言包；显式预构建，避免首次进入设计页时
        // Vite 二次优化依赖并使浏览器中的 element-plus 旧 hash 返回 504。
        '@form-create/designer/locale/zh-cn.es',
        '@form-create/element-ui',
        '@stomp/stompjs',
        '@vicons/ionicons5',
        '@vicons/utils',
        '@vueuse/core',
        'axios',
        'bpmn-js/lib/Modeler',
        'bpmn-js/lib/NavigatedViewer',
        'crypto-js',
        'dagre',
        'dayjs',
        'diagram-js/lib/draw/BaseRenderer',
        'echarts',
        'element-plus',
        'highlight.js',
        'inherits-browser',
        'jsencrypt-ext',
        'lodash-es',
        'marked',
        'md5',
        'naive-ui',
        'pinia',
        'pinia-plugin-persistedstate',
        'sm-crypto',
        'sockjs-client',
        'tiny-svg',
        'vuedraggable',
        'vue3-intro-step',
        'vue3-slide-verify',
      ],
    },
    css: {
      postcss: {
        plugins: [],
      },
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/styles/variables.scss";`,
        },
      },
    },
    server: {
      port: VITE_HTTP_PORT,
      open: false,
      proxy: {
        // 流程服务代理 - 必须在主代理之前，匹配更具体的路径
        [`${VITE_REQUEST_PREFIX}/api/flow`]: {
          target: VITE_FLOW_PROXY_TARGET || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
          rewrite: path => path.replace(/^\/[^/]+/, ''),
        },
        // 工作台接口由流程服务提供，需在主代理之前匹配
        [`${VITE_REQUEST_PREFIX}/api/workspace`]: {
          target: VITE_FLOW_PROXY_TARGET || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
          rewrite: path => path.replace(/^\/[^/]+/, ''),
        },
        // 主代理 - 匹配所有其他请求
        [VITE_REQUEST_PREFIX]: {
          secure: false,
          target: VITE_HTTP_PROXY_TARGET,
          changeOrigin: true,
          rewrite: path => path.replace(new RegExp(`^${VITE_REQUEST_PREFIX}`), ''),
          configure: (proxy, options) => {
            // 配置此项可在响应头中看到请求的真实地址
            proxy.on('proxyRes', (proxyRes, req) => {
              proxyRes.headers['x-real-url'] = new URL(req.url || '', options.target)?.href || ''
            })
          },
        },
        // WebSocket 代理到后端同一服务
        '/ws': {
          target: VITE_HTTP_PROXY_TARGET,
          changeOrigin: true,
          ws: true,
          secure: false,
        },
      },
    },
    build: {
      // 关闭源码映射（大幅降低内存）
      sourcemap: false,
      // 分包优化
      chunkSizeWarningLimit: 2000, // chunk 大小警告的限制（单位kb）
    },
  }
})
