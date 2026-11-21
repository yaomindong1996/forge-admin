<script setup>
import { NDropdown } from 'naive-ui'
import { h } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { modelCapabilityPresentation } from '@/composables/ai/modelResourceUtils'
import { useModelResourceStore } from '@/stores/ai/modelResourceStore'

const store = useModelResourceStore()

function providerName(row) {
  return store.providerNames[String(row.providerId)]
    || (String(store.selectedProvider?.id) === String(row.providerId) ? store.selectedProvider.providerName : '供应商信息加载中')
}

function icon(cls) {
  return () => h('i', { class: cls })
}

function actions(row) {
  return [
    { label: '设为默认模型', key: 'default', disabled: row.isDefault === '1', icon: icon('i-lucide:circle-check') },
    { label: '删除模型', key: 'delete', icon: icon('i-lucide:trash-2') },
  ]
}

function act(key, row) {
  if (key === 'default')
    return store.handleDefaultModelChange(row, true)
  window.$dialog.warning({
    title: '删除模型',
    content: `确定删除“${row.modelName || row.modelId}”吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => store.handleDeleteModel(row),
  })
}

function summary(row) {
  const bits = [providerName(row)]
  if (row.isDefault === '1')
    bits.push('默认模型')
  if (row.description)
    bits.push(row.description)
  return bits.join(' · ')
}
</script>

<template>
  <ul class="model-market-grid" aria-label="模型列表">
    <template v-for="row in store.modelList" :key="row.id">
      <li>
        <div
          class="model-market-card"
          role="button"
          tabindex="0"
          @click="store.handleEditModel(row)"
          @keydown.enter.prevent="store.handleEditModel(row)"
        >
          <span class="model-market-card-icon" aria-hidden="true">
            <i :class="modelCapabilityPresentation(row).icon" />
          </span>
          <span class="model-market-card-copy">
            <strong :title="row.modelName || row.modelId">{{ row.modelName || row.modelId }}</strong>
            <p :title="summary(row)">
              {{ summary(row) }}
            </p>
            <span class="model-market-card-meta">
              <DictTag dict-type="ai_model_type" :value="row.modelType" size="small" />
              <DictTag dict-type="ai_status" :value="row.status" size="small" />
            </span>
          </span>
          <span class="model-market-card-tools" @click.stop>
            <button
              type="button"
              class="model-market-link is-primary"
              :disabled="!!store.modelRowActionLoading[row.id]"
              @click="store.handleEditModel(row)"
            >
              编辑
            </button>
            <button
              type="button"
              class="model-market-link"
              :disabled="!!store.modelRowActionLoading[row.id]"
              @click="store.handleTestModel(row)"
            >
              测试
            </button>
            <NDropdown :options="actions(row)" @select="key => act(key, row)">
              <button
                type="button"
                class="model-market-link is-icon"
                title="更多操作"
                aria-label="更多操作"
                :disabled="!!store.modelRowActionLoading[row.id]"
              >
                <i class="i-lucide:ellipsis" aria-hidden="true" />
              </button>
            </NDropdown>
          </span>
        </div>
      </li>
      <li v-if="store.modelChecks[row.id]" class="model-market-check" :class="{ 'is-error': !store.modelChecks[row.id].ok }">
        <p role="status">
          <i :class="store.modelChecks[row.id].ok ? 'i-lucide:circle-check' : 'i-lucide:circle-alert'" aria-hidden="true" />
          <span>{{ store.modelChecks[row.id].message }}</span>
          <time>{{ store.modelChecks[row.id].checkedAt }}</time>
        </p>
      </li>
    </template>
  </ul>
</template>
