<script setup>
import { computed, ref } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { darkTheme, NButton, NConfigProvider, NTag } from 'naive-ui'
import { control } from './m3b-api'
import { user } from './m3b-mocks'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'
import { usePrintRuntimeStore } from '@/stores/print/printRuntimeStore'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
const router = useRouter(); const dark = ref(false); const notice = ref('')
const templates = usePrintTemplateStore(); const runtime = usePrintRuntimeStore(); const canvas = usePrintDesignerStore()
const source = { applicationId: '2', sourceType: 'LOWCODE', pageId: '3', objectCode: 'purchase' }
const report = computed(() => ({ templateId: templates.row?.id, revision: templates.row?.draftRevision, nameDirty: templates.nameDirty, canvasDirty: canvas.dirty, executionId: runtime.prepared?.executionId, hasRuntimeData: !!runtime.record, error: templates.error || runtime.error }))
async function mode(value) { await control(value); notice.value = `已设置 ${value}` }
</script>
<template>
  <NConfigProvider :theme="dark ? darkTheme : null">
    <nav><NButton size="small" @click="router.push({ path: '/print', query: source })">合成模板管理</NButton><NButton size="small" @click="router.push({ path: '/print/preview', query: { ...source, recordId: 'synthetic_record', scene: 'DETAIL' } })">合成单据打印</NButton><NButton size="small" @click="dark = !dark">切换明暗主题</NButton><NButton size="small" @click="mode('conflict')">下次保存冲突</NButton><NButton size="small" @click="mode('delay')">下次保存延迟</NButton><NButton size="small" @click="mode('provider')">切换缺少适配器</NButton><NButton size="small" @click="user.getDataPermission = ['print:execute']">仅运行权限</NButton><span>{{ notice }}</span></nav>
    <RouterView />
    <details><summary>验证状态（仅合成 HTTP）</summary><pre id="persistence-report">{{ report }}</pre></details>
  </NConfigProvider>
</template>
<style>body { margin: 0; font-family: Arial, sans-serif; } nav { display: flex; gap: 6px; padding: 6px; flex-wrap: wrap; } .print-designer-page,.print-runtime-page { height: calc(100vh - 70px) !important; } #persistence-report { max-height: 100px; overflow: auto; font-size: 11px; } .n-config-provider { min-height: 100vh; }</style>
