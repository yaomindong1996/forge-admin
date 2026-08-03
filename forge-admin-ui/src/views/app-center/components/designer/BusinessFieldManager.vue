<template>
  <div class="field-manager">
    <section class="field-asset-head">
      <div>
        <h2>字段与数据库映射</h2>
        <p>统一维护字段业务定义、数据库列与同步状态；页面展示、顺序和查询条件在对应设计器配置。</p>
      </div>
      <div class="field-summary" aria-label="字段概况">
        <span>业务字段 <strong>{{ fieldStats.business }}</strong></span>
        <span>系统字段 <strong>{{ fieldStats.system }}</strong></span>
        <span>结构一致 <strong>{{ fieldStats.inSync }}</strong></span>
      </div>
    </section>

    <section class="field-asset-toolbar">
      <n-input
        v-model:value="keyword"
        clearable
        placeholder="搜索字段名称、编码、列名"
        class="field-search"
      />
      <n-select
        v-model:value="statusFilter"
        :options="statusFilterOptions"
        class="field-filter"
      />
      <n-select
        v-model:value="sourceFilter"
        :options="sourceFilterOptions"
        class="field-filter"
      />
      <div class="system-field-toggle">
        <n-switch v-model:value="showSystemFields" size="small" />
        <span>{{ showSystemFields ? '显示系统字段' : '隐藏系统字段' }}</span>
        <small>{{ fieldStats.system }} 个</small>
      </div>
      <n-button type="primary" @click="openCreateModal">
        新增字段
      </n-button>
    </section>

    <section class="field-asset-workbench">
      <div class="field-list-pane">
        <BusinessFieldList
          :fields="filteredFields"
          :total-count="visibleFields.length"
          :selected-field-code="selectedFieldCode"
          :used-field-codes="usedFieldCodes"
          :show-head="false"
          @select="selectField"
          @create="openCreateModal"
          @patch="patchField"
          @duplicate="duplicateField"
          @delete="confirmDeleteField"
          @move="moveFilteredField"
          @add-to-form="$emit('addToForm', $event)"
        />
      </div>

      <aside v-if="!compactLayout" class="field-property-pane">
        <BusinessFieldPropertyPanel
          ref="propertyPanelRef"
          :field="selectedField"
          :all-fields="activeFields"
          :relations="relations"
          :object-code="objectCode"
          :developer-mode="developerMode"
          :saving="saving"
          @save="saveField"
          @dirty-change="$emit('dirtyChange', $event)"
        />
      </aside>
    </section>

    <n-drawer
      v-if="compactLayout"
      :show="propertyVisible"
      :width="drawerWidth"
      placement="right"
      :mask-closable="false"
      @update:show="handlePropertyDrawerUpdate"
    >
      <n-drawer-content
        title="字段属性"
        closable
        :native-scrollbar="false"
        body-content-style="padding: 0; overflow: hidden;"
      >
        <BusinessFieldPropertyPanel
          ref="propertyPanelRef"
          class="field-property-drawer-panel"
          :field="selectedField"
          :all-fields="activeFields"
          :relations="relations"
          :object-code="objectCode"
          :developer-mode="developerMode"
          :saving="saving"
          @save="saveField"
          @dirty-change="$emit('dirtyChange', $event)"
        />
      </n-drawer-content>
    </n-drawer>

    <n-modal
      v-model:show="createVisible"
      title="新增业务字段"
      preset="card"
      style="width: 520px"
      :mask-closable="false"
    >
      <n-form label-placement="top" :show-feedback="false">
        <n-form-item label="字段名称">
          <n-input v-model:value="createForm.fieldName" placeholder="例如：客户等级" />
        </n-form-item>
        <n-form-item label="字段英文名">
          <n-input
            v-model:value="createForm.fieldCode"
            placeholder="例如：customerLevel，留空自动生成"
            @blur="createForm.fieldCode = normalizeFieldCode(createForm.fieldCode, createForm.fieldName)"
          />
        </n-form-item>
        <n-form-item label="字段类型">
          <n-select v-model:value="createForm.fieldType" :options="fieldTypeOptions" filterable />
        </n-form-item>
        <n-form-item v-if="needsCreateDict" label="字典类型">
          <DictTypeSelect v-model:value="createForm.dictType" :fields="visibleFields" />
        </n-form-item>
        <div class="create-field-tip">
          创建后字段会加入字段资产。是否出现在当前表单、列表或查询区，请在对应页面设计器中配置。
        </div>
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi>
            <n-checkbox v-model:checked="createForm.required">
              必填
            </n-checkbox>
          </n-form-item-gi>
          <n-form-item-gi>
            <n-checkbox v-model:checked="createForm.unique">
              唯一校验
            </n-checkbox>
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="createVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="creating" @click="createField">
            创建字段
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  businessObjectFields,
  createBusinessObjectField,
  deleteBusinessObjectField,
  sortBusinessObjectFields,
  updateBusinessObjectField,
} from '@/api/business-app'
import DictTypeSelect from '@/components/lowcode-builder/shared/DictTypeSelect.vue'
import BusinessFieldList from './BusinessFieldList.vue'
import BusinessFieldPropertyPanel from './BusinessFieldPropertyPanel.vue'
import { extractForgeSchemaFieldRefs } from './form-first/forgeToFormCreate'
import { generateFieldCode, normalizeFieldCode } from './form-first/namingUtils'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  objectCode: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  relations: {
    type: Array,
    default: () => [],
  },
  developerMode: {
    type: Boolean,
    default: false,
  },
  formDesignerSchema: {
    type: Object,
    default: null,
  },
  tableMapping: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['updated', 'dirtyChange', 'addToForm'])

const message = useMessage()
const localFields = ref([])
const selectedFieldCode = ref('')
const keyword = ref('')
const statusFilter = ref('all')
const sourceFilter = ref('all')
const showSystemFields = ref(false)
const propertyPanelRef = ref(null)
const saving = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const propertyVisible = ref(false)
const compactLayout = ref(false)
const viewportWidth = ref(typeof window === 'undefined' ? 1280 : window.innerWidth)
const createForm = reactive(createDefaultCreateForm())
const lastSuggestedCreateFieldCode = ref('')
let compactMediaQuery = null

const fieldTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '多行文本', value: 'MULTILINE' },
  { label: '数字', value: 'NUMBER' },
  { label: '金额', value: 'MONEY' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '下拉', value: 'DICT' },
  { label: '单选', value: 'RADIO' },
  { label: '多选', value: 'CHECKBOX' },
  { label: '开关', value: 'SWITCH' },
  { label: '附件', value: 'FILE' },
  { label: '图片', value: 'IMAGE' },
  { label: '人员', value: 'USER' },
  { label: '部门', value: 'DEPT' },
  { label: '地区', value: 'REGION' },
  { label: '引用对象', value: 'REFERENCE' },
]

const databaseMappingByField = computed(() => {
  const mappings = new Map()
  for (const item of props.tableMapping?.fields || []) {
    if (item.fieldCode)
      mappings.set(`field:${item.fieldCode}`, item)
    if (item.columnName)
      mappings.set(`column:${String(item.columnName).toLowerCase()}`, item)
  }
  return mappings
})

const orderedFields = computed(() => [...localFields.value]
  .sort((a, b) => Number(a.sortOrder ?? 0) - Number(b.sortOrder ?? 0)))

const activeFields = computed(() => orderedFields.value.filter(field => !isHiddenField(field)))
const visibleFields = computed(() => activeFields.value
  .filter(field => showSystemFields.value || !field.systemField))
const filteredFields = computed(() => visibleFields.value
  .filter(matchesFieldFilters)
  .map(field => ({ ...field, databaseMapping: resolveDatabaseMapping(field) })))
const usedFieldCodes = computed(() => extractForgeSchemaFieldRefs(props.formDesignerSchema || {}))
const selectedField = computed(() => visibleFields.value.find(field => field.fieldCode === selectedFieldCode.value) || null)
const needsCreateDict = computed(() => ['DICT', 'RADIO', 'CHECKBOX'].includes(createForm.fieldType))
const drawerWidth = computed(() => Math.min(520, Math.max(320, viewportWidth.value - 24)))
const fieldStats = computed(() => {
  const fields = activeFields.value
  return {
    business: fields.filter(field => !field.systemField).length,
    system: activeFields.value.filter(field => field.systemField).length,
    inSync: fields.filter(field => resolveDatabaseMapping(field)?.syncStatus === 'IN_SYNC').length,
  }
})

const statusFilterOptions = [
  { label: '全部状态', value: 'all' },
  { label: '启用字段', value: 'enabled' },
  { label: '停用字段', value: 'disabled' },
]

const sourceFilterOptions = [
  { label: '全部来源', value: 'all' },
  { label: '表单生成', value: 'designer' },
  { label: '手动维护', value: 'manual' },
]

watch(
  () => props.fields,
  (value) => {
    localFields.value = cloneValue(value || [])
    if (selectedFieldCode.value && !visibleFields.value.some(field => field.fieldCode === selectedFieldCode.value))
      closePropertyPanel(true, true)
  },
  { immediate: true, deep: true },
)

watch(
  () => createForm.fieldName,
  (value) => {
    if (!String(value || '').trim())
      return
    const suggested = normalizeFieldCode(generateFieldCode(value), value)
    if (!suggested)
      return
    if (!createForm.fieldCode || createForm.fieldCode === lastSuggestedCreateFieldCode.value)
      createForm.fieldCode = suggested
    lastSuggestedCreateFieldCode.value = suggested
  },
)

watch(showSystemFields, (visible) => {
  if (visible || !selectedFieldCode.value)
    return
  const current = localFields.value.find(field => field.fieldCode === selectedFieldCode.value)
  if (current?.systemField)
    closePropertyPanel(true, true)
})

onMounted(() => {
  compactMediaQuery = window.matchMedia('(max-width: 980px)')
  applyCompactLayout(compactMediaQuery)
  compactMediaQuery.addEventListener?.('change', applyCompactLayout)
  window.addEventListener('resize', updateViewportWidth)
})

onBeforeUnmount(() => {
  compactMediaQuery?.removeEventListener?.('change', applyCompactLayout)
  window.removeEventListener('resize', updateViewportWidth)
})

function selectField(field) {
  const nextCode = field?.fieldCode || ''
  if (!nextCode)
    return
  if (nextCode === selectedFieldCode.value) {
    if (compactLayout.value)
      propertyVisible.value = true
    return
  }
  if (propertyPanelRef.value?.hasChanges?.()) {
    window.$dialog.warning({
      title: '未保存字段变更',
      content: '切换字段会放弃当前字段尚未保存的属性修改。',
      positiveText: '放弃并切换',
      negativeText: '继续编辑',
      onPositiveClick: () => openPropertyPanel(nextCode),
    })
    return
  }
  openPropertyPanel(nextCode)
}

function openPropertyPanel(fieldCode) {
  selectedFieldCode.value = fieldCode
  propertyVisible.value = true
}

function closePropertyPanel(discardChanges = false, clearSelection = false) {
  if (discardChanges)
    propertyPanelRef.value?.resetForm?.()
  propertyVisible.value = false
  if (clearSelection)
    selectedFieldCode.value = ''
  emit('dirtyChange', false)
}

function handlePropertyDrawerUpdate(show) {
  if (show) {
    propertyVisible.value = true
    return
  }
  if (propertyPanelRef.value?.hasChanges?.()) {
    window.$dialog.warning({
      title: '未保存字段变更',
      content: '关闭属性窗口会放弃当前字段尚未保存的修改。',
      positiveText: '放弃并关闭',
      negativeText: '继续编辑',
      onPositiveClick: () => closePropertyPanel(true),
    })
    return
  }
  closePropertyPanel(false)
}

function applyCompactLayout(event) {
  compactLayout.value = !!event.matches
  if (!compactLayout.value && selectedFieldCode.value)
    propertyVisible.value = true
}

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth
}

function openCreateModal() {
  Object.assign(createForm, createDefaultCreateForm())
  lastSuggestedCreateFieldCode.value = ''
  createVisible.value = true
}

async function createField() {
  if (!props.objectId)
    return
  if (!createForm.fieldName?.trim()) {
    message.warning('请输入字段名称')
    return
  }
  if (needsCreateDict.value && !createForm.dictType?.trim()) {
    message.warning('字典字段需要选择或填写字典类型')
    return
  }
  creating.value = true
  try {
    const res = await createBusinessObjectField(props.objectId, normalizeFieldPayload(createForm))
    const createdCode = res.data?.fieldCode || ''
    createVisible.value = false
    message.success('字段已创建')
    await reloadFields({ persisted: true, reloadDesigner: true })
    if (createdCode)
      openPropertyPanel(createdCode)
  }
  finally {
    creating.value = false
  }
}

async function duplicateField(field) {
  if (!field || !props.objectId)
    return
  const payload = normalizeFieldPayload({
    ...field,
    fieldName: `${field.fieldName || '字段'}副本`,
    fieldCode: '',
    columnName: '',
    systemField: false,
    readonly: false,
  })
  const res = await createBusinessObjectField(props.objectId, payload)
  const createdCode = res.data?.fieldCode || ''
  message.success('字段已复制')
  await reloadFields({ persisted: true, reloadDesigner: true })
  if (createdCode)
    openPropertyPanel(createdCode)
}

async function patchField(field, patch) {
  if (!field?.fieldCode || !props.objectId)
    return
  saving.value = true
  try {
    await updateBusinessObjectField(props.objectId, field.fieldCode, normalizeFieldPayload({ ...field, ...patch }))
    await reloadFields({ persisted: true, reloadDesigner: true })
  }
  finally {
    saving.value = false
  }
}

async function saveField(payload, targetFieldCode = selectedFieldCode.value) {
  const fieldCode = targetFieldCode || selectedField.value?.fieldCode
  if (!fieldCode || !props.objectId)
    return false
  const targetField = selectedField.value || visibleFields.value.find(field => field.fieldCode === fieldCode)
  if (targetField?.systemField) {
    message.info('系统字段为只读字段，无需保存')
    return false
  }
  if (!propertyPanelRef.value?.hasChanges?.()) {
    message.info('当前字段没有变更')
    return false
  }
  if (!payload.fieldName?.trim()) {
    message.warning('字段名称不能为空')
    return false
  }
  if (!payload.fieldCode?.trim()) {
    message.warning('字段英文名不能为空')
    return false
  }
  const normalizedPayload = normalizeFieldPayload(payload)
  const typeContractChanged = ['fieldType', 'dataType', 'componentType']
    .some(key => String(targetField?.[key] || '') !== String(normalizedPayload[key] || ''))
  saving.value = true
  try {
    const res = await updateBusinessObjectField(props.objectId, fieldCode, normalizedPayload)
    const savedField = res.data || normalizedPayload
    if (savedField.fieldCode && savedField.fieldCode !== selectedFieldCode.value)
      selectedFieldCode.value = savedField.fieldCode
    propertyPanelRef.value?.resetForm?.()
    emit('dirtyChange', false)
    message.success(typeContractChanged
      ? '字段类型已保存；请在发布检查中同步数据表并重新发布业务单元，再发布能力新版本'
      : '字段已保存')
    await reloadFields({
      persisted: true,
      reloadDesigner: true,
      fieldRename: buildFieldRenameMeta(fieldCode, targetField, normalizedPayload, savedField),
    })
    return true
  }
  finally {
    saving.value = false
  }
}

async function saveSelectedField() {
  if (!selectedField.value) {
    message.info('当前没有需要保存的字段属性')
    return true
  }
  if (compactLayout.value && !propertyVisible.value) {
    openPropertyPanel(selectedField.value.fieldCode)
    message.info('字段属性面板已打开，修改后再保存')
    return false
  }
  if (!propertyPanelRef.value) {
    message.warning('字段属性尚未加载完成，请稍后重试')
    return false
  }
  const targetFieldCode = selectedFieldCode.value || propertyPanelRef.value.getPayload?.()?.fieldCode
  return saveField(propertyPanelRef.value.getPayload?.() || {}, targetFieldCode)
}

function confirmDeleteField(field) {
  const references = (field.referencedBy || []).filter(Boolean)
  const content = references.length
    ? `字段正在被 ${references.join('、')} 引用，请先移除引用后再删除。`
    : '删除后字段会从设计列表和页面布局中隐藏，历史模型列保留。'
  window.$dialog.warning({
    title: `删除字段「${field.fieldName || field.fieldCode}」`,
    content,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: () => deleteField(field),
  })
}

async function deleteField(field) {
  if (!field?.fieldCode || !props.objectId)
    return
  await deleteBusinessObjectField(props.objectId, field.fieldCode)
  message.success('字段已隐藏')
  if (selectedFieldCode.value === field.fieldCode)
    closePropertyPanel(true, true)
  await reloadFields({ persisted: true, reloadDesigner: true })
}

async function moveField(from, to) {
  if (from === to || from < 0 || to < 0 || to >= visibleFields.value.length)
    return
  if (visibleFields.value[from]?.systemField || visibleFields.value[to]?.systemField)
    return
  const nextVisible = [...visibleFields.value]
  const [item] = nextVisible.splice(from, 1)
  nextVisible.splice(to, 0, item)
  const visibleCodes = new Set(nextVisible.map(field => field.fieldCode))
  let visibleIndex = 0
  const next = orderedFields.value.map((field) => {
    if (!visibleCodes.has(field.fieldCode))
      return field
    const reordered = nextVisible[visibleIndex]
    visibleIndex += 1
    return reordered
  })
  localFields.value = next.map((field, index) => ({ ...field, sortOrder: index + 1 }))
  if (!props.objectId)
    return
  await sortBusinessObjectFields(props.objectId, localFields.value.map(field => field.fieldCode).filter(Boolean))
  emit('updated', orderedFields.value, { persisted: true, reloadDesigner: true })
}

function moveFilteredField(from, to) {
  const source = filteredFields.value[from]
  const target = filteredFields.value[to]
  if (!source || !target)
    return
  const sourceIndex = visibleFields.value.findIndex(field => field.fieldCode === source.fieldCode)
  const targetIndex = visibleFields.value.findIndex(field => field.fieldCode === target.fieldCode)
  moveField(sourceIndex, targetIndex)
}

async function reloadFields(options = {}) {
  if (!props.objectId)
    return
  const res = await businessObjectFields(props.objectId)
  localFields.value = cloneValue(res.data || [])
  if (selectedFieldCode.value && !visibleFields.value.some(field => field.fieldCode === selectedFieldCode.value))
    closePropertyPanel(true, true)
  emit('updated', orderedFields.value, options)
}

function createDefaultCreateForm() {
  return {
    fieldName: '',
    fieldCode: '',
    fieldType: 'TEXT',
    dictType: '',
    required: false,
    formVisible: true,
    listVisible: true,
    searchable: false,
    unique: false,
    importable: true,
    exportable: true,
  }
}

function isHiddenField(field) {
  return String(field?.fieldStatus || '').toUpperCase() === 'HIDDEN'
}

function matchesFieldFilters(field = {}) {
  const text = keyword.value.trim().toLowerCase()
  if (text) {
    const haystack = [
      field.fieldName,
      field.fieldCode,
      field.columnName,
      field.componentType,
      field.fieldType,
    ].join(' ').toLowerCase()
    if (!haystack.includes(text))
      return false
  }
  if (statusFilter.value === 'enabled' && String(field.fieldStatus || 'ENABLED').toUpperCase() === 'DISABLED')
    return false
  if (statusFilter.value === 'disabled' && String(field.fieldStatus || 'ENABLED').toUpperCase() !== 'DISABLED')
    return false
  if (sourceFilter.value === 'designer' && !isDesignerField(field))
    return false
  if (sourceFilter.value === 'manual' && (isDesignerField(field) || field.systemField))
    return false
  return true
}

function isDesignerField(field = {}) {
  const binding = field.fieldBinding || field.basicProps?.fieldBinding || {}
  return binding.source === 'designer' || binding.createIfMissing === true
}

function resolveDatabaseMapping(field = {}) {
  return databaseMappingByField.value.get(`field:${field.fieldCode}`)
    || databaseMappingByField.value.get(`column:${String(field.columnName || '').toLowerCase()}`)
    || null
}

function normalizeFieldPayload(source) {
  return {
    fieldName: source.fieldName,
    fieldCode: normalizeFieldCode(source.fieldCode, source.fieldName),
    columnName: source.columnName || '',
    fieldType: source.fieldType || 'TEXT',
    dataType: source.dataType || '',
    length: source.length,
    precision: source.precision,
    required: !!source.required,
    defaultValue: source.defaultValue,
    searchable: !!source.searchable,
    listVisible: source.listVisible !== false,
    formVisible: source.formVisible !== false,
    importable: source.importable !== false,
    exportable: source.exportable !== false,
    componentType: source.componentType || '',
    queryType: source.queryType || '',
    dictType: source.dictType || '',
    sensitiveType: source.sensitiveType || '',
    encryptAlgorithm: source.encryptAlgorithm || '',
    sortable: !!source.sortable,
    systemField: !!source.systemField,
    readonly: !!source.readonly,
    fieldStatus: source.fieldStatus || 'ENABLED',
    referenceObjectCode: source.referenceObjectCode || '',
    referenceDisplayField: source.referenceDisplayField || '',
    placeholder: source.placeholder || source.basicProps?.placeholder || '',
    remark: source.remark || '',
    sortOrder: source.sortOrder,
    basicProps: {
      ...(source.basicProps || {}),
      placeholder: source.placeholder || source.basicProps?.placeholder || '',
    },
    advancedProps: { ...(source.advancedProps || {}), unique: !!source.unique || source.advancedProps?.unique === true },
    formulaConfig: source.formulaConfig ?? null,
  }
}

function buildFieldRenameMeta(oldFieldCode, oldField = {}, payload = {}, savedField = {}) {
  const newFieldCode = savedField.fieldCode || payload.fieldCode || oldFieldCode
  if (!oldFieldCode || !newFieldCode || oldFieldCode === newFieldCode)
    return null
  return {
    oldFieldCode,
    newFieldCode,
    oldColumnName: oldField?.columnName || payload.columnName || '',
    newColumnName: savedField.columnName || payload.columnName || '',
    oldFieldName: oldField?.fieldName || oldField?.label || oldFieldCode,
    newFieldName: savedField.fieldName || payload.fieldName || newFieldCode,
  }
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value || []))
}

defineExpose({
  saveSelectedField,
  hasDraftChanges: () => propertyPanelRef.value?.hasChanges?.() || false,
})
</script>

<style scoped>
.field-manager {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
  background: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
}

.field-asset-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--border-default, #e5e6eb);
  background: var(--n-color, var(--bg-primary, #fff));
  padding: 14px 18px;
}

.field-asset-head h2 {
  margin: 0;
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 16px;
  letter-spacing: 0;
}

.field-asset-head p {
  margin: 5px 0 0;
  color: var(--n-text-color-3, var(--text-secondary, #86909c));
  font-size: 12px;
}

.field-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.field-summary span {
  border: 1px solid var(--border-default, #e5e6eb);
  border-radius: 5px;
  background: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
  color: var(--n-text-color-3, var(--text-secondary, #86909c));
  font-size: 12px;
  line-height: 24px;
  padding: 0 8px;
  white-space: nowrap;
}

.field-summary strong {
  margin-left: 3px;
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 12px;
}

.field-asset-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--border-default, #e5e6eb);
  background: var(--n-color, var(--bg-primary, #fff));
  padding: 10px 18px;
}

.field-search {
  max-width: 320px;
}

.field-filter {
  width: 132px;
}

.system-field-toggle {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border-default, #e5e6eb);
  border-radius: 6px;
  color: var(--n-text-color-2, var(--text-regular, #4e5969));
  background: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
  font-size: 12px;
  white-space: nowrap;
}

.system-field-toggle small {
  color: var(--n-text-color-3, var(--text-secondary, #86909c));
  font-size: 11px;
}

.field-asset-workbench {
  display: grid;
  grid-template-columns: minmax(440px, 1fr) minmax(420px, 460px);
  gap: 0;
  min-height: 0;
}

.field-list-pane {
  min-width: 0;
  min-height: 0;
  border-right: 1px solid var(--border-default, #e5e6eb);
  background: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
}

.field-property-pane {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: var(--n-color, var(--bg-primary, #fff));
}

.field-property-drawer-panel {
  width: 100%;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.create-field-tip {
  margin-bottom: 14px;
  border: 1px solid var(--border-default, #e5e6eb);
  border-radius: 6px;
  background: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
  color: var(--n-text-color-3, var(--text-secondary, #86909c));
  font-size: 12px;
  line-height: 1.6;
  padding: 9px 10px;
}

@media (max-width: 980px) {
  .field-asset-head,
  .field-asset-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .field-summary {
    justify-content: flex-start;
  }

  .field-search,
  .field-filter,
  .system-field-toggle {
    width: 100%;
    max-width: none;
  }

  .field-asset-workbench {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: minmax(360px, 1fr);
  }

  .field-list-pane {
    border-right: 0;
  }
}
</style>
