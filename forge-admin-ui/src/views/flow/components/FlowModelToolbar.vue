<script setup>
import { NButton, NDropdown, NInput, NSelect, NTreeSelect } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({
  modelName: { type: String, default: '' },
  category: { type: [String, Number], default: null },
  status: { type: Number, default: null },
  categoryOptions: { type: Array, default: () => [] },
  statusOptions: { type: Array, default: () => [] },
  loading: Boolean,
  sortMode: Boolean,
  sortSaving: Boolean,
  canSort: Boolean,
})
const emit = defineEmits(['update:modelName', 'update:category', 'update:status', 'search', 'reset', 'add', 'sort', 'saveSort', 'cancelSort'])
const moreOptions = computed(() => [{ label: '调整本页排序', key: 'sort', disabled: !props.canSort || props.loading }])
</script>

<template>
  <!-- 查询与页面操作分区；筛选统一点击查询/回车生效，排序期间锁定上下文。 -->
  <div class="model-toolbar" :class="{ 'is-sorting': sortMode }">
    <div class="model-toolbar__filters" role="search" aria-label="流程模型查询">
      <NInput
        :value="modelName" placeholder="搜索流程名称或 Key" clearable class="model-toolbar__search"
        :disabled="sortMode" :input-props="{ 'aria-label': '流程名称或 Key' }"
        @update:value="emit('update:modelName', $event)" @keyup.enter="emit('search')"
      >
        <template #prefix>
          <i class="i-lucide:search" />
        </template>
      </NInput>
      <NTreeSelect
        :value="category" :options="categoryOptions" placeholder="全部分类" clearable
        :disabled="sortMode" class="model-toolbar__category" aria-label="流程分类"
        @update:value="emit('update:category', $event)"
      />
      <NSelect
        :value="status" :options="statusOptions" placeholder="全部状态" clearable
        :disabled="sortMode" class="model-toolbar__status" aria-label="模型状态"
        @update:value="emit('update:status', $event)"
      />
      <div class="model-toolbar__query-actions">
        <NButton secondary :loading="loading" :disabled="sortMode" @click="emit('search')">
          查询
        </NButton>
        <NButton quaternary :disabled="sortMode || loading" @click="emit('reset')">
          重置
        </NButton>
      </div>
    </div>
    <div class="model-toolbar__actions">
      <template v-if="sortMode">
        <NButton :disabled="sortSaving" @click="emit('cancelSort')">
          取消排序
        </NButton>
        <NButton type="primary" :loading="sortSaving" @click="emit('saveSort')">
          保存排序
        </NButton>
      </template>
      <template v-else>
        <NButton type="primary" @click="emit('add')">
          <template #icon>
            <i class="i-lucide:plus" />
          </template>
          新增模型
        </NButton>
        <NDropdown :options="moreOptions" trigger="click" @select="emit('sort')">
          <NButton quaternary aria-label="列表更多操作" title="列表更多操作">
            <template #icon>
              <i class="i-lucide:more-horizontal" />
            </template>
          </NButton>
        </NDropdown>
      </template>
    </div>
  </div>
</template>

<style scoped>
.model-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
}
.model-toolbar__filters,
.model-toolbar__actions,
.model-toolbar__query-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.model-toolbar__filters {
  flex: 1;
  flex-wrap: wrap;
}
.model-toolbar__search {
  width: 230px;
}
.model-toolbar__category {
  width: 145px;
}
.model-toolbar__status {
  width: 125px;
}
.model-toolbar__actions,
.model-toolbar__query-actions {
  flex-shrink: 0;
}
@media (max-width: 760px) {
  .model-toolbar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
  .model-toolbar__filters {
    display: contents;
  }
  .model-toolbar__search {
    grid-column: 1 / -1;
  }
  .model-toolbar__search,
  .model-toolbar__category,
  .model-toolbar__status {
    width: 100%;
    min-width: 0;
  }
  .model-toolbar__actions {
    grid-column: 2;
    justify-content: flex-end;
  }
  .model-toolbar.is-sorting .model-toolbar__actions {
    grid-column: 1 / -1;
  }
}
@media (max-width: 380px) {
  .model-toolbar__actions {
    grid-column: 1 / -1;
  }
}
</style>
