/** BusinessListDesigner.vue setup part 1. */
import {
  AddOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ChevronDownOutline,
  CloseCircleOutline,
  CopyOutline,
  DocumentTextOutline,
  EllipsisHorizontalOutline,
  FunnelOutline,
  GridOutline,
  LayersOutline,
  ListOutline,
  PrintOutline,
  RefreshOutline,
  SparklesOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NIcon as NaiveIcon, useMessage } from 'naive-ui'
import { computed, h, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { saveBusinessObjectActions, saveBusinessObjectDesigner, saveBusinessObjectListLayout } from '@/api/business-app'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  applyCrudHookRules,
  CRUD_HOOK_RULE_TARGETS,
  normalizeCrudHookRules,
} from '@/components/lowcode-builder/page/crud-hook-rules'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import {
  applyGridLayoutToZones,
  bootstrapGridLayoutFromZones,
  buildPageDesignModelSchema,
  createDefaultListGridLayout,
  createDefaultPageSchema,
  createPageModelRef,
  isPageFieldVisible,
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_BASE_COL_WIDTH,
  LIST_PAGE_GRID_GAP,
  resolveDefaultTreeConfig,
  resolveListFieldTitle,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import {
  appendDesignPreviewToApiConfig,
  appendDesignPreviewToApiValue,
} from '@/components/lowcode-builder/shared/runtime-crud-props'
import { applyEmbeddedTreeTableRuntimeProps } from '@/components/lowcode-builder/shared/runtime-tree-table'
import { createViewSchemaFromPageSchema } from '../form-first/viewSchema'
export function applyBusinessListDesignerPart1(props, emit) {
  const __impl = {}
  const mut = {
    applyingExternalSchema: false,
  }
  const buildDesignerActionConfig = (...args) => __impl.buildDesignerActionConfig(...args)
  const buildDesignerColumns = (...args) => __impl.buildDesignerColumns(...args)
  const buildDesignerEditSchema = (...args) => __impl.buildDesignerEditSchema(...args)
  const buildDesignerFieldMap = (...args) => __impl.buildDesignerFieldMap(...args)
  const buildDesignerRuntimeField = (...args) => __impl.buildDesignerRuntimeField(...args)
  const buildDesignerSearchSchema = (...args) => __impl.buildDesignerSearchSchema(...args)
  const buildDesignerSelfTreeOptionSource = (...args) => __impl.buildDesignerSelfTreeOptionSource(...args)
  const collectSchemaCustomActions = (...args) => __impl.collectSchemaCustomActions(...args)
  const deduplicateListActions = (...args) => __impl.deduplicateListActions(...args)
  const designerActionToListAction = (...args) => __impl.designerActionToListAction(...args)
  const designerActionTypeToListType = (...args) => __impl.designerActionTypeToListType(...args)
  const ensureDesignerPages = (...args) => __impl.ensureDesignerPages(...args)
  const ensureGridListSchema = (...args) => __impl.ensureGridListSchema(...args)
  const handleListDesignerShortcut = (...args) => __impl.handleListDesignerShortcut(...args)
  const hydrateDesignerFormLayout = (...args) => __impl.hydrateDesignerFormLayout(...args)
  const hydrateDesignerFormLayoutNode = (...args) => __impl.hydrateDesignerFormLayoutNode(...args)
  const inferLayoutType = (...args) => __impl.inferLayoutType(...args)
  const isInvalidDesignerApiAction = (...args) => __impl.isInvalidDesignerApiAction(...args)
  const isRelationLayout = (...args) => __impl.isRelationLayout(...args)
  const isStandaloneDesignerLayoutNode = (...args) => __impl.isStandaloneDesignerLayoutNode(...args)
  const isStandardListGridLayout = (...args) => __impl.isStandardListGridLayout(...args)
  const isTreeLayout = (...args) => __impl.isTreeLayout(...args)
  const listActionToDesignerAction = (...args) => __impl.listActionToDesignerAction(...args)
  const listActionTypeToDesignerType = (...args) => __impl.listActionTypeToDesignerType(...args)
  const mergePrimaryModelRef = (...args) => __impl.mergePrimaryModelRef(...args)
  const normalizeActionApiMethod = (...args) => __impl.normalizeActionApiMethod(...args)
  const normalizeActionCode = (...args) => __impl.normalizeActionCode(...args)
  const normalizeActionParams = (...args) => __impl.normalizeActionParams(...args)
  const normalizeDesignerActionPosition = (...args) => __impl.normalizeDesignerActionPosition(...args)
  const normalizeDesignerActionType = (...args) => __impl.normalizeDesignerActionType(...args)
  const normalizeDesignerPage = (...args) => __impl.normalizeDesignerPage(...args)
  const normalizeDesignerRuntimeFieldType = (...args) => __impl.normalizeDesignerRuntimeFieldType(...args)
  const normalizeListActionConfig = (...args) => __impl.normalizeListActionConfig(...args)
  const normalizeListActionPosition = (...args) => __impl.normalizeListActionPosition(...args)
  const normalizeListActionType = (...args) => __impl.normalizeListActionType(...args)
  const normalizeListCustomAction = (...args) => __impl.normalizeListCustomAction(...args)
  const normalizeListCustomActions = (...args) => __impl.normalizeListCustomActions(...args)
  const normalizeSchemaForSave = (...args) => __impl.normalizeSchemaForSave(...args)
  const omitTreeRuntimeProps = (...args) => __impl.omitTreeRuntimeProps(...args)
  const openLocalPreview = (...args) => __impl.openLocalPreview(...args)
  const parsePlainObject = (...args) => __impl.parsePlainObject(...args)
  const pushHistorySnapshot = (...args) => __impl.pushHistorySnapshot(...args)
  const redoSchema = (...args) => __impl.redoSchema(...args)
  const resolveDefaultActionButtonType = (...args) => __impl.resolveDefaultActionButtonType(...args)
  const resolveDefaultListButtonType = (...args) => __impl.resolveDefaultListButtonType(...args)
  const resolveDesignModelSchema = (...args) => __impl.resolveDesignModelSchema(...args)
  const resolveDesignerDefaultApiValues = (...args) => __impl.resolveDesignerDefaultApiValues(...args)
  const resolveDesignerZoneRefs = (...args) => __impl.resolveDesignerZoneRefs(...args)
  const resolveLayoutModeLabel = (...args) => __impl.resolveLayoutModeLabel(...args)
  const resolveListActionRoutePath = (...args) => __impl.resolveListActionRoutePath(...args)
  const resolveSchema = (...args) => __impl.resolveSchema(...args)
  const setLocalSchema = (...args) => __impl.setLocalSchema(...args)
  const syncDesignerDraft = (...args) => __impl.syncDesignerDraft(...args)
  const toDesignerActionParams = (...args) => __impl.toDesignerActionParams(...args)
  const toPageField = (...args) => __impl.toPageField(...args)
  const undoSchema = (...args) => __impl.undoSchema(...args)
  const updateDesignerPageGrid = (...args) => __impl.updateDesignerPageGrid(...args)
  const updateTreeZone = (...args) => __impl.updateTreeZone(...args)
  const message = useMessage()
  const saving = ref(false)
  const undoStack = ref([])
  const redoStack = ref([])
  const listGridDesignerRef = ref(null)
  const activePageKey = ref('list')
  const pageSettingsExpanded = ref(false)
  const HISTORY_LIMIT = 50

  const VALID_PAGE_TYPES = new Set(['list', 'create', 'edit', 'dialog', 'drawer', 'custom'])

  const baseModelSchema = computed(() => {
    const modelFields = props.modelSchema?.fields || []
    return {
      ...(props.modelSchema || {}),
      fields: modelFields.length ? modelFields : props.fields.map(toPageField),
    }
  })

  // Placeholder only — real normalize needs part2 helpers (resolveSchema / resolveDesignModelSchema).
  // hydrateInitialState() runs after part2 wires __impl, before first paint.
  const localSchema = ref(cloneSchema(props.modelValue || createDefaultPageSchema(props.modelSchema || {})))
  const pendingModelSchema = ref(null)
  const workingModelSchema = computed(() => pendingModelSchema.value || baseModelSchema.value)
  const effectiveModelSchema = computed(() => resolveDesignModelSchema(localSchema.value, workingModelSchema.value))
  const designFields = computed(() => effectiveModelSchema.value.fields || [])
  const layoutModeLabel = computed(() => resolveLayoutModeLabel(localSchema.value.layoutType))
  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)
  const listPreviewVisible = ref(false)
  const listPreviewLayout = ref(null)
  const listPreviewLayoutKey = ref(0)
  const templateSelectValue = ref(resolveTemplateSelectValue(localSchema.value?.layoutType))
  const listCustomActions = ref([])

  watch(
    () => props.modelSchema,
    (value) => {
      if (!pendingModelSchema.value)
        return
      if (isSameSchema(value || {}, pendingModelSchema.value))
        pendingModelSchema.value = null
    },
    { deep: true },
  )
  const pageTypeOptions = [
    { label: '列表页', value: 'list' },
    { label: '新增页', value: 'create' },
    { label: '编辑页', value: 'edit' },
    { label: '弹窗页', value: 'dialog' },
    { label: '抽屉页', value: 'drawer' },
    { label: '自定义页', value: 'custom' },
  ]
  const listTemplateOptions = [
    { label: '标准列表模板', key: 'simple-crud', icon: renderMenuIcon(ListOutline) },
    { label: '左树右表模板', key: 'tree-crud', icon: renderMenuIcon(GridOutline) },
  ]
  const activeTemplateLabel = computed(() => {
    return listTemplateOptions.find(item => item.key === templateSelectValue.value)?.label || '列表模板'
  })
  const listMoreOptions = computed(() => [
    {
      label: '按字段生成查询条件',
      key: 'resetSearchFields',
      icon: renderMenuIcon(FunnelOutline),
    },
    {
      label: '按字段生成表格列',
      key: 'resetTableFields',
      icon: renderMenuIcon(GridOutline),
    },
    {
      type: 'divider',
      key: 'listMoreDivider',
    },
    {
      label: '新增空白页面',
      key: 'addPage',
      icon: renderMenuIcon(AddOutline),
    },
    {
      label: '打印列表',
      key: 'printDesign',
      icon: renderMenuIcon(PrintOutline),
    },
    {
      type: 'divider',
      key: 'pageDivider',
    },
    {
      label: '重置当前列表',
      key: 'resetListSchema',
      icon: renderMenuIcon(RefreshOutline),
    },
  ])
  const pageActionOptions = computed(() => [
    { label: '按字段生成查询条件', key: 'resetSearchFields', icon: renderMenuIcon(FunnelOutline) },
    { label: '按字段生成表格列', key: 'resetTableFields', icon: renderMenuIcon(GridOutline) },
    { type: 'divider', key: 'pageActionDivider1' },
    { label: '新增空白页面', key: 'addPage', icon: renderMenuIcon(AddOutline) },
    { type: 'divider', key: 'pageActionDivider2' },
    { label: '复制页面', key: 'duplicate', icon: renderMenuIcon(CopyOutline) },
    { label: '重置布局', key: 'reset', icon: renderMenuIcon(SparklesOutline) },
    { label: '重置当前列表', key: 'resetListSchema', icon: renderMenuIcon(RefreshOutline) },
  ])
  const designerPages = computed(() => localSchema.value.pages || [])
  const actionPageOptions = computed(() => {
    const appPages = Array.isArray(props.applicationPages) ? props.applicationPages : []
    if (appPages.length) {
      return appPages.map(page => ({
        pageKey: String(page.pageKey || page.id || ''),
        pageName: page.pageName || page.title || page.name || String(page.pageKey || page.id || ''),
        pageType: page.pageType || page.type || 'custom',
      })).filter(page => page.pageKey)
    }
    return designerPages.value
  })
  const activeDesignerPage = computed(() => designerPages.value.find(page => page.pageKey === activePageKey.value) || designerPages.value[0] || null)
  const currentPageGridLayout = computed(() => activeDesignerPage.value?.gridLayout || localSchema.value.listGridLayout || {})
  const previewGridLayout = computed(() => {
    const persistedGrid = currentPageGridLayout.value || localSchema.value.listGridLayout
    const source = Array.isArray(persistedGrid?.items)
      ? persistedGrid
      : bootstrapGridLayoutFromZones(localSchema.value.zones || [], effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
    return syncGridLayoutWithModel(source, effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
  })
  function filterDesignerGridBlocks(grid) {
    if (!grid || !Array.isArray(grid.items))
      return grid
    return {
      ...grid,
      items: grid.items.filter(item => item.blockType !== 'page-title'),
    }
  }
  const designerGridLayout = computed(() => filterDesignerGridBlocks(currentPageGridLayout.value))
  const designerPreviewGridLayout = computed(() => filterDesignerGridBlocks(previewGridLayout.value))
  const pageWatermark = computed(() => {
    const raw = activeDesignerPage.value?.pageWatermark || localSchema.value?.pageWatermark || {}
    return {
      enabled: raw.enabled === true,
      text: String(raw.text || '').trim().slice(0, 50),
    }
  })
  const pageWatermarkContent = computed(() => pageWatermark.value.text || '内部资料')
  const visibleListCustomActions = computed(() => props.defaultViewOnly ? [] : listCustomActions.value)
  const designerRuntimeCrudProps = computed(() => buildDesignerRuntimeCrudProps(localSchema.value, designFields.value, visibleListCustomActions.value))

  function resolveTemplateSelectValue(layoutType = '') {
    return layoutType === 'tree-crud' ? 'tree-crud' : 'simple-crud'
  }

  function renderMenuIcon(icon) {
    if (!icon)
      return undefined
    return () => h(NaiveIcon, null, { default: () => h(icon) })
  }

  watch(
    () => props.designerActions,
    (value) => {
      const mappedActions = normalizeDesignerActionsForList(value)
      const fallbackActions = mappedActions.length ? mappedActions : collectSchemaCustomActions(localSchema.value)
      if (!isSameSchema(fallbackActions, listCustomActions.value))
        listCustomActions.value = fallbackActions
    },
    { deep: true },
  )

  watch(
    () => props.modelValue,
    (value) => {
      const next = resolveSchema(value, resolveDesignModelSchema(value, baseModelSchema.value))
      setLocalSchema(next, { external: true })
    },
    { deep: true },
  )

  watch(
    baseModelSchema,
    (value) => {
      const next = resolveSchema(localSchema.value, resolveDesignModelSchema(localSchema.value, value))
      setLocalSchema(next)
    },
    { deep: true },
  )

  watch(
    localSchema,
    (value) => {
      if (mut.applyingExternalSchema) {
        mut.applyingExternalSchema = false
        return
      }
      if (!isSameSchema(value, props.modelValue)) {
        emit('update:modelValue', cloneSchema(value))
        emit('update:viewSchema', cloneSchema(buildCurrentViewSchema(value)))
        emit('dirtyChange', true)
      }
    },
    { deep: true },
  )

  watch(
    () => localSchema.value.layoutType,
    (value) => {
      templateSelectValue.value = resolveTemplateSelectValue(value)
    },
    { immediate: true },
  )

  function createCleanTemplateGridLayout(layoutType, schema = localSchema.value) {
    const defaultGrid = createDefaultListGridLayout(effectiveModelSchema.value, { layoutType })
    const listPage = (schema.pages || []).find(page => page.pageKey === 'list')
    const previousGrid = listPage?.gridLayout || schema.listGridLayout || {}
    const previousItems = previousGrid.items || []
    const zones = schema.zones || []
    const searchZone = zones.find(zone => zone.zoneKey === 'search') || {}
    const tableZone = zones.find(zone => zone.zoneKey === 'table') || {}
    const previousCrud = previousItems.find(item => item.blockType === 'AiCrudPage')
      || previousItems.find(item => item.blockType === 'data-table')
      || previousItems.find(item => item.blockType === 'AiTable')
    const previousTree = previousItems.find(item => item.blockType === 'tree-panel')
    const previousToolbar = previousItems.find(item => item.blockType === 'toolbar')
    const tableProps = tableZone.props || {}
    const searchProps = searchZone.props || {}
    const toolbarActions = previousToolbar?.props?.actions || []
    const hasPreviousToolbar = Boolean(previousToolbar)
    const templateBlockTypes = new Set(['tree-panel', 'AiCrudPage', 'data-table', 'AiTable', 'search-form', 'toolbar'])

    const items = defaultGrid.items.map((item) => {
      if (item.blockType === 'tree-panel') {
        // 禁止把 table.zone treeConfig 整包盖到 tree-panel 上：
        // zone 里常残留当前列表对象的空/旧来源，会冲掉用户已选的外部树源。
        const previousProps = previousTree?.props || {}
        return {
          ...item,
          props: {
            ...(item.props || {}),
            ...previousProps,
            style: item.props?.style,
            events: previousProps.events || item.props?.events || [],
          },
        }
      }
      if (item.blockType === 'AiCrudPage') {
        const previousProps = previousCrud?.props || {}
        const normalizedTableProps = layoutType === 'tree-crud'
          ? tableProps
          : omitTreeRuntimeProps(tableProps)
        const normalizedPreviousProps = layoutType === 'tree-crud'
          ? previousProps
          : omitTreeRuntimeProps(previousProps)
        const fieldRefs = previousCrud?.fieldRefs?.length
          ? previousCrud.fieldRefs
          : tableZone.fieldRefs?.length
            ? tableZone.fieldRefs
            : item.fieldRefs
        const hasPreviousSearchRefs = Object.prototype.hasOwnProperty.call(normalizedPreviousProps, 'searchFieldRefs')
        const hasSearchZoneRefs = Array.isArray(searchZone.fieldRefs)
        const searchFieldRefs = hasPreviousSearchRefs
          ? normalizedPreviousProps.searchFieldRefs || []
          : hasSearchZoneRefs
            ? searchZone.fieldRefs || []
            : item.props?.searchFieldRefs || []
        return {
          ...item,
          fieldRefs,
          props: {
            ...(item.props || {}),
            ...normalizedTableProps,
            ...normalizedPreviousProps,
            title: normalizedPreviousProps.title || normalizedTableProps.title || item.props?.title,
            showSearch: searchZone.enabled !== false,
            showImport: normalizedPreviousProps.showImport ?? normalizedTableProps.showImport ?? (hasPreviousToolbar ? toolbarActions.includes('import') : item.props?.showImport ?? true),
            showExport: normalizedPreviousProps.showExport ?? normalizedTableProps.showExport ?? (hasPreviousToolbar ? toolbarActions.includes('export') : item.props?.showExport ?? true),
            hideBatchDelete: normalizedPreviousProps.hideBatchDelete ?? normalizedTableProps.hideBatchDelete ?? (hasPreviousToolbar ? !toolbarActions.includes('batch-delete') : item.props?.hideBatchDelete ?? false),
            enableCustomQuery: normalizedPreviousProps.enableCustomQuery ?? normalizedTableProps.enableCustomQuery ?? (hasPreviousToolbar ? toolbarActions.includes('custom-query') : item.props?.enableCustomQuery ?? true),
            defaultSortField: normalizedPreviousProps.defaultSortField || normalizedTableProps.defaultSortField || 'id',
            defaultSortOrder: normalizedPreviousProps.defaultSortOrder || normalizedTableProps.defaultSortOrder || 'desc',
            fieldSettings: {
              ...(normalizedTableProps.fieldSettings || {}),
              ...(normalizedPreviousProps.fieldSettings || {}),
            },
            searchFieldRefs,
            searchFieldSettings: {
              ...(searchProps.fieldSettings || {}),
              ...(normalizedPreviousProps.searchFieldSettings || {}),
            },
            style: item.props?.style,
            events: normalizedPreviousProps.events || item.props?.events || [],
          },
        }
      }
      return item
    })
    const preservedCustomItems = previousItems
      .filter(item => !templateBlockTypes.has(item.blockType))
      .map((item) => {
        if (layoutType !== 'tree-crud')
          return item
        const gridX = Number(item.gridX || 0)
        const styleX = Number(item.props?.style?.x ?? gridX * (LIST_PAGE_GRID_BASE_COL_WIDTH + LIST_PAGE_GRID_GAP))
        if (gridX >= 3 && styleX >= 3 * (LIST_PAGE_GRID_BASE_COL_WIDTH + LIST_PAGE_GRID_GAP))
          return item
        return {
          ...item,
          gridX: 3,
          gridW: Math.min(9, Math.max(1, Number(item.gridW || 9))),
          props: {
            ...(item.props || {}),
            style: {
              ...(item.props?.style || {}),
              x: Math.max(3 * (LIST_PAGE_GRID_BASE_COL_WIDTH + LIST_PAGE_GRID_GAP), Number(item.props?.style?.x || 0)),
              widthMode: item.props?.style?.widthMode === 'fixed' ? 'fixed' : item.props?.style?.widthMode,
            },
          },
        }
      })

    return syncGridLayoutWithModel(
      {
        ...defaultGrid,
        items: [...items, ...preservedCustomItems],
      },
      effectiveModelSchema.value,
      { layoutType },
    )
  }

  function updateTreeLayoutEnabled(enabled) {
    const nextLayoutType = enabled
      ? 'tree-crud'
      : isRelationLayout(localSchema.value, effectiveModelSchema.value) ? 'master-detail-crud' : 'simple-crud'
    const next = {
      ...localSchema.value,
      layoutType: nextLayoutType,
      zones: updateTreeZone(localSchema.value.zones || [], enabled),
    }

    const templateGrid = createCleanTemplateGridLayout(nextLayoutType, next)
    next.listGridLayout = templateGrid
    next.pages = updateDesignerPageGrid(next.pages || [], 'list', templateGrid)
    next.zones = applyGridLayoutToZones(next.zones || [], templateGrid, effectiveModelSchema.value)

    setLocalSchema(resolveSchema(next, effectiveModelSchema.value))
    // 切到左树右表时清掉误留的本表 TREE 配置，避免草稿保存卡 parentId
    if (enabled)
      clearEmbeddedTreeForLeftTreeLayout()
  }

  function clearEmbeddedTreeForLeftTreeLayout() {
    const model = effectiveModelSchema.value || {}
    const treeEnabled = model.treeConfig?.enabled === true || model.appType === 'TREE'
    if (!treeEnabled && model.treeConfig?.enableTreeAddChild !== true) {
      // 仍要清掉列表区块上残留的 enableTreeAddChild，否则运行态会继续显示「添加下级」
      clearTreeAddChildOnListBlocks()
      return
    }
    const nextModel = {
      ...model,
      appType: model.appType === 'TREE' ? 'SINGLE' : (model.appType || 'SINGLE'),
      treeConfig: {
        ...(model.treeConfig || {}),
        enabled: false,
        enableTreeAddChild: false,
      },
    }
    emit('update:modelSchema', cloneSchema(nextModel))
    clearTreeAddChildOnListBlocks()
  }

  function clearTreeAddChildOnListBlocks() {
    const nextSchema = cloneSchema(localSchema.value || {})
    const clearBlock = (block) => {
      if (!block || typeof block !== 'object')
        return block
      const next = { ...block }
      if (['AiCrudPage', 'data-table', 'AiTable'].includes(block.blockType)) {
        next.props = {
          ...(block.props || {}),
          enableTreeAddChild: false,
        }
      }
      if (Array.isArray(block.children))
        next.children = block.children.map(clearBlock)
      return next
    }
    const clearGrid = (grid) => {
      if (!grid || typeof grid !== 'object')
        return grid
      return {
        ...grid,
        items: Array.isArray(grid.items) ? grid.items.map(clearBlock) : [],
      }
    }
    nextSchema.listGridLayout = clearGrid(nextSchema.listGridLayout)
    nextSchema.pages = Array.isArray(nextSchema.pages)
      ? nextSchema.pages.map(page => ({
          ...page,
          gridLayout: clearGrid(page.gridLayout),
        }))
      : nextSchema.pages
    nextSchema.zones = updateTreeZone(
      nextSchema.zones || [],
      nextSchema.layoutType === 'tree-crud',
      effectiveModelSchema.value || {},
    )
    setLocalSchema(resolveSchema(nextSchema, effectiveModelSchema.value))
  }

  function updateListTemplate(value) {
    if (!value)
      return
    templateSelectValue.value = value
    updateTreeLayoutEnabled(value === 'tree-crud')
  }

  function handleGridLayoutUpdate(layout) {
    const originalGrid = currentPageGridLayout.value
    const pageTitleBlocks = Array.isArray(originalGrid?.items)
      ? originalGrid.items.filter(item => item.blockType === 'page-title')
      : []
    const mergedLayout = pageTitleBlocks.length
      ? { ...layout, items: [...pageTitleBlocks, ...(layout.items || [])] }
      : layout
    const synced = syncGridLayoutWithModel(mergedLayout, effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
    const pageKey = activePageKey.value || 'list'
    const pages = updateDesignerPageGrid(localSchema.value.pages || [], pageKey, synced)
    const nextSchema = {
      ...localSchema.value,
      pages,
    }
    if (pageKey === 'list') {
      nextSchema.listGridLayout = synced
      nextSchema.zones = applyGridLayoutToZones(localSchema.value.zones || [], synced, effectiveModelSchema.value)
    }
    setLocalSchema(nextSchema)
  }

  function handleGridModelSchemaUpdate(modelSchema) {
    const nextModel = cloneSchema(modelSchema || {})
    pendingModelSchema.value = nextModel
    emit('update:modelSchema', nextModel)
    const enabled = nextModel.treeConfig?.enabled === true || nextModel.appType === 'TREE'
    // 启用嵌入式树形时保持当前列表模板，不要切到左树右表（否则画布会右移）
    const keepLayoutType = localSchema.value.layoutType === 'tree-crud'
      ? 'tree-crud'
      : (localSchema.value.layoutType || 'simple-crud')
    const nextPage = {
      ...localSchema.value,
      layoutType: keepLayoutType,
      zones: updateTreeZone(localSchema.value.zones || [], enabled, nextModel),
    }
    setLocalSchema(resolveSchema(nextPage, nextModel))
    emit('dirtyChange', true)
  }

  function switchActivePage(pageKey) {
    activePageKey.value = pageKey || 'list'
  }

  function handleListMoreSelect(key = '') {
    if (key === 'resetSearchFields') {
      resetZoneFields('search')
      return
    }
    if (key === 'resetTableFields') {
      resetZoneFields('table')
      return
    }
    if (key === 'addPage') {
      addDesignerPage()
      return
    }
    if (key === 'printDesign') {
      openPrintPreview()
      return
    }
    if (key === 'resetListSchema')
      resetListSchema()
  }

  /** 打开预览并触发浏览器打印，像 Word 一样输出当前列表设计稿 */
  function openPrintPreview() {
    openLocalPreview()
    setTimeout(() => window.print(), 400)
  }

  function handlePageActionSelect(key = '') {
    if (['resetSearchFields', 'resetTableFields', 'addPage', 'resetListSchema'].includes(key)) {
      handleListMoreSelect(key)
      return
    }
    if (key === 'duplicate') {
      duplicateActivePage()
      return
    }
    if (key === 'reset') {
      resetActivePageLayout()
    }
  }

  function addDesignerPage() {
    const pageKey = createUniquePageKey('page')
    const page = {
      pageKey,
      pageName: `页面 ${designerPages.value.length + 1}`,
      pageType: 'custom',
      routePath: '',
      description: '',
      params: [],
      gridLayout: {
        ...createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: localSchema.value.layoutType }),
        items: [],
      },
    }
    setLocalSchema({
      ...localSchema.value,
      pages: [...(localSchema.value.pages || []), page],
    })
    activePageKey.value = pageKey
  }

  function duplicateActivePage() {
    const source = activeDesignerPage.value
    if (!source)
      return
    const pageKey = createUniquePageKey(source.pageKey || 'page')
    const copy = cloneSchema(source)
    copy.pageKey = pageKey
    copy.pageName = `${source.pageName || '页面'} 副本`
    copy.pageType = source.pageType === 'list' ? 'custom' : normalizeDesignerPageType(source.pageType || 'custom')
    copy.routePath = ''
    setLocalSchema({
      ...localSchema.value,
      pages: [...(localSchema.value.pages || []), copy],
    })
    activePageKey.value = pageKey
  }

  function removeActivePage() {
    if (isProtectedPage(activePageKey.value))
      return
    const removedKey = activePageKey.value
    const nextPages = (localSchema.value.pages || []).filter(page => page.pageKey !== activePageKey.value)
    const nextActive = nextPages.find(page => page.pageKey === 'list')?.pageKey || nextPages[0]?.pageKey || 'list'
    setLocalSchema({
      ...localSchema.value,
      pages: nextPages,
      removedPageKeys: Array.from(new Set([...(localSchema.value.removedPageKeys || []), removedKey])),
    })
    activePageKey.value = nextActive
  }

  function resetActivePageLayout() {
    const page = activeDesignerPage.value
    if (!page)
      return
    const gridLayout = page.pageKey === 'list' || page.pageType === 'list'
      ? createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: localSchema.value.layoutType })
      : {
          ...createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: localSchema.value.layoutType }),
          items: [],
        }
    const pages = updateDesignerPageGrid(localSchema.value.pages || [], page.pageKey, gridLayout)
    const nextSchema = {
      ...localSchema.value,
      pages,
    }
    if (page.pageKey === 'list') {
      nextSchema.listGridLayout = gridLayout
      nextSchema.zones = applyGridLayoutToZones(localSchema.value.zones || [], gridLayout, effectiveModelSchema.value)
    }
    setLocalSchema(nextSchema)
  }

  function clearActivePageLayout() {
    const page = activeDesignerPage.value
    if (!page)
      return
    const gridLayout = {
      ...(page.gridLayout || {}),
      cols: 12,
      rowHeight: 32,
      gap: 8,
      designWidth: page.gridLayout?.designWidth || LIST_PAGE_DESIGN_WIDTH,
      layoutType: localSchema.value.layoutType,
      items: [],
    }
    const pages = updateDesignerPageGrid(localSchema.value.pages || [], page.pageKey, gridLayout)
    const nextSchema = {
      ...localSchema.value,
      pages,
    }
    if (page.pageKey === 'list') {
      nextSchema.listGridLayout = gridLayout
      nextSchema.zones = applyGridLayoutToZones(localSchema.value.zones || [], gridLayout, effectiveModelSchema.value)
    }
    setLocalSchema(nextSchema)
  }

  function patchActivePage(patch) {
    const page = activeDesignerPage.value
    if (!page)
      return
    setLocalSchema({
      ...localSchema.value,
      pages: (localSchema.value.pages || []).map(item => item.pageKey === page.pageKey
        ? { ...item, ...patch }
        : item),
    })
  }

  function patchPageWatermark(partial = {}) {
    const next = {
      ...pageWatermark.value,
      ...partial,
      text: Object.prototype.hasOwnProperty.call(partial, 'text')
        ? String(partial.text || '').trim().slice(0, 50)
        : pageWatermark.value.text,
      enabled: Object.prototype.hasOwnProperty.call(partial, 'enabled')
        ? partial.enabled === true
        : pageWatermark.value.enabled,
    }
    patchActivePage({ pageWatermark: next })
  }

  function updateActivePageKey(value) {
    const page = activeDesignerPage.value
    if (!page || isProtectedPage(page.pageKey))
      return
    const nextKey = normalizePageKey(value)
    if (!nextKey || nextKey === page.pageKey)
      return
    if ((localSchema.value.pages || []).some(item => item.pageKey === nextKey)) {
      message.warning('页面编码已存在')
      return
    }
    const rewrittenSchema = rewritePageEventTargets(localSchema.value, page.pageKey, nextKey)
    const pages = (rewrittenSchema.pages || []).map(item => item.pageKey === page.pageKey
      ? { ...item, pageKey: nextKey }
      : item)
    setLocalSchema({
      ...rewrittenSchema,
      pages,
    })
    activePageKey.value = nextKey
  }

  function addActivePageParam() {
    const page = activeDesignerPage.value
    if (!page)
      return
    patchActivePage({
      params: [...(page.params || []), { name: '', value: '' }],
    })
  }

  function updateActivePageParam(paramIdx, patch) {
    const page = activeDesignerPage.value
    if (!page)
      return
    const params = [...(page.params || [])]
    params[paramIdx] = { ...(params[paramIdx] || {}), ...patch }
    patchActivePage({ params })
  }

  function removeActivePageParam(paramIdx) {
    const page = activeDesignerPage.value
    if (!page)
      return
    const params = [...(page.params || [])]
    params.splice(paramIdx, 1)
    patchActivePage({ params })
  }

  function createUniquePageKey(prefix = 'page') {
    const base = normalizePageKey(prefix) || 'page'
    const existing = new Set((localSchema.value.pages || []).map(page => page.pageKey))
    let index = 1
    let key = `${base}_${Date.now()}`
    while (existing.has(key)) {
      key = `${base}_${Date.now()}_${index}`
      index += 1
    }
    return key
  }

  function normalizePageKey(value) {
    return String(value || '')
      .trim()
      .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
      .replace(/\W/g, '_')
      .replace(/_+/g, '_')
      .toLowerCase()
      .replace(/^[^a-z]+/, '')
  }

  function normalizePageParamName(value) {
    return String(value || '')
      .trim()
      .replace(/[^\w.-]/g, '')
  }

  function isProtectedPage(pageKey = '') {
    return pageKey === 'list'
  }

  function pageTypeText(pageType = 'custom') {
    return pageTypeOptions.find(item => item.value === pageType)?.label || '自定义页'
  }

  function normalizeDesignerPageType(pageType = 'custom') {
    const value = String(pageType || 'custom')
    return VALID_PAGE_TYPES.has(value) ? value : 'custom'
  }

  function rewritePageEventTargets(schema, oldKey, nextKey) {
    const cloned = cloneSchema(schema)
    cloned.pages = (cloned.pages || []).map(page => ({
      ...page,
      gridLayout: rewriteGridLayoutPageEventTargets(page.gridLayout, oldKey, nextKey),
    }))
    cloned.listGridLayout = rewriteGridLayoutPageEventTargets(cloned.listGridLayout, oldKey, nextKey)
    return cloned
  }

  function rewriteGridLayoutPageEventTargets(gridLayout, oldKey, nextKey) {
    if (!gridLayout)
      return gridLayout
    return {
      ...gridLayout,
      items: rewriteBlocksPageEventTargets(gridLayout.items || [], oldKey, nextKey),
    }
  }

  function rewriteBlocksPageEventTargets(blocks = [], oldKey, nextKey) {
    return blocks.map((block) => {
      const props = { ...(block.props || {}) }
      if (Array.isArray(props.events)) {
        props.events = props.events.map(event => event.targetPageKey === oldKey
          ? { ...event, targetPageKey: nextKey }
          : event)
      }
      if (props.fieldSettings && typeof props.fieldSettings === 'object') {
        props.fieldSettings = Object.fromEntries(Object.entries(props.fieldSettings).map(([fieldKey, setting]) => [
          fieldKey,
          setting?.targetPageKey === oldKey ? { ...setting, targetPageKey: nextKey } : setting,
        ]))
      }
      if (Array.isArray(props.tabs)) {
        props.tabs = props.tabs.map(tab => ({
          ...tab,
          children: rewriteBlocksPageEventTargets(tab.children || [], oldKey, nextKey),
        }))
      }
      return {
        ...block,
        props,
        children: rewriteBlocksPageEventTargets(block.children || [], oldKey, nextKey),
      }
    })
  }

  function resetZoneFields(zoneKey = '') {
    const nextRefs = designFields.value
      .filter(field => isPageFieldVisible(field, zoneKey))
      .map(field => field.field)
    const targetBlockTypes = zoneKey === 'search'
      ? ['search-form', 'AiCrudPage']
      : ['data-table', 'AiTable', 'AiCrudPage']
    const nextGridLayout = {
      ...(localSchema.value.listGridLayout || {}),
      items: (localSchema.value.listGridLayout?.items || []).map((item) => {
        if (!targetBlockTypes.includes(item.blockType))
          return item
        if (zoneKey === 'search' && item.blockType === 'AiCrudPage') {
          return {
            ...item,
            props: {
              ...(item.props || {}),
              searchFieldRefs: nextRefs,
            },
          }
        }
        return { ...item, fieldRefs: nextRefs }
      }),
    }
    setLocalSchema({
      ...localSchema.value,
      listLayoutMode: 'grid',
      listGridLayout: nextGridLayout,
      pages: updateDesignerPageGrid(localSchema.value.pages || [], 'list', nextGridLayout),
      zones: applyGridLayoutToZones(localSchema.value.zones || [], nextGridLayout, effectiveModelSchema.value),
    })
  }

  function resetListSchema() {
    const nextLayoutType = localSchema.value.layoutType
    const nextSchema = resolveSchema(createDefaultPageSchema(effectiveModelSchema.value), effectiveModelSchema.value)
    const nextZones = nextLayoutType === 'tree-crud'
      ? updateTreeZone(nextSchema.zones || [], true)
      : nextSchema.zones
    const nextGridLayout = syncGridLayoutWithModel(createDefaultListGridLayout(effectiveModelSchema.value, { layoutType: nextLayoutType }), effectiveModelSchema.value, { layoutType: nextLayoutType })
    setLocalSchema({
      ...nextSchema,
      layoutType: nextLayoutType,
      zones: applyGridLayoutToZones(nextZones || [], nextGridLayout, effectiveModelSchema.value),
      listLayoutMode: 'grid',
      listGridLayout: nextGridLayout,
      pages: ensureDesignerPages({ ...nextSchema, listGridLayout: nextGridLayout, layoutType: nextLayoutType }, effectiveModelSchema.value),
    })
  }

  async function saveLayout() {
    if (!props.objectId)
      return
    // 保存前刷掉画布延迟 emit，避免落盘缺少刚拖入的伴生块
    listGridDesignerRef.value?.flushDeferredLayoutEmit?.()
    const schema = normalizeSchemaForSave(resolveSchema(localSchema.value, effectiveModelSchema.value))
    const designerActions = props.defaultViewOnly
      ? cloneSchema(props.designerActions || [])
      : buildDesignerActionsForSave()
    const invalidApiAction = props.defaultViewOnly
      ? null
      : designerActions.find(action => action.status !== 0 && isInvalidDesignerApiAction(action))
    if (invalidApiAction) {
      const actionName = invalidApiAction.actionName || '自定义操作'
      message.warning(`请为“${actionName}”配置接口地址或能力标识`)
      throw new Error(`请为“${actionName}”配置接口地址或能力标识`)
    }
    saving.value = true
    try {
      if (!props.defaultViewOnly)
        await saveBusinessObjectActions(props.objectId, designerActions)
      await saveBusinessObjectListLayout(props.objectId, {
        layoutKey: 'list',
        layoutName: '列表布局',
        layoutType: schema.layoutType,
        pageSchema: cloneSchema(schema),
        zones: schema.zones?.filter(zone => ['search', 'table'].includes(zone.zoneKey)) || [],
        settings: {
          listLayoutMode: 'grid',
        },
      })
      const viewSchema = buildCurrentViewSchema(schema)
      const designerPayload = {
        pageSchema: cloneSchema(schema),
        viewSchema: cloneSchema(viewSchema),
        modelSchema: cloneSchema(workingModelSchema.value || {}),
      }
      if (!props.defaultViewOnly) {
        designerPayload.designerOptions = cloneSchema({
          ...(props.designerOptions || {}),
          actions: designerActions,
        })
      }
      await saveBusinessObjectDesigner(props.objectId, designerPayload)
      setLocalSchema(schema, { external: true })
      emit('update:viewSchema', cloneSchema(viewSchema))
      if (!props.defaultViewOnly)
        emit('update:designerActions', cloneSchema(designerActions))
      emit('saved', cloneSchema(schema))
      emit('dirtyChange', false)
      message.success('列表布局已保存')
      return schema
    }
    catch (error) {
      message.error(error?.message || '列表布局保存失败')
      throw error
    }
    finally {
      saving.value = false
    }
  }

  function buildCurrentViewSchema(schema = localSchema.value) {
    return createViewSchemaFromPageSchema(normalizeSchemaForSave(schema), designFields.value, props.viewSchema || {})
  }

  function buildDesignerRuntimeCrudProps(schema = {}, fields = [], customActions = []) {
    const zones = schema.zones || []
    const searchZone = zones.find(zone => zone.zoneKey === 'search') || {}
    const tableZone = zones.find(zone => zone.zoneKey === 'table') || {}
    const editZone = zones.find(zone => zone.zoneKey === 'edit') || {}
    const tableProps = tableZone.props || {}
    const searchProps = searchZone.props || {}
    const editProps = editZone.props || {}
    const formLayout = schema.layout || {}
    const apiValues = resolveDesignerDefaultApiValues(schema)
    const fieldMap = buildDesignerFieldMap(fields)
    const editFields = buildDesignerEditSchema(editZone, fieldMap)
    const hookHandlers = buildDesignerCrudHookHandlers(tableProps)
    const runtimeActions = normalizeListCustomActions(customActions)
    const resolvedFormOpenMode = resolveDesignerFormOpenMode(formLayout, tableProps, editProps)
    const resolvedModalType = resolveDesignerModalType(resolvedFormOpenMode, formLayout, tableProps, editProps)
    const modelSchema = effectiveModelSchema.value || {}
    const modelTreeConfig = modelSchema.treeConfig || {}
    const zoneTreeConfig = tableProps.treeConfig || {}
    const treeEnabled = schema.layoutType !== 'tree-crud'
      && (modelTreeConfig.enabled === true || modelSchema.appType === 'TREE' || zoneTreeConfig.enabled === true)
    const treeConfig = treeEnabled
      ? {
          ...resolveDefaultTreeConfig(modelSchema, {
            ...zoneTreeConfig,
            ...modelTreeConfig,
            enabled: true,
          }),
          enabled: true,
        }
      : null
    const crudBlock = (schema.listGridLayout?.items || schema.pages?.find(page => page?.pageKey === 'list')?.gridLayout?.items || [])
      .find(item => ['AiCrudPage', 'data-table', 'AiTable'].includes(item?.blockType))
    const treePanelBlock = (schema.listGridLayout?.items || schema.pages?.find(page => page?.pageKey === 'list')?.gridLayout?.items || [])
      .find(item => item?.blockType === 'tree-panel')
    const treePanelApi = treePanelBlock?.props?.treeApi
      || (treePanelBlock?.props?.sourceConfigKey
        ? `get@/ai/crud/${treePanelBlock.props.sourceConfigKey}/tree`
        : (treePanelBlock?.props?.sourceModelCode
            ? `get@/ai/crud/${treePanelBlock.props.sourceModelCode}/tree`
            : ''))
    // 左树右表：右表平铺默认不显示「添加下级」；本表嵌入式树已启用时跟随配置
    const embeddedTreeOnRight = modelTreeConfig.enabled === true || modelSchema.appType === 'TREE'
    const enableTreeAddChild = schema.layoutType === 'tree-crud'
      ? (embeddedTreeOnRight
          ? (typeof modelTreeConfig.enableTreeAddChild === 'boolean'
              ? modelTreeConfig.enableTreeAddChild
              : typeof crudBlock?.props?.enableTreeAddChild === 'boolean'
                ? crudBlock.props.enableTreeAddChild
                : typeof tableProps.enableTreeAddChild === 'boolean'
                  ? tableProps.enableTreeAddChild
                  : true)
          : false)
      : treeEnabled
        ? (typeof modelTreeConfig.enableTreeAddChild === 'boolean'
            ? modelTreeConfig.enableTreeAddChild
            : typeof crudBlock?.props?.enableTreeAddChild === 'boolean'
              ? crudBlock.props.enableTreeAddChild
              : typeof tableProps.enableTreeAddChild === 'boolean'
                ? tableProps.enableTreeAddChild
                : true)
        : crudBlock?.props?.enableTreeAddChild === true || tableProps.enableTreeAddChild === true
    const baseProps = {
      lazy: true,
      designPreview: true,
      loadDetailOnEdit: false,
      columns: buildDesignerColumns(tableZone, fieldMap),
      searchSchema: buildDesignerSearchSchema(searchZone, fieldMap, schema),
      editSchema: editFields,
      apiConfig: appendDesignPreviewToApiConfig({
        list: tableProps.listApi || apiValues.listApi,
        detail: tableProps.detailApi || apiValues.detailApi,
        create: tableProps.createApi || apiValues.createApi,
        update: tableProps.updateApi || apiValues.updateApi,
        delete: tableProps.deleteApi || apiValues.deleteApi,
        import: tableProps.importApi || '',
        export: tableProps.exportApi || '',
        tree: treePanelApi || tableProps.treeApi || apiValues.treeApi || '',
      }),
      api: appendDesignPreviewToApiValue(tableProps.api || apiValues.api),
      rowKey: tableProps.rowKey || 'id',
      showSearch: searchZone.enabled !== false && tableProps.showSearch !== false,
      showPagination: tableProps.showPagination !== false,
      searchGridCols: searchProps.gridCols || tableProps.searchGridCols || 4,
      searchLabelWidth: searchProps.labelWidth || tableProps.searchLabelWidth || 'auto',
      searchEnableCollapse: tableProps.searchEnableCollapse !== false,
      searchMaxVisibleFields: tableProps.searchMaxVisibleFields || 3,
      searchYGap: tableProps.searchYGap ?? 16,
      editGridCols: formLayout.gridColumns || formLayout.gridCols || editProps.editGridCols || tableProps.editGridCols || 1,
      editLabelWidth: formLayout.labelWidth || editProps.editLabelWidth || editProps.labelWidth || tableProps.editLabelWidth || 'auto',
      editLabelPlacement: formLayout.labelPlacement || editProps.editLabelPlacement || editProps.labelPlacement || tableProps.editLabelPlacement || 'left',
      editLabelAlign: formLayout.labelAlign || editProps.editLabelAlign || editProps.labelAlign || tableProps.editLabelAlign || 'right',
      editSize: formLayout.size || editProps.editSize || editProps.size || tableProps.editSize || 'medium',
      editShowFeedback: editProps.editShowFeedback ?? editProps.showFeedback ?? tableProps.editShowFeedback ?? true,
      editXGap: formLayout.columnGap ?? editProps.editXGap ?? editProps.columnGap ?? tableProps.editXGap ?? 16,
      editYGap: formLayout.rowGap ?? editProps.editYGap ?? editProps.rowGap ?? tableProps.editYGap ?? 8,
      modalWidth: formLayout.modalWidth || editProps.modalWidth || tableProps.modalWidth || '800px',
      detailModalWidth: formLayout.detailModalWidth || formLayout.modalWidth || editProps.detailModalWidth || editProps.modalWidth || tableProps.detailModalWidth || tableProps.modalWidth || '800px',
      formOpenMode: resolvedFormOpenMode,
      tabWorkspace: formLayout.tabWorkspace || editProps.tabWorkspace || tableProps.tabWorkspace || {},
      modalType: resolvedModalType,
      drawerPlacement: formLayout.drawerPlacement || editProps.drawerPlacement || tableProps.drawerPlacement || 'right',
      hideModalFooter: tableProps.hideModalFooter === true,
      hideDefaultDetailContent: tableProps.hideDefaultDetailContent === true,
      hideToolbar: tableProps.hideToolbar === true,
      hideAdd: tableProps.hideAdd === true,
      hideBatchDelete: tableProps.hideBatchDelete === true,
      showImport: tableProps.showImport !== false,
      showExport: tableProps.showExport !== false,
      showExportTasks: tableProps.showExportTasks !== false,
      enableCustomQuery: tableProps.enableCustomQuery !== false,
      addButtonText: tableProps.addButtonText || '新增',
      exportButtonText: tableProps.exportButtonText || '导出',
      exportFileName: tableProps.exportFileName || '',
      renderMode: tableProps.renderMode || 'table',
      showRenderModeSwitch: tableProps.showRenderModeSwitch !== false,
      tableSize: tableProps.tableSize || 'medium',
      bordered: Boolean(tableProps.bordered),
      striped: Boolean(tableProps.striped),
      hideSelection: tableProps.hideSelection === true,
      maxHeight: tableProps.maxHeight || undefined,
      scrollX: tableProps.scrollX || undefined,
      resizable: tableProps.resizable !== false,
      expandConfig: tableProps.expandConfig || {},
      listMethod: tableProps.listMethod || 'get',
      listDataField: tableProps.listDataField || 'records',
      listTotalField: tableProps.listTotalField || 'total',
      isEncrypt: tableProps.isEncrypt === true,
      publicParams: tableProps.publicParams || {},
      publicQuery: tableProps.publicQuery || {},
      formDefaultValues: tableProps.formDefaultValues || {},
      submitDefaultParams: tableProps.submitDefaultParams || {},
      toolbarActions: runtimeActions.filter(action => (action.position || 'toolbar') === 'toolbar'),
      runtimeActions: runtimeActions.filter(action => (action.position || 'row') === 'row'),
      enableTreeAddChild,
      ...(treeConfig ? { treeConfig } : {}),
      ...hookHandlers,
    }
    return applyEmbeddedTreeTableRuntimeProps(baseProps, {
      layoutType: schema.layoutType || 'simple-crud',
      options: treeConfig
        ? { treeConfig, enableTreeAddChild }
        : {},
      apiConfig: baseProps.apiConfig,
    }, { designPreview: true })
  }

  function resolveDesignerFormOpenMode(formLayout = {}, tableProps = {}, editProps = {}) {
    const value = formLayout.formOpenMode || formLayout.modalType || editProps.formOpenMode || tableProps.formOpenMode || editProps.modalType || tableProps.modalType || 'modal'
    const mode = String(value || '').trim()
    if (mode === 'tabWorkspace' || mode.toLowerCase() === 'tabworkspace')
      return 'tabWorkspace'
    const normalized = mode.toLowerCase()
    return ['modal', 'drawer', 'flat'].includes(normalized) ? normalized : 'modal'
  }

  function resolveDesignerModalType(formOpenMode = 'modal', formLayout = {}, tableProps = {}, editProps = {}) {
    if (['modal', 'drawer'].includes(formOpenMode))
      return formOpenMode
    const value = formLayout.modalType || editProps.modalType || tableProps.modalType || 'modal'
    const normalized = String(value || '').trim().toLowerCase()
    return ['modal', 'drawer'].includes(normalized) ? normalized : 'modal'
  }

  function buildDesignerCrudHookHandlers(tableProps = {}) {
    const rules = normalizeCrudHookRules(tableProps.crudHookRules || {}, tableProps.beforeSubmitRules || [])
    return CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
      const list = (rules[target.value] || []).filter(rule => rule.field)
      if (list.length)
        handlers[target.value] = data => applyCrudHookRules(data, list)
      return handlers
    }, {})
  }

  function handleListCustomActionsUpdate(actions = []) {
    if (props.defaultViewOnly)
      return
    const nextActions = normalizeListCustomActions(actions)
    listCustomActions.value = nextActions
    setLocalSchema(syncSchemaCustomActions(localSchema.value, nextActions))
    const managedActions = nextActions.map((action, index) => listActionToDesignerAction(action, index, { preserveDraftParams: true }))
    emit('update:designerActions', cloneSchema(mergeUnmanagedChildRowActions(managedActions)))
    emit('dirtyChange', true)
  }

  function buildDesignerActionsForSave() {
    const managedActions = normalizeListCustomActions(listCustomActions.value)
      .map((action, index) => listActionToDesignerAction(action, index))
    return mergeUnmanagedChildRowActions(managedActions)
  }

  function normalizeDesignerActionsForList(actions = []) {
    if (!Array.isArray(actions))
      return []
    return normalizeListCustomActions(actions
      .filter(action => !isChildRowDesignerAction(action))
      .map(designerActionToListAction))
  }

  function mergeUnmanagedChildRowActions(managedActions = []) {
    const childActions = (Array.isArray(props.designerActions) ? props.designerActions : [])
      .filter(isChildRowDesignerAction)
    return [...managedActions, ...cloneSchema(childActions)]
  }

  function isChildRowDesignerAction(action = {}) {
    return String(action.actionPosition || action.position || '')
      .replace(/[-\s]+/g, '_')
      .toUpperCase() === 'CHILD_ROW'
  }

  function syncSchemaCustomActions(schema = {}, actions = []) {
    const normalizedActions = normalizeListCustomActions(actions)
    const syncGrid = (grid = {}) => {
      if (!grid || !Array.isArray(grid.items))
        return grid
      let changed = false
      const items = grid.items.map((item) => {
        if (!['AiCrudPage', 'data-table', 'AiTable', 'toolbar'].includes(item?.blockType))
          return item
        changed = true
        return {
          ...item,
          props: {
            ...(item.props || {}),
            customActions: cloneSchema(normalizedActions),
          },
        }
      })
      return changed ? { ...grid, items } : grid
    }

    const listGridLayout = syncGrid(schema.listGridLayout)
    const pages = Array.isArray(schema.pages)
      ? schema.pages.map(page => ({
          ...page,
          gridLayout: syncGrid(page?.gridLayout),
        }))
      : schema.pages
    const zones = Array.isArray(schema.zones)
      ? schema.zones.map((zone) => {
          if (zone?.zoneKey !== 'table')
            return zone
          return {
            ...zone,
            props: {
              ...(zone.props || {}),
              customActions: cloneSchema(normalizedActions),
            },
          }
        })
      : schema.zones

    return {
      ...schema,
      listGridLayout,
      pages,
      zones,
    }
  }

  __impl.filterDesignerGridBlocks = filterDesignerGridBlocks
  __impl.resolveTemplateSelectValue = resolveTemplateSelectValue
  __impl.renderMenuIcon = renderMenuIcon
  __impl.createCleanTemplateGridLayout = createCleanTemplateGridLayout
  __impl.updateTreeLayoutEnabled = updateTreeLayoutEnabled
  __impl.clearEmbeddedTreeForLeftTreeLayout = clearEmbeddedTreeForLeftTreeLayout
  __impl.clearTreeAddChildOnListBlocks = clearTreeAddChildOnListBlocks
  __impl.updateListTemplate = updateListTemplate
  __impl.handleGridLayoutUpdate = handleGridLayoutUpdate
  __impl.handleGridModelSchemaUpdate = handleGridModelSchemaUpdate
  __impl.switchActivePage = switchActivePage
  __impl.handleListMoreSelect = handleListMoreSelect
  __impl.openPrintPreview = openPrintPreview
  __impl.handlePageActionSelect = handlePageActionSelect
  __impl.addDesignerPage = addDesignerPage
  __impl.duplicateActivePage = duplicateActivePage
  __impl.removeActivePage = removeActivePage
  __impl.resetActivePageLayout = resetActivePageLayout
  __impl.clearActivePageLayout = clearActivePageLayout
  __impl.patchActivePage = patchActivePage
  __impl.updateActivePageKey = updateActivePageKey
  __impl.addActivePageParam = addActivePageParam
  __impl.updateActivePageParam = updateActivePageParam
  __impl.removeActivePageParam = removeActivePageParam
  __impl.createUniquePageKey = createUniquePageKey
  __impl.normalizePageKey = normalizePageKey
  __impl.normalizePageParamName = normalizePageParamName
  __impl.isProtectedPage = isProtectedPage
  __impl.pageTypeText = pageTypeText
  __impl.normalizeDesignerPageType = normalizeDesignerPageType
  __impl.rewritePageEventTargets = rewritePageEventTargets
  __impl.rewriteGridLayoutPageEventTargets = rewriteGridLayoutPageEventTargets
  __impl.rewriteBlocksPageEventTargets = rewriteBlocksPageEventTargets
  __impl.resetZoneFields = resetZoneFields
  __impl.resetListSchema = resetListSchema
  __impl.saveLayout = saveLayout
  __impl.buildCurrentViewSchema = buildCurrentViewSchema
  __impl.buildDesignerRuntimeCrudProps = buildDesignerRuntimeCrudProps
  __impl.resolveDesignerFormOpenMode = resolveDesignerFormOpenMode
  __impl.resolveDesignerModalType = resolveDesignerModalType
  __impl.buildDesignerCrudHookHandlers = buildDesignerCrudHookHandlers
  __impl.handleListCustomActionsUpdate = handleListCustomActionsUpdate
  __impl.buildDesignerActionsForSave = buildDesignerActionsForSave
  __impl.normalizeDesignerActionsForList = normalizeDesignerActionsForList
  __impl.mergeUnmanagedChildRowActions = mergeUnmanagedChildRowActions
  __impl.isChildRowDesignerAction = isChildRowDesignerAction
  __impl.syncSchemaCustomActions = syncSchemaCustomActions

  return {
    props, emit, __impl, mut, addActivePageParam, addDesignerPage, buildCurrentViewSchema, buildDesignerActionConfig,
    buildDesignerActionsForSave, buildDesignerColumns, buildDesignerCrudHookHandlers, buildDesignerEditSchema, buildDesignerFieldMap, buildDesignerRuntimeCrudProps, buildDesignerRuntimeField, buildDesignerSearchSchema,
    buildDesignerSelfTreeOptionSource, clearActivePageLayout, clearEmbeddedTreeForLeftTreeLayout, clearTreeAddChildOnListBlocks, collectSchemaCustomActions, createCleanTemplateGridLayout, createUniquePageKey, deduplicateListActions,
    designerActionToListAction, designerActionTypeToListType, duplicateActivePage, ensureDesignerPages, ensureGridListSchema, filterDesignerGridBlocks, handleGridLayoutUpdate, handleGridModelSchemaUpdate,
    handleListCustomActionsUpdate, handleListDesignerShortcut, handleListMoreSelect, handlePageActionSelect, hydrateDesignerFormLayout, hydrateDesignerFormLayoutNode, inferLayoutType, isChildRowDesignerAction,
    isInvalidDesignerApiAction, isProtectedPage, isRelationLayout, isStandaloneDesignerLayoutNode, isStandardListGridLayout, isTreeLayout, listActionToDesignerAction, listActionTypeToDesignerType,
    mergePrimaryModelRef, mergeUnmanagedChildRowActions, normalizeActionApiMethod, normalizeActionCode, normalizeActionParams, normalizeDesignerActionPosition, normalizeDesignerActionType, normalizeDesignerActionsForList,
    normalizeDesignerPage, normalizeDesignerPageType, normalizeDesignerRuntimeFieldType, normalizeListActionConfig, normalizeListActionPosition, normalizeListActionType, normalizeListCustomAction, normalizeListCustomActions,
    normalizePageKey, normalizePageParamName, normalizeSchemaForSave, omitTreeRuntimeProps, openLocalPreview, openPrintPreview, pageTypeText, parsePlainObject,
    patchActivePage, patchPageWatermark, pushHistorySnapshot, redoSchema, removeActivePage, removeActivePageParam, renderMenuIcon, resetActivePageLayout, resetListSchema,
    resetZoneFields, resolveDefaultActionButtonType, resolveDefaultListButtonType, resolveDesignModelSchema, resolveDesignerDefaultApiValues, resolveDesignerFormOpenMode, resolveDesignerModalType, resolveDesignerZoneRefs,
    resolveLayoutModeLabel, resolveListActionRoutePath, resolveSchema, resolveTemplateSelectValue, rewriteBlocksPageEventTargets, rewriteGridLayoutPageEventTargets, rewritePageEventTargets, saveLayout,
    setLocalSchema, switchActivePage, syncDesignerDraft, syncSchemaCustomActions, toDesignerActionParams, toPageField, undoSchema, updateActivePageKey,
    updateActivePageParam, updateDesignerPageGrid, updateListTemplate, updateTreeLayoutEnabled, updateTreeZone, message, saving, undoStack,
    redoStack, listGridDesignerRef, activePageKey, pageSettingsExpanded, HISTORY_LIMIT, VALID_PAGE_TYPES, baseModelSchema, localSchema,
    pendingModelSchema, workingModelSchema, effectiveModelSchema, designFields, layoutModeLabel, canUndo, canRedo, listPreviewVisible, listPreviewLayout, listPreviewLayoutKey,
    templateSelectValue, listCustomActions, pageTypeOptions, listTemplateOptions, activeTemplateLabel, listMoreOptions, pageActionOptions, designerPages,
    actionPageOptions,
    activeDesignerPage, currentPageGridLayout, previewGridLayout, designerGridLayout, designerPreviewGridLayout, pageWatermark, pageWatermarkContent, visibleListCustomActions, designerRuntimeCrudProps,
  }
}
