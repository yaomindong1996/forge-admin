<template>
  <div class="crud-default-params-editor">
    <div class="default-param-head">
      <div>
        <strong>{{ title }}</strong>
        <span>{{ description }}</span>
      </div>
    </div>

    <div v-for="section in paramSections" :key="section.key" class="default-param-section">
      <div class="default-param-section-head">
        <div>
          <strong>{{ section.label }}</strong>
          <span>{{ section.description }}</span>
        </div>
        <NButton size="tiny" type="primary" secondary attr-type="button" @click.stop.prevent="addParam(section.key)">
          新增
        </NButton>
      </div>

      <div v-if="localRows[section.key]?.length" class="default-param-list">
        <div v-for="(row, idx) in localRows[section.key]" :key="row.id || idx" class="default-param-row">
          <div class="default-param-key-control">
            <NInput
              :value="row.key || ''"
              size="tiny"
              clearable
              placeholder="参数名，可输入自定义字段"
              @update:value="updateParam(section.key, idx, { key: normalizeKey($event) })"
            />
            <NDropdown
              trigger="click"
              :options="fieldMenuOptions"
              :disabled="!fieldMenuOptions.length"
              @select="updateParam(section.key, idx, { key: normalizeKey($event) })"
            >
              <NButton size="tiny" secondary attr-type="button" title="选择表字段">
                字段
              </NButton>
            </NDropdown>
          </div>
          <NSelect
            :value="row.valueType || 'string'"
            :options="valueTypeOptions"
            size="tiny"
            @update:value="updateParam(section.key, idx, { valueType: $event || 'string' })"
          />
          <NInput
            :value="formatValue(row)"
            size="tiny"
            placeholder="参数值"
            @update:value="updateParam(section.key, idx, { value: parseInputValue($event, row.valueType) })"
          />
          <NButton size="tiny" quaternary type="error" attr-type="button" @click.stop.prevent="removeParam(section.key, idx)">
            删
          </NButton>
        </div>
      </div>
      <div v-else class="default-param-empty">
        暂无参数。
      </div>
    </div>
  </div>
</template>

<script setup>
import { NButton, NDropdown, NInput, NSelect } from 'naive-ui'
import { computed, nextTick, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    default: '默认参数',
  },
  description: {
    type: String,
    default: '配置固定随请求或表单提交携带的参数。',
  },
  sections: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const DEFAULT_PARAM_SECTIONS = [
  {
    key: 'publicParams',
    label: '列表请求参数',
    description: '每次列表查询都会携带；GET 进入 URL 参数，POST 进入请求体。',
  },
  {
    key: 'publicQuery',
    label: 'URL 公共参数',
    description: '每次接口请求固定追加到 URL query，适合租户、来源、外部上下文。',
  },
  {
    key: 'formDefaultValues',
    label: '表单默认值',
    description: '打开新增/编辑弹窗时预填，可包含表单外隐藏字段。',
  },
  {
    key: 'submitDefaultParams',
    label: '提交固定参数',
    description: '新增/编辑提交前固定合入，适合后端要求但表单不展示的字段。',
  },
]
const paramSections = computed(() => {
  const sections = props.sections.length ? props.sections : DEFAULT_PARAM_SECTIONS
  return sections
    .filter(section => section?.key)
    .map(section => ({
      key: section.key,
      label: section.label || section.key,
      description: section.description || '',
    }))
})
const paramKeys = computed(() => paramSections.value.map(section => section.key))

const valueTypeOptions = [
  { label: '文本', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: 'JSON', value: 'json' },
]

const localRows = ref(createEmptyRows())
const fieldMenuOptions = computed(() => props.fieldOptions
  .map(option => ({
    label: option.label || option.value,
    key: option.value,
  }))
  .filter(option => option.key))
let syncingLocalEmit = false

watch(
  () => props.modelValue,
  (value) => {
    if (syncingLocalEmit && isSameParamConfig(value || {}, sectionRowsToObject(localRows.value)))
      return
    localRows.value = objectToSectionRows(value || {})
  },
  { immediate: true, deep: true },
)

function createEmptyRows() {
  return paramKeys.value.reduce((result, key) => {
    result[key] = []
    return result
  }, {})
}

function objectToSectionRows(source = {}) {
  return paramKeys.value.reduce((result, key) => {
    const params = isPlainObject(source[key]) ? source[key] : {}
    result[key] = Object.entries(params).map(([paramKey, value]) => ({
      id: `param_${key}_${paramKey}_${Math.random().toString(36).slice(2, 8)}`,
      key: paramKey,
      value,
      valueType: inferValueType(value),
    }))
    return result
  }, createEmptyRows())
}

function sectionRowsToObject(rows = localRows.value) {
  return paramKeys.value.reduce((result, key) => {
    result[key] = rowsToObject(rows[key] || [])
    return result
  }, {})
}

function rowsToObject(rows = []) {
  return rows.reduce((result, row) => {
    const key = normalizeKey(row.key)
    if (!key)
      return result
    result[key] = normalizeTypedValue(row.value, row.valueType)
    return result
  }, {})
}

function emitRows(nextRows) {
  localRows.value = nextRows
  const nextValue = sectionRowsToObject(nextRows)
  if (isSameParamConfig(nextValue, props.modelValue || {}))
    return
  syncingLocalEmit = true
  emit('update:modelValue', nextValue)
  nextTick(() => {
    syncingLocalEmit = false
  })
}

function addParam(sectionKey) {
  emitRows({
    ...localRows.value,
    [sectionKey]: [
      ...(localRows.value[sectionKey] || []),
      {
        id: `param_${sectionKey}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
        key: '',
        valueType: 'string',
        value: '',
      },
    ],
  })
}

function updateParam(sectionKey, idx, patch) {
  const rows = [...(localRows.value[sectionKey] || [])]
  rows[idx] = { ...(rows[idx] || {}), ...patch }
  emitRows({ ...localRows.value, [sectionKey]: rows })
}

function removeParam(sectionKey, idx) {
  const rows = [...(localRows.value[sectionKey] || [])]
  rows.splice(idx, 1)
  emitRows({ ...localRows.value, [sectionKey]: rows })
}

function normalizeKey(value) {
  return String(value || '').trim()
}

function inferValueType(value) {
  if (typeof value === 'number')
    return 'number'
  if (typeof value === 'boolean')
    return 'boolean'
  if (value && typeof value === 'object')
    return 'json'
  return 'string'
}

function formatValue(row = {}) {
  if (row.valueType === 'json' && row.value !== '' && row.value !== undefined) {
    try {
      return JSON.stringify(row.value)
    }
    catch {
      return String(row.value ?? '')
    }
  }
  return String(row.value ?? '')
}

function parseInputValue(value, valueType = 'string') {
  if (valueType === 'number')
    return value === '' ? '' : Number(value)
  if (valueType === 'boolean')
    return value === true || value === 'true' || value === '1' || value === '是'
  if (valueType === 'json') {
    try {
      return value ? JSON.parse(value) : ''
    }
    catch {
      return value
    }
  }
  return value
}

function normalizeTypedValue(value, valueType = 'string') {
  if (valueType === 'number')
    return value === '' || Number.isNaN(Number(value)) ? '' : Number(value)
  if (valueType === 'boolean')
    return value === true || value === 'true' || value === '1'
  if (valueType === 'json' && typeof value === 'string') {
    try {
      return value ? JSON.parse(value) : ''
    }
    catch {
      return value
    }
  }
  return value
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}

function isSameParamConfig(left = {}, right = {}) {
  return JSON.stringify(normalizeParamConfig(left)) === JSON.stringify(normalizeParamConfig(right))
}

function normalizeParamConfig(source = {}) {
  return paramKeys.value.reduce((result, key) => {
    const params = isPlainObject(source[key]) ? source[key] : {}
    result[key] = Object.keys(params).sort().reduce((next, paramKey) => {
      next[paramKey] = params[paramKey]
      return next
    }, {})
    return result
  }, {})
}
</script>

<style scoped>
.crud-default-params-editor {
  display: grid;
  gap: 10px;
  width: 100%;
  min-width: 0;
}

.default-param-head strong,
.default-param-section-head strong {
  display: block;
  color: #0f172a;
  font-size: 12px;
  line-height: 18px;
}

.default-param-head span,
.default-param-section-head span,
.default-param-empty {
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.default-param-section {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.default-param-section-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: flex-start;
}

.default-param-list {
  display: grid;
  gap: 7px;
}

.default-param-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
  min-width: 0;
  padding: 8px;
  border: 1px solid #dbe3ee;
  border-radius: 7px;
  background: #fff;
}

.default-param-row :deep(.n-select),
.default-param-row :deep(.n-input) {
  width: 100%;
  min-width: 0;
}

.default-param-key-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px;
  min-width: 0;
}

.default-param-empty {
  padding: 7px 9px;
  border: 1px dashed #dbe3ee;
  border-radius: 7px;
  background: #fff;
}
</style>
