<template>
  <div class="status-mapping-table">
    <div class="table-toolbar">
      <div class="status-source">
        <n-tag size="small" :type="statusValueOptions.length ? 'success' : 'warning'" :bordered="false">
          {{ statusValueOptions.length ? `${statusValueOptions.length} 个状态值` : '未绑定状态选项' }}
        </n-tag>
        <span v-if="statusField">{{ statusField }}</span>
        <span v-else>请先选择状态字段</span>
      </div>
      <n-space size="small">
        <n-button size="small" secondary :disabled="disabled || !statusValueOptions.length" @click="useDefaultRows">
          按标准状态匹配
        </n-button>
        <n-button size="small" secondary :disabled="disabled || !statusValueOptions.length" :loading="loadingOptions" @click="generateFromStatusField">
          从状态字段生成
        </n-button>
      </n-space>
    </div>

    <n-alert v-if="statusField && !statusValueOptions.length" type="warning" :bordered="false" class="status-alert">
      当前状态字段没有字典或本地选项，请先在字段配置中绑定字典或维护选项。
    </n-alert>

    <div class="status-table">
      <div class="status-header">
        <span>标准状态</span>
        <span>字段值</span>
        <span>展示名</span>
        <span>标签</span>
        <span>允许编辑</span>
        <span>允许删除</span>
        <span>允许发起</span>
      </div>
      <div v-for="row in localRows" :key="row.standardStatus" class="status-row">
        <strong>{{ row.standardLabel || row.standardStatus }}</strong>
        <n-select
          v-model:value="row.statusValue"
          :disabled="disabled || !statusSelectOptions.length"
          :options="statusSelectOptions"
          size="small"
          filterable
          placeholder="选择字段值"
          @update:value="value => handleStatusValueChange(row, value)"
        />
        <span class="display-name">{{ row.displayName || '-' }}</span>
        <n-select
          v-model:value="row.tagType"
          :disabled="disabled"
          :options="DICT_TAG_TYPE_OPTIONS"
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
import { computed, ref, watch } from 'vue'
import { DICT_TAG_TYPE_OPTIONS, DICT_TAG_TYPES } from '@/constants/dict-options'

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
  statusValueOptions: {
    type: Array,
    default: () => [],
  },
  loadingOptions: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:rows'])
const message = useMessage()
const localRows = ref(normalizeRows(props.rows))

const statusSelectOptions = computed(() => {
  const options = [...props.statusValueOptions]
  const existing = new Set(options.map(option => String(option.value)))
  localRows.value.forEach((row) => {
    if (!row.statusValue || existing.has(String(row.statusValue)))
      return
    options.push({
      label: `${row.displayName || row.statusValue}（已保存值）`,
      value: String(row.statusValue),
      tagType: normalizeTagType(row.tagType),
    })
  })
  return options.map(option => ({
    label: option.label,
    value: String(option.value),
  }))
})

watch(() => props.rows, (rows) => {
  localRows.value = normalizeRows(rows)
  hydrateRowsFromOptions()
}, { deep: true })

watch(() => props.statusValueOptions, () => {
  hydrateRowsFromOptions()
}, { deep: true })

function useDefaultRows() {
  if (!props.statusValueOptions.length) {
    message.warning('状态字段没有可选值，无法生成状态映射')
    return
  }
  const options = props.statusValueOptions
  localRows.value = defaultRows().map((row) => {
    const option = findOptionForStandardRow(row, options)
    if (!option)
      return row
    return applyOptionToRow(row, option)
  })
  emitRows()
}

function generateFromStatusField() {
  if (!props.statusValueOptions.length) {
    message.warning('状态字段没有可选值，无法生成状态映射')
    return
  }
  const defaults = defaultRows()
  localRows.value = defaults.map((row, index) => {
    const option = findOptionForStandardRow(row, props.statusValueOptions) || props.statusValueOptions[index]
    return option ? applyOptionToRow(row, option) : row
  })
  emitRows()
}

function handleStatusValueChange(row, value) {
  const option = props.statusValueOptions.find(item => String(item.value) === String(value))
  if (option)
    applyOptionToRow(row, option)
  emitRows()
}

function hydrateRowsFromOptions() {
  if (!props.statusValueOptions.length)
    return
  let changed = false
  localRows.value.forEach((row) => {
    const option = props.statusValueOptions.find(item => String(item.value) === String(row.statusValue))
    if (!option)
      return
    const nextDisplayName = option.label || row.displayName
    const nextTagType = normalizeTagType(option.tagType || row.tagType)
    if (row.displayName !== nextDisplayName || row.tagType !== nextTagType) {
      row.displayName = nextDisplayName
      row.tagType = nextTagType
      changed = true
    }
  })
  if (changed)
    emitRows()
}

function findOptionForStandardRow(row, options = []) {
  return options.find(option => String(option.value).toUpperCase() === row.standardStatus)
    || options.find(option => String(option.label || '').trim() === row.standardLabel)
}

function applyOptionToRow(row, option) {
  row.statusValue = String(option.value)
  row.displayName = option.label || row.displayName
  row.tagType = normalizeTagType(option.tagType || row.tagType)
  return row
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
      statusValue: row.statusValue === null || row.statusValue === undefined ? '' : String(row.statusValue),
    })
  }
  return Array.from(byKey.values())
}

function defaultRows() {
  return [
    createStatusRow('DRAFT', '草稿', 'DRAFT', '草稿', 'default', true, true, true),
    createStatusRow('SUBMITTED', '已提交', 'SUBMITTED', '已提交', 'info', false, false, false),
    createStatusRow('IN_PROCESS', '流程中', 'IN_PROCESS', '流程中', 'warning', false, false, false),
    createStatusRow('APPROVED', '已通过', 'APPROVED', '已通过', 'success', false, false, false),
    createStatusRow('REJECTED', '已驳回', 'REJECTED', '已驳回', 'error', true, false, true),
    createStatusRow('CANCELED', '已撤回', 'CANCELED', '已撤回', 'default', true, false, true),
    createStatusRow('CLOSED', '已关闭', 'CLOSED', '已关闭', 'default', false, false, false),
  ]
}

function createStatusRow(standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow) {
  return { standardStatus, standardLabel, statusValue, displayName, tagType, allowEdit, allowDelete, allowStartFlow }
}

function normalizeTagType(value) {
  const text = String(value || '').toLowerCase()
  return DICT_TAG_TYPES.includes(text) ? text : 'default'
}
</script>

<style scoped>
.status-mapping-table {
  display: grid;
  gap: 10px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-source {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
}

.status-source span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-alert {
  font-size: 12px;
}

.status-table {
  display: grid;
  min-width: 820px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.status-header,
.status-row {
  display: grid;
  grid-template-columns: 92px minmax(150px, 1fr) minmax(120px, 0.8fr) 96px 68px 68px 68px;
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

.display-name {
  overflow: hidden;
  min-height: 28px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 26px;
  padding: 0 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .status-mapping-table {
    overflow-x: auto;
  }

  .table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
