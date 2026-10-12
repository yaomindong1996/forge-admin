<template>
  <div class="field-table">
    <div class="table-head">
      <span>字段名称</span>
      <span>字段编码</span>
      <span>字段备注</span>
      <span>数据类型</span>
      <span>表单组件</span>
      <span>长度</span>
      <span>小数位</span>
      <span>是否必填</span>
      <span>默认值</span>
      <span>关联配置</span>
      <span>操作</span>
    </div>
    <draggable
      :model-value="fields"
      :item-key="fieldRowKey"
      handle=".drag-handle"
      :move="canMoveField"
      animation="160"
      class="table-body"
      @update:model-value="$emit('update:fields', $event)"
    >
      <template #item="{ element, index }">
        <div class="field-row" :class="{ active: selectedIndex === index, system: isSystemField(element) }" @click="$emit('select', index)">
          <div class="field-name-cell">
            <n-button text size="tiny" class="drag-handle" :disabled="isLockedSystemField(element)" @click.stop>
              <template #icon>
                <n-icon><ReorderFourOutline /></n-icon>
              </template>
            </n-button>
            <n-input
              :value="element.label"
              :disabled="isLockedSystemField(element)"
              size="small"
              placeholder="字段名称"
              @click.stop
              @update:value="updateField(index, { label: $event })"
            />
            <n-tag v-if="isSystemField(element)" size="small" :bordered="false">
              系统
            </n-tag>
          </div>
          <n-input
            :value="element.field"
            :disabled="isLockedSystemField(element)"
            size="small"
            placeholder="fieldName"
            @click.stop
            @blur="normalizeFieldCode(index, element.field)"
            @update:value="handleFieldCodeInput(index, $event)"
          />
          <n-input
            :value="element.remark"
            :disabled="isLockedSystemField(element)"
            size="small"
            placeholder="字段说明"
            @click.stop
            @update:value="updateField(index, { remark: $event })"
          />
          <n-select
            :value="element.dataType"
            :disabled="isLockedSystemField(element)"
            size="small"
            :options="dataTypeOptions"
            @click.stop
            @update:value="handleDataTypeChange(index, $event)"
          />
          <n-select
            :value="element.componentType"
            :disabled="isLockedSystemField(element)"
            size="small"
            :options="componentTypeOptions"
            @click.stop
            @update:value="updateField(index, { componentType: $event })"
          />
          <n-input-number
            :value="element.length"
            :disabled="isLockedSystemField(element)"
            size="small"
            :min="1"
            :max="2048"
            :show-button="false"
            @click.stop
            @update:value="updateField(index, { length: $event })"
          />
          <n-input-number
            :value="element.precision"
            :disabled="isLockedSystemField(element)"
            size="small"
            :min="0"
            :max="12"
            :show-button="false"
            @click.stop
            @update:value="updateField(index, { precision: $event })"
          />
          <n-switch
            :value="element.required"
            :disabled="isLockedSystemField(element)"
            size="small"
            @click.stop
            @update:value="updateField(index, { required: $event })"
          />
          <n-input
            :value="element.defaultValue ?? ''"
            :disabled="isLockedSystemField(element)"
            size="small"
            placeholder="-"
            @click.stop
            @update:value="updateField(index, { defaultValue: $event === '' ? null : $event })"
          />
          <span class="muted">{{ relationText(element) }}</span>
          <div class="field-actions" @click.stop>
            <n-button text size="tiny" class="text-primary" :disabled="isLockedSystemField(element)" @click="$emit('copy', index)">
              复制
            </n-button>
            <n-popconfirm @positive-click="$emit('remove', index)">
              <template #trigger>
                <n-button text size="tiny" class="text-error" :disabled="isLockedSystemField(element)">
                  删除
                </n-button>
              </template>
              删除字段后会同步移除页面引用，确认继续？
            </n-popconfirm>
          </div>
        </div>
      </template>
    </draggable>
  </div>
</template>

<script setup>
import { ReorderFourOutline } from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import { camelToSnake, componentTypeOptions, dataTypeOptions, isLockedSystemField, isSystemField, normalizeFieldName } from './model-schema'

const props = defineProps({
  fields: {
    type: Array,
    default: () => [],
  },
  selectedIndex: {
    type: Number,
    default: -1,
  },
})

const emit = defineEmits(['update:fields', 'select', 'copy', 'remove'])

function updateField(index, patch) {
  if (isLockedSystemField(props.fields[index]))
    return
  const fields = props.fields.map((field, fieldIndex) => fieldIndex === index ? { ...field, ...patch } : field)
  emit('update:fields', fields)
}

function fieldRowKey(field) {
  return props.fields.indexOf(field)
}

function handleFieldCodeInput(index, value) {
  updateField(index, {
    field: value,
    columnName: camelToSnake(value),
  })
}

function normalizeFieldCode(index, value) {
  const nextField = normalizeFieldName(value)
  updateField(index, {
    field: nextField,
    columnName: camelToSnake(nextField),
  })
}

function handleDataTypeChange(index, value) {
  const componentType = ['int', 'bigint', 'decimal'].includes(value)
    ? 'number'
    : ['date', 'datetime', 'time'].includes(value)
        ? value
        : props.fields[index]?.componentType || 'input'
  updateField(index, { dataType: value, componentType })
}

function canMoveField(event) {
  return !isLockedSystemField(event.draggedContext?.element)
}

function relationText(field) {
  if (field.relationLabel)
    return field.relationLabel
  if (field.dictType)
    return field.dictType
  return '-'
}
</script>

<style scoped>
.field-table {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-primary);
}

.table-head,
.field-row {
  display: grid;
  grid-template-columns:
    minmax(136px, 1.15fr)
    minmax(132px, 1fr)
    minmax(150px, 0.95fr)
    112px
    124px
    84px
    84px
    78px
    104px
    104px
    88px;
  align-items: center;
  gap: 10px;
}

.table-head {
  min-height: 38px;
  padding: 0 12px;
  border-bottom: 1px solid #d8dee8;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.table-body {
  min-height: 320px;
}

.field-row {
  min-height: 58px;
  padding: 7px 12px;
  border-bottom: 1px solid #eef2f7;
  cursor: pointer;
}

.field-row:last-child {
  border-bottom: 0;
}

.field-row:hover,
.field-row.active {
  background: #f8fbff;
}

.field-name-cell {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.field-row.system {
  background: var(--bg-secondary);
}

.field-row.system:hover,
.field-row.system.active {
  background: #eef6ff;
}

.field-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.muted {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-table {
  overflow-x: auto;
}

.table-head,
.field-row {
  min-width: 1240px;
}
</style>
