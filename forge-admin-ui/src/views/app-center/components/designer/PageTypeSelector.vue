<template>
  <n-modal
    :show="show"
    preset="card"
    class="page-type-selector-modal"
    title="创建页面"
    :bordered="false"
    :mask-closable="false"
    :style="{ width: 'min(920px, calc(100vw - 40px))' }"
    @update:show="emit('update:show', $event)"
  >
    <div class="page-type-selector-intro">
      <strong>先选择页面形态</strong>
      <span>数据页需要选择运行数据源。可以新建数据表，也可以引用已有表并自动生成表单。</span>
    </div>

    <div class="page-type-grid" role="radiogroup" aria-label="页面形态">
      <button
        v-for="item in PAGE_SHAPE_TYPES"
        :key="item.value"
        type="button"
        class="page-type-card"
        :class="{ active: form.pageType === item.value }"
        role="radio"
        :aria-checked="form.pageType === item.value"
        @click="selectPageType(item.value)"
      >
        <span class="page-type-icon" :class="`kind-${item.value}`">
          <n-icon><component :is="pageTypeIcons[item.value]" /></n-icon>
        </span>
        <span class="page-type-copy">
          <strong>{{ item.label }}</strong>
          <small>{{ item.description }}</small>
        </span>
        <span class="page-type-check">✓</span>
      </button>
    </div>

    <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" class="page-type-form">
      <n-form-item label="页面名称" path="pageName">
        <n-input v-model:value="form.pageName" maxlength="50" show-count placeholder="例如：客户管理" @update:value="handlePageNameChange" />
      </n-form-item>
      <div v-if="form.pageType !== 'custom'" class="page-data-source">
        <div class="page-data-source-head">
          <strong>数据来源</strong>
          <span>先选择已配置的低代码运行数据源，再决定新建表或引用现有表。</span>
        </div>
        <n-radio-group v-model:value="form.dataSourceMode" size="small" class="page-data-source-modes" @update:value="handleDataSourceModeChange">
          <n-radio-button value="CREATE">
            新建数据表
          </n-radio-button>
          <n-radio-button value="EXISTING_TABLE">
            引用现有数据表
          </n-radio-button>
        </n-radio-group>
        <n-grid :cols="form.dataSourceMode === 'EXISTING_TABLE' ? 2 : 1" :x-gap="12">
          <n-form-item-gi label="运行数据源" path="runtimeDatasourceId">
            <n-select
              v-model:value="form.runtimeDatasourceId"
              filterable
              :loading="datasourceLoading"
              :options="datasourceOptions"
              placeholder="选择低代码运行数据源"
              @update:value="handleDatasourceChange"
            />
          </n-form-item-gi>
          <n-form-item-gi v-if="form.dataSourceMode === 'EXISTING_TABLE'" label="数据表" path="importTableName">
            <n-select
              v-model:value="form.importTableName"
              filterable
              clearable
              :disabled="!form.runtimeDatasourceId"
              :loading="tableLoading"
              :options="tableOptions"
              placeholder="选择数据表"
              @update:value="handleTableChange"
            />
          </n-form-item-gi>
        </n-grid>
        <n-alert v-if="!datasourceLoading && !datasourceOptions.length" type="warning" :bordered="false">
          还没有可用的低代码运行数据源。请先在代码生成 / 数据源中配置 LOWCODE_RUNTIME 数据源。
        </n-alert>
        <div v-if="importedFields.length" class="page-table-summary">
          <code>{{ selectedTableLabel }}</code>
          <span>已读取 {{ importedFields.length }} 个字段，将自动生成表单并推断字段类型。</span>
        </div>
      </div>
      <div v-if="form.pageType !== 'custom'" class="page-object-fields">
        <n-form-item label="对象名称" path="objectName">
          <n-input v-model:value="form.objectName" maxlength="50" placeholder="页面中管理的数据对象名称" @update:value="handleObjectNameChange" />
        </n-form-item>
        <n-form-item label="对象编码" path="objectCode">
          <n-input
            v-model:value="form.objectCode"
            maxlength="48"
            placeholder="字母开头，可使用数字和下划线"
            :loading="objectCodeChecking"
            @update:value="objectCodeEdited = true"
          />
          <template #feedback>
            {{ objectCodeChecking ? '正在检查编码是否可用…' : '自动生成后仍可编辑，保存后会显示在设计器顶部。' }}
          </template>
        </n-form-item>
      </div>
    </n-form>

    <template #footer>
      <div class="page-type-footer">
        <n-button @click="emit('update:show', false)">
          取消
        </n-button>
        <n-button type="primary" @click="confirmSelection">
          {{ form.pageType === 'custom' ? '创建并进入自由布局' : '创建并进入设计器' }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { AppsOutline, CreateOutline, DocumentTextOutline, GitBranchOutline, GitNetworkOutline, ListOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { businessObjectCodeAvailable, genDatasourceEnabled, genDatasourceTableColumns, genDatasourceTables } from '@/api/business-app'
import { PAGE_SHAPE_TYPES } from '../../in-app-builder/page-shape-design'
import {
  inferFormFieldsFromColumns,
  PAGE_DATA_SOURCE_CREATE,
  PAGE_DATA_SOURCE_EXISTING_TABLE,
} from '../../in-app-builder/page-table-import'
import { normalizeObjectCode } from './form-first/namingUtils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  defaultParentId: {
    type: [String, Number],
    default: null,
  },
  defaultPageType: {
    type: String,
    default: 'form',
  },
})

const emit = defineEmits(['update:show', 'confirm'])
const message = useMessage()
const formRef = ref(null)
const objectCodeEdited = ref(false)
const objectNameEdited = ref(false)
const pageNameEdited = ref(false)
const datasourceLoading = ref(false)
const tableLoading = ref(false)
const fieldLoading = ref(false)
const objectCodeChecking = ref(false)
const objectCodeCheckSignature = ref('')
const objectCodeCheckResult = ref(null)
let objectCodeCheckRequestId = 0
const datasourceList = ref([])
const tableList = ref([])
const importedFields = ref([])
const form = reactive(createDefaultForm())
const pageTypeIcons = {
  'form': DocumentTextOutline,
  'list': ListOutline,
  'list-form': AppsOutline,
  'tree-list': GitNetworkOutline,
  'tree-table': GitBranchOutline,
  'custom': CreateOutline,
}
const requiredValue = {
  required: true,
  trigger: ['change', 'blur'],
  validator: (_rule, value) => value !== null && value !== undefined && String(value).trim() !== '',
}
const rules = computed(() => {
  const dataPage = form.pageType !== 'custom'
  return {
    pageName: { required: true, message: '请输入页面名称', trigger: ['input', 'blur'] },
    objectName: dataPage ? { required: true, message: '请输入对象名称', trigger: ['input', 'blur'] } : undefined,
    objectCode: dataPage
      ? [
          { required: true, message: '请输入对象编码', trigger: ['input', 'blur'] },
          {
            validator: (_rule, value) => /^[a-z]\w{1,47}$/i.test(String(value || '')),
            message: '对象编码需以字母开头，仅含字母、数字和下划线（2-48 位）',
            trigger: ['input', 'blur'],
          },
          {
            async validator(_rule, value) {
              const code = String(value || '').trim()
              if (!/^[a-z]\w{1,47}$/i.test(code))
                return
              const signature = code.toLowerCase()
              if (objectCodeCheckSignature.value === signature && objectCodeCheckResult.value === false)
                throw new Error('对象编码已存在，请换一个编码')
              if (objectCodeCheckSignature.value === signature && objectCodeCheckResult.value === true)
                return
              const requestId = ++objectCodeCheckRequestId
              objectCodeChecking.value = true
              try {
                const response = await businessObjectCodeAvailable(code)
                if (requestId !== objectCodeCheckRequestId)
                  return
                objectCodeCheckSignature.value = signature
                objectCodeCheckResult.value = response.data === true
                if (!objectCodeCheckResult.value)
                  throw new Error('对象编码已存在，请换一个编码')
              }
              catch (error) {
                if (requestId === objectCodeCheckRequestId && objectCodeCheckResult.value !== false) {
                  objectCodeCheckSignature.value = signature
                  objectCodeCheckResult.value = null
                }
                throw error?.message ? error : new Error('对象编码校验失败，请稍后重试')
              }
              finally {
                if (requestId === objectCodeCheckRequestId)
                  objectCodeChecking.value = false
              }
            },
            trigger: ['blur'],
          },
        ]
      : undefined,
    runtimeDatasourceId: dataPage
      ? { ...requiredValue, message: '请选择运行数据源' }
      : undefined,
    importTableName: dataPage && form.dataSourceMode === PAGE_DATA_SOURCE_EXISTING_TABLE
      ? { ...requiredValue, message: '请选择要引用的数据表' }
      : undefined,
  }
})
const datasourceOptions = computed(() => datasourceList.value.map(item => ({
  label: `${item.datasourceName || item.datasourceCode || '未命名数据源'}${Number(item.isDefault) === 1 ? '（默认）' : ''} / ${item.dbType || '-'}`,
  value: item.datasourceId,
})))
const tableOptions = computed(() => tableList.value.map(item => ({
  label: item.tableComment ? `${item.tableName}（${item.tableComment}）` : item.tableName,
  value: item.tableName,
})))
const selectedTableLabel = computed(() => {
  const table = tableList.value.find(item => item.tableName === form.importTableName)
  if (!table)
    return form.importTableName
  return table.tableComment ? `${table.tableName}（${table.tableComment}）` : table.tableName
})

watch(() => [props.show, props.defaultParentId, props.defaultPageType], async ([visible]) => {
  if (!visible)
    return
  Object.assign(form, createDefaultForm())
  objectCodeEdited.value = false
  objectNameEdited.value = false
  pageNameEdited.value = false
  objectCodeCheckSignature.value = ''
  objectCodeCheckResult.value = null
  objectCodeChecking.value = false
  objectCodeCheckRequestId += 1
  tableList.value = []
  importedFields.value = []
  await loadDatasources()
})

function createDefaultForm() {
  const pageType = PAGE_SHAPE_TYPES.some(item => item.value === props.defaultPageType)
    ? props.defaultPageType
    : 'form'
  return {
    pageType,
    pageName: '',
    objectName: '',
    objectCode: '',
    dataSourceMode: PAGE_DATA_SOURCE_CREATE,
    runtimeDatasourceId: null,
    importTableName: '',
    parentId: props.defaultParentId == null || props.defaultParentId === ''
      ? null
      : String(props.defaultParentId),
  }
}

function selectPageType(pageType) {
  form.pageType = pageType
  formRef.value?.restoreValidation?.()
}

function handlePageNameChange(value) {
  pageNameEdited.value = true
  if (!objectNameEdited.value)
    form.objectName = value
  if (!objectCodeEdited.value)
    form.objectCode = normalizeObjectCode('', value)
}

function handleObjectNameChange(value) {
  objectNameEdited.value = true
  if (!objectCodeEdited.value)
    form.objectCode = normalizeObjectCode('', value)
}

async function handleDataSourceModeChange() {
  form.importTableName = ''
  importedFields.value = []
  if (form.dataSourceMode === PAGE_DATA_SOURCE_EXISTING_TABLE)
    await loadTables(form.runtimeDatasourceId)
  else
    tableList.value = []
}

async function handleDatasourceChange(datasourceId) {
  form.importTableName = ''
  importedFields.value = []
  tableList.value = []
  if (form.dataSourceMode === PAGE_DATA_SOURCE_EXISTING_TABLE)
    await loadTables(datasourceId)
}

async function handleTableChange(tableName) {
  importedFields.value = []
  const table = tableList.value.find(item => item.tableName === tableName)
  if (table) {
    const tableTitle = String(table.tableComment || table.tableName || '').trim()
    if (!pageNameEdited.value)
      form.pageName = tableTitle
    if (!objectNameEdited.value)
      form.objectName = tableTitle
    if (!objectCodeEdited.value)
      form.objectCode = normalizeObjectCode(table.tableName, tableTitle)
  }
  if (!form.runtimeDatasourceId || !tableName)
    return
  fieldLoading.value = true
  try {
    const response = await genDatasourceTableColumns(form.runtimeDatasourceId, tableName)
    importedFields.value = inferFormFieldsFromColumns(response.data || [])
    if (!importedFields.value.length)
      message.warning('该数据表没有可导入的业务字段')
  }
  catch (error) {
    importedFields.value = []
    message.error(error?.message || '读取数据表字段失败')
  }
  finally {
    fieldLoading.value = false
  }
}

async function loadDatasources() {
  datasourceLoading.value = true
  try {
    const response = await genDatasourceEnabled('LOWCODE_RUNTIME')
    datasourceList.value = response.data || []
    selectDefaultDatasource()
  }
  catch (error) {
    datasourceList.value = []
    form.runtimeDatasourceId = null
    message.error(error?.message || '加载运行数据源失败')
  }
  finally {
    datasourceLoading.value = false
  }
}

function selectDefaultDatasource() {
  if (form.runtimeDatasourceId && datasourceList.value.some(
    item => String(item.datasourceId) === String(form.runtimeDatasourceId),
  )) {
    return
  }
  const datasource = datasourceList.value.find(item => Number(item.isDefault) === 1) || datasourceList.value[0]
  form.runtimeDatasourceId = datasource?.datasourceId ?? null
}

async function loadTables(datasourceId) {
  tableList.value = []
  if (!datasourceId)
    return
  tableLoading.value = true
  try {
    const response = await genDatasourceTables(datasourceId)
    tableList.value = response.data || []
  }
  catch (error) {
    tableList.value = []
    message.error(error?.message || '加载数据表失败')
  }
  finally {
    tableLoading.value = false
  }
}

async function confirmSelection() {
  if (form.pageType !== 'custom')
    await formRef.value?.validate()
  else if (!String(form.pageName || '').trim())
    return formRef.value?.validate()
  if (form.pageType !== 'custom' && form.dataSourceMode === PAGE_DATA_SOURCE_EXISTING_TABLE) {
    if (fieldLoading.value) {
      message.warning('正在读取数据表字段，请稍候')
      return
    }
    if (!importedFields.value.length) {
      message.warning('请选择包含业务字段的数据表')
      return
    }
  }
  emit('confirm', {
    ...form,
    pageName: String(form.pageName || '').trim(),
    objectName: String(form.objectName || '').trim(),
    objectCode: form.pageType === 'custom' ? '' : normalizeObjectCode(form.objectCode, form.objectName),
    dataSourceMode: form.pageType === 'custom' ? '' : form.dataSourceMode,
    runtimeDatasourceId: form.pageType === 'custom' ? null : form.runtimeDatasourceId,
    importTableName: form.pageType !== 'custom' && form.dataSourceMode === PAGE_DATA_SOURCE_EXISTING_TABLE
      ? form.importTableName
      : '',
    fields: form.pageType !== 'custom' && form.dataSourceMode === PAGE_DATA_SOURCE_EXISTING_TABLE
      ? importedFields.value
      : [],
  })
}
</script>

<style scoped>
.page-type-selector-intro {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 18px;
}

.page-type-selector-intro strong {
  color: var(--text-primary);
  font-size: 16px;
}

.page-type-selector-intro span {
  color: var(--text-secondary);
  font-size: 13px;
}

.page-type-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.page-type-card {
  position: relative;
  display: flex;
  min-height: 142px;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  background: var(--bg-primary);
  text-align: left;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.page-type-card:hover,
.page-type-card.active {
  border-color: var(--n-primary-color, #3370ff);
  box-shadow: 0 6px 18px color-mix(in srgb, var(--n-primary-color, #3370ff) 16%, transparent);
  transform: translateY(-1px);
}

.page-type-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 10px;
  background: color-mix(in srgb, var(--n-primary-color, #3370ff) 10%, transparent);
  color: var(--n-primary-color, #3370ff);
  font-size: 22px;
}

.page-type-icon.kind-list-form {
  background: #f3efff;
  color: #8b5cf6;
}

.page-type-icon.kind-tree-list {
  background: #f0f9f4;
  color: #1f8a5b;
}

.page-type-icon.kind-tree-table {
  background: #eef6ff;
  color: #2f6fed;
}

.page-type-icon.kind-custom {
  background: #e6fffb;
  color: #0f9f8f;
}

.page-type-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.page-type-copy strong {
  color: var(--text-primary);
  font-size: 15px;
}

.page-type-copy small {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.55;
}

.page-type-check {
  position: absolute;
  top: 10px;
  right: 12px;
  color: var(--n-primary-color, #3370ff);
  opacity: 0;
}

.page-type-card.active .page-type-check {
  opacity: 1;
}

.page-type-form {
  margin-top: 20px;
}

.page-data-source {
  display: grid;
  gap: 12px;
  margin: 4px 0 8px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-secondary);
}

.page-data-source-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-data-source-head strong {
  color: var(--text-primary);
  font-size: 13px;
}

.page-data-source-head span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.page-data-source-modes {
  width: max-content;
}

.page-table-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: var(--text-secondary);
  font-size: 12px;
}

.page-table-summary code {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--bg-primary);
  font-size: 12px;
}

.page-object-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.page-type-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 760px) {
  .page-type-grid,
  .page-object-fields {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 520px) {
  .page-type-grid,
  .page-object-fields {
    grid-template-columns: 1fr;
  }
}
</style>
