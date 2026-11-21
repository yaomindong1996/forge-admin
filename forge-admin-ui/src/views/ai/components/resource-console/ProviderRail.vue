<template>
  <div class="provider-rail">
    <div class="provider-rail__heading">
      <h2>供应商</h2>
    </div>
    <nav class="provider-rail__modes" aria-label="供应商浏览范围">
      <button
        type="button"
        :class="{ 'is-active': railMode === 'connections' }"
        :aria-pressed="railMode === 'connections'"
        @click="railMode = 'connections'"
      >
        我的连接
      </button>
      <button
        type="button"
        :class="{ 'is-active': railMode === 'catalog' }"
        :aria-pressed="railMode === 'catalog'"
        @click="railMode = 'catalog'"
      >
        常用预设 <span>{{ templateCount }}</span>
      </button>
    </nav>
    <div class="provider-rail__search">
      <NInput
        v-if="railMode === 'catalog'"
        v-model:value="catalogSearch"
        clearable
        placeholder="搜索供应商品牌"
        aria-label="搜索供应商品牌"
      >
        <template #prefix>
          <i class="i-lucide:search" aria-hidden="true" />
        </template>
      </NInput>
      <NInput
        v-else
        v-model:value="providerSearch.name"
        clearable
        placeholder="搜索已配置连接"
        aria-label="搜索已配置连接"
        @keyup.enter="store.searchConnections"
        @clear="store.searchConnections"
      >
        <template #prefix>
          <i class="i-lucide:search" aria-hidden="true" />
        </template>
      </NInput>
    </div>
    <div class="provider-rail__scroll">
      <template v-if="railMode === 'connections'">
        <button
          type="button"
          class="provider-option"
          :class="{ 'is-active': !selectedTemplate && !selectedProvider }"
          :aria-pressed="!selectedTemplate && !selectedProvider"
          @click="store.clearSelectedProvider"
        >
          <i class="provider-option__all i-lucide:layout-grid" aria-hidden="true" />
          <span class="provider-option__text">
            <strong>全部模型</strong>
          </span>
        </button>
        <div class="provider-rail__caption">
          <span>{{ selectedTemplate ? `${selectedTemplate.name}的连接` : '已配置连接' }} · {{ providerPagination.itemCount }}</span>
          <button
            v-if="selectedTemplate"
            type="button"
            class="provider-rail__caption-action"
            @click="store.clearSelectedProvider"
          >
            显示全部
          </button>
        </div>
        <NSpin :show="providerLoading">
          <div v-if="providerError" class="resource-inline-error">
            <p>{{ providerError }}</p>
            <NButton size="small" @click="store.loadProviders">
              重新加载
            </NButton>
          </div>
          <button
            v-for="provider in providerList"
            :key="provider.id"
            type="button"
            class="provider-option provider-option--connection"
            :class="{ 'is-active': String(selectedProvider?.id) === String(provider.id) }"
            :aria-pressed="String(selectedProvider?.id) === String(provider.id)"
            @click="store.handleSelectProvider(provider)"
          >
            <ProviderMark :provider="provider" />
            <span class="provider-option__text">
              <strong>{{ provider.providerName }}</strong>
              <small>{{ statusText(provider.status) }}</small>
            </span>
            <NTag
              v-if="provider.isDefault === '1'"
              size="tiny"
              type="success"
              :bordered="false"
              class="provider-option__default-badge"
            >
              默认
            </NTag>
          </button>
          <div v-if="!providerLoading && !providerError && !providerList.length" class="provider-rail__empty">
            <p>{{ providerSearch.name ? '未找到匹配连接' : '尚未配置连接' }}</p>
            <button
              type="button"
              class="provider-rail__caption-action is-primary"
              @click="providerSearch.name ? store.handleResetProviderSearch() : railMode = 'catalog'"
            >
              {{ providerSearch.name ? '清除搜索' : '从常用预设添加' }}
            </button>
          </div>
        </NSpin>
      </template>
      <div v-else class="provider-catalog" aria-label="常用供应商预设">
        <NSpin :show="templateLoading">
          <div v-if="templateError" class="resource-inline-error">
            <p>{{ templateError }}</p>
            <NButton size="small" @click="store.loadTemplates">
              重新加载预设
            </NButton>
          </div>
          <button
            v-for="template in featuredTemplates"
            :key="template.templateKey"
            type="button"
            class="provider-option provider-option--template"
            :class="{ 'is-active': selectedTemplate?.templateKey === template.templateKey }"
            :aria-pressed="selectedTemplate?.templateKey === template.templateKey"
            @click="store.handleSelectTemplate(template)"
          >
            <ProviderMark :provider="template" />
            <span class="provider-option__text">
              <strong>{{ template.name }}</strong>
            </span>
            <span
              class="provider-option__state"
              :class="{ 'is-configured': template.configuredCount > 0 }"
            >
              {{ template.configuredCount == null ? '查看' : template.configuredCount > 0 ? `已配置 ${template.configuredCount}` : '未配置' }}
            </span>
          </button>
          <p v-if="!templateLoading && !templateError && !featuredTemplates.length" class="provider-rail__empty">
            没有匹配预设，可添加自定义连接。
          </p>
        </NSpin>
      </div>
    </div>
    <div
      v-if="railMode === 'connections' && providerPagination.itemCount > providerPagination.pageSize"
      class="provider-rail__pagination"
    >
      <NPagination
        :page="providerPagination.pageNum"
        :page-size="providerPagination.pageSize"
        :item-count="providerPagination.itemCount"
        simple
        size="small"
        @update:page="store.handleProviderPageChange"
      />
    </div>
    <div class="provider-rail__footer">
      <button type="button" class="provider-rail__add" @click="store.handleAddProvider(selectedTemplate)">
        <i class="i-lucide:plus" aria-hidden="true" />
        添加连接
      </button>
    </div>
  </div>
</template>

<script setup>
import { NButton, NInput, NPagination, NSpin, NTag } from 'naive-ui'
import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { useModelResourceStore } from '@/stores/ai/modelResourceStore'
import ProviderMark from './ProviderMark.vue'

const store = useModelResourceStore()
const {
  railMode,
  catalogSearch,
  featuredTemplates,
  providerTemplates,
  templateLoading,
  templateError,
  selectedTemplate,
  providerSearch,
  selectedProvider,
  providerList,
  providerLoading,
  providerError,
  providerPagination,
} = storeToRefs(store)

const templateCount = computed(() => providerTemplates.value.filter(t => t.featured).length)

function statusText(status) {
  if (status === '0' || status === 0)
    return '正常'
  if (status === '1' || status === 1)
    return '停用'
  return '状态待核对'
}
</script>

<style scoped>
.provider-option__default-badge {
  flex: none;
  margin-left: auto;
  font-weight: 600;
}
</style>
