const ALLOWED_OPTION_API_PREFIXES = ['/system/', '/ai/', '/api/']
const MULTIPLE_TYPES = new Set(['userSelect', 'orgTreeSelect', 'objectReference', 'recordSelector', 'treeSelect', 'cascader', 'customSelect'])

export function normalizeMobileSelectorConfig(field = {}) {
  const fragments = [
    field.optionSource,
    field.recordSelector,
    field.selector,
    field.selectorConfig,
    field.recordSelectorConfig,
    field.referenceConfig,
    field.objectConfig,
    field.basicProps?.optionSource,
    field.basicProps?.recordSelector,
    field.basicProps?.selector,
    field.basicProps?.selectorConfig,
    field.basicProps?.recordSelectorConfig,
    field.props?.optionSource,
    field.props?.recordSelector,
    field.props?.selector,
    field.props?.selectorConfig,
    field.props?.recordSelectorConfig,
    field.props?.referenceConfig,
    field.props?.objectConfig,
  ]
  const merged = fragments.reduce((result, item) => ({ ...result, ...parseObject(item) }), {})
  const type = String(field.type || field.componentType || field.componentKey || '')
  const objectCode = firstText(
    merged.objectCode,
    merged.businessObjectCode,
    merged.targetObjectCode,
    merged.targetEntityCode,
    merged.candidateObjectCode,
    merged.referenceObjectCode,
    merged.refObjectCode,
    merged.sourceObjectCode,
    merged.targetCode,
    field.objectCode,
    field.businessObjectCode,
    field.targetObjectCode,
    field.props?.objectCode,
    field.props?.businessObjectCode,
    field.props?.targetObjectCode,
  )
  const querySource = parseObject(merged.querySource || field.querySource || field.props?.querySource)
  const linkageContext = parseObject(field.props?.linkageContext)
  const linkageParams = !isEmpty(linkageContext.sourceValue)
    ? { [firstText(linkageContext.paramName, linkageContext.sourceField)]: linkageContext.sourceValue }
    : {}
  return {
    ...merged,
    type,
    multiple: supportsMultiple(type) && Boolean(merged.multiple === true || field.multiple === true || field.props?.multiple === true || field.basicProps?.multiple === true),
    objectCode,
    businessObjectCode: firstText(merged.businessObjectCode, field.businessObjectCode, field.props?.businessObjectCode, objectCode),
    targetObjectCode: firstText(merged.targetObjectCode, field.targetObjectCode, field.props?.targetObjectCode, objectCode),
    suiteCode: firstText(merged.suiteCode, field.suiteCode, field.props?.suiteCode),
    labelField: firstText(merged.labelField, merged.labelName, merged.labelSourceField, field.labelField, field.props?.labelField, defaultLabelField(type)),
    valueField: firstText(merged.valueField, merged.valueName, field.valueField, field.props?.valueField, defaultValueField(type)),
    childrenField: firstText(merged.childrenField, field.childrenField, field.props?.childrenField, 'children'),
    labelValueField: firstText(merged.labelValueField, field.labelValueField, field.props?.labelValueField, field.field ? `${field.field}Name` : ''),
    targetLabelField: firstText(merged.targetLabelField, merged.labelTargetField, field.props?.targetLabelField),
    api: firstText(merged.api, field.api, field.props?.api),
    method: firstText(merged.method, field.method, field.props?.method),
    params: { ...parseObject(merged.params), ...parseObject(field.params), ...parseObject(field.props?.params), ...linkageParams },
    searchParams: { ...parseObject(merged.searchParams), ...parseObject(field.searchParams), ...parseObject(field.props?.searchParams) },
    displayFields: firstArray(merged.displayFields, field.displayFields, field.props?.displayFields),
    keywordFields: firstArray(merged.keywordFields, field.keywordFields, field.props?.keywordFields),
    fieldMappings: merged.fieldMappings || merged.mappings || field.fieldMappings || field.props?.fieldMappings || [],
    querySourceType: firstText(querySource.sourceType, merged.querySourceType, String(merged.type || '').toUpperCase() === 'QUERY_SOURCE' ? merged.sourceType : '', field.querySourceType, field.props?.querySourceType),
    querySourceKey: firstText(querySource.sourceKey, merged.querySourceKey, String(merged.type || '').toUpperCase() === 'QUERY_SOURCE' ? merged.sourceKey : '', field.querySourceKey, field.props?.querySourceKey),
    keywordParam: firstText(merged.keywordParam, field.keywordParam, field.props?.keywordParam, 'keyword'),
    rootCode: firstText(merged.rootCode, field.rootCode, field.props?.rootCode),
    dataRight: merged.dataRight ?? field.dataRight ?? field.props?.dataRight,
  }
}

export function resolveMobileSelectorKind(field = {}, options = []) {
  if (Array.isArray(options) && options.length) return 'static'
  const config = normalizeMobileSelectorConfig(field)
  if (config.type === 'userSelect') return 'user'
  if (config.type === 'orgTreeSelect') return 'org'
  if (config.type === 'regionTreeSelect') return 'region'
  if (['objectReference', 'recordSelector'].includes(config.type) && config.objectCode) return 'record'
  if (config.querySourceType && config.querySourceKey) return 'query-source'
  if (config.api) return parseRegisteredOptionApi(config.api, config.method) ? 'custom-api' : 'blocked-api'
  return ['treeSelect', 'cascader'].includes(config.type) ? 'tree' : 'static'
}

export function parseRegisteredOptionApi(api = '', method = '') {
  const raw = String(api || '').trim()
  const matched = raw.match(/^([a-z]+)@(.+)$/i)
  const resolvedMethod = String(matched?.[1] || method || 'get').trim().toLowerCase()
  const url = String(matched?.[2] || raw).trim()
  if (!['get', 'post'].includes(resolvedMethod)) return null
  if (!url.startsWith('/') || url.startsWith('//') || url.includes('://')) return null
  if (!ALLOWED_OPTION_API_PREFIXES.some(prefix => url.startsWith(prefix))) return null
  return { method: resolvedMethod, url }
}

export function resolveMobileSelectorParams(params = {}, formData = {}, context = {}) {
  return Object.entries(params || {}).reduce((result, [key, value]) => {
    const resolved = resolveParamValue(value, formData, context)
    if (!isEmpty(resolved)) result[key] = resolved
    return result
  }, {})
}

export function normalizeMobileSelectorOptions(source, config = {}, options = {}) {
  const rows = extractRows(source)
  return rows.map((row, index) => normalizeOption(row, config, options, index)).filter(Boolean)
}

export function flattenMobileSelectorOptions(options = [], level = 0, result = []) {
  for (const option of options || []) {
    if (!option) continue
    result.push({ ...option, level })
    if (Array.isArray(option.children) && option.children.length)
      flattenMobileSelectorOptions(option.children, level + 1, result)
  }
  return result
}

export function parseMobileSelectionValues(value) {
  if (value === null || value === undefined || value === '') return []
  const values = Array.isArray(value) ? value : String(value).split(',')
  return values.map(item => String(item ?? '').trim()).filter(Boolean)
}

export function serializeMobileSelectionValues(values = [], multiple = false) {
  const normalized = parseMobileSelectionValues(values)
  return multiple ? normalized.join(',') : (normalized[0] ?? '')
}

export function buildMobileSelectionPatch(field = {}, records = [], labels = []) {
  const config = normalizeMobileSelectorConfig(field)
  const selected = Array.isArray(records) ? records.filter(Boolean) : []
  const patch = {}
  const serializedLabel = (Array.isArray(labels) ? labels : [labels]).map(String).filter(Boolean).join(',')
  if (config.labelValueField) patch[config.labelValueField] = serializedLabel
  if (config.targetLabelField) patch[config.targetLabelField] = serializedLabel
  if (!config.multiple && selected[0]) {
    const raw = selected[0]._raw || selected[0]
    Object.entries(normalizeMappings(config.fieldMappings)).forEach(([sourceField, targetField]) => {
      if (!sourceField || !targetField) return
      patch[targetField] = readPath(raw, sourceField)
    })
  }
  return patch
}

export function resolveMobileSelectionLabels(value, options = [], field = {}, formData = {}) {
  const config = normalizeMobileSelectorConfig(field)
  const flat = flattenMobileSelectorOptions(options)
  const values = parseMobileSelectionValues(value)
  const found = values.map(item => flat.find(option => String(option.value) === item)?.label).filter(Boolean)
  if (found.length === values.length && found.length) return found
  const stored = formData?.[config.labelValueField] ?? formData?.[config.targetLabelField] ?? field.labelValue ?? field.props?.labelValue
  const storedLabels = parseMobileSelectionValues(stored)
  return storedLabels.length ? storedLabels : found
}

function normalizeOption(row, config, options, index) {
  if (row === null || row === undefined) return null
  if (typeof row !== 'object') return { label: String(row), value: String(row), _raw: row }
  const value = readPath(row, config.valueField) ?? row.value ?? row.id ?? row.key ?? row.code
  if (value === null || value === undefined || value === '') return null
  const fallbackLabel = options.kind === 'user'
    ? row.realName || row.nickname || row.username
    : options.kind === 'org' ? row.orgName || row.name
      : options.kind === 'region' ? row.name
        : undefined
  const displayField = String(config.displayFields?.[0] || '').split(':')[0].trim()
  const label = readPath(row, config.labelField) ?? (displayField ? readPath(row, displayField) : undefined) ?? fallbackLabel ?? row.label ?? row.name ?? value
  const children = Array.isArray(row[config.childrenField])
    ? row[config.childrenField].map((child, childIndex) => normalizeOption(child, config, options, childIndex)).filter(Boolean)
    : []
  return {
    label: String(label),
    value: String(value),
    disabled: row.disabled === true || row.selectable === false,
    children,
    _raw: { ...row },
    _index: index,
  }
}

function extractRows(source) {
  const root = source?.data !== undefined ? source.data : source
  if (Array.isArray(root)) return root
  if (!root || typeof root !== 'object') return []
  for (const candidate of [root.records, root.rows, root.list, root.data, root.items]) {
    if (Array.isArray(candidate)) return candidate
  }
  return []
}

function resolveParamValue(value, formData, context) {
  if (Array.isArray(value)) return value.map(item => resolveParamValue(item, formData, context)).filter(item => !isEmpty(item))
  if (value && typeof value === 'object') return resolveMobileSelectorParams(value, formData, context)
  if (typeof value !== 'string') return value
  const text = value.trim()
  const templateMatch = text.match(/^\$\{(.+)\}$/)
  const formMatch = text.match(/^\$form\.(.+)$/)
  if (!templateMatch && !formMatch) return value
  if (formMatch) return readPath(formData, formMatch[1])
  const source = { ...context, form: formData, formData, record: context.record || formData, row: context.row || formData }
  return readPath(source, templateMatch[1])
}

function normalizeMappings(value) {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value
  return (Array.isArray(value) ? value : []).reduce((result, item) => {
    const source = String(item?.sourceField || item?.source || '').trim()
    const target = String(item?.targetField || item?.target || '').trim()
    if (source && target) result[source] = target
    return result
  }, {})
}

function supportsMultiple(type) {
  return MULTIPLE_TYPES.has(String(type || ''))
}

function defaultLabelField(type) {
  if (type === 'userSelect') return 'realName'
  if (type === 'orgTreeSelect') return 'orgName'
  if (type === 'regionTreeSelect') return 'name'
  return 'label'
}

function defaultValueField(type) {
  return type === 'regionTreeSelect' ? 'code' : 'id'
}

function parseObject(value) {
  if (value && typeof value === 'object' && !Array.isArray(value)) return value
  if (typeof value !== 'string' || !value.trim()) return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch { return {} }
}

function firstText(...values) {
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (text) return text
  }
  return ''
}

function firstArray(...values) {
  for (const value of values) {
    if (Array.isArray(value)) return value
    if (typeof value === 'string' && value.trim()) return value.split(/[\n,]/).map(item => item.trim()).filter(Boolean)
  }
  return []
}

function readPath(source, path = '') {
  return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], source)
}

function isEmpty(value) {
  return value === null || value === undefined || value === '' || (Array.isArray(value) && !value.length)
}
