export function getNestedRuntimeValue(source = {}, path = '') {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

export function buildRuntimeRuleContext(context = {}) {
  const record = context.record || context.row || context.formData || context.data || {}
  return {
    ...context,
    record,
    row: context.row || record,
    formData: context.formData || record,
    data: context.data || record,
    route: context.route || {},
    user: context.user || {},
  }
}

/** schema 上的 disabled/readonly 允许是函数，留给 AiFormItem 按 formData 求值；这里只认静态布尔。 */
function isStaticTrueFlag(value) {
  return typeof value !== 'function' && Boolean(value)
}

export function resolveRuntimeControl(target = {}, context = {}) {
  const runtimeContext = buildRuntimeRuleContext(context)
  const visibility = target.visibility || {}
  const behavior = target.behavior || {}
  const rules = normalizeRuntimeRules(target)
  const control = {
    visible: !(target.hidden === true || target.visible === false || visibility.hidden === true),
    readonly: isStaticTrueFlag(target.readonly)
      || isStaticTrueFlag(target.props?.readonly)
      || isStaticTrueFlag(visibility.readonly)
      || isStaticTrueFlag(behavior.readonly),
    disabled: isStaticTrueFlag(target.disabled)
      || isStaticTrueFlag(target.props?.disabled)
      || isStaticTrueFlag(visibility.disabled)
      || isStaticTrueFlag(behavior.disabled),
    required: target.required,
    style: {},
    className: '',
    matchedRules: [],
  }

  // “满足时显示”规则的默认态应为隐藏，否则字段初始可见时规则看起来不会生效。
  // 设计器会写入 whenUnmatched；不带该标记的旧规则继续沿用原有初始可见性。
  const visibilityDefaults = rules
    .filter(rule => rule?.enabled !== false)
    .map(rule => normalizeRuleEffect(rule))
    .filter(effect => effect.whenUnmatched === 'hidden' || effect.whenUnmatched === 'visible')
  const defaultVisibility = visibilityDefaults.at(-1)?.whenUnmatched
  if (defaultVisibility === 'hidden')
    control.visible = false
  else if (defaultVisibility === 'visible')
    control.visible = true

  if (typeof target.vIf === 'function')
    control.visible = target.vIf(runtimeContext.formData, runtimeContext)
  else if (typeof target.vIf === 'boolean')
    control.visible = target.vIf

  for (const rule of rules) {
    if (rule?.enabled === false)
      continue
    if (!matchRuntimeRule(rule, runtimeContext))
      continue
    control.matchedRules.push(rule)
    const effect = normalizeRuleEffect(rule)
    if (Object.prototype.hasOwnProperty.call(effect, 'visible'))
      control.visible = effect.visible !== false
    if (effect.hidden === true)
      control.visible = false
    if (Object.prototype.hasOwnProperty.call(effect, 'readonly'))
      control.readonly = effect.readonly === true
    if (Object.prototype.hasOwnProperty.call(effect, 'disabled'))
      control.disabled = effect.disabled === true
    if (Object.prototype.hasOwnProperty.call(effect, 'required'))
      control.required = effect.required === true
    if (effect.textColor)
      control.style.color = effect.textColor
    if (effect.backgroundColor)
      control.style.backgroundColor = effect.backgroundColor
    if (effect.className)
      control.className = [control.className, effect.className].filter(Boolean).join(' ')
    if (effect.style && typeof effect.style === 'object')
      control.style = { ...control.style, ...effect.style }
  }

  if (control.readonly)
    control.disabled = true

  return control
}

export function applyRuntimeControl(target = {}, context = {}) {
  const control = resolveRuntimeControl(target, context)
  if (control.visible === false)
    return null
  const props = { ...(target.props || {}) }
  const next = {
    ...target,
    props,
    __runtimeControl: control,
  }
  if (control.readonly) {
    next.readonly = true
    next.disabled = true
    props.readonly = true
    props.disabled = true
  }
  else if (control.disabled) {
    next.disabled = true
    props.disabled = true
  }
  if (Object.prototype.hasOwnProperty.call(control, 'required') && control.required !== undefined)
    next.required = control.required
  if (Object.keys(control.style || {}).length) {
    next.style = {
      ...(target.style || {}),
      ...control.style,
    }
  }
  if (control.className)
    next.className = [target.className, control.className].filter(Boolean).join(' ')
  return next
}

export function matchRuntimeRule(rule = {}, context = {}) {
  const groups = Array.isArray(rule.groups) ? rule.groups : []
  if (groups.length) {
    const mode = rule.mode === 'any' ? 'any' : 'all'
    const matcher = group => matchRuntimeRuleGroup(group, context)
    return mode === 'any' ? groups.some(matcher) : groups.every(matcher)
  }
  const conditions = Array.isArray(rule.conditions) ? rule.conditions : []
  if (!conditions.length && rule.field)
    return matchRuntimeCondition(rule, context)
  if (!conditions.length)
    return true
  const mode = rule.mode === 'any' ? 'any' : 'all'
  const matcher = condition => matchRuntimeCondition(condition, context)
  return mode === 'any' ? conditions.some(matcher) : conditions.every(matcher)
}

export function matchRuntimeRuleGroup(group = {}, context = {}) {
  const conditions = Array.isArray(group.conditions) ? group.conditions : []
  if (!conditions.length)
    return true
  const mode = group.mode === 'any' ? 'any' : 'all'
  const matcher = condition => matchRuntimeCondition(condition, context)
  return mode === 'any' ? conditions.some(matcher) : conditions.every(matcher)
}

export function matchRuntimeCondition(condition = {}, context = {}) {
  if (condition.enabled === false)
    return true
  if (condition.expression)
    return matchSimpleExpression(condition.expression, context.record || context.data || {})
  const actual = resolveConditionActualValue(condition, context)
  const expected = resolveExpectedValue(condition, context)
  const operator = condition.operator || condition.op || 'eq'
  return compareRuntimeValues(actual, expected, operator)
}

export function hasRuntimeVisibilityRules(target = {}) {
  return normalizeRuntimeRules(target).some((rule) => {
    if (rule?.enabled === false)
      return false
    const effect = normalizeRuleEffect(rule)
    return Object.prototype.hasOwnProperty.call(effect, 'visible') || effect.hidden === true
  })
}

export function matchSimpleExpression(expression = '', record = {}) {
  const text = String(expression || '').trim()
  if (!text)
    return true
  const lowerText = text.toLowerCase()
  const inIndex = lowerText.indexOf(' in ')
  if (inIndex > 0) {
    const actual = getNestedRuntimeValue(record, text.slice(0, inIndex).trim())
    return text.slice(inIndex + 4).split(',').map(item => trimRuntimeValue(item)).includes(String(actual ?? ''))
  }
  const operator = ['!=', '==', '>=', '<=', '>', '<', '='].find(item => text.includes(item))
  if (!operator)
    return true
  const [fieldName, ...expectedParts] = text.split(operator)
  const actual = getNestedRuntimeValue(record, fieldName.trim())
  const expected = trimRuntimeValue(expectedParts.join(operator))
  return compareRuntimeValues(actual, expected, normalizeExpressionOperator(operator))
}

function normalizeRuntimeRules(target = {}) {
  return [
    ...(Array.isArray(target.runtimeRules) ? target.runtimeRules : []),
    ...(Array.isArray(target.props?.runtimeRules) ? target.props.runtimeRules : []),
    ...(Array.isArray(target.rules?.runtime) ? target.rules.runtime : []),
    ...(Array.isArray(target.visibilityRules) ? target.visibilityRules : []),
    ...(Array.isArray(target.behaviorRules) ? target.behaviorRules : []),
    ...(Array.isArray(target.displayRules) ? target.displayRules : []),
  ]
}

function normalizeRuleEffect(rule = {}) {
  return {
    ...(rule.effect || {}),
    ...(rule.actions || {}),
    ...(rule.then || {}),
    ...Object.fromEntries(['visible', 'hidden', 'readonly', 'disabled', 'required', 'textColor', 'backgroundColor', 'className', 'style', 'whenUnmatched']
      .filter(key => Object.prototype.hasOwnProperty.call(rule, key))
      .map(key => [key, rule[key]])),
  }
}

function resolveConditionActualValue(condition = {}, context = {}) {
  const source = condition.source || condition.scope || 'record'
  const path = condition.field || condition.path || condition.key || ''
  if (source === 'query' || source === 'routeQuery')
    return getNestedRuntimeValue(context.route?.query || context.query || {}, path)
  if (source === 'params' || source === 'routeParams')
    return getNestedRuntimeValue(context.route?.params || context.params || {}, path)
  if (source === 'route')
    return getNestedRuntimeValue(context.route || {}, path)
  if (source === 'user')
    return getNestedRuntimeValue(context.user || {}, path)
  if (source === 'formData')
    return getNestedRuntimeValue(context.formData || {}, path)
  if (source === 'row')
    return getNestedRuntimeValue(context.row || {}, path)
  return getNestedRuntimeValue(context.record || context.data || {}, path)
}

function resolveExpectedValue(condition = {}, context = {}) {
  if (condition.valueSource === 'field')
    return getNestedRuntimeValue(context.record || context.formData || context.row || {}, condition.valueField || condition.expectedField || '')
  if (Object.prototype.hasOwnProperty.call(condition, 'expected'))
    return condition.expected
  return condition.value
}

function compareRuntimeValues(actual, expected, operator = 'eq') {
  const op = normalizeExpressionOperator(operator)
  if (op === 'empty')
    return isEmptyRuntimeValue(actual)
  if (op === 'notEmpty')
    return !isEmptyRuntimeValue(actual)
  if (op === 'contains')
    return String(actual ?? '').includes(String(expected ?? ''))
  if (op === 'notContains')
    return !String(actual ?? '').includes(String(expected ?? ''))
  if (op === 'in') {
    const expectedList = Array.isArray(expected) ? expected : String(expected ?? '').split(',').map(item => trimRuntimeValue(item))
    return expectedList.map(item => String(item)).includes(String(actual ?? ''))
  }
  if (op === 'notIn') {
    const expectedList = Array.isArray(expected) ? expected : String(expected ?? '').split(',').map(item => trimRuntimeValue(item))
    return !expectedList.map(item => String(item)).includes(String(actual ?? ''))
  }
  if (['gt', 'gte', 'lt', 'lte'].includes(op)) {
    const actualNumber = Number(actual)
    const expectedNumber = Number(expected)
    if (!Number.isFinite(actualNumber) || !Number.isFinite(expectedNumber))
      return false
    if (op === 'gt')
      return actualNumber > expectedNumber
    if (op === 'gte')
      return actualNumber >= expectedNumber
    if (op === 'lt')
      return actualNumber < expectedNumber
    return actualNumber <= expectedNumber
  }
  const actualText = String(actual ?? '')
  const expectedText = String(expected ?? '')
  return op === 'ne' ? actualText !== expectedText : actualText === expectedText
}

function normalizeExpressionOperator(operator = 'eq') {
  const map = {
    '=': 'eq',
    '==': 'eq',
    'eq': 'eq',
    '!=': 'ne',
    'ne': 'ne',
    '>': 'gt',
    'gt': 'gt',
    '>=': 'gte',
    'gte': 'gte',
    '<': 'lt',
    'lt': 'lt',
    '<=': 'lte',
    'lte': 'lte',
    'in': 'in',
    'notIn': 'notIn',
    'empty': 'empty',
    'notEmpty': 'notEmpty',
    'contains': 'contains',
    'notContains': 'notContains',
  }
  return map[operator] || 'eq'
}

function isEmptyRuntimeValue(value) {
  return value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0)
}

function trimRuntimeValue(value = '') {
  return String(value ?? '').trim().replace(/^['"]|['"]$/g, '')
}
