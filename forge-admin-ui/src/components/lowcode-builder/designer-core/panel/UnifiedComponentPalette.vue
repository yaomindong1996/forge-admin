<script setup>
import {
  AlbumsOutline,
  AlertCircleOutline,
  AnalyticsOutline,
  BrowsersOutline,
  BusinessOutline,
  CalculatorOutline,
  CalendarOutline,
  CashOutline,
  CheckboxOutline,
  ChevronDownCircleOutline,
  CloudOutline,
  CloudUploadOutline,
  CodeSlashOutline,
  ColorPaletteOutline,
  ContractOutline,
  DesktopOutline,
  DocumentTextOutline,
  GridOutline,
  HomeOutline,
  ImageOutline,
  KeypadOutline,
  ListOutline,
  LocationOutline,
  NavigateOutline,
  OptionsOutline,
  PersonOutline,
  PricetagOutline,
  QrCodeOutline,
  ReaderOutline,
  RemoveOutline,
  ReorderThreeOutline,
  ResizeOutline,
  SearchOutline,
  StarOutline,
  StatsChartOutline,
  SwapHorizontalOutline,
  TerminalOutline,
  TextOutline,
  TimeOutline,
  TimerOutline,
  ToggleOutline,
} from '@vicons/ionicons5'
/**
 * UnifiedComponentPalette — 统一组件物料面板（P2）
 * @description 两个设计器（表单 / 列表）共用的左侧组件库：
 *   同一注册表（designer-core）、同一分组结构、同一搜索交互、同一 UI。
 *   物料按 scope 过滤：表单显示 scope 含 F 的组件，列表显示 scope 含 L 的组件。
 *   拖拽协议由接入方决定（emit itemDragStart，父组件写各自 dataTransfer）。
 */
import { computed, watch } from 'vue'
import { groupComponents } from '../spec/registry'

const props = defineProps({
  /** 物料作用域：'F' 表单设计器 / 'L' 列表设计器 / 'ALL' 两侧同显合集（画布差异走禁用态） */
  scope: { type: String, required: true },
  /** 显示的物料分类（默认全部；包装节点始终隐藏） */
  categories: {
    type: Array,
    default: () => ['field', 'layout', 'business', 'page', 'media', 'widget'],
  },
  /** 搜索关键字（由接入方外壳管理） */
  keyword: { type: String, default: '' },
  /** 接入方过滤：spec => boolean（如列表侧 onlyFor 过滤） */
  itemFilter: { type: Function, default: null },
  /** 接入方禁用判定：spec => string（返回禁用原因文案，空串表示可用） */
  itemDisabledReason: { type: Function, default: null },
})

const emit = defineEmits(['itemDragStart', 'itemDragEnd', 'itemClick', 'totalChange'])

/** 分组展示顺序（统一注册表的 group 字段） */
const GROUP_ORDER = [
  { key: '布局', title: '布局组件' },
  { key: '输入', title: '输入组件' },
  { key: '选择', title: '选择组件' },
  { key: '业务', title: '业务组件' },
  { key: '数据', title: '数据区块' },
  { key: '操作', title: '操作区块' },
  { key: '页面', title: '页面组件' },
  { key: '导航', title: '导航组件' },
  { key: '内容', title: '内容展示' },
  { key: '媒体', title: '媒体展示' },
  { key: '高级', title: '高级嵌入' },
]

const paletteIconMap = {
  AiCrudPage: DesktopOutline,
  AiForm: DocumentTextOutline,
  AiTable: GridOutline,
  actionButton: ToggleOutline,
  announcement: AlertCircleOutline,
  avatar: PersonOutline,
  backButton: NavigateOutline,
  barcode: StatsChartOutline,
  barcodeScanner: StatsChartOutline,
  box: ResizeOutline,
  breadcrumb: NavigateOutline,
  buttonGroup: ToggleOutline,
  calendar: CalendarOutline,
  card: AlbumsOutline,
  cascader: ChevronDownCircleOutline,
  checkbox: CheckboxOutline,
  code: CodeSlashOutline,
  color: ColorPaletteOutline,
  countdown: TimerOutline,
  customSelect: CloudOutline,
  dataTable: ListOutline,
  date: CalendarOutline,
  daterange: CalendarOutline,
  datetime: TimeOutline,
  datetimerange: TimeOutline,
  descriptions: ReaderOutline,
  detailInfo: ReaderOutline,
  dictSelect: PricetagOutline,
  divider: RemoveOutline,
  emptyState: RemoveOutline,
  fileUpload: CloudUploadOutline,
  formSectionTitle: ReorderThreeOutline,
  grid: ResizeOutline,
  groupTitle: ReorderThreeOutline,
  htmlTag: CodeSlashOutline,
  iframe: BrowsersOutline,
  imageUpload: ImageOutline,
  infoPanel: AlertCircleOutline,
  input: TextOutline,
  list: ListOutline,
  log: TerminalOutline,
  markdown: ReorderThreeOutline,
  menu: ToggleOutline,
  money: CashOutline,
  month: CalendarOutline,
  number: CalculatorOutline,
  numberAnimation: AnalyticsOutline,
  objectReference: BrowsersOutline,
  orgTreeSelect: BusinessOutline,
  pagination: ReaderOutline,
  paragraph: DocumentTextOutline,
  pageTitle: DocumentTextOutline,
  qrcode: QrCodeOutline,
  radioButton: CheckboxOutline,
  radio: CheckboxOutline,
  rate: StarOutline,
  recordSelector: SearchOutline,
  regionTreeSelect: LocationOutline,
  richText: DocumentTextOutline,
  searchForm: SearchOutline,
  select: ListOutline,
  signaturePad: OptionsOutline,
  slider: OptionsOutline,
  space: ResizeOutline,
  spacer: ResizeOutline,
  split: SwapHorizontalOutline,
  statistic: AnalyticsOutline,
  statsStrip: AnalyticsOutline,
  stepForm: ReorderThreeOutline,
  steps: ReorderThreeOutline,
  subTable: ListOutline,
  subTableTabs: BrowsersOutline,
  switch: ToggleOutline,
  table: KeypadOutline,
  tabs: BrowsersOutline,
  tagList: PricetagOutline,
  text: TextOutline,
  textTitle: TextOutline,
  textarea: DocumentTextOutline,
  timeline: TimerOutline,
  timerange: TimerOutline,
  toolbar: ToggleOutline,
  transfer: SwapHorizontalOutline,
  treePanel: ContractOutline,
  treeSelect: GridOutline,
  userSelect: PersonOutline,
  videoPlayer: BrowsersOutline,
  vueComponent: BrowsersOutline,
  watermark: TextOutline,
  year: CalendarOutline,
}

function resolveItemIcon(spec = {}) {
  return paletteIconMap[spec.type] || HomeOutline
}

const visibleGroups = computed(() => {
  const text = (props.keyword || '').trim().toLowerCase()
  const categories = new Set(props.categories)
  // 'ALL' = 两侧同显合集：注册表全量（itemFilter 已排除包装节点），保证两个设计器组件数量一致
  const specs = groupComponents(props.scope === 'ALL' ? undefined : props.scope)
  const grouped = new Map()
  for (const [groupKey, groupSpecs] of specs) {
    if (groupKey === '包装节点')
      continue
    for (const spec of groupSpecs) {
      if (!categories.has(spec.category))
        continue
      if (props.itemFilter && !props.itemFilter(spec))
        continue
      if (text) {
        const haystack = `${spec.label || ''} ${spec.desc || ''} ${spec.type || ''} ${(spec.aliases || []).join(' ')}`.toLowerCase()
        if (!haystack.includes(text))
          continue
      }
      if (!grouped.has(groupKey))
        grouped.set(groupKey, [])
      grouped.get(groupKey).push(spec)
    }
  }
  return GROUP_ORDER
    .filter(order => grouped.has(order.key))
    .map(order => ({ key: order.key, title: order.title, items: grouped.get(order.key) }))
})

const total = computed(() => visibleGroups.value.reduce((sum, group) => sum + group.items.length, 0))

watch(total, value => emit('totalChange', value), { immediate: true })

function disabledReason(spec) {
  return props.itemDisabledReason ? (props.itemDisabledReason(spec) || '') : ''
}

function handleDragStart(event, spec) {
  const reason = disabledReason(spec)
  if (reason) {
    event.preventDefault()
    return
  }
  emit('itemDragStart', { spec, event })
}

function handleClick(spec) {
  // 禁用项与拖拽同口径拦截：不支持当前画布的组件点击也不下发
  if (disabledReason(spec))
    return
  emit('itemClick', spec)
}

defineExpose({ total })
</script>

<template>
  <div class="unified-palette">
    <div class="unified-palette-groups">
      <section v-for="group in visibleGroups" :key="group.key" class="unified-palette-group">
        <div class="unified-palette-group-title">
          <span>{{ group.title }}</span>
          <em>{{ group.items.length }}</em>
        </div>
        <div class="unified-palette-list">
          <button
            v-for="spec in group.items"
            :key="spec.type"
            type="button"
            class="unified-palette-item"
            :class="{ 'is-disabled': !!disabledReason(spec) }"
            :draggable="!disabledReason(spec)"
            :title="disabledReason(spec) || spec.desc"
            @dragstart="handleDragStart($event, spec)"
            @dragend="$emit('itemDragEnd')"
            @click="handleClick(spec)"
          >
            <span class="unified-palette-item-icon">
              <n-icon><component :is="resolveItemIcon(spec)" /></n-icon>
            </span>
            <span class="unified-palette-item-main">
              <span class="unified-palette-item-title">{{ spec.label }}</span>
              <!-- 禁用原因替换描述行完整展示（窄面板下右侧徽标会被截断看不全） -->
              <span v-if="disabledReason(spec)" class="unified-palette-item-reason">
                {{ disabledReason(spec) }}
              </span>
              <span v-else class="unified-palette-item-desc">{{ spec.desc }}</span>
            </span>
          </button>
        </div>
      </section>
      <div v-if="!visibleGroups.length" class="unified-palette-empty">
        没有匹配的组件
      </div>
    </div>
  </div>
</template>

<style scoped>
.unified-palette {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

.unified-palette-groups {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  min-height: 0;
}

.unified-palette-group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--n-text-color, #333);
}

.unified-palette-group-title em {
  font-style: normal;
  font-weight: 400;
  opacity: 0.55;
}

.unified-palette-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.unified-palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 6px;
  background: transparent;
  cursor: grab;
  text-align: left;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.unified-palette-item:hover:not(.is-disabled) {
  border-color: var(--n-primary-color, #2080f0);
}

.unified-palette-item.is-disabled {
  cursor: not-allowed;
  background: var(--n-action-color, #fafafa);
}

.unified-palette-item.is-disabled .unified-palette-item-title,
.unified-palette-item.is-disabled .unified-palette-item-icon {
  opacity: 0.45;
}

.unified-palette-item.is-disabled .unified-palette-item-desc {
  opacity: 0.35;
}

/* 禁用原因保持完整可读（用户需要知道为什么不能用） */
.unified-palette-item.is-disabled .unified-palette-item-reason {
  opacity: 0.9;
}

.unified-palette-item-icon {
  flex-shrink: 0;
  display: inline-flex;
  font-size: 16px;
  color: var(--n-primary-color, #2080f0);
}

.unified-palette-item-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.unified-palette-item-title {
  font-size: 12px;
  line-height: 1.4;
  color: var(--n-text-color, #333);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unified-palette-item-desc {
  font-size: 11px;
  line-height: 1.3;
  opacity: 0.55;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unified-palette-item-reason {
  font-size: 11px;
  line-height: 1.3;
  color: var(--n-warning-color, #f0a020);
  word-break: break-all;
}

.unified-palette-empty {
  padding: 24px 0;
  font-size: 12px;
  text-align: center;
  opacity: 0.55;
}
</style>
