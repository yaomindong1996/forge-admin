<template>
  <div class="dataset-param-editor">
    <div class="editor-toolbar">
      <n-text depth="3">
        {{ helperText }}
      </n-text>

      <n-tag v-if="!isTableMode" size="small" :bordered="false" type="info">
        已识别 {{ sqlParamNames.length }} 个 SQL 参数
      </n-tag>
    </div>

    <div v-if="isTableMode && !tableReady" class="editor-alert">
      <n-alert type="info" :show-icon="false">
        先选择数据连接和数据表，再定义这个数据集支持哪些筛选条件。
      </n-alert>
    </div>

    <div v-if="rows.length > 0" class="editor-body">
      <div class="editor-head" :style="gridStyle">
        <span>参数名</span>
        <span>类型</span>
        <span v-if="isTableMode">匹配方式</span>
        <span v-if="isTableMode">数据表字段</span>
        <span>默认值</span>
        <span>必须</span>
        <span>说明</span>
        <span>操作</span>
      </div>

      <div
        v-for="(row, index) in rows"
        :key="row.__key"
        class="editor-row-card"
      >
        <div class="editor-row" :style="gridStyle">
          <div class="editor-cell">
            <div class="editor-cell__label">
              参数名
            </div>
            <n-input
              v-model:value="row.paramName"
              :disabled="readonly"
              clearable
              placeholder="如 start_time"
            />
          </div>

          <div class="editor-cell">
            <div class="editor-cell__label">
              类型
            </div>
            <n-select
              v-model:value="row.dataType"
              :disabled="readonly"
              :options="dataTypeOptions"
              clearable
              placeholder="选择参数类型"
            />
          </div>

          <div v-if="isTableMode" class="editor-cell">
            <div class="editor-cell__label">
              匹配方式
            </div>
            <n-select
              :value="row.operator"
              :disabled="readonly"
              :options="operatorOptions"
              placeholder="选择操作符"
              @update:value="value => handleOperatorChange(index, value)"
            />
          </div>

          <div v-if="isTableMode" class="editor-cell">
            <div class="editor-cell__label">
              数据表字段
            </div>
            <n-select
              :value="row.fieldName"
              :options="fieldOptions"
              :loading="fieldLoading"
              :disabled="readonly || !tableReady"
              clearable
              filterable
              placeholder="选择数据表字段"
              @update:value="value => handleFieldChange(index, value)"
            />
          </div>

          <div class="editor-cell">
            <div class="editor-cell__label">
              默认值
            </div>
            <n-input
              :value="formatDefaultValue(row.defaultValue)"
              :disabled="readonly"
              clearable
              :placeholder="getDefaultValuePlaceholder(row.dataType)"
              @update:value="value => handleDefaultValueChange(index, value)"
            />
          </div>

          <div class="editor-cell editor-cell--switch">
            <div class="editor-cell__label">
              必须
            </div>
            <n-switch v-model:value="row.required" :disabled="readonly" />
          </div>

          <div class="editor-cell">
            <div class="editor-cell__label">
              说明
            </div>
            <n-input
              v-model:value="row.label"
              :disabled="readonly"
              clearable
              :placeholder="isTableMode ? '如 开始时间（含）' : '如 订单状态'"
            />
          </div>

          <div class="editor-cell editor-cell--action">
            <div class="editor-cell__label">
              操作
            </div>
            <n-button quaternary circle type="error" :disabled="readonly" @click="handleRemoveRow(index)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="editor-empty">
      <n-empty :description="emptyDescription" />
    </div>

    <div class="editor-action-bar">
      <n-space align="center" :size="12">
        <n-button text type="primary" :disabled="readonly" @click="handleAddRow">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          添加参数
        </n-button>
        <n-button v-if="!isTableMode" text type="primary" :disabled="readonly" @click="handleImportSqlParams">
          从 SQL 识别
        </n-button>
      </n-space>
    </div>

    <div class="editor-footer">
      <n-text depth="3">
        {{ footerText }}
      </n-text>
    </div>
  </div>
</template>

<script setup>
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import { getDataConnectionFields } from '@/api/data/connection'

defineOptions({ name: 'DatasetParamSchemaEditor' })

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  datasetType: {
    type: String,
    default: 'TABLE',
  },
  connectionId: {
    type: [Number, String],
    default: null,
  },
  tableName: {
    type: String,
    default: '',
  },
  sqlText: {
    type: String,
    default: '',
  },
  readonly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

const dataTypeOptions = [
  { label: '字符串', value: 'STRING' },
  { label: '数值', value: 'NUMBER' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '布尔', value: 'BOOLEAN' },
]

const operatorOptions = [
  { label: '=', value: '=' },
  { label: '!=', value: '!=' },
  { label: '>', value: '>' },
  { label: '>=', value: '>=' },
  { label: '<', value: '<' },
  { label: '<=', value: '<=' },
  { label: 'LIKE', value: 'LIKE' },
]

const sqlNamedParamPattern = /:([a-z_]\w*)/gi

const rows = ref([])
const fieldOptions = ref([])
const fieldMetaMap = ref({})
const fieldLoading = ref(false)
const fieldCache = new Map()
const lastSyncedRows = ref('[]')
const currentFieldRequestKey = ref('')

let rowSeed = 0

const isTableMode = computed(() => props.datasetType === 'TABLE')
const tableReady = computed(() => Boolean(props.connectionId && props.tableName))
const sqlParamNames = computed(() => extractSqlParamNames(props.sqlText))

const gridStyle = computed(() => ({
  gridTemplateColumns: isTableMode.value
    ? '1.1fr 0.9fr 0.8fr 1.4fr 1fr 0.6fr 1.2fr 52px'
    : '1.1fr 0.9fr 1fr 0.6fr 1.4fr 52px',
}))

const helperText = computed(() => {
  if (isTableMode.value) {
    return '在数据集里定义可筛选条件，报表侧只需要把筛选控件绑定到这些条件即可。'
  }
  return 'SQL 条件以手工维护为主，也可以从当前 SQL 中识别 :paramName 作为辅助。'
})

const emptyDescription = computed(() => {
  if (isTableMode.value) {
    return '暂未定义查询条件'
  }
  return '可以先手动新增条件，也可以点击“从 SQL 识别”。'
})

const footerText = computed(() => {
  if (isTableMode.value) {
    if (!tableReady.value) {
      return '选择数据表后即可从真实字段里定义条件，适合复用在多个报表、AI 查询和问答场景。'
    }
    if (fieldLoading.value) {
      return '正在读取数据表字段，请稍候。'
    }
    return `当前数据表共 ${fieldOptions.value.length} 个可选字段。相同字段可以定义多个条件，例如开始时间和结束时间。`
  }
  return sqlParamNames.value.length
    ? 'SQL 条件手工维护，不会因为你修改 SQL 文本而自动追加重复行。'
    : '当前 SQL 还没有命名参数；你可以先定义条件，后续再把 :条件参数 接入 SQL。'
})

watch(
  () => props.modelValue,
  (value) => {
    const normalizedRows = normalizeRows(value)
    syncRows(normalizedRows)
  },
  { immediate: true, deep: true },
)

watch(
  rows,
  (value) => {
    const serialized = serializeRows(value)
    if (serialized === lastSyncedRows.value) {
      return
    }
    lastSyncedRows.value = serialized
    emit('update:modelValue', toOutputRows(value))
  },
  { deep: true },
)

watch(
  () => [props.datasetType, props.connectionId, props.tableName],
  async ([datasetType, connectionId, tableName]) => {
    if (datasetType !== 'TABLE') {
      fieldOptions.value = []
      fieldMetaMap.value = {}
      fieldLoading.value = false
      currentFieldRequestKey.value = ''
      return
    }

    if (!connectionId || !tableName) {
      fieldOptions.value = []
      fieldMetaMap.value = {}
      clearInvalidFieldMappings()
      return
    }

    const cacheKey = `${connectionId}:${tableName}`
    if (fieldCache.has(cacheKey)) {
      const cached = fieldCache.get(cacheKey)
      fieldOptions.value = cached.options
      fieldMetaMap.value = cached.meta
      clearInvalidFieldMappings()
      return
    }

    currentFieldRequestKey.value = cacheKey
    fieldLoading.value = true
    try {
      const res = await getDataConnectionFields(connectionId, tableName)
      if (currentFieldRequestKey.value !== cacheKey) {
        return
      }

      if (res.code === 200 && Array.isArray(res.data)) {
        const options = res.data.map(field => ({
          label: field.columnComment
            ? `${field.columnComment} (${field.columnName})`
            : field.columnName,
          value: field.columnName,
        }))
        const meta = Object.fromEntries(res.data.map(field => [field.columnName, field]))
        fieldCache.set(cacheKey, { options, meta })
        fieldOptions.value = options
        fieldMetaMap.value = meta
      }
      else {
        fieldOptions.value = []
        fieldMetaMap.value = {}
      }
      clearInvalidFieldMappings()
    }
    catch (error) {
      if (currentFieldRequestKey.value === cacheKey) {
        fieldOptions.value = []
        fieldMetaMap.value = {}
        clearInvalidFieldMappings()
      }
      window.$message?.error(error?.message || '加载字段列表失败')
    }
    finally {
      if (currentFieldRequestKey.value === cacheKey) {
        fieldLoading.value = false
      }
    }
  },
  { immediate: true },
)

function syncRows(nextRows) {
  const serialized = serializeRows(nextRows)
  if (serialized === lastSyncedRows.value) {
    return
  }
  rows.value = nextRows
  lastSyncedRows.value = serialized
}

function handleAddRow() {
  rows.value = [...rows.value, createRow({
    operator: '=',
  })]
}

function handleImportSqlParams() {
  if (isTableMode.value) {
    return
  }
  const detectedParamNames = sqlParamNames.value
  if (!detectedParamNames.length) {
    window.$message?.warning('当前 SQL 里没有识别到命名参数')
    return
  }

  const existingNames = new Set(rows.value.map(row => row.paramName).filter(Boolean))
  const importedRows = detectedParamNames
    .filter(paramName => !existingNames.has(paramName))
    .map(paramName => createRow({
      paramName,
      operator: '=',
    }))

  if (!importedRows.length) {
    window.$message?.info('SQL 参数都已经存在，无需重复识别')
    return
  }

  rows.value = [...rows.value, ...importedRows]
  window.$message?.success(`已导入 ${importedRows.length} 个 SQL 参数`)
}

function handleRemoveRow(index) {
  rows.value = rows.value.filter((_, rowIndex) => rowIndex !== index)
}

function handleOperatorChange(index, value) {
  rows.value = rows.value.map((row, rowIndex) => {
    if (rowIndex !== index) {
      return row
    }
    const nextOperator = value || '='
    const nextRow = {
      ...row,
      operator: nextOperator,
    }
    if (!nextRow.paramName && nextRow.fieldName) {
      nextRow.paramName = buildSuggestedParamName(nextRow.fieldName, nextOperator)
    }
    return nextRow
  })
}

function handleFieldChange(index, value) {
  rows.value = rows.value.map((row, rowIndex) => {
    if (rowIndex !== index) {
      return row
    }
    const nextFieldName = value || ''
    const nextRow = {
      ...row,
      fieldName: nextFieldName,
    }
    const fieldMeta = fieldMetaMap.value[nextFieldName]
    if (!nextRow.label) {
      nextRow.label = fieldMeta?.columnComment || nextFieldName
    }
    if (!nextRow.paramName && nextFieldName) {
      nextRow.paramName = buildSuggestedParamName(nextFieldName, nextRow.operator)
    }
    if ((!nextRow.dataType || nextRow.dataType === 'STRING') && fieldMeta?.columnType) {
      nextRow.dataType = mapColumnTypeToDataType(fieldMeta.columnType)
    }
    return nextRow
  })
}

function handleDefaultValueChange(index, value) {
  rows.value = rows.value.map((row, rowIndex) => {
    if (rowIndex !== index) {
      return row
    }
    return {
      ...row,
      defaultValue: value === '' ? null : value,
    }
  })
}

function clearInvalidFieldMappings() {
  if (!isTableMode.value || rows.value.length === 0) {
    return
  }

  const validFields = new Set(fieldOptions.value.map(item => item.value))
  let changed = false
  const nextRows = rows.value.map((row) => {
    if (!row.fieldName || validFields.has(row.fieldName)) {
      return row
    }
    changed = true
    return {
      ...row,
      fieldName: '',
    }
  })

  if (changed) {
    rows.value = nextRows
  }
}

function createRow(row = {}) {
  rowSeed += 1
  return {
    __key: row.__key || `param-row-${Date.now()}-${rowSeed}`,
    paramName: row.paramName || '',
    label: row.label || '',
    dataType: row.dataType || 'STRING',
    required: row.required === true,
    defaultValue: row.defaultValue ?? null,
    operator: row.operator || '=',
    fieldName: row.fieldName || '',
  }
}

function normalizeRows(value) {
  if (!Array.isArray(value)) {
    return []
  }
  return value.map(createRow)
}

function toOutputRows(value) {
  if (!Array.isArray(value)) {
    return []
  }
  return value.map((row) => {
    const label = typeof row.label === 'string' ? row.label.trim() : row.label
    const fieldName = typeof row.fieldName === 'string' ? row.fieldName.trim() : row.fieldName
    return {
      paramName: typeof row.paramName === 'string' ? row.paramName.trim() : row.paramName,
      label: label || null,
      dataType: row.dataType || 'STRING',
      required: row.required === true,
      defaultValue: row.defaultValue ?? null,
      operator: row.operator || '=',
      fieldName: fieldName || null,
    }
  })
}

function serializeRows(value) {
  return JSON.stringify(toOutputRows(value))
}

function extractSqlParamNames(sqlText) {
  if (!sqlText) {
    return []
  }
  const names = []
  const seen = new Set()
  const matcher = new RegExp(sqlNamedParamPattern)
  let match = matcher.exec(sqlText)
  while (match) {
    const paramName = match[1]
    if (paramName && !seen.has(paramName)) {
      seen.add(paramName)
      names.push(paramName)
    }
    match = matcher.exec(sqlText)
  }
  return names
}

function buildSuggestedParamName(fieldName, operator) {
  const baseName = toCamelCase(fieldName)
  if (!baseName) {
    return ''
  }
  if (operator === '>=' || operator === '>') {
    return `${baseName}Start`
  }
  if (operator === '<=' || operator === '<') {
    return `${baseName}End`
  }
  if (operator === 'LIKE') {
    return `${baseName}Keyword`
  }
  if (operator === '!=') {
    return `${baseName}Exclude`
  }
  return baseName
}

function toCamelCase(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[_-]([a-z0-9])/g, (_, char) => char.toUpperCase())
}

function mapColumnTypeToDataType(columnType) {
  const normalized = String(columnType || '').toUpperCase()
  if (!normalized) {
    return 'STRING'
  }
  if (normalized.includes('DATE') && !normalized.includes('TIME')) {
    return 'DATE'
  }
  if (normalized.includes('TIME')) {
    return 'DATETIME'
  }
  if (
    normalized.includes('INT')
    || normalized.includes('DECIMAL')
    || normalized.includes('NUMERIC')
    || normalized.includes('DOUBLE')
    || normalized.includes('FLOAT')
    || normalized.includes('LONG')
  ) {
    return 'NUMBER'
  }
  if (normalized.includes('BOOL') || normalized.includes('BIT')) {
    return 'BOOLEAN'
  }
  return 'STRING'
}

function formatDefaultValue(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value)
}

function getDefaultValuePlaceholder(dataType) {
  if (dataType === 'NUMBER') {
    return '如 0'
  }
  if (dataType === 'DATE') {
    return '如 2026-05-12'
  }
  if (dataType === 'DATETIME') {
    return '如 2026-05-12 00:00:00'
  }
  if (dataType === 'BOOLEAN') {
    return '如 true / false'
  }
  return '可选，未传参时使用'
}
</script>

<style scoped>
.dataset-param-editor {
  width: 100%;
  padding: 0;
  background: var(--bg-primary);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.editor-alert {
  margin-bottom: 12px;
}

.editor-body {
  display: flex;
  flex-direction: column;
  overflow-x: auto;
  border: 1px solid var(--border-light);
  border-radius: 4px;
}

.editor-head {
  display: grid;
  gap: 8px;
  min-width: 760px;
  padding: 10px 12px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
}

.editor-row-card {
  min-width: 760px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
  transition: background 0.18s ease;
}

.editor-row-card:hover {
  background: var(--bg-tertiary);
}

.editor-row-card:last-child {
  border-bottom: 0;
}

.editor-row {
  display: grid;
  gap: 8px;
  align-items: start;
}

.editor-cell {
  min-width: 0;
}

.editor-cell__label {
  display: none;
  margin-bottom: 6px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.editor-cell--switch {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 40px;
}

.editor-cell--action {
  display: flex;
  align-items: center;
  justify-content: center;
}

.editor-empty {
  padding: 24px 0;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: var(--bg-primary);
}

.editor-action-bar {
  display: flex;
  justify-content: flex-start;
  margin-top: 12px;
}

.editor-footer {
  margin-top: 12px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.dataset-param-editor :deep(.n-input),
.dataset-param-editor :deep(.n-base-selection) {
  --n-border-radius: 4px !important;
  --n-border: 1px solid #dcdfe6 !important;
  --n-border-hover: 1px solid #86909c !important;
  --n-border-focus: 1px solid #1677ff !important;
  --n-box-shadow-focus: 0 0 0 2px rgb(22 119 255 / 20%) !important;
}

.dataset-param-editor :deep(.n-switch.n-switch--active) {
  --n-rail-color-active: #1677ff !important;
}

@media (max-width: 768px) {
  .editor-head {
    display: none;
  }

  .editor-row {
    grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
  }

  .editor-row-card {
    min-width: 0;
    border: 1px solid var(--border-light);
    border-radius: 4px;
  }

  .editor-cell__label {
    display: block;
  }

  .editor-cell--action {
    align-items: flex-end;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .dataset-param-editor {
    padding: 0;
  }

  .editor-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .editor-row {
    grid-template-columns: 1fr !important;
  }
}
</style>
