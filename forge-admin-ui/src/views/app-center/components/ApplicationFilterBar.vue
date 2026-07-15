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

    <n-button secondary :loading="loading" @click="emit('refresh')">
      <template #icon>
        <n-icon><RefreshOutline /></n-icon>
      </template>
      刷新
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
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.application-search {
  width: min(420px, 42vw);
}

.application-filter {
  width: 150px;
}

@media (max-width: 760px) {
  .application-filter-bar {
    flex-wrap: wrap;
  }

  .application-search {
    width: 100%;
  }

  .application-filter {
    flex: 1 1 140px;
    width: auto;
  }
}
</style>
