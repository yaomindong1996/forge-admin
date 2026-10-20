import { normalizeConditionDataType, normalizeConditionRule, parseConditionExpression } from '../panel/condition-expression.js'

const OPERATORS = {
  eq: '=',
  ne: '≠',
  gt: '>',
  ge: '≥',
  lt: '<',
  le: '≤',
  between: '介于',
  contains: '包含',
  notContains: '不包含',
  empty: '为空',
  notEmpty: '不为空',
}

/** 只展示能用真实字段目录安全解释的条件，未知表达式仍走原来的“条件已设”。 */
export function summarizeCondition(edge, fields = []) {
  if (edge?.isDefault)
    return ''
  const rules = edge?.conditionRules?.length
    ? edge.conditionRules.map(normalizeConditionRule)
    : parseConditionExpression(edge?.condition, fields).rules
  if (rules.length !== 1)
    return rules.length > 1 ? `${rules.length} 条条件` : ''
  const rule = rules[0]
  const field = fields.find(item => (item.field || item.fieldCode || item.name) === rule.field)
  const label = field?.label || field?.title
  const operator = OPERATORS[rule.operator]
  if (!label || !operator)
    return ''
  if (['empty', 'notEmpty'].includes(rule.operator))
    return `${label} ${operator}`
  const value = formatValue(rule.value, field)
  return rule.operator === 'between'
    ? `${label} ${operator} ${value} ～ ${formatValue(rule.endValue, field)}`
    : `${label} ${operator} ${value}`
}

function formatValue(value, field) {
  const text = String(value ?? '')
  const isNumber = normalizeConditionDataType(field.dataType || field.type) === 'number'
  if (!isNumber || !/^-?\d+(?:\.\d+)?$/.test(text))
    return text
  // 字符串分组，避免 Long/Decimal 转 Number 后丢失精度。
  const [integer, fraction] = text.split('.')
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return fraction === undefined ? grouped : `${grouped}.${fraction}`
}
