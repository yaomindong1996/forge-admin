/**
 * 将业务表单方案合并进 AiCrudPage editSchema：
 * - SYSTEM 字段：按启用过滤、覆盖 label/required、按 sortOrder 排序
 * - CUSTOM 字段：追加到 schema 末尾（field = extValues.xxx）
 */

function parseOptions(optionsJson) {
  if (!optionsJson)
    return []
  try {
    const parsed = typeof optionsJson === 'string' ? JSON.parse(optionsJson) : optionsJson
    return Array.isArray(parsed) ? parsed : []
  }
  catch {
    return []
  }
}

function customFieldToSchemaItem(item) {
  const label = item.displayLabel || item.fieldLabel || item.fieldCode
  const field = `extValues.${item.fieldCode}`
  const base = {
    field,
    label,
    type: item.componentType || 'input',
    rules: Number(item.required) === 1
      ? [{ required: true, message: `请填写${label}`, trigger: ['blur', 'change'] }]
      : undefined,
  }
  if (item.dictType) {
    base.props = { ...(base.props || {}), dictType: item.dictType }
  }
  const options = parseOptions(item.optionsJson)
  if (options.length) {
    base.props = { ...(base.props || {}), options }
  }
  return base
}

function flattenSchema(schema = []) {
  const out = []
  for (const item of schema || []) {
    if (!item)
      continue
    if (item.type === 'card' || item.type === 'section' || item.children) {
      out.push({
        ...item,
        children: flattenSchema(item.children || item.fields || []),
      })
      continue
    }
    out.push(item)
  }
  return out
}

function mapLeaf(item, fieldMap, hasScheme) {
  if (!item?.field)
    return item
  const meta = fieldMap[item.field]
  if (hasScheme && !meta) {
    // 方案已加载且目录未包含该系统字段 → 隐藏
    return null
  }
  if (!meta)
    return item
  const next = { ...item }
  const label = meta.displayLabel || meta.fieldLabel
  if (label)
    next.label = label
  if (Number(meta.required) === 1) {
    const message = `请填写${label || item.label || item.field}`
    next.rules = [{ required: true, message, trigger: ['blur', 'change'] }, ...(item.rules || []).filter(r => !r.required)]
  }
  else if (Array.isArray(item.rules)) {
    next.rules = item.rules.filter(r => !r.required)
  }
  next.__bizFormSort = meta.sortOrder ?? 9999
  return next
}

function applyToTree(schema, fieldMap, hasScheme) {
  const result = []
  for (const item of schema || []) {
    if (!item)
      continue
    if (item.children || item.fields || item.type === 'card' || item.type === 'section') {
      const children = applyToTree(item.children || item.fields || [], fieldMap, hasScheme)
      if (!children.length && hasScheme)
        continue
      result.push({
        ...item,
        children,
        fields: undefined,
      })
      continue
    }
    const mapped = mapLeaf(item, fieldMap, hasScheme)
    if (mapped)
      result.push(mapped)
  }
  // 同级按 scheme 排序（无 sort 的保持相对顺序）
  return result.slice().sort((a, b) => {
    const ao = a.__bizFormSort
    const bo = b.__bizFormSort
    if (ao == null && bo == null)
      return 0
    if (ao == null)
      return 1
    if (bo == null)
      return -1
    return ao - bo
  }).map(({ __bizFormSort, ...rest }) => rest)
}

/**
 * @param {Array} editSchema
 * @param {object|null} scheme { fields: [] }
 */
export function applyBizFormSchemeToEditSchema(editSchema = [], scheme = null) {
  const fields = scheme?.fields || []
  const hasScheme = Boolean(scheme)
  const fieldMap = Object.create(null)
  for (const item of fields) {
    if (item?.fieldCode && String(item.fieldKind || '').toUpperCase() !== 'CUSTOM')
      fieldMap[item.fieldCode] = item
  }
  const base = applyToTree(flattenSchema(editSchema), fieldMap, hasScheme)
  const customs = fields
    .filter(item => String(item.fieldKind || '').toUpperCase() === 'CUSTOM' && Number(item.enabled ?? 1) === 1)
    .slice()
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map(customFieldToSchemaItem)
  if (!customs.length)
    return base
  return [
    ...base,
    {
      type: 'card',
      title: '扩展信息',
      key: '__biz_form_extra__',
      children: customs,
    },
  ]
}
