import { createPinia } from 'pinia'
import { createApp } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { NTag } from 'naive-ui'
import App from './PersistenceVerificationApp.vue'
import Index from '@/views/print/index.vue'
import Designer from '@/views/print/designer.vue'
import Preview from '@/views/print/preview.vue'
const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/print', component: Index }, { path: '/print/designer', component: Designer }, { path: '/print/preview', component: Preview }] })
await router.push({ path: '/print', query: { applicationId: '2', sourceType: 'LOWCODE', pageId: '3', objectCode: 'purchase' } })
createApp(App).use(createPinia()).use(router).component('NTag', NTag).mount('#app')
