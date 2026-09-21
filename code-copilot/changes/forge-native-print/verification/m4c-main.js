import { createPinia } from 'pinia'
import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { NIcon, NTag } from 'naive-ui'
import App from './WorkspaceVerificationApp.vue'
import Designer from '@/views/print/designer.vue'
import Preview from '@/views/print/preview.vue'
const workspacePath = '/app-center/application/purchase-demo'
const router = createRouter({ history: createWebHistory(), routes: [{ name: 'workspace', path: '/app-center/application/:applicationCode', component: { render: () => null } }, { path: '/workspace', redirect: { path: workspacePath, query: { section: 'printing' } } }, { path: '/print/designer', component: Designer }, { path: '/print/preview', component: Preview }] })
await router.push(location.pathname === '/workspace.html' ? { path: workspacePath, query: { section: 'printing' } } : `${location.pathname}${location.search}`)
createApp(App).use(createPinia()).use(router).component('NTag', NTag).component('NIcon', NIcon).mount('#app')
