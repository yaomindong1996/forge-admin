import { createApp, h } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { applyThemeConfig, defaultThemeConfig } from '@/config/theme.config'
import { setupStore, useAppStore, useUserStore } from '@/store'
import { request } from '@/utils/request'
import { cryptoConfig } from '@/utils/crypto/crypto-config'
import { setupNaiveDiscreteApi } from '@/utils/naiveTools'
import '@/styles/reset.css'
import '@/styles/design-tokens.css'
import '@/styles/global.css'
import '@/styles/theme.css'
import 'uno.css'
import Preview from './preview.vue'

cryptoConfig.enabled = false
cryptoConfig.enableReplay = false
request.defaults.baseURL = '/verification-api'
const app = createApp({ render: () => h(Preview) })
setupStore(app)
useUserStore().userInfo = { userId: '1', tenantId: '1', isAdmin: true }
useAppStore().isDark = false
applyThemeConfig(defaultThemeConfig, false)
setupNaiveDiscreteApi()
const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/system/excel-export-config', component: Preview }] })
app.use(router)
await router.push('/system/excel-export-config')
await router.isReady()
app.mount('#app')
