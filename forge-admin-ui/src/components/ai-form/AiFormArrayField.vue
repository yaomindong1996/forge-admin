<template>
  <div class="ai-form-array" :class="`is-${displayMode}`">
    <div v-if="rows.length" class="ai-form-array__rows">
      <section
        v-for="(row, index) in rows"
        :key="rowKeys[index]"
        class="ai-form-array__row"
      >
        <div class="ai-form-array__row-head">
          <span>{{ resolveItemTitle(index) }}</span>
          <n-button
            v-if="canDeleteRow(index)"
            text
            type="error"
            size="small"
            :disabled="rows.length <= minItems"
            @click="removeRow(index)"
          >
            删除
          </n-button>
        </div>
        <component
          :is="AiFormComponent"
          :ref="instance => setRowFormRef(instance, index)"
          :value="row"
          :schema="runtimeItemSchema(index)"
          :field-permissions="itemPermissionsFor(index)"
          :show-actions="false"
          :show-feedback="true"
          :grid-cols="itemGridCols"
          label-placement="top"
          :context="buildRowContext(row, index)"
          @update:value="updateRow(index, $event)"
        />
      </section>
    </div>

    <n-empty v-else size="small" description="暂无明细" class="ai-form-array__empty" />

    <div v-if="canCreate" class="ai-form-array__actions">
      <n-button
        type="primary"
        secondary
        size="small"
        :disabled="maxItems > 0 && rows.length >= maxItems"
        @click="addRow"
      >
        {{ arrayConfig.addText || '新增明细' }}
      </n-button>
      <span v-if="maxItems > 0" class="ai-form-array__limit">
        {{ rows.length }}/{{ maxItems }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { createArrayPermissionMap, normalizeArrayItemPermissions } from '@/utils/field-permissions'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  field: { type: Object, required: true },
  formData: { type: Object, default: () => ({}) },
  context: { type: Object, default: () => ({}) },
  disabled: Boolean,
})

const emit = defineEmits(['update:modelValue'])

const AiFormComponent = computed(() => props.context?.aiFormComponent || null)
const rows = ref([])
const rowKeys = ref([])
const rowFormRefs = ref([])
const newRowKeys = new Set()
let rowSeed = 0
let pendingLocalSignature = ''

const fieldKey = computed(() => String(props.field.field || props.field.fieldCode || '').trim())
const arrayConfig = computed(() => props.field.arrayConfig || props.field.props?.arrayConfig || {})
const displayMode = computed(() => arrayConfig.value.displayMode || 'list')
const minItems = computed(() => normalizeLimit(arrayConfig.value.min, 0))
const maxItems = computed(() => normalizeLimit(arrayConfig.value.max, 0))
const baseItemSchema = computed(() => Array.isArray(props.field.itemSchema) ? props.field.itemSchema : [])
const explicitArrayPermission = computed(() => createArrayPermissionMap(
  props.context?.fieldPermissions,
  { readOnly: props.disabled },
).get(fieldKey.value))
const savedItemPermissions = computed(() => normalizeArrayItemPermissions(
  props.context?.fieldPermissions,
  fieldKey.value,
  { readOnly: props.disabled },
))
const permissionManaged = computed(() => props.context?.hasExplicitFieldPermissions === true)
const arrayPermissionManaged = computed(() => Boolean(explicitArrayPermission.value) || savedItemPermissions.value.length > 0)
const itemPermissions = computed(() => {
  if (!arrayPermissionManaged.value)
    return savedItemPermissions.value
  const saved = new Map(savedItemPermissions.value.map(item => [item.field, item]))
  return baseItemSchema.value
    .map((field) => {
      const key = String(field?.field || field?.fieldCode || '').trim()
      if (!key)
        return null
      return saved.get(key) || {
        field: key,
        fieldCode: key,
        readable: true,
        visible: true,
        writable: false,
        editable: false,
        required: false,
      }
    })
    .filter(Boolean)
})
const canUpdate = computed(() => !props.disabled
  && arrayConfig.value.allowUpdate !== false
  && explicitArrayPermission.value?.allowUpdate !== false)
const canCreate = computed(() => !props.disabled
  && arrayConfig.value.allowCreate !== false
  && (explicitArrayPermission.value?.allowCreate === true || !permissionManaged.value))
const canDelete = computed(() => !props.disabled
  && arrayConfig.value.allowDelete !== false
  && (explicitArrayPermission.value?.allowDelete === true || !permissionManaged.value))
const itemGridCols = computed(() => {
  const configured = Number(arrayConfig.value.gridCols)
  if (Number.isFinite(configured) && configured > 0)
    return Math.min(4, Math.max(1, configured))
  if (displayMode.value === 'table')
    return Math.min(4, Math.max(1, baseItemSchema.value.length))
  return 2
})

watch(
  () => props.modelValue,
  (value) => {
    const nextRows = Array.isArray(value) ? value.map(cloneRow) : []
    const signature = JSON.stringify(nextRows)
    const localEcho = pendingLocalSignature && signature === pendingLocalSignature
    if (!localEcho) {
      newRowKeys.clear()
      rowKeys.value = []
    }
    pendingLocalSignature = ''
    rows.value = nextRows
    rowKeys.value = nextRows.map((_, index) => rowKeys.value[index] || createRowKey())
    rowFormRefs.value.length = nextRows.length
  },
  { immediate: true, deep: true },
)

function addRow() {
  if (!canCreate.value || (maxItems.value > 0 && rows.value.length >= maxItems.value))
    return
  const rowKey = createRowKey()
  newRowKeys.add(rowKey)
  rows.value = [...rows.value, buildDefaultRow()]
  rowKeys.value = [...rowKeys.value, rowKey]
  emitRows()
}

function removeRow(index) {
  if (!canDeleteRow(index) || rows.value.length <= minItems.value)
    return
  newRowKeys.delete(rowKeys.value[index])
  rows.value = rows.value.filter((_, rowIndex) => rowIndex !== index)
  rowKeys.value = rowKeys.value.filter((_, rowIndex) => rowIndex !== index)
  rowFormRefs.value = rowFormRefs.value.filter((_, rowIndex) => rowIndex !== index)
  emitRows()
}

function updateRow(index, value) {
  if (!isRowEditable(index))
    return
  rows.value = rows.value.map((row, rowIndex) => rowIndex === index ? cloneRow(value) : row)
  emitRows()
}

function emitRows() {
  const value = rows.value.map(cloneRow)
  pendingLocalSignature = JSON.stringify(value)
  emit('update:modelValue', value)
}

function buildDefaultRow() {
  return baseItemSchema.value.reduce((result, field) => {
    const key = field?.field || field?.fieldCode
    if (key)
      result[key] = cloneValue(field.defaultValue ?? field.props?.defaultValue ?? null)
    return result
  }, {})
}

function buildRowContext(row, index) {
  return {
    ...(props.context || {}),
    currentRow: row,
    row,
    rowIndex: index,
    arrayField: fieldKey.value,
    parentFormData: props.formData,
    fieldPermissions: itemPermissionsFor(index),
    hasExplicitFieldPermissions: itemPermissionsFor(index).length > 0,
  }
}

function isRowEditable(index) {
  return canUpdate.value || newRowKeys.has(rowKeys.value[index])
}

function canDeleteRow(index) {
  return canDelete.value || (canCreate.value && newRowKeys.has(rowKeys.value[index]))
}

function runtimeItemSchema(index) {
  if (isRowEditable(index))
    return baseItemSchema.value
  return baseItemSchema.value.map(field => ({
    ...field,
    required: false,
    readonly: true,
    disabled: true,
    props: {
      ...(field.props || {}),
      readonly: true,
      disabled: true,
    },
  }))
}

function itemPermissionsFor(index) {
  if (isRowEditable(index))
    return itemPermissions.value
  return itemPermissions.value.map(item => ({
    ...item,
    editable: false,
    writable: false,
    required: false,
  }))
}

function resolveItemTitle(index) {
  return String(arrayConfig.value.itemTitle || '第{index}项').replace('{index}', String(index + 1))
}

function setRowFormRef(instance, index) {
  rowFormRefs.value[index] = instance || null
}

async function validate() {
  if (rows.value.length < minItems.value)
    throw new Error(`${props.field.label || fieldKey.value}至少需要 ${minItems.value} 条明细`)
  if (maxItems.value > 0 && rows.value.length > maxItems.value)
    throw new Error(`${props.field.label || fieldKey.value}最多允许 ${maxItems.value} 条明细`)
  for (const form of rowFormRefs.value)
    await form?.validate?.()
  return true
}

function getValue() {
  return rows.value.map(cloneRow)
}

function createRowKey() {
  rowSeed += 1
  return `${fieldKey.value || 'array'}-${rowSeed}`
}

function cloneRow(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? cloneValue(value) : {}
}

function cloneValue(value) {
  if (value === undefined || value === null || typeof value !== 'object')
    return value
  return JSON.parse(JSON.stringify(value))
}

function normalizeLimit(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? number : fallback
}

defineExpose({ validate, getValue })
</script>

<style scoped>
.ai-form-array {
  width: 100%;
}

.ai-form-array__rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-form-array__row {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 12px;
}

.ai-form-array.is-card .ai-form-array__row {
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.ai-form-array.is-table .ai-form-array__rows {
  gap: 0;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  overflow: hidden;
}

.ai-form-array.is-table .ai-form-array__row {
  border: 0;
  border-bottom: 1px solid var(--border-light);
  border-radius: 0;
}

.ai-form-array.is-table .ai-form-array__row:last-child {
  border-bottom: 0;
}

.ai-form-array__row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.ai-form-array__empty {
  border: 1px dashed var(--border-light);
  border-radius: 8px;
  padding: 16px 0;
}

.ai-form-array__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.ai-form-array__limit {
  color: var(--text-tertiary);
  font-size: 12px;
}
</style>
