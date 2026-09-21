<script setup>
import { computed, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { darkTheme, NButton, NConfigProvider } from 'naive-ui'
import ApplicationPrintPanel from '@/views/app-center/application-workspace/ApplicationPrintPanel.vue'
import ApplicationWorkspaceNav from '@/views/app-center/application-workspace/ApplicationWorkspaceNav.vue'
import { usePrintWorkspaceStore } from '@/stores/print/printWorkspaceStore'
import { user } from './m3b-mocks'
const router = useRouter(); const route = useRoute(); const dark = ref(false)
const store = usePrintWorkspaceStore()
const model = { objectId: '3', objectCode: 'purchase', configKey: 'purchase', objectName: '采购单' }
const application = { id: '2', applicationName: '合成采购应用', options: { inAppBuilder: { nodes: [{ id: 'page_purchase', type: 'page', title: '采购管理', objectRef: model }, { id: 'page_archive', type: 'page', title: '采购归档', objectRef: model }], pages: {} } } }
const themeVars = computed(() => ({ '--text-tertiary': dark.value ? '#a8abb2' : '#86909c', '--text-secondary': dark.value ? '#c2c2c2' : '#4e5969', '--bg-primary': dark.value ? '#18181c' : '#fff', '--bg-tertiary': dark.value ? '#26262a' : '#f2f3f5', '--bg-hover': dark.value ? '#303034' : '#f2f3f5', '--border-default': dark.value ? '#444448' : '#c9cdd4', color: dark.value ? '#efefef' : '#1f2329', background: dark.value ? '#18181c' : '#f5f7fa' }))
function preview(scene) { router.push({ path: '/print/preview', query: { ...store.sources[0]?.source, recordId: '合成/&?=9007199254740993', scene } }) }
function workspace() { router.push({ path: '/app-center/application/purchase-demo', query: { section: 'printing' } }) }
</script>
<template>
  <NConfigProvider :theme="dark ? darkTheme : null" :style="themeVars">
    <header><strong>采购应用 · 合成验证</strong><NButton size="small" @click="workspace">返回工作台</NButton><NButton size="small" @click="dark = !dark">切换明暗主题</NButton><NButton size="small" @click="preview('LIST')">合成列表打印</NButton><NButton size="small" @click="preview('DETAIL')">合成详情打印</NButton><NButton size="small" @click="user.getDataPermission = ['print:execute']">仅打印使用权限</NButton></header>
    <div v-if="route.name === 'workspace'" class="workspace">
      <aside><ApplicationWorkspaceNav :sections="[{ sectionKey: 'overview', sectionName: '概览' }, { sectionKey: 'printing', sectionName: '打印模板' }]" active-section="printing" /></aside>
      <main><ApplicationPrintPanel :application="application" :application-objects="[model]" /></main>
    </div>
    <RouterView v-else />
  </NConfigProvider>
</template>
<style>
body { margin: 0; font-family: Arial, sans-serif; }
header { display: flex; gap: 8px; align-items: center; padding: 12px; flex-wrap: wrap; }
.workspace { display: grid; grid-template-columns: 190px minmax(0, 1fr); margin: 8px; min-height: calc(100vh - 100px); }
main { min-width: 0; }
.n-config-provider { min-height: 100vh; }
@media (max-width: 600px) { .workspace { grid-template-columns: minmax(0, 1fr); } aside { display: none; } }
</style>
