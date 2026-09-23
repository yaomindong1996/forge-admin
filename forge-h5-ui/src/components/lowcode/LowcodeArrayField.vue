<template>
  <view class="lowcode-array">
    <view v-if="rows.length" class="lowcode-array__rows">
      <view v-for="(row, rowIndex) in rows" :key="rowKeys[rowIndex]" class="lowcode-array__row">
        <view class="lowcode-array__head">
          <text>{{ itemTitle(rowIndex) }}</text>
          <button v-if="canDelete" class="lowcode-array__remove" @click="removeRow(rowIndex)">删除</button>
        </view>
        <LowcodeField
          v-for="itemField in runtimeItemSchema"
          :key="itemField.field"
          :field="itemField"
          :model-value="row[itemField.field]"
          :options="resolveOptions(itemField)"
          :readonly="readonly || !canUpdate"
          :error="errors[`${rowIndex}.${itemField.field}`]"
          @update:model-value="updateCell(rowIndex, itemField.field, $event)"
        />
      </view>
    </view>
    <view v-else class="lowcode-array__empty">暂无明细</view>
    <view v-if="canCreate" class="lowcode-array__actions">
      <wd-button size="small" plain :disabled="maxItems > 0 && rows.length >= maxItems" @click="addRow">
        {{ arrayConfig.addText || '新增明细' }}
      </wd-button>
      <text v-if="maxItems > 0" class="lowcode-array__limit">{{ rows.length }}/{{ maxItems }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive } from 'vue'
import LowcodeField from './LowcodeField.vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  field: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])
const errors = reactive({})

const rows = computed(() => Array.isArray(props.modelValue) ? props.modelValue : [])
const rowKeys = computed(() => rows.value.map((row, index) => row?.id || row?._rowKey || `${index}-${JSON.stringify(row)}`))
const arrayConfig = computed(() => props.field.arrayConfig || props.field.props?.arrayConfig || {})
const runtimeItemSchema = computed(() => (Array.isArray(props.field.itemSchema) ? props.field.itemSchema : [])
  .map(applyItemPermission)
  .filter(field => field && field.formVisible !== false && field.hidden !== true))
const minItems = computed(() => normalizeLimit(arrayConfig.value.min, 0))
const maxItems = computed(() => normalizeLimit(arrayConfig.value.max, 0))
const canCreate = computed(() => !props.readonly && !props.disabled && arrayConfig.value.allowCreate !== false)
const canUpdate = computed(() => !props.readonly && !props.disabled && arrayConfig.value.allowUpdate !== false)
const canDelete = computed(() => !props.readonly && !props.disabled && arrayConfig.value.allowDelete !== false && rows.value.length > minItems.value)

function applyItemPermission(field) {
  const key = String(field?.field || field?.fieldCode || '').trim()
  if (!key) return null
  const permissions = props.field.itemPermissions || props.field.props?.itemPermissions || []
  const permission = permissions.find(item => String(item?.field || item?.fieldCode || '') === key)
  if (!permission) return { ...field, field: key }
  if (permission.readable === false || permission.visible === false) return null
  const writable = permission.writable === true || permission.editable === true
  return {
    ...field,
    field: key,
    readonly: !writable,
    required: writable && permission.required === true,
    props: { ...(field.props || {}), readonly: !writable, disabled: !writable },
  }
}

function resolveOptions(field) {
  const source = field?.options || field?.props?.options || []
  return Array.isArray(source)
    ? source.map(item => typeof item === 'object' ? item : ({ label: String(item), value: item }))
    : []
}

function addRow() {
  if (!canCreate.value || (maxItems.value > 0 && rows.value.length >= maxItems.value)) return
  const row = runtimeItemSchema.value.reduce((result, field) => {
    result[field.field] = clone(field.defaultValue ?? field.props?.defaultValue ?? '')
    return result
  }, { _rowKey: `new-${Date.now()}-${rows.value.length}` })
  emitRows([...rows.value, row])
}

function removeRow(index) {
  if (!canDelete.value) return
  emitRows(rows.value.filter((_, rowIndex) => rowIndex !== index))
}

function updateCell(index, field, value) {
  if (!canUpdate.value) return
  const next = rows.value.map((row, rowIndex) => rowIndex === index ? { ...row, [field]: value } : row)
  delete errors[`${index}.${field}`]
  emitRows(next)
}

function emitRows(value) {
  const next = value.map(row => {
    const cloned = clone(row)
    delete cloned._rowKey
    return cloned
  })
  emit('update:modelValue', next)
  emit('change', next)
}

function itemTitle(index) {
  return String(arrayConfig.value.itemTitle || '第 {index} 项').replace('{index}', String(index + 1))
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (rows.value.length < minItems.value) return false
  if (maxItems.value > 0 && rows.value.length > maxItems.value) return false
  rows.value.forEach((row, rowIndex) => runtimeItemSchema.value.forEach(field => {
    if (!field.required || field.readonly) return
    const value = row?.[field.field]
    if (value === undefined || value === null || value === '' || (Array.isArray(value) && !value.length))
      errors[`${rowIndex}.${field.field}`] = field.requiredMessage || `请输入${field.label || field.field}`
  }))
  return Object.keys(errors).length === 0
}

function normalizeLimit(value, fallback) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

function clone(value) {
  if (value === undefined || value === null || typeof value !== 'object') return value
  return JSON.parse(JSON.stringify(value))
}

defineExpose({ validate })
</script>

<style lang="scss" scoped>
.lowcode-array { display: flex; flex-direction: column; gap: 14rpx; }
.lowcode-array__rows { display: flex; flex-direction: column; gap: 16rpx; }
.lowcode-array__row { padding: 32rpx; border: 1rpx solid var(--forge-color-border, #c9cdd4); border-radius: var(--radius-card); background: var(--forge-color-surface-subtle, #f7f8fa); }
.lowcode-array__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; color: #1d2129; font-size: 28rpx; font-weight: 500; }
.lowcode-array__remove { min-width: 88rpx; min-height: 88rpx; margin: -22rpx -16rpx -22rpx 0; padding: 0; border: 0; color: #f53f3f; font-size: 24rpx; line-height: 88rpx; background: transparent; }
.lowcode-array__remove::after { border: 0; }
.lowcode-array__empty { padding: 32rpx; border: 1rpx dashed var(--border-color); border-radius: var(--radius-card); color: #86909c; font-size: 24rpx; text-align: center; }
.lowcode-array__actions { display: flex; align-items: center; gap: 14rpx; }
.lowcode-array__limit { color: #86909c; font-size: 22rpx; }
</style>
