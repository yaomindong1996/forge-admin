import { resolveForgeComponentKeyFromDragTag } from './forgeBusinessComponents'
import {
  applyGridColumnsToFormDesignerSchema,
  camelToSnake,
  FORM_DESIGNER_SCHEMA_VERSION,
  generateFieldCode,
  isFieldComponent,
  isTemporaryDesignerRef,
  normalizeDesignerComponentLabel,
  normalizeFieldBinding,
  normalizeFormDesignerSchema,
} from './formDesignerSchema'

export function formCreateToForgeSchema(input = {}) {
  const rules = normalizeRules(input.rules || input.rule || input.formCreateRule || [])
  const options = normalizeOptions(input.options || input.option || input.formCreateOptions || {})
  const fieldContext = createFieldContext(input.fields || input.existingFields || [])
  const layout = buildLayoutFromOptions(options)
  const schema = normalizeFormDesignerSchema({
    schemaVersion: FORM_DESIGNER_SCHEMA_VERSION,
    formKey: input.formKey || (input.objectCode ? `${input.objectCode}_default_form` : 'default_form'),
    formName: input.formName || input.objectName || '业务表单',
    layout,
    components: rules.map((rule, index) => convertRuleToComponent(rule, index, fieldContext, layout.gridColumns)).filter(Boolean),
    settings: {
      formCreateOptions: options,
    },
  })
  return applyGridColumnsToFormDesignerSchema(schema, layout.gridColumns)
}

export function convertRuleToComponent(rule = {}, index = 0, fieldContext = createFieldContext(), gridColumns = 2) {
  if (!rule || typeof rule !== 'object')
    return null
  const componentKey = resolveForgeComponentKey(rule)
  const fieldComponent = isFieldComponent({ componentKey })
  const forgeBinding = rule._forge?.fieldBinding || {}
  const sourceLabel = resolveRuleLabel(rule, componentKey, fieldComponent, forgeBinding)
  const label = fieldComponent ? sourceLabel : normalizeDesignerComponentLabel(componentKey, sourceLabel)
  const fieldCode = fieldComponent ? resolveFieldCode(rule, label, forgeBinding, fieldContext) : ''
  return {
    id: resolveComponentId(rule, componentKey, fieldCode, fieldComponent, index),
    componentKey,
    label,
    fieldBinding: normalizeFieldBinding({
      mode: fieldComponent ? forgeBinding.mode || (fieldCode ? 'field' : 'virtual') : 'virtual',
      fieldCode,
      columnName: forgeBinding.columnName || (fieldCode ? camelToSnake(fieldCode) : ''),
      createIfMissing: fieldComponent ? forgeBinding.createIfMissing ?? Boolean(fieldCode) : false,
      source: forgeBinding.source || 'designer',
      locked: Boolean(forgeBinding.locked),
    }, fieldCode),
    props: buildForgeProps(rule),
    layout: buildForgeLayout(rule, gridColumns),
    validation: buildForgeValidation(rule, componentKey, label),
    visibility: buildForgeVisibility(rule),
    children: normalizeRules(rule.children).map((child, childIndex) => convertRuleToComponent(child, childIndex, fieldContext, gridColumns)).filter(Boolean),
  }
}

function resolveRuleLabel(rule = {}, componentKey = '', fieldComponent = false, forgeBinding = {}) {
  const candidates = [
    rule.title,
    rule.label,
    rule.props?.header,
    rule.props?.label,
    rule.props?.title,
    forgeBinding.fieldCode,
    rule.field,
    rule.name,
  ].filter(value => value !== undefined && value !== null && String(value).trim())
  if (fieldComponent)
    return candidates[0] || '字段'
  return candidates.find(value => !isTemporaryDesignerRef(value))
    || normalizeDesignerComponentLabel(componentKey, '')
}

function resolveComponentId(rule = {}, componentKey = '', fieldCode = '', fieldComponent = false, index = 0) {
  const candidates = [rule._forge?.id, rule.id, rule.name].filter(value => value !== undefined && value !== null && String(value).trim())
  const reusable = candidates.find(value => !isTemporaryDesignerRef(value))
  if (reusable)
    return reusable
  return fieldComponent ? `cmp_${fieldCode || index}` : `cmp_${componentKey || 'layout'}_${index}`
}

export function extractFormCreateFieldRefs(rules = []) {
  const refs = []
  walkRules(rules, (rule) => {
    if (rule.field && !refs.includes(rule.field))
      refs.push(rule.field)
  })
  return refs
}

function normalizeRules(rules = []) {
  if (Array.isArray(rules))
    return cloneValue(rules)
  if (typeof rules === 'string' && rules.trim()) {
    try {
      const parsed = JSON.parse(rules)
      return Array.isArray(parsed) ? parsed : []
    }
    catch {
      return []
    }
  }
  return []
}

function normalizeOptions(options = {}) {
  if (!options)
    return {}
  if (typeof options === 'string' && options.trim()) {
    try {
      const parsed = JSON.parse(options)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return typeof options === 'object' && !Array.isArray(options) ? cloneValue(options) : {}
}

function createFieldContext(fields = []) {
  const existingFieldCodes = new Set((Array.isArray(fields) ? fields : [])
    .map(field => field?.fieldCode || field?.field)
    .filter(Boolean))
  return {
    existingFieldCodes,
    usedFieldCodes: new Set(existingFieldCodes),
  }
}

function resolveFieldCode(rule = {}, label = '', forgeBinding = {}, fieldContext = createFieldContext()) {
  if (forgeBinding.mode === 'virtual')
    return forgeBinding.fieldCode || ''
  if (forgeBinding.fieldCode) {
    if (shouldRegenerateDesignerFieldCode(forgeBinding, label, fieldContext)) {
      const generated = generateFieldCode(label)
      return reserveGeneratedFieldCode(generated, fieldContext)
    }
    fieldContext.usedFieldCodes.add(forgeBinding.fieldCode)
    return forgeBinding.fieldCode
  }

  const ruleField = rule.field || rule.name || ''
  if (ruleField && !isTemporaryDesignerField(ruleField)) {
    fieldContext.usedFieldCodes.add(ruleField)
    return ruleField
  }

  const generated = generateFieldCode(label)
  return reserveGeneratedFieldCode(generated, fieldContext)
}

function reserveGeneratedFieldCode(value, fieldContext) {
  const base = value || 'field'
  if (!fieldContext.usedFieldCodes.has(base)) {
    fieldContext.usedFieldCodes.add(base)
    return base
  }
  for (let index = 2; index < 1000; index += 1) {
    const candidate = `${base}${index}`
    if (!fieldContext.usedFieldCodes.has(candidate)) {
      fieldContext.usedFieldCodes.add(candidate)
      return candidate
    }
  }
  return `${base}${Date.now()}`
}

function isTemporaryDesignerField(value) {
  return /^field_\d+(?:_\d+)?$/.test(String(value || '')) || isTemporaryDesignerRef(value)
}

function shouldRegenerateDesignerFieldCode(forgeBinding = {}, label = '', fieldContext = createFieldContext()) {
  const fieldCode = forgeBinding.fieldCode || ''
  if (!fieldCode || fieldContext.existingFieldCodes.has(fieldCode))
    return false
  if (forgeBinding.source !== 'designer' || forgeBinding.createIfMissing === false)
    return false
  if (isGenericDesignerFieldLabel(label))
    return false
  return isGenericDesignerFieldCode(fieldCode)
}

function isGenericDesignerFieldCode(value = '') {
  const text = String(value || '').trim()
  if (/^field[0-9a-z]{4,}$/i.test(text))
    return true
  return [
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
    'selector',
    'radio',
    'checkbox',
    'dictSelect',
    'cascader',
    'field',
  ].includes(text)
}

function isGenericDesignerFieldLabel(value = '') {
  return [
    '字段',
    '输入框',
    '多行输入',
    '数字输入框',
    '选择器',
    '单选框',
    '多选框',
    '日期选择器',
    '时间选择器',
    '开关',
  ].includes(String(value || '').trim())
}

function buildLayoutFromOptions(options = {}) {
  const form = options.form || {}
  const forge = options._forge || {}
  const labelPosition = form.labelPosition || ''
  const layout = {
    labelPlacement: labelPosition === 'top' ? 'top' : 'left',
    labelAlign: normalizeLabelAlign(forge.labelAlign || form.labelAlign || (labelPosition === 'left' ? 'left' : 'right')),
    labelWidth: Number.parseInt(form.labelWidth || 100, 10) || 100,
    size: normalizeFormSize(forge.size || form.size),
    showFeedback: resolveBoolean(forge.showFeedback ?? form.showMessage, true),
    hideRequiredAsterisk: resolveBoolean(forge.hideRequiredAsterisk ?? form.hideRequiredAsterisk, false),
    inlineFeedback: resolveBoolean(forge.inlineFeedback ?? form.inlineMessage, false),
    gridColumns: Number(forge.gridColumns || 2),
    rowGap: Number(forge.rowGap ?? 16),
    columnGap: Number(forge.columnGap ?? 16),
  }
  if (form.style !== undefined)
    layout.formStyle = cloneValue(form.style)
  if (form.className || form.class)
    layout.formClass = form.className || form.class
  return {
    ...layout,
  }
}

function resolveForgeComponentKey(rule = {}) {
  if (rule._forge?.componentKey)
    return rule._forge.componentKey
  const dragComponentKey = resolveForgeComponentKeyFromDragTag(rule._fc_drag_tag)
  if (dragComponentKey)
    return dragComponentKey
  const type = rule.type || 'input'
  if (type === 'input' && rule.props?.type === 'textarea')
    return 'textarea'
  if (type === 'upload' && rule.props?.accept === 'image/*')
    return 'imageUpload'
  const typeMap = {
    input: 'input',
    textarea: 'textarea',
    inputNumber: 'number',
    select: 'select',
    radio: 'radio',
    checkbox: 'checkbox',
    datePicker: rule.props?.type === 'datetime' ? 'datetime' : 'date',
    timePicker: 'time',
    switch: 'switch',
    upload: rule.props?.listType === 'picture-card' ? 'imageUpload' : 'fileUpload',
    cascader: 'cascader',
    tree: 'orgTreeSelect',
    elTreeSelect: 'orgTreeSelect',
    fcRow: 'fcRow',
    row: 'fcRow',
    col: 'col',
    elCard: 'elCard',
    card: 'elCard',
    elTabs: 'elTabs',
    tabs: 'elTabs',
    elTabPane: 'elTabPane',
    tabPane: 'elTabPane',
    elCollapse: 'elCollapse',
    collapse: 'elCollapse',
    elCollapseItem: 'elCollapseItem',
    collapseItem: 'elCollapseItem',
    elDivider: 'elDivider',
    divider: 'elDivider',
    fcTitle: 'fcTitle',
    title: 'fcTitle',
    fcTable: 'fcTable',
    table: 'fcTable',
    fcTableGrid: 'fcTableGrid',
    tableGrid: 'fcTableGrid',
    div: rule.native || rule.wrap ? 'space' : 'div',
  }
  return typeMap[type] || type
}

function buildForgeProps(rule = {}) {
  const componentKey = resolveForgeComponentKey(rule)
  const fieldComponent = isFieldComponent({ componentKey })
  const props = {
    ...(rule.props || {}),
  }
  if (['regionTreeSelect', 'orgTreeSelect'].includes(componentKey)) {
    delete props.data
    delete props.props
    delete props.nodeKey
  }
  if (['fileUpload', 'imageUpload'].includes(componentKey) && props.action === '/')
    delete props.action
  if (Array.isArray(rule.options) && ['select', 'radio', 'checkbox', 'cascader'].includes(componentKey))
    props.options = cloneValue(rule.options)
  if (rule.value !== undefined)
    props.defaultValue = cloneValue(rule.value)
  if (rule._forge?.props)
    Object.assign(props, cloneValue(rule._forge.props))
  const meta = pickFormCreateMeta(rule)
  if (Object.keys(meta).length)
    props.__fc = meta
  if (!fieldComponent) {
    props.__fcType = rule.type || componentKey
    if (Array.isArray(rule.children) && rule.children.length && rule.children.every(child => typeof child !== 'object'))
      props.formCreateChild = rule.children.join('')
  }
  return props
}

function pickFormCreateMeta(rule = {}) {
  const meta = {}
  ;['style', 'class', 'className', 'native', 'wrap', 'slot', 'effect'].forEach((key) => {
    if (rule[key] !== undefined)
      meta[key] = cloneValue(rule[key])
  })
  return meta
}

function buildForgeLayout(rule = {}, gridColumns = 2) {
  const span = resolveForgeSpan(rule, gridColumns)
  return {
    span,
    align: normalizeAlign(rule._forge?.layout?.align),
    ...(rule._forge?.layout?.width ? { width: rule._forge.layout.width } : {}),
    ...(rule._forge?.layout?.labelWidth ? { labelWidth: rule._forge.layout.labelWidth } : {}),
  }
}

function resolveForgeSpan(rule = {}, gridColumns = 2) {
  const columns = Math.max(1, Number(gridColumns || 2))
  const colSpan = Number(rule.col?.span)
  if (colSpan > 0)
    return Math.max(1, Math.min(columns, Math.ceil(columns * Math.min(24, colSpan) / 24)))
  return Math.max(1, Math.min(columns, Number(rule._forge?.layout?.span || rule.props?.span || 1)))
}

function buildForgeValidation(rule = {}, componentKey = 'input', label = '') {
  const validate = Array.isArray(rule.validate) ? rule.validate : []
  const requiredFromSwitch = resolveRequiredSwitch(rule.$required)
  const requiredRule = validate.find(item => item?.required)
  const requiredMessage = resolveRequiredMessage(requiredFromSwitch, requiredRule, componentKey, label)
  const rules = validate.map(item => ({ ...item }))
  if (requiredFromSwitch.required && !rules.some(item => item?.required)) {
    rules.unshift({
      required: true,
      message: requiredMessage,
      trigger: resolveRequiredTrigger(componentKey),
    })
  }
  return {
    required: Boolean(requiredRule) || requiredFromSwitch.required,
    requiredMessage,
    rules,
  }
}

function buildForgeVisibility(rule = {}) {
  return {
    hidden: Boolean(rule.hidden || rule._forge?.visibility?.hidden),
    readonly: Boolean(rule.props?.disabled || rule._forge?.visibility?.readonly),
  }
}

function walkRules(rules = [], visitor) {
  ;(Array.isArray(rules) ? rules : []).forEach((rule) => {
    if (!rule || typeof rule !== 'object')
      return
    visitor(rule)
    if (Array.isArray(rule.children))
      walkRules(rule.children, visitor)
  })
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

function resolveBoolean(value, fallback = false) {
  if (value === undefined || value === null || value === '')
    return fallback
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  return !['false', '0', 'no'].includes(String(value).toLowerCase())
}

function resolveRequiredSwitch(value) {
  if (value === undefined || value === null || value === false || value === 'false' || value === 0)
    return { required: false, message: '' }
  if (typeof value === 'string')
    return { required: true, message: value }
  return { required: true, message: '' }
}

function resolveRequiredMessage(requiredFromSwitch, requiredRule, componentKey, label) {
  return requiredFromSwitch.message || requiredRule?.message || buildRequiredMessage(componentKey, label)
}

function resolveRequiredTrigger(componentKey) {
  return ['select', 'radio', 'checkbox', 'dictSelect', 'cascader', 'date', 'datetime', 'time', 'regionTreeSelect', 'orgTreeSelect', 'userSelect', 'fileUpload', 'imageUpload', 'objectReference'].includes(componentKey)
    ? 'change'
    : ['blur', 'change']
}

function buildRequiredMessage(componentKey, label) {
  const prefix = ['select', 'radio', 'checkbox', 'dictSelect', 'cascader', 'date', 'datetime', 'time', 'regionTreeSelect', 'orgTreeSelect', 'userSelect', 'fileUpload', 'imageUpload', 'objectReference'].includes(componentKey)
    ? '请选择'
    : '请输入'
  return `${prefix}${label || '字段'}`
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}
