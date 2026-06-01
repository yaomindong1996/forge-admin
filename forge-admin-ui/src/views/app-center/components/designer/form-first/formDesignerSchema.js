import {
  camelToSnake,
  generateFieldCode,
} from './namingUtils'

export const FORM_DESIGNER_SCHEMA_VERSION = 'form-first-v1'
export const FORM_DESIGNER_SCHEMA_KEY = 'formDesignerSchema'

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
  'time',
  'switch',
  'select',
  'radio',
  'checkbox',
  'dictSelect',
  'cascader',
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
])

export const VIRTUAL_COMPONENT_KEYS = new Set([
  ...LAYOUT_COMPONENT_KEYS,
  'elDivider',
  'divider',
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
  fcTitle: '标题',
  title: '标题',
  fcTable: '表格布局',
  table: '表格布局',
  fcTableGrid: '表格单元格',
  tableGrid: '表格单元格',
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
      labelWidth: 100,
      size: 'medium',
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
  return {
    schemaVersion: schema.schemaVersion || FORM_DESIGNER_SCHEMA_VERSION,
    formKey: schema.formKey || buildFormKey(schema.objectCode),
    formName: schema.formName || '业务表单',
    layout: normalizeLayout(schema.layout),
    components: components
      .map((component, index) => normalizeComponent(component, index))
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

export function cloneValue(value) {
  if (value === undefined)
    return undefined
  return JSON.parse(JSON.stringify(value ?? null))
}

function normalizeLayout(layout = {}) {
  const source = isPlainObject(layout) ? layout : {}
  const normalized = {
    labelPlacement: source.labelPlacement || 'left',
    labelAlign: normalizeLabelAlign(source.labelAlign),
    labelWidth: resolveNumber(source.labelWidth, 100),
    size: normalizeFormSize(source.size),
    showFeedback: source.showFeedback === undefined ? true : Boolean(source.showFeedback),
    hideRequiredAsterisk: Boolean(source.hideRequiredAsterisk),
    inlineFeedback: Boolean(source.inlineFeedback || source.inlineMessage),
    gridColumns: clampGridColumns(source.gridColumns, 2),
    rowGap: Number(source.rowGap ?? 16),
    columnGap: Number(source.columnGap ?? 16),
  }
  if (source.formStyle !== undefined)
    normalized.formStyle = cloneValue(source.formStyle)
  if (source.formClass !== undefined)
    normalized.formClass = source.formClass
  return normalized
}

function normalizeComponent(component, index) {
  if (!isPlainObject(component))
    return null
  const componentKey = component.componentKey || component.type || 'input'
  const fieldComponent = FIELD_COMPONENT_KEYS.has(componentKey)
  const sourceLabel = component.label || component.title || component.props?.header || component.props?.label || component.props?.title || component.fieldBinding?.fieldCode || (fieldComponent ? '字段' : '布局')
  const label = normalizeDesignerComponentLabel(componentKey, sourceLabel)
  const sourceId = String(component.id || '').trim()
  return {
    id: sourceId && !isTemporaryDesignerRef(sourceId) ? sourceId : `cmp_${fieldComponent ? component.fieldBinding?.fieldCode || index : `${componentKey}_${index}`}`,
    componentKey,
    label,
    fieldBinding: fieldComponent
      ? normalizeFieldBinding(component.fieldBinding, component.field || component.name)
      : normalizeFieldBinding({ ...(component.fieldBinding || {}), mode: 'virtual', fieldCode: '' }),
    props: isPlainObject(component.props) ? component.props : {},
    layout: normalizeComponentLayout(component.layout),
    validation: normalizeValidation(component.validation),
    visibility: normalizeVisibility(component.visibility),
    children: Array.isArray(component.children)
      ? component.children.map((child, childIndex) => normalizeComponent(child, childIndex)).filter(Boolean)
      : [],
  }
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
  next.label = normalizeDesignerComponentLabel(componentKey, next.label)
  next.layout = {
    ...(next.layout || {}),
    span: componentKey === 'col' || (!fieldComponent && !fullRow) ? 1 : fullRow ? gridColumns : 1,
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
  if (componentType === 'date' || businessType === 'DATE')
    return 'date'
  if (componentType === 'time')
    return 'time'
  if (componentType === 'switch' || businessType === 'SWITCH')
    return 'switch'
  if (componentType === 'radio' || businessType === 'RADIO')
    return 'radio'
  if (componentType === 'checkbox' || ['CHECKBOX', 'MULTI_SELECT'].includes(businessType))
    return 'checkbox'
  if (field.dictType || componentType === 'dictSelect')
    return 'dictSelect'
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
  return ['textarea', 'fileUpload', 'imageUpload', 'subTable'].includes(componentKey) ? 2 : 1
}

function clampGridColumns(value, fallback = 2) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return fallback
  return Math.max(1, Math.min(3, number))
}

function buildPlaceholder(componentKey, label) {
  if (['select', 'radio', 'checkbox', 'dictSelect', 'date', 'datetime', 'time', 'regionTreeSelect', 'orgTreeSelect', 'userSelect', 'fileUpload', 'imageUpload', 'objectReference'].includes(componentKey))
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
