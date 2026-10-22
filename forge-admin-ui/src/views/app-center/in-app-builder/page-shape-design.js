import { createGridBlock } from '@/components/lowcode-builder/page/page-schema'
import { isRichListGridLayout } from '@/components/lowcode-builder/shared/runtime-list-grid'
import { createDefaultFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'
import { normalizeObjectCode } from '@/views/app-center/components/designer/form-first/namingUtils'
import { createInAppFormAsset, createNavigationNode } from './in-app-builder-schema'
import {
  PAGE_DATA_SOURCE_CREATE,
  PAGE_DATA_SOURCE_EXISTING_TABLE,
  PAGE_OBJECT_CREATE_BLANK,
  PAGE_OBJECT_CREATE_DB_IMPORT,
  resolvePageCreateMode,
} from './page-table-import'

export const PAGE_SHAPE_TYPES = Object.freeze([
  {
    value: 'form',
    label: '表单页',
    description: '适合登记、申请和单条数据维护',
  },
  {
    value: 'list',
    label: '列表页',
    description: '适合查询、筛选和批量管理数据',
  },
  {
    value: 'list-form',
    label: '列表 + 表单',
    description: '在同一工作区浏览列表并编辑数据',
  },
  {
    value: 'tree-list',
    label: '树形列表',
    description: '自动预置名称、排序号、父级字段，列表按父子层级展开',
  },
  {
    value: 'tree-table',
    label: '左树右表',
    description: '自动预置名称、排序号、父级字段，开箱可用树 + 列表联动',
  },
  {
    value: 'custom',
    label: '自由布局',
    description: '空白画布，自由拖入组件搭建页面，不强制绑定数据对象',
  },
])

const PAGE_SHAPE_VALUES = new Set(PAGE_SHAPE_TYPES.map(item => item.value))
const OBJECT_PAGE_SHAPES = new Set(['form', 'list', 'list-form', 'tree-list', 'tree-table'])
const TREE_PRESET_SHAPES = new Set(['tree-list', 'tree-table'])
const FLAT_FORM_SHAPES = new Set(['list-form', 'tree-list', 'tree-table'])

/** 左树右表模板默认字段（可改、可增删，不锁定）。 */
export function buildTreeTablePresetFields() {
  return [
    {
      fieldCode: 'name',
      fieldName: '名称',
      columnName: 'name',
      fieldType: 'TEXT',
      dataType: 'varchar',
      length: 128,
      componentType: 'input',
      required: true,
      searchable: true,
      listVisible: true,
      formVisible: true,
      sortable: true,
      createIfMissing: true,
    },
    {
      fieldCode: 'sortNo',
      fieldName: '排序号',
      columnName: 'sort_no',
      fieldType: 'NUMBER',
      dataType: 'int',
      length: 11,
      precision: 0,
      componentType: 'number',
      required: false,
      searchable: false,
      listVisible: true,
      formVisible: true,
      sortable: true,
      createIfMissing: true,
    },
    {
      fieldCode: 'parentId',
      fieldName: '父级 ID',
      columnName: 'parent_id',
      fieldType: 'SELECT',
      dataType: 'bigint',
      componentType: 'treeSelect',
      required: false,
      searchable: true,
      listVisible: false,
      formVisible: true,
      createIfMissing: true,
    },
  ]
}

export function buildTreeTableConfig(pageName = '', options = {}) {
  const title = String(pageName || '').trim()
  const embedded = options.embedded === true
  return {
    ...(embedded ? { enabled: true } : {}),
    keyField: 'id',
    parentField: 'parentId',
    labelField: 'name',
    filterField: 'parentId',
    targetField: 'id',
    childrenField: 'children',
    treeTitle: title ? `${title}树` : '分类树',
    loadMode: 'full',
    ...(embedded ? { enableTreeAddChild: true } : {}),
  }
}

export function mergeTreeTablePresetFields(existingFields = []) {
  const merged = Array.isArray(existingFields) ? [...existingFields] : []
  const codes = new Set(merged.map(field => String(field?.fieldCode || field?.field || '').trim()).filter(Boolean))
  for (const preset of buildTreeTablePresetFields()) {
    if (codes.has(preset.fieldCode))
      continue
    merged.push(preset)
    codes.add(preset.fieldCode)
  }
  return merged
}

export function normalizePageShapeSelection(selection = {}) {
  const pageName = String(selection.pageName || '').trim() || '未命名页面'
  const pageType = PAGE_SHAPE_VALUES.has(selection.pageType) ? selection.pageType : 'form'
  const objectName = String(selection.objectName || '').trim() || pageName
  const customPage = pageType === 'custom'
  const dataSourceMode = selection.dataSourceMode === PAGE_DATA_SOURCE_EXISTING_TABLE
    ? PAGE_DATA_SOURCE_EXISTING_TABLE
    : PAGE_DATA_SOURCE_CREATE
  const createMode = customPage ? '' : resolvePageCreateMode({ ...selection, dataSourceMode })
  const runtimeDatasourceId = customPage ? null : normalizeId(selection.runtimeDatasourceId)
  const importTableName = !customPage && createMode === PAGE_OBJECT_CREATE_DB_IMPORT
    ? String(selection.importTableName || '').trim()
    : ''
  return {
    pageName,
    pageType,
    objectName,
    objectCode: customPage
      ? ''
      : normalizeObjectCode(selection.objectCode, objectName),
    parentId: selection.parentId || null,
    dataSourceMode: customPage ? '' : dataSourceMode,
    createMode,
    runtimeDatasourceId,
    importDatasourceId: importTableName
      ? normalizeId(selection.importDatasourceId || selection.runtimeDatasourceId)
      : null,
    importTableName,
    fields: Array.isArray(selection.fields) ? selection.fields : [],
  }
}

export function createPageShapeBuilder(schema, selection = {}) {
  const normalized = normalizePageShapeSelection(selection)
  const customPage = normalized.pageType === 'custom'
  const treeTablePage = normalized.pageType === 'tree-table'
  const treeListPage = normalized.pageType === 'tree-list'
  const needsTreePresets = TREE_PRESET_SHAPES.has(normalized.pageType)
  const pageMode = resolvePageMode(normalized.pageType)
  const layoutType = resolveLayoutType(normalized.pageType)
  const nodeResult = createNavigationNode(schema, {
    type: 'page',
    title: normalized.pageName,
    parentId: normalized.parentId,
    pageType: customPage ? 'content' : 'object',
    pageTemplate: customPage ? 'custom' : normalized.pageType,
    pageShape: customPage ? 'custom' : normalized.pageType,
    objectRef: customPage
      ? null
      : buildPageObjectRef(selection, normalized, pageMode),
  })
  const pageId = nodeResult.nodes.at(-1)?.id || ''
  if (customPage)
    return { schema: nodeResult, pageId, formAssetId: '', selection: normalized }

  const importedFields = normalized.fields.filter(field => field && (field.fieldCode || field.field))
  const shapeFields = needsTreePresets
    ? mergeTreeTablePresetFields(importedFields)
    : importedFields
  const createMissingByCode = new Map(
    shapeFields
      .filter(field => field && (field.fieldCode || field.field))
      .map(field => [String(field.fieldCode || field.field), field.createIfMissing === true]),
  )
  let formDesignerSchema = createDefaultFormDesignerSchema({
    objectCode: normalized.objectCode,
    objectName: normalized.objectName,
    formName: normalized.pageName,
    formOpenMode: FLAT_FORM_SHAPES.has(normalized.pageType) ? 'flat' : 'modal',
    fields: shapeFields,
  })
  if (needsTreePresets && Array.isArray(formDesignerSchema.components)) {
    formDesignerSchema = {
      ...formDesignerSchema,
      components: formDesignerSchema.components.map((component) => {
        const fieldCode = component?.fieldBinding?.fieldCode
        if (!fieldCode || !createMissingByCode.get(fieldCode))
          return component
        return {
          ...component,
          fieldBinding: {
            ...component.fieldBinding,
            createIfMissing: true,
          },
        }
      }),
    }
  }
  const assetResult = createInAppFormAsset(nodeResult, {
    name: normalized.pageName,
    formKey: `${normalized.objectCode}_form`,
    formDesignerSchema,
  })
  const crudBlock = createGridBlock('AiCrudPage', { fields: [] }, { gridX: 0, gridY: 0 })
  const objectRef = buildPageObjectRef(selection, normalized, pageMode)
  const importedFieldCodes = shapeFields.map(field => field.fieldCode || field.field).filter(Boolean)
  const treeConfig = treeTablePage
    ? buildTreeTableConfig(normalized.pageName)
    : treeListPage
      ? buildTreeTableConfig(normalized.pageName, { embedded: true })
      : null
  const block = {
    ...crudBlock,
    label: normalized.pageName,
    props: {
      ...(crudBlock.props || {}),
      title: normalized.pageName,
      formAssetId: assetResult.formAssetId,
      formAssetFieldsInitialized: importedFieldCodes.length > 0,
      ...(importedFieldCodes.length ? { fieldRefs: importedFieldCodes } : {}),
      objectRef,
      layoutType,
      ...(normalized.pageType === 'form'
        ? { formOnly: true, hideToolbar: true, hideBatchDelete: true, showSearch: false }
        : {}),
      ...(FLAT_FORM_SHAPES.has(normalized.pageType)
        ? { formOpenMode: 'flat', modalType: 'flat' }
        : {}),
      ...(treeTablePage
        ? {
            enableTreeAddChild: false,
            treeConfig,
            options: {
              layoutType: 'tree-crud',
              enableTreeAddChild: false,
              treeConfig,
            },
          }
        : {}),
      ...(treeListPage
        ? {
            enableTreeAddChild: true,
            treeConfig,
            options: {
              layoutType: 'list-form',
              enableTreeAddChild: true,
              treeConfig,
            },
          }
        : {}),
    },
  }
  const page = assetResult.schema.pages[pageId]
  const nextSchema = {
    ...assetResult.schema,
    pages: {
      ...assetResult.schema.pages,
      [pageId]: {
        ...page,
        layout: {
          ...page.layout,
          gridLayout: {
            cols: 12,
            rowHeight: 32,
            gap: 8,
            designWidth: 1366,
            layoutType,
            items: [block],
          },
        },
      },
    },
  }
  return {
    schema: nextSchema,
    pageId,
    formAssetId: assetResult.formAssetId,
    selection: normalized,
  }
}

/**
 * 解析页面形态（纯函数，便于单测）。
 * custom → 自由布局；form / list / list-form / tree-list / tree-table → 对象页。
 *
 * @param {object|null} node 页面节点
 * @param {{ designTab?: string }} [options]
 */
export function resolvePageShapeFromNode(node = null, options = {}) {
  const designTab = String(Array.isArray(options.designTab) ? options.designTab[0] : (options.designTab ?? '')).trim()
  // URL/入口明确要求自由布局时，不再回落到表单/列表（否则中间空白）
  if (designTab === 'page')
    return 'custom'

  if (!node || node.type !== 'page')
    return 'custom'

  const explicit = String(node.pageShape || '').trim().toLowerCase()
  if (explicit === 'list_form')
    return 'list-form'
  if (explicit === 'tree_table' || explicit === 'tree-crud')
    return 'tree-table'
  if (explicit === 'tree_list')
    return 'tree-list'
  if (['custom', 'form', 'list', 'list-form', 'tree-list', 'tree-table'].includes(explicit))
    return explicit

  const template = String(node.pageTemplate || '').trim().toLowerCase()
  const pageType = String(node.pageType || '').trim().toLowerCase()
  const pageMode = String(node.objectRef?.pageMode || '').trim().toLowerCase()
  const layoutType = String(node.objectRef?.layoutType || '').trim().toLowerCase()

  // 模板/形态优先于 objectRef：新建快捷页草稿尚未落库对象时 objectRef 可能短暂为空，
  // 不能因此掉成 custom，否则表单/列表 Tab 会消失。
  if (template === 'form')
    return 'form'
  if (template === 'list')
    return 'list'
  if (template === 'list-form' || template === 'list_form')
    return 'list-form'
  if (template === 'tree-list' || template === 'tree_list')
    return 'tree-list'
  if (template === 'tree-table' || template === 'tree-crud' || template === 'tree_table')
    return 'tree-table'

  if (pageType === 'object') {
    if (layoutType === 'tree-crud' || pageMode === 'tree-table' || pageMode === 'tree-crud')
      return 'tree-table'
    if (pageMode === 'tree-list')
      return 'tree-list'
    if (pageMode === 'form')
      return 'form'
    if (pageMode === 'list')
      return 'list'
    return 'list-form'
  }

  if (pageType === 'content' || pageType === 'home' || pageType === 'intro'
    || template === 'blank' || template === 'intro' || template === 'custom'
    || template === 'home'
    || !node.objectRef)
    return 'custom'

  return 'custom'
}

/** 将误判的自由布局节点写回明确的 pageShape，避免下次再丢 Tab。 */
export function ensureFreeLayoutPageNode(node = null) {
  if (!node || node.type !== 'page')
    return node
  // 已绑定数据对象 / 明确是表单列表形态的页面，绝不能改成自由布局
  if (node.objectRef)
    return node
  const shape = String(node.pageShape || node.pageTemplate || '').trim().toLowerCase()
  if (['form', 'list', 'list-form', 'list_form', 'tree-list', 'tree_list', 'tree-table', 'tree-crud', 'tree_table'].includes(shape))
    return node
  if (String(node.pageType || '').trim().toLowerCase() === 'object')
    return node
  if (String(node.pageShape || '').trim().toLowerCase() === 'custom'
    && ['content', 'home', 'intro'].includes(String(node.pageType || '').trim().toLowerCase()))
    return node
  return {
    ...node,
    pageShape: 'custom',
    pageTemplate: String(node.pageTemplate || '').trim() || 'custom',
    pageType: ['content', 'home', 'intro'].includes(String(node.pageType || '').trim().toLowerCase())
      ? node.pageType
      : 'content',
  }
}

/**
 * 应用页画布是否含 CRUD 以外的伴生块（图表、指标、快捷入口等）。
 * 运行态 Portal 会渲染这些块；编辑入口必须进「页面设计」，不能只开对象列表设计器。
 */
export function pageHasFreeLayoutCompanionBlocks(page = null) {
  const layout = page?.layout?.gridLayout
  if (!layout || !Array.isArray(layout.items))
    return false
  return isRichListGridLayout(layout)
}

/**
 * 对象页（表单/列表/列表+表单/左树右表）是否需要「恢复标准布局」提示。
 *
 * 仅在真正异常时判定为污染：
 * - 仍挂 objectRef，但 pageType/pageShape 被误改成 content/custom
 * - 对象页丢失了 AiCrudPage
 *
 * 列表页上叠加图表/指标/快捷入口等自由布局组件是合法能力（市场样例也如此），
 * 不再当成污染，避免导入后误报黄条。
 */
export function isObjectBoundPageLayoutPolluted(node = null, page = null) {
  if (!node || node.type !== 'page')
    return false
  const shape = resolvePageShapeFromNode(node)
  const objectBound = Boolean(node.objectRef)
    || OBJECT_PAGE_SHAPES.has(shape)
    || String(node.pageType || '').trim().toLowerCase() === 'object'
  if (!objectBound)
    return false

  const items = Array.isArray(page?.layout?.gridLayout?.items)
    ? page.layout.gridLayout.items
    : []
  const contentItems = items.filter(item => item && item.blockType !== 'page-title')
  const hasCrud = contentItems.some(item => item.blockType === 'AiCrudPage')
  // 误写成自由布局元数据，但还挂着 objectRef
  const shapeCorrupted = Boolean(node.objectRef)
    && (String(node.pageType || '').trim().toLowerCase() === 'content'
      || String(node.pageShape || '').trim().toLowerCase() === 'custom')
  if (shapeCorrupted)
    return true
  return !hasCrud
}

/**
 * 把被自由布局污染的对象页恢复成单一 AiCrudPage（保留原表单资产与 objectRef）。
 */
export function restoreObjectBoundPageLayout(schema, pageId, options = {}) {
  const next = {
    ...schema,
    nodes: Array.isArray(schema?.nodes) ? [...schema.nodes] : [],
    pages: { ...(schema?.pages || {}) },
    formAssets: Array.isArray(schema?.formAssets) ? schema.formAssets : [],
  }
  const index = next.nodes.findIndex(item => String(item?.id) === String(pageId || ''))
  if (index < 0)
    throw new Error('页面不存在')
  const node = next.nodes[index]
  const page = next.pages[pageId] || { title: node.title, layout: {} }
  const existingItems = Array.isArray(page?.layout?.gridLayout?.items)
    ? page.layout.gridLayout.items
    : []
  const existingCrud = existingItems.find(item => item?.blockType === 'AiCrudPage')
  const shape = resolveRestorePageShape(node)
  const layoutType = resolveLayoutType(shape)
  const formAssetId = String(
    options.formAssetId
    || existingCrud?.props?.formAssetId
    || findFormAssetIdInBlocks(existingItems)
    || next.formAssets[0]?.id
    || '',
  ).trim()

  const objectRef = node.objectRef && typeof node.objectRef === 'object'
    ? {
        ...node.objectRef,
        pageMode: shape === 'form'
          ? 'form'
          : shape === 'list'
            ? 'list'
            : shape === 'tree-table'
              ? 'tree-table'
              : shape === 'tree-list'
                ? 'tree-list'
                : 'crud',
        pageKey: shape === 'form' ? 'form' : 'list',
        valid: node.objectRef.valid !== false,
      }
    : null

  const treeConfig = shape === 'tree-table'
    ? (existingCrud?.props?.treeConfig || existingCrud?.props?.options?.treeConfig || buildTreeTableConfig(node.title))
    : shape === 'tree-list'
      ? (existingCrud?.props?.treeConfig || existingCrud?.props?.options?.treeConfig || buildTreeTableConfig(node.title, { embedded: true }))
      : null

  const crudBlock = {
    ...createGridBlock('AiCrudPage', { fields: [] }, { gridX: 0, gridY: 0 }),
    ...(existingCrud?.id ? { id: existingCrud.id } : {}),
    label: node.title || existingCrud?.label || '数据列表',
    props: {
      ...(existingCrud?.props || {}),
      title: node.title || existingCrud?.props?.title || '数据列表',
      formAssetId,
      formAssetFieldsInitialized: existingCrud?.props?.formAssetFieldsInitialized === true,
      ...(objectRef ? { objectRef } : {}),
      layoutType,
      ...(shape === 'form'
        ? { formOnly: true, hideToolbar: true, hideBatchDelete: true, showSearch: false }
        : {}),
      ...(FLAT_FORM_SHAPES.has(shape)
        ? { formOpenMode: 'flat', modalType: 'flat' }
        : {}),
      ...(shape === 'tree-table'
        ? {
            enableTreeAddChild: false,
            treeConfig,
            options: {
              ...(existingCrud?.props?.options || {}),
              layoutType: 'tree-crud',
              enableTreeAddChild: false,
              treeConfig,
            },
          }
        : {}),
      ...(shape === 'tree-list'
        ? {
            enableTreeAddChild: true,
            treeConfig,
            options: {
              ...(existingCrud?.props?.options || {}),
              layoutType: 'list-form',
              enableTreeAddChild: true,
              treeConfig,
            },
          }
        : {}),
      style: {
        widthMode: 'full',
        heightMode: 'full',
        pageFlowHeight: 640,
        ...(existingCrud?.props?.style || {}),
      },
    },
  }

  next.nodes[index] = {
    ...node,
    pageType: 'object',
    pageTemplate: shape,
    pageShape: shape,
    objectRef,
  }
  next.pages[pageId] = {
    ...page,
    title: page.title || node.title,
    layout: {
      ...(page.layout || {}),
      items: [],
      gridLayout: {
        cols: 12,
        rowHeight: 32,
        gap: 8,
        designWidth: 1366,
        layoutType,
        items: [crudBlock],
      },
      pageTitleComponentInitialized: true,
    },
  }
  return next
}

function resolveRestorePageShape(node = {}) {
  const explicit = String(node.pageShape || node.pageTemplate || '').trim().toLowerCase()
  if (explicit === 'list_form')
    return 'list-form'
  if (explicit === 'tree_table' || explicit === 'tree-crud')
    return 'tree-table'
  if (explicit === 'tree_list')
    return 'tree-list'
  if (['form', 'list', 'list-form', 'tree-list', 'tree-table'].includes(explicit))
    return explicit
  const mode = String(node.objectRef?.pageMode || '').trim().toLowerCase()
  if (mode === 'form')
    return 'form'
  if (mode === 'list')
    return 'list'
  if (mode === 'tree-table' || mode === 'tree-crud')
    return 'tree-table'
  if (mode === 'tree-list')
    return 'tree-list'
  return 'list-form'
}

function findFormAssetIdInBlocks(blocks = []) {
  for (const block of blocks) {
    const id = String(block?.props?.formAssetId || '').trim()
    if (id)
      return id
    const nested = block?.props?.tabs || block?.props?.items || block?.children
    if (Array.isArray(nested)) {
      const found = findFormAssetIdInBlocks(nested)
      if (found)
        return found
    }
  }
  return ''
}

function resolvePageMode(pageType) {
  if (pageType === 'form')
    return 'form'
  if (pageType === 'list')
    return 'list'
  if (pageType === 'tree-table')
    return 'tree-table'
  if (pageType === 'tree-list')
    return 'tree-list'
  return 'crud'
}

function resolveLayoutType(pageType) {
  if (pageType === 'tree-table')
    return 'tree-crud'
  if (pageType === 'tree-list')
    return 'list-form'
  return pageType
}

function buildPageObjectRef(selection = {}, normalized = {}, pageMode = 'crud') {
  return {
    objectId: selection.objectId || null,
    objectCode: normalized.objectCode,
    objectName: normalized.objectName,
    pageKey: pageMode === 'form' ? 'form' : 'list',
    pageMode,
    configKey: selection.configKey || '',
    runtimeDatasourceId: normalized.runtimeDatasourceId,
    createMode: normalized.createMode || PAGE_OBJECT_CREATE_BLANK,
    importDatasourceId: normalized.importDatasourceId,
    importTableName: normalized.importTableName || '',
    valid: true,
  }
}

function normalizeId(value) {
  if (value === null || value === undefined || value === '')
    return null
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : value
}
