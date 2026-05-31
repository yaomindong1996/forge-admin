export const VIEW_SCHEMA_VERSION = 'view-schema-v1'
export const VIEW_SCHEMA_KEY = 'viewSchema'

export function createDefaultViewSchema(options = {}) {
  const fields = Array.isArray(options.fields) ? options.fields : []
  return normalizeViewSchema({
    schemaVersion: VIEW_SCHEMA_VERSION,
    search: {
      fields: fields
        .filter(field => field && field.searchable === true)
        .map((field, index) => createSearchField(field, index)),
    },
    list: {
      columns: fields
        .filter(field => field && field.listVisible !== false)
        .map((field, index) => createListColumn(field, index)),
    },
    detail: {
      sections: [{
        sectionKey: 'basic',
        title: '基础信息',
        fields: fields
          .filter(field => field && field.formVisible !== false)
          .map((field, index) => createDetailField(field, index)),
      }],
    },
    overrides: {},
  })
}

export function normalizeViewSchema(source = {}) {
  const schema = isPlainObject(source) ? cloneValue(source) : {}
  return {
    schemaVersion: schema.schemaVersion || VIEW_SCHEMA_VERSION,
    search: {
      fields: normalizeFieldItems(schema.search?.fields, normalizeSearchField),
      settings: isPlainObject(schema.search?.settings) ? schema.search.settings : {},
    },
    list: {
      columns: normalizeFieldItems(schema.list?.columns, normalizeListColumn),
      settings: isPlainObject(schema.list?.settings) ? schema.list.settings : {},
    },
    detail: {
      sections: normalizeDetailSections(schema.detail?.sections),
      settings: isPlainObject(schema.detail?.settings) ? schema.detail.settings : {},
    },
    overrides: isPlainObject(schema.overrides) ? schema.overrides : {},
  }
}

export function mergeViewOverrides(baseSchema = {}, overrideSchema = {}) {
  const base = normalizeViewSchema(baseSchema)
  const overrides = normalizeViewSchema(overrideSchema)
  return normalizeViewSchema({
    ...base,
    search: {
      ...base.search,
      ...overrides.search,
      fields: mergeByField(base.search.fields, overrides.search.fields),
    },
    list: {
      ...base.list,
      ...overrides.list,
      columns: mergeByField(base.list.columns, overrides.list.columns),
    },
    detail: {
      ...base.detail,
      ...overrides.detail,
    },
    overrides: {
      ...base.overrides,
      ...overrides.overrides,
    },
  })
}

export function validateViewSchema(source = {}) {
  const schema = normalizeViewSchema(source)
  const errors = []
  collectDuplicateFields(schema.search.fields, 'search.fields', errors)
  collectDuplicateFields(schema.list.columns, 'list.columns', errors)
  schema.detail.sections.forEach((section, sectionIndex) => {
    collectDuplicateFields(section.fields, `detail.sections[${sectionIndex}].fields`, errors)
  })
  return {
    valid: errors.length === 0,
    errors,
    schema,
  }
}

export function createViewSchemaFromPageSchema(pageSchema = {}, fields = [], currentSchema = {}) {
  const fieldMap = new Map((Array.isArray(fields) ? fields : [])
    .map(field => [field.fieldCode || field.field, field])
    .filter(([fieldCode]) => fieldCode))
  const base = createDefaultViewSchema({ fields })
  const current = normalizeViewSchema(currentSchema || {})
  const searchZone = findZone(pageSchema, 'search')
  const tableZone = findZone(pageSchema, 'table')
  const detailZone = findZone(pageSchema, 'detail')

  return normalizeViewSchema({
    ...current,
    search: {
      ...current.search,
      fields: buildSearchFields(searchZone, fieldMap, base.search.fields),
    },
    list: {
      ...current.list,
      columns: buildListColumns(tableZone, fieldMap, base.list.columns),
    },
    detail: {
      ...current.detail,
      sections: buildDetailSections(detailZone, fieldMap, base.detail.sections),
    },
  })
}

function createSearchField(field = {}, index = 0) {
  return {
    fieldCode: field.fieldCode || field.field,
    label: field.fieldName || field.label || field.fieldCode || field.field,
    componentKey: resolveSearchComponent(field),
    visible: true,
    order: resolveOrder(field, index),
    align: 'left',
    defaultValue: field.defaultValue ?? null,
    collapsed: index > 3,
    matchMode: field.queryType || 'eq',
  }
}

function createListColumn(field = {}, index = 0) {
  return {
    fieldCode: field.fieldCode || field.field,
    label: field.fieldName || field.label || field.fieldCode || field.field,
    visible: true,
    order: resolveOrder(field, index),
    align: resolveDefaultColumnAlign(field),
    width: field.width || resolveDefaultWidth(field),
    fixed: null,
    sortable: Boolean(field.sortable),
    formatter: field.dictType ? 'dictTag' : null,
  }
}

function createDetailField(field = {}, index = 0) {
  return {
    fieldCode: field.fieldCode || field.field,
    label: field.fieldName || field.label || field.fieldCode || field.field,
    visible: true,
    order: resolveOrder(field, index),
    align: 'left',
    formatter: field.dictType ? 'dictTag' : null,
  }
}

function normalizeSearchField(item = {}, index = 0) {
  return {
    fieldCode: item.fieldCode || item.field || '',
    label: item.label || item.title || item.fieldCode || item.field || '字段',
    componentKey: item.componentKey || item.type || 'input',
    visible: item.visible !== false,
    order: Number(item.order ?? index),
    align: normalizeAlign(item.align),
    defaultValue: item.defaultValue ?? null,
    collapsed: Boolean(item.collapsed),
    matchMode: item.matchMode || item.queryType || 'eq',
  }
}

function normalizeListColumn(item = {}, index = 0) {
  return {
    fieldCode: item.fieldCode || item.field || '',
    label: item.label || item.title || item.fieldCode || item.field || '字段',
    visible: item.visible !== false,
    order: Number(item.order ?? index),
    align: normalizeAlign(item.align),
    width: item.width || null,
    minWidth: item.minWidth || null,
    fixed: item.fixed || null,
    sortable: Boolean(item.sortable),
    formatter: item.formatter || null,
  }
}

function normalizeDetailField(item = {}, index = 0) {
  return {
    fieldCode: item.fieldCode || item.field || '',
    label: item.label || item.title || item.fieldCode || item.field || '字段',
    visible: item.visible !== false,
    order: Number(item.order ?? index),
    align: normalizeAlign(item.align),
    readonly: item.readonly !== false,
    formatter: item.formatter || null,
  }
}

function normalizeDetailSections(sections = []) {
  const items = Array.isArray(sections) && sections.length ? sections : [{ sectionKey: 'basic', title: '基础信息', fields: [] }]
  return items.map((section, index) => ({
    sectionKey: section.sectionKey || section.key || `section_${index + 1}`,
    title: section.title || section.label || '基础信息',
    visible: section.visible !== false,
    order: Number(section.order ?? index),
    fields: normalizeFieldItems(section.fields, normalizeDetailField),
  }))
}

function normalizeFieldItems(items = [], normalizer) {
  return (Array.isArray(items) ? items : [])
    .map((item, index) => normalizer(item, index))
    .filter(item => item.fieldCode)
    .sort((a, b) => a.order - b.order)
}

function mergeByField(baseItems = [], overrideItems = []) {
  const map = new Map(baseItems.map(item => [item.fieldCode, { ...item }]))
  overrideItems.forEach((item) => {
    map.set(item.fieldCode, {
      ...(map.get(item.fieldCode) || {}),
      ...item,
    })
  })
  return Array.from(map.values()).sort((a, b) => a.order - b.order)
}

function collectDuplicateFields(items = [], path, errors) {
  const seen = new Set()
  items.forEach((item, index) => {
    if (seen.has(item.fieldCode)) {
      errors.push({
        path: `${path}[${index}].fieldCode`,
        message: `字段重复：${item.fieldCode}`,
      })
    }
    seen.add(item.fieldCode)
  })
}

function buildSearchFields(zone = {}, fieldMap, fallbackFields = []) {
  const refs = resolveZoneRefs(zone, fallbackFields.map(item => item.fieldCode))
  const settings = zone?.props?.fieldSettings || {}
  return refs.map((fieldCode, index) => {
    const field = fieldMap.get(fieldCode) || {}
    const setting = settings[fieldCode] || {}
    return {
      fieldCode,
      label: field.label || field.fieldName || fieldCode,
      componentKey: setting.componentType || resolveSearchComponent(field),
      visible: zone?.enabled !== false,
      order: index,
      align: setting.align || 'left',
      defaultValue: setting.defaultValue ?? field.defaultValue ?? null,
      collapsed: Boolean(setting.collapsed),
      matchMode: setting.queryType || field.queryType || 'eq',
    }
  })
}

function buildListColumns(zone = {}, fieldMap, fallbackColumns = []) {
  const refs = resolveZoneRefs(zone, fallbackColumns.map(item => item.fieldCode))
  const settings = zone?.props?.fieldSettings || {}
  return refs.map((fieldCode, index) => {
    const field = fieldMap.get(fieldCode) || {}
    const setting = settings[fieldCode] || {}
    return {
      fieldCode,
      label: field.label || field.fieldName || fieldCode,
      visible: zone?.enabled !== false,
      order: index,
      align: setting.align || resolveDefaultColumnAlign(field),
      width: setting.width || field.width || resolveDefaultWidth(field),
      fixed: normalizeFixed(setting.fixed),
      sortable: Boolean(setting.sortable ?? field.sortable),
      formatter: setting.renderType || (field.dictType ? 'dictTag' : null),
    }
  })
}

function buildDetailSections(zone = {}, fieldMap, fallbackSections = []) {
  const groups = Array.isArray(zone?.props?.detailGroups) && zone.props.detailGroups.length
    ? zone.props.detailGroups
    : [{
        key: 'basic',
        title: '基础信息',
        items: resolveZoneRefs(zone, fallbackSections.flatMap(section => section.fields.map(item => item.fieldCode)))
          .map(fieldRef => ({ fieldRef })),
      }]
  return groups.map((group, groupIndex) => ({
    sectionKey: group.key || group.sectionKey || `section_${groupIndex + 1}`,
    title: group.title || '基础信息',
    visible: group.hidden !== true,
    order: groupIndex,
    fields: (group.items || [])
      .map((item, index) => {
        const fieldCode = item.fieldRef || item.fieldCode || item.field
        const field = fieldMap.get(fieldCode) || {}
        return {
          fieldCode,
          label: field.label || field.fieldName || fieldCode,
          visible: item.hidden !== true,
          order: index,
          align: item.align || 'left',
          readonly: item.readonly !== false,
          formatter: field.dictType ? 'dictTag' : null,
        }
      })
      .filter(item => item.fieldCode),
  }))
}

function resolveZoneRefs(zone = {}, fallbackRefs = []) {
  const refs = Array.isArray(zone?.fieldRefs) && zone.fieldRefs.length ? zone.fieldRefs : fallbackRefs
  return Array.from(new Set((refs || []).filter(Boolean)))
}

function findZone(pageSchema = {}, zoneKey) {
  return (pageSchema.zones || []).find(zone => zone?.zoneKey === zoneKey) || null
}

function resolveSearchComponent(field = {}) {
  if (field.dictType || ['select', 'radio', 'checkbox', 'dictSelect'].includes(field.componentType))
    return 'dictSelect'
  if (['orgTreeSelect', 'regionTreeSelect'].includes(field.componentType))
    return field.componentType
  if (field.componentType === 'userSelect')
    return 'userSelect'
  if (['date', 'datetime'].includes(field.componentType))
    return field.componentType
  return 'input'
}

function resolveDefaultColumnAlign(field = {}) {
  if (['int', 'bigint', 'decimal'].includes(field.dataType) || field.componentType === 'number')
    return 'right'
  if (['switch', 'date', 'datetime'].includes(field.componentType))
    return 'center'
  return 'left'
}

function resolveDefaultWidth(field = {}) {
  return field.width || (field.componentType === 'textarea' ? 220 : 160)
}

function resolveOrder(field = {}, index = 0) {
  return Number(field.sortOrder ?? index)
}

function normalizeAlign(value) {
  return ['left', 'center', 'right'].includes(value) ? value : 'left'
}

function normalizeFixed(value) {
  return ['left', 'right'].includes(value) ? value : null
}

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}
