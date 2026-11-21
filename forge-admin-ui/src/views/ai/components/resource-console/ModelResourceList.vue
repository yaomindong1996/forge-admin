<template>
  <section class="resource-main model-market">
    <div class="provider-compact-nav">
      <NButton size="tiny" quaternary @click="compactPanel = 'providers'">
        <template #icon>
          <i class="i-lucide:panel-left" aria-hidden="true" />
        </template>
        供应商列表
      </NButton>
      <span>{{ selectedProvider ? selectedProvider.providerName : '全部模型' }}</span>
    </div>

    <header class="hub-market-header">
      <div class="model-market-heading">
        <h1>
          <ProviderMark v-if="selectedProvider" :provider="selectedProvider" />
          <span>{{ selectedProvider ? selectedProvider.providerName : '全部模型' }}</span>
          <NTag
            v-if="selectedProvider && selectedProvider.isDefault === '1'"
            type="success"
            size="small"
            :bordered="false"
          >
            默认供应商
          </NTag>
        </h1>
        <p>{{ headerSubtitle }}</p>
      </div>

      <div class="hub-market-actions">
        <template v-if="selectedProvider">
          <NButton
            v-if="selectedProvider.isDefault !== '1'"
            size="small"
            secondary
            type="primary"
            :loading="providerDefaultUpdatingId === selectedProvider.id"
            @click="store.handleSetDefault(selectedProvider)"
          >
            <template #icon>
              <i class="i-lucide:star" aria-hidden="true" />
            </template>
            设为默认
          </NButton>
          <button
            type="button"
            class="hub-market-icon-btn"
            title="编辑连接"
            aria-label="编辑连接"
            :disabled="providerEditing"
            @click="store.handleEditProvider(selectedProvider)"
          >
            <i class="i-lucide:settings-2" aria-hidden="true" />
          </button>
          <NDropdown :options="providerMenuOptions" @select="handleProviderAction">
            <button
              type="button"
              class="hub-market-icon-btn"
              title="连接操作"
              aria-label="连接操作"
            >
              <i class="i-lucide:ellipsis" aria-hidden="true" />
            </button>
          </NDropdown>
          <NButton size="small" @click="store.handleFetchModels()">
            <template #icon>
              <i class="i-lucide:download" aria-hidden="true" />
            </template>
            导入模型
          </NButton>
        </template>
        <button
          type="button"
          class="hub-market-add"
          :disabled="!providerList.length && !selectedProvider"
          @click="store.handleAddModel()"
        >
          <i class="i-lucide:plus" aria-hidden="true" />
          添加模型
        </button>
      </div>
    </header>

    <div class="hub-market-search" role="search" aria-label="筛选模型">
      <NInput
        v-model:value="modelSearch.modelName"
        clearable
        placeholder="搜索模型名称或标识"
        aria-label="搜索模型名称或标识"
        @keyup.enter="store.handleModelSearch"
        @clear="store.handleModelSearch"
      >
        <template #prefix>
          <i class="i-lucide:search" aria-hidden="true" />
        </template>
      </NInput>
    </div>

    <div class="hub-market-subtabs" role="tablist" aria-label="按模型类型筛选">
      <button
        type="button"
        class="hub-market-subtab"
        :class="{ selected: !modelSearch.modelType }"
        @click="selectModelType(null)"
      >
        全部
      </button>
      <button
        v-for="item in modelTypeOptions"
        :key="item.value"
        type="button"
        class="hub-market-subtab"
        :class="{ selected: modelSearch.modelType === item.value }"
        @click="selectModelType(item.value)"
      >
        {{ item.label }}
      </button>
      <span class="hub-market-sort">共 {{ modelPagination.itemCount }} 个模型</span>
    </div>

    <div class="resource-scroll">
      <NSpin :show="modelLoading" class="resource-list-spin">
        <ResourceEmpty v-if="modelError" title="模型加载失败" :description="modelError" icon="i-lucide:circle-alert">
          <NButton @click="store.loadModels()">
            重新加载
          </NButton>
        </ResourceEmpty>
        <ResourceEmpty v-else-if="!modelLoading && !modelList.length" :title="emptyTitle" :description="emptyDescription" icon="i-lucide:cpu">
          <NButton v-if="hasFilters" @click="store.resetModelSearch">
            重置筛选
          </NButton>
          <template v-else-if="selectedProvider">
            <NButton type="primary" @click="store.handleFetchModels()">
              导入模型
            </NButton>
            <NButton @click="store.handleAddModel()">
              手工添加
            </NButton>
          </template>
          <NButton v-else type="primary" @click="store.handleAddProvider()">
            添加供应商连接
          </NButton>
        </ResourceEmpty>
        <ModelResourceTable v-else />
      </NSpin>
    </div>

    <footer v-if="modelPagination.itemCount > modelPagination.pageSize" class="resource-pagination">
      <span>第 {{ modelPagination.pageNum }} 页 · 共 {{ modelPagination.itemCount }} 条</span>
      <NPagination
        :page="modelPagination.pageNum"
        :page-size="modelPagination.pageSize"
        :item-count="modelPagination.itemCount"
        show-size-picker
        :page-sizes="[10, 20, 50]"
        @update:page="store.handleModelPageChange"
        @update:page-size="store.handleModelPageSizeChange"
      />
    </footer>

    <span class="model-market-icon-keepalive" aria-hidden="true">
      <i class="i-lucide:message-square-text" />
      <i class="i-lucide:database-zap" />
      <i class="i-lucide:list-ordered" />
      <i class="i-lucide:image" />
      <i class="i-lucide:audio-lines" />
      <i class="i-lucide:cpu" />
    </span>
  </section>
</template>

<script setup>
import { NButton, NDropdown, NInput, NPagination, NSpin, NTag } from 'naive-ui'
import { storeToRefs } from 'pinia'
import { computed, h } from 'vue'
import { useModelResourceStore } from '@/stores/ai/modelResourceStore'
import ModelResourceTable from './ModelResourceTable.vue'
import ProviderMark from './ProviderMark.vue'
import ResourceEmpty from './ResourceEmpty.vue'

const store = useModelResourceStore()
const {
  compactPanel,
  selectedProvider,
  providerList,
  providerEditing,
  providerDefaultUpdatingId,
  modelSearch,
  modelList,
  modelLoading,
  modelError,
  modelPagination,
  modelTypeOptions,
} = storeToRefs(store)

const hasFilters = computed(() => !!(modelSearch.value.modelName || modelSearch.value.modelType))

const headerSubtitle = computed(() => {
  if (selectedProvider.value) {
    return selectedProvider.value.baseUrl || '未配置服务地址，可点击右上角齿轮完善连接信息。'
  }
  return '管理租户下的模型供应商连接与可用模型；设为默认后，全局助手与智能体将优先使用该供应商。'
})

const emptyTitle = computed(() => {
  if (hasFilters.value)
    return '未找到匹配的模型'
  if (selectedProvider.value)
    return `${selectedProvider.value.providerName} 还没有模型`
  return '还没有配置模型'
})

const emptyDescription = computed(() => {
  if (hasFilters.value)
    return '调整或清除筛选条件。'
  if (selectedProvider.value)
    return '可以从供应商导入，也可以手动填写模型标识。'
  return '先在左侧添加或选择一个供应商连接。'
})

function selectModelType(type) {
  modelSearch.value.modelType = type
  store.handleModelSearch()
}

function menuIcon(name) {
  return () => h('i', { class: name })
}

const providerMenuOptions = computed(() => {
  const provider = selectedProvider.value
  if (!provider)
    return []
  return [
    { label: '编辑连接', key: 'edit', disabled: providerEditing.value, icon: menuIcon('i-lucide:settings-2') },
    { label: provider.isDefault === '1' ? '已是默认供应商' : '设为默认供应商', key: 'default', disabled: provider.isDefault === '1' || providerDefaultUpdatingId.value === provider.id, icon: menuIcon('i-lucide:star') },
    { type: 'divider', key: 'divider' },
    { label: '删除连接', key: 'delete', icon: menuIcon('i-lucide:trash-2') },
  ]
})

function handleProviderAction(key) {
  const provider = selectedProvider.value
  if (!provider)
    return
  if (key === 'edit')
    store.handleEditProvider(provider)
  if (key === 'default')
    store.handleSetDefault(provider)
  if (key === 'delete') {
    window.$dialog.warning({
      title: '删除供应商连接',
      content: `确定删除“${provider.providerName}”吗？其下关联的模型配置也将不可用。`,
      positiveText: '删除',
      negativeText: '取消',
      onPositiveClick: () => store.handleDeleteProvider(provider.id),
    })
  }
}
</script>

<style scoped>
.model-market-heading {
  min-width: 0;
  flex: 1;
}
</style>
