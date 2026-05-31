export const FORM_DESIGNER_SCHEMA_VERSION = 'form-first-v1'
export const FORM_DESIGNER_SCHEMA_KEY = 'formDesignerSchema'

const FIELD_COMPONENT_KEYS = new Set([
  'input',
  'textarea',
  'number',
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
  'userSelect',
  'fileUpload',
  'imageUpload',
  'objectReference',
])

const KNOWN_FIELD_CODES = {
  客户名称: 'customerName',
  联系电话: 'contactPhone',
  客户等级: 'customerLevel',
  客户状态: 'customerStatus',
  负责人: 'ownerUserId',
  所属部门: 'ownerDeptId',
  所属地区: 'regionCode',
  备注: 'remark',
  客户编码: 'customerCode',
  联系人: 'contactName',
  联系邮箱: 'contactEmail',
  详细地址: 'address',
  跟进状态: 'followStatus',
  创建时间: 'createTime',
  更新时间: 'updateTime',
}

export function createDefaultFormDesignerSchema(options = {}) {
  const formKey = options.formKey || buildFormKey(options.objectCode)
  const fields = Array.isArray(options.fields) ? options.fields : []
  return normalizeFormDesignerSchema({
    schemaVersion: FORM_DESIGNER_SCHEMA_VERSION,
    formKey,
    formName: options.formName || options.objectName || '业务表单',
    layout: {
      labelPlacement: 'left',
      labelWidth: 100,
      gridColumns: 2,
    },
    components: fields
      .filter(field => field && field.formVisible !== false && !field.systemField && !field.readonly)
      .map((field, index) => createComponentFromField(field, index)),
  })
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

export function generateFieldCode(label = '') {
  const text = String(label || '').trim()
  if (KNOWN_FIELD_CODES[text])
    return KNOWN_FIELD_CODES[text]
  const words = text.match(/[A-Z0-9]+/gi) || []
  if (words.length) {
    return words.map((word, index) => {
      const normalized = word.slice(0, 1).toUpperCase() + word.slice(1)
      return index === 0 ? normalized.slice(0, 1).toLowerCase() + normalized.slice(1) : normalized
    }).join('')
  }
  let hash = 0
  for (let index = 0; index < text.length; index += 1)
    hash = ((hash * 31) + text.charCodeAt(index)) >>> 0
  return `field${hash.toString(36)}`
}

export function camelToSnake(value = '') {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase()
}

export function isFieldComponent(component = {}) {
  return FIELD_COMPONENT_KEYS.has(component.componentKey)
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
  return {
    labelPlacement: source.labelPlacement || 'left',
    labelWidth: Number(source.labelWidth || 100),
    gridColumns: Number(source.gridColumns || 2),
    ...(source.rowGap ? { rowGap: source.rowGap } : {}),
    ...(source.columnGap ? { columnGap: source.columnGap } : {}),
  }
}

function normalizeComponent(component, index) {
  if (!isPlainObject(component))
    return null
  const componentKey = component.componentKey || component.type || 'input'
  const label = component.label || component.title || component.fieldBinding?.fieldCode || '字段'
  return {
    id: component.id || `cmp_${component.fieldBinding?.fieldCode || index}`,
    componentKey,
    label,
    fieldBinding: normalizeFieldBinding(component.fieldBinding, component.field || component.name),
    props: isPlainObject(component.props) ? component.props : {},
    layout: normalizeComponentLayout(component.layout),
    validation: normalizeValidation(component.validation),
    visibility: normalizeVisibility(component.visibility),
    children: Array.isArray(component.children)
      ? component.children.map((child, childIndex) => normalizeComponent(child, childIndex)).filter(Boolean)
      : [],
  }
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
