<template>
  <section class="plugin-build">
    <!-- 当前 UI 与后端声明边界 -->
    <header>
      <div>UI 核心 {{ manifest.coreVersion }} · 构建时间 {{ pluginTime(manifest.builtAt) }}</div>
      <NButton :loading="loading" @click="load">
        刷新后端对比
      </NButton>
    </header>
    <NAlert type="info" :bordered="false">
      这里只核验独立 UI 插件的构建声明，不能证明页面健康或源码安装状态。
      源码工作区尚未连接，请在后续执行器中核验登记、文件冲突和定制。
    </NAlert>
    <NAlert v-if="error" type="error" title="后端对比加载失败">
      {{ error }}
    </NAlert>
    <p v-if="snapshot && snapshot.coreVersion !== manifest.coreVersion" class="text-warning">
      UI 与后端核心版本不一致：后端 {{ snapshot.coreVersion }}，请核对部署。
    </p>
    <!-- UI 构建时验证过的实际独立组件 -->
    <NDataTable :columns="columns" :data="manifest.plugins" :loading="loading" :scroll-x="680">
      <template #empty>
        <NEmpty description="当前 UI 构建未包含独立插件组件" />
      </template>
    </NDataTable>
  </section>
</template>

<script setup>
import { NAlert, NButton, NDataTable, NEmpty } from 'naive-ui'
import manifest from 'virtual:forge-ui-plugin-manifest'
import { computed, onMounted } from 'vue'
import { getRuntimePluginSnapshot } from '@/api/system/plugin'
import { pluginTime, taskResponse } from '../pluginTaskUtils'
import { useLatestPluginRequest } from '../useLatestPluginRequest'

const request = useLatestPluginRequest(async () => {
  const data = taskResponse(await getRuntimePluginSnapshot())
  if (!Array.isArray(data.records))
    throw new Error('后端构建快照响应无效')
  return data
}, true)
const { data: snapshot, loading, error } = request
const load = () => request.run()
const columns = computed(() => [
  { title: 'UI 插件', key: 'name', minWidth: 160 },
  { title: '标识', key: 'id', width: 160 },
  { title: 'UI 构建版本', key: 'version', width: 140 },
  { title: '后端声明版本', key: 'backend', width: 140, render: row =>
    snapshot.value?.records.find(plugin => plugin.id === row.id)?.version || '—' },
  { title: '对比说明', key: 'comparison', width: 200, render: comparison },
])
function comparison(row) {
  if (!snapshot.value)
    return '后端尚未核验'
  const backend = snapshot.value.records.find(plugin => plugin.id === row.id)
  if (!backend)
    return '后端无此声明（可能为纯 UI）'
  return backend.version === row.version ? '版本一致，健康状态未核验' : '构建版本不同，请核对部署'
}
onMounted(load)
</script>

<style scoped>
.plugin-build {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.plugin-build header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
@media (max-width: 640px) {
  .plugin-build header {
    flex-wrap: wrap;
  }
}
</style>
