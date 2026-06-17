import path from 'node:path'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

/**
 * Vitest 独立配置。
 *
 * 不复用 vite.config.js 是为了避免在测试场景下触发 unplugin-vue-router、unocss、
 * unplugin-auto-import 等插件，单测启动更快、依赖更少。仅保留 @vitejs/plugin-vue
 * 让组件单测可以解析 .vue 文件。
 *
 * jsdom 环境提供 DOMParser、document、EventTarget、KeyboardEvent 等浏览器 API。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(process.cwd(), 'src'),
      '~': path.resolve(process.cwd()),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/__tests__/**/*.{spec,test}.{js,ts}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/components/flow-designer/**/*.{js,ts,vue}'],
      exclude: [
        'src/components/flow-designer/**/__tests__/**',
        'src/components/flow-designer/**/index.js',
      ],
    },
  },
})
