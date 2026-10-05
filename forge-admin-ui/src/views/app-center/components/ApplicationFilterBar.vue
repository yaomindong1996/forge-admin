<template>
  <div class="application-filter-bar">
    <n-input
      :value="keyword"
      clearable
      class="application-search"
      placeholder="搜索应用名称、编码或说明"
      @update:value="value => emit('update:keyword', value)"
      @keyup.enter="emit('search')"
    >
      <template #prefix>
        <n-icon><SearchOutline /></n-icon>
      </template>
    </n-input>

    <DictSelect
      :value="designStatus"
      class="application-filter"
      dict-type="ai_business_application_design_status"
      placeholder="设计状态"
      clearable
      @update:value="value => emit('update:designStatus', value)"
    />

    <DictSelect
      :value="status"
      class="application-filter"
      dict-type="sys_enable_disable"
      placeholder="启用状态"
      clearable
      @update:value="value => emit('update:status', value)"
    />

    <n-button
      secondary
      circle
      size="small"
      class="application-refresh"
      :loading="loading"
      aria-label="刷新"
      title="刷新"
      @click="emit('refresh')"
    >
      <template #icon>
        <n-icon><RefreshOutline /></n-icon>
      </template>
    </n-button>
  </div>
</template>

<script setup>
import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import DictSelect from '@/components/DictSelect.vue'

defineProps({
  keyword: {
    type: String,
    default: '',
  },
  designStatus: {
    type: String,
    default: null,
  },
  status: {
    type: [Number, String],
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:keyword',
  'update:designStatus',
  'update:status',
  'search',
  'refresh',
])
</script>

<style scoped>
.application-filter-bar {
  display: flex;
  min-width: 0;
  flex: 1 1 auto;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
}

.application-search {
  min-width: 140px;
  flex: 1 1 180px;
  width: auto;
  max-width: 360px;
}

.application-filter {
  flex: 0 0 118px;
  width: 118px;
}

.application-refresh {
  flex: 0 0 auto;
}

@media (max-width: 720px) {
  .application-filter-bar {
    flex-wrap: wrap;
  }

  .application-search {
    flex: 1 1 100%;
    max-width: none;
    min-width: 0;
  }

  .application-filter {
    flex: 1 1 120px;
    width: auto;
  }
}
</style>
