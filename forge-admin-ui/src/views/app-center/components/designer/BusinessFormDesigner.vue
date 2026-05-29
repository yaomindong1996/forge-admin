<template>
  <div class="business-form-designer">
    <section class="form-canvas-region">
      <div class="designer-section-head">
        <div>
          <h3>表单画布</h3>
          <p>拖拽右侧字段到画布，调整新增和编辑表单布局。</p>
        </div>
        <n-space size="small">
          <n-button size="small" secondary @click="applyFieldGroup('basic')">
            基础信息
          </n-button>
          <n-button size="small" secondary @click="applyFieldGroup('marketing')">
            营销数据
          </n-button>
          <n-button size="small" secondary @click="applyFieldGroup('contact')">
            联系人信息
          </n-button>
          <n-button size="small" type="primary" :loading="saving" @click="saveLayout">
            保存表单
          </n-button>
        </n-space>
      </div>

      <div class="form-builder-grid">
        <CanvasFormDesigner
          :zone="editZone"
          :fields="designFields"
          :selected-item-id="selectedItemId"
          @select-item="selectedItemId = $event"
          @update:zone="handleZoneUpdate"
        />
        <ComponentPropertyPanel
          :zone="editZone"
          :selected-item="selectedItem"
          :fields="designFields"
          :layout-type="localSchema.layoutType"
          @update:zone="handleZoneUpdate"
          @update-item="handleItemUpdate"
          @remove-item="handleItemRemove"
        />
      </div>
    </section>

    <aside class="field-shelf">
      <div class="shelf-head">
        <div>
          <h3>字段库</h3>
          <p>未使用字段可拖入画布，已使用字段会标记。</p>
        </div>
        <n-button size="small" secondary @click="$emit('createField')">
          新增字段
        </n-button>
      </div>

      <div class="field-state-tabs">
        <button type="button" :class="{ active: shelfTab === 'unused' }" @click="shelfTab = 'unused'">
          未使用 {{ unusedFields.length }}
        </button>
        <button type="button" :class="{ active: shelfTab === 'used' }" @click="shelfTab = 'used'">
          已使用 {{ usedFields.length }}
        </button>
        <button type="button" :class="{ active: shelfTab === 'system' }" @click="shelfTab = 'system'">
          系统 {{ systemFields.length }}
        </button>
      </div>

      <div class="shelf-list">
        <button
          v-for="field in visibleShelfFields"
          :key="field.field"
          type="button"
          class="shelf-field"
          draggable="true"
          :disabled="field.systemField || usedFieldSet.has(field.field)"
          @dragstart="handleFieldDragStart($event, field)"
          @click="appendField(field)"
        >
          <strong>{{ field.label || field.field }}</strong>
          <span>{{ field.field }} · {{ field.componentType || field.dataType || 'input' }}</span>
          <em v-if="usedFieldSet.has(field.field)">已使用</em>
          <em v-else-if="field.systemField">系统字段</em>
        </button>
        <n-empty v-if="!visibleShelfFields.length" description="当前分组没有字段" />
      </div>
    </aside>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { saveBusinessObjectFormLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import CanvasFormDesigner from '@/components/lowcode-builder/page/CanvasFormDesigner.vue'
import ComponentPropertyPanel from '@/components/lowcode-builder/page/ComponentPropertyPanel.vue'
import {
  createCanvasItem,
  createDefaultPageSchema,
  isReadonlySystemField,
  patchZoneCanvas,
  resolveDefaultFieldComponentKey,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  modelValue: {
    type: Object,
    default: null,
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
  fields: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'saved', 'dirtyChange', 'createField'])

const message = useMessage()
const selectedItemId = ref('')
const saving = ref(false)
const shelfTab = ref('unused')

const designFields = computed(() => {
  const modelFields = props.modelSchema?.fields || []
  if (modelFields.length)
    return modelFields
  return props.fields.map(toPageField)
})

const effectiveModelSchema = computed(() => ({
  ...(props.modelSchema || {}),
  fields: designFields.value,
}))

const localSchema = ref(resolveSchema(props.modelValue, effectiveModelSchema.value))
const editZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'edit') || null)
const usedFieldSet = computed(() => new Set(resolveCanvasFieldRefs(editZone.value?.props?.canvas?.items || [])))
const selectedItem = computed(() => {
  const items = editZone.value?.props?.canvas?.items || []
  return items.find(item => item.id === selectedItemId.value) || null
})
const businessFields = computed(() => designFields.value.filter(field => !isReadonlySystemField(field)))
const systemFields = computed(() => designFields.value.filter(field => isReadonlySystemField(field)))
const usedFields = computed(() => businessFields.value.filter(field => usedFieldSet.value.has(field.field)))
const unusedFields = computed(() => businessFields.value.filter(field => !usedFieldSet.value.has(field.field)))
const visibleShelfFields = computed(() => {
  if (shelfTab.value === 'used')
    return usedFields.value
  if (shelfTab.value === 'system')
    return systemFields.value
  return unusedFields.value
})

watch(
  () => props.modelValue,
  (value) => {
    const next = resolveSchema(value, effectiveModelSchema.value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
  },
  { deep: true },
)

watch(
  effectiveModelSchema,
  (value) => {
    const next = syncPageSchemaWithModel(localSchema.value, value)
    if (!isSameSchema(next, localSchema.value))
      localSchema.value = next
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
      emit('dirtyChange', true)
    }
  },
  { deep: true },
)

function handleZoneUpdate(zone) {
  localSchema.value = {
    ...localSchema.value,
    zones: (localSchema.value.zones || []).map(item => item.zoneKey === zone.zoneKey ? zone : item),
  }
}

function handleItemUpdate(item) {
  if (!editZone.value)
    return
  const canvas = editZone.value.props?.canvas || { items: [] }
  handleZoneUpdate(patchZoneCanvas(editZone.value, {
    ...canvas,
    items: (canvas.items || []).map(current => current.id === item.id ? item : current),
  }))
}

function handleItemRemove(item) {
  if (!editZone.value || !item)
    return
  const canvas = editZone.value.props?.canvas || { items: [] }
  handleZoneUpdate(patchZoneCanvas(editZone.value, {
    ...canvas,
    items: (canvas.items || []).filter(current => current.id !== item.id),
  }))
  selectedItemId.value = ''
}

function handleFieldDragStart(event, field) {
  if (field.systemField || usedFieldSet.value.has(field.field)) {
    event.preventDefault()
    return
  }
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-lowcode-component', JSON.stringify({
    componentKey: resolveDefaultFieldComponentKey(field),
    fieldRef: field.field,
    label: field.label || field.field,
  }))
}

function appendField(field) {
  if (!editZone.value || field.systemField || usedFieldSet.value.has(field.field))
    return
  const canvas = editZone.value.props?.canvas || { width: 1040, height: 420, items: [] }
  const index = (canvas.items || []).filter(item => item.fieldRef).length
  const item = createCanvasItem({
    componentKey: resolveDefaultFieldComponentKey(field),
    fieldRef: field.field,
    label: field.label || field.field,
    x: 32,
    y: 36 + index * 86,
    w: 720,
    zIndex: (canvas.items || []).length + 1,
  }, {
    zoneKey: 'edit',
    fields: designFields.value,
  })
  handleZoneUpdate(patchZoneCanvas(editZone.value, {
    ...canvas,
    height: Math.max(Number(canvas.height || 420), item.y + item.h + 48),
    items: [...(canvas.items || []), item],
  }))
  selectedItemId.value = item.id
}

function applyFieldGroup(groupKey) {
  const presets = {
    basic: ['name', 'customerName', 'customerLevel', 'phone', 'ownerUserId', 'regionCode'],
    marketing: ['source', 'level', 'stage', 'amount', 'followStatus', 'nextFollowTime'],
    contact: ['contactName', 'contactPhone', 'email', 'wechat', 'address', 'remark'],
  }
  const candidates = presets[groupKey] || []
  const matched = designFields.value.filter((field) => {
    const name = field.field || ''
    return candidates.some(key => name === key || name.toLowerCase().includes(key.toLowerCase()))
  })
  matched.forEach(appendField)
}

async function saveLayout() {
  if (!props.objectId)
    return
  saving.value = true
  try {
    await saveBusinessObjectFormLayout(props.objectId, {
      layoutKey: 'form',
      layoutName: '表单布局',
      layoutType: localSchema.value.layoutType,
      pageSchema: cloneSchema(localSchema.value),
      zones: localSchema.value.zones?.filter(zone => zone.zoneKey === 'edit') || [],
      settings: {},
    })
    emit('saved', cloneSchema(localSchema.value))
    emit('dirtyChange', false)
    message.success('表单布局已保存')
  }
  finally {
    saving.value = false
  }
}

function resolveSchema(pageSchema, modelSchema) {
  return syncPageSchemaWithModel(
    cloneSchema(pageSchema || createDefaultPageSchema(modelSchema)),
    modelSchema,
  )
}

function resolveCanvasFieldRefs(items) {
  const refs = new Set()
  items.forEach((item) => {
    if (item.fieldRef)
      refs.add(item.fieldRef)
    ;(item.fieldRefs || item.props?.fieldRefs || []).forEach(ref => refs.add(ref))
  })
  return Array.from(refs)
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    comment: field.remark || field.fieldName,
    columnName: field.columnName,
    dataType: field.dataType,
    componentType: field.componentType,
    dictType: field.dictType,
    required: field.required,
    systemField: field.systemField,
    readonly: field.readonly,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
  }
}

defineExpose({
  saveLayout,
})
</script>

<style scoped>
.business-form-designer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: calc(100vh - 106px);
  container-type: inline-size;
}

.form-canvas-region {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  border-right: 1px solid #e5e7eb;
}

.designer-section-head,
.shelf-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.designer-section-head h3,
.shelf-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  letter-spacing: 0;
}

.designer-section-head p,
.shelf-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.form-builder-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  min-height: calc(100vh - 168px);
  background: #f8fafc;
  padding: 14px;
}

.field-shelf {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-width: 0;
  background: #fbfcfe;
}

.field-state-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  border-bottom: 1px solid #e5e7eb;
  padding: 10px;
}

.field-state-tabs button {
  min-height: 30px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
}

.field-state-tabs button.active {
  border-color: #2563eb;
  background: #eaf2ff;
  color: #1d4ed8;
}

.shelf-list {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow: auto;
  padding: 10px;
}

.shelf-field {
  position: relative;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: grab;
  text-align: left;
  padding: 10px;
}

.shelf-field:disabled {
  cursor: default;
  opacity: 0.72;
}

.shelf-field strong {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelf-field span {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelf-field em {
  display: inline-flex;
  margin-top: 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-style: normal;
  line-height: 20px;
  padding: 0 6px;
}

@media (max-width: 1500px) {
  .business-form-designer {
    grid-template-columns: 1fr;
  }

  .form-canvas-region {
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .field-shelf {
    max-height: 300px;
  }

  .shelf-list {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

@container (max-width: 860px) {
  .form-builder-grid {
    grid-template-columns: 1fr;
  }

  .designer-section-head,
  .shelf-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

@container (max-width: 640px) {
  .designer-section-head :deep(.n-space) {
    flex-wrap: wrap;
  }
}
</style>
