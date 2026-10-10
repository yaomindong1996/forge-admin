<template>
  <!-- 首页与详情列表复用同一查询模型，返回不会重置筛选 -->
  <form class="plugin-toolbar" :class="{ 'is-compact': compact }" @submit.prevent="store.search">
    <NInput
      v-model:value="store.query.keyword" clearable :maxlength="100"
      placeholder="搜索插件名称或标识" aria-label="搜索插件名称或标识"
    >
      <template #prefix>
        <i class="i-lucide:search" />
      </template>
    </NInput>
    <NSelect
      v-model:value="store.query.origin" clearable :options="origins"
      placeholder="全部来源" aria-label="插件来源" @update:value="store.search"
    />
    <NButton attr-type="submit" :loading="store.loading">
      查询
    </NButton>
    <NButton quaternary aria-label="重置插件筛选" title="重置筛选" @click="store.reset">
      <template #icon>
        <i class="i-lucide:rotate-ccw" />
      </template>
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
  gap: 8px;
  align-items: center;
}
.plugin-toolbar > :deep(.n-input) {
  width: min(340px, 100%);
}
.plugin-toolbar > :deep(.n-select) {
  width: 160px;
}
.plugin-toolbar.is-compact {
  padding: 12px;
}
.is-compact > :deep(.n-input) {
  width: 100%;
}
.is-compact > :deep(.n-select) {
  flex: 1;
  min-width: 100px;
}
@media (max-width: 540px) {
  .plugin-toolbar > :deep(.n-input) {
    width: 100%;
  }
  .plugin-toolbar > :deep(.n-select) {
    flex: 1;
    min-width: 100px;
  }
}
</style>
