<template>
  <div class="form-create-adapter">
    <div class="designer-head">
      <div>
        <div class="designer-title">
          表单与详情页
        </div>
        <div class="designer-meta">
          {{ designerMetaText }}
        </div>
      </div>
      <n-space size="small" align="center">
        <n-switch
          :value="zone?.enabled !== false"
          size="small"
          @update:value="updateEnabled"
        />
        <n-button size="small" @click="resetFromModel">
          按模型重置
        </n-button>
        <n-button size="small" type="primary" @click="applyDesignerConfig">
          应用表单配置
        </n-button>
      </n-space>
    </div>

    <div v-if="showMasterDetailPanel" class="master-detail-panel">
      <div class="master-detail-summary">
        <strong>主子表编辑</strong>
        <span>主表字段使用下方表单设计，子表字段生成明细表格</span>
      </div>
      <div v-if="childFieldGroups.length" class="child-detail-groups">
        <div v-for="group in childFieldGroups" :key="group.key" class="child-detail-group">
          <div class="child-detail-head">
            <strong>{{ group.modelName }}</strong>
            <span>{{ group.fields.length }} 个字段</span>
          </div>
          <div class="child-field-strip">
            <span
              v-for="field in group.fields"
              :key="field.field"
              class="child-field-chip"
            >
              {{ field.rawLabel || field.label || field.sourceField || field.field }}
            </span>
          </div>
        </div>
      </div>
      <n-empty v-else size="small" description="当前编辑页未配置子表字段" />
    </div>

    <div class="designer-body">
      <FcDesigner
        ref="designerRef"
        :height="designerHeight"
        :config="designerConfig"
        @create="queueFlush"
        @copy="queueFlush"
        @delete="queueFlush"
        @drag="queueFlush"
        @paste-rule="queueFlush"
        @sort-up="queueFlush"
        @sort-down="queueFlush"
        @change-field="queueFlush"
      />
    </div>
  </div>
</template>

<script setup>
import FcDesigner from '@form-create/designer'
import formCreate from '@form-create/element-ui'
import ElementPlus from 'element-plus'
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isPageFieldVisible } from './page-schema'
import 'element-plus/dist/index.css'
import '@form-create/element-ui/src/style/index.css'

const props = defineProps({
  zone: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  layoutType: {
    type: String,
    default: 'simple-crud',
  },
})

const emit = defineEmits(['update:zone'])

installDesignerPlugins()

const designerRef = ref(null)
const syncingDesigner = ref(false)
const flushTimer = ref(null)
const designerHeight = '640px'
const designerConfig = {
  showAi: false,
  showInputData: false,
  showLanguage: false,
  showDevice: false,
  showComponentName: false,
  fieldReadonly: false,
  showSaveBtn: false,
  showPreviewBtn: true,
  showJsonPreview: true,
  exitConfirm: false,
  formOptions: buildDefaultOptions(),
}

const editableFields = computed(() => props.fields.filter(field => isPageFieldVisible(field, 'edit')))
const isMasterDetailLayout = computed(() => props.layoutType === 'master-detail-crud')
const primaryEditableFields = computed(() => {
  if (!isMasterDetailLayout.value)
    return editableFields.value
  return editableFields.value.filter(field => !isChildModelField(field))
})
const childEditableFields = computed(() => {
  if (!isMasterDetailLayout.value)
    return []
  return editableFields.value.filter(field => isChildModelField(field))
})
const fieldMap = computed(() => new Map(primaryEditableFields.value.map(field => [field.field, field])))
const childFieldMap = computed(() => new Map(childEditableFields.value.map(field => [field.field, field])))
const selectedPrimaryFieldRefs = computed(() => {
  const refs = extractFieldRefs(props.zone?.props?.formCreateRule || [], fieldMap.value)
  if (refs.length)
    return refs
  return (props.zone?.fieldRefs || []).filter(ref => fieldMap.value.has(ref))
})
const selectedChildFieldRefs = computed(() => {
  if (!isMasterDetailLayout.value)
    return []
  const refs = (props.zone?.fieldRefs || []).filter(ref => childFieldMap.value.has(ref))
  if (refs.length)
    return refs
  return childEditableFields.value.map(field => field.field)
})
const selectedFieldRefs = computed(() => mergeFieldRefs(selectedPrimaryFieldRefs.value, selectedChildFieldRefs.value))
const childFieldGroups = computed(() => {
  const groups = new Map()
  selectedChildFieldRefs.value.forEach((ref) => {
    const field = childFieldMap.value.get(ref)
    if (!field)
      return
    const key = field.modelCode || 'children'
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        modelName: field.modelName || field.sourceLabel || key,
        fields: [],
      })
    }
    groups.get(key).fields.push(field)
  })
  return Array.from(groups.values())
})
const showMasterDetailPanel = computed(() => isMasterDetailLayout.value)
const designerMetaText = computed(() => {
  if (!isMasterDetailLayout.value)
    return `新增、编辑、详情共用字段 · ${selectedFieldRefs.value.length} 个业务字段`
  return `主表字段 ${selectedPrimaryFieldRefs.value.length} 个 · 子表 ${childFieldGroups.value.length} 个 / 明细字段 ${selectedChildFieldRefs.value.length} 个`
})

watch(
  () => [props.zone?.zoneKey, props.zone?.props?.formCreateRule, props.fields, props.layoutType],
  () => loadDesignerRules(),
  { deep: true },
)

onMounted(() => loadDesignerRules())

onBeforeUnmount(() => {
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
})

function loadDesignerRules() {
  if (!props.zone || props.zone.zoneKey !== 'edit')
    return
  nextTick(() => {
    if (!designerRef.value)
      return
    syncingDesigner.value = true
    const rules = resolveDesignerRules()
    designerRef.value.setRule(cloneValue(rules))
    designerRef.value.setOption(resolveDesignerOptions())
    nextTick(() => {
      syncingDesigner.value = false
    })
  })
}

function resolveDesignerRules() {
  const sourceRules = Array.isArray(props.zone?.props?.formCreateRule)
    ? props.zone.props.formCreateRule
    : []
  const refs = (props.zone?.fieldRefs?.length
    ? props.zone.fieldRefs
    : primaryEditableFields.value.map(field => field.field))
    .filter(ref => fieldMap.value.has(ref))

  if (!sourceRules.length)
    return refs.map(ref => buildRuleFromField(fieldMap.value.get(ref)))

  const normalizedRules = normalizeRules(sourceRules, fieldMap.value)
  const existingRefs = new Set(extractFieldRefs(normalizedRules, fieldMap.value))
  refs.forEach((ref) => {
    if (!existingRefs.has(ref))
      normalizedRules.push(buildRuleFromField(fieldMap.value.get(ref)))
  })
  return normalizedRules
}

function resolveDesignerOptions() {
  return {
    ...buildDefaultOptions(),
    ...(props.zone?.props?.formCreateOptions || {}),
  }
}

function queueFlush() {
  if (syncingDesigner.value)
    return
  if (flushTimer.value)
    window.clearTimeout(flushTimer.value)
  flushTimer.value = window.setTimeout(() => flushDesigner(), 160)
}

function flushDesigner() {
  if (!props.zone || !designerRef.value)
    return false
  const rules = designerRef.value.getRule?.() || []
  const options = designerRef.value.getOptions?.() || designerRef.value.getOption?.() || resolveDesignerOptions()
  const refs = mergeFieldRefs(extractFieldRefs(rules, fieldMap.value), selectedChildFieldRefs.value)
  emit('update:zone', {
    ...props.zone,
    fieldRefs: refs,
    props: {
      ...(props.zone.props || {}),
      formCreateRule: cloneValue(rules),
      formCreateOptions: cloneValue(options),
    },
  })
  return true
}

function applyDesignerConfig() {
  if (flushDesigner())
    window.$message?.success('表单与详情配置已应用')
  else
    window.$message?.warning('表单设计器尚未加载完成')
}

function updateEnabled(value) {
  if (!props.zone)
    return
  emit('update:zone', {
    ...props.zone,
    enabled: value,
  })
}

function resetFromModel() {
  if (!props.zone)
    return
  const primaryRefs = primaryEditableFields.value.map(field => field.field)
  const childRefs = childEditableFields.value.map(field => field.field)
  const refs = mergeFieldRefs(primaryRefs, childRefs)
  const rules = primaryRefs.map(ref => buildRuleFromField(fieldMap.value.get(ref)))
  emit('update:zone', {
    ...props.zone,
    fieldRefs: refs,
    props: {
      ...(props.zone.props || {}),
      formCreateRule: rules,
      formCreateOptions: buildDefaultOptions(),
    },
  })
  nextTick(loadDesignerRules)
}

function buildRuleFromField(field) {
  const label = field?.label || field?.field || '字段'
  const componentType = resolveRuleType(field)
  const rule = {
    type: componentType,
    field: field.field,
    title: label,
    name: field.field,
    props: buildRuleProps(field, label),
  }
  if (field?.required) {
    rule.validate = [{
      required: true,
      message: buildPlaceholder(field, label),
      trigger: ['blur', 'change'],
    }]
  }
  if (field?.defaultValue !== undefined && field.defaultValue !== null && field.defaultValue !== '')
    rule.value = field.defaultValue
  if (['select', 'radio', 'checkbox'].includes(componentType))
    rule.options = buildOptions(field)
  return rule
}

function resolveRuleType(field = {}) {
  if (field.componentType === 'textarea')
    return 'textarea'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return 'inputNumber'
  if (field.componentType === 'date' || field.dataType === 'date')
    return 'datePicker'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return 'datePicker'
  if (field.componentType === 'time' || field.dataType === 'time')
    return 'timePicker'
  if (field.componentType === 'switch')
    return 'switch'
  if (field.componentType === 'radio')
    return 'radio'
  if (field.componentType === 'checkbox')
    return 'checkbox'
  if (field.dictType || field.componentType === 'select')
    return 'select'
  if (['upload', 'fileUpload', 'imageUpload'].includes(field.componentType))
    return 'upload'
  return 'input'
}

function buildRuleProps(field, label) {
  const placeholder = buildPlaceholder(field, label)
  const props = {
    placeholder,
    clearable: true,
  }
  if (field.componentType === 'textarea') {
    props.type = 'textarea'
    props.rows = 3
    props.showWordLimit = true
  }
  if (field.length && ['input', 'textarea'].includes(resolveRuleType(field)))
    props.maxlength = field.length
  if (resolveRuleType(field) === 'inputNumber') {
    props.controls = false
    if (field.precision !== undefined && field.precision !== null)
      props.precision = field.precision
  }
  if (field.componentType === 'datetime' || field.dataType === 'datetime') {
    props.type = 'datetime'
    props.format = 'YYYY-MM-DD HH:mm:ss'
    props.valueFormat = 'YYYY-MM-DD HH:mm:ss'
  }
  if (field.componentType === 'date' || field.dataType === 'date') {
    props.type = 'date'
    props.format = 'YYYY-MM-DD'
    props.valueFormat = 'YYYY-MM-DD'
  }
  if (field.componentType === 'switch') {
    props.activeValue = 1
    props.inactiveValue = 0
  }
  if (field.componentType === 'imageUpload') {
    props.listType = 'picture-card'
    props.accept = 'image/*'
    props.autoUpload = false
  }
  if (field.componentType === 'fileUpload' || field.componentType === 'upload') {
    props.autoUpload = false
  }
  return props
}

function buildOptions(field) {
  if (field?.dictType) {
    return [
      { label: `${field.dictType}字典项`, value: '' },
    ]
  }
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function buildPlaceholder(field, label) {
  const type = resolveRuleType(field)
  if (['select', 'radio', 'checkbox', 'datePicker', 'timePicker', 'upload'].includes(type))
    return `请选择${label}`
  return `请输入${label}`
}

function normalizeRules(rules, fields) {
  const seenFields = new Set()
  return cloneValue(rules).map(rule => normalizeRule(rule, fields, seenFields)).filter(Boolean)
}

function isChildModelField(field = {}) {
  const sourceField = field.sourceField || field.field
  return Boolean(field.modelCode) && field.field !== sourceField
}

function mergeFieldRefs(...groups) {
  return Array.from(new Set(groups.flat().filter(Boolean)))
}

function normalizeRule(rule, fields, seenFields) {
  if (!rule || typeof rule !== 'object')
    return null
  if (rule.field && !fields.has(rule.field))
    return null
  if (rule.field && seenFields.has(rule.field))
    return null
  if (rule.field)
    seenFields.add(rule.field)
  const field = rule.field && fields.get(rule.field)
  const nextRule = field
    ? {
        ...rule,
        title: field.label || rule.title || field.field,
        name: rule.name || field.field,
        props: {
          ...buildRuleProps(field, field.label || field.field),
          ...(rule.props || {}),
        },
      }
    : { ...rule }
  if (Array.isArray(nextRule.children))
    nextRule.children = nextRule.children.map(child => normalizeRule(child, fields, seenFields)).filter(Boolean)
  return nextRule
}

function extractFieldRefs(rules, fields) {
  const refs = []
  const walk = (items) => {
    ;(items || []).forEach((rule) => {
      if (!rule || typeof rule !== 'object')
        return
      if (rule.field && fields.has(rule.field) && !refs.includes(rule.field))
        refs.push(rule.field)
      if (Array.isArray(rule.children))
        walk(rule.children)
    })
  }
  walk(rules)
  return refs
}

function buildDefaultOptions() {
  return {
    form: {
      labelPosition: 'right',
      labelWidth: '110px',
      size: 'default',
    },
    submitBtn: false,
    resetBtn: false,
  }
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}

function installDesignerPlugins() {
  const app = getCurrentInstance()?.appContext?.app
  if (!app || app._context.provides.__lowcodeFormCreateInstalled)
    return
  app.use(ElementPlus)
  app.use(formCreate)
  app._context.provides.__lowcodeFormCreateInstalled = true
}
</script>

<style scoped>
.form-create-adapter {
  min-height: 704px;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  overflow: hidden;
}

.designer-head {
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
}

.designer-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.designer-meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}

.master-detail-panel {
  display: grid;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.master-detail-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.master-detail-summary strong {
  color: var(--text-primary);
  font-size: 13px;
}

.child-detail-groups {
  display: grid;
  gap: 8px;
  max-height: 180px;
  overflow-y: auto;
}

.child-detail-group {
  display: grid;
  gap: 8px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px;
}

.child-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 12px;
}

.child-detail-head strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.child-field-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.child-field-chip {
  max-width: 180px;
  overflow: hidden;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 24px;
  padding: 0 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.designer-body {
  min-width: 0;
  min-height: 0;
  background: var(--bg-secondary);
}

:deep(.fc-designer) {
  --fc-primary: #2563eb;
  border: 0;
}

:deep(.fc-designer ._fc-l),
:deep(.fc-designer ._fc-r) {
  background: var(--bg-primary);
}
</style>
