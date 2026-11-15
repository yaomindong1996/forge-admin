import formCreate from '@form-create/element-ui'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@form-create/element-ui/src/style/index.css'

const INSTALL_KEY = '__forgeFormCreateInstalled'
export const DEFAULT_FORM_ITEM_GAP = '20px'
const DEFAULT_RULE_TITLE_MAP = {
  input: '输入框',
  textarea: '多行输入框',
  inputNumber: '计数器',
  number: '计数器',
  select: '选择器',
  radio: '单选框',
  checkbox: '多选框',
  datePicker: '日期',
  timePicker: '时间',
  switch: '开关',
  cascader: '级联选择器',
  tree: '树形控件',
  elTreeSelect: '树形选择器',
  upload: '上传',
}
const GENERIC_ENGLISH_TITLES = new Set([
  'Input',
  'Textarea',
  'InputNumber',
  'Number',
  'Select',
  'Radio',
  'Checkbox',
  'DatePicker',
  'TimePicker',
  'Switch',
  'Cascader',
  'Tree',
  'TreeSelect',
  'Upload',
])

export function installFormCreate(app) {
  if (!app || app._context.provides[INSTALL_KEY])
    return
  // 运行态下拉默认占位依赖 Element Plus 语言包；未装 zh-cn 时显示英文 Select
  app.use(ElementPlus, { locale: zhCn })
  app.use(formCreate)
  app._context.provides[INSTALL_KEY] = true
}

export function getFormCreateComponent() {
  return formCreate.$form()
}

export function normalizeFormCreateRules(source) {
  const rules = parseRules(source)
  return rules.map(rule => normalizeRule(rule)).filter(Boolean)
}

export function normalizeFormCreateOptions(source = {}) {
  const parsed = parseObject(source)
  return {
    ...buildDefaultFormOptions(),
    ...parsed,
    form: {
      ...buildDefaultFormOptions().form,
      ...(parsed.form || {}),
    },
  }
}

export function buildDefaultFormOptions() {
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

export function cloneValue(value) {
  if (value === undefined)
    return undefined
  return JSON.parse(JSON.stringify(value ?? null))
}

function parseRules(source) {
  if (Array.isArray(source))
    return cloneValue(source)
  if (typeof source === 'string' && source.trim()) {
    try {
      const parsed = JSON.parse(source)
      return Array.isArray(parsed) ? parsed : []
    }
    catch {
      return []
    }
  }
  return []
}

function parseObject(source) {
  if (!source)
    return {}
  if (typeof source === 'string' && source.trim()) {
    try {
      const parsed = JSON.parse(source)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return typeof source === 'object' && !Array.isArray(source) ? cloneValue(source) : {}
}

function normalizeRule(rule) {
  if (!rule || typeof rule !== 'object')
    return null

  const next = cloneValue(rule)
  const isFormCreateRule = Boolean(next.title || next.validate || next.effect || next.control)

  if (!isFormCreateRule) {
    next.title = next.label || next.field || '字段'
    next.type = normalizeLegacyType(next.type)
    next.props = normalizeLegacyProps(next)
    next.validate = normalizeLegacyValidate(next)
    delete next.label
    delete next.rules
    delete next.required
    delete next.disabled
    delete next.defaultValue
  }
  else {
    next.type = normalizeLegacyType(next.type)
    next.props = normalizeRuleProps(next.props || {})
    if (!next.validate && Array.isArray(next.rules))
      next.validate = normalizeValidateRules(next.rules, next.title)
    delete next.rules
  }

  if (next.value === undefined && rule.defaultValue !== undefined)
    next.value = rule.defaultValue

  normalizeRuleTitle(next)
  normalizeRuleWrap(next)

  if (Array.isArray(next.children))
    next.children = next.children.map(child => normalizeRule(child)).filter(Boolean)

  return next
}

function normalizeRuleWrap(rule) {
  const wrap = rule.wrap && typeof rule.wrap === 'object' && !Array.isArray(rule.wrap)
    ? { ...rule.wrap }
    : {}
  const style = wrap.style && typeof wrap.style === 'object' && !Array.isArray(wrap.style)
    ? { ...wrap.style }
    : {}
  if (style.marginBottom === undefined || style.marginBottom === null || style.marginBottom === '')
    style.marginBottom = DEFAULT_FORM_ITEM_GAP
  else if (typeof style.marginBottom === 'number')
    style.marginBottom = `${style.marginBottom}px`
  wrap.style = style
  rule.wrap = wrap
}

function normalizeRuleTitle(rule) {
  const defaultTitle = DEFAULT_RULE_TITLE_MAP[rule.type]
  if (!defaultTitle)
    return
  const title = String(rule.title || '').trim()
  if (!title || GENERIC_ENGLISH_TITLES.has(title))
    rule.title = defaultTitle
}

function normalizeLegacyType(type) {
  const typeMap = {
    inputNumber: 'inputNumber',
    input_number: 'inputNumber',
    date: 'datePicker',
    datetime: 'datePicker',
    datePicker: 'datePicker',
    textarea: 'textarea',
    timePicker: 'timePicker',
    treeSelect: 'tree',
    richText: 'input',
    colorPicker: 'input',
  }
  return typeMap[type] || type || 'input'
}

function normalizeLegacyProps(rule) {
  const props = normalizeRuleProps(rule.props || {})
  if (rule.disabled !== undefined)
    props.disabled = rule.disabled
  if (rule.type === 'textarea') {
    props.type = 'textarea'
    props.rows = props.rows || 3
  }
  if (rule.type === 'datePicker' && props.type === 'datetime') {
    props.valueFormat = props.valueFormat || 'YYYY-MM-DD HH:mm:ss'
    props.format = props.format || 'YYYY-MM-DD HH:mm:ss'
  }
  return props
}

function normalizeRuleProps(props) {
  const next = { ...props }
  if (next.maxLength !== undefined && next.maxlength === undefined) {
    next.maxlength = next.maxLength
    delete next.maxLength
  }
  if (next.showCount !== undefined && next.showWordLimit === undefined)
    next.showWordLimit = next.showCount
  if (next.checkedText !== undefined && next.activeText === undefined)
    next.activeText = next.checkedText
  if (next.uncheckedText !== undefined && next.inactiveText === undefined)
    next.inactiveText = next.uncheckedText
  if (next.maxCount !== undefined && next.limit === undefined)
    next.limit = next.maxCount
  return next
}

function normalizeLegacyValidate(rule) {
  const validate = normalizeValidateRules(rule.rules, rule.label || rule.field)
  if (rule.required && !validate.some(item => item.required)) {
    validate.push({
      required: true,
      message: `请填写${rule.label || rule.field || '字段'}`,
      trigger: ['blur', 'change'],
    })
  }
  return validate
}

function normalizeValidateRules(rules, title) {
  if (!Array.isArray(rules))
    return []
  return rules.map(item => ({
    ...item,
    message: item.message || `请填写${title || '字段'}`,
    trigger: item.trigger || ['blur', 'change'],
  }))
}
