export const CRUD_HOOK_RULE_ACTIONS = [
  { label: '设置固定值', value: 'set' },
  { label: '从字段复制', value: 'copyFrom' },
  { label: '尾部拼接', value: 'append' },
  { label: '清空字段', value: 'clear' },
]

export const CRUD_HOOK_RULE_TARGETS = [
  { label: '加载列表前', value: 'beforeLoadList', description: '修改列表请求参数' },
  { label: '搜索前', value: 'beforeSearch', description: '修改搜索参数' },
  { label: '打开表单前', value: 'beforeRenderForm', description: '预填新增/编辑表单' },
  { label: '打开详情前', value: 'beforeRenderDetail', description: '调整详情展示数据' },
  { label: '提交前', value: 'beforeSubmit', description: '修改表单提交数据' },
  { label: '构建提交数据后', value: 'afterBuildSubmitData', description: '修改主从表合并后的提交数据' },
]

export function createCrudHookRule(hookName = 'beforeSubmit') {
  return {
    id: `hook_rule_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
    hookName,
    action: 'set',
    field: '',
    value: '',
    sourceField: '',
  }
}

export function normalizeCrudHookRules(source = {}, legacyBeforeSubmitRules = []) {
  const result = {}
  CRUD_HOOK_RULE_TARGETS.forEach((target) => {
    const list = Array.isArray(source?.[target.value]) ? source[target.value] : []
    result[target.value] = normalizeRuleList(list, target.value)
  })
  if (legacyBeforeSubmitRules?.length) {
    result.beforeSubmit = [
      ...(result.beforeSubmit || []),
      ...normalizeRuleList(legacyBeforeSubmitRules, 'beforeSubmit'),
    ]
  }
  return result
}

export function normalizeRuleList(list = [], hookName = 'beforeSubmit') {
  return (Array.isArray(list) ? list : [])
    .map(rule => ({
      id: rule?.id || `hook_rule_${Math.random().toString(36).slice(2, 8)}`,
      hookName: rule?.hookName || hookName,
      action: ['set', 'copyFrom', 'append', 'clear'].includes(rule?.action) ? rule.action : 'set',
      field: String(rule?.field || '').trim(),
      sourceField: String(rule?.sourceField || '').trim(),
      value: rule?.value ?? '',
    }))
    .filter(rule => rule.field || rule.sourceField || rule.value !== '')
}

export function hasCrudHookRules(rules = {}) {
  return CRUD_HOOK_RULE_TARGETS.some(target => normalizeRuleList(rules?.[target.value], target.value).some(rule => rule.field))
}

export function applyCrudHookRules(payload, rules = []) {
  const normalizedRules = normalizeRuleList(rules).filter(rule => rule.field)
  if (!normalizedRules.length)
    return payload
  const next = Array.isArray(payload)
    ? [...payload]
    : { ...(payload || {}) }
  normalizedRules.forEach((rule) => {
    if (rule.action === 'clear') {
      next[rule.field] = undefined
      return
    }
    if (rule.action === 'copyFrom') {
      next[rule.field] = rule.sourceField ? next[rule.sourceField] : undefined
      return
    }
    if (rule.action === 'append') {
      next[rule.field] = `${next[rule.field] ?? ''}${rule.value ?? ''}`
      return
    }
    next[rule.field] = rule.value
  })
  return next
}
