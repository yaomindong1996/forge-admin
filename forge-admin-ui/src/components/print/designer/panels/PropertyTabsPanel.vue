<script setup>
import { NTabPane, NTabs } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import BindingPanel from './BindingPanel.vue'
import DescriptionsPanel from './DescriptionsPanel.vue'
import ElementGeometryPanel from './ElementGeometryPanel.vue'
import ElementOptionsPanel from './ElementOptionsPanel.vue'
import StaticTablePanel from './StaticTablePanel.vue'
import TablePanel from './TablePanel.vue'
import TextPanel from './TextPanel.vue'

const store = usePrintDesignerStore()
const tab = ref('basic')

const hasBinding = computed(() => !!(store.activeElement?.binding || store.activeSurface?.kind === 'TEXT'))
const isTable = computed(() => store.activeSurface?.kind === 'TABLE' || store.activeElement?.type === 'DATA_TABLE')
const isStaticTable = computed(() => store.activeElement?.type === 'STATIC_TABLE')
const isDescriptions = computed(() => store.activeElement?.type === 'DESCRIPTIONS')
const hasElement = computed(() => !!store.activeElement)

/** Only reset tab when selection identity changes — not on every style patch. */
watch(
  () => `${store.surfaceId}|${store.activeElement?.id || ''}|${store.activeElement?.type || ''}`,
  (next, prev) => {
    if (next === prev)
      return
    tab.value = 'basic'
  },
)
</script>

<template>
  <NTabs v-model:value="tab" type="line" size="small" class="prop-tabs" justify-content="space-evenly">
    <NTabPane name="basic" tab="基础">
      <StaticTablePanel v-if="isStaticTable" />
      <DescriptionsPanel v-if="isDescriptions" />
      <TablePanel v-if="isTable" />
      <BindingPanel v-if="hasBinding && !isStaticTable" />
      <ElementGeometryPanel />
    </NTabPane>
    <NTabPane name="style" tab="样式">
      <TextPanel mode="style" />
    </NTabPane>
    <NTabPane name="border" tab="边框">
      <TextPanel mode="border" />
    </NTabPane>
    <NTabPane name="advanced" tab="高级" :disabled="!hasElement && !isTable">
      <ElementOptionsPanel v-if="hasElement" />
      <p v-else-if="isTable" class="muted tip">
        明细列、表头合并与合计请在「基础」页编辑。
      </p>
      <p v-else class="muted tip">
        选中画布元素后可配置旋转、锁定等高级项。
      </p>
    </NTabPane>
  </NTabs>
</template>

<style scoped>
.prop-tabs {
  --n-tab-gap: 0;
  --n-pane-padding-top: 4px;
}
.prop-tabs :deep(.n-tabs-nav) {
  padding: 0 2px;
}
.prop-tabs :deep(.n-tabs-tab) {
  padding: 4px 0 !important;
  font-size: 12px;
  font-weight: 600;
}
.prop-tabs :deep(.designer-group) {
  border-bottom: 0;
  padding-top: 4px;
  padding-bottom: 8px;
}
.prop-tabs :deep(.designer-group > h3) {
  margin-bottom: 6px;
  color: var(--text-secondary);
  font-size: 11px;
}
.prop-tabs :deep(.n-form-item) {
  width: 100%;
  margin-bottom: 4px;
}
.prop-tabs :deep(.n-form-item-blank) {
  width: 100%;
  min-width: 0;
}
.prop-tabs :deep(.panel-grid) {
  grid-template-columns: 1fr;
}
.tip {
  margin: 8px 4px;
  font-size: 11px;
}
:deep(.n-color-picker__value) {
  display: none !important;
}
</style>
