import { createApp, h } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setupStore, useUserStore, useAppStore } from '@/store'
import { request } from '@/utils/request'
import { setupNaiveDiscreteApi } from '@/utils/naiveTools'
import { cryptoConfig } from '@/utils/crypto/crypto-config'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import '@/styles/reset.css'
import '@/styles/design-tokens.css'
import '@/styles/global.css'
import '@/styles/theme.css'
import 'uno.css'
import Preview from './preview.vue'

// 只在独立验证入口使用：所有业务请求交由 Playwright 的模拟 API 处理。
cryptoConfig.enabled = false
cryptoConfig.enableReplay = false
request.defaults.baseURL = '/verification-api'
const app = createApp({ render: () => h(Preview) })
setupStore(app)
useUserStore().userInfo = { userId: '1', tenantId: '1', isAdmin: true }
useAppStore().isDark = false
applyThemeConfig(defaultThemeConfig, false)
setupNaiveDiscreteApi()
const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/system/dataScopeConfig', component: Preview }] })
app.use(router)
await router.push('/system/dataScopeConfig')
await router.isReady()
app.mount('#app')
