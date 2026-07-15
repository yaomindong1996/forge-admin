<template>
  <div class="source-picker">
    <div class="source-picker-heading">
      <div>
        <strong>{{ title }}</strong>
        <span>{{ description }}</span>
      </div>
    </div>

    <div class="source-type-switch" role="radiogroup" aria-label="对象来源">
      <button
        type="button"
        role="radio"
        :aria-checked="source.sourceType === 'DATABASE_TABLE'"
        :class="{ active: source.sourceType === 'DATABASE_TABLE' }"
        @click="changeSourceType('DATABASE_TABLE')"
      >
        从数据库选择
      </button>
      <button
        type="button"
        role="radio"
        :aria-checked="source.sourceType === 'EXISTING_OBJECT'"
        :class="{ active: source.sourceType === 'EXISTING_OBJECT' }"
        @click="changeSourceType('EXISTING_OBJECT')"
      >
        选择已有对象
      </button>
    </div>

    <n-grid v-if="source.sourceType === 'DATABASE_TABLE'" cols="1 s:2" :x-gap="10" responsive="screen">
      <n-form-item-gi label="数据源">
        <n-select
          :value="source.datasourceId"
          filterable
          :options="datasourceOptions"
          placeholder="选择运行数据源"
          @update:value="changeDatasource"
        />
      </n-form-item-gi>
      <n-form-item-gi label="数据库表">
        <n-select
          :value="source.tableName"
          filterable
          clearable
          :disabled="!source.datasourceId"
          :loading="loadingTables"
          :options="tableOptions"
          placeholder="选择数据库表"
          @update:value="changeTable"
        />
      </n-form-item-gi>
    </n-grid>

    <n-form-item v-else label="已有业务对象">
      <n-select
        :value="source.objectId"
        filterable
        clearable
        :loading="loadingObjects"
        :options="objectOptions"
        placeholder="选择当前业务域中的对象"
        @update:value="changeObject"
      />
    </n-form-item>

    <div v-if="selectedAssetText" class="selected-asset">
      <n-icon><CheckmarkCircleOutline /></n-icon>
      <span>{{ selectedAssetText }}</span>
      <small v-if="loadingFields">正在读取字段…</small>
      <small v-else-if="fieldOptions.length">
        {{ source.sourceType === 'EXISTING_OBJECT' ? '复用对象并应用模板布局' : `已读取 ${fieldOptions.length} 个字段` }}
      </small>
    </div>
  </div>
</template>

<script setup>
import { CheckmarkCircleOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import {
  businessObjectFields,
  genDatasourceTableColumns,
  genDatasourceTables,
} from '@/api/business-app'

const props = defineProps({
  modelValue: { type: Object, default: () => ({ sourceType: 'DATABASE_TABLE' }) },
  title: { type: String, required: true },
  description: { type: String, default: '选择这个页面区域使用的数据资产。' },
  objectOptions: { type: Array, default: () => [] },
  datasourceOptions: { type: Array, default: () => [] },
  loadingObjects: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'update:fieldOptions'])
const tables = ref([])
const fieldOptions = ref([])
const loadingTables = ref(false)
const loadingFields = ref(false)
let tableRequestToken = 0
let fieldRequestToken = 0

const source = computed(() => ({ sourceType: 'DATABASE_TABLE', ...(props.modelValue || {}) }))
const tableOptions = computed(() => tables.value.map(item => ({
  label: item.tableComment ? `${item.tableName}（${item.tableComment}）` : item.tableName,
  value: item.tableName,
})))
const selectedAssetText = computed(() => {
  if (source.value.sourceType === 'EXISTING_OBJECT')
    return props.objectOptions.find(item => String(item.value) === String(source.value.objectId))?.label || ''
  return tableOptions.value.find(item => item.value === source.value.tableName)?.label || ''
})

watch(() => [source.value.sourceType, source.value.datasourceId], async ([sourceType, datasourceId]) => {
  if (sourceType !== 'DATABASE_TABLE' || !datasourceId) {
    tables.value = []
    return
  }
  await loadTables(datasourceId)
}, { immediate: true })

watch(() => [source.value.sourceType, source.value.objectId, source.value.datasourceId, source.value.tableName], async () => {
  await loadFields()
}, { immediate: true })

watch(() => props.datasourceOptions, (options) => {
  if (source.value.sourceType !== 'DATABASE_TABLE' || source.value.datasourceId || !options?.length)
    return
  updateSource({ datasourceId: options[0].value, tableName: null, objectId: null })
}, { immediate: true })

function changeSourceType(sourceType) {
  fieldOptions.value = []
  emit('update:fieldOptions', [])
  updateSource({
    sourceType,
    objectId: null,
    datasourceId: sourceType === 'DATABASE_TABLE' ? props.datasourceOptions[0]?.value || null : null,
    tableName: null,
  })
}

function changeDatasource(datasourceId) {
  updateSource({ datasourceId, tableName: null, objectId: null })
}

function changeTable(tableName) {
  updateSource({ tableName, objectId: null })
}

function changeObject(objectId) {
  updateSource({ objectId, datasourceId: null, tableName: null })
}

function updateSource(patch) {
  emit('update:modelValue', { ...source.value, ...patch })
}

async function loadTables(datasourceId) {
  const requestToken = ++tableRequestToken
  loadingTables.value = true
  try {
    const response = await genDatasourceTables(datasourceId)
    if (requestToken === tableRequestToken)
      tables.value = response.data || []
  }
  catch {
    if (requestToken === tableRequestToken)
      tables.value = []
  }
  finally {
    if (requestToken === tableRequestToken)
      loadingTables.value = false
  }
}

async function loadFields() {
  const requestToken = ++fieldRequestToken
  const current = source.value
  fieldOptions.value = []
  emit('update:fieldOptions', [])
  if (current.sourceType === 'EXISTING_OBJECT' && !current.objectId)
    return
  if (current.sourceType === 'DATABASE_TABLE' && (!current.datasourceId || !current.tableName))
    return
  loadingFields.value = true
  try {
    const response = current.sourceType === 'EXISTING_OBJECT'
      ? await businessObjectFields(current.objectId)
      : await genDatasourceTableColumns(current.datasourceId, current.tableName)
    if (requestToken !== fieldRequestToken)
      return
    const options = (response.data || []).map(normalizeFieldOption).filter(item => item.value)
    fieldOptions.value = options
    emit('update:fieldOptions', options)
  }
  catch {
    if (requestToken === fieldRequestToken) {
      fieldOptions.value = []
      emit('update:fieldOptions', [])
    }
  }
  finally {
    if (requestToken === fieldRequestToken)
      loadingFields.value = false
  }
}

function normalizeFieldOption(field) {
  const fieldCode = field.fieldCode || normalizeFieldCode(field.javaField || field.columnName)
  const label = field.fieldName || field.columnComment || field.columnName || fieldCode
  return {
    label: `${label} · ${fieldCode}`,
    value: fieldCode,
    isPrimaryKey: Number(field.isPk) === 1 || fieldCode === 'id',
    systemField: Boolean(field.systemField),
    dataType: field.dataType || field.columnType || field.javaType || '',
  }
}

function normalizeFieldCode(value) {
  const normalized = String(value || '')
    .trim()
    .replace(/[^a-zA-Z0-9_]/g, '_')
    .replace(/_+/g, '_')
  if (!normalized)
    return ''
  if (!normalized.includes('_'))
    return normalized.charAt(0).toLowerCase() + normalized.slice(1)
  const parts = normalized.toLowerCase().split('_').filter(Boolean)
  return parts.map((part, index) => (
    index === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)
  )).join('')
}
</script>

<style scoped>
.source-picker {
  box-sizing: border-box;
  display: grid;
  min-width: 0;
  overflow: hidden;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 8px;
  background: var(--n-color, #fff);
}

.source-picker-heading {
  display: block;
  min-width: 0;
}

.source-picker-heading > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.source-picker-heading strong {
  color: var(--n-text-color);
  font-size: 13px;
}

.source-type-switch {
  box-sizing: border-box;
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3px;
  padding: 3px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 7px;
  background: var(--n-color-embedded, #f7f8fa);
}

.source-type-switch button {
  box-sizing: border-box;
  min-width: 0;
  min-height: 32px;
  overflow: hidden;
  padding: 0 10px;
  cursor: pointer;
  border: 0;
  border-radius: 5px;
  color: var(--n-text-color-2);
  background: transparent;
  font: inherit;
  font-size: 12px;
  line-height: 32px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-type-switch button:hover {
  color: var(--n-text-color);
  background: color-mix(in srgb, var(--n-color, #fff) 72%, transparent);
}

.source-type-switch button.active {
  color: var(--n-primary-color);
  background: var(--n-color, #fff);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--n-primary-color) 42%, var(--n-border-color, #e5e7eb));
  font-weight: 600;
}

.source-type-switch button:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--n-primary-color) 50%, transparent);
  outline-offset: 1px;
}

.source-picker-heading span,
.selected-asset small {
  color: var(--n-text-color-3);
  font-size: 11px;
  line-height: 1.5;
}

.selected-asset {
  display: flex;
  min-width: 0;
  gap: 6px;
  align-items: center;
  color: var(--n-text-color-2);
  font-size: 12px;
}

.selected-asset .n-icon {
  flex: 0 0 auto;
  color: var(--n-primary-color);
}

.selected-asset span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-asset small {
  flex: 0 0 auto;
  margin-left: auto;
}

@media (max-width: 420px) {
  .source-type-switch button {
    padding: 0 6px;
    font-size: 11px;
  }
}
</style>
