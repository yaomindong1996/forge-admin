<template>
  <div class="ai-crud-page-wrapper">
    <component
      :is="currentTemplate"
      v-if="configLoaded && currentTemplate"
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
import { getDictData } from '@/composables/useDict'
import { useTabStore } from '@/store'
import { request } from '@/utils'
import { getDefaultPageTitle } from '@/utils/page-title'

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
const runtimeOpenMode = computed(() => String(route.query?.runtimeOpenMode || '').toUpperCase())
const formOnlyRuntime = computed(() => runtimeOpenMode.value === 'CREATE_FORM')

/** 当前加载的模板组件（null 表示降级到 AiCrudPage） */
const currentTemplate = ref(null)

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
    // 统一补prop字段，AiTable需要这个字段来匹配数据
    const newCol = { ...col, prop: key }
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

    // dictTag 渲染
    if (col.render && typeof col.render === 'object' && col.render.type === 'dictTag') {
      newCol.render = row => h(DictTag, {
        dictType: col.render.dictType,
        value: row[key],
        size: 'small',
      })
      return newCol
    }
    if (col.render && typeof col.render === 'object' && col.render.type === 'relationName') {
      const targetField = col.render.targetField || `${key}Name`
      newCol.render = row => row[targetField] ?? row[key] ?? '-'
      return newCol
    }
    if (col.render && typeof col.render === 'object' && ['orgName', 'userName', 'regionName', 'fileUpload'].includes(col.render.type)) {
      const targetField = col.render.targetField || `${key}Name`
      newCol.render = row => row[targetField] ?? row[key] ?? '-'
      return newCol
    }
    // 如果该字段有翻译配置，优先显示翻译后的值，没有则显示原字段值
    if (transMap[key]) {
      const targetField = transMap[key]
      newCol.render = row => row[targetField] ?? row[key]
      return newCol
    }
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
  const treeTable = isTreeTableRuntime(cfg)
  const treeConfig = options.treeConfig || {}
  const treeLoadMode = resolveTreeLoadMode(treeConfig)
  const defaultSortParams = resolveDefaultSortParams(options.defaultSort)
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
    }),
    editSchema: transformEditFields(cfg.editSchema, options.editFormLayout, buildRuntimeFieldMetaMap(cfg.modelSchema)),
    childrenConfig: transformChildrenConfig(masterDetailConfig.children || []),
    apiConfig,
    options,
    rowKey: cfg.rowKey || 'id',
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
    formAssets: options.formAssets || cfg.formAssets || [],
    editXGap: normalizeNumberOption(options.editXGap ?? cfg.editXGap, 12),
    editYGap: normalizeNumberOption(options.editYGap ?? cfg.editYGap, 8),
    loadDetailOnEdit: options.loadDetailOnEdit ?? cfg.loadDetailOnEdit ?? true,
    searchGridCols: options.searchGridCols || cfg.searchGridCols || 4,
    hideAdd: !!options.hideAdd,
    hideBatchDelete: !!options.hideBatchDelete,
    showImport: !!options.showImport,
    showExport: !!options.showExport,
    showPagination: treeTable ? false : options.showPagination !== false,
    importApi: extractApiUrl(cfg.apiConfig?.import),
    exportApi: cfg.apiConfig?.export || '',
    importTemplateUrl: extractApiUrl(cfg.apiConfig?.importTemplate),
    enableCustomQuery: options.enableCustomQuery !== false,
    customQueryConfigKey: cfg.configKey,
    toolbarActions: normalizeRuntimePageActions(options.toolbarActions || [], 'toolbar'),
    businessObjectCode: resolveBusinessObjectCode(cfg),
    publicParams: treeTable ? { ...defaultSortParams, loadMode: treeLoadMode } : defaultSortParams,
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

function isTreeTableRuntime(cfg = {}) {
  return !!cfg.options?.treeConfig && (cfg.layoutType || 'simple-crud') !== 'tree-crud'
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

function resolveRuntimeTitle(cfg = {}) {
  if (route.query?.title)
    return String(route.query.title)
  return cfg.menuName || cfg.appName || cfg.objectName || cfg.tableComment || cfg.configKey
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
    const title = resolveRuntimeTitle(cfg)
    if (title) {
      route.meta.title = title
      document.title = `${title} | ${getDefaultPageTitle()}`
      tabStore.updateTabTitle(route.fullPath, title)
    }
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
</script>

<style scoped>
.ai-crud-page-wrapper {
  height: 100%;
}

.loading-wrapper,
.error-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}
</style>
