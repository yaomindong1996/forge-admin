<template>
  <div class="json-field-editor">
    <div class="jfe-toolbar">
      <NButton size="tiny" quaternary type="primary" @click="toggleMode">
        {{ mode === 'struct' ? '切换为 JSON 编辑' : '切换为表格编辑' }}
      </NButton>
      <span v-if="hint" class="jfe-hint">{{ hint }}</span>
    </div>

    <template v-if="mode === 'struct'">
      <div class="jfe-rows">
        <div v-for="(row, index) in rows" :key="row._id" class="jfe-card">
          <div class="jfe-card__head">
            <span>{{ row.key || `参数 ${index + 1}` }}</span>
            <NButton
              quaternary
              type="error"
              size="tiny"
              :disabled="rows.length === 1"
              @click="removeRow(index)"
            >
              删除
            </NButton>
          </div>
          <label class="jfe-field">
            <span>参数名</span>
            <NInput v-model:value="row.key" size="small" placeholder="如 userId、keyword" />
          </label>
          <label class="jfe-field">
            <span>类型</span>
            <NSelect v-model:value="row.type" size="small" :options="typeOptions" />
          </label>
          <label class="jfe-field">
            <span>值</span>
            <NInput
              v-if="row.type === 'string'"
              v-model:value="row.textValue"
              size="small"
              :placeholder="valuePlaceholder"
            />
            <NInputNumber
              v-else-if="row.type === 'number'"
              v-model:value="row.numberValue"
              size="small"
              placeholder="数值"
              class="jfe-number"
            />
            <div v-else class="jfe-bool">
              <NSwitch v-model:value="row.boolValue" size="small" />
              <span>{{ row.boolValue ? '是' : '否' }}</span>
            </div>
          </label>
        </div>
      </div>
      <NButton size="small" dashed type="primary" @click="addRow">
        添加字段
      </NButton>
    </template>

    <div v-else class="jfe-json">
      <div class="jfe-json-tools">
        <NButton size="tiny" quaternary @click="formatJsonText">
          格式化
        </NButton>
        <span v-if="jsonError" class="jfe-error">{{ jsonError }}</span>
        <span v-else class="jfe-hint">包含嵌套结构时使用 JSON 模式编辑</span>
      </div>
      <NInput
        v-model:value="jsonText"
        type="textarea"
        :rows="textareaRows"
        class="jfe-textarea"
        :placeholder="jsonPlaceholder"
      />
    </div>
  </div>
</template>

<script setup>
import { NButton, NInput, NInputNumber, NSelect, NSwitch } from 'naive-ui'
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  hint: { type: String, default: '' },
  textareaRows: { type: Number, default: 5 },
  valuePlaceholder: { type: String, default: '值，可用 {param} 占位' },
  jsonPlaceholder: { type: String, default: 'JSON 对象，如 {"userId": 1001}' },
})

const emit = defineEmits(['update:modelValue'])

const typeOptions = [
  { label: '文本', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
]

const mode = ref('struct')
const rows = ref([])
const jsonText = ref('')
const jsonError = ref('')
let rowSeq = 0

watch(() => props.modelValue, (value) => {
  syncFromValue(value)
}, { immediate: true })

watch([mode, rows, jsonText], () => {
  emitValue()
}, { deep: true })

function nextId() {
  rowSeq += 1
  return `jfe-${rowSeq}`
}

function createEmptyRow() {
  return { _id: nextId(), key: '', type: 'string', textValue: '', numberValue: null, boolValue: false }
}

function isFlatObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    && Object.values(value).every(item => ['string', 'number', 'boolean'].includes(typeof item))
}

function parseIncoming(value) {
  if (value == null || String(value).trim() === '')
    return null
  try {
    return JSON.parse(value)
  }
  catch {
    return undefined
  }
}

function syncFromValue(value) {
  const parsed = parseIncoming(value)
  if (parsed === undefined) {
    mode.value = 'json'
    jsonText.value = typeof value === 'string' ? value : ''
    jsonError.value = jsonText.value ? '当前内容不是合法 JSON' : ''
    return
  }
  if (parsed === null) {
    mode.value = 'struct'
    if (!rows.value.some(row => row.key))
      rows.value = [createEmptyRow()]
    return
  }
  if (isFlatObject(parsed)) {
    // 编辑回显时保持当前编辑模式，避免输入过程中视图来回切换
    if (mode.value === 'json' && jsonText.value.trim() === String(value ?? '').trim())
      return
    if (!(mode.value === 'struct' && rowsEqual(parsed))) {
      mode.value = 'struct'
      rows.value = rowsFromObject(parsed)
    }
    jsonError.value = ''
    return
  }
  mode.value = 'json'
  jsonText.value = typeof value === 'string' && value.trim() ? value : JSON.stringify(parsed, null, 2)
  jsonError.value = ''
}

function rowsFromObject(parsed) {
  const next = Object.entries(parsed).map(([key, itemValue]) => ({
    _id: nextId(),
    key,
    type: typeof itemValue,
    textValue: typeof itemValue === 'string' ? itemValue : String(itemValue),
    numberValue: typeof itemValue === 'number' ? itemValue : null,
    boolValue: itemValue === true,
  }))
  return next.length ? next : [createEmptyRow()]
}

function rowsEqual(parsed) {
  return rows.value.length && JSON.stringify(serializeRows(rows.value)) === JSON.stringify(parsed)
}

function serializeRows(list) {
  const result = {}
  list.filter(row => row.key).forEach((row) => {
    if (row.type === 'number')
      result[row.key] = row.numberValue ?? null
    else if (row.type === 'boolean')
      result[row.key] = row.boolValue
    else
      result[row.key] = row.textValue ?? ''
  })
  return result
}

function emitValue() {
  if (mode.value === 'struct') {
    const hasContent = rows.value.some(row => row.key)
    const next = hasContent ? JSON.stringify(serializeRows(rows.value)) : ''
    if (next === normalizeIncoming(props.modelValue))
      return
    emit('update:modelValue', next)
    return
  }
  const trimmed = (jsonText.value ?? '').trim()
  if (!trimmed) {
    jsonError.value = ''
    if (normalizeIncoming(props.modelValue) === '')
      return
    emit('update:modelValue', '')
    return
  }
  try {
    JSON.parse(trimmed)
    jsonError.value = ''
  }
  catch {
    jsonError.value = 'JSON 格式有误，保存前请修正'
  }
  if (trimmed === String(props.modelValue ?? '').trim())
    return
  emit('update:modelValue', trimmed)
}

function normalizeIncoming(value) {
  if (value == null || String(value).trim() === '')
    return ''
  try {
    return JSON.stringify(JSON.parse(value))
  }
  catch {
    return String(value).trim()
  }
}

function toggleMode() {
  if (mode.value === 'struct') {
    const hasContent = rows.value.some(row => row.key)
    jsonText.value = hasContent ? JSON.stringify(serializeRows(rows.value), null, 2) : ''
    jsonError.value = ''
    mode.value = 'json'
    return
  }
  const parsed = parseIncoming(jsonText.value)
  if (parsed === undefined || !isFlatObject(parsed)) {
    jsonError.value = parsed === undefined ? 'JSON 格式有误，无法切换' : '包含嵌套结构，请继续使用 JSON 模式'
    return
  }
  syncFromValue(jsonText.value)
}

function formatJsonText() {
  try {
    jsonText.value = JSON.stringify(JSON.parse(jsonText.value), null, 2)
    jsonError.value = ''
  }
  catch {
    jsonError.value = 'JSON 格式有误，无法格式化'
  }
}

function addRow() {
  rows.value.push(createEmptyRow())
}

function removeRow(index) {
  rows.value.splice(index, 1)
  if (!rows.value.length)
    addRow()
}
</script>

<style scoped>
.json-field-editor {
  width: 100%;
  min-width: 0;
  display: grid;
  gap: 8px;
}

.jfe-toolbar,
.jfe-json-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.jfe-hint {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.jfe-error {
  color: var(--error-color);
  font-size: 12px;
  line-height: 18px;
}

.jfe-rows {
  display: grid;
  gap: 10px;
}

.jfe-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary, #f8fafc);
}

.jfe-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.jfe-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.jfe-field > span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.jfe-row > :deep(.n-input),
.jfe-row > :deep(.n-select),
.jfe-row > :deep(.n-input-number) {
  min-width: 0;
}

.jfe-bool {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.jfe-textarea :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}
</style>
