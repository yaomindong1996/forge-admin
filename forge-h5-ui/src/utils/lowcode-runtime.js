const DANGEROUS_KEYS = new Set([
  'url', 'uri', 'headers', 'header', 'authorization', 'credential', 'credentials',
  'secret', 'token', 'sql', 'script', 'handler',
])

export function parseJson(value, fallback = {}) {
  if (value && typeof value === 'object')
    return value
  if (typeof value !== 'string' || !value.trim())
    return fallback
  try {
    return JSON.parse(value)
  }
  catch {
    return fallback
  }
}

export function normalizeRuntimePageSchema(value = {}) {
  const source = parseJson(value, {})
  if (!source || typeof source !== 'object' || Array.isArray(source))
    return { zones: [] }
  const zones = Array.isArray(source.zones)
    ? source.zones.filter(zone => zone && typeof zone === 'object').map((zone, index) => normalizeRuntimeZone(zone, index))
    : []
  return { ...source, zones }
}

export function resolveRuntimePageZones(config = {}, mode = '') {
  const pageSchema = normalizeRuntimePageSchema(config.pageSchema)
  return pageSchema.zones.filter(zone => isRuntimeZoneVisible(zone, mode))
}

export function resolveRuntimeFormZone(config = {}, mode = 'edit') {
  const zones = resolveRuntimePageZones(config, mode).filter(zone => zone.zoneType === 'form')
  const preferred = zones.find(zone => matchesLegacyFormZone(zone, mode)) || zones[0]
  if (!preferred)
    return null
  const schema = resolveRuntimeZoneFormDesignerSchema(preferred)
  return schema ? { zone: preferred, schema } : null
}

export function resolveRuntimeZoneFormDesignerSchema(zone = {}) {
  return extractRuntimeFormSchema(zone)
}

export function resolveRuntimeFormDesignerSchema(config = {}, mode = 'edit') {
  const zone = resolveRuntimeFormZone(config, mode)
  if (zone?.schema)
    return zone.schema
  return parseJson(config.options?.formDesignerSchema || config.options?.formDesignerSchemaJson, {})
}

export function hasComposedRuntimePageSchema(config = {}) {
  const pageSchema = normalizeRuntimePageSchema(config.pageSchema)
  return pageSchema.zones.some(zone => Boolean(zone.zoneTypeExplicit || zone.zoneIdExplicit))
}

export function parseRuntimeConfig(data = {}) {
  const candidate = data?.data && typeof data.data === 'object' ? data.data : data
  const source = candidate && typeof candidate === 'object' && !Array.isArray(candidate) ? candidate : {}
  const options = parseJson(source.options, {})
  const pageSchemaCandidate = hasRuntimeZones(options.pageSchema)
    ? options.pageSchema
    : source.pageSchema
  const pageSchema = normalizeRuntimePageSchema(pageSchemaCandidate)
  const fallbackFormSchema = parseJson(options.formDesignerSchema || options.formDesignerSchemaJson, {})
  const formZone = resolveRuntimeFormZone({ pageSchema, options: { formDesignerSchema: fallbackFormSchema } }, 'edit')
  const normalizedOptions = {
    ...options,
    formDesignerSchema: formZone?.schema || fallbackFormSchema,
    flowInteraction: normalizeRuntimeFlowInteraction(
      options.flowInteraction || options.inAppBuilder?.flowInteraction || source.flowInteraction,
    ),
  }
  const masterDetailConfig = formZone?.zone?.props?.masterDetailConfig
    || formZone?.zone?.masterDetailConfig
  if (!normalizedOptions.masterDetailConfig && masterDetailConfig)
    normalizedOptions.masterDetailConfig = masterDetailConfig
  return {
    ...source,
    searchSchema: parseJson(source.searchSchema, []),
    columnsSchema: parseJson(source.columnsSchema, []),
    editSchema: parseJson(source.editSchema, []),
    apiConfig: parseJson(source.apiConfig, {}),
    options: normalizedOptions,
    modelSchema: parseJson(source.modelSchema, {}),
    pageSchema,
  }
}

export function normalizeMainFields(config = {}, formDesignerSchema) {
  const stored = Array.isArray(config.editSchema) ? config.editSchema : []
  const schema = formDesignerSchema || config.options?.formDesignerSchema
  const designer = Array.isArray(schema?.components)
    ? schema.components
    : []
  const designerFields = designer.map(normalizeDesignerField).filter(Boolean)
  const byField = new Map(designerFields.map(field => [field.field, field]))
  return stored.map(field => mergeField(field, byField.get(field?.field))).filter(isRenderableField)
    .concat(designerFields.filter(field => !stored.some(item => item?.field === field.field)))
}

function hasRuntimeZones(value) {
  const pageSchema = parseJson(value, {})
  return Array.isArray(pageSchema?.zones) && pageSchema.zones.length > 0
}

function normalizeRuntimeZone(zone = {}, index = 0) {
  const props = zone.props && typeof zone.props === 'object' && !Array.isArray(zone.props) ? zone.props : {}
  // Preserve explicitness across repeated normalization. Generated legacy ids
  // must not turn an old zoneKey-only page into the composed page protocol.
  const zoneTypeExplicit = typeof zone.zoneTypeExplicit === 'boolean'
    ? zone.zoneTypeExplicit
    : Boolean(zone.zoneType)
  const zoneIdExplicit = typeof zone.zoneIdExplicit === 'boolean'
    ? zone.zoneIdExplicit
    : Boolean(zone.zoneId)
  return {
    ...zone,
    props,
    zoneId: String(zone.zoneId || zone.zoneKey || `zone_${index}`),
    zoneType: resolveRuntimeZoneType(zone),
    zoneTypeExplicit,
    zoneIdExplicit,
  }
}

function resolveRuntimeZoneType(zone = {}) {
  const explicit = String(zone.zoneType || '').trim().toLowerCase()
  if (explicit)
    return explicit
  const key = String(zone.zoneKey || zone.componentKey || '').trim().toLowerCase()
  if (['form', 'edit', 'detail', 'edit-form', 'detail-panel', 'form-panel'].includes(key))
    return 'form'
  if (['actions', 'action', 'action-bar', 'toolbar', 'button-bar'].includes(key))
    return 'actions'
  if (['list', 'table', 'data-table', 'crud'].includes(key))
    return 'list'
  if (['chart', 'chart-panel'].includes(key))
    return 'chart'
  return key || 'unknown'
}

function isRuntimeZoneVisible(zone = {}, mode = '') {
  if (zone.enabled === false || zone.visible === false)
    return false
  const visibleModes = Array.isArray(zone.visibleInModes)
    ? zone.visibleInModes
    : Array.isArray(zone.props?.visibleInModes) ? zone.props.visibleInModes : []
  return !visibleModes.length || visibleModes.map(String).includes(String(mode))
}

function matchesLegacyFormZone(zone = {}, mode = '') {
  const key = String(zone.zoneKey || '').toLowerCase()
  if (key === 'detail')
    return String(mode).toLowerCase() === 'detail'
  if (key === 'edit')
    return ['create', 'edit'].includes(String(mode).toLowerCase())
  return true
}

function extractRuntimeFormSchema(zone = {}) {
  const candidate = zone.props?.formDesignerSchema
    || zone.formDesignerSchema
    || zone.props?.formSchema
    || zone.formSchema
  const parsed = parseJson(candidate, null)
  if (parsed && typeof parsed === 'object' && !Array.isArray(parsed))
    return parsed
  const inline = { ...zone.props }
  delete inline.masterDetailConfig
  delete inline.actions
  delete inline.list
  return ['components', 'pageSections', 'bottomBar', 'settings'].some(key => inline[key] !== undefined)
    ? inline
    : null
}

export function normalizeChildFields(child = {}) {
  return (Array.isArray(child.fields) ? child.fields : [])
    .map(normalizeField)
    .filter(isRenderableField)
}

export function normalizeChildrenConfig(config = {}) {
  const children = config.options?.masterDetailConfig?.children
  return (Array.isArray(children) ? children : [])
    .map((child, index) => ({
      ...child,
      key: String(child?.key || child?.relationKey || child?.modelCode || `children_${index}`),
      modelCode: String(child?.modelCode || child?.tableName || child?.key || `children_${index}`),
      relationKey: String(child?.relationKey || child?.key || child?.modelCode || `children_${index}`),
      fields: normalizeChildFields(child),
      rowActions: Array.isArray(child?.rowActions) ? child.rowActions : [],
      toolbarActions: Array.isArray(child?.toolbarActions) ? child.toolbarActions : [],
    }))
}

export function resolveVisiblePageSections(sections = [], mode = '', flowInteraction = {}, nodeKey = '') {
  const policy = resolveNodeSectionPolicy(flowInteraction, nodeKey)
  const visibleSectionIds = new Set(policy?.visibleSectionIds || [])
  return (Array.isArray(sections) ? sections : []).filter((section) => {
    if (!section || typeof section !== 'object')
      return false
    const visibleModes = Array.isArray(section.visibleInModes) ? section.visibleInModes.map(String) : []
    const modeVisible = !visibleModes.length || visibleModes.includes(String(mode))
    return modeVisible && (!visibleSectionIds.size || visibleSectionIds.has(String(section.sectionId || '')))
  })
}

export function isPageSectionReadonly(section = {}, flowInteraction = {}, nodeKey = '') {
  const policy = resolveNodeSectionPolicy(flowInteraction, nodeKey)
  return Boolean(policy?.readonlySectionIds?.map(String).includes(String(section.sectionId || '')))
}

export function normalizeRuntimeFlowInteraction(source = {}) {
  const flow = parseJson(source, {})
  if (!flow || typeof flow !== 'object' || Array.isArray(flow))
    return { approvalActions: [], timeline: { enabled: false }, nodePermissions: [], callbacks: {} }
  return {
    ...flow,
    approvalActions: (Array.isArray(flow.approvalActions) ? flow.approvalActions : [])
      .filter(action => action && typeof action === 'object' && action.enabled !== false)
      .map((action, index) => ({
        ...action,
        actionId: String(action.actionId || `flow_action_${index + 1}`),
        type: 'flow_action',
        operation: ['approve', 'reject', 'return', 'delegate'].includes(action.operation) ? action.operation : 'approve',
        label: String(action.label || flowOperationLabel(action.operation)),
        visibleInModes: Array.isArray(action.visibleInModes) ? action.visibleInModes : ['detail'],
      })),
    timeline: {
      ...(flow.timeline && typeof flow.timeline === 'object' ? flow.timeline : {}),
      enabled: flow.timeline?.enabled === true,
      title: String(flow.timeline?.title || '审批记录'),
    },
    nodePermissions: Array.isArray(flow.nodePermissions) ? flow.nodePermissions : [],
    callbacks: flow.callbacks && typeof flow.callbacks === 'object' ? flow.callbacks : {},
  }
}

export function mergeFlowActionsIntoBottomBar(bottomBar = {}, flowInteraction = {}) {
  const flow = normalizeRuntimeFlowInteraction(flowInteraction)
  const actions = Array.isArray(bottomBar?.actions) ? bottomBar.actions : []
  if (!flow.approvalActions.length)
    return bottomBar
  const actionIds = new Set(actions.map(action => String(action?.actionId || '')))
  return {
    ...(bottomBar || {}),
    actions: [...actions, ...flow.approvalActions.filter(action => !actionIds.has(action.actionId))],
  }
}

function resolveNodeSectionPolicy(flowInteraction = {}, nodeKey = '') {
  const key = String(nodeKey || '').trim()
  if (!key)
    return null
  return normalizeRuntimeFlowInteraction(flowInteraction).nodePermissions
    .find(item => String(item?.nodeKey || '') === key) || null
}

function flowOperationLabel(operation) {
  return { approve: '同意', reject: '拒绝', return: '退回', delegate: '委派' }[operation] || '同意'
}

export function resolvePageSectionFields(section = {}, fields = []) {
  const byField = new Map((Array.isArray(fields) ? fields : [])
    .filter(field => field?.field)
    .map(field => [String(field.field), field]))
  return (Array.isArray(section.fields) ? section.fields : [])
    .map((fieldCode) => {
      const field = byField.get(String(fieldCode))
      if (!field)
        return null
      const override = section.fieldOverrides?.[fieldCode]
      if (!override || typeof override !== 'object')
        return field
      return {
        ...field,
        type: override.componentKey === 'pillSelect' ? 'pillSelect' : field.type,
        props: { ...(field.props || {}), ...(override.props || {}) },
      }
    })
    .filter(Boolean)
}

export function resolvePageSectionChild(section = {}, children = []) {
  const relationKey = String(section.relationKey || '').trim()
  if (!relationKey)
    return null
  return (Array.isArray(children) ? children : [])
    .find(child => String(child?.relationKey || '') === relationKey) || null
}

export function resolveFieldLinkages(formSchema = {}, config = {}) {
  const governed = formSchema?.settings?.governance?.fieldLinkages
  if (Array.isArray(governed))
    return governed.filter(rule => rule && typeof rule === 'object')
  const legacy = parseJson(config?.options?.linkageSchema, {})
  return Array.isArray(legacy?.rules) ? legacy.rules.filter(rule => rule && typeof rule === 'object') : []
}

export function applyFieldLinkageChange(rules = [], sourceField = '', formData = {}) {
  const patch = {}
  ;(Array.isArray(rules) ? rules : []).forEach((rule) => {
    if (rule?.enabled === false || String(rule?.sourceField || '') !== String(sourceField || ''))
      return
    const targetField = String(rule?.targetField || '').trim()
    if (targetField && targetField !== sourceField && rule.clearOnSourceChange !== false)
      patch[targetField] = ''
  })
  Object.assign(formData, patch)
  return patch
}

export function resolveFieldLinkageContext(rules = [], targetField = '', formData = {}) {
  const rule = (Array.isArray(rules) ? rules : []).find(item => item?.enabled !== false
    && String(item?.targetField || '') === String(targetField || ''))
  if (!rule)
    return null
  return {
    ruleId: String(rule.ruleId || ''),
    type: String(rule.type || ''),
    sourceField: String(rule.sourceField || ''),
    sourceValue: readPath(formData, rule.sourceField),
    paramName: String(rule.remoteConfig?.paramName || rule.orgConfig?.paramName || rule.sourceField || ''),
    emptyStrategy: String(rule.emptyStrategy || 'empty'),
    remoteConfig: { ...(rule.remoteConfig || {}) },
    dictConfig: { ...(rule.dictConfig || {}) },
    objectConfig: { ...(rule.objectConfig || {}) },
  }
}

export function filterFieldOptionsByLinkage(options = [], linkageContext = null) {
  const source = Array.isArray(options) ? options : []
  if (!linkageContext)
    return source
  const sourceValue = linkageContext.sourceValue
  if (sourceValue === undefined || sourceValue === null || sourceValue === '')
    return linkageContext.emptyStrategy === 'all' ? source : []
  if (!['linkedDict', 'parentDictCode'].includes(linkageContext.type))
    return source
  const metadataKeys = ['parentValue', 'parentCode', 'parentDictCode', 'linkedDictValue', 'linked_dict_value']
  const hasMetadata = source.some(option => metadataKeys.some(key => option?.[key] !== undefined))
  if (!hasMetadata)
    return source
  return source.filter(option => metadataKeys.some(key => option?.[key] !== undefined
    && String(option[key]) === String(sourceValue)))
}

export function resolveBottomBarActions(bottomBar = {}, data = {}, mode = '', permissions = []) {
  const supportedTypes = new Set(['save', 'reset', 'action', 'cancel', 'flow_action'])
  return (Array.isArray(bottomBar?.actions) ? bottomBar.actions : [])
    .filter(action => action && supportedTypes.has(String(action.type || '').toLowerCase()))
    .filter((action) => {
      const type = String(action.type || '').toLowerCase()
      const visibleModes = Array.isArray(action.visibleInModes) ? action.visibleInModes.map(String) : []
      if (visibleModes.length && !visibleModes.includes(String(mode)))
        return false
      if (mode && type === 'reset' && mode !== 'create')
        return false
      return !(mode === 'detail' && type === 'save')
    })
    .filter(action => actionVisible(action, data))
    .map(action => resolveActionPermission(action, permissions))
    .filter(Boolean)
}

export function resolveActionPermissionKey(action = {}) {
  return String(action.permissionKey || action.permissionCode || action.permission || '').trim()
}

export function hasActionPermission(action = {}, permissions = []) {
  const required = resolveActionPermissionKey(action)
  if (!required)
    return true
  return (Array.isArray(permissions) ? permissions : [])
    .map(permission => String(permission || '').trim())
    .filter(Boolean)
    .some(permission => permissionPatternMatches(permission, required))
}

export function resolveActionPermission(action = {}, permissions = []) {
  if (hasActionPermission(action, permissions))
    return { ...action, disabled: action.disabled === true, permissionDenied: false }
  if (action.permissionStrategy === 'disable')
    return { ...action, disabled: true, permissionDenied: true }
  return null
}

function permissionPatternMatches(granted, required) {
  if (granted === '**' || granted === '*:*:*' || granted === required)
    return true
  if (!granted.includes('*'))
    return false
  const escaped = granted.replace(/[.+?^${}()|[\]\\]/g, '\\$&')
  const pattern = escaped.replace(/\*\*/g, '.*').replace(/\*/g, '[^:]*')
  return new RegExp(`^${pattern}$`).test(required)
}

export function childDataKeys(child = {}) {
  return uniqueNonBlank([
    child.modelCode,
    child.relationKey,
    child.key,
  ])
}

export function resolveChildRows(child = {}, childData = {}) {
  for (const key of childDataKeys(child)) {
    if (Array.isArray(childData[key]))
      return childData[key]
  }
  return []
}

export function ensureChildRows(child = {}, childData = {}) {
  const rows = resolveChildRows(child, childData)
  if (rows.length || childDataKeys(child).some(key => Array.isArray(childData[key]))) {
    syncChildRowAliases(child, childData, rows)
    return rows
  }
  const key = child.modelCode || child.relationKey || child.key
  if (!key)
    return []
  childData[key] = []
  syncChildRowAliases(child, childData, childData[key])
  return childData[key]
}

export function syncChildRowAliases(child = {}, childData = {}, rows = resolveChildRows(child, childData)) {
  if (!Array.isArray(rows))
    return
  for (const key of childDataKeys(child)) {
    if (!Array.isArray(childData[key]))
      childData[key] = rows
  }
}

export function resolveChildTitle(child = {}) {
  const label = [
    child.tabTitle,
    child.relationName,
    child.modelName,
    child.objectName,
    child.tableComment,
    child.label,
    child.name,
  ].map(value => String(value || '').trim()).find(Boolean)
  if (label)
    return label
  return prettifyTechnicalName(child.key || child.relationKey || child.modelCode || '明细')
}

export function resolveChildSubtitle(child = {}) {
  const label = [
    child.modelName,
    child.objectName,
    child.tableComment,
  ].map(value => String(value || '').trim()).find(Boolean)
  return label || ''
}

export function normalizeActions(config = {}) {
  const source = Array.isArray(config.options?.actions) ? config.options.actions : []
  const byCode = new Map(source.map(action => [String(action?.actionCode || action?.key || ''), action]))
  const rowActions = Array.isArray(config.options?.rowActions) ? config.options.rowActions : []
  const children = normalizeChildrenConfig(config)
  return [...rowActions, ...children.flatMap(child => child.rowActions)]
    .map(action => ({ ...byCode.get(String(action?.actionCode || action?.key || '')), ...action }))
    .filter(action => action.actionCode || action.key)
}

export function normalizeDesignerField(component = {}) {
  const binding = component.fieldBinding || {}
  const field = String(binding.fieldCode || component.field || '').trim()
  if (!field)
    return null
  const validation = component.validation || {}
  const visibility = component.visibility || {}
  return {
    field,
    label: component.label || field,
    type: component.componentKey || component.type || 'input',
    props: { ...(component.props || {}) },
    required: validation.required === true,
    requiredMessage: validation.requiredMessage,
    readonly: visibility.readonly === true,
    hidden: visibility.hidden === true,
    defaultValue: component.defaultValue ?? component.props?.defaultValue,
    runtimeRules: component.props?.runtimeRules || component.runtimeRules || [],
    formVisible: component.formVisible !== false,
  }
}

// ── Layout node helpers (tabs / card / collapse) ──

const LAYOUT_NODE_TYPES = new Set([
  'row', 'fcRow', 'col',
  'card', 'elCard',
  'tabs', 'elTabs',
  'collapse', 'elCollapse',
])

function resolveNodeType(component = {}) {
  return component.nodeType || component.type || component.componentKey || ''
}

export function isDesignerLayoutNode(component = {}) {
  return LAYOUT_NODE_TYPES.has(resolveNodeType(component))
}

export function hasDesignerLayoutNodes(components = []) {
  return Array.isArray(components) && components.some(isDesignerLayoutNode)
}

function normalizePaneChild(component, index, fieldByCode) {
  const label = component.label || component.props?.label || component.props?.title || `分组 ${index + 1}`
  return {
    key: component.id || component.key || `pane_${index}`,
    nodeType: resolveNodeType(component),
    label,
    props: { ...(component.props || {}) },
    children: normalizeDesignerNodeTree(component.children || [], fieldByCode),
  }
}

function normalizeDesignerNodeTree(components, fieldByCode) {
  if (!Array.isArray(components)) return []
  return components.map((comp, index) => {
    if (isDesignerLayoutNode(comp)) {
      const isTabs = ['tabs', 'elTabs'].includes(resolveNodeType(comp))
      const isCollapse = ['collapse', 'elCollapse'].includes(resolveNodeType(comp))
      const children = Array.isArray(comp.children)
        ? (isTabs || isCollapse)
            ? comp.children.map((child, i) => normalizePaneChild(child, i, fieldByCode))
            : normalizeDesignerNodeTree(comp.children, fieldByCode)
        : []
      return {
        key: comp.id || comp.key || `layout_${resolveNodeType(comp)}_${index}`,
        nodeType: resolveNodeType(comp),
        label: comp.label || '',
        props: { ...(comp.props || {}) },
        children,
      }
    }
    // Field or pane-wrapper treated as field
    const normalized = normalizeDesignerField(comp)
    if (normalized) {
      const stored = fieldByCode.get(normalized.field)
      return stored ? mergeField(stored, normalized) : normalized
    }
    return null
  }).filter(Boolean)
}

/**
 * Build a renderable node tree from designer components, merging stored
 * field data (editSchema) into leaf field nodes.  Layout nodes (tabs/card/
 * collapse) are preserved; non-field non-layout nodes become field leaves.
 */
export function normalizeDesignerComponents(config = {}, formDesignerSchema) {
  const schema = formDesignerSchema || config.options?.formDesignerSchema
  const components = Array.isArray(schema?.components) ? schema.components : []
  if (!components.length) return []
  const stored = Array.isArray(config.editSchema) ? config.editSchema : []
  const storedFields = stored.map(normalizeField).filter(f => f?.field)
  const fieldByCode = new Map(storedFields.map(f => [f.field, f]))
  return normalizeDesignerNodeTree(components, fieldByCode)
}

export function normalizeField(field = {}) {
  const type = String(field.type || field.componentType || 'input')
  return {
    ...field,
    field: String(field.field || field.sourceField || '').trim(),
    label: field.label || field.field || '',
    type,
    props: { ...(field.props || {}) },
    runtimeRules: field.runtimeRules || field.props?.runtimeRules || [],
    required: field.required === true,
    readonly: field.readonly === true,
  }
}

function mergeField(stored, designer) {
  const base = normalizeField(stored)
  if (!designer)
    return base
  return {
    ...designer,
    ...base,
    props: { ...(designer.props || {}), ...(base.props || {}) },
    runtimeRules: base.runtimeRules?.length ? base.runtimeRules : designer.runtimeRules,
    hidden: base.hidden === true || designer.hidden === true,
    readonly: base.readonly === true || designer.readonly === true,
    required: base.required === true || designer.required === true,
  }
}

function isRenderableField(field) {
  return Boolean(field?.field && field.formVisible !== false && field.type !== 'sectionTitle' && field.type !== 'groupTitle')
}

export function resolveFieldControl(field = {}, context = {}) {
  const control = {
    visible: !(field.hidden === true || field.visible === false || field.formVisible === false),
    readonly: Boolean(field.readonly || field.props?.readonly),
    required: field.required === true,
  }
  const rules = [
    ...(Array.isArray(field.runtimeRules) ? field.runtimeRules : []),
    ...(Array.isArray(field.visibilityRules) ? field.visibilityRules : []),
    ...(Array.isArray(field.displayRules) ? field.displayRules : []),
  ]
  const defaults = rules.map(rule => rule?.effect?.whenUnmatched || rule?.whenUnmatched).find(Boolean)
  if (defaults === 'hidden') control.visible = false
  if (defaults === 'visible') control.visible = true
  for (const rule of rules) {
    if (rule?.enabled === false || !matchRule(rule, context)) continue
    const effect = { ...(rule.effect || {}), ...(rule.actions || {}), ...rule }
    if (Object.prototype.hasOwnProperty.call(effect, 'visible')) control.visible = effect.visible !== false
    if (effect.hidden === true) control.visible = false
    if (Object.prototype.hasOwnProperty.call(effect, 'readonly')) control.readonly = effect.readonly === true
    if (Object.prototype.hasOwnProperty.call(effect, 'required')) control.required = effect.required === true
  }
  return control
}

export function matchRule(rule = {}, context = {}) {
  const conditions = Array.isArray(rule.conditions) ? rule.conditions : []
  if (!conditions.length && rule.field)
    return matchCondition(rule, context)
  if (!conditions.length) return true
  const mode = String(rule.mode || 'all').toLowerCase() === 'any' ? 'any' : 'all'
  return mode === 'any' ? conditions.some(condition => matchCondition(condition, context)) : conditions.every(condition => matchCondition(condition, context))
}

function matchCondition(condition = {}, context = {}) {
  const source = condition.source || 'record'
  const record = source === 'formData' ? context.formData : source === 'row' ? context.row : source === 'user' ? context.user : context.record || context.formData || {}
  const actual = readPath(record, condition.field || condition.path || condition.key)
  const expected = condition.valueSource === 'field'
    ? readPath(context.formData || context.record || {}, condition.valueField || condition.expectedField)
    : Object.prototype.hasOwnProperty.call(condition, 'expected') ? condition.expected : condition.value
  const op = String(condition.operator || condition.op || 'eq').toLowerCase()
  if (op === 'empty') return actual === undefined || actual === null || actual === ''
  if (op === 'notempty') return !(actual === undefined || actual === null || actual === '')
  if (op === 'contains') return String(actual ?? '').includes(String(expected ?? ''))
  if (op === 'notcontains') return !String(actual ?? '').includes(String(expected ?? ''))
  if (op === 'in') return (Array.isArray(expected) ? expected : String(expected ?? '').split(',')).map(String).includes(String(actual ?? ''))
  if (['gt', 'gte', 'lt', 'lte'].includes(op)) {
    const left = Number(actual); const right = Number(expected)
    if (!Number.isFinite(left) || !Number.isFinite(right)) return false
    return op === 'gt' ? left > right : op === 'gte' ? left >= right : op === 'lt' ? left < right : left <= right
  }
  if (op === 'ne' || op === '!=') return String(actual ?? '') !== String(expected ?? '')
  return String(actual ?? '') === String(expected ?? '')
}

export function readPath(source, path = '') {
  return String(path || '').split('.').filter(Boolean).reduce((value, key) => value?.[key], source)
}

export function applyEventMappings(rule = {}, data, formData = {}) {
  const selected = String(rule.resultMode || 'ROOT').toUpperCase() === 'FIRST_ROW'
    ? (Array.isArray(data) ? data[0] : data?.records?.[0] || data?.list?.[0] || data?.rows?.[0])
    : data
  const found = selected !== undefined && selected !== null && !(Array.isArray(selected) && selected.length === 0)
  const patch = {}
  for (const mapping of Array.isArray(rule.resultMappings) ? rule.resultMappings : []) {
    const value = found ? readPath(selected, mapping.from) : undefined
    if (value !== undefined) patch[mapping.to] = value
    else if (String(mapping.whenMissing || 'CLEAR').toUpperCase() === 'CLEAR') patch[mapping.to] = ''
  }
  return { found, patch }
}

export function buildEventParams(rule = {}, formData = {}, context = {}, routeQuery = {}) {
  const params = {}
  for (const mapping of Array.isArray(rule.paramMappings) ? rule.paramMappings : []) {
    const source = String(mapping.source || '').toUpperCase()
    const value = source === 'FORM_FIELD'
      ? readPath(formData, mapping.field)
      : source === 'ROUTE_QUERY'
        ? readPath(routeQuery, mapping.path)
        : readPath(context, mapping.path)
    params[mapping.param] = value
  }
  return params
}

export function shouldSkipFieldEvent(rule = {}, formData = {}) {
  if (rule.skipWhenEmpty === false || !rule.sourceField)
    return false
  const value = readPath(formData, rule.sourceField)
  return value === undefined
    || value === null
    || (typeof value === 'string' && !value.trim())
    || (Array.isArray(value) && value.length === 0)
}

export function buildEventClearPatch(rule = {}) {
  const patch = {}
  for (const mapping of Array.isArray(rule.resultMappings) ? rule.resultMappings : []) {
    if (String(mapping.whenMissing || 'CLEAR').toUpperCase() === 'CLEAR' && mapping.to)
      patch[mapping.to] = ''
  }
  return patch
}

export function normalizeScanContext(scan) {
  const value = String(scan?.value || '').trim()
  if (!value || value.length > 2048)
    return null
  return {
    value,
    type: String(scan?.type || 'UNKNOWN').trim().slice(0, 64) || 'UNKNOWN',
    platform: String(scan?.platform || 'BROWSER').trim().slice(0, 32) || 'BROWSER',
  }
}

export function safeEventRules(rules, fields = []) {
  const known = new Set(fields.map(field => field.field || field).filter(Boolean).map(String))
  return (Array.isArray(rules) ? rules : []).filter(rule => {
    if (!rule || rule.enabled === false || DANGEROUS_KEYS.has(String(rule.sourceKey || '').toLowerCase())) return false
    if (!rule.sourceKey || DANGEROUS_KEYS.has(String(rule.sourceType || '').toLowerCase())) return false
    if (rule.trigger !== 'FORM_LOAD' && known.size && !known.has(rule.sourceField)) return false
    return !containsDangerousKey(rule)
  })
}

function containsDangerousKey(value, seen = new Set()) {
  if (!value || typeof value !== 'object') return false
  if (seen.has(value)) return true
  seen.add(value)
  const result = Object.entries(value).some(([key, child]) => DANGEROUS_KEYS.has(key.toLowerCase()) || containsDangerousKey(child, seen))
  seen.delete(value)
  return result
}

export function normalizeDictOptions(data) {
  const rows = Array.isArray(data) ? data : data?.records || data?.list || data?.rows || []
  return rows.map(item => ({
    ...item,
    label: item.dictLabel || item.label || item.name || item.value,
    value: item.dictValue ?? item.value ?? item.code,
  })).filter(item => item.value !== undefined && item.value !== null)
}

export function resolveActionDefinition(config = {}, action = {}) {
  const actions = Array.isArray(config.options?.actions) ? config.options.actions : []
  const full = actions.find(item => String(item?.actionCode || item?.key) === String(action?.actionCode || action?.key))
  return { ...(full || {}), ...action, actionCode: action.actionCode || action.key || full?.actionCode || full?.key }
}

export function actionVisible(action = {}, row = {}) {
  if (action.visible === false || action.enabled === false) return false
  const condition = action.displayCondition || action.visibleCondition
  if (!condition) return true
  if (typeof condition === 'string') {
    const match = condition.match(/^\s*([\w.]+)\s*(==|=|!=)\s*(.+?)\s*$/)
    if (!match) return true
    const actual = readPath(row, match[1]); const expected = match[3].replace(/^['"]|['"]$/g, '')
    return match[2] === '!=' ? String(actual ?? '') !== expected : String(actual ?? '') === expected
  }
  return matchRule(condition, { record: row, row, formData: row })
}

export function actionInputSchema(action = {}) {
  const config = parseJson(action.actionConfig, {})
  return Array.isArray(config.inputSchema) ? config.inputSchema : []
}

export function buildActionPayload({ action, config, objectCode, recordId, parentRecordId, childRecordId, relationKey, formData, routeQuery }) {
  return {
    suiteCode: action.suiteCode || config.options?.suiteCode || '',
    objectCode,
    recordId: recordId === undefined || recordId === null ? '' : String(recordId),
    actionCode: action.actionCode || action.key || '',
    formData: formData && typeof formData === 'object' ? { ...formData } : {},
    context: { routeQuery: { ...(routeQuery || {}) } },
    ...(parentRecordId ? { parentRecordId: String(parentRecordId) } : {}),
    ...(childRecordId ? { childRecordId: String(childRecordId) } : {}),
    ...(relationKey ? { relationKey: String(relationKey) } : {}),
    idempotencyKey: `h5:${Date.now().toString(36)}:${Math.random().toString(36).slice(2, 12)}`,
  }
}

export function buildDefaultData(fields = []) {
  return Object.fromEntries(fields.map(field => [field.field, field.defaultValue ?? field.props?.defaultValue ?? '']))
}

function uniqueNonBlank(values = []) {
  return Array.from(new Set(values
    .map(value => String(value || '').trim())
    .filter(Boolean)))
}

function prettifyTechnicalName(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return '明细'
  return text
    .replace(/[_-]+/g, ' ')
    .replace(/\b\w/g, letter => letter.toUpperCase())
}
