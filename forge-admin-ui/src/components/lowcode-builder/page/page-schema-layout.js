import { toCanvasComponentCatalog as _toCanvasComponentCatalog } from '@/components/lowcode-builder/designer-core'
import { createPageWidgetDefaultProps, createWidgetDataBinding, pageWidgetCatalog } from '@/components/lowcode-builder/shared/page-widget-schema'
import { safeKey } from './page-schema-utils.js'

import {
  pageZoneCatalog,
  canvasComponentCatalog,
  isPageFieldVisible,
  isListFieldSelectable,
  filterPageFields,
  filterDefaultListFields,
  isChildPageModelField,
} from './page-schema-fields.js'
import {
  LIST_PAGE_GRID_COLS,
  LIST_PAGE_GRID_ROW_HEIGHT,
  LIST_PAGE_GRID_GAP,
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_BASE_COL_WIDTH,
  resolveListPageBlockMeta,
  resolveDefaultTreeConfig,
  normalizeTreeConfigAliases,
} from './page-schema-catalog.js'

export function createDefaultListGridLayout(modelSchema, options = {}) {
  const fields = modelSchema?.fields || []
  const layoutType = options.layoutType || (modelSchema?.appType === 'TREE' ? 'tree-crud' : 'simple-crud')
  const isTree = layoutType === 'tree-crud'
  const items = []
  const mainX = isTree ? 3 : 0
  const mainW = isTree ? 9 : 12
  const treeConfig = isTree
    ? {
        ...resolveDefaultTreeConfig(modelSchema),
        sourceModelCode: '',
        sourceModelName: '',
        sourceTableName: '',
        sourceConfigKey: '',
        treeApi: '',
        treeTitle: '',
      }
    : null

  if (isTree) {
    items.push({
      id: 'block_tree',
      blockType: 'tree-panel',
      gridX: 0,
      gridY: 0,
      gridW: 3,
      gridH: 18,
      label: '筛选树',
      props: {
        style: createDefaultBlockFrameStyle(0, 0, 3, 18, 'fixed'),
        events: [],
        ...treeConfig,
      },
      fieldRefs: [],
    })
  }

  items.push({
    id: 'block_crud',
    blockType: 'AiCrudPage',
    gridX: mainX,
    gridY: 0,
    gridW: mainW,
    gridH: 16,
    label: '业务列表',
    props: {
      ...createDefaultAiCrudPageProps(),
      title: modelSchema?.businessName || modelSchema?.object?.name || '业务列表',
      style: createDefaultBlockFrameStyle(mainX, 0, mainW, 16),
      events: [],
      fieldSettings: {},
      searchFieldRefs: filterPageFields(fields, 'search').map(f => f.field),
      searchFieldSettings: {},
      defaultSortField: 'id',
      defaultSortOrder: 'desc',
    },
    fieldRefs: filterDefaultListFields(fields).map(f => f.field),
  })

  return {
    cols: LIST_PAGE_GRID_COLS,
    rowHeight: LIST_PAGE_GRID_ROW_HEIGHT,
    gap: LIST_PAGE_GRID_GAP,
    designWidth: LIST_PAGE_DESIGN_WIDTH,
    layoutType,
    items,
  }
}

export function syncGridLayoutWithModel(layout, modelSchema, options = {}) {
  const tableFieldSet = new Set((modelSchema?.fields || [])
    .filter(field => isListFieldSelectable(field, 'table'))
    .map(f => f.field))
  const searchFieldSet = new Set(filterPageFields(modelSchema?.fields || [], 'search').map(f => f.field))
  // 优先用显式 layoutType；appType=TREE 只表示树形数据，不自动切左树右表。
  const layoutType = options.layoutType
    || layout?.layoutType
    || (modelSchema?.appType === 'MASTER_DETAIL' ? 'master-detail-crud' : 'simple-crud')
  const fallback = createDefaultListGridLayout(modelSchema, { layoutType })
  const hasExplicitItems = Array.isArray(layout?.items)
  const source = hasExplicitItems ? layout : fallback
  const modeChanged = Boolean(source.layoutType && source.layoutType !== layoutType)
  const sourceItems = (source.items || []).filter((item) => {
    const meta = resolveListPageBlockMeta(item.blockType) || {}
    return !meta.onlyFor || meta.onlyFor.includes(layoutType)
  }).map((item) => {
    const meta = resolveListPageBlockMeta(item.blockType) || {}
    const fieldSet = item.blockType === 'search-form' ? searchFieldSet : tableFieldSet
    const refs = (item.fieldRefs || []).filter(field => fieldSet.has(field))
    let props = item.blockType === 'tree-panel'
      ? {
          ...sanitizeGridBlockProps(item.blockType, item.props || {}, new Set(refs), searchFieldSet),
          ...resolveDefaultTreeConfig(modelSchema, item.props || {}),
        }
      : sanitizeGridBlockProps(item.blockType, item.props || {}, new Set(refs), searchFieldSet)
    if (item.blockType === 'AiCrudPage') {
      const hasSearchRefs = Object.prototype.hasOwnProperty.call(item.props || {}, 'searchFieldRefs')
      if (hasSearchRefs) {
        const searchRefs = Array.isArray(item.props?.searchFieldRefs)
          ? item.props.searchFieldRefs.filter(field => searchFieldSet.has(field))
          : []
        props = {
          ...props,
          searchFieldRefs: searchRefs,
          searchFieldSettings: sanitizeFieldSettings(item.props?.searchFieldSettings, new Set(searchRefs), searchFieldSet),
        }
      }
      else {
        // 旧区块补齐显式查询配置：默认取模型 searchable，避免「查询」开关误跟列表列
        const defaultSearchRefs = filterPageFields(modelSchema?.fields || [], 'search')
          .filter(field => field.searchable === true)
          .map(field => field.field)
          .filter(field => searchFieldSet.has(field))
        props = {
          ...props,
          searchFieldRefs: defaultSearchRefs,
          searchFieldSettings: sanitizeFieldSettings({}, new Set(defaultSearchRefs), searchFieldSet),
        }
      }
      // 左树右表：右表未开本表嵌入式树时清掉残留「添加下级」；已启用则跟随 model
      if (layoutType === 'tree-crud') {
        const embeddedOn = modelSchema?.treeConfig?.enabled === true || modelSchema?.appType === 'TREE'
        props = {
          ...props,
          enableTreeAddChild: embeddedOn
            ? modelSchema?.treeConfig?.enableTreeAddChild === true
            : false,
        }
      }
    }
    if (item.blockType === 'grid-layout') {
      props = {
        ...props,
        cells: sanitizeGridCells(props.cells || [], modelSchema, layoutType),
      }
    }
    return {
      id: item.id || createBlockId(item.blockType),
      blockType: item.blockType,
      label: item.label || meta.title || item.blockType,
      gridX: clampNumber(item.gridX, 0, LIST_PAGE_GRID_COLS - 1),
      gridY: Math.max(0, Number(item.gridY) || 0),
      gridW: clampNumber(item.gridW, 1, LIST_PAGE_GRID_COLS),
      gridH: Math.max(resolveBlockMinGridH(item.blockType, meta), Number(item.gridH) || meta.defaultH || 2),
      props,
      fieldRefs: refs,
      children: sanitizeContainerChildren(item.children || [], modelSchema, layoutType),
    }
  })
  const preserveEmpty = hasExplicitItems && !sourceItems.length
  const items = normalizeGridItemsForLayout(sourceItems, modelSchema, layoutType, modeChanged, preserveEmpty, hasExplicitItems, source.designWidth || LIST_PAGE_DESIGN_WIDTH)
  return {
    cols: Number(source.cols) || LIST_PAGE_GRID_COLS,
    rowHeight: Number(source.rowHeight) || LIST_PAGE_GRID_ROW_HEIGHT,
    gap: Number(source.gap) || LIST_PAGE_GRID_GAP,
    designWidth: clampNumber(source.designWidth || LIST_PAGE_DESIGN_WIDTH, 960, 2560),
    layoutType,
    items,
  }
}

/**
 * 列表设计器的页面模型需要保持稳定，当前区块字段目录则可能来自页面表单、
 * 业务对象草稿或已发布运行配置。字段清洗必须以本次实际展示的字段为准，
 * 不能因为稳定模型没有携带 fields 就清空用户刚选择的字段引用。
 */
export function buildGridSyncModelSchema(modelSchema = {}, fields = []) {
  return {
    ...(modelSchema || {}),
    fields: Array.isArray(fields) ? fields : [],
  }
}

function normalizeGridItemsForLayout(items, modelSchema, layoutType, modeChanged, preserveEmpty = false, hasExplicitItems = false, designWidth = LIST_PAGE_DESIGN_WIDTH) {
  const next = [...items]
  const isTree = layoutType === 'tree-crud'
  const treeIndex = next.findIndex(item => item.blockType === 'tree-panel')
  const needsTreeInsert = !preserveEmpty && !hasExplicitItems && isTree && treeIndex < 0
  const needsTreeRepair = !preserveEmpty && isTree && hasTreePanelStructureRisk(next)
  const defaultLayout = (modeChanged || needsTreeInsert || needsTreeRepair)
    ? createDefaultListGridLayout(modelSchema, { layoutType })
    : null

  if (needsTreeInsert) {
    const treeBlock = defaultLayout?.items?.find(item => item.blockType === 'tree-panel')
      || createGridBlock('tree-panel', modelSchema, { gridX: 0, gridY: 0 })
    if (treeBlock)
      next.unshift(treeBlock)
  }

  if (!modeChanged && !needsTreeInsert && !needsTreeRepair)
    return alignTreeLayoutMainBlocks(next, layoutType, designWidth)

  const defaultByType = new Map((defaultLayout?.items || []).map(item => [item.blockType, item]))
  const repaired = next.map((item) => {
    const repairableTypes = new Set(['search-form', 'toolbar', 'data-table', 'tree-panel', 'AiCrudPage', 'AiTable'])
    const mainTypes = new Set(['search-form', 'toolbar', 'data-table', 'AiCrudPage', 'AiTable'])
    if (!repairableTypes.has(item.blockType))
      return item
    const defaultItem = defaultByType.get(item.blockType)
    if (!defaultItem && mainTypes.has(item.blockType)) {
      return {
        ...item,
        gridX: 3,
        gridW: Math.min(9, Math.max(1, Number(item.gridW || 9))),
        props: {
          ...(item.props || {}),
          style: createDefaultBlockFrameStyle(3, Number(item.gridY || 0), 9, Number(item.gridH || 2)),
        },
      }
    }
    if (!defaultItem)
      return item
    return {
      ...item,
      gridX: defaultItem.gridX,
      gridY: defaultItem.gridY,
      gridW: defaultItem.gridW,
      gridH: Math.max(item.gridH || 1, defaultItem.gridH || 1),
      props: item.blockType === 'tree-panel'
        ? {
            ...defaultItem.props,
            ...(item.props || {}),
            style: defaultItem.props?.style || item.props?.style,
          }
        : ['AiCrudPage', 'AiTable', 'data-table', 'search-form', 'toolbar'].includes(item.blockType)
            ? { ...(item.props || {}), style: defaultItem.props?.style || item.props?.style }
            : item.props,
    }
  })
  return alignTreeLayoutMainBlocks(repaired, layoutType, designWidth)
}

function hasTreePanelStructureRisk(items = []) {
  const tree = items.find(item => item.blockType === 'tree-panel')
  const treeStyle = tree?.props?.style || {}
  return !!tree && (tree.gridX !== 0 || treeStyle.widthMode !== 'fixed')
}

function alignTreeLayoutMainBlocks(items = [], layoutType = 'simple-crud', designWidth = LIST_PAGE_DESIGN_WIDTH) {
  if (layoutType !== 'tree-crud')
    return items
  const tree = items.find(item => item.blockType === 'tree-panel')
  if (!tree)
    return items
  const treeFrame = resolveSchemaItemFrame(tree, designWidth)
  const mainX = Math.min(
    Math.max(0, Math.round(treeFrame.x + treeFrame.width + LIST_PAGE_GRID_GAP)),
    Math.max(0, designWidth - 24),
  )
  return items.map((item) => {
    if (!isTreeMainBlock(item) || !isFullWidthBlock(item) || !isFrameVerticalOverlap(resolveSchemaItemFrame(item, designWidth), treeFrame))
      return item
    const itemFrame = resolveSchemaItemFrame(item, designWidth)
    return {
      ...item,
      gridX: pixelXToGridX(mainX, designWidth),
      gridW: Math.max(1, LIST_PAGE_GRID_COLS - pixelXToGridX(mainX, designWidth)),
      props: {
        ...(item.props || {}),
        style: {
          ...(item.props?.style || createDefaultBlockStyle()),
          x: mainX,
          widthMode: 'full',
          width: '100%',
          height: itemFrame.height,
        },
      },
    }
  })
}

function isTreeMainBlock(item = {}) {
  return ['AiCrudPage', 'AiTable', 'data-table', 'search-form', 'toolbar'].includes(item.blockType)
}

function isFullWidthBlock(item = {}) {
  const style = item.props?.style || {}
  return style.widthMode === 'full' || style.width === '100%' || style.width === '' || style.width === undefined || style.width === null
}

function resolveSchemaItemFrame(item = {}, designWidth = LIST_PAGE_DESIGN_WIDTH) {
  const colWidth = Math.floor((designWidth - (LIST_PAGE_GRID_COLS - 1) * LIST_PAGE_GRID_GAP) / LIST_PAGE_GRID_COLS)
  const cellWidth = colWidth + LIST_PAGE_GRID_GAP
  const fallbackX = Number(item.gridX || 0) * cellWidth
  const fallbackY = Number(item.gridY || 0) * (LIST_PAGE_GRID_ROW_HEIGHT + LIST_PAGE_GRID_GAP)
  const fallbackWidth = (Number(item.gridW || 1) * colWidth) + (Math.max(1, Number(item.gridW || 1)) - 1) * LIST_PAGE_GRID_GAP
  const fallbackHeight = (Number(item.gridH || 1) * LIST_PAGE_GRID_ROW_HEIGHT) + (Math.max(1, Number(item.gridH || 1)) - 1) * LIST_PAGE_GRID_GAP
  const style = item.props?.style || {}
  const x = resolveStyleNumber(style.x ?? style.left, fallbackX)
  const y = resolveStyleNumber(style.y ?? style.top, fallbackY)
  const width = isFullWidthBlock(item)
    ? Math.max(24, designWidth - x)
    : Math.max(24, resolveStyleNumber(style.width, fallbackWidth))
  const height = Math.max(24, resolveStyleNumber(style.height, fallbackHeight))
  return { x, y, width, height }
}

function resolveStyleNumber(value, fallback = 0) {
  if (value === null || value === undefined || value === '' || value === '100%' || value === 'auto')
    return Math.round(fallback)
  if (typeof value === 'number')
    return Math.round(value)
  const num = Number(String(value).trim().replace('px', ''))
  return Number.isFinite(num) ? Math.round(num) : Math.round(fallback)
}

function pixelXToGridX(x = 0, designWidth = LIST_PAGE_DESIGN_WIDTH) {
  const colWidth = Math.floor((designWidth - (LIST_PAGE_GRID_COLS - 1) * LIST_PAGE_GRID_GAP) / LIST_PAGE_GRID_COLS)
  return clampNumber(Math.round((Number(x) || 0) / (colWidth + LIST_PAGE_GRID_GAP)), 0, LIST_PAGE_GRID_COLS - 1)
}

function isFrameVerticalOverlap(a = {}, b = {}) {
  const aTop = Number(a.y) || 0
  const bTop = Number(b.y) || 0
  return aTop < bTop + (Number(b.height) || 0) && bTop < aTop + (Number(a.height) || 0)
}

export function bootstrapGridLayoutFromZones(zones, modelSchema, options = {}) {
  const layoutType = options.layoutType || (modelSchema?.appType === 'TREE' ? 'tree-crud' : 'simple-crud')
  const isTree = layoutType === 'tree-crud'
  const search = (zones || []).find(z => z.zoneKey === 'search')
  const table = (zones || []).find(z => z.zoneKey === 'table')
  const items = []
  const mainX = isTree ? 3 : 0
  const mainW = isTree ? 9 : 12
  const searchFieldSet = new Set(filterPageFields(modelSchema?.fields || [], 'search').map(f => f.field))
  const tableFieldSet = new Set((modelSchema?.fields || [])
    .filter(field => isListFieldSelectable(field, 'table'))
    .map(f => f.field))

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
      label: '筛选树',
      props: {
        ...defaultTreeConfig,
      },
      fieldRefs: [],
    })
  }

  let yCursor = 0
  if (search?.enabled !== false) {
    const searchRefs = (search?.fieldRefs || []).filter(ref => searchFieldSet.has(ref))
    items.push({
      id: 'block_search',
      blockType: 'search-form',
      gridX: mainX,
      gridY: yCursor,
      gridW: mainW,
      gridH: 4,
      label: '查询表单',
      props: {
        fieldSettings: sanitizeFieldSettings(search?.props?.fieldSettings, new Set(searchRefs), searchFieldSet),
        collapsible: true,
      },
      fieldRefs: searchRefs,
    })
    yCursor += 4
  }

  const tableActions = []
  if (table && table.props?.showImport !== false)
    tableActions.push('import')
  if (table && table.props?.showExport !== false)
    tableActions.push('export')
  if (table && table.props?.hideBatchDelete !== true)
    tableActions.push('batch-delete')
  if (table && table.props?.enableCustomQuery !== false)
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
    const tableRefs = (table?.fieldRefs || []).filter(ref => tableFieldSet.has(ref))
    items.push({
      id: 'block_table',
      blockType: 'data-table',
      gridX: mainX,
      gridY: yCursor,
      gridW: mainW,
      gridH: 10,
      label: '数据列表',
      props: {
        fieldSettings: sanitizeFieldSettings(table?.props?.fieldSettings, new Set(tableRefs)),
        defaultSortField: table?.props?.defaultSortField || 'id',
        defaultSortOrder: table?.props?.defaultSortOrder || 'desc',
      },
      fieldRefs: tableRefs,
    })
  }

  return {
    cols: LIST_PAGE_GRID_COLS,
    rowHeight: LIST_PAGE_GRID_ROW_HEIGHT,
    gap: LIST_PAGE_GRID_GAP,
    designWidth: LIST_PAGE_DESIGN_WIDTH,
    layoutType,
    items,
  }
}

export function applyGridLayoutToZones(zones, gridLayout, modelSchema) {
  const items = gridLayout?.items || []
  const crud = items.find(i => i.blockType === 'AiCrudPage')
  const search = items.find(i => i.blockType === 'search-form')
  const table = items.find(i => i.blockType === 'data-table') || crud || items.find(i => i.blockType === 'AiTable')
  const tree = items.find(i => i.blockType === 'tree-panel')
  const toolbar = items.find(i => i.blockType === 'toolbar')
  const searchFieldSet = new Set(filterPageFields(modelSchema?.fields || [], 'search').map(f => f.field))
  const tableFieldSet = new Set((modelSchema?.fields || [])
    .filter(field => isListFieldSelectable(field, 'table'))
    .map(f => f.field))

  return (zones || []).map((zone) => {
    if (zone.zoneKey === 'search') {
      const hasCrudSearchRefs = Object.prototype.hasOwnProperty.call(crud?.props || {}, 'searchFieldRefs')
      const crudSearchRefs = hasCrudSearchRefs
        ? crud?.props?.searchFieldRefs || []
        : crud?.fieldRefs || []
      const refs = (search?.fieldRefs || crudSearchRefs || []).filter(ref => searchFieldSet.has(ref))
      const fieldSettings = search?.props?.fieldSettings
        || crud?.props?.searchFieldSettings
        || zone.props?.fieldSettings
      return {
        ...zone,
        enabled: Boolean(search || crud),
        fieldRefs: refs,
        props: {
          ...(zone.props || {}),
          fieldSettings: sanitizeFieldSettings(
            fieldSettings,
            new Set(refs),
            searchFieldSet,
          ),
        },
      }
    }
    if (zone.zoneKey === 'table') {
      const refs = (table?.fieldRefs || []).filter(ref => tableFieldSet.has(ref))
      const actions = toolbar?.props?.actions || []
      const tableProps = table?.props || {}
      const crudProps = crud?.props || {}
      const sourceProps = {
        ...crudProps,
        ...tableProps,
      }
      const treeProps = normalizeTreeConfigAliases(tree?.props || {})
      const zoneTreeConfig = normalizeTreeConfigAliases(zone.props?.treeConfig || {})
      const nextProps = {
        ...(zone.props || {}),
        ...pickRuntimeTableProps(sourceProps),
        fieldSettings: sanitizeFieldSettings(sourceProps.fieldSettings || zone.props?.fieldSettings, new Set(refs)),
        showImport: toolbar ? actions.includes('import') : sourceProps.showImport !== false,
        showExport: toolbar ? actions.includes('export') : sourceProps.showExport !== false,
        hideBatchDelete: toolbar ? !actions.includes('batch-delete') : sourceProps.hideBatchDelete === true,
        enableCustomQuery: toolbar ? actions.includes('custom-query') : sourceProps.enableCustomQuery !== false,
        customActions: toolbar ? (toolbar.props?.customActions || sourceProps.customActions || []) : (sourceProps.customActions || []),
        defaultSortField: sourceProps.defaultSortField || zone.props?.defaultSortField || 'id',
        defaultSortOrder: sourceProps.defaultSortOrder || zone.props?.defaultSortOrder || 'desc',
      }
      if (tree) {
        nextProps.treeConfig = {
          ...zoneTreeConfig,
          ...treeProps,
          enabled: treeProps.enabled ?? zoneTreeConfig.enabled ?? true,
          sourceModelCode: treeProps.sourceModelCode || zoneTreeConfig.sourceModelCode || '',
          sourceModelName: treeProps.sourceModelName || zoneTreeConfig.sourceModelName || '',
          sourceTableName: treeProps.sourceTableName || zoneTreeConfig.sourceTableName || '',
          sourceConfigKey: treeProps.sourceConfigKey || zoneTreeConfig.sourceConfigKey || '',
          treeApi: treeProps.treeApi || zoneTreeConfig.treeApi || '',
          treeTitle: treeProps.treeTitle || zoneTreeConfig.treeTitle,
          keyField: treeProps.keyField || zoneTreeConfig.keyField || 'id',
          parentField: treeProps.parentField || zoneTreeConfig.parentField || 'parentId',
          labelField: treeProps.labelField || zoneTreeConfig.labelField || '',
          filterField: treeProps.filterField || zoneTreeConfig.filterField || '',
          targetField: treeProps.targetField || zoneTreeConfig.targetField || '',
          childrenField: treeProps.childrenField || zoneTreeConfig.childrenField || 'children',
          loadMode: treeProps.loadMode || zoneTreeConfig.loadMode || 'full',
        }
        // 左树右表默认关掉「添加下级」；本表嵌入式树已启用时跟随 model
        nextProps.enableTreeAddChild = modelSchema?.treeConfig?.enabled === true
          ? modelSchema.treeConfig.enableTreeAddChild === true
          : false
      }
      else if (modelSchema?.treeConfig?.enabled === true || modelSchema?.appType === 'TREE') {
        // 嵌入式树表：无 tree-panel 时也要把模型树配置落到 table zone，避免被同步清掉
        nextProps.treeConfig = {
          ...resolveDefaultTreeConfig(modelSchema, {
            ...zoneTreeConfig,
            ...(modelSchema.treeConfig || {}),
            enabled: true,
          }),
          enabled: true,
        }
        if (typeof modelSchema.treeConfig?.enableTreeAddChild === 'boolean')
          nextProps.enableTreeAddChild = modelSchema.treeConfig.enableTreeAddChild
        else if (typeof sourceProps.enableTreeAddChild === 'boolean')
          nextProps.enableTreeAddChild = sourceProps.enableTreeAddChild
        else
          nextProps.enableTreeAddChild = true
      }
      else {
        delete nextProps.treeConfig
        nextProps.enableTreeAddChild = false
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

function pickRuntimeTableProps(props = {}) {
  const keys = [
    'title',
    'api',
    'rowKey',
    'listApi',
    'detailApi',
    'createApi',
    'updateApi',
    'deleteApi',
    'importApi',
    'exportApi',
    'listMethod',
    'listDataField',
    'listTotalField',
    'isEncrypt',
    'publicParams',
    'publicQuery',
    'formDefaultValues',
    'submitDefaultParams',
    'showSearch',
    'showPagination',
    'searchGridCols',
    'searchLabelWidth',
    'searchEnableCollapse',
    'searchMaxVisibleFields',
    'searchYGap',
    'tableSize',
    'childListDisplayMode',
    'renderMode',
    'showRenderModeSwitch',
    'hideSelection',
    'striped',
    'bordered',
    'maxHeight',
    'scrollX',
    'editGridCols',
    'editLabelWidth',
    'editLabelPlacement',
    'editLabelAlign',
    'editSize',
    'editShowFeedback',
    'editXGap',
    'editYGap',
    'modalWidth',
    'detailModalWidth',
    'modalType',
    'formOpenMode',
    'tabWorkspace',
    'drawerPlacement',
    'hideModalFooter',
    'hideDefaultDetailContent',
    'hideToolbar',
    'hideAdd',
    'hideBatchDelete',
    'showImport',
    'showExport',
    'showExportTasks',
    'enableCustomQuery',
    'addButtonText',
    'exportButtonText',
    'exportFileName',
    'crudHookRules',
    'beforeSubmitRules',
    'previewLiveData',
    'previewMode',
    'previewRecordId',
    'lastPreviewStatus',
    'lastPreviewMessage',
    'lastPreviewError',
    'lastPreviewAt',
    'enableTreeAddChild',
    'treeApi',
  ]
  return keys.reduce((next, key) => {
    if (Object.prototype.hasOwnProperty.call(props, key))
      next[key] = props[key]
    return next
  }, {})
}

function sanitizeGridBlockProps(blockType, props = {}, ownerFieldSet = new Set(), queryFieldSet = new Set()) {
  const next = {
    ...(props || {}),
    style: sanitizeBlockStyle(props?.style || {}, blockType),
    events: sanitizeBlockEvents(props?.events),
  }
  if (blockType !== 'tree-panel')
    delete next.treeConfig
  if (blockType === 'search-form') {
    next.fieldSettings = sanitizeFieldSettings(next.fieldSettings, ownerFieldSet, queryFieldSet)
  }
  else if (['data-table', 'AiCrudPage', 'AiTable', 'AiForm', 'detail-info'].includes(blockType)) {
    next.fieldSettings = sanitizeFieldSettings(next.fieldSettings, ownerFieldSet)
    if (blockType === 'AiCrudPage') {
      const hasSearchRefs = Object.prototype.hasOwnProperty.call(next, 'searchFieldRefs')
      if (hasSearchRefs) {
        const searchRefs = Array.isArray(next.searchFieldRefs)
          ? next.searchFieldRefs.filter(field => queryFieldSet.has(field))
          : []
        next.searchFieldRefs = searchRefs
        next.searchFieldSettings = sanitizeFieldSettings(next.searchFieldSettings, new Set(searchRefs), queryFieldSet)
      }
    }
  }
  return next
}

function sanitizeBlockStyle(style = {}, blockType = '') {
  const next = {
    ...createDefaultBlockStyle(),
    ...(style || {}),
  }
  const widthModeSet = ['full', 'auto', 'fixed']
  if (!widthModeSet.includes(next.widthMode)) {
    const rawWidth = next.width
    const numericWidth = Number(String(rawWidth ?? '').replace('px', ''))
    const isLegacyFullWidth = blockType !== 'tree-panel' && Number.isFinite(numericWidth) && numericWidth >= 900
    if (rawWidth === 'auto')
      next.widthMode = 'auto'
    else if (rawWidth === '100%' || rawWidth === '' || isLegacyFullWidth)
      next.widthMode = 'full'
    else
      next.widthMode = 'fixed'
  }
  if (next.widthMode === 'full')
    next.width = '100%'
  if (next.widthMode === 'auto')
    next.width = 'auto'
  if (next.widthMode === 'fixed' && (next.width === '' || next.width === '100%' || next.width === 'auto'))
    next.width = 320
  if (!['fixed', 'auto', 'full'].includes(next.heightMode))
    next.heightMode = 'fixed'
  return next
}

function sanitizeBlockEvents(events = []) {
  if (!Array.isArray(events))
    return []
  return events
    .filter(event => event && typeof event === 'object')
    .map((event, index) => ({
      id: event.id || `evt_${Date.now()}_${index}`,
      trigger: event.trigger || 'click',
      action: event.action || 'none',
      targetBlockId: event.targetBlockId || '',
      targetPageKey: event.targetPageKey || '',
      targetFormKey: event.targetFormKey || '',
      description: event.description || '',
      params: Array.isArray(event.params)
        ? event.params.map(param => ({
            name: param?.name || '',
            value: param?.value || '',
            sourceType: param?.sourceType || 'static',
            sourceField: param?.sourceField || '',
          }))
        : [],
    }))
}

function sanitizeContainerChildren(children = [], modelSchema = {}, layoutType = 'simple-crud') {
  if (!Array.isArray(children))
    return []
  return children
    .filter(child => child && typeof child === 'object' && resolveListPageBlockMeta(child.blockType))
    .map((child) => {
      const meta = resolveListPageBlockMeta(child.blockType) || {}
      let props = child.blockType === 'tree-panel'
        ? {
            ...sanitizeGridBlockProps(child.blockType, child.props || {}),
            ...resolveDefaultTreeConfig(modelSchema, child.props || {}),
          }
        : sanitizeGridBlockProps(child.blockType, child.props || {})
      if (child.blockType === 'grid-layout') {
        props = {
          ...props,
          cells: sanitizeGridCells(props.cells || [], modelSchema, layoutType),
        }
      }
      return {
        id: child.id || createBlockId(child.blockType),
        blockType: child.blockType,
        label: child.label || meta.title || child.blockType,
        gridX: clampNumber(child.gridX, 0, LIST_PAGE_GRID_COLS - 1),
        gridY: Math.max(0, Number(child.gridY) || 0),
        gridW: clampNumber(child.gridW || meta.defaultW || 12, 1, LIST_PAGE_GRID_COLS),
        gridH: Math.max(resolveBlockMinGridH(child.blockType, meta), Number(child.gridH) || meta.defaultH || 2),
        props,
        fieldRefs: Array.isArray(child.fieldRefs) ? child.fieldRefs : [],
        children: sanitizeContainerChildren(child.children || [], modelSchema, layoutType),
      }
    })
    .filter((child) => {
      const meta = resolveListPageBlockMeta(child.blockType) || {}
      return !meta.onlyFor || meta.onlyFor.includes(layoutType)
    })
}

function sanitizeGridCells(cells = [], modelSchema = {}, layoutType = 'simple-crud') {
  if (!Array.isArray(cells))
    return []
  return cells.map((cell, index) => ({
    key: cell?.key || `cell_${index + 1}`,
    title: cell?.title ?? `栅格 ${index + 1}`,
    span: clampNumber(cell?.span ?? 6, 1, 24),
    children: sanitizeContainerChildren(cell?.children || [], modelSchema, layoutType),
  }))
}

function sanitizeFieldSettings(settings = {}, ownerFieldSet = new Set(), queryFieldSet = null) {
  if (!settings || typeof settings !== 'object' || Array.isArray(settings))
    return {}
  const next = {}
  Object.entries(settings).forEach(([fieldName, value]) => {
    if (!ownerFieldSet.has(fieldName) || !value || typeof value !== 'object' || Array.isArray(value))
      return
    const setting = { ...value }
    if (queryFieldSet instanceof Set && setting.queryField && !queryFieldSet.has(setting.queryField))
      delete setting.queryField
    next[fieldName] = setting
  })
  return next
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
  const defaultWidthMode = resolveDefaultBlockWidthMode(blockType)
  const base = {
    id: createBlockId(blockType),
    blockType,
    label: meta.title,
    gridX: clampNumber(position.gridX ?? 0, 0, LIST_PAGE_GRID_COLS - 1),
    gridY: Math.max(0, Number(position.gridY) || 0),
    gridW: Math.min(meta.defaultW || 6, LIST_PAGE_GRID_COLS),
    gridH: meta.defaultH || 2,
    props: {
      style: createDefaultBlockStyle(defaultWidthMode),
      events: [],
    },
    fieldRefs: [],
    children: [],
  }
  if (blockType === 'back-button') {
    base.props = {
      ...base.props,
      text: '返回',
      type: 'default',
      action: 'back',
      events: [
        {
          id: `evt_back_${Date.now()}`,
          trigger: 'click',
          action: 'back',
          targetBlockId: '',
          description: '返回上一页',
          params: [],
        },
      ],
    }
  }
  if (blockType === 'page-title') {
    base.props = {
      ...base.props,
      title: '页面标题',
      subtitle: '页面说明或当前业务对象摘要',
      content: '<h1>页面标题</h1><p>页面说明或当前业务对象摘要</p>',
      statusText: '',
      statusType: 'info',
      size: 'medium',
      textAlign: 'left',
      fontStyle: 'normal',
      textDecoration: 'none',
      indent: 0,
      titleTextStyle: { fontSize: 20, fontWeight: 800, textAlign: 'left', color: '#0f172a' },
      subtitleTextStyle: { fontSize: 13, fontWeight: 400, textAlign: 'left', color: '#64748b' },
    }
  }
  if (blockType === 'grid-layout') {
    base.props = {
      ...base.props,
      style: {
        ...(base.props?.style || {}),
        heightMode: 'auto',
      },
      columns: 24,
      gutter: 16,
      cellMinHeight: 120,
      alignItems: 'stretch',
      justifyItems: 'stretch',
      showCellBorder: true,
      cellBackground: 'transparent',
      cells: Array.from({ length: 4 }).map((_, index) => ({
        key: `cell_${index + 1}`,
        title: `栅格 ${index + 1}`,
        span: 6,
        children: [],
      })),
    }
    base.children = []
  }
  if (blockType === 'detail-info') {
    base.fieldRefs = filterPageFields(fields, 'detail').slice(0, 8).map(f => f.field)
    base.props = {
      ...base.props,
      title: '详情信息',
      columnCount: 2,
      labelPlacement: 'left',
      bordered: false,
      fieldSettings: {},
    }
  }
  if (blockType === 'search-form') {
    base.fieldRefs = filterPageFields(fields, 'search').slice(0, 8).map(f => f.field)
    base.props = { ...base.props, fieldSettings: {}, collapsible: true }
  }
  if (blockType === 'data-table') {
    base.fieldRefs = filterDefaultListFields(fields).map(f => f.field)
    base.props = { ...base.props, fieldSettings: {}, defaultSortField: 'id', defaultSortOrder: 'desc' }
  }
  if (blockType === 'toolbar') {
    base.props = { ...base.props, actions: ['add', 'batch-delete', 'import', 'export', 'custom-query'], customActions: [] }
  }
  if (blockType === 'AiCrudPage') {
    base.props = {
      ...base.props,
      ...createDefaultAiCrudPageProps(),
      searchFieldRefs: filterPageFields(fields, 'search').slice(0, 8).map(f => f.field),
      searchFieldSettings: {},
      style: {
        ...base.props.style,
        // 用设计高度，避免 full 铺满后盖住同页其它组件（预览/运行均可见）
        heightMode: 'fixed',
      },
    }
    base.fieldRefs = filterPageFields(fields, 'table').slice(0, 8).map(f => f.field)
  }
  if (blockType === 'AiTable') {
    base.props = {
      ...base.props,
      title: 'AiTable',
      rowKey: 'id',
      size: 'small',
      renderMode: 'table',
      showToolbar: true,
      showPagination: true,
      showRefresh: true,
      showDensity: true,
      showColumnFilter: true,
      showSearchToggle: false,
      showFullscreen: false,
      showRenderModeSwitch: true,
      hideSelection: false,
      striped: false,
      bordered: true,
      singleLine: false,
      maxHeight: '',
      scrollX: undefined,
    }
    base.fieldRefs = filterPageFields(fields, 'table').slice(0, 8).map(f => f.field)
  }
  if (blockType === 'AiForm') {
    base.props = {
      ...base.props,
      title: '数据表单',
      gridCols: 2,
      labelPlacement: 'left',
      labelWidth: 100,
      labelAlign: 'right',
      size: 'medium',
      xGap: 12,
      yGap: 0,
      showActions: true,
      showSubmit: true,
      showReset: true,
      showCancel: false,
      submitText: '提交',
      resetText: '重置',
      cancelText: '取消',
      enableCollapse: false,
      maxVisibleFields: 6,
      showFeedback: true,
    }
    base.fieldRefs = filterPageFields(fields, 'table').slice(0, 6).map(f => f.field)
  }
  if (blockType === 'tree-panel') {
    base.props = { ...base.props, ...resolveDefaultTreeConfig(modelSchema) }
  }
  if (blockType === 'stats-strip') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('items', {
        renderMode: 'manual',
        labelField: 'label',
        valueField: 'value',
        metaField: 'trend',
      }),
      metrics: [
        { label: '总数', value: '128', trend: '+8%' },
        { label: '活跃', value: '92', trend: '+3%' },
        { label: '本月新增', value: '21', trend: '+12%' },
        { label: '异常', value: '3', trend: '-1' },
      ],
    }
  }
  if (blockType === 'workspace-summary-metrics') {
    base.props = {
      ...base.props,
      visibleKeys: ['todo', 'done', 'started', 'cc'],
      columns: 4,
      showHeader: false,
      style: {
        ...(base.props?.style || {}),
        widthMode: 'full',
        heightMode: 'auto',
        height: 200,
      },
    }
  }
  if (blockType === 'info-panel') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'title', contentField: 'content', valueField: 'type' }),
      title: '提示信息',
      content: '在这里展示当前页面的说明、风险提醒或操作结果。',
      type: 'info',
    }
  }
  if (blockType === 'custom-html') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'title', contentField: 'content' }),
      title: '说明',
      content: '在此填写业务说明、操作指引或链接。',
    }
  }
  if (blockType === 'action-button') {
    base.props = {
      ...base.props,
      text: '操作',
      type: 'primary',
      size: 'small',
      secondary: false,
      block: false,
      disabled: false,
      loading: false,
    }
  }
  if (blockType === 'button-group') {
    base.props = {
      ...base.props,
      buttons: [
        { key: 'primary', text: '主操作', type: 'primary' },
        { key: 'secondary', text: '次操作', type: 'default' },
      ],
    }
  }
  if (blockType === 'tag-list') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('items', { labelField: 'label', valueField: 'type' }),
      tags: [
        { label: '启用', type: 'success' },
        { label: '重点', type: 'warning' },
        { label: '业务', type: 'info' },
      ],
    }
  }
  if (blockType === 'steps') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('items', { titleField: 'title', descriptionField: 'description' }),
      current: 1,
      steps: [
        { title: '提交', description: '创建业务记录' },
        { title: '处理', description: '业务审核中' },
        { title: '完成', description: '流程结束' },
      ],
    }
  }
  if (blockType === 'timeline') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('items', { titleField: 'title', descriptionField: 'content', metaField: 'time' }),
      title: '操作记录',
      items: [
        { title: '创建记录', time: '2026-06-18 09:00', content: '系统创建业务数据' },
        { title: '更新状态', time: '2026-06-18 10:30', content: '处理人更新状态' },
      ],
    }
  }
  if (blockType === 'empty-state') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'title', descriptionField: 'description', valueField: 'actionText' }),
      title: '暂无数据',
      description: '当前条件下没有可展示的数据',
      actionText: '新建数据',
    }
  }
  if (pageWidgetCatalog.some(item => item.blockType === blockType)) {
    base.props = {
      ...base.props,
      ...createPageWidgetDefaultProps(blockType),
    }
  }
  if (blockType === 'signature-pad') {
    base.props = {
      ...base.props,
      title: '手写签名',
      height: 160,
      strokeWidth: 2.6,
      disabled: false,
      businessType: 'lowcode_signature',
      required: false,
    }
  }
  if (blockType === 'step-form') {
    base.props = {
      ...base.props,
      title: '分步表单',
      current: 1,
      direction: 'horizontal',
      steps: [
        { title: '基础信息', description: '填写基础字段' },
        { title: '业务配置', description: '补充业务信息' },
        { title: '确认提交', description: '检查并提交' },
      ],
    }
    base.fieldRefs = filterPageFields(fields, 'table').slice(0, 6).map(f => f.field)
  }
  if (blockType === 'text-title') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'text', descriptionField: 'subtitle' }),
      text: '标题示例',
      level: 2,
      weight: 800,
      align: 'left',
      textAlign: 'left',
      fontStyle: 'normal',
      textDecoration: 'none',
      indent: 0,
      color: '#0f172a',
      subtitle: '',
    }
  }
  if (blockType === 'paragraph') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { contentField: 'content' }),
      content: '这里是一段页面说明文字，可用于介绍当前业务页面、操作规则或注意事项。',
      align: 'left',
      textAlign: 'left',
      fontStyle: 'normal',
      textDecoration: 'none',
      listType: 'none',
      quote: false,
      indent: 0,
      color: '#475569',
      lineHeight: 1.7,
      clamp: 0,
    }
  }
  if (blockType === 'statistic') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('value', { titleField: 'title', valueField: 'value', descriptionField: 'description', metaField: 'trend' }),
      title: '统计指标',
      value: '12,836',
      prefix: '',
      suffix: '条',
      trend: '+12.5%',
      color: '#2563eb',
      description: '较上月增长',
    }
  }
  if (blockType === 'link') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'text', valueField: 'href' }),
      text: '查看详情',
      href: '',
      target: '_self',
      type: 'primary',
      underline: false,
    }
  }
  if (blockType === 'text-tip') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'title', contentField: 'content', valueField: 'type' }),
      title: '提示',
      content: '请先选择左侧数据，再查看右侧明细。',
      type: 'info',
      showIcon: true,
      closable: false,
    }
  }
  if (blockType === 'audio-player') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'title', valueField: 'src' }),
      title: '音频播放器',
      src: '',
      controls: true,
      autoplay: false,
      loop: false,
    }
  }
  if (blockType === 'video-player') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'title', valueField: 'src', metaField: 'poster' }),
      title: '视频播放器',
      src: '',
      poster: '',
      controls: true,
      autoplay: false,
      loop: false,
      muted: false,
    }
  }
  if (blockType === 'avatar') {
    base.props = {
      ...base.props,
      dataBinding: createWidgetDataBinding('content', { titleField: 'name', descriptionField: 'description', valueField: 'src' }),
      name: '用户名称',
      description: '角色 / 部门',
      src: '',
      size: 48,
      shape: 'circle',
      showInfo: true,
    }
  }
  if (blockType === 'iframe') {
    base.props = {
      ...base.props,
      title: '内嵌页面',
      src: '',
      allowFullscreen: false,
      sandbox: 'allow-same-origin allow-forms',
      loading: 'lazy',
    }
  }
  if (blockType === 'box-layout') {
    base.props = {
      ...base.props,
      style: {
        ...(base.props?.style || {}),
        heightMode: 'auto',
      },
      title: '盒子布局',
      direction: 'row',
      gap: 12,
      alignItems: 'stretch',
      justifyContent: 'flex-start',
      wrap: true,
    }
    base.children = []
  }
  if (blockType === 'space') {
    base.props = {
      ...base.props,
      direction: 'vertical',
      size: 24,
      lineVisible: false,
    }
  }
  if (blockType === 'sub-table-tabs') {
    base.props = {
      ...base.props,
      tabs: [
        { key: 'detail', title: '基本信息' },
        { key: 'related', title: '关联记录' },
      ],
    }
  }
  if (blockType === 'section-divider') {
    base.props = { ...base.props, title: '分组标题' }
  }
  if (blockType === 'card') {
    base.props = {
      ...base.props,
      style: {
        ...(base.props?.style || {}),
        heightMode: 'auto',
      },
      title: '卡片标题',
      content: '',
    }
    base.children = []
  }
  if (blockType === 'tabs') {
    base.props = {
      ...base.props,
      style: {
        ...(base.props?.style || {}),
        heightMode: 'auto',
      },
      tabs: [
        { key: 'tab1', title: '标签一', children: [] },
        { key: 'tab2', title: '标签二', children: [] },
      ],
    }
    base.children = []
  }
  if (blockType === 'divider') {
    base.props = {
      ...base.props,
      orientation: 'horizontal',
      title: '',
    }
  }
  if (blockType === 'spacer') {
    base.props = {
      ...base.props,
      style: {
        ...base.props.style,
        backgroundColor: 'transparent',
        borderWidth: 0,
        boxShadow: 'none',
      },
    }
  }
  return base
}

function resolveDefaultBlockWidthMode(blockType = '') {
  const autoWidthBlockTypes = [
    'back-button',
    'action-button',
    'button-group',
    'tag-list',
    'link',
    'statistic',
    'countdown',
    'number-animation',
    'breadcrumb',
    'pagination',
    'qrcode',
    'barcode',
    'avatar',
    'space',
    'divider',
    'section-divider',
  ]
  if (autoWidthBlockTypes.includes(blockType))
    return 'auto'
  return 'full'
}

export function createDefaultBlockStyle(widthMode = 'full') {
  const normalizedWidthMode = ['full', 'auto', 'fixed'].includes(widthMode) ? widthMode : 'full'
  let normalizedWidth = 320
  if (normalizedWidthMode === 'full')
    normalizedWidth = '100%'
  if (normalizedWidthMode === 'auto')
    normalizedWidth = 'auto'
  return {
    x: '',
    y: '',
    widthMode: normalizedWidthMode,
    width: normalizedWidth,
    heightMode: 'fixed',
    height: '100%',
    backgroundColor: 'transparent',
    borderColor: 'transparent',
    borderWidth: 0,
    borderStyle: 'none',
    borderRadius: 0,
    boxShadow: 'none',
    padding: 0,
    margin: 0,
    minWidth: '',
    maxWidth: '',
    minHeight: '',
    maxHeight: '',
    customStyle: '',
  }
}

function createDefaultBlockFrameStyle(gridX = 0, gridY = 0, gridW = 12, gridH = 2, widthMode = 'full') {
  const fixedWidth = gridW * LIST_PAGE_GRID_BASE_COL_WIDTH + (gridW - 1) * LIST_PAGE_GRID_GAP
  return {
    ...createDefaultBlockStyle(),
    x: gridX * (LIST_PAGE_GRID_BASE_COL_WIDTH + LIST_PAGE_GRID_GAP),
    y: gridY * (LIST_PAGE_GRID_ROW_HEIGHT + LIST_PAGE_GRID_GAP),
    widthMode,
    width: widthMode === 'full' ? '100%' : fixedWidth,
    height: gridH * LIST_PAGE_GRID_ROW_HEIGHT + (gridH - 1) * LIST_PAGE_GRID_GAP,
  }
}

function createDefaultAiCrudPageProps() {
  return {
    title: '数据列表',
    description: '自动生成筛选、列表和数据维护表单',
    api: '',
    rowKey: 'id',
    listApi: '',
    detailApi: '',
    createApi: '',
    updateApi: '',
    deleteApi: '',
    importApi: '',
    exportApi: '',
    listMethod: 'get',
    listDataField: 'records',
    listTotalField: 'total',
    isEncrypt: false,
    showSearch: true,
    showPagination: true,
    searchGridCols: 4,
    searchLabelWidth: 'auto',
    searchEnableCollapse: true,
    searchMaxVisibleFields: 3,
    searchYGap: 16,
    searchFieldRefs: [],
    searchFieldSettings: {},
    tableSize: 'medium',
    renderMode: 'table',
    showRenderModeSwitch: true,
    hideSelection: false,
    striped: false,
    bordered: false,
    maxHeight: '',
    scrollX: undefined,
    editGridCols: 1,
    editLabelWidth: 'auto',
    editLabelPlacement: 'left',
    editLabelAlign: 'right',
    editSize: 'medium',
    editShowFeedback: true,
    editXGap: 16,
    editYGap: 8,
    modalWidth: '800px',
    detailModalWidth: 'min(1080px, 92vw)',
    modalType: 'modal',
    formOpenMode: 'modal',
    tabWorkspace: {
      maxTabs: 8,
      reuseRecordTab: true,
      closeAfterSave: false,
      showDirtyMark: true,
    },
    drawerPlacement: 'right',
    hideModalFooter: false,
    hideDefaultDetailContent: false,
    hideToolbar: false,
    hideAdd: false,
    hideBatchDelete: false,
    showImport: true,
    showExport: true,
    showExportTasks: true,
    enableCustomQuery: true,
    addButtonText: '新增',
    exportButtonText: '导出',
    exportFileName: '',
    customActions: [],
    crudHookRules: {},
    beforeSubmitRules: [],
    publicParams: {},
    publicQuery: {},
    formDefaultValues: {},
    submitDefaultParams: {},
    previewLiveData: false,
    previewMode: 'mock',
    previewRecordId: '',
    lastPreviewStatus: 'idle',
    lastPreviewMessage: '当前使用模拟预览，不请求接口',
    lastPreviewError: '',
    lastPreviewAt: '',
  }
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


function resolveBlockMinGridH(blockType, meta = {}) {
  if (blockType === 'toolbar')
    return Math.max(2, Number(meta.defaultH) || 2)
  if (blockType === 'AiCrudPage')
    return 5
  if (blockType === 'AiTable')
    return Math.max(8, Number(meta.defaultH) || 8)
  if (blockType === 'AiForm')
    return Math.max(5, Number(meta.defaultH) || 5)
  if (blockType === 'detail-info')
    return Math.max(5, Number(meta.defaultH) || 5)
  if (blockType === 'box-layout')
    return Math.max(5, Number(meta.defaultH) || 5)
  if (['rich-text', 'transfer', 'step-form', 'video-player', 'iframe', 'markdown', 'descriptions', 'signature-pad'].includes(blockType))
    return Math.max(4, Number(meta.defaultH) || 4)
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
      showImport: componentKeys.size ? componentKeys.has('import-button') : zone?.props?.showImport !== false,
      showExport: componentKeys.size ? componentKeys.has('export-button') : zone?.props?.showExport !== false,
      enableCustomQuery: componentKeys.size ? componentKeys.has('custom-query') : zone?.props?.enableCustomQuery !== false,
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
    align: normalizeAlign(item.style?.textAlign || item.style?.align),
  }
}

function normalizeAlign(value) {
  const align = String(value || '').toLowerCase()
  return ['left', 'center', 'right'].includes(align) ? align : undefined
}

function createItemId(zoneKey, componentKey, fieldRef) {
  if (fieldRef)
    return `${zoneKey}_${safeKey(fieldRef)}`
  const suffix = Math.random().toString(36).slice(2, 8)
  return `${zoneKey}_${safeKey(componentKey)}_${Date.now()}_${suffix}`
}


