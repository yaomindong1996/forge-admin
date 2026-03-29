import { createApp } from 'vue'
import App from './App.vue'
import { setupDirectives } from './directives'

import { setupRouter } from './router'
import { setupStore } from './store'
import { setupNaiveDiscreteApi } from './utils'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import '@/styles/reset.css'
import '@/styles/design-tokens.css'
import '@/styles/animations.css'
import '@/styles/global.css'
import '@/styles/theme.css'
import '@/styles/responsive-vars.css'
import 'uno.css'

async function bootstrap() {
  const app = createApp(App)
  
  // 先初始化 Store，因为 setupNaiveDiscreteApi 需要用到
  setupStore(app)
  
  // 优先初始化 Naive UI 的全局 API，确保 $message 等可用
  setupNaiveDiscreteApi()
  
  setupDirectives(app)

  // 初始化主题配置：使用 store 中的配置，如果没有则使用默认配置
  const { useAppStore } = await import('@/store')
  const appStore = useAppStore()
  const themeConfig = appStore.themeConfig || defaultThemeConfig
  applyThemeConfig(themeConfig, appStore.isDark)

  await setupRouter(app)
  app.mount('#app')
}

bootstrap()
