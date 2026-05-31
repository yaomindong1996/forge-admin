import { getDesignPlaceholderOptions, resolveDesignerDragTag } from './forgeBusinessComponents'
import { isFieldComponent, normalizeFormDesignerSchema } from './formDesignerSchema'

export function forgeSchemaToFormCreate(input = {}) {
  const schema = normalizeFormDesignerSchema(input.schema || input.formDesignerSchema || input)
  const fieldMap = new Map((input.fields || []).map(field => [field.fieldCode || field.field, field]))
  const gridColumns = Number(schema.layout?.gridColumns || 2)
  return {
    rules: schema.components.map(component => convertComponentToRule(component, fieldMap, gridColumns)).filter(Boolean),
    options: buildFormCreateOptions(schema),
  }
}

export function convertComponentToRule(component = {}, fieldMap = new Map(), gridColumns = 2) {
  if (!component || typeof component !== 'object')
    return null
  const fieldCode = component.fieldBinding?.fieldCode || ''
  const fieldComponent = isFieldComponent(component)
  const field = fieldMap.get(fieldCode) || {}
  const rule = {
    type: resolveFormCreateType(component, field),
    field: fieldComponent && fieldCode ? fieldCode : undefined,
    title: component.label || field.fieldName || field.label || fieldCode || '字段',
    name: fieldComponent && fieldCode ? fieldCode : component.id,
    props: buildRuleProps(component, field),
    col: buildRuleCol(component, gridColumns),
    _forge: {
      id: component.id,
      componentKey: component.componentKey,
      fieldBinding: component.fieldBinding || {},
      layout: component.layout || {},
      visibility: component.visibility || {},
      props: component.props || {},
    },
  }
  const dragTag = resolveDesignerDragTag(component.componentKey)
  if (dragTag)
    rule._fc_drag_tag = dragTag

  if (component.props?.defaultValue !== undefined)
    rule.value = component.props.defaultValue
  applyFormCreateMeta(rule, component)
  if (component.validation?.required || component.validation?.rules?.length)
    rule.validate = buildRuleValidate(component)
  const options = resolveRuleOptions(component)
  if (options.length)
    rule.options = options
  if (Array.isArray(component.children) && component.children.length)
    rule.children = component.children.map(child => convertComponentToRule(child, fieldMap, gridColumns)).filter(Boolean)
  if (!fieldComponent && !rule.children?.length && component.props?.formCreateChild)
    rule.children = [component.props.formCreateChild]
  if (!rule.field)
    delete rule.field
  return rule
}

export function extractForgeSchemaFieldRefs(schema = {}) {
  return normalizeFormDesignerSchema(schema).components.flatMap(component => collectComponentFieldRefs(component)).filter((field, index, all) => field && all.indexOf(field) === index)
}

function buildFormCreateOptions(schema = {}) {
  const layout = schema.layout || {}
  return {
    form: {
      labelPosition: layout.labelPlacement === 'top' ? 'top' : 'right',
      labelWidth: `${Number(layout.labelWidth || 100)}px`,
      size: 'default',
    },
    submitBtn: false,
    resetBtn: false,
    _forge: {
      schemaVersion: schema.schemaVersion,
      formKey: schema.formKey,
      gridColumns: layout.gridColumns || 2,
      rowGap: Number(layout.rowGap ?? 16),
      columnGap: Number(layout.columnGap ?? 16),
    },
  }
}

function buildRuleCol(component = {}, gridColumns = 2) {
  const columns = Math.max(1, Number(gridColumns || 2))
  const span = Math.max(1, Math.min(columns, Number(component.layout?.span || 1)))
  return {
    span: Math.max(1, Math.min(24, Math.ceil(24 * span / columns))),
  }
}

function resolveFormCreateType(component = {}, field = {}) {
  const componentKey = component.componentKey || field.componentType || 'input'
  if (!isFieldComponent(component))
    return component.props?.__fcType || resolveLayoutFormCreateType(componentKey)
  const typeMap = {
    input: 'input',
    textarea: 'input',
    number: 'inputNumber',
    integer: 'inputNumber',
    money: 'inputNumber',
    select: 'select',
    dictSelect: 'select',
    radio: 'radio',
    checkbox: 'checkbox',
    date: 'datePicker',
    datetime: 'datePicker',
    time: 'timePicker',
    switch: 'switch',
    fileUpload: 'upload',
    imageUpload: 'upload',
    cascader: 'cascader',
    regionTreeSelect: 'elTreeSelect',
    orgTreeSelect: 'elTreeSelect',
    userSelect: 'select',
    objectReference: 'select',
  }
  return typeMap[componentKey] || 'input'
}

function resolveLayoutFormCreateType(componentKey = '') {
  const typeMap = {
    row: 'fcRow',
    fcRow: 'fcRow',
    col: 'col',
    card: 'elCard',
    elCard: 'elCard',
    tabs: 'elTabs',
    elTabs: 'elTabs',
    tabPane: 'elTabPane',
    elTabPane: 'elTabPane',
    collapse: 'elCollapse',
    elCollapse: 'elCollapse',
    collapseItem: 'elCollapseItem',
    elCollapseItem: 'elCollapseItem',
    divider: 'elDivider',
    elDivider: 'elDivider',
    title: 'fcTitle',
    fcTitle: 'fcTitle',
    table: 'fcTable',
    fcTable: 'fcTable',
    tableGrid: 'fcTableGrid',
    fcTableGrid: 'fcTableGrid',
    space: 'div',
  }
  return typeMap[componentKey] || componentKey || 'div'
}

function buildRuleProps(component = {}, field = {}) {
  const props = sanitizeRuleProps(component.props || {})
  const componentKey = component.componentKey || field.componentType || 'input'
  if (componentKey === 'textarea') {
    props.type = 'textarea'
    props.rows = props.rows || 3
    props.showWordLimit = props.showWordLimit !== false
  }
  if (componentKey === 'datetime') {
    props.type = 'datetime'
    props.format = props.format || 'YYYY-MM-DD HH:mm:ss'
    props.valueFormat = props.valueFormat || 'YYYY-MM-DD HH:mm:ss'
  }
  if (componentKey === 'date') {
    props.type = 'date'
    props.format = props.format || 'YYYY-MM-DD'
    props.valueFormat = props.valueFormat || 'YYYY-MM-DD'
  }
  if (componentKey === 'money') {
    props.precision = props.precision ?? field.precision ?? 2
    props.controls = props.controls ?? false
  }
  if (componentKey === 'imageUpload') {
    props.listType = props.listType || 'picture-card'
    props.accept = props.accept || 'image/*'
    props.autoUpload = false
  }
  if (componentKey === 'fileUpload') {
    props.autoUpload = false
  }
  if (componentKey === 'regionTreeSelect') {
    props.rootCode = props.rootCode || '150000'
    props.dataRight = props.dataRight !== false
    props.virtualDisabled = props.virtualDisabled !== false
    props.data = props.data || [
      { label: '行政区划示例', value: '150000', children: [{ label: '下级区划示例', value: '150100' }] },
    ]
    props.props = props.props || { label: 'label', value: 'value', children: 'children' }
  }
  if (componentKey === 'orgTreeSelect') {
    props.data = props.data || [
      { label: '组织示例', value: 1, children: [{ label: '下级组织示例', value: 2 }] },
    ]
    props.props = props.props || { label: 'label', value: 'value', children: 'children' }
  }
  if (componentKey === 'objectReference') {
    props.referenceObjectCode = props.referenceObjectCode || field.referenceObjectCode || ''
    props.referenceDisplayField = props.referenceDisplayField || field.referenceDisplayField || 'name'
  }
  if (component.visibility?.readonly)
    props.disabled = true
  return props
}

function sanitizeRuleProps(source = {}) {
  const props = { ...(source || {}) }
  delete props.__fcType
  delete props.__fc
  return props
}

function applyFormCreateMeta(rule = {}, component = {}) {
  if (isFieldComponent(component))
    return
  const meta = component.props?.__fc || {}
  ;['style', 'native', 'wrap', 'slot', 'effect'].forEach((key) => {
    if (meta[key] !== undefined)
      rule[key] = cloneValue(meta[key])
  })
}

function resolveRuleOptions(component = {}) {
  if (Array.isArray(component.props?.options))
    return component.props.options
  return getDesignPlaceholderOptions(component.componentKey)
}

function buildRuleValidate(component = {}) {
  const rules = Array.isArray(component.validation?.rules) ? [...component.validation.rules] : []
  if (component.validation?.required && !rules.some(rule => rule.required)) {
    rules.unshift({
      required: true,
      message: component.validation.requiredMessage || `请填写${component.label || '字段'}`,
      trigger: ['blur', 'change'],
    })
  }
  return rules
}

function collectComponentFieldRefs(component = {}) {
  const refs = []
  if (component.fieldBinding?.mode === 'field' && component.fieldBinding?.fieldCode)
    refs.push(component.fieldBinding.fieldCode)
  if (Array.isArray(component.children)) {
    component.children.forEach((child) => {
      refs.push(...collectComponentFieldRefs(child))
    })
  }
  return refs
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}
