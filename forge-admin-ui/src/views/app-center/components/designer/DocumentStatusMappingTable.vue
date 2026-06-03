<template>
  <div class="status-mapping-table">
    <div class="table-toolbar">
      <n-space size="small">
        <n-button size="small" secondary :disabled="disabled" @click="useDefaultRows">
          使用默认状态集
        </n-button>
        <n-button size="small" secondary :disabled="disabled" @click="generateFromStatusField">
          从状态字段生成
        </n-button>
      </n-space>
    </div>

    <div class="status-table">
      <div class="status-header">
        <span>标准状态</span>
        <span>存储值</span>
        <span>展示名</span>
        <span>标签</span>
        <span>允许编辑</span>
        <span>允许删除</span>
        <span>允许发起</span>
      </div>
      <div v-for="row in localRows" :key="row.standardStatus" class="status-row">
        <strong>{{ row.standardLabel || row.standardStatus }}</strong>
        <n-input
          v-model:value="row.statusValue"
          :disabled="disabled"
          size="small"
          placeholder="存储值"
          @update:value="emitRows"
        />
        <n-input
          v-model:value="row.displayName"
          :disabled="disabled"
          size="small"
          placeholder="展示名"
          @update:value="emitRows"
        />
        <n-select
          v-model:value="row.tagType"
          :disabled="disabled"
          :options="tagOptions"
          size="small"
          @update:value="emitRows"
        />
        <n-switch v-model:value="row.allowEdit" :disabled="disabled" size="small" @update:value="emitRows" />
        <n-switch v-model:value="row.allowDelete" :disabled="disabled" size="small" @update:value="emitRows" />
        <n-switch v-model:value="row.allowStartFlow" :disabled="disabled" size="small" @update:value="emitRows" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { ref, watch } from 'vue'

const props = defineProps({
  rows: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  statusField: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:rows'])
const message = useMessage()
const localRows = ref(normalizeRows(props.rows))

const tagOptions = [
  { label: '默认', value: 'default' },
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
]

watch(() => props.rows, (rows) => {
  localRows.value = normalizeRows(rows)
}, { deep: true })

function useDefaultRows() {
  localRows.value = defaultRows()
  emitRows()
}

function generateFromStatusField() {
  const field = props.fields.find(item => fieldCode(item) === props.statusField)
  const options = Array.isArray(field?.basicProps?.options)
    ? field.basicProps.options
    : Array.isArray(field?.advancedProps?.options)
      ? field.advancedProps.options
      : []
  if (!options.length) {
    message.info('状态字段未包含本地选项，已使用默认状态集')
    useDefaultRows()
    return
  }
  const defaults = defaultRows()
  localRows.value = defaults.map((row, index) => {
    const option = options[index]
    if (!option)
      return row
    return {
      ...row,
      statusValue: String(option.value ?? option.dictValue ?? row.statusValue),
      displayName: String(option.label ?? option.dictLabel ?? row.displayName),
    }
  })
  emitRows()
}

function emitRows() {
  emit('update:rows', localRows.value.map(row => ({ ...row })))
}

function normalizeRows(rows = []) {
  const byKey = new Map(defaultRows().map(row => [row.standardStatus, { ...row }]))
  for (const row of rows || []) {
    if (!row?.standardStatus)
      continue
    const key = String(row.standardStatus).toUpperCase()
    byKey.set(key, {
      ...(byKey.get(key) || {}),
      ...row,
      standardStatus: key,
    })
  }
  return Array.from(byKey.values())
}

function defaultRows() {
  return [
    row('DRAFT', '草稿', 'DRAFT', '草稿', 'default', true, true, true),
    row('SUBMITTED', '已提交', 'SUBMITTED', '已提交', 'info', false, false, false),
    row('IN_PROCESS', '流程中', 'IN_PROCESS', '流程中', 'warning', false, false, false),
    row('APPROVED', '已通过', 'APPROVED', '已通过', 'success', false, false, false),
    row('REJECTED', '已驳回', 'REJECTED', '已驳回', 'error', true, false, true),
    row('CANCELED', '已撤回', 'CANCELED', '已撤回', 'default', true, false, true),
    row('CLOSED', '已关闭', 'CLOSED', '已关闭', 'default', false, false, false),
  ]
}

function row(standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow) {
  return { standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow }
}

function fieldCode(field = {}) {
  return field.fieldCode || field.field || ''
}
</script>

<style scoped>
.status-mapping-table {
  display: grid;
  gap: 10px;
}

.table-toolbar {
  display: flex;
  justify-content: flex-end;
}

.status-table {
  display: grid;
  min-width: 760px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.status-header,
.status-row {
  display: grid;
  grid-template-columns: 92px minmax(120px, 1fr) minmax(120px, 1fr) 96px 68px 68px 68px;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
}

.status-header {
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
}

.status-row + .status-row {
  border-top: 1px solid #edf2f7;
}

.status-row strong {
  color: #111827;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 900px) {
  .status-mapping-table {
    overflow-x: auto;
  }
}
</style>
