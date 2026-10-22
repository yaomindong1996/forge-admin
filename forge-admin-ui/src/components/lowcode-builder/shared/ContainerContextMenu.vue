<script setup>
/**
 * 容器右键菜单：分组插入组件 + 删除/清空等快捷操作
 * 插入目录与加号弹层共用 page-block-picker，避免新组件只出现在一侧。
 */
import { computed, nextTick, ref, watch } from 'vue'
import {
  AlbumsOutline,
  AnalyticsOutline,
  BrowsersOutline,
  CheckboxOutline,
  CopyOutline,
  DocumentTextOutline,
  GridOutline,
  HomeOutline,
  ImageOutline,
  ListOutline,
  NotificationsOutline,
  PricetagOutline,
  ResizeOutline,
  SearchOutline,
  StarOutline,
  StatsChartOutline,
  TextOutline,
  TimerOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import {
  DEFAULT_NEST_EXCLUDE_BLOCK_TYPES,
  buildPageBlockPickerGroups,
} from '@/components/lowcode-builder/page/page-block-picker'

const props = defineProps({
  show: { type: Boolean, default: false },
  x: { type: Number, default: 0 },
  y: { type: Number, default: 0 },
  /** insert | child */
  mode: { type: String, default: 'insert' },
  title: { type: String, default: '' },
  /** child 模式下是否仍允许插入到同一区域 */
  allowInsert: { type: Boolean, default: true },
  excludeBlockTypes: { type: Array, default: () => [...DEFAULT_NEST_EXCLUDE_BLOCK_TYPES] },
})

const emit = defineEmits(['update:show', 'insert', 'action', 'close'])

const keyword = ref('')
const searchInputRef = ref(null)
const menuRef = ref(null)
/** 视口内夹紧后的坐标 / 高度，避免右键靠近底边时菜单被裁切 */
const menuStyle = ref({ left: '0px', top: '0px' })

const MENU_PAD = 12
const MENU_MAX_H = 520

function clampMenuInViewport() {
  const el = menuRef.value
  if (!el)
    return
  const vw = window.innerWidth || 0
  const vh = window.innerHeight || 0
  let left = Number(props.x) || 0
  let top = Number(props.y) || 0
  // 先按可用高度限制，再量宽高做左右/上下翻转
  const maxH = Math.max(240, Math.min(MENU_MAX_H, vh - MENU_PAD * 2))
  el.style.maxHeight = `${maxH}px`
  const rect = el.getBoundingClientRect()
  const width = rect.width || 420
  const height = Math.min(rect.height || maxH, maxH)
  if (left + width > vw - MENU_PAD)
    left = Math.max(MENU_PAD, vw - MENU_PAD - width)
  if (left < MENU_PAD)
    left = MENU_PAD
  if (top + height > vh - MENU_PAD)
    top = Math.max(MENU_PAD, vh - MENU_PAD - height)
  if (top < MENU_PAD)
    top = MENU_PAD
  const fitH = Math.max(240, Math.min(maxH, vh - top - MENU_PAD))
  menuStyle.value = {
    left: `${Math.round(left)}px`,
    top: `${Math.round(top)}px`,
    maxHeight: `${Math.round(fitH)}px`,
  }
}

const BLOCK_ICON_MAP = {
  'stats-strip': AnalyticsOutline,
  'quick-entry': GridOutline,
  'bar-chart': StatsChartOutline,
  'line-chart': AnalyticsOutline,
  'area-chart': AnalyticsOutline,
  'pie-chart': StatsChartOutline,
  'workflow-task-list': CheckboxOutline,
  'top-ranking': StatsChartOutline,
  'notice-board': NotificationsOutline,
  'welcome-banner': StarOutline,
  'info-panel': DocumentTextOutline,
  'custom-html': DocumentTextOutline,
  'tag-list': PricetagOutline,
  steps: TimerOutline,
  timeline: TimerOutline,
  'empty-state': AlbumsOutline,
  'text-title': TextOutline,
  paragraph: DocumentTextOutline,
  statistic: AnalyticsOutline,
  link: TextOutline,
  'text-tip': DocumentTextOutline,
  'audio-player': BrowsersOutline,
  'video-player': BrowsersOutline,
  avatar: ImageOutline,
  card: AlbumsOutline,
  'panel-box': BrowsersOutline,
  'box-layout': BrowsersOutline,
  'grid-layout': GridOutline,
  tabs: BrowsersOutline,
  divider: ResizeOutline,
  image: ImageOutline,
  table: ListOutline,
  'data-list': ListOutline,
  'crud-table': ListOutline,
  'search-form': SearchOutline,
}

watch(() => props.show, async (visible) => {
  if (!visible)
    return
  keyword.value = ''
  menuStyle.value = {
    left: `${Number(props.x) || 0}px`,
    top: `${Number(props.y) || 0}px`,
  }
  await nextTick()
  clampMenuInViewport()
  // 二次测量：分组列表渲染后再夹一次，避免高度低估仍溢出
  await nextTick()
  clampMenuInViewport()
  searchInputRef.value?.focus?.()
})

watch(() => [props.x, props.y], async () => {
  if (!props.show)
    return
  await nextTick()
  clampMenuInViewport()
})

const actionOptions = computed(() => {
  if (props.mode === 'child') {
    return [
      { key: 'duplicate', label: '复制', icon: CopyOutline },
      { key: 'delete', label: '删除', icon: TrashOutline, danger: true },
    ]
  }
  return [
    { key: 'clear', label: '清空此区域', icon: TrashOutline, danger: true },
  ]
})

const showInsertSection = computed(() => props.mode !== 'child' || props.allowInsert)

const insertGroups = computed(() => buildPageBlockPickerGroups({
  excludeBlockTypes: props.excludeBlockTypes,
  keyword: keyword.value,
}).map(group => ({
  key: group.key,
  label: group.label,
  items: group.items.map(item => ({
    blockType: item.blockType,
    label: item.title || item.blockType,
    desc: item.desc || '',
    icon: BLOCK_ICON_MAP[item.blockType] || HomeOutline,
  })),
})))

const totalCount = computed(() =>
  insertGroups.value.reduce((sum, group) => sum + group.items.length, 0),
)

function close() {
  emit('update:show', false)
  emit('close')
}

function onAction(key) {
  emit('action', key)
  close()
}

function onInsert(blockType) {
  emit('insert', blockType)
  close()
}

function onPanelClick(event) {
  event.stopPropagation()
}
</script>

<template>
  <teleport to="body">
    <div
      v-if="show"
      class="container-ctx-mask"
      @mousedown.self="close"
      @contextmenu.prevent="close"
    >
      <div
        ref="menuRef"
        class="container-ctx-menu"
        :style="menuStyle"
        @mousedown="onPanelClick"
        @click="onPanelClick"
      >
        <!-- 标题栏 -->
        <div class="container-ctx-head">
          <div class="container-ctx-head-text">
            <strong>{{ title || (mode === 'child' ? '组件操作' : '插入组件') }}</strong>
            <span v-if="showInsertSection && !keyword">共 {{ totalCount }} 个</span>
            <span v-else-if="showInsertSection && keyword">匹配 {{ totalCount }} 个</span>
          </div>
          <button type="button" class="container-ctx-close" aria-label="关闭" @click="close">
            ×
          </button>
        </div>

        <!-- 快捷操作：与插入区用分隔线分开，避免挤在一起 -->
        <div v-if="actionOptions.length" class="container-ctx-actions">
          <button
            v-for="item in actionOptions"
            :key="item.key"
            type="button"
            class="container-ctx-action"
            :class="{ danger: item.danger }"
            @click="onAction(item.key)"
          >
            <n-icon :component="item.icon" :size="14" />
            <span>{{ item.label }}</span>
          </button>
        </div>

        <template v-if="showInsertSection">
          <div class="container-ctx-divider" />

          <div class="container-ctx-search-wrap">
            <n-input
              ref="searchInputRef"
              v-model:value="keyword"
              clearable
              size="small"
              placeholder="搜索组件名称…"
              class="container-ctx-search"
            >
              <template #prefix>
                <n-icon :component="SearchOutline" :size="14" />
              </template>
            </n-input>
          </div>

          <div class="container-ctx-scroll">
            <section
              v-for="group in insertGroups"
              :key="group.key"
              class="container-ctx-group"
            >
              <header class="container-ctx-group-head">
                <span>{{ group.label }}</span>
                <em>{{ group.items.length }}</em>
              </header>
              <div class="container-ctx-grid">
                <button
                  v-for="item in group.items"
                  :key="item.blockType"
                  type="button"
                  class="container-ctx-item"
                  :title="item.desc || item.label"
                  @click="onInsert(item.blockType)"
                >
                  <span class="container-ctx-item-icon">
                    <n-icon :component="item.icon" :size="16" />
                  </span>
                  <span class="container-ctx-item-main">
                    <span class="container-ctx-item-title">{{ item.label }}</span>
                    <span v-if="item.desc" class="container-ctx-item-desc">{{ item.desc }}</span>
                  </span>
                </button>
              </div>
            </section>
            <div v-if="!insertGroups.length" class="container-ctx-empty">
              没有匹配的组件
            </div>
          </div>
        </template>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.container-ctx-mask {
  position: fixed;
  inset: 0;
  z-index: 4000;
}

.container-ctx-menu {
  position: fixed;
  z-index: 4001;
  display: flex;
  flex-direction: column;
  width: min(420px, calc(100vw - 24px));
  max-height: min(520px, calc(100vh - 24px));
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  box-shadow:
    0 4px 6px rgba(15, 23, 42, 0.04),
    0 12px 28px rgba(15, 23, 42, 0.12);
  /* 列表在内部滚动，避免贴底时整块被裁切 */
}

.container-ctx-head {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px 8px;
}

.container-ctx-head-text {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.container-ctx-head-text strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.container-ctx-head-text span {
  color: var(--text-tertiary);
  font-size: 11px;
}

.container-ctx-close {
  display: grid;
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  place-items: center;
  border: 0;
  border-radius: 6px;
  padding: 0;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.container-ctx-close:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.container-ctx-actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 12px 8px;
}

.container-ctx-action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.2;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease,
    color 0.15s ease;
}

.container-ctx-action:hover {
  border-color: var(--primary-color, #2080f0);
  background: #f0f7ff;
  color: #165dff;
}

.container-ctx-action.danger {
  color: var(--error-color, #f53f3f);
}

.container-ctx-action.danger:hover {
  border-color: #fbaca3;
  background: #fff1f0;
  color: #cb2634;
}

.container-ctx-divider {
  flex: 0 0 auto;
  height: 1px;
  margin: 0 12px;
  background: var(--border-light);
}

.container-ctx-search-wrap {
  flex: 0 0 auto;
  padding: 8px 12px;
}

.container-ctx-search {
  width: 100%;
}

.container-ctx-scroll {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 0 12px 12px;
  overflow: auto;
  scrollbar-gutter: stable;
  overscroll-behavior: contain;
}

.container-ctx-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.container-ctx-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
}

.container-ctx-group-head em {
  font-style: normal;
  font-weight: 400;
  color: var(--text-tertiary);
  font-size: 11px;
}

.container-ctx-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.container-ctx-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.container-ctx-item:hover {
  border-color: var(--primary-color, #2080f0);
  background: #f7faff;
}

.container-ctx-item-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #f0f5ff;
  color: var(--primary-color, #2080f0);
}

.container-ctx-item:hover .container-ctx-item-icon {
  background: #e8f0ff;
}

.container-ctx-item-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.container-ctx-item-title {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.container-ctx-item-desc {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.container-ctx-empty {
  padding: 28px 8px;
  color: var(--text-tertiary);
  font-size: 12px;
  text-align: center;
}
</style>
