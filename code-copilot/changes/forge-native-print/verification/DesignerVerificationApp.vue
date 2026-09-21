<script setup>
import { computed, ref } from 'vue'
import { darkTheme, NButton, NConfigProvider } from 'naive-ui'
import { usePrintDesignerStore } from '../../../../forge-admin-ui/src/stores/print/printDesignerStore'
import PrintDesigner from '../../../../forge-admin-ui/src/components/print/designer/PrintDesigner.vue'
import { fixture, resolveSyntheticImage } from './fixtures'
const dark = ref(false)
const invalidCatalog = ref(false)
const sample = fixture(100)
const store = usePrintDesignerStore()
const catalog = computed(() => invalidCatalog.value ? sample.catalog.filter(f => f.path !== 'main.total') : sample.catalog)
const report = computed(() => ({
  zoom: store.zoom, dirty: store.dirty, undo: store.history.past.length, redo: store.history.future.length,
  selected: store.selectedIds, surface: store.surfaceId, issues: store.fieldIssues,
  sections: store.document.body.map(s => ({ id: s.id, kind: s.kind, elements: s.elements })),
  paper: store.document.paper,
}))
</script>
<template>
  <NConfigProvider :theme="dark ? darkTheme : null">
    <div :style="{ background: dark ? '#18181c' : '#fff', color: dark ? '#eee' : '#222', height: '100vh', display: 'flex', flexDirection: 'column' }">
      <div style="display: flex; gap: 8px; padding: 4px"><NButton size="small" @click="dark = !dark">切换明暗主题</NButton><NButton size="small" @click="invalidCatalog = !invalidCatalog">切换失效字段</NButton><span>仅合成数据 · 100 行明细</span></div>
      <div style="flex: 1; min-height: 0"><PrintDesigner :template="sample.template" :context="sample.context" :catalog="catalog" :resolve-file="resolveSyntheticImage" /></div>
      <details><summary>验证状态</summary><pre id="designer-report">{{ JSON.stringify(report, null, 2) }}</pre></details>
    </div>
  </NConfigProvider>
</template>
<style>body { margin: 0; font-family: Arial, sans-serif; } #designer-report { max-height: 180px; overflow: auto; font-size: 10px; }</style>
