<template>
  <div class="ai-crud-page-wrapper">
    <ListPageGridDesigner
      v-if="shouldRenderRuntimeGrid"
      class="runtime-list-grid"
      :model-value="runtimeGridLayout"
      :fields="runtimeFields"
      :model-schema="renderConfig?.modelSchema || {}"
      :layout-type="runtimeEffectiveLayoutType"
      :page-name="resolveRuntimeTitle(renderConfig)"
      readonly
      :runtime-crud-props="crudProps"
      :runtime-record="runtimeDetailRecord"
    />
    <component
      :is="currentTemplate"
      v-else-if="configLoaded && currentTemplate"
      ref="runtimeCrudRef"
      :crud-props="crudProps"
    />
    <AiCrudPage
      v-else-if="configLoaded && !currentTemplate"
      ref="runtimeCrudRef"
      v-bind="crudProps"
    />
    <div v-else-if="loading" class="loading-wrapper">
      <n-spin size="large" description="加载配置中..." />
    </div>
    <div v-else-if="errorMsg" class="error-wrapper">
      <n-result status="error" :title="errorMsg">
        <template #footer>
          <n-button @click="loadConfig">
            重新加载
          </n-button>
        </template>
      </n-result>
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, h, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { crudConfigRender } from '@/api/ai'
import { businessDocumentRuntime } from '@/api/business-app'
import catalog from '@/catalog'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import DictTag from '@/components/DictTag.vue'
import { applyCrudHookRules, CRUD_HOOK_RULE_TARGETS, normalizeCrudHookRules } from '@/components/lowcode-builder/page/crud-hook-rules'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import { getDictData } from '@/composables/useDict'
import { useTabStore } from '@/store'
import { postEncrypt, request } from '@/utils'
import { getDefaultPageTitle } from '@/utils/page-title'
import { normalizeMultiFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'

const route = useRoute()
const router = useRouter()
const tabStore = useTabStore()

const loading = ref(false)
const configLoaded = ref(false)
const errorMsg = ref('')
const renderConfig = ref(null)
const dictCache = ref({})
const runtimeCrudRef = ref(null)
const lastInitialActionKey = ref('')
const runtimeDetailRecord = ref({})
const runtimeDetailLoading = ref(false)
const runtimeOpenMode = computed(() => String(route.query?.runtimeOpenMode || '').toUpperCase())
const formOnlyRuntime = computed(() => runtimeOpenMode.value === 'CREATE_FORM')

/** 当前加载的模板组件（null 表示降级到 AiCrudPage） */
const currentTemplate = ref(null)

const activeRuntimePageKey = computed(() => String(route.query?.pageKey || 'list').trim() || 'list')
const activeRuntimeFormKey = computed(() => String(route.query?.formKey || '').trim())
const runtimePages = computed(() => {
  const pages = renderConfig.value?.pageSchema?.pages
  return Array.isArray(pages) ? pages : []
})
const activeRuntimePage = computed(() => {
  const key = activeRuntimePageKey.value
  return runtimePages.value.find(page => page?.pageKey === key) || null
})
const activeRuntimeGridLayout = computed(() => {
  const pageSchema = renderConfig.value?.pageSchema || {}
  const page = activeRuntimePage.value
  if (page?.gridLayout && Array.isArray(page.gridLayout.items))
    return page.gridLayout
  if (activeRuntimePageKey.value !== 'list')
    return null
  const layout = pageSchema.listGridLayout
  if (!layout || !Array.isArray(layout.items))
    return null
  return layout
})
const runtimeGridLayout = computed(() => {
  const layout = activeRuntimeGridLayout.value
  if (!layout)
    return null
  const pageSchema = renderConfig.value?.pageSchema || {}
  return {
    ...layout,
    layoutType: layout.layoutType || pageSchema.layoutType || renderConfig.value?.layoutType || 'simple-crud',
  }
})
// 标准列表入口必须渲染真实 CRUD 组件；listGridLayout 只是设计态/自定义页布局元数据。
const standardListRuntime = computed(() => activeRuntimePageKey.value === 'list' && !formOnlyRuntime.value)
const shouldRenderRuntimeGrid = computed(() => (
  configLoaded.value
  && runtimeGridLayout.value
  && !formOnlyRuntime.value
  && !standardListRuntime.value
))
const runtimeEffectiveLayoutType = computed(() => (
  runtimeGridLayout.value?.layoutType
  || renderConfig.value?.pageSchema?.layoutType
  || renderConfig.value?.layoutType
  || 'simple-crud'
))

const runtimeFields = computed(() => {
  const fields = renderConfig.value?.modelSchema?.fields
  return Array.isArray(fields) ? fields.map(normalizeRuntimeField).filter(field => field.field) : []
})

const runtimeColumnSettings = computed(() => {
  const settings = {}
  const items = activeRuntimeGridLayout.value?.items
  if (!Array.isArray(items)) {
    return settings
  }
  ;['data-table', 'AiCrudPage', 'AiTable'].forEach((blockType) => {
    const block = items.find(item => item?.blockType === blockType)
    const fieldSettings = block?.props?.fieldSettings
    if (fieldSettings && typeof fieldSettings === 'object') {
      Object.assign(settings, fieldSettings)
    }
  })
  return settings
})
const runtimeAiCrudBlockProps = computed(() => {
  const items = activeRuntimeGridLayout.value?.items
  if (!Array.isArray(items))
    return {}
  return items.find(item => item?.blockType === 'AiCrudPage')?.props || {}
})
const activeRuntimeFormProfile = computed(() => buildRuntimeFormProfile(renderConfig.value, activeRuntimeFormKey.value))
const routeEntryPublicQuery = computed(() => extractRouteEntryPublicQuery(route.query || {}))
const routeEntryFormDefaultValues = computed(() => parseRouteRecordParam(route.query?.formDefaultValues))
const routeEntrySubmitDefaultParams = computed(() => parseRouteRecordParam(route.query?.submitDefaultParams))

function normalizeRuntimeField(field = {}) {
  return {
    ...field,
    field: field.field || field.fieldCode || field.columnName || '',
    label: field.label || field.fieldName || field.field || field.fieldCode || field.columnName || '',
    componentType: field.componentType || field.componentKey || field.dataType || 'input',
  }
}

const RUNTIME_ROUTE_PARAM_KEYS = new Set([
  'appId',
  'menuKey',
  'menuResourceId',
  'runtimeOpenMode',
  'pageKey',
  'formKey',
  'mode',
  'title',
  'configKey',
  'id',
  'recordId',
  'formDefaultValues',
  'submitDefaultParams',
])

function extractRouteEntryPublicQuery(query = {}) {
  return Object.entries(query).reduce((result, [key, value]) => {
    if (!key || RUNTIME_ROUTE_PARAM_KEYS.has(key))
      return result
    const normalizedValue = normalizeRouteParamValue(value)
    if (normalizedValue !== undefined && normalizedValue !== '')
      result[key] = normalizedValue
    return result
  }, {})
}

function normalizeRouteParamValue(value) {
  if (Array.isArray(value))
    return value.length > 1 ? value : value[0]
  if (value === null || value === undefined)
    return undefined
  return value
}

function parseRouteRecordParam(value) {
  const normalizedValue = normalizeRouteParamValue(value)
  if (!normalizedValue)
    return {}
  if (typeof normalizedValue === 'object' && !Array.isArray(normalizedValue))
    return normalizedValue
  if (typeof normalizedValue !== 'string')
    return {}
  try {
    const parsed = JSON.parse(normalizedValue)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

/**
 * 转换表格列配置：将 JSON 格式的 render 对象转为 Vue render 函数
 * 如果配置了 transConfig，则使用翻译后的 xxxName 字段直接显示文本
 */
function transformColumns(columns, transConfig, options = {}) {
  // 构建翻译映射: { field -> targetField }
  const transMap = {}
  if (transConfig && typeof transConfig === 'object') {
    for (const [field, conf] of Object.entries(transConfig)) {
      transMap[field] = conf.targetField || (`${field}Name`)
    }
  }

  let treeColumnApplied = false
  const isActionColumnKey = key => ['actions', 'action', 'operations', 'operation'].includes(String(key || ''))
  const result = (columns || []).map((col) => {
    // 统一提取字段名，优先级：prop > key > dataIndex
    const key = col.prop || col.key || col.dataIndex
    const columnSetting = key ? options.columnSettings?.[key] || {} : {}
    // 统一补prop字段，AiTable需要这个字段来匹配数据
    const newCol = { ...col, ...columnSetting, prop: key }
    if (isActionColumnKey(key)) {
      const mergedActions = mergeRowActions(Array.isArray(col.actions) ? col.actions : [], options.rowActions || [])
      if (mergedActions.length) {
        newCol.actions = options.includeDetailAction ? ensureDetailRowAction(mergedActions) : mergedActions
        newCol.width = Math.max(Number(col.width) || 0, newCol.actions.length * 58, 180)
      }
    }
    else if (Array.isArray(col.actions) && options.includeDetailAction) {
      newCol.actions = ensureDetailRowAction(col.actions)
      newCol.width = Math.max(Number(col.width) || 0, newCol.actions.length * 58, 180)
    }
    if (options.treeTable && !treeColumnApplied && key && !isActionColumnKey(key)) {
      newCol.tree = true
      treeColumnApplied = true
    }

    if (options.fitTableToContainer && !newCol.fixed && !isActionColumnKey(key)) {
      delete newCol.width
      delete newCol.minWidth
      delete newCol.maxWidth
    }

    let baseRender = null
    // dictTag 渲染
    if (col.render && typeof col.render === 'object' && col.render.type === 'dictTag') {
      baseRender = row => h(DictTag, {
        dictType: col.render.dictType,
        value: row[key],
        size: 'small',
      })
    }
    else if (col.render && typeof col.render === 'object' && col.render.type === 'relationName') {
      const targetField = col.render.targetField || `${key}Name`
      baseRender = row => row[targetField] ?? row[key] ?? '-'
    }
    else if (col.render && typeof col.render === 'object' && ['orgName', 'userName', 'regionName', 'fileUpload'].includes(col.render.type)) {
      const targetField = col.render.targetField || `${key}Name`
      baseRender = row => row[targetField] ?? row[key] ?? '-'
    }
    // 如果该字段有翻译配置，优先显示翻译后的值，没有则显示原字段值
    else if (transMap[key]) {
      const targetField = transMap[key]
      baseRender = row => row[targetField] ?? row[key]
    }
    if (baseRender)
      newCol.render = baseRender
    applyRuntimeColumnPresentation(newCol, newCol, key)
    return newCol
  })

  const rowActions = normalizeRuntimePageActions(options.rowActions || [], 'row')
  const hasActionColumn = result.some((col) => {
    const key = col.prop || col.key || col.dataIndex
    return isActionColumnKey(key)
  })
  if (!hasActionColumn && rowActions.length) {
    result.push({
      key: 'actions',
      title: '操作',
      dataIndex: 'actions',
      prop: 'actions',
      width: Math.max(180, rowActions.length * 58),
      fixed: 'right',
      actions: options.includeDetailAction ? ensureDetailRowAction(rowActions) : rowActions,
      maxActionButtons: 3,
    })
  }

  return result
}

function applyRuntimeColumnPresentation(targetCol, sourceCol = {}, key = '') {
  const hasTextColor = !!sourceCol.textColor
  const isNavigable = sourceCol.clickAction === 'navigate'
  if (!hasTextColor && !isNavigable)
    return
  const baseRender = typeof targetCol.render === 'function'
    ? targetCol.render
    : row => row[key] ?? '-'
  targetCol.render = (row) => {
    const content = baseRender(row)
    const style = hasTextColor ? { color: sourceCol.textColor } : {}
    if (!isNavigable)
      return h('span', { style }, content)
    const children = Array.isArray(content) ? content : [content]
    return h('a', {
      class: 'runtime-column-link',
      style,
      href: buildRuntimeColumnTarget(sourceCol, row),
      onClick: (event) => {
        event.preventDefault()
        const target = buildRuntimeColumnRoute(sourceCol, row)
        if (target)
          router.push(target)
      },
    }, children)
  }
}

function buildRuntimeColumnRoute(col = {}, row = {}) {
  const configKey = renderConfig.value?.configKey || resolveRouteConfigKey()
  if (!configKey)
    return null
  const paramName = col.targetParamName || 'id'
  const paramField = col.targetParamField || 'id'
  const paramValue = row[paramField] ?? row.id
  const query = {
    ...route.query,
    pageKey: col.targetPageKey || 'detail',
    formKey: col.targetFormKey || route.query?.formKey || undefined,
    [paramName]: paramValue,
  }
  if (!query.formKey)
    delete query.formKey
  if ((col.targetPageKey || 'detail') === 'detail') {
    query.mode = 'detail'
    query.recordId = paramValue
  }
  return {
    path: `/ai/crud-page/${encodeURIComponent(configKey)}`,
    query,
  }
}

function buildRuntimeColumnTarget(col = {}, row = {}) {
  const target = buildRuntimeColumnRoute(col, row)
  if (!target)
    return '#'
  return router.resolve(target).href
}

function mergeRowActions(baseActions = [], extraActions = []) {
  const next = [...baseActions]
  const existingKeys = new Set(next.map(action => String(action?.key || action?.actionCode || '').toLowerCase()).filter(Boolean))
  normalizeRuntimePageActions(extraActions, 'row').forEach((action) => {
    const key = String(action.key || '').toLowerCase()
    if (!key || existingKeys.has(key))
      return
    existingKeys.add(key)
    next.push(action)
  })
  return next
}

function ensureDetailRowAction(actions = []) {
  if (actions.some(action => action?.key === 'detail'))
    return actions
  const next = [...actions]
  const editIndex = next.findIndex(action => action?.key === 'edit')
  const detailAction = { key: 'detail', label: '查看详情', type: 'info', position: 'row' }
  if (editIndex >= 0) {
    next.splice(editIndex + 1, 0, detailAction)
    return next
  }
  next.unshift(detailAction)
  return next
}

function normalizeRuntimePageActions(actions = [], position = 'row') {
  if (!Array.isArray(actions))
    return []
  return actions
    .map(action => normalizeRuntimePageAction(action, position))
    .filter(Boolean)
}

function normalizeRuntimePageAction(action = {}, position = 'row') {
  if (!action || action.visible === false || action.status === 0)
    return null
  const actionPosition = normalizeActionPosition(action.position || action.actionPosition || position)
  if (actionPosition !== position)
    return null
  const actionType = normalizeActionType(action.actionType || action.type)
  const config = action.actionConfig || {}
  const routePath = action.routePath
    || config.targetPath
    || config.routePath
    || config.url
    || ''
  const key = action.key || action.actionCode || action.actionName || action.label
  if (!key)
    return null
  const label = action.label || action.actionName || key
  if (position === 'toolbar' && isBuiltinCreateToolbarAction(key, label, actionType, routePath))
    return null
  return {
    ...action,
    key,
    label,
    type: resolveRuntimeButtonType(action, actionType),
    position: actionPosition,
    actionType,
    routePath,
    targetFormKey: action.targetFormKey || config.targetFormKey || '',
    openTarget: action.openTarget || config.openTarget || (actionType === 'external' ? '_blank' : '_self'),
    confirmText: action.confirmText || (action.confirmRequired ? `确认执行“${action.actionName || action.label || key}”？` : ''),
  }
}

function isBuiltinCreateToolbarAction(key, label, actionType, routePath) {
  if (actionType !== 'route' || routePath)
    return false
  const normalizedKey = normalizeActionIdentity(key)
  const normalizedLabel = normalizeActionIdentity(label)
  return ['add', 'create', 'new', '新增', '新建'].includes(normalizedKey)
    || ['add', 'create', 'new', '新增', '新建'].includes(normalizedLabel)
}

function normalizeActionIdentity(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[-_\s]+/g, '')
}

function normalizeActionPosition(value) {
  const text = String(value || 'row').toLowerCase()
  if (text === 'toolbar')
    return 'toolbar'
  if (text === 'detail')
    return 'detail'
  return 'row'
}

function normalizeActionType(value) {
  const text = String(value || 'route')
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace('-', '_')
    .toUpperCase()
  if (text === 'START_FLOW')
    return 'START_FLOW'
  if (text === 'OPEN_EXTERNAL' || text === 'EXTERNAL')
    return 'external'
  if (text === 'OPEN_PAGE' || text === 'ROUTE')
    return 'route'
  return text || 'route'
}

function resolveRuntimeButtonType(action = {}, actionType = '') {
  if (action.buttonType)
    return action.buttonType
  if (action.type && !['OPEN_PAGE', 'OPEN_EXTERNAL', 'START_FLOW', 'CALL_API', 'TRIGGER', 'route', 'external'].includes(action.type))
    return action.type
  if (actionType === 'START_FLOW')
    return 'success'
  if (actionType === 'external')
    return 'info'
  if (['TRIGGER', 'CALL_API'].includes(actionType))
    return 'warning'
  return 'primary'
}

/**
 * 转换表单字段配置：为 dictType 字段注入字典选项，为日期字段配置格式化
 */
function transformFields(fields, fieldMetaMap = new Map()) {
  return (fields || []).map((field) => {
    const newField = { ...field }
    applyRuntimeFieldMeta(newField, fieldMetaMap.get(field.field || field.fieldCode))

    if (field.dictType && ['select', 'radio', 'checkbox'].includes(field.type)) {
      const options = dictCache.value[field.dictType] || []
      newField.props = {
        ...(newField.props || {}),
        options,
      }
    }

    // 数字类型字段自动转换类型
    if (['number', 'inputNumber'].includes(field.type)) {
      newField.onMounted = (vm) => {
        if (vm.field && vm.value) {
          // 如果值是字符串，尝试转换成数字
          if (typeof vm.value === 'string') {
            const num = Number.parseFloat(vm.value)
            if (!Number.isNaN(num)) {
              vm.value = num
            }
          }
        }
      }
    }

    const timeProps = resolveDateTimeProps(field.type)
    if (timeProps) {
      newField.props = {
        ...(newField.props || {}),
        ...timeProps,
      }
    }

    return newField
  })
}

function buildRuntimeFormProfile(cfg = {}, requestedFormKey = '') {
  const baseEditSchema = Array.isArray(cfg?.editSchema) ? cfg.editSchema : []
  const formDesignerSchema = cfg?.options?.formDesignerSchema || cfg?.formDesignerSchema
  if (!formDesignerSchema)
    return { editSchema: baseEditSchema, editFormLayout: cfg?.options?.editFormLayout, formAssets: cfg?.options?.formAssets || cfg?.formAssets || [], governance: {} }
  const multiSchema = normalizeMultiFormDesignerSchema(formDesignerSchema)
  const selectedForm = resolveRuntimeForm(multiSchema, requestedFormKey)
  if (!selectedForm?.schema)
    return { editSchema: baseEditSchema, editFormLayout: cfg?.options?.editFormLayout, formAssets: [], governance: {} }
  const governance = normalizeFormGovernance(selectedForm.schema.settings?.governance || selectedForm.schema.governance)
  const baseFieldMap = new Map(baseEditSchema.map(field => [field.field, field]))
  const components = flattenDesignerComponents(selectedForm.schema.components || [])
  const editSchema = components
    .map(component => buildRuntimeFieldFromDesignerComponent(component, baseFieldMap))
    .filter(Boolean)
  return {
    editSchema: applyRuntimeFormGovernance(editSchema.length ? editSchema : baseEditSchema, governance),
    editFormLayout: buildRuntimeFormLayoutFromDesignerComponents(selectedForm.schema.components || []),
    formAssets: buildRuntimeFormAssets(multiSchema, selectedForm.formKey),
    governance,
  }
}

function normalizeFormGovernance(value = {}) {
  if (!value || typeof value !== 'object')
    return {}
  return {
    permission: value.permission && typeof value.permission === 'object' ? value.permission : {},
    fieldRules: Array.isArray(value.fieldRules) ? value.fieldRules : [],
    events: Array.isArray(value.events) ? value.events : [],
  }
}

function applyRuntimeFormGovernance(fields = [], governance = {}) {
  const permission = governance.permission || {}
  if (permission.visible === false)
    return []
  const ruleMap = new Map((governance.fieldRules || [])
    .filter(rule => rule?.field)
    .map(rule => [rule.field, rule]))

  return (Array.isArray(fields) ? fields : [])
    .map(field => applyRuntimeFieldGovernance(field, ruleMap.get(field?.field), permission))
    .filter(Boolean)
}

function applyRuntimeFieldGovernance(field = {}, rule = {}, permission = {}) {
  if (!field?.field)
    return field
  if (rule?.hidden === true)
    return null
  const next = { ...field, props: { ...(field.props || {}) } }
  if (Object.prototype.hasOwnProperty.call(rule, 'required')) {
    next.required = !!rule.required
    if (!next.required && Array.isArray(next.rules))
      next.rules = next.rules.filter(item => !item?.required)
  }
  if (Object.prototype.hasOwnProperty.call(rule, 'defaultValue'))
    next.defaultValue = rule.defaultValue
  const readonly = permission.editable === false
    ? true
    : Object.prototype.hasOwnProperty.call(rule, 'readonly')
      ? !!rule.readonly
      : next.readonly
  if (readonly) {
    next.readonly = true
    next.disabled = true
    next.props.readonly = true
    next.props.disabled = true
  }
  return next
}

function resolveRuntimeForm(multiSchema = {}, requestedFormKey = '') {
  const forms = Array.isArray(multiSchema.forms) ? multiSchema.forms : []
  if (!forms.length)
    return null
  return forms.find(form => form.formKey === requestedFormKey)
    || forms.find(form => form.formKey === multiSchema.defaultFormKey)
    || forms[0]
}

function flattenDesignerComponents(components = []) {
  const result = []
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component || typeof component !== 'object')
      return
    result.push(component)
    result.push(...flattenDesignerComponents(component.children || []))
  })
  return result
}

function buildRuntimeFieldFromDesignerComponent(component = {}, baseFieldMap = new Map()) {
  const fieldCode = component.fieldBinding?.fieldCode
  if (!fieldCode)
    return null
  const visibility = component.visibility || {}
  if (visibility.hidden === true)
    return null
  const base = baseFieldMap.get(fieldCode) || { field: fieldCode, type: 'input', label: fieldCode }
  const props = { ...(base.props || {}), ...(component.props || {}) }
  const validation = component.validation || {}
  return {
    ...base,
    field: fieldCode,
    label: component.label || base.label || fieldCode,
    type: normalizeDesignerRuntimeFieldType(component.componentKey || base.type),
    required: validation.required ?? base.required,
    readonly: visibility.readonly ?? base.readonly,
    disabled: visibility.readonly ?? base.disabled,
    defaultValue: props.defaultValue ?? base.defaultValue,
    dictType: props.dictType || base.dictType,
    props,
  }
}

function normalizeDesignerRuntimeFieldType(componentKey = '') {
  const key = String(componentKey || '').trim()
  const map = {
    inputNumber: 'number',
    integer: 'number',
    money: 'number',
    dictSelect: 'select',
    orgTreeSelect: 'treeSelect',
    deptTreeSelect: 'treeSelect',
    departmentTreeSelect: 'treeSelect',
    regionTreeSelect: 'treeSelect',
    userSelect: 'select',
    imageUpload: 'imageUpload',
    fileUpload: 'fileUpload',
  }
  return map[key] || key || 'input'
}

function buildRuntimeFormLayoutFromDesignerComponents(components = []) {
  return (Array.isArray(components) ? components : [])
    .map(component => buildRuntimeFormLayoutNode(component))
    .filter(Boolean)
}

function buildRuntimeFormLayoutNode(component = {}) {
  if (!component || typeof component !== 'object')
    return null
  const fieldCode = component.fieldBinding?.fieldCode
  if (fieldCode) {
    return {
      nodeType: 'field',
      key: component.id || fieldCode,
      field: fieldCode,
      span: component.layout?.span,
      gridStyle: component.layout?.gridStyle,
    }
  }
  const children = buildRuntimeFormLayoutFromDesignerComponents(component.children || [])
  const node = {
    nodeType: component.componentKey || component.nodeType || 'groupTitle',
    componentKey: component.componentKey,
    key: component.id,
    label: component.label,
    props: component.props || {},
    span: component.layout?.span,
    align: component.layout?.align,
    style: component.style,
    children,
  }
  if (!children.length && !isStandaloneRuntimeLayoutNode(node))
    return null
  return node
}

function buildRuntimeFormAssets(multiSchema = {}, activeFormKey = '') {
  const forms = Array.isArray(multiSchema.forms) ? multiSchema.forms : []
  return forms
    .filter(form => form?.formKey && form.formKey !== activeFormKey)
    .map(form => ({
      formKey: form.formKey,
      formName: form.formName || form.formKey,
      usage: form.usage || [],
      schema: form.schema || {},
    }))
}

function transformEditFields(fields = [], layout = [], fieldMetaMap = new Map()) {
  const transformedFields = transformFields(fields, fieldMetaMap)
  if (!Array.isArray(layout) || !layout.length)
    return transformedFields

  const fieldMap = new Map(transformedFields.map(field => [field.field, field]))
  const usedFields = new Set()
  const nodes = layout
    .map(node => hydrateRuntimeLayoutNode(node, fieldMap, usedFields))
    .filter(Boolean)

  transformedFields.forEach((field) => {
    if (field.field && !usedFields.has(field.field))
      nodes.push(field)
  })
  return nodes
}

function hydrateRuntimeLayoutNode(node = {}, fieldMap, usedFields) {
  if (!node || typeof node !== 'object')
    return null
  const nodeType = resolveRuntimeLayoutNodeType(node)
  if (node.nodeType === 'field') {
    const field = fieldMap.get(node.field)
    if (!field)
      return null
    usedFields.add(node.field)
    return {
      ...field,
      nodeType: 'field',
      key: node.key || field.field,
      span: node.span || field.span,
      gridStyle: node.gridStyle || field.gridStyle,
    }
  }

  const children = (node.children || [])
    .map(child => hydrateRuntimeLayoutNode(child, fieldMap, usedFields))
    .filter(Boolean)
  if (!children.length && !isStandaloneRuntimeLayoutNode({ ...node, nodeType }))
    return null
  return {
    ...node,
    nodeType,
    children,
  }
}

function resolveRuntimeLayoutNodeType(node = {}) {
  if (isGroupTitleRuntimeLayoutNode(node))
    return 'groupTitle'
  if (isLegacyGroupTitleRuntimeLayoutNode(node))
    return 'groupTitle'
  if (isSectionTitleRuntimeLayoutNode(node))
    return 'divider'
  if (isActionRuntimeLayoutNode(node))
    return node.componentKey || node.type || node.nodeType
  return node.nodeType
}

function isGroupTitleRuntimeLayoutNode(node = {}) {
  return ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
    .includes(node.componentKey || node.type || node.nodeType)
}

function isSectionTitleRuntimeLayoutNode(node = {}) {
  return ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
    .includes(node.componentKey || node.type || node.nodeType)
}

function isLegacyGroupTitleRuntimeLayoutNode(node = {}) {
  const props = node.props || {}
  return node.nodeType === 'divider'
    && !node.componentKey
    && Object.prototype.hasOwnProperty.call(props, 'description')
    && !Object.prototype.hasOwnProperty.call(props, 'title')
}

function isStandaloneRuntimeLayoutNode(node = {}) {
  return isSectionTitleRuntimeLayoutNode(node) || isGroupTitleRuntimeLayoutNode(node) || isActionRuntimeLayoutNode(node)
}

function isActionRuntimeLayoutNode(node = {}) {
  return ['button', 'table', 'tableGrid', 'AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock']
    .includes(node.componentKey || node.type || node.nodeType)
}

function resolveDateTimeProps(type) {
  switch (String(type || '').toLowerCase()) {
    case 'date':
    case 'daterange':
      return { format: 'yyyy-MM-dd', valueFormat: 'yyyy-MM-dd' }
    case 'datetime':
    case 'datetimerange':
      return { format: 'yyyy-MM-dd HH:mm:ss', valueFormat: 'yyyy-MM-dd HH:mm:ss' }
    case 'time':
    case 'timerange':
      return { format: 'HH:mm:ss', valueFormat: 'HH:mm:ss' }
    default:
      return null
  }
}

const crudProps = computed(() => {
  if (!renderConfig.value)
    return {}
  const cfg = renderConfig.value
  const options = cfg.options || {}
  const treeTable = isTreeTableRuntime(cfg, runtimeEffectiveLayoutType.value)
  const treeConfig = options.treeConfig || {}
  const gridCrudProps = runtimeAiCrudBlockProps.value || {}
  const treeLoadMode = resolveTreeLoadMode(treeConfig)
  const defaultSortParams = resolveDefaultSortParams(options.defaultSort)
  const crudHookHandlers = buildCrudHookHandlers(normalizeCrudHookRules(
    options.crudHookRules || cfg.crudHookRules || gridCrudProps.crudHookRules || {},
    options.beforeSubmitRules || cfg.beforeSubmitRules || gridCrudProps.beforeSubmitRules || [],
  ))
  const governanceEventHandlers = buildFormGovernanceEventHandlers(activeRuntimeFormProfile.value.governance?.events || [])
  const runtimeHookHandlers = composeRuntimeHookHandlers(crudHookHandlers, governanceEventHandlers)
  const configuredPublicParams = {
    ...(options.publicParams || cfg.publicParams || {}),
    ...(gridCrudProps.publicParams || {}),
  }
  const configuredPublicQuery = {
    ...(options.publicQuery || cfg.publicQuery || {}),
    ...(gridCrudProps.publicQuery || {}),
    ...routeEntryPublicQuery.value,
  }
  const formDefaultValues = {
    ...(options.formDefaultValues || cfg.formDefaultValues || {}),
    ...(gridCrudProps.formDefaultValues || {}),
    ...routeEntryFormDefaultValues.value,
  }
  const submitDefaultParams = {
    ...(options.submitDefaultParams || cfg.submitDefaultParams || {}),
    ...(gridCrudProps.submitDefaultParams || {}),
    ...routeEntrySubmitDefaultParams.value,
  }
  const apiConfig = treeTable
    ? { ...(cfg.apiConfig || {}), list: cfg.apiConfig?.tree || cfg.apiConfig?.list }
    : cfg.apiConfig || {}
  const masterDetailConfig = options.masterDetailConfig || {}
  return {
    searchSchema: transformFields(cfg.searchSchema),
    columns: transformColumns(cfg.columnsSchema, cfg.transConfig, {
      treeTable,
      includeDetailAction: true,
      rowActions: options.rowActions,
      columnSettings: runtimeColumnSettings.value,
      fitTableToContainer: shouldRenderRuntimeGrid.value,
    }),
    editSchema: transformEditFields(activeRuntimeFormProfile.value.editSchema, activeRuntimeFormProfile.value.editFormLayout || options.editFormLayout, buildRuntimeFieldMetaMap(cfg.modelSchema)),
    childrenConfig: transformChildrenConfig(masterDetailConfig.children || []),
    apiConfig,
    options,
    rowKey: cfg.rowKey || 'id',
    formOpenMode: options.formOpenMode || cfg.formOpenMode || options.modalType || cfg.modalType || 'modal',
    tabWorkspace: options.tabWorkspace || cfg.tabWorkspace || {},
    modalType: options.modalType || cfg.modalType || 'modal',
    modalWidth: options.modalWidth || cfg.modalWidth || '800px',
    editGridCols: options.editGridCols || cfg.editGridCols || 1,
    editLabelWidth: options.editLabelWidth || cfg.editLabelWidth || 'auto',
    editLabelPlacement: options.editLabelPlacement || cfg.editLabelPlacement || 'left',
    editLabelAlign: options.editLabelAlign || cfg.editLabelAlign || 'right',
    editSize: options.editSize || cfg.editSize || 'medium',
    editShowFeedback: options.editShowFeedback ?? cfg.editShowFeedback ?? true,
    editFormClass: options.editFormClass || cfg.editFormClass || '',
    editFormStyle: options.editFormStyle || cfg.editFormStyle,
    formAssets: activeRuntimeFormProfile.value.formAssets || options.formAssets || cfg.formAssets || [],
    editXGap: normalizeNumberOption(options.editXGap ?? cfg.editXGap, 12),
    editYGap: normalizeNumberOption(options.editYGap ?? cfg.editYGap, 8),
    loadDetailOnEdit: options.loadDetailOnEdit ?? cfg.loadDetailOnEdit ?? true,
    searchGridCols: options.searchGridCols || cfg.searchGridCols || 4,
    hideAdd: !!options.hideAdd,
    hideBatchDelete: !!options.hideBatchDelete,
    showImport: !!options.showImport,
    showExport: !!options.showExport,
    showPagination: treeTable ? false : options.showPagination !== false,
    enableTreeAddChild: treeTable && (options.enableTreeAddChild === true || gridCrudProps.enableTreeAddChild === true),
    importApi: extractApiUrl(cfg.apiConfig?.import),
    exportApi: cfg.apiConfig?.export || '',
    importTemplateUrl: extractApiUrl(cfg.apiConfig?.importTemplate),
    enableCustomQuery: options.enableCustomQuery !== false,
    customQueryConfigKey: cfg.configKey,
    toolbarActions: normalizeRuntimePageActions(options.toolbarActions || [], 'toolbar'),
    businessObjectCode: resolveBusinessObjectCode(cfg),
    publicParams: treeTable
      ? { ...configuredPublicParams, ...defaultSortParams, loadMode: treeLoadMode }
      : { ...configuredPublicParams, ...defaultSortParams },
    publicQuery: configuredPublicQuery,
    formDefaultValues,
    submitDefaultParams,
    ...runtimeHookHandlers,
    beforeRenderList: list => prepareRuntimeList(list, { treeTable, treeConfig }),
    treeConfig: treeTable ? treeConfig : {},
    tableProps: buildRuntimeTableProps(cfg),
    onSubmitSuccess: handleRuntimeSubmitSuccess,
    formOnly: formOnlyRuntime.value,
    formOnlyTitle: resolveRuntimeTitle(cfg),
    formOnlySubmitText: '提交',
    formOnlySuccessTitle: '提交成功',
    formOnlySuccessDescription: '单据已保存',
  }
})

function resolveDefaultSortParams(defaultSort = {}) {
  const orderByColumn = defaultSort.orderByColumn || defaultSort.field || 'id'
  const isAsc = defaultSort.isAsc || defaultSort.order || 'desc'
  return {
    orderByColumn,
    isAsc,
  }
}

function buildRuntimeFieldMetaMap(modelSchema = {}) {
  const result = new Map()
  const fields = Array.isArray(modelSchema?.fields) ? modelSchema.fields : []
  fields.forEach((field) => {
    const fieldCode = field?.field || field?.fieldCode
    if (fieldCode)
      result.set(fieldCode, field)
  })
  return result
}

function applyRuntimeFieldMeta(field, meta = {}) {
  if (!field || !meta)
    return
  if (!field.dataType && meta.dataType)
    field.dataType = meta.dataType
  if (!field.fieldDataType && meta.dataType)
    field.fieldDataType = meta.dataType
  if (!field.componentType && meta.componentType)
    field.componentType = meta.componentType
  const formulaConfig = field.formulaConfig || meta.formulaConfig
  if (!hasRuntimeFormulaConfig(formulaConfig))
    return
  field.formulaConfig = formulaConfig
  field.required = false
  field.disabled = true
  field.readonly = true
  if (Array.isArray(field.rules))
    field.rules = field.rules.filter(rule => !rule?.required)
  field.props = {
    ...(field.props || {}),
    disabled: true,
    readonly: true,
  }
}

function hasRuntimeFormulaConfig(formulaConfig) {
  if (!formulaConfig)
    return false
  if (typeof formulaConfig === 'string')
    return formulaConfig.trim().length > 0
  return typeof formulaConfig === 'object' && Object.keys(formulaConfig).length > 0
}

function normalizeNumberOption(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function buildCrudHookHandlers(rules = {}) {
  return CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
    const list = (rules[target.value] || []).filter(rule => rule.field)
    if (list.length)
      handlers[target.value] = data => applyCrudHookRules(data, list)
    return handlers
  }, {})
}

function buildFormGovernanceEventHandlers(events = []) {
  return (Array.isArray(events) ? events : []).reduce((handlers, eventItem) => {
    const hookName = normalizeFormGovernanceHook(eventItem?.hook)
    if (!hookName || !eventItem?.handler)
      return handlers
    if (!handlers[hookName])
      handlers[hookName] = []
    handlers[hookName].push(eventItem)
    return handlers
  }, {})
}

function normalizeFormGovernanceHook(hook = '') {
  const value = String(hook || '')
  if (value === 'beforeLoad' || value === 'afterLoad')
    return 'beforeRenderForm'
  if (value === 'beforeSubmit')
    return 'beforeSubmit'
  if (value === 'afterSubmit')
    return 'afterSubmit'
  return ''
}

function composeRuntimeHookHandlers(...handlerGroups) {
  const hookNames = new Set(handlerGroups.flatMap(group => Object.keys(group || {})))
  return Array.from(hookNames).reduce((handlers, hookName) => {
    const handlersForHook = handlerGroups
      .map(group => group?.[hookName])
      .filter(Boolean)
    handlers[hookName] = async (payload) => {
      let nextPayload = payload
      for (const handler of handlersForHook) {
        if (Array.isArray(handler)) {
          nextPayload = await runFormGovernanceEvents(handler, hookName, nextPayload)
        }
        else {
          nextPayload = await handler(nextPayload)
        }
        if (nextPayload === false)
          return false
      }
      return nextPayload
    }
    return handlers
  }, {})
}

async function runFormGovernanceEvents(events = [], hookName = '', payload) {
  for (const eventItem of events) {
    await runFormGovernanceEvent(eventItem, hookName, payload)
  }
  return payload
}

async function runFormGovernanceEvent(eventItem = {}, hookName = '', payload) {
  const action = String(eventItem.action || '')
  if (action === 'customScript') {
    return runWhitelistedFormScript(eventItem.handler, payload)
  }
  if (action === 'setFieldValue') {
    applySetFieldValueEvent(eventItem.handler, payload)
    return
  }
  if (action !== 'request')
    return
  const apiConfig = parseApiConfigValue(eventItem.handler)
  if (!apiConfig.url)
    return
  const method = apiConfig.method || 'post'
  const requestConfig = {
    method,
    url: apiConfig.url,
  }
  if (method === 'get')
    requestConfig.params = payload
  else
    requestConfig.data = payload
  try {
    const response = await request(requestConfig)
    applyFormEventResultMapping(payload, response?.data, eventItem.resultMapping)
  }
  catch (error) {
    console.warn(`[crud-page] 表单事件请求失败(${hookName}):`, error?.message || error)
    throw error
  }
}

function runWhitelistedFormScript(handler = '', payload = {}) {
  const name = String(handler || '').trim()
  if (!name || name === 'noop')
    return
  const scripts = {
    fillCurrentDate: () => {
      payload.currentDate = new Date().toISOString().slice(0, 10)
    },
    fillCurrentTime: () => {
      payload.currentTime = new Date().toISOString()
    },
  }
  if (!scripts[name]) {
    console.warn(`[crud-page] 表单事件脚本不在白名单内: ${name}`)
    return
  }
  scripts[name]()
}

function applySetFieldValueEvent(handler = '', payload = {}) {
  const [field, ...valueParts] = String(handler || '').split('=')
  const fieldName = field?.trim()
  if (!fieldName)
    return
  payload[fieldName] = valueParts.join('=').trim()
}

function applyFormEventResultMapping(payload = {}, responseData, mappingText = '') {
  const mappings = String(mappingText || '').split(',').map(item => item.trim()).filter(Boolean)
  mappings.forEach((item) => {
    const [from, to] = item.split('->').map(part => part?.trim())
    if (!from || !to)
      return
    const value = getByPath(responseData, from)
    if (value !== undefined)
      payload[to] = value
  })
}

function getByPath(source, path = '') {
  return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], source)
}

function isTreeTableRuntime(cfg = {}, layoutType = '') {
  return !!cfg.options?.treeConfig && (layoutType || cfg.layoutType || 'simple-crud') !== 'tree-crud'
}

function resolveTreeLoadMode(treeConfig = {}) {
  return treeConfig.loadMode === 'lazy' ? 'lazy' : 'full'
}

function buildTreeTableProps(cfg = {}) {
  const treeConfig = cfg.options?.treeConfig || {}
  const loadMode = resolveTreeLoadMode(treeConfig)
  return {
    childrenKey: treeConfig.childrenField || 'children',
    defaultExpandAll: loadMode !== 'lazy',
    onLoad: loadMode === 'lazy' ? node => loadTreeTableChildren(node, cfg) : undefined,
  }
}

function buildRuntimeTableProps(cfg = {}) {
  const props = isTreeTableRuntime(cfg) ? buildTreeTableProps(cfg) : {}
  const rowGap = normalizeNumberOption(cfg.options?.tableRowGap, 8)
  const rowHeight = Math.max(34, 32 + rowGap)
  const rawRowProps = props.rowProps
  return {
    ...props,
    rowProps: (row, index) => {
      const base = typeof rawRowProps === 'function' ? rawRowProps(row, index) : {}
      const baseStyle = base.style && typeof base.style === 'object' && !Array.isArray(base.style) ? base.style : {}
      return {
        ...base,
        style: {
          ...baseStyle,
          height: `${rowHeight}px`,
        },
      }
    },
  }
}

async function loadTreeTableChildren(node, cfg = {}) {
  const treeConfig = cfg.options?.treeConfig || {}
  const treeApi = cfg.apiConfig?.tree
  if (!treeApi || !node)
    return
  const { method, url } = parseApiConfigValue(treeApi)
  const keyField = treeConfig.keyField || 'id'
  const parentValue = node[keyField] ?? node.key ?? node.targetValue
  const defaultSortParams = resolveDefaultSortParams(cfg.options?.defaultSort)
  try {
    const res = await request({
      method,
      url,
      params: {
        ...defaultSortParams,
        loadMode: 'lazy',
        parentValue,
      },
    })
    node[treeConfig.childrenField || 'children'] = normalizeTreeTableNodes(res?.data || [], treeConfig)
    if (!node[treeConfig.childrenField || 'children'].length)
      node.isLeaf = true
  }
  catch (error) {
    console.warn('[crud-page] 加载树形子节点失败', error)
    node.isLeaf = true
  }
}

function normalizeTreeTableNodes(nodes = [], treeConfig = {}) {
  if (!Array.isArray(nodes))
    return []
  const keyField = treeConfig.keyField || 'id'
  const childrenField = treeConfig.childrenField || 'children'
  return nodes.map((node) => {
    const children = Array.isArray(node?.[childrenField])
      ? normalizeTreeTableNodes(node[childrenField], treeConfig)
      : []
    const normalized = {
      ...(node || {}),
      key: node?.key ?? node?.[keyField],
    }
    if (children.length) {
      normalized[childrenField] = children
      normalized.isLeaf = false
    }
    else if (node?.isLeaf !== undefined) {
      normalized.isLeaf = !!node.isLeaf
    }
    return normalized
  })
}

async function prepareRuntimeList(list = [], options = {}) {
  const treeTable = !!options.treeTable
  const treeConfig = options.treeConfig || {}
  const normalizedList = treeTable ? normalizeTreeTableNodes(list, treeConfig) : list
  const objectCode = resolveBusinessObjectCode(renderConfig.value)
  if (!objectCode || !Array.isArray(normalizedList) || !normalizedList.length)
    return normalizedList
  await attachRuntimeActions(normalizedList, objectCode, treeConfig)
  return normalizedList
}

async function attachRuntimeActions(rows = [], objectCode, treeConfig = {}) {
  const childrenField = treeConfig.childrenField || 'children'
  await Promise.all(rows.map(async (row) => {
    if (!row || typeof row !== 'object')
      return
    row._runtimeObjectCode = objectCode
    const recordId = resolveRuntimeRecordId(row)
    if (recordId) {
      try {
        const res = await businessDocumentRuntime(objectCode, recordId)
        row._runtimeActions = res.data?.runtimeActions || []
        row._documentRuntime = res.data || null
      }
      catch (error) {
        row._runtimeActions = []
        console.warn('[crud-page] 加载单据运行态失败', error.message)
      }
    }
    if (Array.isArray(row[childrenField]) && row[childrenField].length)
      await attachRuntimeActions(row[childrenField], objectCode, treeConfig)
  }))
}

function resolveRuntimeRecordId(row = {}) {
  const rowKey = renderConfig.value?.rowKey || 'id'
  return row[rowKey] ?? row.id ?? row.Id
}

function resolveBusinessObjectCode(cfg = {}) {
  const options = cfg.options || {}
  const modelSchema = cfg.modelSchema || {}
  return cfg.businessObjectCode
    || options.businessObjectCode
    || modelSchema.objectCode
    || modelSchema.object?.code
    || cfg.objectCode
    || options.objectCode
    || modelSchema.modelCode
    || ''
}

function parseApiConfigValue(apiConfigValue) {
  const text = String(apiConfigValue || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

function extractApiUrl(apiConfigValue) {
  if (!apiConfigValue)
    return ''
  const parts = String(apiConfigValue).split('@')
  return parts.length > 1 ? parts.slice(1).join('@') : apiConfigValue
}

function resolveRuntimeDetailRecordId() {
  return route.query?.recordId || route.query?.id || route.query?.[crudProps.value.rowKey || 'id'] || ''
}

function replaceRuntimeApiParams(url = '', params = {}) {
  let finalUrl = String(url || '')
  let hasPlaceholder = false
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '')
      return
    const encoded = encodeURIComponent(String(value))
    if (finalUrl.includes(`:${key}`)) {
      finalUrl = finalUrl.replaceAll(`:${key}`, encoded)
      hasPlaceholder = true
    }
    if (finalUrl.includes(`{${key}}`)) {
      finalUrl = finalUrl.replaceAll(`{${key}}`, encoded)
      hasPlaceholder = true
    }
  })
  return { url: finalUrl, hasPlaceholder }
}

function resolveRuntimeDetailApi(recordId) {
  const cfg = renderConfig.value || {}
  const page = activeRuntimePage.value || {}
  const rawApi = page.detailApi || cfg.apiConfig?.detail || ''
  if (!rawApi && cfg.api) {
    return {
      method: 'get',
      url: `${cfg.api}/${encodeURIComponent(String(recordId))}`,
      params: {},
    }
  }
  const parsed = parseApiConfigValue(rawApi)
  const rowKey = crudProps.value.rowKey || cfg.rowKey || 'id'
  const urlParams = {
    id: recordId,
    [rowKey]: recordId,
  }
  const { url, hasPlaceholder } = replaceRuntimeApiParams(parsed.url, urlParams)
  const method = String(parsed.method || page.detailMethod || 'get').toLowerCase()
  const params = {}
  let finalUrl = url
  if (!hasPlaceholder) {
    if (method === 'get') {
      finalUrl = `${url.replace(/\/$/, '')}/${encodeURIComponent(String(recordId))}`
    }
    else {
      params[rowKey] = recordId
    }
  }
  return { method, url: finalUrl, params }
}

function resolveRuntimeDetailData(payload, dataField = 'data') {
  if (!payload || typeof payload !== 'object')
    return payload || {}
  const field = String(dataField || '').trim()
  if (!field || field === '.' || field === '$')
    return payload
  return field.split('.').reduce((data, key) => {
    if (data && typeof data === 'object' && key in data)
      return data[key]
    return undefined
  }, payload) ?? payload.data ?? payload
}

async function loadRuntimeDetailRecord() {
  if (!configLoaded.value || formOnlyRuntime.value)
    return
  if (activeRuntimePageKey.value !== 'detail') {
    runtimeDetailRecord.value = {}
    return
  }
  const recordId = resolveRuntimeDetailRecordId()
  if (!recordId) {
    runtimeDetailRecord.value = {}
    return
  }
  runtimeDetailLoading.value = true
  try {
    const { method, url, params } = resolveRuntimeDetailApi(recordId)
    if (!url) {
      runtimeDetailRecord.value = {}
      return
    }
    const requestMethod = method === 'postencrypt' ? 'postEncrypt' : method
    const response = requestMethod === 'postEncrypt'
      ? await postEncrypt(url, {}, { params, needTip: false })
      : await request({ method: requestMethod, url, params, needTip: false })
    runtimeDetailRecord.value = resolveRuntimeDetailData(response, activeRuntimePage.value?.detailDataField || 'data') || {}
  }
  catch (error) {
    runtimeDetailRecord.value = {}
    console.warn('[crud-page] 加载详情页记录失败', error?.message || error)
  }
  finally {
    runtimeDetailLoading.value = false
  }
}

function resolveBaseRuntimeTitle(cfg = {}) {
  if (route.query?.title)
    return String(route.query.title)
  return cfg.menuName || cfg.appName || cfg.objectName || cfg.tableComment || cfg.configKey
}

function resolveRuntimeTitle(cfg = {}) {
  const baseTitle = resolveBaseRuntimeTitle(cfg)
  if (activeRuntimePageKey.value === 'list')
    return baseTitle

  const pageTitle = activeRuntimePage.value?.pageName
    || (activeRuntimePageKey.value === 'detail' ? '详情页' : activeRuntimePageKey.value)
  if (!baseTitle || baseTitle === pageTitle)
    return pageTitle
  return `${baseTitle} - ${pageTitle}`
}

function syncRuntimeTitle() {
  const title = resolveRuntimeTitle(renderConfig.value || {})
  if (!title)
    return
  route.meta.title = title
  document.title = `${title} | ${getDefaultPageTitle()}`
  tabStore.updateTabTitle(route.fullPath, title)
}

function normalizeConfigKey(value) {
  const key = Array.isArray(value) ? value[0] : value
  return String(key || '').trim()
}

function resolveRouteConfigKey() {
  const paramKey = normalizeConfigKey(route.params?.configKey)
  if (paramKey)
    return paramKey

  const queryKey = normalizeConfigKey(route.query?.configKey)
  if (queryKey)
    return queryKey

  const prefix = '/ai/crud-page/'
  if (!route.path?.startsWith(prefix))
    return ''

  return normalizeConfigKey(decodeURIComponent(route.path.slice(prefix.length)))
}

function transformChildrenConfig(children = []) {
  return (children || []).map(child => ({
    ...child,
    fields: transformFields(child.fields || []),
  }))
}

/**
 * 预加载所有用到的字典数据
 */
async function preloadDicts(cfg) {
  const types = new Set()

  // 从 columnsSchema 提取 dictType
  ;(cfg.columnsSchema || []).forEach((col) => {
    if (col.render?.dictType)
      types.add(col.render.dictType)
  })

  // 从 searchSchema / editSchema 提取 dictType
  ;[...(cfg.searchSchema || []), ...(cfg.editSchema || [])].forEach((field) => {
    if (field.dictType)
      types.add(field.dictType)
  })

  const children = cfg.options?.masterDetailConfig?.children || []
  children.forEach((child) => {
    ;(child.fields || []).forEach((field) => {
      if (field.dictType)
        types.add(field.dictType)
    })
  })

  for (const type of types) {
    if (!dictCache.value[type]) {
      try {
        dictCache.value[type] = await getDictData(type)
      }
      catch (e) {
        console.warn(`[crud-page] 加载字典 ${type} 失败`, e)
      }
    }
  }
}

async function loadConfig() {
  // 支持三种格式：
  // 1. /ai/crud-page/:configKey （route.params，unplugin-vue-router 动态路由）
  // 2. /ai/crud-page/order_manage （从 route.path 解析，permission.js 静态路由）
  // 3. /ai/crud-page?configKey=xxx （旧的 query 格式）
  const configKey = resolveRouteConfigKey()
  if (!configKey || configKey.startsWith('/')) {
    errorMsg.value = '缺少 configKey 参数'
    return
  }

  loading.value = true
  errorMsg.value = ''
  configLoaded.value = false

  try {
    const res = await crudConfigRender(configKey)
    const cfg = res.data
    renderConfig.value = cfg
    // 动态页面的 Tab/浏览器标题以发布菜单名为准，避免再次点击 Tab 时回退成主模型名。
    syncRuntimeTitle()
    await preloadDicts(cfg)
    // 加载模板组件
    const layoutType = cfg.layoutType || 'simple-crud'
    const catalogEntry = catalog[layoutType]
    if (catalogEntry) {
      currentTemplate.value = defineAsyncComponent(catalogEntry.component)
    }
    else {
      // 未注册的模板，降级使用 AiCrudPage
      currentTemplate.value = null
    }
    configLoaded.value = true
    await loadRuntimeDetailRecord()
    scheduleInitialRuntimeAction()
  }
  catch (e) {
    errorMsg.value = e.message || '加载配置失败'
  }
  finally {
    loading.value = false
  }
}

async function scheduleInitialRuntimeAction() {
  if (!configLoaded.value)
    return
  if (formOnlyRuntime.value)
    return
  const mode = String(route.query?.mode || '').toLowerCase()
  if (!['create', 'detail'].includes(mode))
    return
  if (mode === 'detail' && activeRuntimePageKey.value !== 'list' && runtimeGridLayout.value)
    return
  const actionKey = `${route.fullPath}:${renderConfig.value?.configKey || ''}:${mode}`
  if (lastInitialActionKey.value === actionKey)
    return
  lastInitialActionKey.value = actionKey

  const crud = await waitRuntimeCrudRef()
  if (!crud)
    return
  if (mode === 'create') {
    crud.showAdd?.()
    return
  }
  const recordId = route.query?.recordId || route.query?.id
  if (recordId) {
    const rowKey = crudProps.value.rowKey || 'id'
    crud.showDetail?.({ [rowKey]: recordId, id: recordId })
  }
}

async function waitRuntimeCrudRef() {
  for (let i = 0; i < 10; i++) {
    await nextTick()
    const crud = runtimeCrudRef.value
    if (crud?.showAdd || crud?.showDetail)
      return crud
    await new Promise(resolve => window.setTimeout(resolve, 50))
  }
  return null
}

function handleRuntimeSubmitSuccess(payload = {}) {
  if (formOnlyRuntime.value)
    return
  if (payload.isEdit || String(route.query?.mode || '').toLowerCase() !== 'create')
    return
  const query = { ...route.query }
  delete query.mode
  router.replace({
    path: route.path,
    query,
    hash: route.hash,
  })
}

onMounted(() => {
  loadConfig()
})

// 监听 configKey 变化，兼容各种路由方式
watch(
  () => resolveRouteConfigKey(),
  (newKey, oldKey) => {
    if (newKey && newKey !== oldKey) {
      loadConfig()
    }
  },
)

watch(
  () => route.query?.mode,
  () => {
    scheduleInitialRuntimeAction()
  },
)

watch(
  () => [
    activeRuntimePageKey.value,
    route.query?.recordId || '',
    route.query?.id || '',
    route.query?._refresh || '',
    renderConfig.value?.configKey || '',
  ],
  () => {
    syncRuntimeTitle()
    loadRuntimeDetailRecord()
    scheduleInitialRuntimeAction()
  },
)
</script>

<style scoped>
.ai-crud-page-wrapper {
  height: 100%;
  min-height: 0;
  overflow: auto;
  background: transparent;
}

.loading-wrapper,
.error-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.runtime-column-link {
  cursor: pointer;
  text-decoration: none;
  font-weight: 600;
}

.runtime-column-link:hover {
  text-decoration: underline;
}

.runtime-list-grid {
  min-height: 100%;
  padding: 0;
  background: transparent;
}

.runtime-list-grid :deep(.canvas-panel) {
  border: 0;
  border-radius: 0;
  background: transparent;
}

.runtime-list-grid :deep(.canvas-scroll) {
  min-height: 100%;
  overflow: visible;
}

.runtime-list-grid :deep(.canvas-grid) {
  background: transparent;
  box-shadow: none;
}
</style>
