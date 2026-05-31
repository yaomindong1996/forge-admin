export const LINKAGE_SCHEMA_VERSION = 'linkage-schema-v1'
export const LINKAGE_SCHEMA_KEY = 'linkageSchema'

export const LINKAGE_TYPES = [
  'parentDictCode',
  'linkedDict',
  'remoteParam',
  'objectReference',
  'orgScope',
  'visibility',
  'disabled',
]

export function createDefaultLinkageSchema(options = {}) {
  return normalizeLinkageSchema({
    schemaVersion: LINKAGE_SCHEMA_VERSION,
    rules: Array.isArray(options.rules) ? options.rules : [],
  })
}

export function normalizeLinkageSchema(source = {}) {
  const schema = isPlainObject(source) ? cloneValue(source) : {}
  return {
    schemaVersion: schema.schemaVersion || LINKAGE_SCHEMA_VERSION,
    rules: (Array.isArray(schema.rules) ? schema.rules : [])
      .map((rule, index) => normalizeLinkageRule(rule, index))
      .filter(Boolean),
    settings: isPlainObject(schema.settings) ? schema.settings : {},
  }
}

export function createDictParentRule(options = {}) {
  return normalizeLinkageRule({
    ruleId: options.ruleId,
    type: 'parentDictCode',
    sourceField: options.sourceField,
    targetField: options.targetField,
    dataSourceType: 'dict',
    matchMode: 'parentDictCode',
    dictConfig: {
      sourceDictType: options.sourceDictType || '',
      targetDictType: options.targetDictType || '',
      parentValueSource: 'sourceValue',
    },
    emptyStrategy: options.emptyStrategy || 'empty',
    clearOnSourceChange: options.clearOnSourceChange !== false,
  })
}

export function createLinkedDictRule(options = {}) {
  return normalizeLinkageRule({
    ruleId: options.ruleId,
    type: 'linkedDict',
    sourceField: options.sourceField,
    targetField: options.targetField,
    dataSourceType: 'dict',
    matchMode: 'linkedDict',
    dictConfig: {
      sourceDictType: options.sourceDictType || '',
      targetDictType: options.targetDictType || '',
      linkedDictTypeField: 'linked_dict_type',
      linkedDictValueField: 'linked_dict_value',
    },
    emptyStrategy: options.emptyStrategy || 'empty',
    clearOnSourceChange: options.clearOnSourceChange !== false,
  })
}

export function createRemoteParamRule(options = {}) {
  return normalizeLinkageRule({
    ruleId: options.ruleId,
    type: options.type || 'remoteParam',
    sourceField: options.sourceField,
    targetField: options.targetField,
    dataSourceType: options.dataSourceType || 'remote',
    matchMode: options.matchMode || 'eq',
    remoteConfig: {
      url: options.url || '',
      method: options.method || 'GET',
      paramName: options.paramName || options.sourceField || '',
      valuePath: options.valuePath || 'data',
      labelField: options.labelField || 'label',
      valueField: options.valueField || 'value',
    },
    emptyStrategy: options.emptyStrategy || 'disabled',
    clearOnSourceChange: options.clearOnSourceChange !== false,
  })
}

export function createLinkageSchemaFromFields(fields = [], currentSchema = {}) {
  const schema = normalizeLinkageSchema(currentSchema)
  const fieldMap = createFieldMap(fields)
  const rules = new Map(schema.rules.map(rule => [rule.ruleId, rule]))

  ;(Array.isArray(fields) ? fields : []).forEach((field, index) => {
    const fieldCode = resolveFieldCode(field)
    const cascade = normalizeFieldCascade(field?.basicProps?.cascade || field?.cascade || field?.props?.cascade)
    if (!fieldCode || !cascade.enabled || !cascade.sourceField)
      return
    const matchedRule = findRuleByTarget(schema.rules, cascade.sourceField, fieldCode)
    const ruleId = cascade.ruleId || matchedRule?.ruleId || buildRuleId(cascade.sourceField, fieldCode, index)
    rules.set(ruleId, normalizeRuleFromCascade({
      ...(matchedRule || {}),
      ruleId,
      sourceField: cascade.sourceField,
      targetField: fieldCode,
      type: cascade.type || cascade.mode || cascade.matchMode,
      cascade,
      sourceFieldConfig: fieldMap.get(cascade.sourceField),
      targetFieldConfig: field,
    }))
  })

  return normalizeLinkageSchema({
    ...schema,
    rules: Array.from(rules.values()),
  })
}

export function applyLinkageSchemaToFields(fields = [], source = {}) {
  const schema = normalizeLinkageSchema(source)
  const rulesByTarget = new Map(schema.rules
    .filter(rule => rule.enabled !== false && rule.sourceField && rule.targetField)
    .map(rule => [rule.targetField, rule]))

  return (Array.isArray(fields) ? fields : []).map((field) => {
    const fieldCode = resolveFieldCode(field)
    const rule = rulesByTarget.get(fieldCode)
    const basicProps = { ...(field?.basicProps || {}) }

    if (!rule) {
      if (basicProps.cascade?.managedBy === LINKAGE_SCHEMA_KEY)
        delete basicProps.cascade
      return {
        ...field,
        basicProps,
      }
    }

    return {
      ...field,
      dictType: field.dictType || rule.dictConfig?.targetDictType || '',
      referenceObjectCode: field.referenceObjectCode || rule.objectConfig?.targetObjectCode || '',
      referenceDisplayField: field.referenceDisplayField || rule.objectConfig?.displayField || '',
      basicProps: {
        ...basicProps,
        cascade: buildCascadeFromRule(rule),
      },
    }
  })
}

export function repairLinkageSchema(source = {}, fields = []) {
  const schema = normalizeLinkageSchema(source)
  const fieldSet = new Set((Array.isArray(fields) ? fields : [])
    .map(resolveFieldCode)
    .filter(Boolean))
  if (!fieldSet.size)
    return schema
  return normalizeLinkageSchema({
    ...schema,
    rules: schema.rules.filter(rule => fieldSet.has(rule.sourceField) && fieldSet.has(rule.targetField)),
  })
}

export function validateLinkageSchema(source = {}, fieldCodes = []) {
  const schema = normalizeLinkageSchema(source)
  const fieldSet = new Set(fieldCodes.filter(Boolean))
  const errors = []
  const ids = new Set()

  schema.rules.forEach((rule, index) => {
    const path = `rules[${index}]`
    if (!rule.ruleId)
      errors.push({ path: `${path}.ruleId`, message: '规则 ID 不能为空' })
    if (rule.ruleId && ids.has(rule.ruleId))
      errors.push({ path: `${path}.ruleId`, message: `规则 ID 重复：${rule.ruleId}` })
    if (rule.ruleId)
      ids.add(rule.ruleId)
    if (!rule.sourceField)
      errors.push({ path: `${path}.sourceField`, message: '上级字段不能为空' })
    if (!rule.targetField)
      errors.push({ path: `${path}.targetField`, message: '目标字段不能为空' })
    if (fieldSet.size && rule.sourceField && !fieldSet.has(rule.sourceField))
      errors.push({ path: `${path}.sourceField`, message: `上级字段不存在：${rule.sourceField}` })
    if (fieldSet.size && rule.targetField && !fieldSet.has(rule.targetField))
      errors.push({ path: `${path}.targetField`, message: `目标字段不存在：${rule.targetField}` })
    if (rule.dataSourceType === 'dict' && !rule.dictConfig?.targetDictType)
      errors.push({ path: `${path}.dictConfig.targetDictType`, message: '目标字典类型不能为空' })
    if (rule.type === 'linkedDict' && !rule.dictConfig?.linkedDictType && !rule.dictConfig?.sourceDictType)
      errors.push({ path: `${path}.dictConfig.linkedDictType`, message: '关联字典类型不能为空' })
    if (['remoteParam', 'orgScope', 'objectReference'].includes(rule.type) && !rule.remoteConfig?.paramName)
      errors.push({ path: `${path}.remoteConfig.paramName`, message: '请求参数名不能为空' })
    if (rule.type === 'remoteParam' && !rule.remoteConfig?.url)
      errors.push({ path: `${path}.remoteConfig.url`, message: '远程接口不能为空' })
    if (rule.type === 'objectReference' && !rule.objectConfig?.targetObjectCode)
      errors.push({ path: `${path}.objectConfig.targetObjectCode`, message: '目标对象不能为空' })
  })

  return {
    valid: errors.length === 0,
    errors,
    schema,
  }
}

function normalizeRuleFromCascade(options = {}) {
  const cascade = options.cascade || {}
  const targetField = options.targetFieldConfig || {}
  const sourceField = options.sourceFieldConfig || {}
  const type = normalizeRuleType(options.type)
  const dataSourceType = resolveDataSourceType(type)
  return normalizeLinkageRule({
    ...options,
    type,
    dataSourceType,
    matchMode: cascade.mode || cascade.matchMode || type,
    dictConfig: {
      ...(options.dictConfig || {}),
      sourceDictType: cascade.sourceDictType || sourceField.dictType || options.dictConfig?.sourceDictType || '',
      targetDictType: cascade.targetDictType || targetField.dictType || options.dictConfig?.targetDictType || '',
      linkedDictType: cascade.linkedDictType || options.dictConfig?.linkedDictType || cascade.sourceDictType || sourceField.dictType || '',
      parentValueSource: options.dictConfig?.parentValueSource || 'sourceValue',
    },
    remoteConfig: {
      ...(options.remoteConfig || {}),
      url: cascade.url || options.remoteConfig?.url || targetField.basicProps?.optionSource?.api || '',
      method: cascade.method || options.remoteConfig?.method || 'GET',
      paramName: cascade.paramName || options.remoteConfig?.paramName || options.sourceField || '',
      valuePath: cascade.valuePath || options.remoteConfig?.valuePath || 'data',
      labelField: cascade.labelField || options.remoteConfig?.labelField || 'label',
      valueField: cascade.valueField || options.remoteConfig?.valueField || 'value',
    },
    objectConfig: {
      ...(options.objectConfig || {}),
      targetObjectCode: cascade.targetObjectCode || targetField.referenceObjectCode || targetField.basicProps?.referenceObjectCode || options.objectConfig?.targetObjectCode || '',
      displayField: cascade.displayField || targetField.referenceDisplayField || targetField.basicProps?.referenceDisplayField || options.objectConfig?.displayField || '',
    },
    orgConfig: {
      ...(options.orgConfig || {}),
      paramName: cascade.paramName || options.orgConfig?.paramName || options.sourceField || '',
    },
    emptyStrategy: cascade.emptyStrategy || options.emptyStrategy || 'empty',
    clearOnSourceChange: cascade.clearOnSourceChange ?? cascade.clearOnParentChange ?? options.clearOnSourceChange ?? true,
  })
}

function buildCascadeFromRule(rule = {}) {
  const mode = rule.dataSourceType === 'dict' ? (rule.matchMode || rule.type) : 'remoteParam'
  return {
    enabled: rule.enabled !== false,
    managedBy: LINKAGE_SCHEMA_KEY,
    ruleId: rule.ruleId,
    sourceField: rule.sourceField,
    sourceDictType: rule.dictConfig?.sourceDictType || '',
    targetDictType: rule.dictConfig?.targetDictType || '',
    linkedDictType: rule.dictConfig?.linkedDictType || rule.dictConfig?.sourceDictType || '',
    mode,
    matchMode: mode,
    paramName: rule.remoteConfig?.paramName || rule.orgConfig?.paramName || rule.sourceField || '',
    emptyStrategy: rule.emptyStrategy || 'empty',
    clearOnParentChange: rule.clearOnSourceChange !== false,
    clearOnSourceChange: rule.clearOnSourceChange !== false,
    ...(rule.remoteConfig?.url ? { url: rule.remoteConfig.url } : {}),
    ...(rule.objectConfig?.targetObjectCode ? { targetObjectCode: rule.objectConfig.targetObjectCode } : {}),
    ...(rule.objectConfig?.displayField ? { displayField: rule.objectConfig.displayField } : {}),
  }
}

function normalizeFieldCascade(source = {}) {
  const cascade = isPlainObject(source) ? source : {}
  const sourceField = cascade.sourceField || ''
  return {
    ...cascade,
    enabled: cascade.enabled !== false && Boolean(sourceField),
    sourceField,
    mode: normalizeRuleType(cascade.mode || cascade.matchMode || cascade.type || 'linkedDict'),
    emptyStrategy: ['empty', 'all', 'disabled'].includes(cascade.emptyStrategy) ? cascade.emptyStrategy : 'empty',
    clearOnSourceChange: cascade.clearOnSourceChange ?? cascade.clearOnParentChange ?? true,
  }
}

function findRuleByTarget(rules = [], sourceField, targetField) {
  return rules.find(rule => rule.sourceField === sourceField && rule.targetField === targetField)
}

function buildRuleId(sourceField, targetField, index = 0) {
  return `linkage_${sourceField || 'source'}_${targetField || index + 1}`
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
}

function createFieldMap(fields = []) {
  return new Map((Array.isArray(fields) ? fields : [])
    .map(field => [resolveFieldCode(field), field])
    .filter(([fieldCode]) => fieldCode))
}

function resolveFieldCode(field = {}) {
  return field?.fieldCode || field?.field || ''
}

function normalizeRuleType(type) {
  return LINKAGE_TYPES.includes(type) ? type : 'remoteParam'
}

function normalizeLinkageRule(rule = {}, index = 0) {
  if (!isPlainObject(rule))
    return null
  const type = normalizeRuleType(rule.type)
  return {
    ruleId: rule.ruleId || `linkage_${index + 1}`,
    type,
    sourceField: rule.sourceField || '',
    targetField: rule.targetField || '',
    dataSourceType: rule.dataSourceType || resolveDataSourceType(type),
    matchMode: rule.matchMode || type,
    dictConfig: normalizeObject(rule.dictConfig),
    remoteConfig: normalizeObject(rule.remoteConfig),
    objectConfig: normalizeObject(rule.objectConfig),
    orgConfig: normalizeObject(rule.orgConfig),
    condition: normalizeObject(rule.condition),
    emptyStrategy: ['empty', 'all', 'disabled'].includes(rule.emptyStrategy) ? rule.emptyStrategy : 'empty',
    clearOnSourceChange: rule.clearOnSourceChange !== false,
    enabled: rule.enabled !== false,
  }
}

function resolveDataSourceType(type) {
  if (['parentDictCode', 'linkedDict'].includes(type))
    return 'dict'
  if (type === 'orgScope')
    return 'org'
  if (type === 'objectReference')
    return 'object'
  return 'remote'
}

function normalizeObject(value) {
  return isPlainObject(value) ? value : {}
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}
