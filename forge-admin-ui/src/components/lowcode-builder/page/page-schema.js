export const pageZoneCatalog = [
  {
    zoneKey: 'search',
    componentKey: 'search-form',
    title: '查询页',
    desc: '查询集、重置、自定义筛选条件',
  },
  {
    zoneKey: 'table',
    componentKey: 'data-table',
    title: '列表页',
    desc: '列表列、导入导出、批量操作',
  },
  {
    zoneKey: 'edit',
    componentKey: 'edit-form',
    title: '表单与详情页',
    desc: '新增、编辑、详情展示共用字段',
  },
  {
    zoneKey: 'detail',
    componentKey: 'detail-panel',
    title: '详情页兼容区',
    desc: '历史协议保留，新配置使用表单与详情页',
  },
]

export const canvasComponentCatalog = [
  {
    group: 'business',
    componentKey: 'query-set',
    title: '查询集',
    desc: '选择查询字段、调整顺序',
    zones: ['search', 'table'],
    defaultWidth: 640,
    defaultHeight: 132,
    multiField: true,
  },
  {
    group: 'business',
    componentKey: 'custom-query',
    title: '自定义查询',
    desc: '高级查询入口',
    zones: ['search', 'table'],
    defaultWidth: 128,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'import-button',
    title: '导入',
    desc: 'Excel 批量导入',
    zones: ['table'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'export-button',
    title: '导出',
    desc: 'Excel 动态导出',
    zones: ['table'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'add-button',
    title: '新增',
    desc: '打开新增表单',
    zones: ['table'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'business',
    componentKey: 'reset-button',
    title: '重置',
    desc: '清空当前表单',
    zones: ['search'],
    defaultWidth: 104,
    defaultHeight: 40,
  },
  {
    group: 'data',
    componentKey: 'data-table',
    title: '数据列表',
    desc: '配置展示列和顺序',
    zones: ['table'],
    defaultWidth: 860,
    defaultHeight: 280,
    multiField: true,
  },
  {
    group: 'field',
    componentKey: 'field-input',
    title: '单行输入',
    desc: '文本字段',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-textarea',
    title: '多行文本',
    desc: '长文本字段',
    zones: ['edit', 'detail'],
    defaultWidth: 580,
    defaultHeight: 98,
  },
  {
    group: 'field',
    componentKey: 'field-number',
    title: '数字输入',
    desc: '整数、小数、金额',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-select',
    title: '下拉选择',
    desc: '系统字典或枚举',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-dict-select',
    title: '字典选择器',
    desc: '系统字典组件',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-tree-select',
    title: '树形选择',
    desc: '组织、分类、区域等树形字段',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-org-tree-select',
    title: '组织树选择',
    desc: '当前系统组织树',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-user-select',
    title: '用户选择',
    desc: '当前系统用户列表',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-region-tree-select',
    title: '区划树选择',
    desc: '行政区划树组件',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-cascader',
    title: '级联选择',
    desc: '多级分类或行政区划',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-date',
    title: '日期',
    desc: '日期选择',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-datetime',
    title: '日期时间',
    desc: '日期时间选择',
    zones: ['search', 'edit', 'detail'],
    defaultWidth: 280,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-switch',
    title: '开关',
    desc: '启用、禁用类字段',
    zones: ['edit', 'detail'],
    defaultWidth: 220,
    defaultHeight: 64,
  },
  {
    group: 'field',
    componentKey: 'field-upload',
    title: '文件上传',
    desc: '附件上传',
    zones: ['edit', 'detail'],
    defaultWidth: 300,
    defaultHeight: 72,
  },
  {
    group: 'field',
    componentKey: 'field-image-upload',
    title: '图片上传',
    desc: '图片上传',
    zones: ['edit', 'detail'],
    defaultWidth: 300,
    defaultHeight: 88,
  },
]

const hiddenPageFieldNames = new Set(['tenantId', 'delFlag'])
const hiddenPageColumnNames = new Set(['tenant_id', 'del_flag'])
const readonlySystemFieldNames = new Set([
  'id',
  'tenantId',
  'createBy',
  'createTime',
  'createDept',
  'updateBy',
  'updateTime',
  'delFlag',
])
const readonlySystemColumnNames = new Set([
  'id',
  'tenant_id',
  'create_by',
  'create_time',
  'create_dept',
  'update_by',
  'update_time',
  'del_flag',
])

export function isHiddenPageField(field = {}) {
  const fieldName = field.sourceField || field.field
  return hiddenPageFieldNames.has(fieldName)
    || hiddenPageFieldNames.has(field.field)
    || hiddenPageColumnNames.has(field.columnName)
}

export function isInactivePageField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

export function isReadonlySystemField(field = {}) {
  const fieldName = field.sourceField || field.field
  return Boolean(field.systemField)
    || Boolean(field.readonly)
    || readonlySystemFieldNames.has(fieldName)
    || readonlySystemFieldNames.has(field.field)
    || readonlySystemColumnNames.has(field.columnName)
}

export function isPageFieldVisible(field = {}, zoneKey = 'table') {
  if (!field || isHiddenPageField(field) || isInactivePageField(field))
    return false
  if (zoneKey === 'edit')
    return !isReadonlySystemField(field) && field.formVisible !== false
  if (zoneKey === 'detail')
    return field.formVisible !== false
  if (zoneKey === 'search')
    return !isReadonlySystemField(field)
  if (zoneKey === 'table')
    return field.listVisible !== false
  return true
}

function filterPageFields(fields = [], zoneKey = 'table') {
  return (fields || []).filter(field => isPageFieldVisible(field, zoneKey))
}

function isChildPageModelField(field = {}) {
  const sourceField = field.sourceField || field.field
  return Boolean(field.modelCode) && field.field !== sourceField
}

function mergePageFieldRefs(...groups) {
  return Array.from(new Set(groups.flat().filter(Boolean)))
}

export function createDefaultPageSchema(modelSchema) {
  const fields = modelSchema?.fields || []
  const isTree = modelSchema?.appType === 'TREE'
  const isMasterDetail = modelSchema?.appType === 'MASTER_DETAIL'
  const layoutType = isTree ? 'tree-crud' : isMasterDetail ? 'master-detail-crud' : 'simple-crud'
  const treeConfig = isTree ? resolveDefaultTreeConfig(modelSchema) : null
  const schema = {
    layoutType,
    listLayoutMode: 'grid',
    listGridLayout: createDefaultListGridLayout(modelSchema, { layoutType }),
    zones: [
      {
        zoneKey: 'search',
        componentKey: 'search-form',
        enabled: true,
        fieldRefs: filterPageFields(fields, 'search').map(field => field.field),
        props: {},
      },
      {
        zoneKey: 'table',
        componentKey: 'data-table',
        enabled: true,
        fieldRefs: filterPageFields(fields, 'table').map(field => field.field),
        props: {
          showImport: true,
          showExport: true,
          hideBatchDelete: false,
          enableCustomQuery: true,
          customActions: [],
          defaultSortField: 'id',
          defaultSortOrder: 'desc',
          ...(isTree
            ? {
                treeConfig,
              }
            : {}),
        },
      },
      {
        zoneKey: 'edit',
        componentKey: 'edit-form',
        enabled: true,
        fieldRefs: filterPageFields(fields, 'edit').map(field => field.field),
        props: {
          editGridCols: 1,
        },
      },
      {
        zoneKey: 'detail',
        componentKey: 'detail-panel',
        enabled: false,
        fieldRefs: filterPageFields(fields, 'detail').map(field => field.field),
        props: {},
      },
    ],
  }
  return syncPageSchemaWithModel(schema, modelSchema)
}

export function syncPageSchemaWithModel(pageSchema, modelSchema) {
  const current = pageSchema || createDefaultPageSchema(modelSchema)
  const fields = modelSchema?.fields || []
  const layoutType = current.layoutType || (modelSchema?.appType === 'TREE'
    ? 'tree-crud'
    : modelSchema?.appType === 'MASTER_DETAIL' ? 'master-detail-crud' : 'simple-crud')
  const zones = (current.zones || []).map((zone) => {
    const zoneFields = filterPageFields(fields, zone.zoneKey)
    const zoneFieldSet = new Set(zoneFields.map(field => field.field))
    const childEditRefs = zone.zoneKey === 'edit' && layoutType === 'master-detail-crud'
      ? zoneFields.filter(field => isChildPageModelField(field)).map(field => field.field)
      : []
    const childEditSet = new Set(childEditRefs)
    const props = zone.props || {}
    const normalizedZone = {
      ...zone,
      fieldRefs: (zone.fieldRefs || []).filter(field => zoneFieldSet.has(field)),
      props,
    }
    const canvas = normalizeZoneCanvas(normalizedZone, modelSchema)
    const canvasRefs = resolveFieldRefsFromCanvas(canvas).filter(field => zoneFieldSet.has(field))
    const formCreateRefs = normalizedZone.zoneKey === 'edit' && !canvasRefs.length
      ? resolveFieldRefsFromFormCreateRules(props.formCreateRule, zoneFieldSet)
      : []
    const explicitRefs = (normalizedZone.fieldRefs || []).filter(field => zoneFieldSet.has(field))
    const explicitChildRefs = explicitRefs.filter(ref => childEditSet.has(ref))
    const customChildSelection = normalizedZone.zoneKey === 'edit'
      && layoutType === 'master-detail-crud'
      && (String(props.relationFieldSelectionMode || '').toUpperCase() === 'CUSTOM'
        || props.relationFieldSelectionTouched === true)
    const mergedChildRefs = customChildSelection
      ? explicitChildRefs
      : explicitChildRefs.length ? explicitChildRefs : childEditRefs
    const preferCanvasRefs = ['edit', 'detail'].includes(normalizedZone.zoneKey) && canvasRefs.length
    const fieldRefs = preferCanvasRefs
      ? mergePageFieldRefs(canvasRefs, mergedChildRefs)
      : formCreateRefs.length
        ? mergePageFieldRefs(formCreateRefs, mergedChildRefs)
        : explicitRefs.length
          ? mergePageFieldRefs(explicitRefs, mergedChildRefs)
          : mergePageFieldRefs(canvasRefs, mergedChildRefs)
    return {
      ...normalizedZone,
      fieldRefs,
      props: {
        ...normalizedZone.props,
        canvas,
      },
    }
  })

  for (const catalog of pageZoneCatalog) {
    if (!zones.some(zone => zone.zoneKey === catalog.zoneKey)) {
      const zone = {
        zoneKey: catalog.zoneKey,
        componentKey: catalog.componentKey,
        enabled: catalog.zoneKey !== 'detail',
        fieldRefs: [],
        props: {},
      }
      const canvas = normalizeZoneCanvas(zone, modelSchema)
      const zoneFields = filterPageFields(fields, zone.zoneKey)
      const zoneFieldSet = new Set(zoneFields.map(field => field.field))
      const childEditRefs = zone.zoneKey === 'edit' && layoutType === 'master-detail-crud'
        ? zoneFields.filter(field => isChildPageModelField(field)).map(field => field.field)
        : []
      const formCreateRefs = zone.zoneKey === 'edit'
        ? resolveFieldRefsFromFormCreateRules(zone.props?.formCreateRule, zoneFieldSet)
        : []
      zones.push({
        ...zone,
        fieldRefs: formCreateRefs.length
          ? mergePageFieldRefs(formCreateRefs, childEditRefs)
          : resolveFieldRefsFromCanvas(canvas).filter(field => zoneFieldSet.has(field)),
        props: {
          canvas,
        },
      })
    }
  }

  const listLayoutMode = current.listLayoutMode || 'grid'
  let listGridLayout = current.listGridLayout
  if (listLayoutMode === 'grid') {
    if (!listGridLayout || !listGridLayout.items?.length) {
      listGridLayout = bootstrapGridLayoutFromZones(zones, modelSchema, { layoutType })
    }
    listGridLayout = syncGridLayoutWithModel(listGridLayout, modelSchema, { layoutType })
  }
  const finalZones = listLayoutMode === 'grid' && listGridLayout
    ? applyGridLayoutToZones(zones, listGridLayout, modelSchema)
    : zones

  return {
    ...current,
    layoutType,
    listLayoutMode,
    listGridLayout,
    zones: finalZones,
  }
}

export function createPageModelRef(model = {}, options = {}) {
  const schema = model.modelSchema || model
  const modelCode = model.modelCode || schema?.object?.code || options.modelCode || ''
  const modelName = model.modelName || schema?.object?.name || schema?.businessName || modelCode
  const primary = Boolean(options.primary)

  return {
    modelId: isNumericId(model.id) ? Number(model.id) : null,
    modelCode,
    modelName,
    tableName: schema?.tableName || model.tableName || '',
    relations: Array.isArray(schema?.relations) ? clonePlain(schema.relations) : [],
    primary,
    fields: (schema?.fields || [])
      .filter(field => !isHiddenPageField(field) && !isInactivePageField(field))
      .map(field => ({
        ...field,
        sourceField: field.field,
        fieldRef: resolveModelFieldRef(modelCode, field.field, primary),
        modelCode,
        modelName,
      })),
  }
}

export function buildPageDesignModelSchema(modelSchema, modelRefs = []) {
  const refs = Array.isArray(modelRefs) && modelRefs.length
    ? modelRefs
    : [createPageModelRef({ modelSchema }, { primary: true })]
  const fields = refs.flatMap((modelRef) => {
    const modelCode = modelRef.modelCode || ''
    const modelName = modelRef.modelName || modelCode || '数据模型'
    return (modelRef.fields || []).filter(field => !isHiddenPageField(field) && !isInactivePageField(field)).map((field) => {
      const sourceField = field.sourceField || field.field
      const fieldRef = field.fieldRef || resolveModelFieldRef(modelCode, sourceField, modelRef.primary)
      return {
        ...field,
        field: fieldRef,
        sourceField,
        rawLabel: field.rawLabel || field.label || sourceField,
        modelId: modelRef.modelId || null,
        modelCode,
        modelName,
        label: field.label || sourceField,
        sourceLabel: modelName,
      }
    })
  })

  return {
    ...(modelSchema || {}),
    fields,
    pageModelRefs: refs,
  }
}

export function resolveModelFieldRef(modelCode, fieldName, primary = false) {
  if (primary)
    return fieldName || ''
  return `${safeKey(modelCode || 'model')}__${fieldName || 'field'}`
}

export const LIST_PAGE_GRID_COLS = 12
export const LIST_PAGE_GRID_ROW_HEIGHT = 32
export const LIST_PAGE_GRID_GAP = 8

export const listPageBlockCatalog = [
  {
    blockType: 'search-form',
    group: 'data',
    title: '查询表单',
    desc: '查询字段集 + 查询/重置/收起',
    defaultW: 12,
    defaultH: 4,
    multiField: true,
    requireFields: true,
    unique: true,
  },
  {
    blockType: 'toolbar',
    group: 'action',
    title: '操作工具栏',
    desc: '新增 / 导入 / 导出 / 自定义查询',
    defaultW: 12,
    defaultH: 2,
    unique: true,
  },
  {
    blockType: 'data-table',
    group: 'data',
    title: '数据列表',
    desc: '配置展示列、排序、宽度',
    defaultW: 12,
    defaultH: 10,
    multiField: true,
    requireFields: true,
    unique: true,
  },
  {
    blockType: 'tree-panel',
    group: 'data',
    title: '左侧导航树',
    desc: '左树右表场景使用',
    defaultW: 3,
    defaultH: 14,
    unique: true,
    onlyFor: ['tree-crud'],
  },
  {
    blockType: 'stats-strip',
    group: 'extra',
    title: '指标卡片',
    desc: '顶部 KPI / 统计条',
    defaultW: 12,
    defaultH: 2,
  },
  {
    blockType: 'custom-html',
    group: 'extra',
    title: '说明文本',
    desc: '富文本 / Markdown 提示',
    defaultW: 6,
    defaultH: 3,
  },
  {
    blockType: 'sub-table-tabs',
    group: 'extra',
    title: '子表 Tab',
    desc: '关联模型分页签',
    defaultW: 12,
    defaultH: 8,
    onlyFor: ['master-detail-crud'],
  },
  {
    blockType: 'section-divider',
    group: 'extra',
    title: '分组标题',
    desc: '区块分隔与说明',
    defaultW: 12,
    defaultH: 1,
  },
]

export function resolveListPageBlockMeta(blockType) {
  return listPageBlockCatalog.find(item => item.blockType === blockType) || null
}

export function resolveTreeSourceRefs(modelSchema = {}) {
  const refs = Array.isArray(modelSchema.pageModelRefs) && modelSchema.pageModelRefs.length
    ? modelSchema.pageModelRefs
    : [createPageModelRef({ modelSchema }, { primary: true })]
  return refs.filter(ref => ref?.modelCode || ref?.primary)
}

export function resolveDefaultTreeConfig(modelSchema = {}, overrides = {}) {
  const sourceRef = resolveTreeSourceRef(modelSchema, overrides.sourceModelCode)
  const primaryRef = resolvePrimaryModelRef(modelSchema)
  const sourceFields = sourceRef?.fields || modelSchema.fields || []
  const modelTreeConfig = sourceRef?.primary ? (modelSchema.treeConfig || {}) : {}
  const keyField = overrides.keyField
    || modelTreeConfig.keyField
    || pickSourceField(sourceFields, ['id'])
    || 'id'
  const parentField = overrides.parentField
    || modelTreeConfig.parentField
    || pickSourceField(sourceFields, ['parentId', 'pid', 'parentCode'])
    || 'parentId'
  const labelField = overrides.labelField
    || modelTreeConfig.labelField
    || pickSourceField(sourceFields, ['name', 'title', 'label'])
    || firstBusinessSourceField(sourceFields, [keyField, parentField])
    || 'name'
  const relation = sourceRef?.primary ? null : findRelationToSource(primaryRef, sourceRef)
  const filterField = overrides.filterField
    || (sourceRef?.primary ? parentField : relation?.sourceField)
    || parentField
  const targetField = overrides.targetField
    || (sourceRef?.primary ? keyField : relation?.targetField)
    || keyField

  return {
    enabled: overrides.enabled ?? true,
    sourceModelCode: sourceRef?.modelCode || '',
    sourceModelName: sourceRef?.modelName || modelSchema.businessName || '',
    sourceTableName: sourceRef?.tableName || modelSchema.tableName || '',
    keyField,
    parentField,
    labelField,
    filterField,
    targetField,
    childrenField: overrides.childrenField || modelTreeConfig.childrenField || 'children',
    treeTitle: overrides.treeTitle || modelTreeConfig.treeTitle || `${sourceRef?.modelName || modelSchema.businessName || '业务'}树`,
    loadMode: overrides.loadMode || modelTreeConfig.loadMode || 'full',
  }
}

export function resolveTreeSourceRef(modelSchema = {}, sourceModelCode = '') {
  const refs = resolveTreeSourceRefs(modelSchema)
  if (!refs.length)
    return null
  if (sourceModelCode) {
    const matched = refs.find(ref => ref.modelCode === sourceModelCode)
    if (matched)
      return matched
  }
  return refs.find(ref => !ref.primary) || refs.find(ref => ref.primary) || refs[0]
}

export function resolveTreeFieldOptions(modelSchema = {}, sourceModelCode = '') {
  const sourceRef = resolveTreeSourceRef(modelSchema, sourceModelCode)
  const fields = sourceRef?.fields || modelSchema.fields || []
  return fields
    .filter(field => !isHiddenPageField(field))
    .map(field => ({
      label: (field.rawLabel || field.label)
        ? `${field.rawLabel || field.label}（${sourceFieldName(field)}）`
        : sourceFieldName(field),
      value: sourceFieldName(field),
    }))
}

function resolvePrimaryModelRef(modelSchema = {}) {
  const refs = resolveTreeSourceRefs(modelSchema)
  return refs.find(ref => ref.primary) || refs[0] || null
}

function sourceFieldName(field = {}) {
  return field?.sourceField || field?.field || ''
}

function pickSourceField(fields = [], names = []) {
  const field = fields.find((item) => {
    const sourceField = sourceFieldName(item)
    return names.includes(sourceField) || names.includes(item.field)
  })
  return sourceFieldName(field)
}

function firstBusinessSourceField(fields = [], excluded = []) {
  const excludedSet = new Set(excluded.filter(Boolean))
  const field = fields.find((item) => {
    const sourceField = sourceFieldName(item)
    return sourceField && !excludedSet.has(sourceField) && !isReadonlySystemField(item)
  }) || fields.find(item => sourceFieldName(item) && !excludedSet.has(sourceFieldName(item)))
  return sourceFieldName(field)
}

function findRelationToSource(primaryRef, sourceRef) {
  if (!primaryRef || !sourceRef?.modelCode)
    return null
  return (primaryRef.relations || []).find(relation => relation?.targetObjectCode === sourceRef.modelCode) || null
}

export function createDefaultListGridLayout(modelSchema, options = {}) {
  const fields = modelSchema?.fields || []
  const layoutType = options.layoutType || (modelSchema?.appType === 'TREE' ? 'tree-crud' : 'simple-crud')
  const isTree = layoutType === 'tree-crud'
  const items = []
  const mainX = isTree ? 3 : 0
  const mainW = isTree ? 9 : 12
  const treeConfig = isTree ? resolveDefaultTreeConfig(modelSchema) : null

  if (isTree) {
    items.push({
      id: 'block_tree',
      blockType: 'tree-panel',
      gridX: 0,
      gridY: 0,
      gridW: 3,
      gridH: 18,
      label: '导航树',
      props: {
        ...treeConfig,
      },
      fieldRefs: [],
    })
  }

  let yCursor = 0
  items.push({
    id: 'block_search',
    blockType: 'search-form',
    gridX: mainX,
    gridY: yCursor,
    gridW: mainW,
    gridH: 4,
    label: '查询表单',
    props: {
      fieldSettings: {},
      collapsible: true,
    },
    fieldRefs: filterPageFields(fields, 'search').map(f => f.field),
  })
  yCursor += 4

  items.push({
    id: 'block_toolbar',
    blockType: 'toolbar',
    gridX: mainX,
    gridY: yCursor,
    gridW: mainW,
    gridH: 2,
    label: '操作工具栏',
    props: {
      actions: ['add', 'import', 'export', 'custom-query'],
      customActions: [],
    },
    fieldRefs: [],
  })
  yCursor += 2

  items.push({
    id: 'block_table',
    blockType: 'data-table',
    gridX: mainX,
    gridY: yCursor,
    gridW: mainW,
    gridH: 10,
    label: '数据列表',
    props: {
      fieldSettings: {},
      defaultSortField: 'id',
      defaultSortOrder: 'desc',
    },
    fieldRefs: filterPageFields(fields, 'table').map(f => f.field),
  })

  return {
    cols: LIST_PAGE_GRID_COLS,
    rowHeight: LIST_PAGE_GRID_ROW_HEIGHT,
    gap: LIST_PAGE_GRID_GAP,
    layoutType,
    items,
  }
}

export function syncGridLayoutWithModel(layout, modelSchema, options = {}) {
  const tableFieldSet = new Set(filterPageFields(modelSchema?.fields || [], 'table').map(f => f.field))
  const searchFieldSet = new Set(filterPageFields(modelSchema?.fields || [], 'search').map(f => f.field))
  const layoutType = options.layoutType || layout?.layoutType || (modelSchema?.appType === 'TREE'
    ? 'tree-crud'
    : modelSchema?.appType === 'MASTER_DETAIL' ? 'master-detail-crud' : 'simple-crud')
  const fallback = createDefaultListGridLayout(modelSchema, { layoutType })
  const source = (layout?.items || []).length ? layout : fallback
  const modeChanged = Boolean(source.layoutType && source.layoutType !== layoutType)
  const sourceItems = (source.items || []).filter((item) => {
    const meta = resolveListPageBlockMeta(item.blockType) || {}
    return !meta.onlyFor || meta.onlyFor.includes(layoutType)
  }).map((item) => {
    const meta = resolveListPageBlockMeta(item.blockType) || {}
    const fieldSet = item.blockType === 'search-form' ? searchFieldSet : tableFieldSet
    const refs = (item.fieldRefs || []).filter(field => fieldSet.has(field))
    return {
      id: item.id || createBlockId(item.blockType),
      blockType: item.blockType,
      label: item.label || meta.title || item.blockType,
      gridX: clampNumber(item.gridX, 0, LIST_PAGE_GRID_COLS - 1),
      gridY: Math.max(0, Number(item.gridY) || 0),
      gridW: clampNumber(item.gridW, 1, LIST_PAGE_GRID_COLS),
      gridH: Math.max(resolveBlockMinGridH(item.blockType, meta), Number(item.gridH) || meta.defaultH || 2),
      props: item.blockType === 'tree-panel'
        ? resolveDefaultTreeConfig(modelSchema, item.props || {})
        : { ...(item.props || {}) },
      fieldRefs: refs,
    }
  })
  const items = normalizeGridItemsForLayout(sourceItems, modelSchema, layoutType, modeChanged)
  return {
    cols: Number(source.cols) || LIST_PAGE_GRID_COLS,
    rowHeight: Number(source.rowHeight) || LIST_PAGE_GRID_ROW_HEIGHT,
    gap: Number(source.gap) || LIST_PAGE_GRID_GAP,
    layoutType,
    items,
  }
}

function normalizeGridItemsForLayout(items, modelSchema, layoutType, modeChanged) {
  const next = [...items]
  const isTree = layoutType === 'tree-crud'
  const treeIndex = next.findIndex(item => item.blockType === 'tree-panel')
  const needsTreeInsert = isTree && treeIndex < 0
  const defaultLayout = (modeChanged || needsTreeInsert)
    ? createDefaultListGridLayout(modelSchema, { layoutType })
    : null

  if (needsTreeInsert) {
    const treeBlock = defaultLayout?.items?.find(item => item.blockType === 'tree-panel')
      || createGridBlock('tree-panel', modelSchema, { gridX: 0, gridY: 0 })
    if (treeBlock)
      next.unshift(treeBlock)
  }

  if (!modeChanged && !needsTreeInsert)
    return next

  const defaultByType = new Map((defaultLayout?.items || []).map(item => [item.blockType, item]))
  return next.map((item) => {
    if (!['search-form', 'toolbar', 'data-table', 'tree-panel'].includes(item.blockType))
      return item
    const defaultItem = defaultByType.get(item.blockType)
    if (!defaultItem)
      return item
    return {
      ...item,
      gridX: defaultItem.gridX,
      gridY: defaultItem.gridY,
      gridW: defaultItem.gridW,
      gridH: Math.max(item.gridH || 1, defaultItem.gridH || 1),
      props: item.blockType === 'tree-panel'
        ? { ...defaultItem.props, ...(item.props || {}) }
        : item.props,
    }
  })
}

export function bootstrapGridLayoutFromZones(zones, modelSchema, options = {}) {
  const layoutType = options.layoutType || (modelSchema?.appType === 'TREE' ? 'tree-crud' : 'simple-crud')
  const isTree = layoutType === 'tree-crud'
  const search = (zones || []).find(z => z.zoneKey === 'search')
  const table = (zones || []).find(z => z.zoneKey === 'table')
  const items = []
  const mainX = isTree ? 3 : 0
  const mainW = isTree ? 9 : 12

  if (isTree) {
    const treeConfig = table?.props?.treeConfig || {}
    const defaultTreeConfig = resolveDefaultTreeConfig(modelSchema, treeConfig)
    items.push({
      id: 'block_tree',
      blockType: 'tree-panel',
      gridX: 0,
      gridY: 0,
      gridW: 3,
      gridH: 18,
      label: '导航树',
      props: {
        ...defaultTreeConfig,
      },
      fieldRefs: [],
    })
  }

  let yCursor = 0
  if (search?.enabled !== false) {
    items.push({
      id: 'block_search',
      blockType: 'search-form',
      gridX: mainX,
      gridY: yCursor,
      gridW: mainW,
      gridH: 4,
      label: '查询表单',
      props: {
        fieldSettings: search?.props?.fieldSettings || {},
        collapsible: true,
      },
      fieldRefs: search?.fieldRefs || [],
    })
    yCursor += 4
  }

  const tableActions = []
  if (table?.props?.showImport)
    tableActions.push('import')
  if (table?.props?.showExport)
    tableActions.push('export')
  if (table?.props?.enableCustomQuery !== false)
    tableActions.push('custom-query')
  tableActions.unshift('add')

  items.push({
    id: 'block_toolbar',
    blockType: 'toolbar',
    gridX: mainX,
    gridY: yCursor,
    gridW: mainW,
    gridH: 2,
    label: '操作工具栏',
    props: {
      actions: Array.from(new Set(tableActions)),
      customActions: table?.props?.customActions || [],
    },
    fieldRefs: [],
  })
  yCursor += 2

  if (table?.enabled !== false) {
    items.push({
      id: 'block_table',
      blockType: 'data-table',
      gridX: mainX,
      gridY: yCursor,
      gridW: mainW,
      gridH: 10,
      label: '数据列表',
      props: {
        fieldSettings: table?.props?.fieldSettings || {},
        defaultSortField: table?.props?.defaultSortField || 'id',
        defaultSortOrder: table?.props?.defaultSortOrder || 'desc',
      },
      fieldRefs: table?.fieldRefs || [],
    })
  }

  return {
    cols: LIST_PAGE_GRID_COLS,
    rowHeight: LIST_PAGE_GRID_ROW_HEIGHT,
    gap: LIST_PAGE_GRID_GAP,
    layoutType,
    items,
  }
}

export function applyGridLayoutToZones(zones, gridLayout, modelSchema) {
  const items = gridLayout?.items || []
  const search = items.find(i => i.blockType === 'search-form')
  const table = items.find(i => i.blockType === 'data-table')
  const tree = items.find(i => i.blockType === 'tree-panel')
  const toolbar = items.find(i => i.blockType === 'toolbar')
  const searchFieldSet = new Set(filterPageFields(modelSchema?.fields || [], 'search').map(f => f.field))
  const tableFieldSet = new Set(filterPageFields(modelSchema?.fields || [], 'table').map(f => f.field))

  return (zones || []).map((zone) => {
    if (zone.zoneKey === 'search') {
      const refs = (search?.fieldRefs || []).filter(ref => searchFieldSet.has(ref))
      return {
        ...zone,
        enabled: !!search,
        fieldRefs: refs,
        props: {
          ...(zone.props || {}),
          fieldSettings: search?.props?.fieldSettings || zone.props?.fieldSettings || {},
        },
      }
    }
    if (zone.zoneKey === 'table') {
      const refs = (table?.fieldRefs || []).filter(ref => tableFieldSet.has(ref))
      const actions = toolbar?.props?.actions || []
      const nextProps = {
        ...(zone.props || {}),
        fieldSettings: table?.props?.fieldSettings || zone.props?.fieldSettings || {},
        showImport: actions.includes('import'),
        showExport: actions.includes('export'),
        hideBatchDelete: !actions.includes('batch-delete'),
        enableCustomQuery: actions.includes('custom-query'),
        customActions: toolbar?.props?.customActions || zone.props?.customActions || [],
        defaultSortField: table?.props?.defaultSortField || zone.props?.defaultSortField || 'id',
        defaultSortOrder: table?.props?.defaultSortOrder || zone.props?.defaultSortOrder || 'desc',
      }
      if (tree) {
        nextProps.treeConfig = {
          ...(zone.props?.treeConfig || {}),
          enabled: tree.props?.enabled ?? zone.props?.treeConfig?.enabled ?? true,
          sourceModelCode: tree.props?.sourceModelCode || zone.props?.treeConfig?.sourceModelCode || '',
          sourceModelName: tree.props?.sourceModelName || zone.props?.treeConfig?.sourceModelName || '',
          sourceTableName: tree.props?.sourceTableName || zone.props?.treeConfig?.sourceTableName || '',
          treeTitle: tree.props?.treeTitle || zone.props?.treeConfig?.treeTitle,
          keyField: tree.props?.keyField || zone.props?.treeConfig?.keyField || 'id',
          parentField: tree.props?.parentField || zone.props?.treeConfig?.parentField || 'parentId',
          labelField: tree.props?.labelField || zone.props?.treeConfig?.labelField || '',
          filterField: tree.props?.filterField || zone.props?.treeConfig?.filterField || '',
          targetField: tree.props?.targetField || zone.props?.treeConfig?.targetField || '',
          childrenField: tree.props?.childrenField || zone.props?.treeConfig?.childrenField || 'children',
          loadMode: tree.props?.loadMode || zone.props?.treeConfig?.loadMode || 'full',
        }
      }
      return {
        ...zone,
        enabled: !!table,
        fieldRefs: refs,
        props: nextProps,
      }
    }
    return zone
  })
}

export function ensureGridBlockId(blockType, existingIds = new Set()) {
  let id = createBlockId(blockType)
  while (existingIds.has(id))
    id = createBlockId(blockType)
  return id
}

export function createGridBlock(blockType, modelSchema, position = {}) {
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return null
  const fields = modelSchema?.fields || []
  const base = {
    id: createBlockId(blockType),
    blockType,
    label: meta.title,
    gridX: clampNumber(position.gridX ?? 0, 0, LIST_PAGE_GRID_COLS - 1),
    gridY: Math.max(0, Number(position.gridY) || 0),
    gridW: Math.min(meta.defaultW || 6, LIST_PAGE_GRID_COLS),
    gridH: meta.defaultH || 2,
    props: {},
    fieldRefs: [],
  }
  if (blockType === 'search-form') {
    base.fieldRefs = filterPageFields(fields, 'search').slice(0, 8).map(f => f.field)
    base.props = { fieldSettings: {}, collapsible: true }
  }
  if (blockType === 'data-table') {
    base.fieldRefs = filterPageFields(fields, 'table').map(f => f.field)
    base.props = { fieldSettings: {}, defaultSortField: 'id', defaultSortOrder: 'desc' }
  }
  if (blockType === 'toolbar') {
    base.props = { actions: ['add', 'import', 'export', 'custom-query'], customActions: [] }
  }
  if (blockType === 'tree-panel') {
    base.props = resolveDefaultTreeConfig(modelSchema)
  }
  if (blockType === 'stats-strip') {
    base.props = {
      metrics: [
        { label: '总数', value: '128', trend: '+8%' },
        { label: '活跃', value: '92', trend: '+3%' },
        { label: '本月新增', value: '21', trend: '+12%' },
        { label: '异常', value: '3', trend: '-1' },
      ],
    }
  }
  if (blockType === 'custom-html') {
    base.props = {
      title: '说明',
      content: '在此填写业务说明、操作指引或链接。',
    }
  }
  if (blockType === 'sub-table-tabs') {
    base.props = {
      tabs: [
        { key: 'detail', title: '基本信息' },
        { key: 'related', title: '关联记录' },
      ],
    }
  }
  if (blockType === 'section-divider') {
    base.props = { title: '分组标题' }
  }
  return base
}

function createBlockId(blockType) {
  return `${safeKey(blockType || 'block')}_${Math.random().toString(36).slice(2, 8)}`
}

function clampNumber(value, min, max) {
  const num = Number(value)
  if (!Number.isFinite(num))
    return min
  return Math.min(Math.max(num, min), max)
}

function clonePlain(value) {
  return JSON.parse(JSON.stringify(value ?? null))
}

function resolveBlockMinGridH(blockType, meta = {}) {
  if (blockType === 'toolbar')
    return Math.max(2, Number(meta.defaultH) || 2)
  return 1
}

export function resolveZoneTitle(zoneKey) {
  return pageZoneCatalog.find(item => item.zoneKey === zoneKey)?.title || zoneKey
}

export function resolveCanvasComponent(componentKey) {
  return canvasComponentCatalog.find(item => item.componentKey === componentKey) || null
}

export function resolveDefaultFieldComponentKey(field) {
  if (field?.componentType === 'textarea')
    return 'field-textarea'
  if (field?.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field?.dataType))
    return 'field-number'
  if (field?.componentType === 'date' || field?.dataType === 'date')
    return 'field-date'
  if (field?.componentType === 'datetime' || field?.dataType === 'datetime')
    return 'field-datetime'
  if (field?.componentType === 'switch')
    return 'field-switch'
  if (field?.componentType === 'fileUpload')
    return 'field-upload'
  if (field?.componentType === 'imageUpload')
    return 'field-image-upload'
  if (field?.componentType === 'dictSelect')
    return 'field-dict-select'
  if (field?.componentType === 'orgTreeSelect')
    return 'field-org-tree-select'
  if (field?.componentType === 'userSelect')
    return 'field-user-select'
  if (field?.componentType === 'regionTreeSelect')
    return 'field-region-tree-select'
  if (field?.componentType === 'treeSelect')
    return 'field-tree-select'
  if (field?.componentType === 'cascader')
    return 'field-cascader'
  if (field?.dictType || ['select', 'radio', 'checkbox'].includes(field?.componentType))
    return 'field-select'
  return 'field-input'
}

export function resolveComponentTypeFromComponentKey(componentKey, fallback = 'input') {
  const typeMap = {
    'field-input': 'input',
    'field-textarea': 'textarea',
    'field-number': 'number',
    'field-select': 'select',
    'field-dict-select': 'dictSelect',
    'field-tree-select': 'treeSelect',
    'field-org-tree-select': 'orgTreeSelect',
    'field-user-select': 'userSelect',
    'field-region-tree-select': 'regionTreeSelect',
    'field-cascader': 'cascader',
    'field-date': 'date',
    'field-datetime': 'datetime',
    'field-switch': 'switch',
    'field-upload': 'fileUpload',
    'field-image-upload': 'imageUpload',
    'detail-field': fallback,
  }
  return typeMap[componentKey] || fallback
}

export function createCanvasItem(payload = {}, context = {}) {
  const zoneKey = context.zoneKey || payload.zoneKey || 'edit'
  const fieldMap = new Map((context.fields || []).map(field => [field.field, field]))
  const field = payload.field || fieldMap.get(payload.fieldRef)
  const componentKey = payload.componentKey || resolveDefaultFieldComponentKey(field, zoneKey)
  const component = resolveCanvasComponent(componentKey)
  const width = Number(payload.w || payload.width || component?.defaultWidth || 280)
  const height = Number(payload.h || payload.height || component?.defaultHeight || 64)
  const fieldRefs = normalizeFieldRefs(payload.fieldRefs || payload.props?.fieldRefs || [])

  return {
    id: payload.id || createItemId(zoneKey, componentKey, field?.field),
    componentKey,
    label: payload.label || field?.label || component?.title || '组件',
    fieldRef: payload.fieldRef || field?.field || '',
    modelCode: payload.modelCode || field?.modelCode || '',
    modelName: payload.modelName || field?.modelName || '',
    fieldRefs,
    x: Number(payload.x ?? context.x ?? 32),
    y: Number(payload.y ?? context.y ?? 32),
    w: width,
    h: height,
    zIndex: Number(payload.zIndex ?? context.zIndex ?? 1),
    locked: !!payload.locked,
    style: {
      labelWidth: 86,
      radius: 6,
      fill: '#ffffff',
      stroke: '#cbd5e1',
      ...payload.style,
    },
    props: {
      placeholder: field?.label ? `请输入${field.label}` : '',
      ...payload.props,
      ...(fieldRefs.length ? { fieldRefs } : {}),
    },
  }
}

export function normalizeZoneCanvas(zone, modelSchema) {
  const fields = modelSchema?.fields || []
  const oldCanvas = zone?.props?.canvas || {}
  const defaultCanvas = createDefaultCanvasForZone(zone?.zoneKey, fields, zone?.fieldRefs, modelSchema)
  const sourceItems = Array.isArray(oldCanvas.items) && oldCanvas.items.length
    ? oldCanvas.items
    : defaultCanvas.items
  const fieldSet = new Set(filterPageFields(fields, zone?.zoneKey)
    .filter(field => isCanvasFieldVisible(field, zone?.zoneKey))
    .map(field => field.field))
  let items = sourceItems
    .map(item => normalizeCanvasItem(item, zone?.zoneKey, fields))
    .filter(item => zone?.zoneKey !== 'edit' || !['save-button', 'reset-button'].includes(item.componentKey))
    .filter(item => !item.fieldRef || fieldSet.has(item.fieldRef))
    .map((item) => {
      const refs = normalizeFieldRefs(item.fieldRefs || item.props?.fieldRefs || []).filter(ref => fieldSet.has(ref))
      const props = { ...(item.props || {}) }
      if (item.fieldRefs?.length || item.props?.fieldRefs)
        props.fieldRefs = refs
      return {
        ...item,
        fieldRefs: refs,
        props,
      }
    })

  if (['edit', 'detail'].includes(zone?.zoneKey)) {
    const explicitRefs = normalizeFieldRefs(zone?.fieldRefs || []).filter(ref => fieldSet.has(ref))
    const usedRefs = new Set(resolveFieldRefsFromCanvas({ items }))
    const missingRefs = explicitRefs.filter(ref => !usedRefs.has(ref))
    if (missingRefs.length) {
      const fieldMap = new Map(fields.map(field => [field.field, field]))
      const startIndex = items.filter(item => item.fieldRef).length
      const missingItems = missingRefs
        .map(ref => fieldMap.get(ref))
        .filter(Boolean)
        .map((field, index) => createDefaultFieldCanvasItem(field, zone?.zoneKey, startIndex + index, fields))
      items = [...items, ...missingItems]
    }
  }

  return {
    width: Number(oldCanvas.width || defaultCanvas.width),
    height: Number(oldCanvas.height || defaultCanvas.height),
    snap: Number(oldCanvas.snap || 8),
    items,
  }
}

export function resolveFieldRefsFromCanvas(canvas) {
  const refs = []
  sortCanvasItemsByPosition(canvas?.items || []).forEach((item) => {
    if (item.fieldRef)
      refs.push(item.fieldRef)
    refs.push(...normalizeFieldRefs(item.fieldRefs || item.props?.fieldRefs || []))
  })
  return Array.from(new Set(refs))
}

export function patchZoneCanvas(zone, canvasPatch = {}) {
  const canvas = {
    ...(zone?.props?.canvas || {}),
    ...canvasPatch,
  }
  const syncedProps = syncZoneBusinessProps(zone, canvas)
  const props = {
    ...(zone?.props || {}),
    ...syncedProps,
    canvas,
  }
  if (zone?.zoneKey === 'edit') {
    delete props.formCreateRule
    delete props.formCreateOptions
  }
  return {
    ...zone,
    fieldRefs: resolveFieldRefsFromCanvas(canvas),
    props,
  }
}

export function resolveFieldRefsFromFormCreateRules(rules, fieldSet) {
  const refs = []
  const modelFields = fieldSet instanceof Set ? fieldSet : new Set(fieldSet || [])
  const walk = (items) => {
    ;(Array.isArray(items) ? items : []).forEach((rule) => {
      if (!rule || typeof rule !== 'object')
        return
      if (rule.field && modelFields.has(rule.field) && !refs.includes(rule.field))
        refs.push(rule.field)
      if (Array.isArray(rule.children))
        walk(rule.children)
    })
  }
  walk(rules)
  return refs
}

function createDefaultCanvasForZone(zoneKey, allFields, fieldRefs = [], modelSchema) {
  const fields = resolveDefaultFields(zoneKey, allFields, fieldRefs)
  if (zoneKey === 'table') {
    const tableRefs = fields.map(field => field.field)
    return {
      width: 1040,
      height: 460,
      snap: 8,
      items: [
        createCanvasItem({
          id: 'table_action_add',
          componentKey: 'add-button',
          label: '新增',
          x: 32,
          y: 28,
          zIndex: 1,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_action_import',
          componentKey: 'import-button',
          label: '导入',
          x: 148,
          y: 28,
          zIndex: 2,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_action_export',
          componentKey: 'export-button',
          label: '导出',
          x: 264,
          y: 28,
          zIndex: 3,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_action_custom_query',
          componentKey: 'custom-query',
          label: '自定义查询',
          x: 380,
          y: 28,
          zIndex: 4,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'table_data_grid',
          componentKey: 'data-table',
          label: `${modelSchema?.businessName || '业务'}列表`,
          fieldRefs: tableRefs,
          props: { fieldRefs: tableRefs },
          x: 32,
          y: 88,
          w: 940,
          h: 300,
          zIndex: 5,
        }, { zoneKey, fields: allFields }),
      ],
    }
  }

  if (zoneKey === 'search') {
    const searchRefs = fields.map(field => field.field)
    return {
      width: 1040,
      height: 300,
      snap: 8,
      items: [
        createCanvasItem({
          id: 'search_query_set',
          componentKey: 'query-set',
          label: '查询集',
          fieldRefs: searchRefs,
          props: { fieldRefs: searchRefs },
          x: 32,
          y: 36,
          w: 700,
          h: 132,
          zIndex: 1,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'search_action_reset',
          componentKey: 'reset-button',
          label: '重置',
          x: 760,
          y: 82,
          zIndex: 2,
        }, { zoneKey, fields: allFields }),
        createCanvasItem({
          id: 'search_action_custom_query',
          componentKey: 'custom-query',
          label: '自定义查询',
          x: 876,
          y: 82,
          zIndex: 3,
        }, { zoneKey, fields: allFields }),
      ],
    }
  }

  const items = fields.map((field, index) => {
    return createDefaultFieldCanvasItem(field, zoneKey, index, allFields)
  })

  return {
    width: 1040,
    height: Math.max(420, Math.max(...items.map(item => item.y + item.h), 0) + 48),
    snap: 8,
    items,
  }
}

function createDefaultFieldCanvasItem(field, zoneKey, index, allFields) {
  const componentKey = resolveDefaultFieldComponentKey(field, zoneKey)
  const compactFormZone = ['edit', 'detail'].includes(zoneKey)
  const colCount = compactFormZone ? 2 : 3
  const col = index % colCount
  const row = Math.floor(index / colCount)
  const width = compactFormZone
    ? 340
    : componentKey === 'field-textarea' ? 580 : 280
  const height = compactFormZone
    ? (componentKey === 'field-textarea' || componentKey === 'field-upload' || componentKey === 'field-image-upload' ? 76 : 64)
    : componentKey === 'field-textarea' ? 98 : componentKey === 'field-upload' || componentKey === 'field-image-upload' ? 88 : 64
  const gapX = compactFormZone ? 24 : 28
  const rowStep = compactFormZone ? 88 : height + (zoneKey === 'detail' ? 10 : 22)
  const x = compactFormZone
    ? 32 + col * (width + gapX)
    : 32 + col * 308
  const y = 32 + row * rowStep
  return createCanvasItem({
    id: `${zoneKey}_${safeKey(field.field)}`,
    componentKey,
    label: field.label || field.field,
    fieldRef: field.field,
    x,
    y,
    w: width,
    h: height,
    zIndex: index + 1,
  }, { zoneKey, fields: allFields })
}

function resolveDefaultFields(zoneKey, fields, fieldRefs) {
  const refSet = new Set(fieldRefs || [])
  const visibleFields = (source = []) => source.filter(field => isCanvasFieldVisible(field, zoneKey))
  if (refSet.size)
    return visibleFields(fields.filter(field => refSet.has(field.field) && isPageFieldVisible(field, zoneKey)))
  if (zoneKey === 'search')
    return visibleFields(filterPageFields(fields, 'search'))
  if (zoneKey === 'table')
    return visibleFields(filterPageFields(fields, 'table'))
  return visibleFields(filterPageFields(fields, zoneKey))
}

function isCanvasFieldVisible(field, zoneKey) {
  return !(['edit', 'detail'].includes(zoneKey) && isChildPageModelField(field))
}

function normalizeCanvasItem(item, zoneKey, fields) {
  return createCanvasItem({
    ...item,
    id: item.id,
  }, {
    zoneKey,
    fields,
    x: item.x,
    y: item.y,
    zIndex: item.zIndex,
  })
}

function normalizeFieldRefs(refs) {
  return Array.from(new Set((Array.isArray(refs) ? refs : []).filter(Boolean)))
}

function syncZoneBusinessProps(zone, canvas) {
  const componentKeys = new Set((canvas?.items || []).map(item => item.componentKey))
  if (zone?.zoneKey === 'table') {
    return {
      showImport: componentKeys.has('import-button'),
      showExport: componentKeys.has('export-button'),
      enableCustomQuery: componentKeys.has('custom-query'),
    }
  }
  if (zone?.zoneKey === 'edit' || zone?.zoneKey === 'search' || zone?.zoneKey === 'detail') {
    const fieldSettings = {}
    const gridCols = resolveCanvasGridCols(canvas)
    ;(canvas?.items || []).forEach((item) => {
      if (!item.fieldRef)
        return
      const componentType = resolveComponentTypeFromComponentKey(item.componentKey, '')
      if (!componentType)
        return
      fieldSettings[item.fieldRef] = {
        ...(zone.props?.fieldSettings?.[item.fieldRef] || {}),
        componentType,
        ...resolveCanvasItemLayoutSetting(item, canvas, gridCols),
      }
      if (item.props?.placeholder) {
        fieldSettings[item.fieldRef].props = {
          ...(fieldSettings[item.fieldRef].props || {}),
          placeholder: item.props.placeholder,
        }
      }
    })
    return {
      fieldSettings,
      ...(zone?.zoneKey === 'edit' ? { editGridCols: gridCols } : {}),
      ...(zone?.zoneKey === 'detail' ? { detailGridCols: gridCols } : {}),
    }
  }
  return {}
}

function resolveCanvasGridCols(canvas) {
  const fieldItems = (canvas?.items || [])
    .filter(item => item.fieldRef)
    .sort((a, b) => Number(a.x || 0) - Number(b.x || 0))
  const columns = []
  fieldItems.forEach((item) => {
    const x = Number(item.x || 0)
    if (!columns.some(colX => Math.abs(colX - x) < 80))
      columns.push(x)
  })
  return Math.max(1, Math.min(3, columns.length || 1))
}

function sortCanvasItemsByPosition(items = []) {
  return [...items].sort((a, b) => {
    const rowA = Math.round(Number(a.y || 0) / 16)
    const rowB = Math.round(Number(b.y || 0) / 16)
    if (rowA !== rowB)
      return rowA - rowB
    const xDiff = Number(a.x || 0) - Number(b.x || 0)
    if (xDiff !== 0)
      return xDiff
    return Number(a.zIndex || 0) - Number(b.zIndex || 0)
  })
}

function resolveCanvasItemLayoutSetting(item, canvas, gridCols) {
  const canvasWidth = Number(canvas?.width || 1040)
  const colWidth = Math.max(1, (canvasWidth - 64) / Math.max(1, gridCols))
  const itemWidth = Number(item.w || 280)
  const span = Math.max(1, Math.min(gridCols, Math.round(itemWidth / colWidth) || 1))
  return {
    span,
    labelWidth: item.style?.labelWidth || 86,
  }
}

function createItemId(zoneKey, componentKey, fieldRef) {
  if (fieldRef)
    return `${zoneKey}_${safeKey(fieldRef)}`
  const suffix = Math.random().toString(36).slice(2, 8)
  return `${zoneKey}_${safeKey(componentKey)}_${Date.now()}_${suffix}`
}

function isNumericId(value) {
  return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value))
}

function safeKey(value) {
  return String(value || 'item').replace(/[^\w-]/g, '_')
}
