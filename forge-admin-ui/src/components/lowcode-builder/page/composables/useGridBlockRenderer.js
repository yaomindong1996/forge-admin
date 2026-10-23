/** GridBlockRenderer setup. */
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { useRoute, useRouter } from 'vue-router'
import { createRuntimeBlockApi, findNestedAiCrudApi, registerRuntimeBlock } from '../runtime-block-registry'
import {
  applyPageNavigationQuery,
  handleRuntimeEventSuccess as runRuntimeEventSuccess,
  navigateRuntimeEvent as runNavigateRuntimeEvent,
  runBlockClickEvent,
} from '../runtime-block-events'
import { isPageWidgetComponentKey, pageWidgetComponentKeys } from '@/components/lowcode-builder/shared/page-widget-schema'
import { isComposeDisplayEnabled, resolveComposeDisplayValue } from '@/components/lowcode-builder/shared/widget-field-catalog'
import {
  buildBindingPreviewRecord,
  hasResolvedBindingValues,
  isBindingSlotVisible,
  isExplicitBindingPath,
  resolveWidgetBindingProfile,
  resolveWidgetRenderMode,
} from '@/components/lowcode-builder/shared/widget-binding-slots'
import { resolveCrudPagePresentation } from '@/components/lowcode-builder/shared/runtime-crud-page-mode'
import { buildCrudSearchTypeRequestParams, normalizeTableRowGap, resolveCrudPreviewReloadKey, resolveCrudSearchFieldCatalog, shouldUseStaticCrudPreview } from '@/components/lowcode-builder/shared/runtime-crud-props'
import {
  resolveRuntimeListGridModel,
  shouldRenderRuntimeListGrid,
} from '@/components/lowcode-builder/shared/runtime-list-grid'
import { collectTreeFilterValues } from '@/components/lowcode-builder/shared/runtime-tree-table'
import { matchSimpleExpression, resolveRuntimeControl } from '@/components/lowcode-builder/shared/runtime-rules'
import { resolveDefaultSearchComponentType } from '../fieldDrawerConfig'
import {
  DEFAULT_NEST_EXCLUDE_BLOCK_TYPES,
  GRID_CELL_NEST_EXCLUDE_BLOCK_TYPES,
} from '../page-block-picker'
import { useUserStore } from '@/store'
import { postEncrypt, request } from '@/utils'
import { applyCrudHookRules, CRUD_HOOK_RULE_TARGETS, normalizeCrudHookRules } from '../crud-hook-rules'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import { isDataFieldBlockType, resolveListFieldTitle } from '../page-schema'
import { resolveWidgetLinkAction } from '../free-layout-widget-kit.js'
import { safeHtml as sanitizeCustomHtmlContent } from '@/components/lowcode-builder/shared/page-widget-schema'
import { buildRuntimeCrudBlockProps, resolveEffectiveFormOpenMode, resolveEffectiveModalType } from '../runtime-crud-block-props'
import {
  resolvePreviewChildHeight,
  normalizeCssNumberValue,
  normalizeCssNumber,
  hasGridCellChildren,
  resolveFormLayoutBlockType,
  nestedChildShellStyle,
  isNestedFillHeightChild,
  resolveSurfaceBackgroundStyle,
  resolveChartBlockHeight,
  normalizeCssSize,
  createLegacyPageTitleContent,
  resolvePageTitleRichContent,
  mergeBlockTypographyStyle,
  escapeRichText,
  loadBarcodeModule,
  componentType,
  resolveTitleFontSize,
  sanitizeIframeSrc,
  toOptionalNumber,
  isDefaultAiFormField,
  resolveSearchOptionSource,
  toAiFormField,
  resolveAiFieldType,
  trendClass,
  sampleValue,
  extractBlockBoundRows,
  normalizeBlockRow,
  interpolateText,
  buildRuntimeTemplateContext,
  interpolateApiUrl,
  getNestedRecordValue,
  parseApiConfigValue,
  extractRuntimeTreeRows,
  countTreeNodes,
  collectTreeKeys,
  normalizeRuntimeTreeConfig,
  resolveTemplatePlaceholder,
  matchDisplayCondition,
  parseRuntimeApiConfig,
  confirmRuntimeAction,
  toCssSize,
  parseInlineStyle,
} from '../gridBlockRendererUtils.js'
import { stripAccidentalSelectionChrome } from '../block-style-chrome.js'
import { buildChartDataFromRows } from '../page-chart-option.js'
export function useGridBlockRenderer(props, emit) {
const localDataBindableBlockTypes = new Set([
  'stats-strip',
  'quick-entry',
  'top-ranking',
  'bar-chart',
  'line-chart',
  'area-chart',
  'pie-chart',
  'detail-info',
  'info-panel',
  'custom-html',
  'tag-list',
  'steps',
  'timeline',
  'empty-state',
  'text-title',
  'paragraph',
  'statistic',
  'link',
  'text-tip',
  'audio-player',
  'video-player',
  'avatar',
])

const route = useRoute()
const router = useRouter()
const runtimeMessage = useMessage()
const userStore = useUserStore()
const fieldMap = computed(() => new Map(props.fields.map(f => [f.field, f])))
const isDataFieldBlock = computed(() => isDataFieldBlockType(props.block.blockType))
const configuredFieldRefs = computed(() => Array.isArray(props.block.fieldRefs) ? props.block.fieldRefs.filter(Boolean) : [])
const resolvedFields = computed(() => {
  if (!configuredFieldRefs.value.length && isDataFieldBlock.value)
    return props.fields
  return configuredFieldRefs.value.map(ref => fieldMap.value.get(ref)).filter(Boolean)
})
const shouldShowDataSourceGuide = computed(() => props.showDataSourceGuide
  && isDataFieldBlock.value
  && !props.dataSourceConfigured
  && props.fields.length === 0)
const visibleResolvedFields = computed(() => resolvedFields.value
  .filter(field => props.block.props?.fieldSettings?.[field.field]?.visible !== false))
const previewFormValue = ref({})
const aiFormRef = ref(null)
const aiFormSubmitting = ref(false)
const signaturePreviewValue = ref('')
const runtimeCrudRef = ref(null)
const runtimeTreeLoading = ref(false)
const runtimeTreeNodes = ref([])
const runtimeTreeError = ref('')
const runtimeTreeNodeMap = ref(new Map())
const runtimeExpandedTreeKeys = ref([])
const detailInfoLoading = ref(false)
const detailInfoError = ref('')
const remoteDetailInfoRecord = ref({})
const blockBindingLoading = ref(false)
const blockBindingError = ref('')
const remoteBlockBindingData = ref(null)
const previewTreeExpanded = ref(true)
const treePanelCollapsed = ref(false)
const activeTabKeyByBlockId = ref({})
const runtimeTreeChildrenField = computed(() => props.block.props?.childrenField || 'children')
const runtimeTreeConfig = computed(() => normalizeRuntimeTreeConfig(props.block.props || {}))
const runtimeTreeLoadMode = computed(() => runtimeTreeConfig.value.loadMode)
const hasRuntimeTreeApi = computed(() => Boolean(resolveRuntimeTreeApi()))
const runtimeTreeTotal = computed(() => countTreeNodes(runtimeTreeNodes.value, runtimeTreeChildrenField.value))
const runtimeSelectedTreeKeys = computed(() => props.runtimeTreeActiveKey === '__all__' ? [] : [props.runtimeTreeActiveKey])
const runtimeRuleContext = computed(() => ({
  record: props.runtimeRecord || {},
  row: props.runtimeRecord || {},
  data: props.runtimeRecord || {},
  route: {
    query: route.query || {},
    params: route.params || {},
    path: route.path,
    fullPath: route.fullPath,
    name: route.name,
  },
  user: userStore.userInfo || userStore.user || {},
}))
const blockRuntimeControl = computed(() => resolveRuntimeControl(props.block || {}, runtimeRuleContext.value))
const hasMeaningfulRuntimeRecord = computed(() => {
  const record = props.runtimeRecord
  return Boolean(record && typeof record === 'object' && Object.keys(record).length > 0)
})
// 设计预览（readonly 且无行上下文）：只尊重显式 hidden，不套用 whenUnmatched:hidden
const blockRuntimeVisible = computed(() => {
  if (!props.readonly)
    return true
  const block = props.block || {}
  const explicitlyHidden = block.hidden === true
    || block.visible === false
    || block.visibility?.hidden === true
  if (!props.runtimeInteractive && !hasMeaningfulRuntimeRecord.value)
    return !explicitlyHidden
  return blockRuntimeControl.value.visible !== false
})
function isDetailInfoBindingRemote(binding = {}) {
  return binding.enabled === true && binding.sourceType === 'remote'
}

function isDetailInfoBindingContext(binding = {}) {
  return binding.enabled === true && binding.sourceType === 'context'
}

const detailInfoRecord = computed(() => {
  if (props.block.blockType !== 'detail-info')
    return props.runtimeRecord || {}
  const binding = props.block.props?.dataBinding || {}
  // 统一 dataBinding（与图表远程接口同一路径）
  if (isDetailInfoBindingRemote(binding)) {
    const source = remoteBlockBindingData.value
    if (!source)
      return {}
    const dataPath = binding.dataPath
    if (!dataPath)
      return source || {}
    return getNestedRecordValue(source, dataPath) || {}
  }
  if (isDetailInfoBindingContext(binding)) {
    const path = binding.contextPath || props.block.props?.contextPath || ''
    if (path)
      return getNestedRecordValue(props.runtimeRecord || {}, path) || {}
    return props.runtimeRecord || {}
  }
  // 存量：仅有 dataSourceType / detailApi
  const sourceType = props.block.props?.dataSourceType || 'remote'
  if (sourceType === 'remote')
    return remoteDetailInfoRecord.value || {}
  const path = props.block.props?.contextPath || ''
  if (path)
    return getNestedRecordValue(props.runtimeRecord || {}, path) || {}
  return props.runtimeRecord || {}
})

const detailInfoBusy = computed(() => {
  if (props.block.blockType !== 'detail-info')
    return false
  const binding = props.block.props?.dataBinding || {}
  if (isDetailInfoBindingRemote(binding))
    return blockBindingLoading.value
  return detailInfoLoading.value
})

const detailInfoFailMessage = computed(() => {
  if (props.block.blockType !== 'detail-info')
    return ''
  const binding = props.block.props?.dataBinding || {}
  if (isDetailInfoBindingRemote(binding))
    return blockBindingError.value || ''
  return detailInfoError.value || ''
})
const blockBoundData = computed(() => {
  const binding = props.block.props?.dataBinding || {}
  if (binding.enabled !== true || binding.sourceType === 'static')
    return null
  // 手动多项模式：取整条详情对象（不按数组截断），供每张指标卡/入口单独绑字段
  if (
    ['stats-strip', 'quick-entry'].includes(props.block.blockType)
    && resolveWidgetRenderMode(binding, props.block.blockType) === 'manual'
  ) {
    return resolveContextObjectSource(binding)
  }
  if (binding.sourceType === 'remote') {
    const source = remoteBlockBindingData.value
    if (!source)
      return null
    const dataPath = binding.dataPath
    if (!dataPath)
      return source
    const nested = getNestedRecordValue(source, dataPath)
    return nested === undefined || nested === null ? source : nested
  }
  // context：有真实详情用真实值；否则按已绑字段生成预览样例，避免仍显示静态占位
  const realRecord = props.runtimeRecord || {}
  let source = realRecord
  if (!hasResolvedBindingValues(realRecord, binding, props.block.blockType, props.fields)) {
    const preview = buildBindingPreviewRecord(binding, props.fields, props.block.blockType)
    if (Array.isArray(preview))
      return preview
    source = { ...preview, ...realRecord }
  }
  const dataPath = binding.contextPath
  if (!dataPath)
    return source
  const nested = getNestedRecordValue(source, dataPath)
  return nested === undefined || nested === null ? source : nested
})
function resolveContextObjectSource(binding = {}) {
  if (binding.sourceType === 'remote') {
    const source = remoteBlockBindingData.value
    if (!source)
      return null
    const dataPath = binding.dataPath
    if (!dataPath)
      return source
    const nested = getNestedRecordValue(source, dataPath)
    return nested === undefined || nested === null ? source : nested
  }
  const realRecord = props.runtimeRecord || {}
  let source = realRecord
  if (!hasResolvedBindingValues(realRecord, binding, props.block.blockType, props.fields)) {
    const preview = buildBindingPreviewRecord(
      { ...binding, renderMode: 'manual' },
      props.fields,
      'statistic',
    )
    source = { ...(preview && !Array.isArray(preview) ? preview : {}), ...realRecord }
  }
  const dataPath = binding.contextPath
  if (!dataPath)
    return source
  const nested = getNestedRecordValue(source, dataPath)
  return nested === undefined || nested === null ? source : nested
}
function isBlockSlotVisible(slotKey) {
  return isBindingSlotVisible(props.block.props?.dataBinding || {}, slotKey)
}
const isLocalDataBindableBlock = computed(() => localDataBindableBlockTypes.has(props.block.blockType))
const showBlockBindingState = computed(() => isLocalDataBindableBlock.value && (blockBindingLoading.value || blockBindingError.value))
const statsMetrics = computed(() => {
  const binding = props.block.props?.dataBinding || {}
  const mode = resolveWidgetRenderMode(binding, 'stats-strip')
  const metrics = Array.isArray(props.block.props?.metrics) ? props.block.props.metrics : []
  if (mode === 'list' && binding.enabled === true && binding.sourceType !== 'static') {
    return normalizeBlockBoundRows(metrics).map((row, index) => ({
      key: row.key || `metric_${index + 1}`,
      label: boundRowField(row, 'labelField', 'label', `指标${index + 1}`),
      value: boundRowField(row, 'valueField', 'value', '-'),
      trend: boundRowField(row, 'metaField', 'trend', ''),
      icon: row.icon || '',
      tone: row.tone || 'primary',
      color: row.color || '',
      linkType: row.linkType || (row.targetPageId ? 'page' : 'none'),
      targetPageId: String(row.targetPageId || ''),
      href: String(row.href || ''),
    }))
  }
  // 手动多项：始终用 metrics 数组长度；单项可绑字段覆盖静态值
  const data = blockBoundData.value
  return metrics.map((metric, index) => ({
    key: metric.key || `metric_${index + 1}`,
    label: resolveManualMetricValue(metric, data, 'labelField', 'label', metric.label || `指标${index + 1}`),
    value: resolveManualMetricValue(metric, data, 'valueField', 'value', metric.value ?? '-'),
    trend: resolveManualMetricValue(metric, data, 'metaField', 'trend', metric.trend || ''),
    icon: metric.icon || '',
    tone: metric.tone || 'primary',
    color: metric.color || '',
    linkType: metric.linkType || (metric.targetPageId ? 'page' : 'none'),
    targetPageId: String(metric.targetPageId || ''),
    href: String(metric.href || ''),
  }))
})

function resolveManualMetricValue(metric = {}, data, fieldKey, defaultField, fallback) {
  const binding = props.block.props?.dataBinding || {}
  const path = String(metric?.[fieldKey] || binding[fieldKey] || '').trim()
  if (path && isExplicitBindingPath(path, props.fields) && data && typeof data === 'object' && !Array.isArray(data)) {
    const value = getNestedRecordValue(data, path)
    if (value !== undefined && value !== null && value !== '')
      return value
  }
  if (metric?.[defaultField] !== undefined && metric?.[defaultField] !== null && metric?.[defaultField] !== '')
    return metric[defaultField]
  return fallback
}

const quickEntryItems = computed(() => {
  const binding = props.block.props?.dataBinding || {}
  const mode = resolveWidgetRenderMode(binding, 'quick-entry')
  const items = Array.isArray(props.block.props?.items) ? props.block.props.items : []
  if (mode === 'list' && binding.enabled === true && binding.sourceType !== 'static') {
    return normalizeBlockBoundRows(items).map((row, index) => ({
      key: row.key || `entry_${index + 1}`,
      title: boundRowField(row, 'titleField', 'title', `入口${index + 1}`),
      description: boundRowField(row, 'descriptionField', 'description', ''),
      targetPageId: String(boundRowField(row, 'valueField', 'targetPageId', row.pageId || '') || ''),
      icon: boundRowField(row, 'metaField', 'icon', 'GridOutline'),
      tone: row.tone || 'primary',
      color: row.color || '',
      linkType: row.linkType || '',
      href: String(row.href || row.url || ''),
    }))
  }
  const data = blockBoundData.value
  return items.map((item, index) => ({
    key: item.key || `entry_${index + 1}`,
    title: resolveManualMetricValue(item, data, 'titleField', 'title', item.title || `入口${index + 1}`),
    description: resolveManualMetricValue(item, data, 'descriptionField', 'description', item.description || ''),
    targetPageId: String(resolveManualMetricValue(item, data, 'valueField', 'targetPageId', item.targetPageId || item.pageId || '') || ''),
    icon: resolveManualMetricValue(item, data, 'metaField', 'icon', item.icon || 'GridOutline'),
    tone: item.tone || 'primary',
    color: item.color || '',
    linkType: item.linkType || '',
    href: String(item.href || item.url || ''),
  }))
})

const topRankingItems = computed(() => {
  const binding = props.block.props?.dataBinding || {}
  const mode = resolveWidgetRenderMode(binding, 'top-ranking')
  const items = Array.isArray(props.block.props?.items) ? props.block.props.items : []
  const defaultUnit = String(props.block.props?.valueUnit || '')
  if (mode === 'list' && binding.enabled === true && binding.sourceType !== 'static') {
    return normalizeBlockBoundRows(items).map((row, index) => ({
      key: row.key || `rank_${index + 1}`,
      name: boundRowField(row, 'labelField', 'name', `项目${index + 1}`),
      value: boundRowField(row, 'valueField', 'value', 0),
      unit: boundRowField(row, 'metaField', 'unit', defaultUnit),
      targetPageId: String(row.targetPageId || ''),
      href: String(row.href || row.url || ''),
      linkType: row.linkType || '',
    }))
  }
  const data = blockBoundData.value
  return items.map((item, index) => ({
    key: item.key || `rank_${index + 1}`,
    name: resolveManualMetricValue(item, data, 'labelField', 'name', item.name || item.label || `项目${index + 1}`),
    value: resolveManualMetricValue(item, data, 'valueField', 'value', item.value ?? 0),
    unit: resolveManualMetricValue(item, data, 'metaField', 'unit', item.unit || defaultUnit),
    targetPageId: String(item.targetPageId || ''),
    href: String(item.href || item.url || ''),
    linkType: item.linkType || '',
  }))
})

const chartSeriesPayload = computed(() => {
  const blockType = props.block.blockType
  if (!['bar-chart', 'line-chart', 'area-chart', 'pie-chart'].includes(blockType))
    return { categories: [], series: [] }
  const binding = props.block.props?.dataBinding || {}
  const mode = resolveWidgetRenderMode(binding, blockType)
  if (mode === 'list' && binding.enabled === true && binding.sourceType !== 'static') {
    const fallbackRows = Array.isArray(props.block.props?.chartRows) ? props.block.props.chartRows : []
    return buildChartDataFromRows(normalizeBlockBoundRows(fallbackRows), {
      labelField: binding.labelField || 'category',
      valueField: binding.valueField || 'value',
      metaField: binding.metaField || 'series',
    })
  }
  return {
    categories: Array.isArray(props.block.props?.categories) ? props.block.props.categories : [],
    series: Array.isArray(props.block.props?.series) ? props.block.props.series : [],
  }
})

async function handleQuickEntrySelect(item = {}) {
  await handleWidgetLinkSelect(item)
}

/** 工作台路径不得作为应用内「全部」跳转 */
const WORKBENCH_TASK_PATHS = new Set([
  '/flow/todo',
  '/flow/done',
  '/workspace/todo',
  '/workspace/done',
])

function sanitizeWorkflowExternalPath(path = '') {
  const raw = String(path || '').trim()
  if (!raw)
    return ''
  const bare = raw.split('?')[0].replace(/\/+$/, '') || '/'
  if (WORKBENCH_TASK_PATHS.has(bare))
    return ''
  return raw
}

/**
 * 待办/已办「全部」与行点击：优先应用内 pageId，避免冲到工作台 /flow/todo。
 * @param {{ pageId?: string, path?: string, taskId?: string, mode?: string }} payload
 */
async function handleWorkflowTaskNavigate(payload = {}) {
  if (!props.readonly)
    return
  const pageId = String(payload.pageId || '').trim()
  const path = sanitizeWorkflowExternalPath(payload.path)
  const taskId = String(payload.taskId || '').trim()
  if (pageId) {
    const params = taskId
      ? [{ name: 'taskId', value: taskId }, { name: 'source', value: 'free-layout' }]
      : []
    await runBlockClickEvent({
      trigger: 'click',
      action: 'navigate',
      targetPageKey: pageId,
      params,
    }, buildRuntimeEventContext())
    return
  }
  if (path) {
    if (taskId) {
      router.push({ path, query: { taskId, source: 'free-layout', t: Date.now() } })
      return
    }
    router.push(path)
    return
  }
  runtimeMessage.warning('请先在属性面板「跳转」里选择应用内的待办/已办目标页')
}

/** 快捷入口 / 指标卡 / 提示面板统一跳转：应用内页面或外部链接 */
async function handleWidgetLinkSelect(item = {}) {
  if (!props.readonly)
    return
  const link = resolveWidgetLinkAction(item)
  if (link.type === 'page') {
    await runBlockClickEvent({
      trigger: 'click',
      action: 'navigate',
      targetPageKey: link.targetPageId,
    }, buildRuntimeEventContext())
    return
  }
  if (link.type === 'url') {
    await runBlockClickEvent({
      trigger: 'click',
      action: 'openUrl',
      href: link.href,
    }, buildRuntimeEventContext())
  }
}
const boundInfoTitle = computed(() => boundContentValue(props.block.props?.title || '提示信息', 'titleField', 'title'))
const boundInfoContent = computed(() => boundContentValue(props.block.props?.content || '在右侧填写提示内容', 'contentField', 'content'))
const boundInfoType = computed(() => boundContentValue(props.block.props?.type || 'info', 'valueField', 'type'))
const boundCustomTitle = computed(() => boundContentValue(props.block.props?.title || '', 'titleField', 'title'))
const boundCustomContent = computed(() => boundContentValue(props.block.props?.content || '', 'contentField', 'content'))
const boundTags = computed(() => normalizeBlockBoundRows(props.block.props?.tags || []).map((row, index) => ({
  label: boundRowField(row, 'labelField', 'label', `标签${index + 1}`),
  type: boundRowField(row, 'valueField', 'type', 'default'),
})))
const boundSteps = computed(() => normalizeBlockBoundRows(props.block.props?.steps || []).map((row, index) => ({
  title: boundRowField(row, 'titleField', 'title', `步骤${index + 1}`),
  description: boundRowField(row, 'descriptionField', 'description', ''),
})))
const boundTimelineTitle = computed(() => boundContentValue(props.block.props?.title || '', 'titleField', 'title'))
const boundTimelineItems = computed(() => normalizeBlockBoundRows(props.block.props?.items || []).map((row, index) => ({
  title: boundRowField(row, 'titleField', 'title', `节点${index + 1}`),
  content: boundRowField(row, 'descriptionField', 'content', ''),
  time: boundRowField(row, 'metaField', 'time', ''),
})))
const boundEmptyTitle = computed(() => boundContentValue(props.block.props?.title || '暂无数据', 'titleField', 'title'))
const boundEmptyDescription = computed(() => boundContentValue(props.block.props?.description || '当前没有可展示的数据', 'descriptionField', 'description'))
const boundEmptyActionText = computed(() => boundContentValue(props.block.props?.actionText || '', 'valueField', 'actionText'))
const boundTextTitle = computed(() => boundContentValue(props.block.props?.text || '页面标题', 'titleField', 'text'))
const boundTextSubtitle = computed(() => boundContentValue(props.block.props?.subtitle || '', 'descriptionField', 'subtitle'))
const boundParagraphContent = computed(() => boundContentValue(props.block.props?.content || '段落内容', 'contentField', 'content'))
const boundStatisticTitle = computed(() => boundContentValue(props.block.props?.title || '统计指标', 'titleField', 'title'))
const boundStatisticValue = computed(() => boundContentValue(props.block.props?.value || '0', 'valueField', 'value'))
const boundStatisticDescription = computed(() => boundContentValue(props.block.props?.description || '', 'descriptionField', 'description'))
const boundStatisticTrend = computed(() => boundContentValue(props.block.props?.trend || '', 'metaField', 'trend'))
const boundLinkText = computed(() => boundContentValue(props.block.props?.text || '链接文本', 'titleField', 'text'))
const boundLinkHref = computed(() => boundContentValue(props.block.props?.href || '#', 'valueField', 'href'))
const boundTextTipTitle = computed(() => boundContentValue(props.block.props?.title || '提示', 'titleField', 'title'))
const boundTextTipContent = computed(() => boundContentValue(props.block.props?.content || '提示内容', 'contentField', 'content'))
const boundTextTipType = computed(() => boundContentValue(props.block.props?.type || 'info', 'valueField', 'type'))
const boundMediaTitle = computed(() => boundContentValue(props.block.props?.title || '', 'titleField', 'title'))
const boundMediaSrc = computed(() => boundContentValue(props.block.props?.src || '', 'valueField', 'src'))
const boundVideoPoster = computed(() => boundContentValue(props.block.props?.poster || '', 'metaField', 'poster'))
const boundAvatarName = computed(() => boundContentValue(props.block.props?.name || '用户名称', 'titleField', 'name'))
const boundAvatarDescription = computed(() => boundContentValue(props.block.props?.description || '角色 / 部门', 'descriptionField', 'description'))
const boundAvatarSrc = computed(() => boundContentValue(props.block.props?.src || '', 'valueField', 'src'))
const resizeAnchors = ['top-left', 'top', 'top-right', 'right', 'bottom-right', 'bottom', 'bottom-left', 'left']
const nestedBlockMenuOptions = [
  { label: '复制', key: 'duplicate' },
  { label: '删除', key: 'delete' },
]
/** 嵌套子块操作条：仅悬停/选中挂载，减少容器内大量「更多操作」DOM */
const hoveredNestedBlockId = ref('')
function showNestedChrome(childId = '') {
  const id = String(childId || '')
  if (!id || props.readonly)
    return false
  return id === props.selectedBlockId || id === hoveredNestedBlockId.value
}
function onNestedPointerEnter(childId = '') {
  hoveredNestedBlockId.value = String(childId || '')
}
function onNestedPointerLeave(childId = '') {
  if (hoveredNestedBlockId.value === String(childId || ''))
    hoveredNestedBlockId.value = ''
}
const containerMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  mode: 'insert',
  title: '',
  containerId: '',
  containerType: '',
  cellKey: '',
  tabKey: '',
  childId: '',
})

/** 右键插入目录：卡片等可出栅格；栅格/标签页内仍禁嵌套栅格与标签页 */
const containerMenuExcludeBlockTypes = computed(() => {
  const type = String(props.block?.blockType || '')
  if (type === 'grid-layout' || type === 'tabs')
    return [...GRID_CELL_NEST_EXCLUDE_BLOCK_TYPES]
  return [...DEFAULT_NEST_EXCLUDE_BLOCK_TYPES]
})

function openContainerMenu(payload = {}) {
  if (props.readonly)
    return
  const event = payload.event
  event?.preventDefault?.()
  event?.stopPropagation?.()
  containerMenu.value = {
    visible: true,
    x: Number(event?.clientX || 0),
    y: Number(event?.clientY || 0),
    mode: payload.mode || 'insert',
    title: payload.title || '',
    containerId: String(payload.containerId || props.block.id || ''),
    containerType: String(payload.containerType || props.block.blockType || ''),
    cellKey: String(payload.cellKey || ''),
    tabKey: String(payload.tabKey || ''),
    childId: String(payload.childId || ''),
  }
}

function closeContainerMenu() {
  containerMenu.value = {
    ...containerMenu.value,
    visible: false,
  }
}

function handleGridCellContextMenu(payload = {}) {
  openContainerMenu({
    event: payload.event,
    mode: 'insert',
    title: '插入到栅格',
    containerId: props.block.id,
    containerType: 'grid-layout',
    cellKey: payload.cellKey,
  })
}

function handleContainerSlotContextMenu(event, extra = {}) {
  openContainerMenu({
    event,
    mode: 'insert',
    title: extra.title || '插入组件',
    containerId: props.block.id,
    containerType: props.block.blockType,
    ...extra,
  })
}

function handleChildContextMenu(event, child, extra = {}) {
  openContainerMenu({
    event,
    mode: 'child',
    title: extra.title || child?.label || child?.blockType || '组件操作',
    containerId: props.block.id,
    containerType: props.block.blockType,
    childId: child?.id,
    cellKey: extra.cellKey,
    tabKey: extra.tabKey,
  })
}

function handleContainerMenuInsert(blockType) {
  const menu = containerMenu.value
  closeContainerMenu()
  if (!blockType)
    return
  if (menu.containerType === 'grid-layout') {
    emit('gridCellDrop', {
      blockId: menu.containerId,
      cellKey: menu.cellKey,
      blockType,
    })
    return
  }
  if (menu.containerType === 'tabs') {
    emit('tabDrop', {
      blockId: menu.containerId,
      tabKey: menu.tabKey,
      blockType,
    })
    return
  }
  emit('containerInsert', {
    blockId: menu.containerId,
    containerType: menu.containerType,
    blockType,
    cellKey: menu.cellKey,
    tabKey: menu.tabKey,
  })
}

function handleContainerMenuAction(actionKey) {
  const menu = containerMenu.value
  closeContainerMenu()
  if (menu.mode === 'child' && menu.childId) {
    emit('childBlockMenuSelect', {
      key: actionKey,
      block: { id: menu.childId },
    })
    return
  }
  if (actionKey === 'clear') {
    emit('containerClear', {
      blockId: menu.containerId,
      containerType: menu.containerType,
      cellKey: menu.cellKey,
      tabKey: menu.tabKey,
    })
  }
}
const crudPreviewMode = computed(() => props.block.props?.previewMode || (props.block.props?.previewLiveData === true ? 'realList' : 'mock'))
const crudPreviewReloadKey = computed(() => resolveCrudPreviewReloadKey(props.block, props.runtimeCrudProps))
const gridLayoutCells = computed(() => {
  const rawCells = Array.isArray(props.block.props?.cells) ? props.block.props.cells : []
  const sourceCells = rawCells.length ? rawCells : [{ key: 'cell_1', title: '栅格 1', span: 24, children: [] }]
  return sourceCells.map((cell, index) => {
    const children = Array.isArray(cell.children) ? cell.children : []
    return {
      key: cell.key || `cell_${index + 1}`,
      title: cell.title ?? `栅格 ${index + 1}`,
      span: clampGridSpan(cell.span, 6),
      children,
      minHeight: Number(cell.minHeight || 0) || resolveGridCellPreviewMinHeight(children),
    }
  })
})
function resolveGridCellPreviewMinHeight(children = []) {
  if (!children.length)
    return Math.max(24, Number(props.block.props?.cellMinHeight || 160))
  return children.reduce((sum, child) => sum + resolvePreviewChildHeight(child), 0) + Math.max(0, children.length - 1) * 8 + 16
}
function isActiveDropCell(cell = {}) {
  return props.activeDropCell?.containerId === props.block.id && props.activeDropCell?.cellKey === cell.key
}
const isActiveDropContainer = computed(() => {
  if (!['card', 'box-layout', 'panel-box'].includes(props.block.blockType))
    return false
  const dropContainerId = props.activeDropContainer?.containerId || props.activeDropContainer?.blockId
  if (dropContainerId === props.block.id)
    return true
  // 与栅格同一套命中：卡片/盒子/面板挂了 data-grid-cell-key=__body__
  return props.activeDropCell?.containerId === props.block.id
    && String(props.activeDropCell?.cellKey || '') === '__body__'
})
function shouldShowGridCellEmpty(cell = {}) {
  return !props.readonly && !hasGridCellChildren(cell) && !isActiveDropCell(cell)
}
function isCatalogDragEvent(event) {
  const types = Array.from(event.dataTransfer?.types || [])
  return Boolean(props.catalogDragBlockType)
    || types.includes('application/x-forge-app-page-block')
    || types.includes('application/x-list-block')
    || types.includes('application/x-forge-form-layout')
}
function handleGridCellDragOver(event, cellKey) {
  if (!isCatalogDragEvent(event))
    return
  event.preventDefault()
  event.stopPropagation()
  event.dataTransfer.dropEffect = 'copy'
  emit('gridCellDragOver', { blockId: props.block.id, cellKey })
}
function handleGridCellDrop(event, cellKey) {
  const blockType = props.catalogDragBlockType
    || event.dataTransfer?.getData('application/x-forge-app-page-block')
    || event.dataTransfer?.getData('application/x-list-block')
    || resolveFormLayoutBlockType(event)
  if (!blockType)
    return
  event.preventDefault()
  event.stopPropagation()
  emit('gridCellDrop', {
    blockId: props.block.id,
    cellKey,
    blockType,
  })
}

function handleContainerDragOver(event) {
  if (!isCatalogDragEvent(event))
    return
  event.preventDefault()
  event.stopPropagation()
  event.dataTransfer.dropEffect = 'copy'
  emit('containerDragOver', {
    blockId: props.block.id,
    containerType: props.block.blockType,
  })
}


function handleContainerDrop(event) {
  const blockType = props.catalogDragBlockType
    || event.dataTransfer?.getData('application/x-forge-app-page-block')
    || event.dataTransfer?.getData('application/x-list-block')
    || resolveFormLayoutBlockType(event)
  if (!blockType)
    return
  event.preventDefault()
  event.stopPropagation()
  emit('containerInsert', {
    blockId: props.block.id,
    containerType: props.block.blockType,
    blockType,
  })
}
function handleTabPaneDragOver(event) {
  const types = Array.from(event.dataTransfer?.types || [])
  if (!props.catalogDragBlockType
    && !types.includes('application/x-forge-app-page-block')
    && !types.includes('application/x-list-block')
    && !types.includes('application/x-forge-form-layout')) {
    return
  }
  event.preventDefault()
  event.stopPropagation()
  event.dataTransfer.dropEffect = 'copy'
}

function handleTabPaneDragEnter(event) {
  const types = Array.from(event.dataTransfer?.types || [])
  if (!props.catalogDragBlockType
    && !types.includes('application/x-forge-app-page-block')
    && !types.includes('application/x-list-block')
    && !types.includes('application/x-forge-form-layout')) {
    return
  }
  event.preventDefault()
  event.stopPropagation()
  event.dataTransfer.dropEffect = 'copy'
}

function resolveActiveTabKey(block = {}) {
  const tabs = Array.isArray(block.props?.tabs) ? block.props.tabs : []
  const requested = activeTabKeyByBlockId.value[block.id]
  return tabs.some(tab => tab.key === requested) ? requested : tabs[0]?.key
}

function handleTabsValueChange(block, tabKey) {
  activeTabKeyByBlockId.value = {
    ...activeTabKeyByBlockId.value,
    [block.id]: tabKey,
  }
  emit('tabsActiveChange', { blockId: block.id, tabKey })
}

function handleTabPaneDrop(event, tabKey) {
  const blockType = props.catalogDragBlockType
    || event.dataTransfer?.getData('application/x-forge-app-page-block')
    || event.dataTransfer?.getData('application/x-list-block')
    || resolveFormLayoutBlockType(event)
  if (!blockType)
    return
  event.preventDefault()
  event.stopPropagation()
  emit('tabDrop', {
    blockId: props.block.id,
    tabKey,
    blockType,
  })
}

function clampGridSpan(value, fallback = 1) {
  const columns = Math.max(1, Number(props.block.props?.columns || 24))
  const number = Number(value)
  return Math.max(1, Math.min(columns, Number.isFinite(number) ? number : fallback))
}
const actionButtonVisible = computed(() => {
  if (props.block.blockType !== 'action-button')
    return true
  if (!props.readonly)
    return true
  const eventItem = resolvePrimaryClickEvent()
  return hasRuntimePermission(eventItem?.permissionCode)
    && matchDisplayCondition(eventItem?.displayCondition, props.runtimeRecord || {})
})
const blockStyle = computed(() => {
  const raw = props.block.props?.style || {}
  const {
    pageFlowX: _pageFlowX,
    pageFlowY: _pageFlowY,
    pageFlowWidth: _pageFlowWidth,
    pageFlowHeight: _pageFlowHeight,
    widthMode,
    heightMode,
    ...visualStyle
  } = raw
  // 运行/预览剥离误写入的选中态蓝边，避免样式面板看不见却看得见框
  const safeVisual = props.readonly
    ? stripAccidentalSelectionChrome(visualStyle)
    : visualStyle
  const style = {
    width: '100%',
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
    ...safeVisual,
  }
  const resolvedWidthMode = widthMode || 'full'
  const resolvedHeightMode = heightMode || 'fixed'
  const isChartBlock = ['bar-chart', 'line-chart', 'area-chart', 'pie-chart'].includes(props.block.blockType)
  // 通栏/自适应：强制跟壳宽走，禁止把历史测量出的 px 宽锁死（小屏缩放后切回大屏会变窄）
  const forceFluidWidth = resolvedWidthMode === 'full' || resolvedWidthMode === 'auto'
  // 文档流 auto 高：内容撑开，禁止默认 100% 把 tabs/日历等裁进视口外
  // 表单/详情按字段撑开，避免壳 height:auto 时子块仍写 height:100% 导致裁切
  const forceAutoHeight = !isChartBlock && (
    resolvedHeightMode === 'auto'
    || ['AiForm', 'detail-info'].includes(props.block.blockType)
  )
  // 图表固定跟画布壳 100% 高，锚点改的是外层 .grid-item / 嵌套壳，不能写死 style.height px
  const forceFillHeight = isChartBlock || resolvedHeightMode === 'full'
  const resolvedStyle = {
    width: forceFluidWidth
      ? (resolvedWidthMode === 'auto' ? 'auto' : '100%')
      : toCssSize(style.width, '100%'),
    height: forceAutoHeight
      ? 'auto'
      : forceFillHeight
        ? '100%'
        : toCssSize(style.height, '100%'),
    backgroundColor: style.backgroundColor || 'transparent',
    borderColor: style.borderColor || 'transparent',
    borderWidth: toCssSize(style.borderWidth, '0px'),
    borderStyle: style.borderStyle || 'solid',
    borderRadius: toCssSize(style.borderRadius, '0px'),
    boxShadow: style.boxShadow || 'none',
    minWidth: forceFluidWidth ? '0' : toCssSize(style.minWidth, undefined),
    maxWidth: forceFluidWidth ? '100%' : toCssSize(style.maxWidth, undefined),
    minHeight: toCssSize(style.minHeight, undefined),
    maxHeight: toCssSize(style.maxHeight, undefined),
    margin: toCssSize(style.margin, '0px'),
    padding: toCssSize(style.padding, '0px'),
  }
  if (style.opacity != null && style.opacity !== '')
    resolvedStyle.opacity = style.opacity
  if (style.lineHeight != null && style.lineHeight !== '')
    resolvedStyle.lineHeight = style.lineHeight
  // css/顶层字色字号进外壳；customStyle 最后覆盖
  Object.assign(resolvedStyle, mergeBlockTypographyStyle({}, style))
  Object.assign(resolvedStyle, parseInlineStyle(style.customStyle))
  if (forceFluidWidth) {
    resolvedStyle.width = resolvedWidthMode === 'auto' ? 'auto' : '100%'
    resolvedStyle.maxWidth = '100%'
  }
  if (forceAutoHeight) {
    resolvedStyle.height = 'auto'
  }
  else if (forceFillHeight) {
    resolvedStyle.height = '100%'
  }
  if (props.block.blockType === 'tree-panel' && treePanelCollapsed.value) {
    resolvedStyle.width = '100%'
    resolvedStyle.minWidth = '0px'
    resolvedStyle.maxWidth = '100%'
    resolvedStyle.overflow = 'hidden'
  }
  // 设计态不再给每个透明区块强加灰底虚线（统一由外层选中/hover 描边表达边界）
  return resolvedStyle
})

const tableColumns = computed(() => [
  ...visibleResolvedFields.value.slice(0, 8).map(field => ({
    key: field.field,
    title: field.label || field.field,
    minWidth: 96,
    align: fieldAlign(field.field),
    ellipsis: { tooltip: true },
    render: row => renderTableCell(field, row),
  })),
  { key: '__actions', title: '操作', width: 96, fixed: 'right', align: fieldAlign('__actions') },
])
const aiActionColumn = computed(() => {
  const rowActions = (props.block.props?.customActions || []).filter(action => (action.position || 'row') === 'row' && action.visible !== false)
  if (!rowActions.length)
    return null
  return {
    emit,
    key: 'actions',
    title: '操作',
    width: Math.max(96, rowActions.length * 58),
    fixed: 'right',
    align: fieldAlign('actions'),
    actions: rowActions.map(action => ({
      key: action.key,
      label: action.label || action.key,
      type: action.type || 'primary',
    })),
    render: () => h('div', { class: 'designer-row-actions' }, rowActions.map(action => h('a', {
      key: action.key,
      href: '#',
      class: `designer-row-action type-${action.type || 'primary'}`,
      title: action.routePath || action.targetPageKey || action.actionType || '',
      onClick: event => event.preventDefault(),
    }, action.label || action.key))),
  }
})
const aiTableColumns = computed(() => visibleResolvedFields.value.slice(0, 8).map((field) => {
  const setting = props.block.props?.fieldSettings?.[field.field] || {}
  return {
    key: field.field,
    field: field.field,
    title: resolveListFieldTitle(field, setting, field.field),
    minWidth: setting.minWidth || 110,
    width: setting.width,
    fixed: setting.fixed || undefined,
    renderType: setting.renderType || undefined,
    align: fieldAlign(field.field),
    ellipsis: setting.ellipsis === false ? false : { tooltip: true },
    render: row => renderTableCell(field, row),
  }
}).concat(aiActionColumn.value ? [aiActionColumn.value] : []))
const hasExplicitSearchFieldRefs = computed(() => Object.prototype.hasOwnProperty.call(
  props.block.props || {},
  'searchFieldRefs',
))
const aiSearchSchema = computed(() => resolveCrudSearchFieldCatalog(props.fields, props.block)
  .map(field => toAiFormField(field, 'search')))
const crudSearchTypeRequestParams = computed(() => buildCrudSearchTypeRequestParams(aiSearchSchema.value))
const designerCrudPublicParams = computed(() => ({
  ...(props.block.props?.publicParams || {}),
  ...(hasExplicitSearchFieldRefs.value ? crudSearchTypeRequestParams.value : {}),
}))
const runtimeExtensionHandlers = computed(() => (typeof props.runtimeExtensionHooks === 'function'
  ? props.runtimeExtensionHooks(props.block) || {}
  : props.runtimeExtensionHooks || {}))
const aiFormSchema = computed(() => {
  const formFields = configuredFieldRefs.value.length
    ? visibleResolvedFields.value
    : visibleResolvedFields.value.filter(isDefaultAiFormField)
  // widget 虚拟组件不在 block.fieldRefs 中，需要从 props.fields 额外注入
  const existingKeys = new Set(formFields.map(f => f.field || f.fieldCode || f.id))
  const widgetFields = (props.fields || []).filter(
    f => f.nodeType === 'widget' && !existingKeys.has(f.field || f.fieldCode || f.id),
  )
  return [...formFields, ...widgetFields].map(field => toAiFormField(field, 'form'))
})
const aiFormCreateApi = computed(() => {
  const configuredApi = props.block.props?.createApi || props.runtimeCrudProps?.apiConfig?.create || ''
  if (configuredApi)
    return configuredApi
  const baseApi = props.block.props?.api || props.runtimeCrudProps?.api || ''
  if (!baseApi)
    return ''
  return String(baseApi).includes('@') ? baseApi : `post@${baseApi}`
})
const blockApiConfig = computed(() => ({
  list: props.block.props?.listApi || '',
  detail: props.block.props?.detailApi || '',
  create: props.block.props?.createApi || '',
  update: props.block.props?.updateApi || '',
  delete: props.block.props?.deleteApi || '',
  import: props.block.props?.importApi || '',
  export: props.block.props?.exportApi || '',
}))
const crudPagePresentation = computed(() => resolveCrudPagePresentation(props.block.props, props.runtimeCrudProps || {}))
const effectiveRuntimeCrudProps = computed(() => {
  const runtimeProps = props.runtimeCrudProps
  if (!runtimeProps) {
    return null
  }
  return buildRuntimeCrudBlockProps({
    runtimeProps,
    blockProps: props.block.props || {},
    runtimeInteractive: props.runtimeInteractive,
    extensionHooks: runtimeExtensionHandlers.value,
    configuredFieldRefs: configuredFieldRefs.value,
    blockApiConfig: blockApiConfig.value,
    // 保留原有按需计算，已有编译配置时不再重复生成字段 schema。
    aiTableColumns: runtimeProps.columns?.length ? [] : aiTableColumns.value,
    aiSearchSchema: aiSearchSchema.value,
    aiFormSchema: runtimeProps.editSchema?.length ? [] : aiFormSchema.value,
    hasExplicitSearchFieldRefs: hasExplicitSearchFieldRefs.value,
    designerCrudPublicParams: designerCrudPublicParams.value,
    preventStaticCrudSubmit,
    extensionRuntimeApi,
    // 自由布局抑制列表伴生壳后，属性面板改的 formOpenMode/modalWidth 必须盖过对象 runtime
    preferBlockFormChrome: props.suppressRuntimeListGrid === true,
  })
})

/** 与对象多画布 pageKey 对齐（应用路由保留 pageId，用 query.pageKey 切 list/dialog/自定义页） */
const runtimeObjectPageKey = computed(() => {
  const fromQuery = String(route.query?.pageKey || '').trim()
  if (fromQuery)
    return fromQuery
  return 'list'
})

/** 列表设计器有伴生块时，门户/画布内 AiCrudPage 改走完整自由布局只读渲染 */
const shouldRenderRuntimeListGridShell = computed(() => {
  if (props.suppressRuntimeListGrid)
    return false
  const crud = effectiveRuntimeCrudProps.value
  if (!crud || crud.formOnly === true)
    return false
  return shouldRenderRuntimeListGrid({
    pageSchema: crud.pageSchema,
    modelSchema: crud.modelSchema,
    layoutType: crud.layoutType,
    formOnly: crud.formOnly === true,
    pageKey: runtimeObjectPageKey.value,
  })
})

const runtimeListGridModel = computed(() => {
  const crud = effectiveRuntimeCrudProps.value
  if (!crud || !shouldRenderRuntimeListGridShell.value)
    return null
  return resolveRuntimeListGridModel(crud.pageSchema, {
    modelSchema: crud.modelSchema,
    layoutType: crud.layoutType,
    pageKey: runtimeObjectPageKey.value,
  })
})

/**
 * 任意非 list 的 pageKey：禁止回退裸 AiCrudPage。
 * 已登记空页走 RuntimeListGridFlow；未登记/解析失败时由空壳占位，避免误显示列表 CRUD。
 */
const shouldSuppressBareCrudFallback = computed(() => {
  const key = runtimeObjectPageKey.value
  return Boolean(key && key !== 'list')
})

/** 对象页通常只有 AiCrudPage；tree-crud 时用模板壳出左树。设计器已有 tree-panel 时抑制，避免双树。 */
const shouldRenderTreeCrudShell = computed(() => {
  const crud = effectiveRuntimeCrudProps.value
  if (!crud || crud.formOnly === true || crud.suppressTreeCrudShell === true)
    return false
  // 已在 RuntimeListGridFlow 内：父级可能已渲 tree-panel，禁止再套 TreeCrudTemplate
  if (props.suppressRuntimeListGrid)
    return false
  // 富列表自由布局已含 tree-panel 时，不要再套 TreeCrudTemplate
  if (shouldRenderRuntimeListGridShell.value)
    return false
  if (String(crud.layoutType || '') !== 'tree-crud')
    return false
  return Boolean(crud.apiConfig?.tree || crud.options?.treeConfig)
})

function extensionRuntimeApi() {
  return {
    triggerAction: (actionCode, payload = {}) => runtimeCrudRef.value?.triggerAction?.(actionCode, payload),
    refresh: () => runtimeCrudRef.value?.refresh?.(),
  }
}

const designerCrudHookHandlers = computed(() => {
  const rules = normalizeCrudHookRules(props.block.props?.crudHookRules || {}, props.block.props?.beforeSubmitRules || [])
  return CRUD_HOOK_RULE_TARGETS.reduce((handlers, target) => {
    const list = (rules[target.value] || []).filter(rule => rule.field)
    if (list.length)
      handlers[target.value] = data => applyCrudHookRules(data, list)
    return handlers
  }, {})
})
const livePreviewRecord = computed(() => {
  const rowKey = props.block.props?.rowKey || props.runtimeCrudProps?.rowKey || 'id'
  const id = props.block.props?.previewRecordId
  return id === undefined || id === null || id === ''
    ? props.runtimeRecord || {}
    : { ...(props.runtimeRecord || {}), [rowKey]: id }
})
const blockTableRowHeight = computed(() => `${Math.max(34, 32 + normalizeTableRowGap(props.block.props?.rowGap, 8))}px`)
const detailInfoGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Math.min(4, Number(props.block.props?.columnCount || 2)))}, minmax(0, 1fr))`,
}))
const detailInfoLabelStyle = computed(() => {
  const width = Number(props.block.props?.labelWidth)
  const style = {}
  if (Number.isFinite(width) && width > 0)
    style.width = `${width}px`
  if (props.block.props?.labelColor)
    style.color = props.block.props.labelColor
  return Object.keys(style).length ? style : undefined
})
const detailInfoContentStyle = computed(() => {
  const style = {}
  if (props.block.props?.contentColor)
    style.color = props.block.props.contentColor
  if (props.block.props?.contentWrap === false) {
    style.overflow = 'hidden'
    style.textOverflow = 'ellipsis'
    style.whiteSpace = 'nowrap'
  }
  return Object.keys(style).length ? style : undefined
})

function resolveDetailFieldLabel(field = {}) {
  const setting = fieldSetting(field.field) || {}
  return setting.title || setting.label || field.label || field.field || ''
}

function resolveDetailFieldSpan(field = {}) {
  const setting = fieldSetting(field.field) || {}
  const span = Number(setting.span ?? setting.colSpan ?? 1)
  const columns = Math.max(1, Math.min(4, Number(props.block.props?.columnCount || 2)))
  if (!Number.isFinite(span) || span < 1)
    return 1
  return Math.min(columns, Math.round(span))
}
const resolvedApiConfig = computed(() => ({
  list: props.block.props?.listApi || '',
  detail: props.block.props?.detailApi || '',
  create: props.block.props?.createApi || '',
  update: props.block.props?.updateApi || '',
  delete: props.block.props?.deleteApi || '',
  import: props.block.props?.importApi || '',
  export: props.block.props?.exportApi || '',
}))
const hasConfiguredCrudRequest = computed(() => {
  const blockProps = props.block.props || {}
  const runtimeApiConfig = props.runtimeCrudProps?.apiConfig || {}
  return Boolean(blockProps.api
    || props.runtimeCrudProps?.api
    || Object.values(resolvedApiConfig.value).some(Boolean)
    || Object.values(runtimeApiConfig).some(Boolean))
})
const shouldRequestCrudPreviewApi = computed(() => props.block.props?.previewLiveData === true && hasConfiguredCrudRequest.value)
const isStaticCrudPreview = computed(() => shouldUseStaticCrudPreview({
  blockType: props.block.blockType,
  runtimeInteractive: props.runtimeInteractive,
  previewLiveData: props.block.props?.previewLiveData === true,
  hasConfiguredRequest: hasConfiguredCrudRequest.value,
}))
const staticCrudPreviewMessage = computed(() => hasConfiguredCrudRequest.value
  ? '静态结构预览 · 已连接数据存储，可在右侧开启真实数据预览'
  : '静态结构预览 · 连接数据存储后即可提交、查询数据')

function preventStaticCrudSubmit() {
  window.$message?.info(hasConfiguredCrudRequest.value
    ? '当前是静态结构预览，可在右侧开启真实数据预览后提交数据'
    : '当前是静态结构预览，连接数据存储后即可提交新增数据')
  return false
}
const resolvedDesignerCrudHookHandlers = computed(() => ({
  ...designerCrudHookHandlers.value,
  ...(isStaticCrudPreview.value ? { beforeSubmit: preventStaticCrudSubmit } : {}),
}))
const previewPagination = {
  page: 1,
  pageSize: 10,
  itemCount: 3,
  showSizePicker: true,
  showQuickJumper: true,
}
const toolbarCustomActions = computed(() => (props.block.props?.customActions || [])
  .filter(action => (action.position || 'toolbar') === 'toolbar' && action.visible !== false))
const transferOptions = computed(() => normalizeOptionItems(props.block.props?.options))
const transferValue = computed(() => Array.isArray(props.block.props?.value) ? props.block.props.value : [])
const transferSelectedOptions = computed(() => transferOptions.value.filter(item => transferValue.value.includes(item.value)))
const resolvedTextAlign = computed(() => props.block.props?.textAlign || props.block.props?.align || 'left')
const resolvedTextFontSize = computed(() => Number(props.block.props?.fontSize) || 0)
const resolvedTextFontWeight = computed(() => Number(props.block.props?.fontWeight || props.block.props?.weight || 0) || undefined)
const textContentStyle = computed(() => ({
  color: props.block.props?.color || undefined,
  textAlign: resolvedTextAlign.value,
  fontSize: resolvedTextFontSize.value ? `${resolvedTextFontSize.value}px` : undefined,
  fontWeight: resolvedTextFontWeight.value,
  fontStyle: props.block.props?.fontStyle || 'normal',
  textDecoration: props.block.props?.textDecoration || 'none',
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
}))
const pageTitleStyle = computed(() => ({
  textAlign: resolvedTextAlign.value,
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
}))
const pageTitleRichContent = computed(() => resolvePageTitleRichContent(props.block.props, props.block.label))
const textTitleStyle = computed(() => ({
  color: props.block.props?.color || '#0f172a',
  fontSize: `${resolvedTextFontSize.value || resolveTitleFontSize(props.block.props?.level || 2)}px`,
  fontWeight: resolvedTextFontWeight.value || 800,
  textAlign: resolvedTextAlign.value,
  fontStyle: props.block.props?.fontStyle || 'normal',
  textDecoration: props.block.props?.textDecoration || 'none',
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
}))
const paragraphStyle = computed(() => ({
  color: props.block.props?.color || '#475569',
  textAlign: resolvedTextAlign.value,
  fontSize: resolvedTextFontSize.value ? `${resolvedTextFontSize.value}px` : undefined,
  fontWeight: resolvedTextFontWeight.value,
  fontStyle: props.block.props?.fontStyle || 'normal',
  textDecoration: props.block.props?.textDecoration || 'none',
  paddingInlineStart: Number(props.block.props?.indent || 0) > 0 ? `${Number(props.block.props.indent) * 16}px` : undefined,
  lineHeight: Number(props.block.props?.lineHeight || 1.7),
  WebkitLineClamp: Number(props.block.props?.clamp || 0) || undefined,
}))
const paragraphListTag = computed(() => ({ ordered: 'ol', unordered: 'ul' })[props.block.props?.listType] || '')
const paragraphLines = computed(() => String(boundParagraphContent.value || '段落内容').split(/\n+/).filter(Boolean))

function updatePageTitleRichContent(value) {
  emit('inlineTextUpdate', {
    blockId: props.block.id,
    patch: { content: value },
  })
}


const watermarkContent = computed(() => {
  if (Array.isArray(props.block.props?.content))
    return props.block.props.content
  const text = String(props.block.props?.content || '内部资料')
  return text.includes('\n') ? text.split('\n') : text
})
const watermarkProps = computed(() => ({
  content: watermarkContent.value,
  cross: props.block.props?.cross === true,
  debug: props.block.props?.debug === true,
  fontSize: Number(props.block.props?.fontSize || 14),
  fontFamily: props.block.props?.fontFamily || undefined,
  fontStyle: props.block.props?.fontStyle || 'normal',
  fontVariant: props.block.props?.fontVariant || '',
  fontWeight: Number(props.block.props?.fontWeight || 400),
  fontColor: props.block.props?.fontColor || 'rgba(128, 128, 128, .3)',
  fullscreen: props.block.props?.fullscreen === true,
  globalRotate: Number(props.block.props?.globalRotate || 0),
  lineHeight: Number(props.block.props?.lineHeight || 14),
  height: Number(props.block.props?.height || 32),
  image: props.block.props?.image || undefined,
  imageHeight: toOptionalNumber(props.block.props?.imageHeight),
  imageOpacity: Number(props.block.props?.imageOpacity ?? 1),
  imageWidth: toOptionalNumber(props.block.props?.imageWidth),
  rotate: Number(props.block.props?.rotate || 0),
  selectable: props.block.props?.selectable !== false,
  textAlign: props.block.props?.textAlign || 'left',
  width: Number(props.block.props?.width || 32),
  xGap: Number(props.block.props?.xGap || 0),
  xOffset: Number(props.block.props?.xOffset || 0),
  yGap: Number(props.block.props?.yGap || 0),
  yOffset: Number(props.block.props?.yOffset || 0),
  zIndex: Number(props.block.props?.zIndex || 10),
}))
const avatarInitial = computed(() => String(boundAvatarName.value || '用户').trim().slice(0, 1) || 'U')
const safeIframeSrc = computed(() => sanitizeIframeSrc(props.block.props?.src || ''))
const qrcodeQrOptions = computed(() => ({
  typeNumber: 0,
  mode: 'Byte',
  errorCorrectionLevel: props.block.props?.errorCorrectionLevel || 'Q',
}))
const qrcodeDotsOptions = computed(() => ({
  type: props.block.props?.dotsType || 'square',
  color: props.block.props?.foreground || '#0f172a',
}))
const qrcodeBackgroundOptions = computed(() => ({
  color: props.block.props?.background || '#ffffff',
}))
const qrcodeCornersSquareOptions = computed(() => ({
  type: props.block.props?.cornersSquareType || 'square',
  color: props.block.props?.cornerColor || props.block.props?.foreground || '#0f172a',
}))
const qrcodeCornersDotOptions = computed(() => ({
  type: props.block.props?.cornersDotType || 'square',
  color: props.block.props?.cornerColor || props.block.props?.foreground || '#0f172a',
}))
const barcodeSvgRef = ref(null)
const barcodeValid = ref(true)
let barcodeModulePromise
async function renderBarcode() {
  if (props.block.blockType !== 'barcode')
    return
  await nextTick()
  const svg = barcodeSvgRef.value
  if (!svg)
    return
  try {
    const JsBarcode = await loadBarcodeModule()
    JsBarcode(svg, String(props.block.props?.value || 'FORGE-2026-0001'), {
      format: props.block.props?.format || 'CODE128',
      width: Number(props.block.props?.barWidth || 2),
      height: Number(props.block.props?.barHeight || 72),
      displayValue: props.block.props?.showText !== false,
      lineColor: props.block.props?.lineColor || '#0f172a',
      background: props.block.props?.background || '#ffffff',
      fontSize: Number(props.block.props?.fontSize || 14),
      margin: Number(props.block.props?.margin ?? 8),
      valid: (value) => {
        barcodeValid.value = value !== false
      },
    })
    barcodeValid.value = true
  }
  catch {
    barcodeValid.value = false
    svg.replaceChildren()
  }
}
const boxLayoutStyle = computed(() => ({
  display: 'flex',
  flexDirection: props.block.props?.direction || 'row',
  flexWrap: props.block.props?.wrap === false ? 'nowrap' : 'wrap',
  alignItems: props.block.props?.alignItems || 'stretch',
  justifyContent: props.block.props?.justifyContent || 'flex-start',
  gap: `${Number(props.block.props?.gap ?? 12)}px`,
}))

const layoutCardStyle = computed(() => {
  const p = props.block.props || {}
  const style = p.style || {}
  const contentPadding = Number.isFinite(Number(p.contentPadding)) ? Number(p.contentPadding) : undefined
  const titleAccent = p.showTitleAccent !== false
  // 样式 Tab 的圆角写在 props.style；卡片表面有独立底色，必须落到 .layout-card 上才看得见
  const radiusRaw = style.borderRadius != null && style.borderRadius !== ''
    ? style.borderRadius
    : (p.borderRadius != null && p.borderRadius !== '' ? p.borderRadius : 8)
  return {
    ...resolveSurfaceBackgroundStyle(p),
    borderRadius: toCssSize(radiusRaw, '8px'),
    ...(style.boxShadow && style.boxShadow !== 'none' ? { boxShadow: style.boxShadow } : {}),
    ...(contentPadding != null ? { padding: `${Math.max(0, contentPadding)}px` } : {}),
    ...(p.titleAccentColor && titleAccent
      ? { '--card-title-accent': p.titleAccentColor }
      : {}),
    ...(p.titleFontSize ? { '--card-title-size': `${Number(p.titleFontSize) || 14}px` } : {}),
    ...(p.titleFontWeight ? { '--card-title-weight': String(p.titleFontWeight) } : {}),
    ...(p.titleColor ? { '--card-title-color': p.titleColor } : {}),
  }
})

const panelBoxStyle = computed(() => {
  const p = props.block.props || {}
  const style = p.style || {}
  const minH = Number(p.minHeight)
  const minW = Number(p.minWidth)
  const pad = p.contentPadding ?? p.padding
  const radiusRaw = style.borderRadius != null && style.borderRadius !== ''
    ? style.borderRadius
    : (p.borderRadius != null && p.borderRadius !== '' ? p.borderRadius : 8)
  return {
    ...resolveSurfaceBackgroundStyle(p),
    minHeight: Number.isFinite(minH) && minH > 0 ? `${minH}px` : '120px',
    minWidth: Number.isFinite(minW) && minW > 0 ? `${minW}px` : undefined,
    padding: pad != null && pad !== '' ? toCssSize(pad, '12px') : '12px',
    borderRadius: toCssSize(radiusRaw, '8px'),
    borderWidth: p.bordered === false ? '0' : toCssSize(p.borderWidth ?? style.borderWidth ?? 1, '1px'),
    borderStyle: p.bordered === false ? 'none' : (p.borderStyle || style.borderStyle || 'solid'),
    borderColor: p.bordered === false ? 'transparent' : (p.borderColor || style.borderColor || 'var(--border-light)'),
    ...(style.boxShadow && style.boxShadow !== 'none' ? { boxShadow: style.boxShadow } : {}),
  }
})
const spacePreviewStyle = computed(() => ({
  '--space-size': `${Number(props.block.props?.size || 24)}px`,
}))
const descriptionGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Math.min(4, Number(props.block.props?.columnCount || 2)))}, minmax(0, 1fr))`,
}))

const sampleRows = computed(() => Array.from({ length: 3 }).map((_, idx) => {
  const row = { id: idx + 1, __actions: '编辑' }
  visibleResolvedFields.value.slice(0, 8).forEach((field) => {
    row[field.field] = sampleValue(field, idx)
  })
  return row
}))


function normalizeOptionItems(options = []) {
  return (Array.isArray(options) ? options : []).map((item, index) => {
    if (typeof item === 'string')
      return { label: item, value: item }
    return {
      label: item?.label || item?.value || `选项${index + 1}`,
      value: item?.value || item?.label || `option${index + 1}`,
      disabled: item?.disabled === true,
    }
  })
}



function hasAction(key) {
  return Array.isArray(props.block.props?.actions) && props.block.props.actions.includes(key)
}

function fieldAlign(fieldName) {
  const align = props.block.props?.fieldSettings?.[fieldName]?.align
  if (['left', 'center', 'right'].includes(align))
    return align
  const globalAlign = props.block.props?.globalAlign
  return ['left', 'center', 'right'].includes(globalAlign) ? globalAlign : 'left'
}


function fieldSetting(fieldName) {
  return props.block.props?.fieldSettings?.[fieldName] || {}
}

function renderTableCell(field, row = {}) {
  const setting = fieldSetting(field.field)
  return h(FieldValueRenderer, {
    value: row[field.field],
    row,
    field,
    setting,
    context: runtimeRuleContext.value,
  })
}




function resolveNestedBlockFields(block = {}) {
  return props.blockFieldsResolver ? props.blockFieldsResolver(block) : props.fields
}

function resolveNestedBlockRuntimeCrudProps(block = {}) {
  return props.runtimeCrudPropsResolver ? props.runtimeCrudPropsResolver(block) : props.runtimeCrudProps
}

function resolveNestedBlockRuntimeCrudLoading(block = {}) {
  return props.runtimeCrudLoadingResolver ? Boolean(props.runtimeCrudLoadingResolver(block)) : props.runtimeCrudLoading
}

function resolveNestedBlockDataSourceConfigured(block = {}) {
  return props.dataSourceConfiguredResolver
    ? Boolean(props.dataSourceConfiguredResolver(block))
    : props.dataSourceConfigured
}




function detailValue(field, idx) {
  const record = detailInfoRecord.value || {}
  const candidates = [
    field?.field,
    field?.sourceField,
    field?.columnName,
    field?.field ? `${field.field}Name` : '',
  ].filter(Boolean)
  for (const key of candidates) {
    const value = record[key]
    if (value !== undefined && value !== null && value !== '')
      return value
  }
  if (Object.keys(record).length)
    return '-'
  return sampleValue(field, idx)
}

function normalizeBlockBoundRows(fallbackRows = []) {
  const rows = extractBlockBoundRows(blockBoundData.value)
  const sourceRows = rows.length ? rows : (Array.isArray(fallbackRows) ? fallbackRows : [])
  return sourceRows.map((row, index) => normalizeBlockRow(row, index))
}



function boundContentValue(fallback = '', fieldKey = 'contentField', defaultField = 'content') {
  const binding = props.block.props?.dataBinding || {}
  const data = blockBoundData.value
  const primarySlot = resolveWidgetBindingProfile(props.block.blockType).primarySlot || 'valueField'
  if (
    fieldKey === primarySlot
    && isComposeDisplayEnabled(binding)
    && data
    && typeof data === 'object'
    && !Array.isArray(data)
  ) {
    const composed = resolveComposeDisplayValue(data, binding, getNestedRecordValue)
    if (composed !== '')
      return composed
  }
  if (data === undefined || data === null)
    return fallback
  if (Array.isArray(data))
    return fallback
  if (typeof data !== 'object')
    return fieldKey === primarySlot || fieldKey === 'valueField' || fieldKey === 'contentField' ? data : fallback
  return readBoundField(data, fieldKey, defaultField, fallback)
}


function readBoundField(source = {}, fieldKey = 'valueField', defaultField = 'value', fallback = '') {
  const binding = props.block.props?.dataBinding || {}
  const explicit = String(binding[fieldKey] || '').trim()
  // 用户点选了真实字段时，只读该路径，避免误落到默认 value/title
  if (explicit && isExplicitBindingPath(explicit, props.fields)) {
    const value = getNestedRecordValue(source, explicit)
    if (value !== undefined && value !== null && value !== '')
      return value
    return fallback
  }
  const candidates = Array.from(new Set([explicit || defaultField, defaultField].filter(Boolean)))
  for (const key of candidates) {
    const value = getNestedRecordValue(source, key)
    if (value !== undefined && value !== null && value !== '')
      return value
  }
  return fallback
}

function resolveBindingTemplateContext() {
  // 详情页等组件常从地址栏取 {{ id }}；与详情专用加载共用同一上下文
  return resolveDetailTemplateContext()
}

async function loadBlockBindingData() {
  const binding = props.block.props?.dataBinding || {}
  if (!isLocalDataBindableBlock.value || binding.enabled !== true || binding.sourceType !== 'remote') {
    remoteBlockBindingData.value = null
    blockBindingError.value = ''
    blockBindingLoading.value = false
    return
  }
  if (!binding.api) {
    remoteBlockBindingData.value = null
    blockBindingError.value = props.block.blockType === 'detail-info'
      ? '未配置详情接口：请在属性里选「远程接口」填写地址，或点「填入对象接口」'
      : '未配置数据接口'
    blockBindingLoading.value = false
    return
  }
  blockBindingLoading.value = true
  blockBindingError.value = ''
  try {
    const ctx = resolveBindingTemplateContext()
    const apiConfig = String(binding.api).includes('@') ? binding.api : `${binding.method || 'get'}@${binding.api}`
    const { method, url } = parseApiConfigValue(apiConfig)
    const params = normalizeBlockBindingParams(binding.paramsText || '{}', ctx)
    const resolvedUrl = interpolateApiUrl(url, ctx)
    if (!resolvedUrl || /\{\{|\}\}/.test(resolvedUrl) || /\/:[A-Za-z_]/.test(resolvedUrl)) {
      remoteBlockBindingData.value = null
      blockBindingError.value = '接口地址未解析完整，请确认地址栏是否带 id 等参数（支持 {{ id }} 与 :id）'
      return
    }
    const response = await request({
      method,
      url: resolvedUrl,
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
      needTip: false,
    })
    remoteBlockBindingData.value = response?.data ?? response
  }
  catch (error) {
    remoteBlockBindingData.value = null
    blockBindingError.value = `数据加载失败：${error?.message || '请检查接口配置'}`
  }
  finally {
    blockBindingLoading.value = false
  }
}

function normalizeBlockBindingParams(value = '{}', context = null) {
  const ctx = context || resolveBindingTemplateContext()
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value || '{}') : value
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      return {}
    return Object.fromEntries(Object.entries(parsed).map(([key, item]) => [
      key,
      typeof item === 'string' ? interpolateText(item, ctx) : item,
    ]))
  }
  catch {
    return {}
  }
}

function resolveDetailTemplateContext() {
  return buildRuntimeTemplateContext({
    record: props.runtimeRecord || {},
    query: route.query || {},
    params: route.params || {},
  })
}

async function loadDetailInfoRecord() {
  if (props.block.blockType !== 'detail-info') {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = ''
    return
  }
  const binding = props.block.props?.dataBinding || {}
  // 已走统一 dataBinding 远程时，由 loadBlockBindingData 负责
  if (isDetailInfoBindingRemote(binding) || isDetailInfoBindingContext(binding)) {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = ''
    return
  }
  const sourceType = props.block.props?.dataSourceType || 'remote'
  if (sourceType !== 'remote') {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = ''
    return
  }
  const objectRef = props.block.props?.objectRef || props.block.props?.businessObjectRef || {}
  const objectKey = String(objectRef.configKey || objectRef.objectCode || '').trim()
  const api = props.block.props?.detailApi
    || (objectKey ? `get@/ai/crud/${objectKey}/{{ id }}` : '')
  if (!api) {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = '未配置详情接口：请到「组件 → 属性」填写详情接口，或到「数据」Tab 绑定业务对象后点「填入对象接口」'
    return
  }
  detailInfoLoading.value = true
  detailInfoError.value = ''
  try {
    const ctx = resolveDetailTemplateContext()
    const apiConfig = String(api).includes('@') ? api : `${props.block.props?.detailMethod || 'get'}@${api}`
    const { method, url } = parseApiConfigValue(apiConfig)
    const params = normalizeDetailInfoParams(props.block.props?.paramsText || '{}', ctx)
    const resolvedUrl = interpolateApiUrl(url, ctx)
    // 仍含 {{ }} 或未替换的 :id 说明地址栏缺参
    if (!resolvedUrl || /\{\{|\}\}/.test(resolvedUrl) || /\/:[A-Za-z_]/.test(resolvedUrl)) {
      remoteDetailInfoRecord.value = {}
      detailInfoError.value = '详情接口地址未解析完整，请确认地址栏是否带 id 等参数（支持 {{ id }} 与 :id）'
      return
    }
    const response = await request({
      method,
      url: resolvedUrl,
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
      needTip: false,
    })
    const data = response?.data ?? response
    remoteDetailInfoRecord.value = props.block.props?.dataPath ? getNestedRecordValue(data, props.block.props.dataPath) || {} : data || {}
  }
  catch (error) {
    remoteDetailInfoRecord.value = {}
    detailInfoError.value = `详情加载失败：${error?.message || '请检查接口配置'}`
  }
  finally {
    detailInfoLoading.value = false
  }
}

function normalizeDetailInfoParams(value = '{}', context = null) {
  const ctx = context || resolveDetailTemplateContext()
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value || '{}') : value
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      return {}
    return Object.fromEntries(Object.entries(parsed).map(([key, item]) => [
      key,
      typeof item === 'string' ? interpolateText(item, ctx) : item,
    ]))
  }
  catch {
    return {}
  }
}

// 与 PageWidgetRenderer.interpolateTemplate 保持同一套占位符语法：{{ 字段 }}、${字段}、$form.字段


function resolveRuntimeTreeApi() {
  const blockProps = props.block?.props || {}
  if (blockProps.treeApi)
    return blockProps.treeApi
  const sourceConfigKey = String(blockProps.sourceConfigKey || blockProps.sourceModelCode || '').trim()
  if (sourceConfigKey)
    return `get@/ai/crud/${sourceConfigKey}/tree`
  return effectiveRuntimeCrudProps.value?.apiConfig?.tree || ''
}

async function loadRuntimeTree() {
  if (props.block.blockType !== 'tree-panel')
    return
  const treeApi = resolveRuntimeTreeApi()
  if (!treeApi) {
    runtimeTreeNodes.value = []
    runtimeTreeNodeMap.value = new Map()
    runtimeTreeError.value = ''
    return
  }
  runtimeTreeLoading.value = true
  runtimeTreeError.value = ''
  try {
    const { method, url } = parseApiConfigValue(treeApi)
    const loadMode = runtimeTreeLoadMode.value
    // 仅 tree-panel 显式排序；不用右表 defaultSort，避免节点序被 id desc 带偏
    const blockProps = props.block?.props || {}
    const panelSortField = String(blockProps.defaultSortField || blockProps.orderByColumn || '').trim()
    const panelSortOrder = String(blockProps.defaultSortOrder || blockProps.isAsc || 'asc').toLowerCase()
    const sortParams = panelSortField
      ? { orderByColumn: panelSortField, isAsc: panelSortOrder === 'asc' ? 'asc' : 'desc' }
      : {}
    const response = await request({
      method,
      url,
      params: {
        loadMode,
        ...sortParams,
      },
      needTip: false,
    })
    const rows = extractRuntimeTreeRows(response)
    const nodeMap = new Map()
    runtimeTreeNodes.value = normalizeRuntimeTreeNodes(rows, nodeMap)
    runtimeTreeNodeMap.value = nodeMap
    runtimeExpandedTreeKeys.value = loadMode === 'lazy'
      ? []
      : collectTreeKeys(runtimeTreeNodes.value, runtimeTreeChildrenField.value)
  }
  catch (error) {
    runtimeTreeNodes.value = []
    runtimeTreeNodeMap.value = new Map()
    runtimeTreeError.value = error?.message || '加载筛选树失败'
    console.warn('[GridBlockRenderer] 加载筛选树失败', error?.message || error)
  }
  finally {
    runtimeTreeLoading.value = false
  }
}

async function loadRuntimeTreeChildren(node) {
  const treeApi = resolveRuntimeTreeApi()
  if (!treeApi || !node)
    return
  const { method, url } = parseApiConfigValue(treeApi)
  const config = runtimeTreeConfig.value
  const parentValue = node?.[config.keyField] ?? node?.key ?? node?.targetValue
  if (parentValue === undefined || parentValue === null || parentValue === '') {
    node.isLeaf = true
    return
  }
  try {
    const response = await request({
      method,
      url,
      params: {
        loadMode: 'lazy',
        parentValue: String(parentValue),
      },
      needTip: false,
    })
    const childMap = new Map()
    const children = normalizeRuntimeTreeNodes(extractRuntimeTreeRows(response), childMap)
    node[runtimeTreeChildrenField.value] = children
    node.isLeaf = children.length === 0
    childMap.forEach((value, key) => runtimeTreeNodeMap.value.set(key, value))
  }
  catch (error) {
    node.isLeaf = true
    console.warn('[GridBlockRenderer] 加载筛选树子节点失败', error?.message || error)
  }
}



function normalizeRuntimeTreeNodes(rows = [], nodeMap = new Map()) {
  const config = runtimeTreeConfig.value
  const keyField = config.keyField
  const labelField = config.labelField
  const childrenField = runtimeTreeChildrenField.value
  return (Array.isArray(rows) ? rows : []).map((row) => {
    const rawKey = row?.[keyField] ?? row?.key ?? row?.id
    const key = String(rawKey ?? '')
    const label = row?.[labelField] ?? row?.label ?? row?.name ?? key
    const children = normalizeRuntimeTreeNodes(row?.[childrenField] || [], nodeMap)
    const node = {
      ...(row || {}),
      key,
      label: String(label || '-'),
      [childrenField]: children,
    }
    if (children.length)
      node.isLeaf = false
    else if (row?.isLeaf !== undefined)
      node.isLeaf = row.isLeaf === true || row.isLeaf === 1 || row.isLeaf === '1'
    if (key)
      nodeMap.set(key, node)
    return node
  })
}



function expandTree() {
  if (hasRuntimeTreeApi.value) {
    runtimeExpandedTreeKeys.value = collectTreeKeys(runtimeTreeNodes.value, runtimeTreeChildrenField.value)
    return
  }
  previewTreeExpanded.value = true
}

function collapseTree() {
  if (hasRuntimeTreeApi.value) {
    runtimeExpandedTreeKeys.value = []
    return
  }
  previewTreeExpanded.value = false
}

function toggleTreePanel() {
  treePanelCollapsed.value = !treePanelCollapsed.value
  emit('treePanelCollapseChange', {
    id: props.block.id,
    collapsed: treePanelCollapsed.value,
  })
}

function clearRuntimeTreeSelection() {
  const config = runtimeTreeConfig.value
  emit('runtimeTreeSelect', {
    key: '__all__',
    clear: true,
    blockId: props.block.id,
    filterField: config.filterField,
  })
}

function handleRuntimeTreeSelected(keys = []) {
  const key = keys?.[0]
  if (!key) {
    clearRuntimeTreeSelection()
    return
  }
  const node = runtimeTreeNodeMap.value.get(String(key)) || {}
  const config = runtimeTreeConfig.value
  const targetField = config.targetField
  const filterField = config.filterField
  const includeChildren = props.block?.props?.includeChildren !== false
  const value = node?.[targetField] ?? node?.targetValue ?? node?.value ?? key
  const expandedValues = collectTreeFilterValues(node, {
    targetField,
    childrenField: runtimeTreeChildrenField.value,
    includeChildren,
  })
  emit('runtimeTreeSelect', {
    key,
    blockId: props.block.id,
    node,
    filterField,
    value: value === null || value === undefined ? value : String(value),
    expandedValues,
    // 默认点上级查本级+全部下级；树面板 props.includeChildren === false 时仅本节点
    includeChildren,
  })
}


function emitCrudPreviewState(patch = {}) {
  if (props.block.blockType !== 'AiCrudPage')
    return
  emit('crudPreviewStateChange', {
    blockId: props.block.id,
    patch,
  })
}

function handleCrudPreviewSuccess(payload = {}) {
  if (props.block.props?.previewLiveData !== true)
    return
  emitCrudPreviewState({
    lastPreviewStatus: 'success',
    lastPreviewError: '',
    lastPreviewMessage: `接口预览成功，读取 ${Number(payload.total ?? payload.list?.length ?? 0)} 条数据`,
  })
  applyCrudPreviewMode(payload?.list || [])
}

function handleCrudPreviewError(error) {
  if (props.block.props?.previewLiveData !== true)
    return
  emitCrudPreviewState({
    lastPreviewStatus: 'error',
    lastPreviewError: error?.message || '接口请求失败',
    lastPreviewMessage: '接口预览失败，请检查接口地址、响应字段或权限',
  })
}

async function applyCrudPreviewMode(list = []) {
  await Promise.resolve()
  const mode = crudPreviewMode.value
  if (!['create', 'edit', 'detail'].includes(mode))
    return
  const crud = runtimeCrudRef.value
  if (!crud)
    return
  if (mode === 'create') {
    crud.showAdd?.(props.block.props?.formDefaultValues || {})
    emitCrudPreviewState({
      lastPreviewStatus: 'success',
      lastPreviewMessage: '新增表单预览已打开',
      lastPreviewError: '',
    })
    return
  }
  const row = Object.keys(livePreviewRecord.value || {}).length
    ? livePreviewRecord.value
    : list[0]
  if (!row) {
    emitCrudPreviewState({
      lastPreviewStatus: 'error',
      lastPreviewError: '缺少预览记录，请填写记录 ID 或确保列表接口返回数据',
      lastPreviewMessage: '无法打开编辑/详情预览',
    })
    return
  }
  if (mode === 'edit') {
    await crud.showEdit?.(row)
    emitCrudPreviewState({
      lastPreviewStatus: 'success',
      lastPreviewMessage: '编辑表单预览已打开',
      lastPreviewError: '',
    })
    return
  }
  await crud.showDetail?.(row)
  emitCrudPreviewState({
    lastPreviewStatus: 'success',
    lastPreviewMessage: '详情状态预览已打开',
    lastPreviewError: '',
  })
}

function buildRuntimeEventContext() {
  return {
    route,
    router,
    // 组件树内 useMessage 比离散 window.$message 更稳定（预览/门户可弹出）
    message: runtimeMessage,
    selfBlockId: props.block?.id || '',
    resolveRuntimeText,
    resolveRuntimeParamValue,
    appendRuntimeEventParams,
    isApplicationRuntimeRoute,
  }
}

/** 设计态拦截链接；预览/发布态优先执行配置的 click 事件，否则放行 href */
async function handleLinkClick(event) {
  if (!props.readonly) {
    event?.preventDefault?.()
    return
  }
  const eventItem = resolvePrimaryClickEvent()
  if (!eventItem || eventItem.action === 'none')
    return
  event?.preventDefault?.()
  await runBlockClickEvent(eventItem, buildRuntimeEventContext())
}

/**
 * 返回按钮：优先执行设计器配置的 click 事件（与 action-button/link 同协议）；
 * 未配置事件时再走 props.action=navigate 或 history.back 兜底。
 */
async function handleBackClick(event) {
  if (!props.readonly)
    return
  const eventItem = resolvePrimaryClickEvent()
  if (eventItem && eventItem.action !== 'none') {
    event?.preventDefault?.()
    await runBlockClickEvent(eventItem, buildRuntimeEventContext())
    return
  }
  if (props.block.props?.action === 'navigate' && props.block.props?.targetPageKey) {
    const target = props.block.props.targetPageKey
    const query = applyPageNavigationQuery({ ...route.query }, target, isApplicationRuntimeRoute)
    ;['mode', 'id', 'recordId'].forEach(key => delete query[key])
    if (props.block.props.targetFormKey)
      query.formKey = props.block.props.targetFormKey
    else
      delete query.formKey
    router.replace({
      path: route.path,
      query,
      hash: route.hash,
    })
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  const query = { ...route.query }
  ;['pageKey', 'pageId', 'formKey', 'mode', 'id', 'recordId'].forEach(key => delete query[key])
  router.replace({
    path: route.path,
    query,
    hash: route.hash,
  })
}

async function handleActionButtonClick() {
  if (!props.readonly)
    return
  const eventItem = resolvePrimaryClickEvent()
  if (!eventItem || eventItem.action === 'none')
    return
  await runBlockClickEvent(eventItem, buildRuntimeEventContext())
}

/** 按钮组运行态点击：复用区块级 click 事件（与 action-button 同协议） */
async function handleButtonGroupClick(button = {}) {
  if (!props.readonly)
    return
  if (button?.disabled === true)
    return
  const eventItem = resolvePrimaryClickEvent()
  if (!eventItem || eventItem.action === 'none')
    return
  await runBlockClickEvent(eventItem, {
    ...buildRuntimeEventContext(),
    clickedButton: button,
  })
}

/**
 * 运行态区块外壳点击：让 info-panel 等「内容型」区块配置的 click 事件真正生效。
 * 自带点击控件的区块（按钮/链接/返回）自行处理，避免重复触发。
 */
async function handleRuntimeBlockShellClick(event) {
  if (!props.readonly)
    return
  const selfHandled = ['action-button', 'button-group', 'link', 'back-button', 'quick-entry']
  if (selfHandled.includes(props.block?.blockType))
    return
  const target = event?.target
  if (target?.closest?.('button, a, input, textarea, select, .n-button, .n-input, .n-base-selection, .n-checkbox, .n-radio, .n-switch'))
    return
  const eventItem = resolvePrimaryClickEvent()
  if (!eventItem || eventItem.action === 'none')
    return
  await runBlockClickEvent(eventItem, buildRuntimeEventContext())
}

function resolvePrimaryClickEvent() {
  const raw = props.block.props?.events
  const events = Array.isArray(raw)
    ? raw
    : (raw && typeof raw === 'object' ? Object.values(raw) : [])
  return events.find(item => item && (item.trigger || 'click') === 'click' && item.action && item.action !== 'none') || null
}

function navigateRuntimeEvent(eventItem = {}) {
  return runNavigateRuntimeEvent(eventItem, buildRuntimeEventContext())
}

/** 应用运行态用 pageId 切页；对象多页 CRUD 用 pageKey。 */
function isApplicationRuntimeRoute() {
  return Boolean(route.params?.applicationCode)
    || String(route.name || '').includes('Application')
    || String(route.path || '').includes('/app-center/application/')
    || String(route.path || '').startsWith('/app/')
}



function resolveRuntimeParamValue(param = {}) {
  const sourceType = param.sourceType || 'static'
  const sourceField = String(param.sourceField || '').trim()
  if (sourceType === 'rowField' && sourceField)
    return props.runtimeRecord?.[sourceField] ?? ''
  if (sourceType === 'routeQuery' && sourceField)
    return route.query?.[sourceField] ?? ''
  if (sourceType === 'system' && sourceField)
    return resolveSystemRuntimeParam(sourceField)
  return resolveRuntimeText(param.value || '')
}

function resolveRuntimeText(template = '') {
  let text = String(template || '')
  const data = props.runtimeRecord || {}
  Object.keys(data).forEach((key) => {
    const value = data[key]
    if (value === null || value === undefined || value === '')
      return
    text = text.replaceAll(`:${key}`, value)
    text = text.replaceAll(`\${${key}}`, value)
  })
  Object.keys(route.query || {}).forEach((key) => {
    const value = route.query[key]
    if (value === null || value === undefined || value === '')
      return
    text = text.replaceAll(resolveTemplatePlaceholder(`route.${key}`), Array.isArray(value) ? value[0] : value)
  })
  return text
    .replaceAll(resolveTemplatePlaceholder('system.now'), new Date().toISOString())
    .replaceAll(resolveTemplatePlaceholder('system.today'), new Date().toISOString().slice(0, 10))
}


function resolveSystemRuntimeParam(sourceField = '') {
  if (sourceField === 'now')
    return new Date().toISOString()
  if (sourceField === 'today')
    return new Date().toISOString().slice(0, 10)
  if (sourceField === 'tenantId')
    return route.query?.tenantId || ''
  return ''
}

function hasRuntimePermission(permissionCode = '') {
  const code = String(permissionCode || '').trim()
  if (!code)
    return true
  if (userStore.isAdmin || userStore.isTenantAdmin)
    return true
  const permissions = [
    ...(Array.isArray(userStore.permissions) ? userStore.permissions : []),
    ...(Array.isArray(userStore.apiPermissions) ? userStore.apiPermissions : []),
    ...(Array.isArray(userStore.getDataPermission) ? userStore.getDataPermission : []),
  ]
  return permissions.includes('**') || permissions.includes(code)
}


async function handleAiFormSubmit(formData = {}) {
  if (!props.runtimeInteractive || aiFormSubmitting.value)
    return
  if (!aiFormCreateApi.value) {
    window.$message?.warning('当前表单尚未配置新增接口')
    return
  }

  aiFormSubmitting.value = true
  try {
    const { method, url } = parseRuntimeApiConfig(aiFormCreateApi.value)
    let data = {
      ...(props.runtimeCrudProps?.submitDefaultParams || {}),
      ...(props.block.props?.submitDefaultParams || {}),
      ...(formData || {}),
    }
    const extensionHooks = runtimeExtensionHandlers.value
    if (typeof extensionHooks.beforeSubmit === 'function') {
      const result = await extensionHooks.beforeSubmit(data, extensionRuntimeApi())
      if (result === false)
        return
      if (result && typeof result === 'object' && !Array.isArray(result))
        data = { ...result }
    }
    previewFormValue.value = { ...data }
    let response
    if (method === 'postencrypt') {
      response = await postEncrypt(url, data, { needTip: false })
    }
    else {
      response = await request({
        method,
        url,
        ...(method === 'get' ? { params: data } : { data }),
        needTip: false,
      })
    }
    window.$message?.success('提交成功')
    if (typeof extensionHooks.afterSubmit === 'function') {
      await extensionHooks.afterSubmit({
        data,
        response,
        isEdit: false,
      }, extensionRuntimeApi())
    }
    aiFormRef.value?.reset?.()
  }
  catch (error) {
    window.$message?.error(error?.message || '提交失败，请稍后重试')
  }
  finally {
    aiFormSubmitting.value = false
  }
}

let standaloneFormChangeSequence = 0
async function handleStandaloneAiFormValueUpdate(value = {}) {
  const record = value && typeof value === 'object' && !Array.isArray(value) ? { ...value } : {}
  previewFormValue.value = record
  const formChange = runtimeExtensionHandlers.value.formChange
  if (!props.runtimeInteractive || typeof formChange !== 'function')
    return
  const sequence = ++standaloneFormChangeSequence
  try {
    const result = await formChange({
      data: record,
      record,
      modalStatus: 'create',
    }, extensionRuntimeApi())
    if (sequence !== standaloneFormChangeSequence || result === false)
      return
    const next = result?.record || result?.data || result
    if (next && typeof next === 'object' && !Array.isArray(next))
      previewFormValue.value = { ...next }
  }
  catch (error) {
    window.$message?.error(error?.message || '字段联动增强执行失败')
  }
}


function handleRuntimeEventSuccess(eventItem = {}) {
  runRuntimeEventSuccess(eventItem, buildRuntimeEventContext())
}

function boundRowField(row = {}, fieldKey = 'valueField', defaultField = 'value', fallback = '') {
  if (row === undefined || row === null)
    return fallback
  if (typeof row !== 'object')
    return fieldKey === 'valueField' || fieldKey === 'contentField' ? row : fallback
  return readBoundField(row, fieldKey, defaultField, fallback)
}

function appendRuntimeEventParams(target = {}, params = []) {
  ;(Array.isArray(params) ? params : []).forEach((param) => {
    const name = String(param?.name || '').trim()
    if (!name)
      return
    const value = resolveRuntimeParamValue(param)
    if (value !== undefined && value !== '')
      target[name] = value
  })
  return target
}

const gridBlockExposeApi = {
  getRuntimeCrudRef: () => runtimeCrudRef.value,
}

function resolveRuntimeCrudApi(instance = runtimeCrudRef.value) {
  return findNestedAiCrudApi(instance)
}

let unregisterRuntimeBlock = null
function syncRuntimeBlockRegistry() {
  unregisterRuntimeBlock?.()
  unregisterRuntimeBlock = null
  const blockId = props.block?.id
  if (!blockId)
    return
  // 流式壳 / 普通 AiCrudPage 都注册到 block.id；壳场景通过子树查找真实 AiCrudPage。
  unregisterRuntimeBlock = registerRuntimeBlock(blockId, createRuntimeBlockApi(resolveRuntimeCrudApi))
}

watch(
  [() => props.block?.id, runtimeCrudRef, shouldRenderRuntimeListGridShell],
  () => syncRuntimeBlockRegistry(),
  { immediate: true },
)

onMounted(() => {
  loadRuntimeTree()
  loadDetailInfoRecord()
  loadBlockBindingData()
  renderBarcode()
})

onBeforeUnmount(() => {
  unregisterRuntimeBlock?.()
  unregisterRuntimeBlock = null
})

watch([
  () => props.block.blockType,
  () => props.block.props?.value,
  () => props.block.props?.format,
  () => props.block.props?.barWidth,
  () => props.block.props?.barHeight,
  () => props.block.props?.showText,
  () => props.block.props?.lineColor,
  () => props.block.props?.background,
  () => props.block.props?.fontSize,
  () => props.block.props?.margin,
], () => renderBarcode())

watch([
  () => props.block.blockType,
  () => props.block.props?.sourceModelCode,
  () => props.block.props?.sourceConfigKey,
  () => props.block.props?.treeApi,
  () => props.block.props?.keyField,
  () => props.block.props?.parentField,
  () => props.block.props?.labelField,
  () => props.block.props?.displayField,
  () => props.block.props?.loadMode,
  () => props.block.props?.lazy,
  () => props.block.props?.childrenField,
  () => props.runtimeCrudProps?.apiConfig?.tree,
], () => loadRuntimeTree())

watch([
  () => props.block.blockType,
  () => props.block.props?.dataSourceType,
  () => props.block.props?.detailApi,
  () => props.block.props?.detailMethod,
  () => props.block.props?.paramsText,
  () => props.block.props?.dataPath,
  () => props.runtimeRecord,
  () => route.query,
  () => route.params,
], () => loadDetailInfoRecord())

watch([
  () => props.block.blockType,
  () => props.block.props?.dataBinding?.enabled,
  () => props.block.props?.dataBinding?.sourceType,
  () => props.block.props?.dataBinding?.api,
  () => props.block.props?.dataBinding?.method,
  () => props.block.props?.dataBinding?.paramsText,
  () => props.block.props?.dataBinding?.dataPath,
  () => props.block.props?.dataBinding?.contextPath,
  () => props.runtimeRecord,
  () => route.query,
  () => route.params,
], () => loadBlockBindingData())

watch(
  crudPreviewReloadKey,
  () => {
    if (props.block.blockType !== 'AiCrudPage' || props.block.props?.previewLiveData !== true)
      return
    emitCrudPreviewState({
      lastPreviewStatus: 'loading',
      lastPreviewMessage: '正在请求真实接口预览',
      lastPreviewError: '',
    })
    runtimeCrudRef.value?.loadList?.()
  },
)

function shouldShowGridCellDropPreview(cell = {}) {
  if (!isActiveDropCell(cell))
    return false
  // 目录拖入时即便格子已有内容也提示可放入（追加到该格）
  return true
}

function handleGridCellDragEnter(event) {
  if (!isCatalogDragEvent(event))
    return
  event.preventDefault()
  event.stopPropagation()
  event.dataTransfer.dropEffect = 'copy'
}

function handleContainerDragEnter(event) {
  if (!isCatalogDragEvent(event))
    return
  event.preventDefault()
  event.stopPropagation()
  event.dataTransfer.dropEffect = 'copy'
}


  return {
    props,
    emit,
    localDataBindableBlockTypes, route, router, userStore, fieldMap, isDataFieldBlock, configuredFieldRefs, resolvedFields,
    shouldShowDataSourceGuide, visibleResolvedFields, previewFormValue, aiFormRef, aiFormSubmitting, signaturePreviewValue, runtimeCrudRef, runtimeTreeLoading,
    runtimeTreeNodes, runtimeTreeError, runtimeTreeNodeMap, runtimeExpandedTreeKeys, detailInfoLoading, detailInfoError, detailInfoBusy, detailInfoFailMessage, remoteDetailInfoRecord, blockBindingLoading,
    blockBindingError, remoteBlockBindingData, previewTreeExpanded, treePanelCollapsed, activeTabKeyByBlockId, runtimeTreeChildrenField, runtimeTreeConfig, runtimeTreeLoadMode,
    hasRuntimeTreeApi, runtimeTreeTotal, runtimeSelectedTreeKeys, runtimeRuleContext, blockRuntimeControl, blockRuntimeVisible, detailInfoRecord, blockBoundData,
    resolveContextObjectSource, isBlockSlotVisible, isLocalDataBindableBlock, showBlockBindingState, statsMetrics, quickEntryItems, topRankingItems, chartSeriesPayload, resolveManualMetricValue, boundInfoTitle, boundInfoContent,
    boundInfoType, boundCustomTitle, boundCustomContent, boundTags, boundSteps, boundTimelineTitle, boundTimelineItems, boundEmptyTitle,
    boundEmptyDescription, boundEmptyActionText, boundTextTitle, boundTextSubtitle, boundParagraphContent, boundStatisticTitle, boundStatisticValue, boundStatisticDescription,
    boundStatisticTrend, boundLinkText, boundLinkHref, boundTextTipTitle, boundTextTipContent, boundTextTipType, boundMediaTitle, boundMediaSrc,
    boundVideoPoster, boundAvatarName, boundAvatarDescription, boundAvatarSrc, resizeAnchors, nestedBlockMenuOptions, hoveredNestedBlockId, showNestedChrome, onNestedPointerEnter, onNestedPointerLeave, containerMenu, containerMenuExcludeBlockTypes, openContainerMenu,
    closeContainerMenu, handleGridCellContextMenu, handleContainerSlotContextMenu, handleChildContextMenu, handleContainerMenuInsert, handleContainerMenuAction, crudPreviewMode, crudPreviewReloadKey,
    gridLayoutCells, resolveGridCellPreviewMinHeight, isActiveDropCell, isActiveDropContainer, shouldShowGridCellEmpty, isCatalogDragEvent, handleGridCellDragOver, handleGridCellDrop,
    handleContainerDragOver, handleContainerDrop, handleTabPaneDragOver, handleTabPaneDragEnter, resolveActiveTabKey, handleTabsValueChange, handleTabPaneDrop, clampGridSpan,
    actionButtonVisible, blockStyle, tableColumns, aiActionColumn, aiTableColumns, hasExplicitSearchFieldRefs, aiSearchSchema, crudSearchTypeRequestParams,
    designerCrudPublicParams, runtimeExtensionHandlers, aiFormSchema, aiFormCreateApi, blockApiConfig, crudPagePresentation, effectiveRuntimeCrudProps, shouldRenderTreeCrudShell,
    shouldRenderRuntimeListGridShell, runtimeListGridModel, shouldSuppressBareCrudFallback,
    extensionRuntimeApi, designerCrudHookHandlers, livePreviewRecord, blockTableRowHeight, detailInfoGridStyle, detailInfoLabelStyle, detailInfoContentStyle, resolveDetailFieldLabel, resolveDetailFieldSpan, resolvedApiConfig, hasConfiguredCrudRequest, shouldRequestCrudPreviewApi,
    isStaticCrudPreview, staticCrudPreviewMessage, preventStaticCrudSubmit, resolvedDesignerCrudHookHandlers, previewPagination, toolbarCustomActions, transferOptions, transferValue,
    transferSelectedOptions, resolvedTextAlign, resolvedTextFontSize, resolvedTextFontWeight, textContentStyle, pageTitleStyle, pageTitleRichContent, textTitleStyle,
    paragraphStyle, paragraphListTag, paragraphLines, updatePageTitleRichContent, watermarkContent, watermarkProps, avatarInitial, safeIframeSrc,
    qrcodeQrOptions, qrcodeDotsOptions, qrcodeBackgroundOptions, qrcodeCornersSquareOptions, qrcodeCornersDotOptions, barcodeSvgRef, barcodeValid, renderBarcode,
    boxLayoutStyle, layoutCardStyle, panelBoxStyle, spacePreviewStyle, descriptionGridStyle, sampleRows, normalizeOptionItems, hasAction, fieldAlign, fieldSetting,
    renderTableCell, resolveSearchOptionSource, resolveNestedBlockFields, resolveNestedBlockRuntimeCrudProps, resolveNestedBlockRuntimeCrudLoading, resolveNestedBlockDataSourceConfigured, detailValue, normalizeBlockBoundRows,
    boundContentValue, readBoundField, loadBlockBindingData, normalizeBlockBindingParams, loadDetailInfoRecord, normalizeDetailInfoParams, resolveRuntimeTreeApi, loadRuntimeTree,
    loadRuntimeTreeChildren, normalizeRuntimeTreeNodes, expandTree, collapseTree, toggleTreePanel, clearRuntimeTreeSelection, handleRuntimeTreeSelected, emitCrudPreviewState,
    handleCrudPreviewSuccess, handleCrudPreviewError, applyCrudPreviewMode, handleBackClick, handleLinkClick, handleActionButtonClick, handleButtonGroupClick, handleQuickEntrySelect, handleWorkflowTaskNavigate, handleWidgetLinkSelect, handleRuntimeBlockShellClick, resolvePrimaryClickEvent, navigateRuntimeEvent, resolveRuntimeParamValue,
    resolveRuntimeText, resolveSystemRuntimeParam, hasRuntimePermission, handleAiFormSubmit, handleStandaloneAiFormValueUpdate, handleRuntimeEventSuccess,
    resolvePreviewChildHeight, hasGridCellChildren, shouldShowGridCellDropPreview, handleGridCellDragEnter, handleContainerDragEnter, resolveFormLayoutBlockType, nestedChildShellStyle, isNestedFillHeightChild, resolveChartBlockHeight, createLegacyPageTitleContent,
    loadBarcodeModule, componentType, resolveTitleFontSize, sanitizeIframeSrc, sanitizeCustomHtmlContent, toOptionalNumber, isDefaultAiFormField, toAiFormField, trendClass,
    sampleValue, extractBlockBoundRows, normalizeBlockRow, boundRowField, interpolateText, getNestedRecordValue, parseApiConfigValue, extractRuntimeTreeRows,
    countTreeNodes, collectTreeKeys, normalizeRuntimeTreeConfig, appendRuntimeEventParams, resolveTemplatePlaceholder, matchDisplayCondition, parseRuntimeApiConfig,
    resolveEffectiveFormOpenMode, resolveEffectiveModalType, buildRuntimeCrudBlockProps, normalizeTableRowGap, pageWidgetComponentKeys, confirmRuntimeAction, toCssSize, parseInlineStyle, gridBlockExposeApi,
  }
}
