<script setup>
import { NButton, NEmpty, NInput, NScrollbar, NSelect, NSpin } from 'naive-ui'
import { computed } from 'vue'
import { usePrintCenterStore } from '@/stores/print/printCenterStore'

defineProps({ canManage: Boolean })
defineEmits(['create'])

const store = usePrintCenterStore()
const typeOptions = [
  { label: '全部', value: null },
  { label: '代码业务', value: 'SERVICE' },
  { label: '数据集', value: 'DATASET' },
]
const countText = computed(() => `${store.total} 个来源`)

function iconOf(type) {
  return type === 'DATASET' ? 'i-lucide:database' : 'i-lucide:braces'
}
</script>

<template>
  <div class="source-list-panel">
    <!-- 来源工具栏 -->
    <header class="source-list-panel__head">
      <div>
        <strong>可打印业务</strong>
        <span>{{ countText }}</span>
      </div>
      <NButton v-if="canManage" size="small" type="primary" @click="$emit('create')">
        <template #icon>
          <i class="i-lucide:plus" />
        </template>
        新增
      </NButton>
    </header>

    <!-- 来源筛选 -->
    <div class="source-list-panel__filters">
      <NInput
        v-model:value="store.keyword"
        size="small"
        clearable
        placeholder="搜索业务名称"
        @keyup.enter="store.load()"
      />
      <NSelect
        v-model:value="store.type"
        size="small"
        :options="typeOptions"
        @update:value="store.load()"
      />
    </div>

    <!-- 来源列表 -->
    <NSpin :show="store.loading" class="source-list-panel__spin">
      <NScrollbar class="source-list-panel__scroll">
        <div class="source-list" role="list" aria-label="打印业务来源">
          <button
            v-for="item in store.sources"
            :key="item.id"
            type="button"
            class="source-list__item"
            :class="{ active: String(item.id) === String(store.selectedId) }"
            @click="store.select(item.id)"
          >
            <span class="source-list__icon" aria-hidden="true">
              <i :class="iconOf(item.sourceType)" />
            </span>
            <span class="source-list__content">
              <strong :title="item.sourceName">{{ item.sourceName }}</strong>
              <span :title="item.sourceCode">{{ item.sourceCode }}</span>
            </span>
            <span class="source-list__status" :class="{ enabled: Number(item.status) === 1 }">
              {{ Number(item.status) === 1 ? '启用' : '停用' }}
            </span>
          </button>
          <NEmpty
            v-if="!store.loading && !store.sources.length"
            size="small"
            description="暂无可打印业务"
          />
        </div>
      </NScrollbar>
    </NSpin>
  </div>
</template>

<style scoped>
.source-list-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.source-list-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.source-list-panel__head > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.source-list-panel__head strong {
  color: var(--text-primary, #1d2129);
  font-size: 14px;
  font-weight: 600;
}

.source-list-panel__head span,
.source-list__content span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.source-list-panel__filters {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.source-list-panel__spin,
.source-list-panel__scroll {
  flex: 1;
  min-height: 0;
}

.source-list-panel__spin :deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
}

.source-list {
  display: flex;
  flex-direction: column;
  padding: 6px;
}

.source-list__item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  width: 100%;
  min-height: 54px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition:
    background 150ms ease,
    border-color 150ms ease;
}

.source-list__item:hover {
  background: var(--gray-100, #f6f8fb);
}

.source-list__item.active {
  border-color: color-mix(in srgb, var(--primary-color, #1677ff) 22%, transparent);
  background: color-mix(in srgb, var(--primary-color, #1677ff) 7%, transparent);
}

.source-list__icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 3px;
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  font-size: 15px;
}

.source-list__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.source-list__content strong,
.source-list__content span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-list__content strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  font-weight: 500;
}

.source-list__status {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.source-list__status.enabled {
  color: var(--primary-color, #1677ff);
}
</style>
