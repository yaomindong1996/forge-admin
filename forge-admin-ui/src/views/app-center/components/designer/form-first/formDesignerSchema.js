import {
  camelToSnake,
  generateFieldCode,
} from './namingUtils'

export const FORM_DESIGNER_SCHEMA_VERSION = 'form-first-v1'
export const FORM_DESIGNER_SCHEMA_KEY = 'formDesignerSchema'
export const MAX_FORM_GRID_COLUMNS = 6

export {
  camelToSnake,
  generateFieldCode,
}

const FIELD_COMPONENT_KEYS = new Set([
  'input',
  'textarea',
  'number',
  'inputNumber',
  'integer',
  'money',
  'date',
  'datetime',
  'daterange',
  'datetimerange',
  'month',
  'year',
  'time',
  'timerange',
  'switch',
  'select',
  'radio',
  'radioButton',
  'checkbox',
  'slider',
  'rate',
  'color',
  'dictSelect',
  'cascader',
  'treeSelect',
  'regionTreeSelect',
  'orgTreeSelect',
  'orgSelect',
  'departmentSelect',
  'departmentTreeSelect',
  'deptSelect',
  'deptTreeSelect',
  'elTreeSelect',
  'orgName',
  'deptName',
  'userSelect',
  'userPicker',
  'userName',
  'fileUpload',
  'imageUpload',
  'upload',
  'objectReference',
  'customSelect',
  'text',
])

export const LAYOUT_COMPONENT_KEYS = new Set([
  'fcRow',
  'row',
  'col',
  'elCard',
  'card',
  'elTabs',
  'tabs',
  'elTabPane',
  'tabPane',
  'elCollapse',
  'collapse',
  'elCollapseItem',
  'collapseItem',
  'fcTable',
  'table',
  'fcTableGrid',
  'tableGrid',
  'AiCrudPage',
  'crudBlock',
])

export const VIRTUAL_COMPONENT_KEYS = new Set([
  ...LAYOUT_COMPONENT_KEYS,
  'elDivider',
  'divider',
  'AiFormSectionTitle',
  'fcTitle',
  'title',
  'text',
  'html',
  'space',
  'elAlert',
  'alert',
  'elButton',
  'button',
  'elTag',
  'tag',
  'elImage',
  'image',
])

const GENERIC_COMPONENT_ID_SUFFIXES = new Set([
  'input',
  'textarea',
  'number',
  'inputnumber',
  'integer',
  'money',
  'date',
  'datetime',
  'daterange',
  'datetimerange',
  'month',
  'year',
  'time',
  'timerange',
  'switch',
  'select',
  'radio',
  'radiobutton',
  'checkbox',
  'slider',
  'rate',
  'color',
  'dictselect',
  'cascader',
  'treeselect',
  'regiontreeselect',
  'orgtreeselect',
  'orgselect',
  'departmentselect',
  'departmenttreeselect',
  'deptselect',
  'depttreeselect',
  'eltreeselect',
  'orgname',
  'deptname',
  'userselect',
  'userpicker',
  'username',
  'fileupload',
  'imageupload',
  'upload',
  'objectreference',
  'customselect',
  'text',
  'fcrow',
  'row',
  'col',
  'elcard',
  'card',
  'eltabs',
  'tabs',
  'eltabpane',
  'tabpane',
  'elcollapse',
  'collapse',
  'elcollapseitem',
  'collapseitem',
  'fctable',
  'table',
  'fctablegrid',
  'tablegrid',
  'crudblock',
  'eldivider',
  'divider',
  'fctitle',
  'title',
  'text',
  'html',
  'space',
  'elalert',
  'alert',
  'elbutton',
  'button',
  'eltag',
  'tag',
  'elimage',
  'image',
])

const FULL_ROW_COMPONENT_KEYS = new Set([
  'textarea',
  'fileUpload',
  'imageUpload',
  'upload',
  'subTable',
  'elDivider',
  'divider',
  'fcTitle',
  'title',
  'fcTable',
  'table',
  'fcTableGrid',
  'tableGrid',
  'AiCrudPage',
  'crudBlock',
])

const FULL_ROW_LAYOUT_COMPONENT_KEYS = new Set([
  'fcRow',
  'row',
  'elCard',
  'card',
  'elTabs',
  'tabs',
  'elTabPane',
  'tabPane',
  'elCollapse',
  'collapse',
  'elCollapseItem',
  'collapseItem',
])

const LAYOUT_COMPONENT_LABELS = {
  fcRow: '栅格布局',
  row: '栅格布局',
  col: '栅格列',
  elCard: '卡片',
  card: '卡片',
  elTabs: '标签页',
  tabs: '标签页',
  elTabPane: '标签面板',
  tabPane: '标签面板',
  elCollapse: '折叠面板',
  collapse: '折叠面板',
  elCollapseItem: '折叠项',
  collapseItem: '折叠项',
  elDivider: '分割线',
  divider: '分割线',
  AiFormSectionTitle: '表单分隔线',
  fcTitle: '分组标题',
  title: '分组标题',
  fcTable: '表格布局',
  table: '表格布局',
  fcTableGrid: '表格单元格',
  tableGrid: '表格单元格',
  AiCrudPage: 'CRUD区块',
  crudBlock: 'CRUD区块',
}

const RAW_LAYOUT_LABELS = new Set([
  '',
  'layout',
  'row',
  'Row',
  'fcRow',
  'FcRow',
  'col',
  'Col',
  'Grid',
  'elCard',
  'ElCard',
  'card',
  'Card',
  'elTabs',
  'ElTabs',
  'tabs',
  'Tabs',
  'elTabPane',
  'ElTabPane',
  'tabPane',
  'TabPane',
  'Tab Pane',
  'elCollapse',
  'ElCollapse',
  'collapse',
  'Collapse',
  'elCollapseItem',
  'ElCollapseItem',
  'collapseItem',
  'CollapseItem',
  'Collapse Item',
  'elDivider',
  'ElDivider',
  'divider',
  'Divider',
  'fcTitle',
  'FcTitle',
  'title',
  'Title',
  'fcTable',
  'FcTable',
  'table',
  'Table',
  'fcTableGrid',
  'FcTableGrid',
  'tableGrid',
  'TableGrid',
  'Table Grid',
  'AiCrudPage',
  'AI CRUD Page',
  'crudBlock',
  'CrudBlock',
  'CRUD Block',
  'CRUD区块',
  'div',
  '布局',
])

export function createDefaultFormDesignerSchema(options = {}) {
  const formKey = options.formKey || buildFormKey(options.objectCode)
  const fields = Array.isArray(options.fields) ? options.fields : []
  const gridColumns = clampGridColumns(options.gridColumns, 2)
  return applyGridColumnsToFormDesignerSchema({
    schemaVersion: FORM_DESIGNER_SCHEMA_VERSION,
    formKey,
    formName: options.formName || options.objectName || '业务表单',
    layout: {
      labelPlacement: 'left',
      labelAlign: 'right',
      labelWidth: 'auto',
      size: 'medium',
      modalType: options.modalType || 'modal',
      showFeedback: true,
      gridColumns,
      rowGap: 16,
      columnGap: 16,
    },
    components: fields
      .filter(field => field && field.formVisible !== false && !field.systemField && !field.readonly)
      .map((field, index) => createComponentFromField(field, index)),
  }, gridColumns)
}

export function normalizeFormDesignerSchema(source = {}) {
  const schema = isPlainObject(source) ? cloneValue(source) : {}
  const components = Array.isArray(schema.components) ? schema.components : []
  const usedIds = collectReservedComponentIds(components)
  return {
    schemaVersion: schema.schemaVersion || FORM_DESIGNER_SCHEMA_VERSION,
    formKey: schema.formKey || buildFormKey(schema.objectCode),
    formName: schema.formName || '业务表单',
    layout: normalizeLayout(schema.layout),
    components: components
      .map((component, index) => normalizeComponent(component, index, usedIds))
      .filter(Boolean),
    settings: isPlainObject(schema.settings) ? schema.settings : {},
  }
}

export function validateFormDesignerSchema(source = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const errors = []
  const ids = new Set()

  if (!schema.formKey)
    errors.push({ path: 'formKey', message: '表单编码不能为空' })
  if (!schema.formName)
    errors.push({ path: 'formName', message: '表单名称不能为空' })

  schema.components.forEach((component, index) => {
    const path = `components[${index}]`
    if (!component.id)
      errors.push({ path: `${path}.id`, message: '组件 ID 不能为空' })
    if (component.id && ids.has(component.id))
      errors.push({ path: `${path}.id`, message: `组件 ID 重复：${component.id}` })
    if (component.id)
      ids.add(component.id)
    if (!component.componentKey)
      errors.push({ path: `${path}.componentKey`, message: '组件类型不能为空' })
    if (isFieldComponent(component) && !component.fieldBinding?.fieldCode) {
      errors.push({
        path: `${path}.fieldBinding.fieldCode`,
        message: `${component.label || component.componentKey} 未绑定业务字段`,
      })
    }
  })

  return {
    valid: errors.length === 0,
    errors,
    schema,
  }
}

export function createComponentFromField(field = {}, index = 0) {
  const fieldCode = field.fieldCode || field.field || ''
  const label = field.fieldName || field.label || fieldCode || '字段'
  const componentKey = resolveComponentKey(field)
  return normalizeComponent({
    id: `cmp_${fieldCode || index}`,
    componentKey,
    label,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName: field.columnName || camelToSnake(fieldCode),
      createIfMissing: false,
      source: 'field_asset',
      locked: Boolean(field.systemField || field.readonly),
    },
    props: buildComponentProps(field, label),
    layout: {
      span: resolveDefaultSpan(componentKey),
      align: 'left',
    },
    validation: {
      required: Boolean(field.required),
      requiredMessage: field.required ? buildRequiredMessage(componentKey, label) : '',
    },
    visibility: {
      hidden: field.formVisible === false,
      readonly: Boolean(field.readonly),
    },
  }, index)
}

export function applyGridColumnsToFormDesignerSchema(source = {}, gridColumns = 2) {
  const columns = clampGridColumns(gridColumns, 2)
  const schema = normalizeFormDesignerSchema(source)
  return normalizeFormDesignerSchema({
    ...schema,
    layout: {
      ...(schema.layout || {}),
      gridColumns: columns,
    },
    components: (schema.components || []).map(component => applyGridColumnsToComponent(component, columns)),
  })
}

export function normalizeDesignerComponentLabel(componentKey = '', label = '') {
  const fallback = LAYOUT_COMPONENT_LABELS[componentKey]
  if (!fallback)
    return label || ''
  const value = stripTemporaryDesignerRef(String(label || '').trim())
  return !value || RAW_LAYOUT_LABELS.has(value) ? fallback : value
}

export function resolveDesignerComponentDefaultLabel(componentKey = '') {
  return LAYOUT_COMPONENT_LABELS[componentKey] || ''
}

export function isTemporaryDesignerRef(value = '') {
  return /^ref_[A-Z0-9]+$/i.test(String(value || '').trim())
}

export function isGenericDesignerComponentId(value = '', componentKey = '') {
  const id = String(value || '').trim()
  if (!id.toLowerCase().startsWith('cmp_'))
    return false
  const suffix = id.slice(4).trim()
  if (!suffix)
    return true
  const normalizedSuffix = suffix.toLowerCase()
  const normalizedKey = String(componentKey || '').trim().toLowerCase()
  return normalizedSuffix === normalizedKey
    || FIELD_COMPONENT_KEYS.has(suffix)
    || LAYOUT_COMPONENT_KEYS.has(suffix)
    || VIRTUAL_COMPONENT_KEYS.has(suffix)
    || GENERIC_COMPONENT_ID_SUFFIXES.has(normalizedSuffix)
}

export function createFieldBindingFromLabel(label, options = {}) {
  const fieldCode = options.fieldCode || generateFieldCode(label)
  return {
    mode: options.mode || 'field',
    fieldCode,
    columnName: options.columnName || camelToSnake(fieldCode),
    createIfMissing: options.createIfMissing !== false,
    source: options.source || 'designer',
    locked: Boolean(options.locked),
  }
}

export function isFieldComponent(component = {}) {
  return FIELD_COMPONENT_KEYS.has(component.componentKey)
}

export function isLayoutComponent(component = {}) {
  return LAYOUT_COMPONENT_KEYS.has(component.componentKey)
}

export function isVirtualComponent(component = {}) {
  return VIRTUAL_COMPONENT_KEYS.has(component.componentKey) || !isFieldComponent(component)
}

export function extractFieldBindings(schema = {}) {
  return normalizeFormDesignerSchema(schema).components.filter(component => isFieldComponent(component) && component.fieldBinding?.mode === 'field').map(component => ({
    componentId: component.id,
    fieldCode: component.fieldBinding.fieldCode,
    createIfMissing: Boolean(component.fieldBinding.createIfMissing),
    locked: Boolean(component.fieldBinding.locked),
  }))
}

export function findDesignerComponentPath(source = {}, componentId = '') {
  if (!componentId)
    return null
  const schema = normalizeFormDesignerSchema(source)
  return findComponentPathInList(schema.components, componentId, [])
}

export function getDesignerComponent(source = {}, componentId = '') {
  const schema = normalizeFormDesignerSchema(source)
  const path = findDesignerComponentPath(schema, componentId)
  if (!path)
    return null
  return getComponentAtPath(schema.components, path)
}

export function insertDesignerComponent(source = {}, target = {}, component = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const nextComponent = normalizeComponent(component, schema.components.length, collectReservedComponentIds(schema.components))
  if (!nextComponent)
    return schema
  const parentChildren = resolveTargetChildren(schema, target.parentId)
  const index = clampInsertIndex(target.index, parentChildren.length)
  parentChildren.splice(index, 0, nextComponent)
  return normalizeFormDesignerSchema(schema)
}

export function moveDesignerComponent(source = {}, sourceId = '', target = {}) {
  if (!sourceId)
    return normalizeFormDesignerSchema(source)
  const schema = normalizeFormDesignerSchema(source)
  const sourcePath = findComponentPathInList(schema.components, sourceId, [])
  if (!sourcePath)
    return schema
  const sourceComponent = getComponentAtPath(schema.components, sourcePath)
  if (!sourceComponent || isDescendantTarget(sourceComponent, target.parentId))
    return schema
  const sourceParentId = resolveParentComponentId(schema.components, sourcePath)
  const removed = removeComponentAtPath(schema.components, sourcePath)
  if (!removed)
    return schema
  const parentChildren = resolveTargetChildren(schema, target.parentId)
  const movingDownInSameParent = sourceParentId === (target.parentId || '') && sourcePath[sourcePath.length - 1] < Number(target.index)
  const index = clampInsertIndex(movingDownInSameParent ? Number(target.index) - 1 : target.index, parentChildren.length)
  parentChildren.splice(index, 0, removed)
  return normalizeFormDesignerSchema(schema)
}

export function removeDesignerComponent(source = {}, componentId = '') {
  if (!componentId)
    return normalizeFormDesignerSchema(source)
  const schema = normalizeFormDesignerSchema(source)
  const path = findComponentPathInList(schema.components, componentId, [])
  if (!path)
    return schema
  removeComponentAtPath(schema.components, path)
  return normalizeFormDesignerSchema(schema)
}

export function duplicateDesignerComponent(source = {}, componentId = '') {
  const schema = normalizeFormDesignerSchema(source)
  const path = findComponentPathInList(schema.components, componentId, [])
  const component = path ? getComponentAtPath(schema.components, path) : null
  if (!component)
    return schema
  const copy = cloneValue(component)
  rewriteComponentIds(copy, collectReservedComponentIds(schema.components))
  const parentPath = path.slice(0, -1)
  const parentChildren = parentPath.length ? getComponentAtPath(schema.components, parentPath)?.children : schema.components
  parentChildren.splice(path[path.length - 1] + 1, 0, copy)
  return normalizeFormDesignerSchema(schema)
}

export function updateDesignerComponent(source = {}, componentId = '', patch = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const path = findComponentPathInList(schema.components, componentId, [])
  const component = path ? getComponentAtPath(schema.components, path) : null
  if (!component)
    return schema
  Object.assign(component, mergeComponentPatch(component, patch))
  return normalizeFormDesignerSchema(schema)
}

export function updateDesignerLayout(source = {}, patch = {}) {
  const schema = normalizeFormDesignerSchema(source)
  const next = normalizeFormDesignerSchema({
    ...schema,
    layout: {
      ...(schema.layout || {}),
      ...(isPlainObject(patch) ? patch : {}),
    },
  })
  if (Object.prototype.hasOwnProperty.call(patch || {}, 'gridColumns'))
    return reconcileDesignerGridColumns(next, next.layout.gridColumns)
  return next
}

export function canAcceptDesignerChild(parentComponent = null, childComponent = {}) {
  if (!childComponent)
    return false
  if (!parentComponent)
    return true
  const parentKey = parentComponent.componentKey || ''
  const childKey = childComponent.componentKey || ''
  if (['tabs', 'elTabs'].includes(parentKey))
    return ['tabPane', 'elTabPane'].includes(childKey)
  if (['collapse', 'elCollapse'].includes(parentKey))
    return ['collapseItem', 'elCollapseItem'].includes(childKey)
  if (['table', 'fcTable'].includes(parentKey))
    return ['tableGrid', 'fcTableGrid'].includes(childKey)
  if (['tabPane', 'elTabPane', 'collapseItem', 'elCollapseItem', 'card', 'elCard', 'crudBlock', 'row', 'fcRow', 'col', 'tableGrid', 'fcTableGrid'].includes(parentKey))
    return true
  return false
}

export function cloneValue(value) {
  if (value === undefined)
    return undefined
  return JSON.parse(JSON.stringify(value ?? null))
}

function findComponentPathInList(components = [], componentId = '', parentPath = []) {
  for (let index = 0; index < components.length; index += 1) {
    const component = components[index]
    const path = [...parentPath, index]
    if (component?.id === componentId)
      return path
    const childPath = findComponentPathInList(component?.children || [], componentId, path)
    if (childPath)
      return childPath
  }
  return null
}

function getComponentAtPath(components = [], path = []) {
  let current = null
  let children = components
  for (const index of path) {
    current = children[index]
    if (!current)
      return null
    children = current.children || []
  }
  return current
}

function resolveParentComponentId(components = [], path = []) {
  if (!path.length || path.length === 1)
    return ''
  const parent = getComponentAtPath(components, path.slice(0, -1))
  return parent?.id || ''
}

function removeComponentAtPath(components = [], path = []) {
  if (!path.length)
    return null
  const parentPath = path.slice(0, -1)
  const parent = parentPath.length ? getComponentAtPath(components, parentPath) : null
  const children = parent ? parent.children || [] : components
  const index = path[path.length - 1]
  const [removed] = children.splice(index, 1)
  return removed || null
}

function resolveTargetChildren(schema = {}, parentId = '') {
  if (!parentId)
    return schema.components
  const path = findComponentPathInList(schema.components, parentId, [])
  const parent = path ? getComponentAtPath(schema.components, path) : null
  if (!parent)
    return schema.components
  if (!Array.isArray(parent.children))
    parent.children = []
  return parent.children
}

function clampInsertIndex(index, length) {
  const number = Number(index)
  if (!Number.isFinite(number))
    return length
  return Math.max(0, Math.min(length, number))
}

function isDescendantTarget(component = {}, targetParentId = '') {
  if (!targetParentId)
    return false
  if (component.id === targetParentId)
    return true
  return (component.children || []).some(child => isDescendantTarget(child, targetParentId))
}

function rewriteComponentIds(component = {}, usedIds = new Set()) {
  component.id = reserveComponentId(`${component.id || 'cmp_component'}_copy`, usedIds)
  ;(component.children || []).forEach(child => rewriteComponentIds(child, usedIds))
}

function mergeComponentPatch(component = {}, patch = {}) {
  const source = isPlainObject(patch) ? patch : {}
  return {
    ...source,
    props: source.props ? { ...(component.props || {}), ...source.props } : component.props,
    layout: source.layout ? { ...(component.layout || {}), ...source.layout } : component.layout,
    validation: source.validation ? { ...(component.validation || {}), ...source.validation } : component.validation,
    visibility: source.visibility ? { ...(component.visibility || {}), ...source.visibility } : component.visibility,
  }
}

function normalizeLayout(layout = {}) {
  const source = isPlainObject(layout) ? layout : {}
  const normalized = {
    labelPlacement: source.labelPlacement || 'left',
    labelAlign: normalizeLabelAlign(source.labelAlign),
    labelWidth: normalizeLabelWidth(source.labelWidth),
    size: normalizeFormSize(source.size),
    modalType: ['modal', 'drawer'].includes(source.modalType) ? source.modalType : 'modal',
    showFeedback: source.showFeedback === undefined ? true : Boolean(source.showFeedback),
    hideRequiredAsterisk: Boolean(source.hideRequiredAsterisk),
    inlineFeedback: Boolean(source.inlineFeedback || source.inlineMessage),
    gridColumns: clampGridColumns(source.gridColumns ?? source.gridCols, 2),
    gridCols: clampGridColumns(source.gridCols ?? source.gridColumns, 2),
    rowGap: Number(source.rowGap ?? source.yGap ?? 16),
    columnGap: Number(source.columnGap ?? source.xGap ?? 16),
    xGap: Number(source.xGap ?? source.columnGap ?? 16),
    yGap: Number(source.yGap ?? source.rowGap ?? 16),
  }
  if (source.formStyle !== undefined)
    normalized.formStyle = cloneValue(source.formStyle)
  if (source.formStyleText !== undefined)
    normalized.formStyleText = source.formStyleText
  if (source.formClass !== undefined)
    normalized.formClass = source.formClass
  normalized.showActions = source.showActions === undefined ? true : Boolean(source.showActions)
  normalized.showSubmit = source.showSubmit === undefined ? true : Boolean(source.showSubmit)
  normalized.showReset = source.showReset === undefined ? true : Boolean(source.showReset)
  normalized.showCancel = Boolean(source.showCancel)
  normalized.submitText = source.submitText || '提交'
  normalized.resetText = source.resetText || '重置'
  normalized.cancelText = source.cancelText || '取消'
  normalized.enableCollapse = Boolean(source.enableCollapse)
  normalized.maxVisibleFields = resolveNumber(source.maxVisibleFields, 6)
  return normalized
}

function normalizeComponent(component, index, usedIds = new Set()) {
  if (!isPlainObject(component))
    return null
  const componentKey = component.componentKey || component.type || 'input'
  const fieldComponent = FIELD_COMPONENT_KEYS.has(componentKey)
  const sourceLabel = component.label || component.title || component.props?.header || component.props?.label || component.props?.title || component.fieldBinding?.fieldCode || (fieldComponent ? '字段' : '布局')
  const label = normalizeDesignerComponentLabel(componentKey, sourceLabel)
  const sourceId = String(component.id || '').trim()
  const fieldBinding = fieldComponent
    ? normalizeFieldBinding(component.fieldBinding, component.field || component.name)
    : normalizeFieldBinding({ ...(component.fieldBinding || {}), mode: 'virtual', fieldCode: '' })
  return {
    id: resolveNormalizedComponentId(sourceId, componentKey, fieldBinding, fieldComponent, index, usedIds),
    componentKey,
    label,
    fieldBinding,
    props: normalizeComponentProps(componentKey, component.props),
    layout: normalizeComponentLayout(component.layout),
    validation: normalizeValidation(component.validation),
    visibility: normalizeVisibility(component.visibility),
    children: Array.isArray(component.children)
      ? component.children.map((child, childIndex) => normalizeComponent(child, childIndex, usedIds)).filter(Boolean)
      : [],
  }
}

function normalizeComponentProps(componentKey = '', props = {}) {
  const normalizedProps = isPlainObject(props) ? { ...props } : {}
  if (normalizedProps.dictType && ['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'cascader'].includes(componentKey))
    delete normalizedProps.options
  return normalizedProps
}

function collectReservedComponentIds(components = [], ids = new Set()) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!isPlainObject(component))
      return
    const componentKey = component.componentKey || component.type || 'input'
    const sourceId = String(component.id || '').trim()
    if (sourceId && !isTemporaryDesignerRef(sourceId) && !isGenericDesignerComponentId(sourceId, componentKey))
      ids.add(sourceId)
    if (Array.isArray(component.children))
      collectReservedComponentIds(component.children, ids)
  })
  return ids
}

function resolveNormalizedComponentId(sourceId, componentKey, fieldBinding, fieldComponent, index, usedIds = new Set()) {
  if (sourceId && !isTemporaryDesignerRef(sourceId) && !isGenericDesignerComponentId(sourceId, componentKey))
    return sourceId
  const base = fieldComponent
    ? `cmp_${fieldBinding?.fieldCode || index}`
    : `cmp_${componentKey || 'layout'}_${index}`
  return reserveComponentId(base, usedIds)
}

function reserveComponentId(baseId, usedIds = new Set()) {
  const base = String(baseId || 'cmp_component').trim() || 'cmp_component'
  if (!usedIds.has(base)) {
    usedIds.add(base)
    return base
  }
  for (let index = 2; index < 1000; index += 1) {
    const candidate = `${base}_${index}`
    if (!usedIds.has(candidate)) {
      usedIds.add(candidate)
      return candidate
    }
  }
  const candidate = `${base}_${Date.now()}`
  usedIds.add(candidate)
  return candidate
}

function stripTemporaryDesignerRef(value = '') {
  return String(value || '')
    .trim()
    .replace(/^ref_[A-Z0-9]+[\s:：-]*/i, '')
    .trim()
}

export function normalizeFieldBinding(binding = {}, fallbackFieldCode = '') {
  const source = isPlainObject(binding) ? binding : {}
  const fieldCode = source.fieldCode || fallbackFieldCode || ''
  return {
    mode: source.mode || (fieldCode ? 'field' : 'virtual'),
    fieldCode,
    ...(source.columnName ? { columnName: source.columnName } : {}),
    createIfMissing: source.createIfMissing !== false && Boolean(fieldCode),
    source: source.source || 'designer',
    locked: Boolean(source.locked),
  }
}

function normalizeComponentLayout(layout = {}) {
  const source = isPlainObject(layout) ? layout : {}
  return {
    span: Number(source.span || 1),
    align: normalizeAlign(source.align),
    ...(source.width ? { width: source.width } : {}),
    ...(source.labelWidth ? { labelWidth: source.labelWidth } : {}),
  }
}

function applyGridColumnsToComponent(component = {}, gridColumns = 2) {
  const next = cloneValue(component)
  const componentKey = next.componentKey || ''
  const fieldComponent = FIELD_COMPONENT_KEYS.has(componentKey)
  const fullRow = FULL_ROW_COMPONENT_KEYS.has(componentKey) || FULL_ROW_LAYOUT_COMPONENT_KEYS.has(componentKey)
  const currentSpan = resolveNumber(next.layout?.span, fullRow ? gridColumns : 1)
  next.label = normalizeDesignerComponentLabel(componentKey, next.label)
  next.layout = {
    ...(next.layout || {}),
    span: componentKey === 'col' || (!fieldComponent && !fullRow)
      ? 1
      : fullRow
        ? gridColumns
        : Math.max(1, Math.min(gridColumns, currentSpan)),
  }
  if (componentKey === 'col') {
    next.props = {
      ...(next.props || {}),
      span: toFormCreateColSpan(next.layout.span, gridColumns),
    }
  }
  next.children = Array.isArray(next.children)
    ? next.children.map(child => applyGridColumnsToComponent(child, gridColumns))
    : []
  return next
}

function reconcileDesignerGridColumns(source = {}, gridColumns = 2) {
  const columns = clampGridColumns(gridColumns, 2)
  return normalizeFormDesignerSchema({
    ...source,
    layout: {
      ...(source.layout || {}),
      gridColumns: columns,
    },
    components: (source.components || []).map(component => applyGridColumnsToComponent(component, columns)),
  })
}

function toFormCreateColSpan(span, gridColumns) {
  const columns = clampGridColumns(gridColumns, 2)
  const normalizedSpan = Math.max(1, Math.min(columns, Number(span || 1)))
  return Math.max(1, Math.min(24, Math.ceil((24 * normalizedSpan) / columns)))
}

function normalizeValidation(validation = {}) {
  const source = isPlainObject(validation) ? validation : {}
  return {
    required: Boolean(source.required),
    requiredMessage: source.requiredMessage || '',
    rules: Array.isArray(source.rules) ? source.rules : [],
  }
}

function normalizeVisibility(visibility = {}) {
  const source = isPlainObject(visibility) ? visibility : {}
  return {
    hidden: Boolean(source.hidden),
    readonly: Boolean(source.readonly),
  }
}

function normalizeAlign(value) {
  return ['left', 'center', 'right'].includes(value) ? value : 'left'
}

function normalizeLabelAlign(value) {
  return ['left', 'right'].includes(value) ? value : 'right'
}

function normalizeLabelWidth(value) {
  if (value === undefined || value === null || value === '')
    return 'auto'
  if (String(value).trim() === 'auto')
    return 'auto'
  return resolveNumber(value, 100)
}

function normalizeFormSize(value) {
  if (value === 'default' || value === 'medium')
    return 'medium'
  return ['small', 'large'].includes(value) ? value : 'medium'
}

function resolveNumber(value, fallback) {
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number.parseInt(value, 10)
    return Number.isFinite(parsed) ? parsed : fallback
  }
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function resolveComponentKey(field = {}) {
  const componentType = field.componentType || ''
  const businessType = String(field.fieldType || field.businessFieldType || '').toUpperCase()
  if (componentType === 'textarea' || businessType === 'MULTILINE')
    return 'textarea'
  if (componentType === 'number' || businessType === 'NUMBER')
    return 'number'
  if (businessType === 'MONEY')
    return 'money'
  if (componentType === 'datetime' || businessType === 'DATETIME')
    return 'datetime'
  if (componentType === 'daterange')
    return 'daterange'
  if (componentType === 'datetimerange')
    return 'datetimerange'
  if (componentType === 'month')
    return 'month'
  if (componentType === 'year')
    return 'year'
  if (componentType === 'date' || businessType === 'DATE')
    return 'date'
  if (componentType === 'time')
    return 'time'
  if (componentType === 'timerange')
    return 'timerange'
  if (componentType === 'switch' || businessType === 'SWITCH')
    return 'switch'
  if (componentType === 'radioButton')
    return 'radioButton'
  if (componentType === 'radio' || businessType === 'RADIO')
    return 'radio'
  if (componentType === 'checkbox' || ['CHECKBOX', 'MULTI_SELECT'].includes(businessType))
    return 'checkbox'
  if (componentType === 'slider')
    return 'slider'
  if (componentType === 'rate')
    return 'rate'
  if (componentType === 'color')
    return 'color'
  if (field.dictType || componentType === 'dictSelect')
    return 'dictSelect'
  if (componentType === 'treeSelect')
    return 'treeSelect'
  if (componentType === 'regionTreeSelect' || businessType === 'REGION')
    return 'regionTreeSelect'
  if (componentType === 'orgTreeSelect' || businessType === 'DEPT')
    return 'orgTreeSelect'
  if (componentType === 'userSelect' || businessType === 'USER')
    return 'userSelect'
  if (componentType === 'imageUpload' || businessType === 'IMAGE')
    return 'imageUpload'
  if (['fileUpload', 'upload'].includes(componentType) || ['FILE', 'ATTACHMENT'].includes(businessType))
    return 'fileUpload'
  if (businessType === 'REFERENCE')
    return 'objectReference'
  if (componentType === 'customSelect')
    return 'customSelect'
  if (componentType === 'select' || businessType === 'SELECT' || businessType === 'DICT')
    return 'select'
  return 'input'
}

function buildComponentProps(field = {}, label = '') {
  const props = {
    ...(isPlainObject(field.basicProps) ? field.basicProps : {}),
  }
  delete props.fieldBinding
  if (props.clearable === undefined)
    props.clearable = true
  props.placeholder = props.placeholder || buildPlaceholder(resolveComponentKey(field), label)
  if (field.dictType)
    props.dictType = field.dictType
  if (field.length)
    props.maxlength = field.length
  if (field.referenceObjectCode)
    props.referenceObjectCode = field.referenceObjectCode
  if (field.referenceDisplayField)
    props.referenceDisplayField = field.referenceDisplayField
  return props
}

function resolveDefaultSpan(componentKey) {
  return ['textarea', 'fileUpload', 'imageUpload', 'subTable', 'daterange', 'datetimerange', 'timerange'].includes(componentKey) ? 2 : 1
}

function clampGridColumns(value, fallback = 2) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return fallback
  return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, number))
}

function buildPlaceholder(componentKey, label) {
  if (['select', 'radio', 'radioButton', 'checkbox', 'dictSelect', 'date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'time', 'timerange', 'regionTreeSelect', 'orgTreeSelect', 'treeSelect', 'userSelect', 'fileUpload', 'imageUpload', 'objectReference', 'customSelect', 'color'].includes(componentKey))
    return `请选择${label}`
  return `请输入${label}`
}

function buildRequiredMessage(componentKey, label) {
  return buildPlaceholder(componentKey, label)
}

function buildFormKey(objectCode = '') {
  return objectCode ? `${objectCode}_default_form` : 'default_form'
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}
