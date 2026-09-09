<template>
  <div class="external-config-editor" :class="`is-${mode}`">
    <div class="editor-list">
      <div
        v-for="(row, index) in rows"
        :key="row._id"
        class="editor-card editor-row"
      >
        <div class="editor-card__head">
          <span>{{ rowTitle(row, index) }}</span>
          <NButton
            quaternary
            type="error"
            size="tiny"
            :disabled="readonly || rows.length === 1"
            @click="removeRow(index)"
          >
            删除
          </NButton>
        </div>

        <template v-if="mode === 'key-value'">
          <label class="editor-field">
            <span>名称</span>
            <NInput v-model:value="row.key" size="small" placeholder="如 X-Request-Id、appKey" :disabled="readonly" />
          </label>
          <label class="editor-field">
            <span>值</span>
            <NInput v-model:value="row.value" size="small" placeholder="对应的值" :disabled="readonly" />
          </label>
        </template>

        <template v-else>
          <label class="editor-field">
            <span>页面参数</span>
            <NInput v-model:value="row.source" size="small" placeholder="如 keyword" :disabled="readonly" />
          </label>
          <label class="editor-field">
            <span>接口参数</span>
            <NInput v-model:value="row.target" size="small" placeholder="如 searchKey" :disabled="readonly" />
          </label>
          <label class="editor-field">
            <span>默认值（可选）</span>
            <NInput v-model:value="row.defaultValue" size="small" placeholder="未传时使用的值" :disabled="readonly" />
          </label>
        </template>
      </div>
    </div>

    <div class="editor-footer">
      <NButton v-if="!readonly" size="small" dashed type="primary" @click="addRow">
        添加一项
      </NButton>
      <span v-if="hint" class="editor-hint">{{ hint }}</span>
    </div>
  </div>
</template>

<script setup>
import { NButton, NInput } from 'naive-ui'
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Array, Object], default: '' },
  mode: { type: String, default: 'key-value' },
  readonly: { type: Boolean, default: false },
  hint: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])
const rows = ref([])
let rowSeq = 0

watch(() => [props.modelValue, props.mode], () => {
  syncFromValue(props.modelValue)
}, { immediate: true })

watch(rows, () => {
  const next = serialize(rows.value)
  if (next === normalizeIncoming(props.modelValue))
    return
  emit('update:modelValue', next)
}, { deep: true })

function nextId() {
  rowSeq += 1
  return `row-${rowSeq}`
}

function createEmptyRow() {
  if (props.mode === 'mapping')
    return { _id: nextId(), source: '', target: '', defaultValue: '' }
  return { _id: nextId(), key: '', value: '' }
}

function rowTitle(row, index) {
  if (props.mode === 'mapping')
    return row.source || row.target || `映射 ${index + 1}`
  return row.key || `第 ${index + 1} 项`
}

function normalizeIncoming(value) {
  if (value == null || value === '')
    return emptyPayload()
  if (typeof value === 'string') {
    const text = value.trim()
    return text || emptyPayload()
  }
  try {
    return JSON.stringify(value)
  }
  catch {
    return emptyPayload()
  }
}

function emptyPayload() {
  return '{}'
}

function parseValue(value) {
  let parsed = value
  if (typeof value === 'string') {
    try {
      parsed = value.trim() ? JSON.parse(value) : undefined
    }
    catch {
      parsed = undefined
    }
  }
  if (props.mode === 'mapping') {
    const list = parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? Object.entries(parsed).map(([source, config]) => typeof config === 'object' && config !== null
          ? { _id: nextId(), source: String(config.source || source), target: String(config.target || source), defaultValue: String(config.defaultValue ?? '') }
          : { _id: nextId(), source, target: String(config ?? ''), defaultValue: '' })
      : []
    return list.length ? list : [createEmptyRow()]
  }
  const entries = parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? Object.entries(parsed) : []
  return entries.length
    ? entries.map(([key, itemValue]) => ({ _id: nextId(), key, value: stringify(itemValue) }))
    : [createEmptyRow()]
}

function stringify(value) {
  return typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value ?? '')
}

function serialize(list) {
  if (!Array.isArray(list) || !list.length)
    return emptyPayload()
  const result = {}
  if (props.mode === 'mapping') {
    list.filter(row => row.source || row.target).forEach((row) => {
      if (!row.source)
        return
      result[row.source] = row.defaultValue
        ? { target: row.target || row.source, defaultValue: row.defaultValue }
        : (row.target || row.source)
    })
  }
  else {
    list.filter(row => row.key).forEach((row) => {
      result[row.key] = row.value
    })
  }
  return JSON.stringify(result)
}

function syncFromValue(value) {
  const incoming = normalizeIncoming(value)
  if (rows.value.length && serialize(rows.value) === incoming)
    return
  rows.value = parseValue(value)
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
.external-config-editor {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.editor-list {
  display: grid;
  gap: 10px;
}

.editor-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary, #f8fafc);
}

.editor-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.editor-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.editor-field > span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.editor-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
}

.editor-hint {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}
</style>
