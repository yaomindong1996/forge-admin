<template>
  <div class="page-builder-shell">
    <div class="builder-toolbar">
      <n-tabs v-model:value="builderTab" type="segment" size="small" class="builder-tabs">
        <n-tab name="list">
          列表页面
        </n-tab>
        <n-tab name="edit">
          表单与详情
        </n-tab>
      </n-tabs>
      <n-space size="small" align="center">
        <n-radio-group v-model:value="localSchema.layoutType" size="small">
          <n-radio-button
            v-for="option in layoutOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </n-radio-button>
        </n-radio-group>
      </n-space>
    </div>
    <div v-if="builderTab === 'list'" class="structured-builder">
      <ListPageGridDesigner
        :model-value="localSchema.listGridLayout || {}"
        :fields="fields"
        :model-schema="modelSchema"
        :layout-type="localSchema.layoutType"
        @update:model-value="handleGridLayoutUpdate"
      />
    </div>
    <div v-else class="form-component-builder">
      <ComponentPalette
        selected-zone-key="edit"
        :fields="fields"
        :used-field-refs="editUsedFieldRefs"
        @select="noop"
      />
      <CanvasFormDesigner
        :zone="editZone"
        :fields="fields"
        :selected-item-id="selectedEditItemId"
        @select-item="selectedEditItemId = $event"
        @update:zone="handleZoneUpdate"
      />
      <ComponentPropertyPanel
        :zone="editZone"
        :selected-item="selectedEditItem"
        :fields="fields"
        :layout-type="localSchema.layoutType"
        @update:zone="handleZoneUpdate"
        @update-item="handleEditItemUpdate"
        @remove-item="handleEditItemRemove"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { cloneSchema, isSameSchema } from '../model/model-schema'
import CanvasFormDesigner from './CanvasFormDesigner.vue'
import ComponentPalette from './ComponentPalette.vue'
import ComponentPropertyPanel from './ComponentPropertyPanel.vue'
import ListPageGridDesigner from './ListPageGridDesigner.vue'
import {
  applyGridLayoutToZones,
  createDefaultListGridLayout,
  patchZoneCanvas,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from './page-schema'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  modelSchema: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const localSchema = ref(resolvePageBuilderSchema(props.modelValue, props.modelSchema))
const builderTab = ref('list')
const selectedEditItemId = ref('')
const layoutOptions = [
  { label: '标准单表', value: 'simple-crud' },
  { label: '左树右表', value: 'tree-crud' },
  { label: '主子表', value: 'master-detail-crud' },
]

const fields = computed(() => props.modelSchema?.fields || [])
const editZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'edit') || null)
const selectedEditItem = computed(() => {
  const items = editZone.value?.props?.canvas?.items || []
  return items.find(item => item.id === selectedEditItemId.value) || null
})
const editUsedFieldRefs = computed(() => resolveCanvasFieldRefs(editZone.value?.props?.canvas?.items || []))

watch(
  () => props.modelValue,
  (value) => {
    const next = resolvePageBuilderSchema(value, props.modelSchema)
    if (!isSameSchema(next, localSchema.value)) {
      localSchema.value = next
    }
  },
  { deep: true },
)

watch(
  () => props.modelSchema,
  (value) => {
    const next = resolvePageBuilderSchema(localSchema.value, value)
    if (!isSameSchema(next, localSchema.value)) {
      localSchema.value = next
    }
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
    }
  },
  { deep: true },
)

watch(
  () => localSchema.value.layoutType,
  () => {
    const synced = syncGridLayoutWithModel(
      localSchema.value.listGridLayout || createDefaultListGridLayout(props.modelSchema, { layoutType: localSchema.value.layoutType }),
      props.modelSchema,
      { layoutType: localSchema.value.layoutType },
    )
    applyGridLayoutChange(synced)
  },
)

function handleZoneUpdate(zone) {
  localSchema.value = {
    ...localSchema.value,
    zones: (localSchema.value.zones || []).map(item => item.zoneKey === zone.zoneKey ? zone : item),
  }
}

function noop() {}

function resolvePageBuilderSchema(schema, modelSchema) {
  return syncPageSchemaWithModel({
    ...cloneSchema(schema || {}),
    listLayoutMode: 'grid',
  }, modelSchema)
}

function resolveCanvasFieldRefs(items = []) {
  const refs = []
  items.forEach((item) => {
    if (item.fieldRef)
      refs.push(item.fieldRef)
    refs.push(...(item.fieldRefs || item.props?.fieldRefs || []))
  })
  return Array.from(new Set(refs))
}

function handleEditItemUpdate(item) {
  if (!editZone.value || !item)
    return
  const canvas = editZone.value.props?.canvas || {}
  const items = (canvas.items || []).map(canvasItem => canvasItem.id === item.id ? item : canvasItem)
  handleZoneUpdate(patchZoneCanvas(editZone.value, {
    ...canvas,
    items,
  }))
}

function handleEditItemRemove(itemId) {
  if (!editZone.value || !itemId)
    return
  const canvas = editZone.value.props?.canvas || {}
  const items = (canvas.items || []).filter(item => item.id !== itemId)
  if (selectedEditItemId.value === itemId)
    selectedEditItemId.value = ''
  handleZoneUpdate(patchZoneCanvas(editZone.value, {
    ...canvas,
    items,
  }))
}

function handleGridLayoutUpdate(layout) {
  applyGridLayoutChange(layout)
}

function applyGridLayoutChange(layout) {
  const synced = syncGridLayoutWithModel(layout, props.modelSchema, { layoutType: localSchema.value.layoutType })
  const zones = applyGridLayoutToZones(localSchema.value.zones || [], synced, props.modelSchema)
  localSchema.value = {
    ...localSchema.value,
    listLayoutMode: 'grid',
    listGridLayout: synced,
    zones,
  }
}
</script>

<style scoped>
.page-builder-shell {
  display: grid;
  gap: 12px;
}

.builder-toolbar {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}

.builder-tabs {
  width: 240px;
}

.structured-builder {
  min-height: 704px;
}

.form-component-builder {
  min-height: 704px;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 300px;
  gap: 12px;
}
</style>
