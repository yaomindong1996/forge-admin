<template>
  <NModal
    :show="fetchModelsModal.show"
    preset="card"
    :title="fetchModelsModal.providerName ? `导入模型 · ${fetchModelsModal.providerName}` : '导入模型'"
    class="resource-import-modal"
    :style="fetchModelsModalStyle"
    :content-style="fetchModelsModalContentStyle"
    :mask-closable="false"
    @update:show="v => !v && store.cancelModelImport()"
  >
    <div v-if="fetchModelsModal.loading" class="resource-import-state">
      <i class="resource-spin i-lucide:loader-2" aria-hidden="true" />
      <p>正在从供应商接口读取可用模型列表…</p>
    </div>

    <div v-else-if="fetchModelsModal.error" class="resource-import-state is-error">
      <i class="i-lucide:circle-alert" aria-hidden="true" />
      <p>{{ fetchModelsModal.error }}</p>
      <NSpace>
        <NButton @click="store.retryFetchModels">
          重新拉取
        </NButton>
        <NButton type="primary" @click="switchToManualAdd">
          改用手工添加
        </NButton>
      </NSpace>
    </div>

    <template v-else>
      <div class="resource-import-toolbar">
        <NInput
          v-model:value="fetchModelsModalSearch"
          clearable
          placeholder="按模型标识筛选"
          aria-label="筛选可导入模型"
        >
          <template #prefix>
            <i class="i-lucide:search" aria-hidden="true" />
          </template>
        </NInput>
        <NSelect
          v-model:value="fetchModelsModalCategory"
          style="width: 150px"
          :options="fetchModelsModalCategories"
        />
      </div>

      <div class="resource-import-list">
        <section v-for="group in fetchModelsModalGroups" :key="group.type" class="resource-import-group">
          <header class="resource-import-group__head">
            <NCheckbox
              :checked="store.isGroupChecked(group.type)"
              :indeterminate="store.isGroupIndeterminate(group.type)"
              @update:checked="val => store.handleToggleGroup(group.type, val)"
            >
              <strong>{{ group.label }}</strong>
              <span class="resource-import-group__count">（{{ group.models.length }}）</span>
            </NCheckbox>
          </header>
          <div
            v-for="item in group.models"
            :key="item.id"
            class="resource-import-row"
            @click="store.handleToggleModel(item.id, !fetchModelsModal.checked[item.id])"
          >
            <NCheckbox
              :checked="!!fetchModelsModal.checked[item.id]"
              @update:checked="val => store.handleToggleModel(item.id, val)"
              @click.stop
            />
            <div class="resource-import-copy">
              <code>{{ item.id }}</code>
            </div>
            <NSelect
              v-model:value="fetchModelsModal.modelTypes[item.id]"
              size="tiny"
              style="width: 120px"
              :options="modelTypeOverrideOptions"
              @click.stop
            />
          </div>
        </section>
        <p v-if="!fetchModelsModalGroups.length" class="resource-import-empty">
          没有匹配的可导入模型，可关闭后手工添加。
        </p>
      </div>
    </template>

    <template #action>
      <NSpace justify="space-between" align="center" style="width: 100%">
        <NButton text type="primary" @click="switchToManualAdd">
          找不到目标模型？手工添加
        </NButton>
        <NSpace>
          <NButton @click="store.cancelModelImport">
            关闭
          </NButton>
          <NButton
            type="primary"
            :loading="fetchModelsModal.importing"
            :disabled="!fetchModelsModalVisibleCheckedCount"
            @click="store.handleImportModels"
          >
            导入所选（{{ fetchModelsModalVisibleCheckedCount }}）
          </NButton>
        </NSpace>
      </NSpace>
    </template>
  </NModal>
</template>

<script setup>
import { NButton, NCheckbox, NInput, NModal, NSelect, NSpace } from 'naive-ui'
import { storeToRefs } from 'pinia'
import { useModelResourceStore } from '@/stores/ai/modelResourceStore'

const store = useModelResourceStore()
const {
  fetchModelsModal,
  fetchModelsModalStyle,
  fetchModelsModalContentStyle,
  fetchModelsModalSearch,
  fetchModelsModalCategory,
  fetchModelsModalCategories,
  fetchModelsModalGroups,
  fetchModelsModalVisibleCheckedCount,
  modelTypeOverrideOptions,
} = storeToRefs(store)

function switchToManualAdd() {
  store.cancelModelImport()
  store.handleAddModel()
}
</script>

<style scoped>
.resource-import-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 16px;
  color: var(--text-secondary, #4b5563);
  text-align: center;
}

.resource-import-state > i {
  font-size: 28px;
}

.resource-import-state.is-error > i {
  color: #dc2626;
}

.resource-spin {
  animation: resource-spin 1s linear infinite;
}

@keyframes resource-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.resource-import-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.resource-import-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
}

.resource-import-group__head {
  padding: 8px 12px;
  background: var(--bg-secondary, #f9fafb);
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.resource-import-group__count {
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
}

.resource-import-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light, #f3f4f6);
  cursor: pointer;
}

.resource-import-row:hover {
  background: var(--bg-secondary, #f9fafb);
}

.resource-import-copy {
  flex: 1;
  min-width: 0;
}

.resource-import-copy code {
  font-size: 12px;
  color: var(--text-primary, #111827);
}

.resource-import-empty {
  padding: 24px 12px;
  text-align: center;
  color: var(--text-tertiary, #6b7280);
  font-size: 13px;
}
</style>
