/** Auto-split part 1 of ListPageGridDesigner setup. */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { businessObjectFields, businessObjectList, enabledApiConfigs } from '@/api/business-app'
import {
  getComponentSpec,
  isPaletteUnionSpec,
  LIST_BLOCK_TYPE_OVERRIDES,
  LIST_PALETTE_EXCLUDED_TYPES,
  toListPageBlockCatalog,
} from '@/components/lowcode-builder/designer-core'
import { pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import { resolveWidgetRenderMode } from '@/components/lowcode-builder/shared/widget-binding-slots'
import {
  collectBlocksInTree,
  findBlockInTree,
  mapBlockSiblingsInTree,
  mapBlocksInTree,
  removeBlockFromTree,
} from '../blockTree'
import {
  alignOptions,
  normalizeParamName,
  resolveSelectedFieldRefs,
} from '../fieldDrawerConfig'
import { cloneSchema, createDefaultField } from '@/components/lowcode-builder/model/model-schema'
import { useListDesignerStore } from '@/store'
import {
  buildGridSyncModelSchema,
  createDefaultBlockStyle,
  createDefaultListGridLayout,
  createGridBlock,
  isChildListField,
  isListFieldSelectable,
  isPageFieldVisible,
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_COLS,
  listPageBlockCatalog,
  resolveChildListDisplayHint,
  resolveListFieldTitle,
  resolveListPageBlockMeta,
  resolveTreeFieldOptions,
  resolveTreeSourceRefs,
  syncGridLayoutWithModel,
} from '../page-schema'

import { provideListPageDesignerApi } from '../listPageDesignerContext'
import {
  alignSearchSchemaWithLeftTree,
  findTreePanelProps,
} from '@/components/lowcode-builder/shared/runtime-tree-table'
import {
  canvasWidthOptions,
  canvasZoomOptions,
  canvasPreviewModeOptions,
  crudPreviewModeOptions,
  actionPositionOptions,
  actionTypeOptions,
  actionBehaviorOptions,
  actionOpenTargetOptions,
  successBehaviorOptions,
  apiMethodOptions,
  apiParamTargetOptions,
  paramSourceOptions,
  routeParamOptions,
  systemVariableOptions,
  labelPlacementOptions,
  labelAlignOptions,
  gridVerticalAlignOptions,
  directionOptions,
  splitDirectionOptions,
  menuModeOptions,
  alertTypeOptions,
  richEditorModeOptions,
  wangEditorModeOptions,
  watermarkFontStyleOptions,
  watermarkTextAlignOptions,
  barcodeFormatOptions,
  qrcodeErrorCorrectionOptions,
  qrcodeDotsTypeOptions,
  qrcodeCornerTypeOptions,
  transferDataSourceOptions,
  widgetDataSourceOptions,
  detailInfoDataSourceOptions,
  dataBindablePageWidgetKeys,
  localDataBindableBlockTypes,
  markdownPreviewModeOptions,
  htmlTagOptions,
  htmlRenderModeOptions,
  vuePreviewModeOptions,
  justifyContentOptions,
  simpleConfigBlockTypes,
  componentSizeOptions,
  tableDensityOptions,
  renderModeOptions,
  expandTriggerOptions,
  expandLayoutModeOptions,
  expandPanelTypeOptions,
  expandDataSourceTypeOptions,
  requestMethodOptions,
  tagTypeOptions,
  treeLoadModeOptions,
  sortOrderOptions,
  shadowOptions,
  eventTriggerOptions,
  blockEventActionOptions,
  backButtonActionOptions,
  resizeAnchors,
  CANVAS_AUTO_SCROLL_EDGE,
  CANVAS_AUTO_SCROLL_MAX_STEP,
} from '../listDesignerOptions'
export function useListPageGridDesignerPart1(props, emit, deps = {}) {
// 画布布局 / 选中区块 / 属性面板 tab 等通信状态统一走 Pinia（listDesigner store）；
// 组件保留 props/emit 桥接：props -> store 同步，store 变更 -> emit 对外广播
const designerStore = useListDesignerStore()
// 预览弹窗也是 ListPageGridDesigner(readonly)：禁止与设计态共用 Pinia，
// 否则 hydrate 会覆盖设计器 layout，或读到尚未 emit 的空壳 props。
const previewLayoutRef = ref({})
const previewSelectedBlockId = ref(null)

provideListPageDesignerApi()

const __impl = {}
const appendBlock = (...args) => __impl.appendBlock(...args)
const buildSystemMenuPageTargetOptions = (...args) => __impl.buildSystemMenuPageTargetOptions(...args)
const clamp = (...args) => __impl.clamp(...args)
const colorToHexInput = (...args) => __impl.colorToHexInput(...args)
const ensureTreeSourceCatalog = (...args) => __impl.ensureTreeSourceCatalog(...args)
const findExistingBlockByType = (...args) => __impl.findExistingBlockByType(...args)
const gridHeightToPixels = (...args) => __impl.gridHeightToPixels(...args)
const gridWidthToPixels = (...args) => __impl.gridWidthToPixels(...args)
const hexInputToColor = (...args) => __impl.hexInputToColor(...args)
const isCurrentListObject = (...args) => __impl.isCurrentListObject(...args)
const loadTreeSourceFields = (...args) => __impl.loadTreeSourceFields(...args)
const normalizeCustomActionList = (...args) => __impl.normalizeCustomActionList(...args)
const normalizeDesignerLayout = (...args) => __impl.normalizeDesignerLayout(...args)
const normalizeGridLayoutCells = (...args) => __impl.normalizeGridLayoutCells(...args)
const patchBlockProps = (...args) => __impl.patchBlockProps(...args)
const resolveBlockFrame = (...args) => __impl.resolveBlockFrame(...args)
const resolveBlockHeightMode = (...args) => __impl.resolveBlockHeightMode(...args)
const resolveBlockWidthMode = (...args) => __impl.resolveBlockWidthMode(...args)
const resolveCanvasDropFrame = (...args) => __impl.resolveCanvasDropFrame(...args)
const resolveCssNumber = (...args) => __impl.resolveCssNumber(...args)
const resolvePropertySearchTab = (...args) => __impl.resolvePropertySearchTab(...args)
const resolveTreeSourceObjectValue = (...args) => __impl.resolveTreeSourceObjectValue(...args)
const scrollBlockIntoView = (...args) => __impl.scrollBlockIntoView(...args)
const scrollPropertySearchTarget = (...args) => __impl.scrollPropertySearchTarget(...args)
const selectBlock = (...args) => __impl.selectBlock(...args)
const updateGridLayoutStructure = (...args) => __impl.updateGridLayoutStructure(...args)

const rowHeight = 32
const gap = 8
const previewMinWidth = 360
const TREE_PANEL_COLLAPSED_WIDTH = 44

// Bitable 风格 SVG 图标组已下沉 ./bitableIcons.js（复杂页面拆分规范：纯数据/图标与页面逻辑分离）

const canvasRef = ref(null)
const canvasScrollRef = ref(null)
const selectedBlockId = computed({
  get: () => (props.readonly ? previewSelectedBlockId.value : designerStore.selectedBlockId),
  set: (blockId) => {
    if (props.readonly)
      previewSelectedBlockId.value = blockId
    else
      designerStore.selectedBlockId = blockId
  },
})
const fieldDrawerOpen = ref(false)
const specDrawerVisible = ref(false)
const fieldDrawerMode = ref('table')
const fieldDrawerInitialField = ref('')
const expandDescriptionFieldPanelOpen = ref(false)
const customActionModalOpen = ref(false)
const sourceModalOpen = ref(false)
const sourceModalTab = ref('layout')
const layoutSourceDraft = ref('')
const blockSourceDraft = ref('')
const sourceError = ref('')
const activeActionIndex = ref(0)
const propertyPanelTab = computed(() => designerStore.propertyPanelTab)
const propertyPanelRef = ref(null)
const paletteKeyword = ref('')
const propertyKeyword = ref('')
const propertyCollapseExpandedNames = ref(['base'])
const allPropertyCollapseNames = ['base', 'search', 'edit', 'toolbar', 'table', 'form-actions', 'default-params', 'event-help']
const propertySearchTabIndex = {
  props: [
    '属性',
    '基础配置',
    '接口',
    '数据源',
    '基础路径',
    '行主键',
    '真实接口预览',
    '响应字段',
    '表单布局',
    '查询',
    '列表',
    '字段',
    '搜索',
    '搜索布局',
    '搜索字段',
    '表格',
    '表格列',
    '列标题',
    '列宽',
    '对齐',
    '固定',
    '省略',
    '排序',
    '分页',
    '表单',
    '弹窗',
    '新增',
    '编辑',
    '详情',
    '页脚',
    '打开方式',
    '标签位置',
    '标签对齐',
    '标签宽度',
    '表单列数',
    '抽屉',
    '工具栏',
    '导入',
    '导出',
    '自定义查询',
    '自定义操作',
    '按钮文案',
    '默认参数',
    '公共参数',
    '表单默认值',
    '查询默认参数',
    '数据处理',
    '组件标题',
    '默认排序',
    '行间距',
    '显示项',
    '多选',
    '模式切换',
    '斑马纹',
    '边框',
    '富文本',
    '签名',
    '穿梭框',
    '分步表单',
    'Vue',
    'HTML',
    '标题',
    '段落',
    '统计',
    '链接',
    '提示',
    '水印',
    '音频',
    '视频',
    '头像',
    '条码',
    '二维码',
    'Markdown',
    '盒子',
    '间距',
    '描述列表',
    'iframe',
    'api',
    'list',
    'detail',
    'create',
    'update',
    'delete',
    'import',
    'export',
  ],
  style: [
    '样式',
    '位置',
    '尺寸',
    '坐标',
    '左',
    '上',
    'x',
    'y',
    '宽度',
    '高度',
    '固定宽',
    '自适应',
    '背景',
    '背景色',
    '边框色',
    '圆角',
    '阴影',
    '内边距',
    '外边距',
    'padding',
    'margin',
    '外观',
    '装饰',
    '颜色',
    '自定义 style',
    'customStyle',
  ],
  interaction: [
    '交互',
    '事件',
    '生命周期',
    '回调',
    '点击',
    '加载完成',
    '提交成功',
    '行点击',
    '跳转',
    '刷新',
    '过滤',
    '接口请求',
    '自定义脚本',
    '参数',
    '目标页面',
    '目标表单',
  ],
}
const propertySearchKeywordRegistry = [
  '基础配置 接口 基础路径 行主键 真实接口预览 响应字段 表单布局 api list detail create update delete import export',
  '查询与列表字段 搜索 查询区 搜索布局 搜索字段 表格 表格列 列标题 列宽 对齐 固定 省略 排序 分页 最大高度 横向宽度 边框 斑马纹 行按钮 操作列 列表行自定义按钮',
  '表单与弹窗 新增 编辑 表单 弹窗 抽屉 详情 页脚 打开方式 标签位置 标签对齐 标签宽度',
  '工具栏 导入 导出 自定义查询 自定义操作 按钮文案 回调 参数处理 提交前 搜索前 加载列表前',
  '默认参数 公共参数 publicParams publicQuery 表单默认值 提交固定参数 查询默认参数',
  '事件 生命周期 回调 点击 加载完成 提交成功 行点击 跳转 弹窗 接口请求',
  '样式 位置 尺寸 左 上 宽度 高度 背景 边框 圆角 阴影 内边距 外边距 自定义 style',
  '树 左树右表 树接口 节点字段 过滤字段 展开 折叠',
  Object.values(propertySearchTabIndex).flat().join(' '),
].map(item => item.toLowerCase())
const paletteCollapsed = ref(false)
const propertyCollapsed = ref(false)
const canvasFocusMode = ref(false)
const canvasPreviewMode = ref('desktop')
const activeTabKey = ref('')
const canvasDragActive = ref(false)
const draggedBlockType = ref('')
const draggedExistingBlockId = ref('')
const dragOverCell = ref(null)
const dragOverPoint = ref(null)
const dragBlockedBlockId = ref('')
const activeDropCell = ref(null)
const movingBlockId = ref('')
const movingPreviewBlock = ref(null)
const movingPixelOffset = ref({ x: 0, y: 0 })
const nestedMovingBlockId = ref('')
const canvasViewportWidth = ref(0)
const canvasZoom = ref(1)
const mut = {
  suppressNextBlockClick: false,
  canvasResizeObserver: null,
  treeSourceFieldRequestId: 0,
  treeSourceCatalogPromise: null,
}

// 设计态：localLayout 直连 Pinia；只读预览：用本地 ref，避免踩设计器 store
const localLayout = computed({
  get: () => (props.readonly ? previewLayoutRef.value : designerStore.layout),
  set: (value) => {
    if (props.readonly)
      previewLayoutRef.value = value
    else
      designerStore.applyLayout(value)
  },
})
// layout hydrated after later parts wire __impl (see hydrateInitialState)

const blocks = computed(() => localLayout.value.items || [])
watch(() => props.activeBlockId, (blockId) => {
  if (props.readonly)
    return
  if (blockId && findBlockInTree(blocks.value, blockId))
    selectedBlockId.value = blockId
}, { immediate: true })
const runtimeTreeFilter = ref({})
const runtimeTreeActiveKey = ref('__all__')
const collapsedTreePanelMap = ref({})
const resolvedRuntimeCrudProps = computed(() => {
  if (!props.runtimeCrudProps)
    return props.runtimeCrudProps
  const treePanelProps = findTreePanelProps(blocks.value) || {}
  const searchSchema = alignSearchSchemaWithLeftTree(props.runtimeCrudProps.searchSchema, {
    treePanelProps,
    runtimeProps: props.runtimeCrudProps,
  })
  // 左树右表：主表不吃 treeConfig（由左侧 tree-panel 筛），否则会把右表也渲成嵌套树
  // 嵌入式树表（layout ≠ tree-crud）：保留 treeConfig，画布预览与运行态一致
  if (props.layoutType === 'tree-crud') {
    const { treeConfig: _treeConfig, ...runtimeOptions } = props.runtimeCrudProps.options || {}
    return {
      ...props.runtimeCrudProps,
      // 画布已有 tree-panel，禁止再套 TreeCrudTemplate 左树
      suppressTreeCrudShell: true,
      treeConfig: {},
      options: runtimeOptions,
      searchSchema,
      publicParams: {
        ...(props.runtimeCrudProps.publicParams || {}),
        ...runtimeTreeFilter.value,
      },
    }
  }
  return {
    ...props.runtimeCrudProps,
    searchSchema,
    publicParams: {
      ...(props.runtimeCrudProps.publicParams || {}),
      ...runtimeTreeFilter.value,
    },
  }
})
const designCanvasWidth = computed(() => clamp(localLayout.value.designWidth || LIST_PAGE_DESIGN_WIDTH, 375, 2560))
const totalRows = computed(() => {
  const maxGridBottom = blocks.value.reduce((acc, b) => Math.max(acc, b.gridY + b.gridH), 0)
  const maxPixelBottom = blocks.value.reduce((acc, block) => {
    const rect = resolveBlockFrame(block)
    return Math.max(acc, rect.y + rect.height)
  }, 0)
  const rowsByPixel = Math.ceil(maxPixelBottom / (rowHeight + gap))
  const minRows = props.readonly ? 1 : 20
  const bufferRows = props.readonly ? 0 : 4
  return Math.max(minRows, maxGridBottom + bufferRows, rowsByPixel + bufferRows)
})
const colWidth = computed(() => {
  const availableWidth = props.readonly
    ? Math.max(previewMinWidth, canvasViewportWidth.value)
    : designCanvasWidth.value
  const nextWidth = Math.floor((availableWidth - (LIST_PAGE_GRID_COLS - 1) * gap) / LIST_PAGE_GRID_COLS)
  return Math.max(1, nextWidth)
})
const canvasGridWidth = computed(() => LIST_PAGE_GRID_COLS * colWidth.value + (LIST_PAGE_GRID_COLS - 1) * gap)
const canvasGridHeight = computed(() => totalRows.value * (rowHeight + gap))
const canvasStyle = computed(() => ({
  width: `${canvasGridWidth.value}px`,
  minHeight: `${canvasGridHeight.value}px`,
}))
const canvasZoomLabel = computed(() => `${Math.round(canvasZoom.value * 100)}%`)
const canvasPreviewModeLabel = computed(() => canvasPreviewModeOptions.find(item => item.value === canvasPreviewMode.value)?.label || '桌面')
const canvasViewportSummary = computed(() => {
  return `${canvasPreviewModeLabel.value} ${designCanvasWidth.value}px / ${canvasZoomLabel.value}`
})
const canvasScaleStyle = computed(() => ({
  transform: `scale(${canvasZoom.value})`,
  transformOrigin: '0 0',
}))
const canvasZoomStageStyle = computed(() => ({
  width: `${(canvasGridWidth.value + (props.readonly ? 0 : 24)) * canvasZoom.value}px`,
  minHeight: `${(canvasGridHeight.value + (props.readonly ? 0 : 24)) * canvasZoom.value}px`,
}))
const collapsedTreeFrames = computed(() => {
  return blocks.value
    .filter(block => block.blockType === 'tree-panel' && collapsedTreePanelMap.value[block.id])
    .map(block => ({ block, rect: resolveBlockFrame(block) }))
    .sort((a, b) => a.rect.x - b.rect.x)
})

const selectedBlock = computed(() => {
  if (props.readonly)
    return findBlockInTree(blocks.value, selectedBlockId.value) || null
  return designerStore.selectedBlock
})

// ─── 统一组件属性面板（designer-core spec 驱动，P2）──────────
// 手写模板分支已覆盖的属性 key：spec 面板跳过，仅渲染增量属性，避免重复入口
const SPEC_PANEL_EXCLUDED_PROPS = {
  'AiCrudPage': ['addButtonText', 'api', 'createApi', 'deleteApi', 'detailApi', 'enableTreeAddChild', 'exportApi', 'exportButtonText', 'exportFileName', 'importApi', 'listApi', 'listDataField', 'listMethod', 'listTotalField', 'maxHeight', 'previewRecordId', 'renderMode', 'rowKey', 'scrollX', 'searchGridCols', 'searchLabelWidth', 'searchMaxVisibleFields', 'searchYGap', 'tableSize', 'updateApi'],
  'AiForm': ['cancelText', 'resetText', 'submitText', 'xGap', 'yGap'],
  'AiTable': ['gridCols', 'labelAlign', 'labelPlacement', 'labelWidth', 'maxHeight', 'maxVisibleFields', 'renderMode', 'rowKey', 'scrollX', 'size'],
  // action-button：手写区已有 文案/类型/尺寸/点击动作；secondary/dashed/block/loading/disabled
  // 等由 SpecPropertyPanel 提供（此前 spec 缺这些 key，排除项形同虚设、属性无编辑入口）
  'action-button': ['text', 'type', 'size'],
  'announcement': ['bordered', 'content', 'showIcon', 'type'],
  'avatar': ['description', 'name', 'size', 'src', 'value'],
  'back-button': ['action', 'targetFormKey', 'targetPageKey', 'text', 'type'],
  'barcode': ['barHeight', 'barWidth', 'fontSize', 'format', 'margin', 'showText'],
  'box-layout': ['alignItems', 'direction', 'gap', 'justifyContent'],
  'card': ['content', 'title'],
  'code': ['itemsText'],
  'countdown': ['active', 'duration', 'precision'],
  'custom-html': ['content', 'title'],
  'descriptions': [],
  'detail-info': ['bordered', 'columnCount', 'contextPath', 'dataPath', 'dataSourceType', 'detailApi', 'detailMethod', 'labelPlacement', 'paramsText'],
  'divider': ['orientation', 'title'],
  'empty-state': ['actionText', 'description', 'title'],
  'html-tag': ['attributesText', 'htmlContent', 'renderMode', 'semanticRole', 'tagName', 'textContent'],
  'info-panel': ['content', 'title', 'type'],
  'markdown': ['content', 'height', 'previewMode'],
  'menu': ['mode', 'optionsText', 'value'],
  'number-animation': ['color', 'duration', 'from', 'to'],
  'page-title': ['content', 'size', 'statusText', 'statusType'],
  'pagination': ['itemCount', 'page', 'pageSize', 'simple'],
  'paragraph': ['content'],
  'qrcode': ['background', 'cornerColor', 'cornersDotType', 'cornersSquareType', 'dotsType', 'errorCorrectionLevel', 'margin', 'showText', 'size'],
  'rich-text': ['content', 'editorMode', 'fontSize', 'lineHeight', 'minHeight', 'toolbarMode'],
  'search-form': ['collapsible', 'defaultSortField', 'defaultSortOrder', 'rowGap', 'title'],
  'section-divider': ['title'],
  'signature-pad': ['height', 'required', 'strokeWidth', 'title'],
  'space': ['direction', 'lineVisible', 'size'],
  'split': ['defaultSize', 'direction', 'max', 'min', 'pane1Content', 'pane2Content'],
  'statistic': ['color', 'prefix', 'suffix', 'title', 'trend', 'value'],
  'step-form': ['current', 'direction', 'title'],
  'steps': ['current'],
  // tabs 属性全部由 SpecPropertyPanel 渲染（手写区只保留页签管理），spec 是唯一属性源
  'tabs': [],
  'text-title': ['align', 'color', 'level', 'subtitle', 'text', 'weight'],
  'timeline': ['title'],
  'toolbar': ['actions'],
  'transfer': ['dataSourceType', 'filterable', 'sourceTitle', 'targetTitle', 'title'],
  'tree-panel': ['filterField', 'keyField', 'labelField', 'loadMode', 'parentField', 'targetField', 'treeTitle'],
  'video-player': ['poster'],
  'vue-component': ['componentName', 'previewMode', 'propsJson', 'scriptCode', 'styleCode', 'templateCode'],
  'watermark': ['content', 'cross', 'fontColor', 'fontSize', 'fontStyle', 'fontWeight', 'globalRotate', 'height', 'image', 'imageHeight', 'imageWidth', 'lineHeight', 'rotate', 'textAlign', 'width', 'xGap', 'xOffset', 'yGap', 'yOffset'],
}

const specPanelExcludeKeys = computed(() => {
  const blockType = selectedBlock.value?.blockType
  // 栅格属性已内联渲染在「栅格配置」区（与表单设计器共用 SpecPropertyPanel），
  // 底部增量面板动态排除全部 spec key，避免重复入口；后续 grid spec 新增属性自动同步
  if (blockType === 'grid-layout') {
    const properties = getComponentSpec(blockType)?.propsSchema?.properties
    return properties ? Object.keys(properties) : []
  }
  return SPEC_PANEL_EXCLUDED_PROPS[blockType] || []
})

const specPanelPropertyCount = computed(() => {
  const spec = getComponentSpec(selectedBlock.value?.blockType || '')
  const properties = spec?.propsSchema?.properties
  if (!properties)
    return 0
  const exclude = new Set(specPanelExcludeKeys.value)
  return Object.keys(properties).filter(key => !exclude.has(key)).length
})

function handleSpecPropUpdate({ key, value }) {
  if (!selectedBlock.value?.id || !key)
    return
  // 栅格总列数变化需同步收敛各格子 span（updateGridLayoutStructure 内置 clamp + cells 归一化）
  if (selectedBlock.value.blockType === 'grid-layout' && key === 'columns') {
    updateGridLayoutStructure({ columns: value })
    return
  }
  patchBlockProps(selectedBlock.value.id, { [key]: value })
}

const selectedAiCrudFormModalProps = computed(() => {
  if (selectedBlock.value?.blockType !== 'AiCrudPage')
    return {}
  return {
    ...(selectedBlock.value.props || {}),
    ...(resolvedRuntimeCrudProps.value || {}),
  }
})
const selectedBlockEvents = computed(() => selectedBlock.value?.props?.events || [])
const selectedBlockStyle = computed(() => ({
  ...createDefaultBlockStyle(),
  ...(selectedBlock.value?.props?.style || {}),
}))
const selectedBlockBackgroundHex = computed(() => colorToHexInput(selectedBlockStyle.value.backgroundColor, ''))
const selectedBlockBorderHex = computed(() => colorToHexInput(selectedBlockStyle.value.borderColor, 'E4E4E7'))
const selectedBlockBackgroundPreview = computed(() => hexInputToColor(selectedBlockBackgroundHex.value, 'transparent'))
const selectedBlockBackgroundColorInput = computed(() => hexInputToColor(selectedBlockBackgroundHex.value, '#ffffff'))
const selectedBlockBorderPreview = computed(() => {
  if (selectedBlockStyle.value.borderStyle === 'none')
    return '#e4e4e7'
  return hexInputToColor(selectedBlockBorderHex.value, '#e4e4e7')
})
const selectedBlockFrame = computed(() => selectedBlock.value
  ? resolveBlockFrame(selectedBlock.value)
  : { x: 0, y: 0, width: 24, height: 24 })
const selectedBlockWidthMode = computed(() => selectedBlock.value ? resolveBlockWidthMode(selectedBlock.value) : 'full')
const selectedBlockHeightMode = computed(() => selectedBlock.value ? resolveBlockHeightMode(selectedBlock.value) : 'fixed')
const selectedBlockContentAlign = computed(() => selectedBlock.value?.props?.style?.textAlign || selectedBlock.value?.props?.textAlign || selectedBlock.value?.props?.align || 'left')
const selectedBlockFixedWidth = computed(() => {
  if (!selectedBlock.value)
    return 24
  const value = selectedBlock.value.props?.style?.width
  const fallback = selectedBlockFrame.value.width
  return Math.max(24, resolveCssNumber(value, fallback))
})
const layoutCodeText = computed(() => JSON.stringify(localLayout.value || {}, null, 2))
const selectedBlockCodeText = computed(() => selectedBlock.value ? JSON.stringify(selectedBlock.value, null, 2) : '')
const externalCustomActionsEnabled = computed(() => Array.isArray(props.customActions))
const customActionList = computed(() => normalizeCustomActionList(
  externalCustomActionsEnabled.value ? (props.customActions || []) : (selectedBlock.value?.props?.customActions || []),
))
const toolbarCustomActions = computed(() => customActionList.value.filter(action => (action.position || 'toolbar') === 'toolbar'))
const rowCustomActions = computed(() => customActionList.value.filter(action => (action.position || 'row') === 'row'))
const activeAction = computed(() => customActionList.value[activeActionIndex.value] || null)
const apiConfigs = ref([])
const apiConfigLoading = ref(false)
const apiConfigLoaded = ref(false)
const systemMenuPages = ref([])
const systemMenuPageLoading = ref(false)
const systemMenuPageLoaded = ref(false)
const apiConfigOptions = computed(() => apiConfigs.value.map(item => ({
  label: `${item.apiName || item.apiCode || item.urlPath} · ${item.reqMethod || 'GET'} ${item.urlPath || ''}`,
  value: String(item.id || item.apiCode || item.urlPath),
})))
const systemMenuPageTargetOptions = computed(() => buildSystemMenuPageTargetOptions(systemMenuPages.value))
const dropPreviewLabel = computed(() => {
  const meta = resolveListPageBlockMeta(resolveDraggedPreviewBlockType())
  return meta ? `放置 ${meta.title}` : '放置区块'
})
const dropPreviewStyle = computed(() => {
  if (!canvasDragActive.value || !dragOverPoint.value)
    return null
  if (activeDropCell.value)
    return null
  if (dragBlockedBlockId.value)
    return null
  const meta = resolveListPageBlockMeta(resolveDraggedPreviewBlockType())
  if (!meta)
    return null
  const source = draggedExistingBlockId.value ? findBlockInTree(blocks.value, draggedExistingBlockId.value) : null
  const sourceFrame = source ? resolveDetachedBlockFrame(source, meta) : null
  const width = Math.min(
    sourceFrame?.width || gridWidthToPixels(Math.min(meta.defaultW || 4, LIST_PAGE_GRID_COLS)),
    canvasGridWidth.value,
  )
  const height = sourceFrame?.height || gridHeightToPixels(Math.max(1, meta.defaultH || 2))
  const frame = resolveCanvasDropFrame(source?.id || '', {
    x: clamp(dragOverPoint.value.x, 0, Math.max(0, canvasGridWidth.value - width)),
    y: Math.max(0, dragOverPoint.value.y),
    width,
    height,
  })
  return {
    left: `${frame.x}px`,
    top: `${frame.y}px`,
    width: `${width}px`,
    height: `${height}px`,
  }
})
const blockedDropPreviewStyle = computed(() => {
  if (!canvasDragActive.value || !dragBlockedBlockId.value)
    return null
  const block = findBlockInTree(blocks.value, dragBlockedBlockId.value)
  if (!block)
    return null
  const rect = resolveBlockFrame(block)
  return {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
  }
})
const movePlaceholderStyle = computed(() => {
  const block = movingPreviewBlock.value
  if (!block)
    return null
  const rect = resolveBlockFrame(block)
  return {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
  }
})
const layoutTitle = computed(() => {
  if (props.layoutType === 'tree-crud')
    return '自由画布 · 左树右表模板'
  if (props.layoutType === 'master-detail-crud')
    return '自由画布 · 关联数据'
  return '自由画布'
})
const selectedBlockMeta = computed(() => selectedBlock.value ? resolveListPageBlockMeta(selectedBlock.value.blockType) : null)

function resolveDraggedPreviewBlockType() {
  if (draggedBlockType.value)
    return draggedBlockType.value
  if (!draggedExistingBlockId.value)
    return ''
  return findBlockInTree(blocks.value, draggedExistingBlockId.value)?.blockType || ''
}

function resolveDetachedBlockFrame(block = {}, meta = null) {
  const style = block.props?.style || {}
  const fallbackGridW = Math.min(Number(block.gridW) || meta?.defaultW || 4, LIST_PAGE_GRID_COLS)
  const fallbackGridH = Math.max(1, Number(block.gridH) || meta?.defaultH || 2)
  const fallbackWidth = gridWidthToPixels(fallbackGridW)
  const fallbackHeight = gridHeightToPixels(fallbackGridH)
  const width = style.widthMode === 'auto'
    ? Math.min(520, Math.max(240, fallbackWidth))
    : resolveCssNumber(style.width, fallbackWidth)
  const height = resolveCssNumber(style.height, fallbackHeight)
  return {
    width: Math.max(24, Math.min(width || fallbackWidth, canvasGridWidth.value)),
    height: Math.max(24, height || fallbackHeight),
  }
}
const blockTargetOptions = computed(() => blocks.value
  .flatMap(block => collectBlocksInTree(block))
  .filter(block => block.id !== selectedBlock.value?.id)
  .map(block => ({
    label: `${block.label || block.blockType}（${block.blockType}）`,
    value: block.id,
  })))
const pageTargetOptions = computed(() => {
  const source = Array.isArray(props.actionPages) && props.actionPages.length
    ? props.actionPages
    : (props.pages || [])
  return source.map(page => ({
    label: `${page.pageName || page.pageKey}（${page.pageType || 'custom'}）`,
    value: page.pageKey,
  }))
})
const formTargetOptions = computed(() => props.formOptions
  .filter(item => item?.value)
  .map(item => ({
    label: item.label || item.value,
    value: item.value,
  })))
const rowFieldOptions = computed(() => props.fields
  .filter(field => field?.field)
  .map(field => ({
    label: `${field.label || field.field}（${field.field}）`,
    value: field.field,
  })))
const runtimeRuleFieldOptions = computed(() => rowFieldOptions.value)
const NESTED_CONTAINER_BLOCK_TYPES = ['card', 'tabs', 'grid-layout', 'box-layout']

/** 容器嵌套保护：画布 > 容器 > 容器，最多两层（嵌套容器内不可再放入容器） */
function isTopLevelBlockId(id = '') {
  return (blocks.value || []).some(block => block?.id === id)
}

const childBlockTypeOptions = computed(() => {
  const allowContainer = isTopLevelBlockId(selectedBlock.value?.id)
  return listPageBlockCatalog
    .filter(item => !item.unique && !item.hidden)
    .filter(item => allowContainer || !NESTED_CONTAINER_BLOCK_TYPES.includes(item.blockType))
    .filter(item => !item.onlyFor || item.onlyFor.includes(props.layoutType))
    .map(item => ({
      label: `${item.title}（${item.blockType}）`,
      value: item.blockType,
    }))
})
const tabPaneOptions = computed(() => (selectedBlock.value?.props?.tabs || []).map(tab => ({
  label: tab.title || tab.key,
  value: tab.key,
})))
const activeTabChildren = computed(() => {
  const tabs = selectedBlock.value?.props?.tabs || []
  const tab = tabs.find(item => item.key === activeTabKey.value) || tabs[0]
  return tab?.children || []
})
const selectedGridCells = computed(() => normalizeGridLayoutCells(selectedBlock.value))

const primaryModelCode = computed(() => props.modelSchema?.pageModelRefs?.find(ref => ref.primary)?.modelCode || '')
const primaryFieldOptions = computed(() => props.fields
  .filter(field => !field.modelCode || !primaryModelCode.value || field.modelCode === primaryModelCode.value)
  .filter(field => isPageFieldVisible(field, 'table'))
  .map(f => ({
    label: f.label ? `${f.label}（${f.sourceField || f.field}）` : (f.sourceField || f.field),
    value: f.sourceField || f.field,
  })))
const sortFieldOptions = computed(() => {
  const options = primaryFieldOptions.value.map(item => ({ ...item }))
  if (!options.some(item => item.value === 'id')) {
    options.unshift({ label: 'ID（id）', value: 'id' })
  }
  return options
})
const defaultApiValues = computed(() => {
  const key = props.modelSchema?.configKey
    || props.modelSchema?.object?.configKey
    || props.modelSchema?.object?.code
    || props.modelSchema?.objectCode
    || props.modelSchema?.modelCode
    || ''
  const prefix = key ? `/ai/crud/${key}` : '/ai/crud/当前配置'
  return {
    api: prefix,
    listApi: `get@${prefix}/page`,
    detailApi: `get@${prefix}/:id`,
    createApi: `post@${prefix}`,
    updateApi: `put@${prefix}`,
    deleteApi: `delete@${prefix}/:id`,
    importApi: `post@${prefix}/import`,
    exportApi: `get@${prefix}/export`,
  }
})
const treeSourceCatalog = ref([])
const treeSourceFields = ref([])
const treeSourceLoading = ref(false)
const treeSourceFieldsLoading = ref(false)

const currentListObjectCodes = computed(() => {
  const schema = props.modelSchema || {}
  return [
    schema.objectCode,
    schema.object?.code,
    schema.modelCode,
    schema.configKey,
    schema.object?.configKey,
    primaryModelCode.value,
  ].map(value => String(value || '').trim()).filter(Boolean)
})

const selectedTreeSourceValue = computed(() => {
  const propsData = selectedBlock.value?.props || {}
  const raw = String(propsData.sourceModelCode || propsData.sourceConfigKey || '').trim()
  const matched = treeSourceCatalog.value.find(item => (
    resolveTreeSourceObjectValue(item) === raw
    || String(item.objectCode || '') === raw
    || String(item.configKey || '') === raw
    || String(item.modelCode || '') === raw
  ))
  return matched ? resolveTreeSourceObjectValue(matched) : raw
})

const treeSourceOptions = computed(() => {
  const options = []
  const seen = new Set()
  const pushOption = (option) => {
    const value = String(option?.value || '').trim()
    if (!value || seen.has(value))
      return
    seen.add(value)
    options.push(option)
  }

  treeSourceCatalog.value.forEach((item) => {
    const value = resolveTreeSourceObjectValue(item)
    if (!value)
      return
    const isCurrent = isCurrentListObject(item)
    pushOption({
      label: `${item.objectName || item.objectCode || value}${isCurrent ? '（当前列表，一般不选）' : ''}`,
      value,
    })
  })

  resolveTreeSourceRefs(props.modelSchema).forEach((ref) => {
    const value = String(ref.modelCode || '').trim()
    if (!value)
      return
    pushOption({
      label: `${ref.modelName || value}${ref.primary ? '（当前列表，一般不选）' : '（引用模型）'}`,
      value,
    })
  })

  const selected = selectedTreeSourceValue.value
  if (selected && !seen.has(selected)) {
    const name = selectedBlock.value?.props?.sourceModelName || selected
    pushOption({ label: name, value: selected })
  }
  return options
})
const treeFieldOptions = computed(() => {
  // 已选树数据源时，只展示该对象字段；不要回退到当前列表对象字段，否则看起来像没切换成功
  if (selectedTreeSourceValue.value) {
    return treeSourceFields.value.map(field => ({
      label: field.label ? `${field.label}（${field.field}）` : field.field,
      value: field.field,
    }))
  }
  return resolveTreeFieldOptions(
    props.modelSchema,
    selectedBlock.value?.props?.sourceModelCode || '',
  )
})
const embeddedTreeConfig = computed(() => {
  const source = props.modelSchema?.treeConfig || {}
  const fields = Array.isArray(props.modelSchema?.fields) ? props.modelSchema.fields : props.fields
  const parentField = source.parentField || 'parentId'
  const keyField = source.keyField || 'id'
  const labelField = source.labelField
    || fields.find(field => (field.field || field.fieldCode) === 'name')?.field
    || fields.find(field => (field.field || field.fieldCode) === 'name')?.fieldCode
    || fields.find((field) => {
      const code = field.field || field.fieldCode
      return code && code !== parentField && code !== keyField && !field.systemField
    })?.field
    || fields.find(field => field.field || field.fieldCode)?.field
    || fields.find(field => field.field || field.fieldCode)?.fieldCode
    || 'name'
  return {
    enabled: source.enabled === true || props.modelSchema?.appType === 'TREE',
    keyField,
    parentField,
    labelField,
    filterField: source.filterField || parentField,
    targetField: source.targetField || keyField,
    childrenField: source.childrenField || 'children',
    treeTitle: source.treeTitle || '',
    loadMode: source.loadMode || 'full',
  }
})
const embeddedTreeEnabled = computed(() => embeddedTreeConfig.value.enabled === true)

function findPrimaryListCrudBlock() {
  const preferred = ['AiCrudPage', 'data-table', 'AiTable']
  const all = collectBlocksInTree(blocks.value)
  for (const type of preferred) {
    const found = all.find(block => block?.blockType === type)
    if (found)
      return found
  }
  return null
}

const treeAddChildEnabled = computed(() => {
  const fromModel = props.modelSchema?.treeConfig?.enableTreeAddChild
  if (typeof fromModel === 'boolean')
    return fromModel
  const block = selectedBlock.value && ['AiCrudPage', 'data-table', 'AiTable'].includes(selectedBlock.value.blockType)
    ? selectedBlock.value
    : findPrimaryListCrudBlock()
  if (typeof block?.props?.enableTreeAddChild === 'boolean')
    return block.props.enableTreeAddChild === true
  // 左树右表默认不开启「添加下级」：右表通常是平铺列表，不是本表树
  if (props.layoutType === 'tree-crud')
    return false
  // 启用本表嵌入树后默认开启「添加下级」
  return embeddedTreeEnabled.value
})

function commitEmbeddedTreeModel(model) {
  emit('update:modelSchema', cloneSchema(model || {}))
}

function ensureEmbeddedTreeModel(model = {}) {
  const next = cloneSchema(model || {})
  next.fields = Array.isArray(next.fields) ? [...next.fields] : (props.fields || []).map(field => ({
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
  }))
  const parentField = next.treeConfig?.parentField || embeddedTreeConfig.value.parentField || 'parentId'
  if (!next.fields.some(field => (field.field || field.fieldCode) === parentField)) {
    next.fields.push({
      ...createDefaultField(parentField, '上级节点'),
      dataType: 'bigint',
      componentType: 'treeSelect',
      queryType: 'eq',
      searchable: false,
      listVisible: false,
      formVisible: true,
      width: 120,
    })
  }
  next.appType = 'TREE'
  next.treeConfig = {
    ...embeddedTreeConfig.value,
    ...(next.treeConfig || {}),
    enabled: true,
    parentField,
    filterField: next.treeConfig?.filterField || parentField,
    targetField: next.treeConfig?.targetField || next.treeConfig?.keyField || 'id',
    enableTreeAddChild: next.treeConfig?.enableTreeAddChild !== false,
  }
  return next
}

function disableEmbeddedTreeModel(model = {}) {
  const next = cloneSchema(model || {})
  next.appType = next.appType === 'TREE' ? 'SINGLE' : (next.appType || 'SINGLE')
  next.treeConfig = {
    ...embeddedTreeConfig.value,
    ...(next.treeConfig || {}),
    enabled: false,
    enableTreeAddChild: false,
  }
  return next
}

function updateEmbeddedTreeEnabled(enabled) {
  const next = enabled
    ? ensureEmbeddedTreeModel(props.modelSchema)
    : disableEmbeddedTreeModel(props.modelSchema)
  commitEmbeddedTreeModel(next)
  const crudBlock = findPrimaryListCrudBlock()
  if (!crudBlock)
    return
  patchBlockProps(crudBlock.id, {
    enableTreeAddChild: enabled ? next.treeConfig?.enableTreeAddChild !== false : false,
  })
}

function patchEmbeddedTreeConfig(patch = {}) {
  if (!embeddedTreeEnabled.value)
    return
  const next = ensureEmbeddedTreeModel(props.modelSchema)
  next.treeConfig = {
    ...(next.treeConfig || {}),
    ...patch,
    enabled: true,
  }
  if (!next.treeConfig.filterField)
    next.treeConfig.filterField = next.treeConfig.parentField || 'parentId'
  if (!next.treeConfig.targetField)
    next.treeConfig.targetField = next.treeConfig.keyField || 'id'
  commitEmbeddedTreeModel(next)
}

function updateTreeAddChildEnabled(enabled) {
  const nextEnabled = enabled === true
  // 左树右表：只改「添加下级」，不得关掉本表嵌入式树（已启用）
  if (props.layoutType === 'tree-crud') {
    const currentlyEnabled = embeddedTreeEnabled.value
    const base = currentlyEnabled
      ? ensureEmbeddedTreeModel(props.modelSchema)
      : cloneSchema(props.modelSchema || {})
    base.treeConfig = {
      ...embeddedTreeConfig.value,
      ...(base.treeConfig || {}),
      // 右表未开嵌入式树时保持 enabled=false，也不要因「添加下级」写成 TREE
      enabled: currentlyEnabled,
      enableTreeAddChild: nextEnabled,
    }
    if (currentlyEnabled)
      base.appType = 'TREE'
    commitEmbeddedTreeModel(base)
    const block = selectedBlock.value && ['AiCrudPage', 'data-table', 'AiTable'].includes(selectedBlock.value.blockType)
      ? selectedBlock.value
      : findPrimaryListCrudBlock()
    if (block)
      patchBlockProps(block.id, { enableTreeAddChild: nextEnabled })
    return
  }
  if (nextEnabled && !embeddedTreeEnabled.value) {
    const next = ensureEmbeddedTreeModel(props.modelSchema)
    next.treeConfig = {
      ...(next.treeConfig || {}),
      enabled: true,
      enableTreeAddChild: true,
    }
    commitEmbeddedTreeModel(next)
  }
  else {
    const base = cloneSchema(props.modelSchema || {})
    base.treeConfig = {
      ...embeddedTreeConfig.value,
      ...(base.treeConfig || {}),
      enabled: embeddedTreeEnabled.value || nextEnabled,
      enableTreeAddChild: nextEnabled,
    }
    if (base.treeConfig.enabled)
      base.appType = 'TREE'
    else if (base.appType === 'TREE')
      base.appType = 'SINGLE'
    commitEmbeddedTreeModel(base)
  }
  const block = selectedBlock.value && ['AiCrudPage', 'data-table', 'AiTable'].includes(selectedBlock.value.blockType)
    ? selectedBlock.value
    : findPrimaryListCrudBlock()
  if (block)
    patchBlockProps(block.id, { enableTreeAddChild: nextEnabled })
}

// ─── 统一组件物料面板（designer-core，P2）──────────────────
// 列表画布支持能力判定基准：spec.type 映射为存量 blockType 后存在于列表区块目录（bridge 红线测试保证 59 项一致）
const unifiedPaletteBlockTypes = new Set(toListPageBlockCatalog().map(item => item.blockType))
const unifiedPaletteTotal = ref(0)

function resolveUnifiedBlockType(spec = {}) {
  return LIST_BLOCK_TYPE_OVERRIDES[spec.type] || spec.type
}

/**
 * B2 修正：画布不支持的组件直接隐藏（与表单侧 ForgeFieldShelf 策略统一，用户验收反馈：禁用态+开发术语同显太乱）。
 * 字段组件由表单/查询区块承载、列表画布暂无渲染分支的类型、旧 zone 画布专属操作组件 —— 统一不展示。
 * 注册表仍是唯一事实源，两侧加/改组件只改 spec。
 */
function listPaletteItemFilter(spec) {
  // 基础过滤：排除包装节点
  if (!isPaletteUnionSpec(spec))
    return false
  // 字段组件在列表画布由表单区块/查询区块承载，不直接拖入 → 隐藏
  if (spec.category === 'field')
    return false
  const blockType = resolveUnifiedBlockType(spec)
  if (LIST_PALETTE_EXCLUDED_TYPES.includes(spec.type) || LIST_PALETTE_EXCLUDED_TYPES.includes(blockType))
    return false
  if (!unifiedPaletteBlockTypes.has(blockType)) {
    // 列表画布渲染器暂无分支 / 旧 zone 画布专属 / 表单画布专属 → 隐藏
    return false
  }
  return true
}

/**
 * 列表画布临时禁用原因：只保留用户可理解、可操作的原因（唯一性约束 / 布局限制）。
 * 不再出现"由表单区块承载"/"待流式画布支持"等开发术语。
 */
function unifiedPaletteDisabledReason(spec) {
  const blockType = resolveUnifiedBlockType(spec)
  if (spec.meta?.unique && findExistingBlockByType(blockType))
    return '已在画布中'
  if (spec.meta?.onlyFor && !spec.meta.onlyFor.includes(props.layoutType))
    return '当前布局不可用'
  return ''
}

function handleUnifiedPaletteDragStart({ spec, event }) {
  if (props.readonly) {
    event.preventDefault()
    return
  }
  const blockType = resolveUnifiedBlockType(spec)
  draggedBlockType.value = blockType
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-list-block', blockType)
}

function handleUnifiedPaletteClick(spec) {
  if (props.readonly)
    return
  const blockType = resolveUnifiedBlockType(spec)
  if (spec.meta?.unique) {
    const existingBlock = findExistingBlockByType(blockType)
    if (existingBlock) {
      selectBlock(existingBlock.id)
      scrollBlockIntoView(existingBlock)
      return
    }
  }
  if (spec.meta?.onlyFor && !spec.meta.onlyFor.includes(props.layoutType)) {
    window.$message?.info('当前布局不可用')
    return
  }
  appendBlock(blockType)
}

const showChildListDisplaySetting = computed(() => {
  return selectedBlock.value?.blockType === 'AiCrudPage'
    && props.fields.some(field => isChildListField(field))
})

const crudTablePanelFields = computed(() => {
  if (selectedBlock.value?.blockType !== 'AiCrudPage')
    return []
  const tableFields = props.fields.filter(field => isListFieldSelectable(field, 'table') && field?.field)
  const refIndex = new Map(resolveSelectedFieldRefs(selectedBlock.value, 'table', props.fields).map((ref, index) => [ref, index]))
  return [...tableFields].sort((left, right) => {
    const leftIndex = refIndex.has(left.field) ? refIndex.get(left.field) : Number.MAX_SAFE_INTEGER
    const rightIndex = refIndex.has(right.field) ? refIndex.get(right.field) : Number.MAX_SAFE_INTEGER
    if (leftIndex !== rightIndex)
      return leftIndex - rightIndex
    return tableFields.indexOf(left) - tableFields.indexOf(right)
  })
})

watch(
  selectedBlockId,
  (blockId) => {
    const block = findBlockInTree(blocks.value, blockId)
    propertyCollapseExpandedNames.value = ['base']
    if (block?.blockType === 'tabs') {
      const firstKey = block.props?.tabs?.[0]?.key || ''
      if (!block.props?.tabs?.some(tab => tab.key === activeTabKey.value))
        activeTabKey.value = firstKey
    }
  },
)

watch(
  () => [selectedBlock.value?.blockType, selectedTreeSourceValue.value],
  async ([blockType, sourceValue]) => {
    if (blockType !== 'tree-panel') {
      treeSourceFields.value = []
      return
    }
    await ensureTreeSourceCatalog()
    if (sourceValue)
      await loadTreeSourceFields(sourceValue)
    else
      treeSourceFields.value = []
  },
)

watch(
  propertyKeyword,
  (value) => {
    const keyword = String(value || '').trim().toLowerCase()
    if (!keyword)
      return
    propertyPanelTab.value = resolvePropertySearchTab(keyword)
    propertyCollapseExpandedNames.value = [...allPropertyCollapseNames]
    nextTick(() => {
      scrollPropertySearchTarget(keyword)
      setTimeout(() => scrollPropertySearchTarget(keyword), 80)
    })
  },
)

watch(
  () => props.modelValue,
  (value) => {
    const next = normalizeDesignerLayout(syncGridLayoutWithModel(value, buildGridSyncModelSchema(props.modelSchema, props.fields), { layoutType: props.layoutType }))
    if (JSON.stringify(next) !== JSON.stringify(localLayout.value))
      localLayout.value = next
  },
  { deep: true },
)

watch(
  () => props.modelSchema,
  () => {
    localLayout.value = normalizeDesignerLayout(syncGridLayoutWithModel(localLayout.value, buildGridSyncModelSchema(props.modelSchema, props.fields), { layoutType: props.layoutType }))
  },
  { deep: true },
)

watch(
  () => props.layoutType,
  () => {
    localLayout.value = normalizeDesignerLayout(syncGridLayoutWithModel(localLayout.value, buildGridSyncModelSchema(props.modelSchema, props.fields), { layoutType: props.layoutType }))
  },
)

watch(
  blocks,
  (value) => {
    const blockIds = new Set(value.map(block => block.id))
    const nextMap = Object.fromEntries(
      Object.entries(collapsedTreePanelMap.value).filter(([id]) => blockIds.has(id)),
    )
    if (Object.keys(nextMap).length !== Object.keys(collapsedTreePanelMap.value).length)
      collapsedTreePanelMap.value = nextMap
  },
  { deep: true },
)

watch(
  localLayout,
  (value) => {
    if (props.readonly)
      return
    if (designerStore.deferLayoutEmit) {
      designerStore.markDeferredLayoutEmit()
      return
    }
    emitLayoutChange(value)
  },
  { deep: true },
)

function emitLayoutChange(value = localLayout.value) {
  emit('update:modelValue', JSON.parse(JSON.stringify(value)))
}

function openSourceModal() {
  layoutSourceDraft.value = layoutCodeText.value
  blockSourceDraft.value = selectedBlockCodeText.value
  sourceModalTab.value = selectedBlock.value ? 'block' : 'layout'
  sourceError.value = ''
  sourceModalOpen.value = true
}

function cancelSourceModalEdit() {
  layoutSourceDraft.value = ''
  blockSourceDraft.value = ''
  sourceError.value = ''
  sourceModalOpen.value = false
}

/** Function ref: Options/inject proxy auto-unwraps `propertyPanelRef` in child templates. */
function setPropertyPanelRef(el) {
  propertyPanelRef.value = el
}

async function hydrateInitialState() {
  localLayout.value = normalizeDesignerLayout(syncGridLayoutWithModel(
    props.modelValue || createDefaultListGridLayout(props.modelSchema, { layoutType: props.layoutType }),
    buildGridSyncModelSchema(props.modelSchema, props.fields),
    { layoutType: props.layoutType },
  ))
  // one-shot equivalent of the former immediate tree-panel watch
  const blockType = selectedBlock.value?.blockType
  const sourceValue = selectedTreeSourceValue.value
  if (blockType === 'tree-panel') {
    await ensureTreeSourceCatalog()
    if (sourceValue)
      await loadTreeSourceFields(sourceValue)
    else
      treeSourceFields.value = []
  }
}
__impl.hydrateInitialState = hydrateInitialState


  return {
    __impl,
    mut,
    hydrateInitialState,
    designerStore,
    rowHeight,
    gap,
    previewMinWidth,
    TREE_PANEL_COLLAPSED_WIDTH,
    canvasRef,
    canvasScrollRef,
    selectedBlockId,
    fieldDrawerOpen,
    specDrawerVisible,
    fieldDrawerMode,
    fieldDrawerInitialField,
    expandDescriptionFieldPanelOpen,
    customActionModalOpen,
    sourceModalOpen,
    sourceModalTab,
    layoutSourceDraft,
    blockSourceDraft,
    sourceError,
    activeActionIndex,
    propertyPanelTab,
    propertyPanelRef,
    paletteKeyword,
    propertyKeyword,
    propertyCollapseExpandedNames,
    allPropertyCollapseNames,
    propertySearchTabIndex,
    propertySearchKeywordRegistry,
    paletteCollapsed,
    propertyCollapsed,
    canvasFocusMode,
    canvasPreviewMode,
    activeTabKey,
    canvasDragActive,
    draggedBlockType,
    draggedExistingBlockId,
    dragOverCell,
    dragOverPoint,
    dragBlockedBlockId,
    activeDropCell,
    movingBlockId,
    movingPreviewBlock,
    movingPixelOffset,
    nestedMovingBlockId,
    canvasViewportWidth,
    canvasZoom,
    localLayout,
    blocks,
    runtimeTreeFilter,
    runtimeTreeActiveKey,
    collapsedTreePanelMap,
    resolvedRuntimeCrudProps,
    designCanvasWidth,
    totalRows,
    colWidth,
    canvasGridWidth,
    canvasGridHeight,
    canvasStyle,
    canvasZoomLabel,
    canvasPreviewModeLabel,
    canvasViewportSummary,
    canvasScaleStyle,
    canvasZoomStageStyle,
    collapsedTreeFrames,
    selectedBlock,
    SPEC_PANEL_EXCLUDED_PROPS,
    specPanelExcludeKeys,
    specPanelPropertyCount,
    handleSpecPropUpdate,
    selectedAiCrudFormModalProps,
    selectedBlockEvents,
    selectedBlockStyle,
    selectedBlockBackgroundHex,
    selectedBlockBorderHex,
    selectedBlockBackgroundPreview,
    selectedBlockBackgroundColorInput,
    selectedBlockBorderPreview,
    selectedBlockFrame,
    selectedBlockWidthMode,
    selectedBlockHeightMode,
    selectedBlockContentAlign,
    selectedBlockFixedWidth,
    layoutCodeText,
    selectedBlockCodeText,
    externalCustomActionsEnabled,
    customActionList,
    toolbarCustomActions,
    rowCustomActions,
    activeAction,
    apiConfigs,
    apiConfigLoading,
    apiConfigLoaded,
    systemMenuPages,
    systemMenuPageLoading,
    systemMenuPageLoaded,
    apiConfigOptions,
    systemMenuPageTargetOptions,
    dropPreviewLabel,
    dropPreviewStyle,
    blockedDropPreviewStyle,
    movePlaceholderStyle,
    layoutTitle,
    selectedBlockMeta,
    resolveDraggedPreviewBlockType,
    resolveDetachedBlockFrame,
    blockTargetOptions,
    pageTargetOptions,
    formTargetOptions,
    rowFieldOptions,
    runtimeRuleFieldOptions,
    NESTED_CONTAINER_BLOCK_TYPES,
    isTopLevelBlockId,
    childBlockTypeOptions,
    tabPaneOptions,
    activeTabChildren,
    selectedGridCells,
    primaryModelCode,
    primaryFieldOptions,
    sortFieldOptions,
    defaultApiValues,
    treeSourceCatalog,
    treeSourceFields,
    treeSourceLoading,
    treeSourceFieldsLoading,
    currentListObjectCodes,
    selectedTreeSourceValue,
    treeSourceOptions,
    treeFieldOptions,
    embeddedTreeConfig,
    embeddedTreeEnabled,
    findPrimaryListCrudBlock,
    treeAddChildEnabled,
    commitEmbeddedTreeModel,
    ensureEmbeddedTreeModel,
    disableEmbeddedTreeModel,
    updateEmbeddedTreeEnabled,
    patchEmbeddedTreeConfig,
    updateTreeAddChildEnabled,
    unifiedPaletteBlockTypes,
    unifiedPaletteTotal,
    resolveUnifiedBlockType,
    listPaletteItemFilter,
    unifiedPaletteDisabledReason,
    handleUnifiedPaletteDragStart,
    handleUnifiedPaletteClick,
    showChildListDisplaySetting,
    crudTablePanelFields,
    emitLayoutChange,
    openSourceModal,
    cancelSourceModalEdit,
    setPropertyPanelRef,
    // Template constants (Options shell + inject proxy require explicit return)
    alignOptions,
    normalizeParamName,
    pageWidgetComponentKeys,
    isChildListField,
    resolveChildListDisplayHint,
    canvasWidthOptions,
    canvasZoomOptions,
    canvasPreviewModeOptions,
    crudPreviewModeOptions,
    actionPositionOptions,
    actionTypeOptions,
    actionBehaviorOptions,
    actionOpenTargetOptions,
    successBehaviorOptions,
    apiMethodOptions,
    apiParamTargetOptions,
    paramSourceOptions,
    routeParamOptions,
    systemVariableOptions,
    labelPlacementOptions,
    labelAlignOptions,
    gridVerticalAlignOptions,
    directionOptions,
    splitDirectionOptions,
    menuModeOptions,
    alertTypeOptions,
    richEditorModeOptions,
    wangEditorModeOptions,
    watermarkFontStyleOptions,
    watermarkTextAlignOptions,
    barcodeFormatOptions,
    qrcodeErrorCorrectionOptions,
    qrcodeDotsTypeOptions,
    qrcodeCornerTypeOptions,
    transferDataSourceOptions,
    widgetDataSourceOptions,
    detailInfoDataSourceOptions,
    dataBindablePageWidgetKeys,
    localDataBindableBlockTypes,
    markdownPreviewModeOptions,
    htmlTagOptions,
    htmlRenderModeOptions,
    vuePreviewModeOptions,
    justifyContentOptions,
    simpleConfigBlockTypes,
    componentSizeOptions,
    tableDensityOptions,
    renderModeOptions,
    expandTriggerOptions,
    expandLayoutModeOptions,
    expandPanelTypeOptions,
    expandDataSourceTypeOptions,
    requestMethodOptions,
    tagTypeOptions,
    treeLoadModeOptions,
    sortOrderOptions,
    shadowOptions,
    eventTriggerOptions,
    blockEventActionOptions,
    backButtonActionOptions,
    resizeAnchors,
  }
}
