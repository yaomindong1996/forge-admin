<template>
  <div class="field-manager">
    <section class="field-asset-head">
      <div>
        <h2>字段资产</h2>
        <p>表单拖入组件会自动沉淀为字段资产，这里只处理字段编码、存储和高级维护。</p>
      </div>
      <div class="field-metrics">
        <span><strong>{{ fieldStats.total }}</strong>全部</span>
        <span><strong>{{ fieldStats.designer }}</strong>表单生成</span>
        <span><strong>{{ fieldStats.searchable }}</strong>查询</span>
        <span><strong>{{ fieldStats.system }}</strong>系统</span>
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
      <n-button type="primary" @click="openCreateModal">
        新增字段
      </n-button>
    </section>

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

    <n-drawer
      :show="propertyVisible"
      :width="430"
      placement="right"
      :mask-closable="false"
      @update:show="handlePropertyVisibleChange"
    >
      <n-drawer-content :native-scrollbar="false" closable body-content-style="padding: 0;">
        <BusinessFieldPropertyPanel
          ref="propertyPanelRef"
          class="field-property-drawer-panel"
          :field="selectedField"
          :all-fields="visibleFields"
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
          <n-input v-model:value="createForm.fieldCode" placeholder="例如：customerLevel，留空自动生成" />
        </n-form-item>
        <n-form-item label="字段类型">
          <n-select v-model:value="createForm.fieldType" :options="fieldTypeOptions" filterable />
        </n-form-item>
        <n-form-item v-if="needsCreateDict" label="字典类型">
          <DictTypeSelect v-model:value="createForm.dictType" :fields="visibleFields" />
        </n-form-item>
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi>
            <n-checkbox v-model:checked="createForm.formVisible">
              显示在表单
            </n-checkbox>
          </n-form-item-gi>
          <n-form-item-gi>
            <n-checkbox v-model:checked="createForm.listVisible">
              显示在列表
            </n-checkbox>
          </n-form-item-gi>
          <n-form-item-gi>
            <n-checkbox v-model:checked="createForm.searchable">
              作为查询条件
            </n-checkbox>
          </n-form-item-gi>
          <n-form-item-gi>
            <n-checkbox v-model:checked="createForm.required">
              必填
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
import { computed, reactive, ref, watch } from 'vue'
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

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  fields: {
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
})

const emit = defineEmits(['updated', 'dirtyChange', 'addToForm'])

const message = useMessage()
const localFields = ref([])
const selectedFieldCode = ref('')
const keyword = ref('')
const statusFilter = ref('all')
const sourceFilter = ref('all')
const propertyPanelRef = ref(null)
const saving = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const propertyVisible = ref(false)
const createForm = reactive(createDefaultCreateForm())

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

const orderedFields = computed(() => {
  return [...localFields.value].sort((a, b) => Number(a.sortOrder ?? 0) - Number(b.sortOrder ?? 0))
})

const visibleFields = computed(() => orderedFields.value.filter(field => !isHiddenField(field)))
const filteredFields = computed(() => visibleFields.value.filter(matchesFieldFilters))
const usedFieldCodes = computed(() => extractForgeSchemaFieldRefs(props.formDesignerSchema || {}))
const selectedField = computed(() => visibleFields.value.find(field => field.fieldCode === selectedFieldCode.value) || null)
const needsCreateDict = computed(() => ['DICT', 'RADIO', 'CHECKBOX'].includes(createForm.fieldType))
const fieldStats = computed(() => {
  const fields = visibleFields.value
  return {
    total: fields.length,
    designer: fields.filter(isDesignerField).length,
    searchable: fields.filter(field => field.searchable).length,
    system: fields.filter(field => field.systemField).length,
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
  { label: '系统字段', value: 'system' },
]

watch(
  () => props.fields,
  (value) => {
    localFields.value = cloneValue(value || [])
    if (selectedFieldCode.value && !visibleFields.value.some(field => field.fieldCode === selectedFieldCode.value))
      closePropertyPanel(true)
  },
  { immediate: true, deep: true },
)

function selectField(field) {
  const nextCode = field?.fieldCode || ''
  if (!nextCode)
    return
  if (nextCode === selectedFieldCode.value && propertyVisible.value)
    return
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

function handlePropertyVisibleChange(show) {
  if (show) {
    propertyVisible.value = true
    return
  }
  requestClosePropertyPanel()
}

function requestClosePropertyPanel() {
  if (propertyPanelRef.value?.hasChanges?.()) {
    window.$dialog.warning({
      title: '未保存字段变更',
      content: '关闭属性面板会放弃当前字段尚未保存的修改。',
      positiveText: '放弃修改',
      negativeText: '继续编辑',
      onPositiveClick: () => closePropertyPanel(true),
    })
    return
  }
  closePropertyPanel(false)
}

function closePropertyPanel(discardChanges = false) {
  if (discardChanges)
    propertyPanelRef.value?.resetForm?.()
  propertyVisible.value = false
  selectedFieldCode.value = ''
  emit('dirtyChange', false)
}

function openCreateModal() {
  Object.assign(createForm, createDefaultCreateForm())
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
    await reloadFields()
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
  await reloadFields()
  if (createdCode)
    openPropertyPanel(createdCode)
}

async function patchField(field, patch) {
  if (!field?.fieldCode || !props.objectId)
    return
  saving.value = true
  try {
    const res = await updateBusinessObjectField(props.objectId, field.fieldCode, normalizeFieldPayload({ ...field, ...patch }))
    mergeField(res.data || { ...field, ...patch })
    emit('updated', orderedFields.value)
  }
  finally {
    saving.value = false
  }
}

async function saveField(payload) {
  const fieldCode = selectedField.value?.fieldCode
  if (!fieldCode || !props.objectId)
    return false
  if (selectedField.value?.systemField) {
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
  saving.value = true
  try {
    const res = await updateBusinessObjectField(props.objectId, fieldCode, normalizeFieldPayload(payload))
    const savedField = res.data || payload
    mergeField(savedField)
    if (savedField.fieldCode && savedField.fieldCode !== selectedFieldCode.value)
      selectedFieldCode.value = savedField.fieldCode
    propertyPanelRef.value?.resetForm?.()
    emit('dirtyChange', false)
    message.success('字段已保存')
    await reloadFields()
    return true
  }
  finally {
    saving.value = false
  }
}

async function saveSelectedField() {
  if (!selectedField.value) {
    message.warning('请先选择需要保存的字段')
    return false
  }
  if (!propertyVisible.value) {
    openPropertyPanel(selectedField.value.fieldCode)
    message.info('字段属性面板已打开，修改后再保存')
    return false
  }
  return saveField(propertyPanelRef.value?.getPayload?.() || {})
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
    closePropertyPanel(true)
  await reloadFields()
}

async function moveField(from, to) {
  if (from === to || from < 0 || to < 0 || to >= visibleFields.value.length)
    return
  const nextVisible = [...visibleFields.value]
  const [item] = nextVisible.splice(from, 1)
  nextVisible.splice(to, 0, item)
  const hiddenFields = orderedFields.value.filter(isHiddenField)
  const next = [...nextVisible, ...hiddenFields]
  localFields.value = next.map((field, index) => ({ ...field, sortOrder: index + 1 }))
  if (!props.objectId)
    return
  await sortBusinessObjectFields(props.objectId, localFields.value.map(field => field.fieldCode).filter(Boolean))
  emit('updated', orderedFields.value)
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

async function reloadFields() {
  if (!props.objectId)
    return
  const res = await businessObjectFields(props.objectId)
  localFields.value = cloneValue(res.data || [])
  if (selectedFieldCode.value && !visibleFields.value.some(field => field.fieldCode === selectedFieldCode.value))
    closePropertyPanel(true)
  emit('updated', orderedFields.value)
}

function mergeField(field) {
  const index = localFields.value.findIndex(item => item.fieldCode === field.fieldCode)
  if (index >= 0)
    localFields.value.splice(index, 1, { ...localFields.value[index], ...field })
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
  if (sourceFilter.value === 'system' && !field.systemField)
    return false
  return true
}

function isDesignerField(field = {}) {
  const binding = field.fieldBinding || field.basicProps?.fieldBinding || {}
  return binding.source === 'designer' || binding.createIfMissing === true
}

function normalizeFieldPayload(source) {
  return {
    fieldName: source.fieldName,
    fieldCode: source.fieldCode || '',
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
    advancedProps: { ...(source.advancedProps || {}) },
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
  background: #fbfcfe;
}

.field-asset-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  padding: 16px 18px;
}

.field-asset-head h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  letter-spacing: 0;
}

.field-asset-head p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 12px;
}

.field-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(72px, auto));
  gap: 8px;
}

.field-metrics span {
  display: grid;
  justify-items: end;
  gap: 2px;
  color: #64748b;
  font-size: 12px;
}

.field-metrics strong {
  color: #111827;
  font-size: 18px;
  line-height: 22px;
}

.field-asset-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  padding: 12px 18px;
}

.field-search {
  max-width: 320px;
}

.field-filter {
  width: 132px;
}

.field-property-drawer-panel {
  min-height: calc(100vh - 64px);
}

@media (max-width: 980px) {
  .field-asset-head,
  .field-asset-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .field-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .field-metrics span {
    justify-items: start;
  }

  .field-search,
  .field-filter {
    width: 100%;
    max-width: none;
  }
}
</style>
