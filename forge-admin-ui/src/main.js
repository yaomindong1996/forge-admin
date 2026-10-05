import { createApp } from 'vue'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import App from './App.vue'

import { setupDirectives } from './directives'
import { setupRouter } from './router'
import { setupStore, useAppStore } from './store'
import { setupNaiveDiscreteApi } from './utils'
import { loadRuntimeCryptoConfig } from './utils/crypto/crypto-config'
import { setupDebugConsole } from './utils/debug-console'
import { setupDynamicImportRecovery } from './utils/dynamic-import-recovery'
import { runWeComAutoLogin } from './utils/wecom'
import '@/styles/reset.css'
import '@/styles/design-tokens.css'
import '@/styles/animations.css'
import '@/styles/global.css'
import '@/styles/theme.css'
import '@/styles/layout-chrome.css'
import '@/styles/responsive-vars.css'
import 'uno.css'

// 用户在发布期间保持旧页面打开时，懒加载路由可能仍引用已被替换的旧 hash chunk。
// 尽早监听 Vite 的预加载异常，以便刷新到最新 index.html。
setupDynamicImportRecovery()

async function bootstrap() {
  // 优先加载页内调试面板（?vdebug=1 开启），确保后续 console 可见
  await setupDebugConsole()

  await loadRuntimeCryptoConfig()

  const app = createApp(App)

  // 先初始化 Store，因为 setupNaiveDiscreteApi 需要用到
  setupStore(app)

  // 优先初始化 Naive UI 的全局 API，确保 $message 等可用
  setupNaiveDiscreteApi()

  setupDirectives(app)

  // 初始化主题配置：使用 store 中的配置，如果没有则使用默认配置
  const appStore = useAppStore()
  const themeConfig = appStore.themeConfig || defaultThemeConfig
  applyThemeConfig(themeConfig, appStore.isDark)

  // 企微PC客户端工作台免登：写入 token 后由路由守卫补拉用户信息/菜单/密钥交换
  const wecomResult = await runWeComAutoLogin()
  if (wecomResult?.status === 'redirecting') {
    // 正在跳转企微授权页，停止后续挂载
    return
  }

  await setupRouter(app)
  app.mount('#app')
}

bootstrap()
