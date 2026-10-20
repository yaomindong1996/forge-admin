<script setup>
import { computed, defineComponent, ref } from 'vue'
import { darkTheme, dateZhCN, useDialog, useMessage, zhCN } from 'naive-ui'
import FlowModel from '@/views/flow/model.vue'
import { fixture } from './runtime'

const dark = ref(false)
const theme = computed(() => dark.value ? darkTheme : null)
const themeOverrides = computed(() => ({ common: {
  primaryColor: dark.value ? '#79a6f9' : '#1677ff',
  primaryColorHover: dark.value ? '#a1bef9' : '#4093ff',
  primaryColorPressed: dark.value ? '#6492e9' : '#0e64dc',
  primaryColorSuppl: dark.value ? '#79a6f9' : '#1677ff',
} }))
const Bridge = defineComponent({ setup() {
  window.$dialog = useDialog()
  window.$message = useMessage()
  return () => null
} })
function changeScenario(value) {
  fixture.scenario = value
  fixture.requests = []
  fixture.revision += 1
}
</script>
<template>
  <n-config-provider :theme="theme" :theme-overrides="themeOverrides" :locale="zhCN" :date-locale="dateZhCN">
    <n-message-provider>
      <n-dialog-provider>
        <Bridge />
        <div class="catalog-fixture" :class="{ dark }">
          <header><strong>流程模型</strong><span>本地真实组件 · 合成数据，不连接业务服务</span></header>
          <main><FlowModel :key="fixture.revision" /></main>
          <footer>
            <span>{{ fixture.notice || `模型请求 ${fixture.requests.filter(path => path === '/api/flow/model/page').length} · 批量绑定 ${fixture.requests.filter(path => path.includes('business-bindings')).length}` }}</span>
            <div>
              <button @click="changeScenario('normal')">正常列表</button>
              <button @click="changeScenario('error')">列表失败</button>
              <button @click="changeScenario('bindings-error')">绑定失败</button>
              <button @click="changeScenario('empty')">空列表</button>
              <button @click="dark = !dark">切换{{ dark ? '浅色' : '深色' }}</button>
            </div>
          </footer>
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>
<style>
body { margin: 0; font-family: system-ui, -apple-system, 'PingFang SC', sans-serif; --primary-color: #1677ff; --error-color: #d03050; --warning-color: #c67b16; --success-color: #189c69; --bg-primary: #fff; --bg-secondary: #f5f7fa; --bg-tertiary: #f2f3f5; --border-light: #e5e6eb; --text-primary: #1d2129; --text-secondary: #4e5969; --text-tertiary: #86909c; --text-disabled: #b7bdc7; }
body:has(.catalog-fixture.dark) { --primary-color: #79a6f9; --error-color: #e88080; --warning-color: #e3b15f; --success-color: #63cba3; --bg-primary: #202126; --bg-secondary: #17181c; --bg-tertiary: #282b32; --border-light: #34383f; --text-primary: #e7e9ed; --text-secondary: #bcc2cd; --text-tertiary: #929aab; }
.catalog-fixture { display: flex; flex-direction: column; height: 100dvh; min-height: 0; color: var(--text-primary); background: var(--bg-secondary); }
.catalog-fixture header { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding: 14px 18px; background: var(--bg-primary); border-bottom: 1px solid var(--border-light); }
.catalog-fixture header span, .catalog-fixture footer { font-size: 12px; color: var(--text-tertiary); }
.catalog-fixture main { flex: 1; min-height: 0; min-width: 0; padding: 12px; }
.catalog-fixture footer { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px; padding: 8px 12px; border-top: 1px solid var(--border-light); }
.catalog-fixture footer div { display: flex; flex-wrap: wrap; gap: 8px; }
.catalog-fixture footer button { font: inherit; color: var(--primary-color); background: transparent; border: 0; cursor: pointer; padding: 4px; }
</style>
