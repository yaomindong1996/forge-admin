import { resolveForgeComponentKeyFromDragTag } from './forgeBusinessComponents'
import {
  camelToSnake,
  FORM_DESIGNER_SCHEMA_VERSION,
  generateFieldCode,
  normalizeFieldBinding,
  normalizeFormDesignerSchema,
} from './formDesignerSchema'

export function formCreateToForgeSchema(input = {}) {
  const rules = normalizeRules(input.rules || input.rule || input.formCreateRule || [])
  const options = normalizeOptions(input.options || input.option || input.formCreateOptions || {})
  const fieldContext = createFieldContext(input.fields || input.existingFields || [])
  const layout = buildLayoutFromOptions(options)
  return normalizeFormDesignerSchema({
    schemaVersion: FORM_DESIGNER_SCHEMA_VERSION,
    formKey: input.formKey || (input.objectCode ? `${input.objectCode}_default_form` : 'default_form'),
    formName: input.formName || input.objectName || '业务表单',
    layout,
    components: rules.map((rule, index) => convertRuleToComponent(rule, index, fieldContext, layout.gridColumns)).filter(Boolean),
    settings: {
      formCreateOptions: options,
    },
  })
}

export function convertRuleToComponent(rule = {}, index = 0, fieldContext = createFieldContext(), gridColumns = 2) {
  if (!rule || typeof rule !== 'object')
    return null
  const componentKey = resolveForgeComponentKey(rule)
  const forgeBinding = rule._forge?.fieldBinding || {}
  const label = rule.title || rule.label || forgeBinding.fieldCode || rule.field || rule.name || '字段'
  const fieldCode = resolveFieldCode(rule, label, forgeBinding, fieldContext)
  return {
    id: rule._forge?.id || rule.id || `cmp_${fieldCode || index}`,
    componentKey,
    label,
    fieldBinding: normalizeFieldBinding({
      mode: forgeBinding.mode || (fieldCode ? 'field' : 'virtual'),
      fieldCode,
      columnName: forgeBinding.columnName || (fieldCode ? camelToSnake(fieldCode) : ''),
      createIfMissing: forgeBinding.createIfMissing ?? Boolean(fieldCode),
      source: forgeBinding.source || 'designer',
      locked: Boolean(forgeBinding.locked),
    }, fieldCode),
    props: buildForgeProps(rule),
    layout: buildForgeLayout(rule, gridColumns),
    validation: buildForgeValidation(rule),
    visibility: buildForgeVisibility(rule),
    children: normalizeRules(rule.children).map((child, childIndex) => convertRuleToComponent(child, childIndex, fieldContext, gridColumns)).filter(Boolean),
  }
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
  return {
    usedFieldCodes: new Set((Array.isArray(fields) ? fields : [])
      .map(field => field?.fieldCode || field?.field)
      .filter(Boolean)),
  }
}

function resolveFieldCode(rule = {}, label = '', forgeBinding = {}, fieldContext = createFieldContext()) {
  if (forgeBinding.mode === 'virtual')
    return forgeBinding.fieldCode || ''
  if (forgeBinding.fieldCode) {
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
  return /^field_\d+(?:_\d+)?$/.test(String(value || ''))
}

function buildLayoutFromOptions(options = {}) {
  const form = options.form || {}
  return {
    labelPlacement: form.labelPosition === 'top' ? 'top' : 'left',
    labelWidth: Number.parseInt(form.labelWidth || 100, 10) || 100,
    gridColumns: Number(options._forge?.gridColumns || 2),
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
  if (type === 'select' && rule.props?.dictType)
    return 'dictSelect'
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
  }
  return typeMap[type] || type
}

function buildForgeProps(rule = {}) {
  const componentKey = resolveForgeComponentKey(rule)
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
  return props
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

function buildForgeValidation(rule = {}) {
  const validate = Array.isArray(rule.validate) ? rule.validate : []
  const requiredRule = validate.find(item => item?.required)
  return {
    required: Boolean(requiredRule),
    requiredMessage: requiredRule?.message || '',
    rules: validate.map(item => ({ ...item })),
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

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}
