<template>
  <!-- 首页与详情列表复用同一查询模型，返回不会重置筛选 -->
  <form class="plugin-toolbar" :class="{ 'is-compact': compact }" @submit.prevent="store.search">
    <NInput
      v-model:value="store.query.keyword"
      clearable
      :maxlength="100"
      placeholder="搜索插件名称或标识"
      aria-label="搜索插件名称或标识"
    >
      <template #prefix>
        <i class="i-lucide:search" />
      </template>
    </NInput>
    <NSelect
      v-model:value="store.query.origin"
      clearable
      :options="origins"
      placeholder="全部来源"
      aria-label="插件来源"
      @update:value="store.search"
    >
      <template #arrow>
        <i class="i-lucide:sliders-horizontal" />
      </template>
    </NSelect>
    <NButton attr-type="submit" type="primary" :loading="store.loading">
      查询
    </NButton>
    <NButton quaternary aria-label="重置插件筛选" @click="store.reset">
      重置
    </NButton>
  </form>
</template>

<script setup>
import { NButton, NInput, NSelect } from 'naive-ui'
import { computed } from 'vue'
import { useDict } from '@/composables/useDict'
import { usePluginCenterStore } from '@/stores/plugin/centerStore'

defineProps({ compact: Boolean })
const store = usePluginCenterStore()
const { dict } = useDict('sys_plugin_origin')
const origins = computed(() => dict.value.sys_plugin_origin || [])
</script>

<style scoped>
.plugin-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.plugin-toolbar > :deep(.n-input) {
  flex: 1 1 240px;
  min-width: 0;
}

.plugin-toolbar > :deep(.n-select) {
  flex: 0 1 180px;
  min-width: 140px;
}

.plugin-toolbar.is-compact {
  padding: 12px;
}

.is-compact > :deep(.n-input) {
  flex: 1 1 100%;
}

.is-compact > :deep(.n-select) {
  flex: 1 1 140px;
  min-width: 100px;
}

@media (max-width: 540px) {
  .plugin-toolbar > :deep(.n-input),
  .plugin-toolbar > :deep(.n-select) {
    flex: 1 1 100%;
  }
}
</style>
