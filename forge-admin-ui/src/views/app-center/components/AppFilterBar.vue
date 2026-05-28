<template>
  <div class="app-filter-bar">
    <n-input
      :value="keyword"
      clearable
      class="filter-search"
      placeholder="搜索业务对象或应用入口"
      @update:value="value => emit('update:keyword', value)"
      @keyup.enter="emit('search')"
    >
      <template #prefix>
        <n-icon><SearchOutline /></n-icon>
      </template>
    </n-input>
    <n-select
      v-if="showSuite"
      :value="suiteCode"
      clearable
      filterable
      class="filter-select"
      placeholder="业务套件"
      :options="suiteOptions"
      @update:value="value => emit('update:suiteCode', value)"
    />
    <DictSelect
      :value="appType"
      class="filter-select"
      dict-type="ai_business_app_type"
      placeholder="应用类型"
      @update:value="value => emit('update:appType', value)"
    />
    <n-button secondary @click="emit('refresh')">
      <template #icon>
        <n-icon><RefreshOutline /></n-icon>
      </template>
      刷新
    </n-button>
    <n-button secondary @click="emit('createObject')">
      <template #icon>
        <n-icon><CubeOutline /></n-icon>
      </template>
      业务对象
    </n-button>
    <n-button type="primary" @click="emit('createApp')">
      <template #icon>
        <n-icon><AddOutline /></n-icon>
      </template>
      应用入口
    </n-button>
  </div>
</template>

<script setup>
import { AddOutline, CubeOutline, RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import { computed } from 'vue'
import DictSelect from '@/components/DictSelect.vue'

const props = defineProps({
  keyword: {
    type: String,
    default: '',
  },
  suiteCode: {
    type: String,
    default: null,
  },
  appType: {
    type: String,
    default: null,
  },
  suites: {
    type: Array,
    default: () => [],
  },
  showSuite: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits([
  'update:keyword',
  'update:suiteCode',
  'update:appType',
  'search',
  'refresh',
  'createObject',
  'createApp',
])

const suiteOptions = computed(() => props.suites.map(item => ({
  label: item.suiteName || item.suiteCode,
  value: item.suiteCode,
})))
</script>

<style scoped>
.app-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.filter-search {
  width: min(360px, 100%);
}

.filter-select {
  width: 180px;
}

@media (max-width: 680px) {
  .app-filter-bar {
    align-items: stretch;
  }

  .filter-search,
  .filter-select,
  .app-filter-bar :deep(.n-button) {
    width: 100%;
  }
}
</style>
