<template>
  <n-modal
    :show="show"
    preset="card"
    :title="title"
    class="ai-record-selector-modal"
    :style="{ width: 'min(920px, calc(100vw - 32px))' }"
    :mask-closable="false"
    @update:show="emit('update:show', $event)"
  >
    <AiForm
      v-if="searchSchema.length"
      ref="searchFormRef"
      v-model:value="searchFormData"
      :schema="searchSchema"
      :grid-cols="3"
      label-width="auto"
      :show-feedback="false"
      :show-submit="false"
      :show-reset="false"
      :enable-collapse="false"
      :y-gap="8"
      class="record-selector-search"
    >
      <template #formAction>
        <n-button type="primary" size="small" :loading="loading" @click="handleSearchSubmit">
          查询
        </n-button>
        <n-button size="small" quaternary :disabled="loading" @click="resetFilters">
          重置
        </n-button>
      </template>
    </AiForm>
    <div v-else class="record-selector-search record-selector-search--compact">
      <n-input
        v-model:value="keyword"
        clearable
        size="small"
        :placeholder="placeholder"
        @keyup.enter="reload"
      />
      <n-button type="primary" size="small" :loading="loading" @click="reload">
        查询
      </n-button>
      <n-button size="small" quaternary :disabled="loading" @click="resetFilters">
        重置
      </n-button>
    </div>

    <n-data-table
      :columns="tableColumns"
      :data="records"
      :loading="loading"
      :row-key="row => row.id"
      :checked-row-keys="checkedRowKeys"
      :pagination="pagination"
      :scroll-x="tableScrollX"
      :single-line="false"
      remote
      @update:checked-row-keys="handleCheckedKeys"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <template #footer>
      <n-space justify="space-between" align="center">
        <span class="record-selector-count">已选 {{ checkedRows.length }} 条</span>
        <n-space>
          <n-button @click="emit('update:show', false)">
            取消
          </n-button>
          <n-button type="primary" :disabled="!checkedRows.length" @click="confirmSelection">
            确定
          </n-button>
        </n-space>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { queryBusinessRecordSelector } from '@/api/business-app'
import { executeLowcodeQuerySource } from '@/api/lowcode-query-source'
import AiForm from './AiForm.vue'
import { normalizeSelectorMappings, readPath, resolveSelectorSearchParams } from './record-selector-utils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '选择记录',
  },
  objectCode: {
    type: String,
    default: '',
  },
  businessObjectCode: {
    type: String,
    default: '',
  },
  targetObjectCode: {
    type: String,
    default: '',
  },
  targetEntityCode: {
    type: String,
    default: '',
  },
  candidateObjectCode: {
    type: String,
    default: '',
  },
  referenceObjectCode: {
    type: String,
    default: '',
  },
  refObjectCode: {
    type: String,
    default: '',
  },
  sourceObjectCode: {
    type: String,
    default: '',
  },
  targetCode: {
    type: String,
    default: '',
  },
  suiteCode: {
    type: String,
    default: '',
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  displayFields: {
    type: Array,
    default: () => [],
  },
  keywordFields: {
    type: Array,
    default: () => [],
  },
  fieldMappings: {
    type: [Array, Object],
    default: () => [],
  },
  searchParams: {
    type: Object,
    default: () => ({}),
  },
  filterFields: {
    type: Array,
    default: () => [],
  },
  runtimeContext: {
    type: Object,
    default: () => ({}),
  },
  pageSize: {
    type: Number,
    default: 10,
  },
  /** 受管查询源模式：sourceType + sourceKey 同时非空时改走统一查询源，不再依赖业务对象编码 */
  querySourceType: {
    type: String,
    default: '',
  },
  querySourceKey: {
    type: String,
    default: '',
  },
  /** 关键词映射到的查询参数名，默认 keyword；仅在受管查询源模式下生效 */
  keywordParam: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:show', 'confirm'])

const loading = ref(false)
const keyword = ref('')
const records = ref([])
const columns = ref([])
const checkedRowKeys = ref([])
const checkedRows = ref([])
const searchFormRef = ref(null)
const searchFormData = ref({})
const pagination = ref({
  page: 1,
  pageSize: props.pageSize,
  itemCount: 0,
  pageSizes: [10, 20, 50],
  showSizePicker: true,
})

const placeholder = computed(() => props.keywordFields?.length ? '输入关键词查询' : '输入关键词')
const resolvedSearchParams = computed(() => resolveSelectorSearchParams(props.searchParams, props.runtimeContext))

/** 弹窗顶部的筛选字段（设计态配置，含默认值来源） */
const normalizedFilterFields = computed(() =>
  (Array.isArray(props.filterFields) ? props.filterFields : []).filter(f => f && f.fieldCode),
)

/** 将关键字搜索字段 + 过滤参数合并为统一的搜索 schema，与 AiCrudPage 保持一致 */
const KEYWORD_FIELD_KEY = '__rs_keyword__'

/** 根据过滤字段元数据解析 AiFormItem 组件类型（dictType/componentType/fieldType 来自设计态保存） */
function resolveFilterSchemaType(filter) {
  if (filter.dictType)
    return 'dictSelect'
  const ct = filter.componentType || filter.type || ''
  const orgTypes = ['orgTreeSelect', 'orgSelect', 'organizationSelect', 'departmentSelect', 'forgeOrgTreeSelect']
  const userTypes = ['userSelect', 'userPicker', 'user', 'userName', 'forgeUserSelect']
  if (orgTypes.includes(ct))
    return 'orgTreeSelect'
  if (userTypes.includes(ct))
    return 'userSelect'
  if (ct === 'regionTreeSelect')
    return 'regionTreeSelect'
  const ft = (filter.fieldType || '').toUpperCase()
  if (ft === 'DATE' || ft === 'DATETIME')
    return ft === 'DATE' ? 'date' : 'datetime'
  if (ft === 'NUMBER' || ft === 'MONEY')
    return 'number'
  return 'input'
}

const searchSchema = computed(() => {
  const schema = []
  const kwFields = Array.isArray(props.keywordFields) ? props.keywordFields : []
  if (kwFields.length) {
    schema.push({
      field: KEYWORD_FIELD_KEY,
      label: '关键字',
      type: 'input',
      clearable: true,
      placeholder: '输入关键词',
    })
  }
  normalizedFilterFields.value.forEach((filter) => {
    const type = resolveFilterSchemaType(filter)
    const entry = {
      field: filter.fieldCode,
      label: cleanFilterLabel(filter),
      type,
      clearable: true,
      placeholder: type === 'input' ? `请输入${cleanFilterLabel(filter)}` : `请选择${cleanFilterLabel(filter)}`,
    }
    // 字典类型透传 dictType，AiFormItem 自动加载字典选项
    if (filter.dictType)
      entry.dictType = filter.dictType
    schema.push(entry)
  })
  return schema
})

/** 筛选标签：优先 fieldLabel，去掉可能包含的编码后缀；回退时将 fieldCode 格式化可读 */
function cleanFilterLabel(filter) {
  const raw = filter.fieldLabel || filter.fieldCode || ''
  const cleaned = raw.replace(/[\uFF08(][^\uFF09)]*[\uFF09)]$/, '').trim()
  if (cleaned)
    return cleaned
  const code = filter.fieldCode || ''
  return code.replace(/_/g, ' ').replace(/([a-z])([A-Z])/g, '$1 $2').trim() || code
}

const effectiveObjectCode = computed(() => firstText(
  props.objectCode,
  props.businessObjectCode,
  props.targetObjectCode,
  props.targetEntityCode,
  props.candidateObjectCode,
  props.referenceObjectCode,
  props.refObjectCode,
  props.sourceObjectCode,
  props.targetCode,
))

/** 受管查询源模式：数据集 / 业务对象 / 接口统一入口，与选择器专属链路互斥 */
const isQuerySourceMode = computed(() =>
  Boolean(String(props.querySourceType || '').trim() && String(props.querySourceKey || '').trim()),
)
const tableColumns = computed(() => [
  {
    type: 'selection',
    multiple: props.multiple,
    width: 48,
  },
  ...columns.value.map(column => ({
    title: column.label || column.field,
    key: column.field,
    width: column.width || 140,
    ellipsis: { tooltip: true },
  })),
])

/** 动态列宽合计，防止多字段时撑破 920 弹窗 */
const tableScrollX = computed(() =>
  tableColumns.value.reduce((sum, column) => sum + (Number(column.width) || 140), 0),
)

watch(() => props.show, (visible) => {
  if (visible) {
    initSearchFormData()
    reload()
  }
  else {
    resetSelection()
  }
})

/** 按配置初始化搜索表单数据：关键字 + 过滤参数默认值 */
function initSearchFormData() {
  const data = {}
  data[KEYWORD_FIELD_KEY] = ''
  normalizedFilterFields.value.forEach((filter) => {
    const fallback = resolveFilterDefault(filter)
    data[filter.fieldCode] = fallback === null || fallback === undefined ? '' : fallback
  })
  searchFormData.value = data
}

function resolveFilterDefault(filter) {
  if (filter.defaultType === 'form' && filter.formField)
    return readPath(props.runtimeContext?.form || props.runtimeContext?.formData || {}, filter.formField)
  if (filter.defaultType === 'fixed')
    return filter.defaultValue
  return ''
}

/** 从统一搜索表单提取关键字和过滤参数，执行查询 */
function handleSearchSubmit() {
  const data = searchFormData.value || {}
  keyword.value = data[KEYWORD_FIELD_KEY] || ''
  pagination.value.page = 1
  reload()
}

function resetFilters() {
  initSearchFormData()
  keyword.value = ''
  pagination.value.page = 1
  reload()
}

/** 从统一搜索表单读取过滤参数，合并预过滤条件 */
function mergedSearchParams() {
  const merged = { ...resolvedSearchParams.value }
  const data = searchFormData.value || {}
  normalizedFilterFields.value.forEach((filter) => {
    const value = data[filter.fieldCode]
    if (value !== undefined && value !== null && String(value).trim() !== '')
      merged[filter.fieldCode] = value
    else
      delete merged[filter.fieldCode]
  })
  return merged
}

async function reload() {
  if (isQuerySourceMode.value) {
    await reloadFromQuerySource()
    return
  }
  if (!effectiveObjectCode.value) {
    records.value = []
    columns.value = []
    pagination.value.itemCount = 0
    return
  }
  loading.value = true
  try {
    const res = await queryBusinessRecordSelector({
      suiteCode: props.suiteCode,
      objectCode: effectiveObjectCode.value,
      businessObjectCode: props.businessObjectCode || effectiveObjectCode.value,
      targetObjectCode: props.targetObjectCode || effectiveObjectCode.value,
      targetEntityCode: props.targetEntityCode || effectiveObjectCode.value,
      candidateObjectCode: props.candidateObjectCode,
      referenceObjectCode: props.referenceObjectCode,
      refObjectCode: props.refObjectCode,
      sourceObjectCode: props.sourceObjectCode,
      targetCode: props.targetCode,
      keyword: keyword.value,
      keywordFields: props.keywordFields,
      searchParams: mergedSearchParams(),
      displayFields: props.displayFields,
      fieldMappings: toMappingArray(props.fieldMappings),
    }, {
      pageNum: pagination.value.page,
      pageSize: pagination.value.pageSize,
    })
    const data = res.data || {}
    records.value = Array.isArray(data.records) ? data.records : []
    columns.value = Array.isArray(data.columns) ? data.columns : []
    pagination.value.itemCount = Number(data.total || 0)
  }
  finally {
    loading.value = false
  }
}

/** 受管查询源加载：筛选参数与关键词并入 params，列定义由返回的字段元数据生成 */
async function reloadFromQuerySource() {
  loading.value = true
  try {
    const params = { ...mergedSearchParams() }
    const keywordText = String(keyword.value || '').trim()
    if (keywordText)
      params[props.keywordParam || 'keyword'] = keywordText
    const res = await executeLowcodeQuerySource({
      sourceType: String(props.querySourceType || '').trim(),
      sourceKey: String(props.querySourceKey || '').trim(),
      params,
      pageNum: pagination.value.page,
      pageSize: pagination.value.pageSize,
    })
    const data = res?.data || {}
    records.value = Array.isArray(data.data) ? data.data : []
    columns.value = (Array.isArray(data.fields) ? data.fields : [])
      .filter(field => field && field.field && field.sensitive !== true)
      .map(field => ({ field: field.field, label: field.label || field.field }))
    pagination.value.itemCount = Number(data.total ?? records.value.length)
  }
  finally {
    loading.value = false
  }
}

function handleCheckedKeys(keys = [], rows = []) {
  checkedRowKeys.value = props.multiple ? keys : keys.slice(-1)
  checkedRows.value = props.multiple ? rows : rows.slice(-1)
}

function handlePageChange(page) {
  pagination.value.page = page
  reload()
}

function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  reload()
}

function confirmSelection() {
  emit('confirm', {
    rows: checkedRows.value,
    mappings: normalizeSelectorMappings(props.fieldMappings),
  })
  emit('update:show', false)
}

function resetSelection() {
  checkedRowKeys.value = []
  checkedRows.value = []
  keyword.value = ''
}

function toMappingArray(mappings = []) {
  if (Array.isArray(mappings))
    return mappings
  return Object.entries(mappings || {}).map(([sourceField, targetField]) => ({ sourceField, targetField }))
}

function firstText(...values) {
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (text)
      return text
  }
  return ''
}
</script>

<style scoped>
.record-selector-search {
  margin-bottom: 12px;
}

.record-selector-search--compact {
  display: flex;
  gap: 8px;
  align-items: center;
}

.record-selector-search--compact .n-input {
  flex: 1;
  min-width: 0;
}

.record-selector-count {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
