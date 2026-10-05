<template>
  <div
    id="top-tab"
    ref="topTabRef"
    :class="{
      'has-tab-overflow-start': canScrollLeft,
      'has-tab-overflow-end': canScrollRight,
    }"
    @wheel.capture="handleTabWheel"
    @mousedown.capture="handleTabMouseDown"
    @click.capture="handleTabClickCapture"
  >
    <div
      ref="tabScrollRef"
      class="top-tab-scroll-viewport"
      @scroll="syncTabOverflowState"
    >
      <VueDraggable
        class="top-tab-list"
        role="tablist"
        item-key="path"
        :model-value="tabStore.tabs"
        :animation="180"
        :move="canMoveTab"
        ghost-class="top-tab-drag-ghost"
        chosen-class="top-tab-drag-chosen"
        drag-class="top-tab-dragging-item"
        @update:model-value="handleTabReorder"
        @start="handleTabSortStart"
        @end="handleTabSortEnd"
      >
        <template #item="{ element: item }">
          <div
            class="top-tab-item"
            :class="[
              `is-group-${resolveTabGroup(item.path)}`,
              {
                'is-active': item.path === tabStore.activeTab,
                'is-pinned': item.pinned,
                'is-dirty': item.dirty,
              },
            ]"
            :data-tab-path="item.path"
            role="tab"
            tabindex="0"
            :title="`${item.title}\n${item.path}`"
            :aria-selected="item.path === tabStore.activeTab"
            @click="handleItemClick(item.path)"
            @dblclick="handleTabDoubleClick(item)"
            @auxclick="handleTabAuxClick($event, item)"
            @keydown.enter.prevent="handleItemClick(item.path)"
            @keydown.space.prevent="handleItemClick(item.path)"
            @contextmenu.prevent="handleContextMenu($event, item)"
          >
            <span class="top-tab-group-marker" aria-hidden="true" />
            <IconRenderer
              v-if="item.icon"
              :icon="item.icon"
              :size="14"
              class="top-tab-page-icon"
            />
            <span class="top-tab-label">{{ item.title }}</span>
            <span v-if="item.dirty" class="top-tab-dirty-dot" title="有未保存的更改" aria-label="有未保存的更改" />
            <button
              v-if="item.pinned"
              type="button"
              class="top-tab-pin"
              :aria-label="`取消固定${item.title}`"
              title="取消固定"
              @click.stop="tabStore.unpinTab(item.path)"
            >
              <i class="i-material-symbols:keep-rounded" />
            </button>
            <button
              v-else-if="isTabClosable(item)"
              type="button"
              class="top-tab-close"
              :aria-label="`关闭${item.title}`"
              title="关闭"
              @click.stop="requestCloseTab(item)"
            >
              <i class="i-material-symbols:close-rounded" />
            </button>
          </div>
        </template>
      </VueDraggable>
    </div>

    <button
      v-show="canScrollLeft"
      type="button"
      class="top-tab-scroll-button is-left"
      aria-label="向左滚动标签页"
      title="向左滚动"
      @click.stop="scrollTabs(-1)"
    >
      <i class="i-material-symbols:chevron-left-rounded" />
    </button>
    <div class="top-tab-end-tools">
      <button
        v-show="canScrollRight"
        type="button"
        class="top-tab-scroll-button is-right"
        aria-label="向右滚动标签页"
        title="向右滚动"
        @click.stop="scrollTabs(1)"
      >
        <i class="i-material-symbols:chevron-right-rounded" />
      </button>

      <n-popover
        v-model:show="searchPanelVisible"
        trigger="click"
        placement="bottom-end"
        :show-arrow="false"
        @update:show="handleSearchPanelVisibleChange"
      >
        <template #trigger>
          <button
            type="button"
            class="top-tab-search-button"
            aria-label="查找已打开的标签页"
            title="查找已打开的标签页"
          >
            <i class="i-lucide:list-filter" />
          </button>
        </template>
        <div class="top-tab-search-panel">
          <n-input
            ref="tabSearchInputRef"
            v-model:value="tabSearchKeyword"
            clearable
            placeholder="搜索页面名称或路径"
          />
          <div class="top-tab-search-results">
            <button
              v-for="item in filteredTabs"
              :key="item.path"
              type="button"
              class="top-tab-search-result"
              :class="{ 'is-active': item.path === tabStore.activeTab }"
              @click="handleSearchTabSelect(item)"
            >
              <IconRenderer v-if="item.icon" :icon="item.icon" :size="16" />
              <span class="top-tab-search-result__content">
                <strong>{{ item.title }}</strong>
                <small>{{ item.path }}</small>
              </span>
              <i v-if="item.pinned" class="i-material-symbols:keep-rounded" />
            </button>
            <div v-if="!filteredTabs.length" class="top-tab-search-empty">
              没有匹配的标签页
            </div>
          </div>
        </div>
      </n-popover>
    </div>

    <ContextMenu
      v-if="contextMenuOption.show"
      v-model:show="contextMenuOption.show"
      :current-path="contextMenuOption.currentPath"
      :x="contextMenuOption.x"
      :y="contextMenuOption.y"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import VueDraggable from 'vuedraggable'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenu } from '@/composables'
import { useTabStore } from '@/store'
import { confirmDirtyTabs } from '@/utils/tab-interactions'
import ContextMenu from './ContextMenu.vue'

const tabStore = useTabStore()
const { handleMenuSelect: baseHandleMenuSelect } = useMenu()
const topTabRef = ref(null)
const tabScrollRef = ref(null)
const tabSearchInputRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)
const searchPanelVisible = ref(false)
const tabSearchKeyword = ref('')
let observedScrollElement = null
let tabResizeObserver = null

const filteredTabs = computed(() => {
  const keyword = tabSearchKeyword.value.trim().toLowerCase()
  if (!keyword)
    return tabStore.tabs
  return tabStore.tabs.filter(tab => `${tab.title || ''} ${tab.path || ''}`.toLowerCase().includes(keyword))
})

const tabDragState = {
  active: false,
  moved: false,
  startX: 0,
  startScrollLeft: 0,
  scrollElement: null,
  previousUserSelect: '',
  suppressClick: false,
}

const contextMenuOption = reactive({
  show: false,
  x: 0,
  y: 0,
  currentPath: '',
})

function isTabClosable(tab) {
  if (tab?.pinned)
    return false
  if (tab?.closable === false)
    return false
  if (tab?.forceClosable)
    return true
  return tabStore.tabs.length > 1
}

function getActiveTab() {
  return tabStore.tabs.find(tab => tab.path === tabStore.activeTab || tab.key === tabStore.activeTab)
}

function authorizeDirtyNavigation(tab) {
  if (tab?.dirty)
    tabStore.authorizeDirtyNavigation(tab.key || tab.path)
}

async function handleItemClick(path) {
  if (path === tabStore.activeTab)
    return
  const activeTab = getActiveTab()
  if (!await confirmDirtyTabs(activeTab, '切换页面'))
    return
  authorizeDirtyNavigation(activeTab)
  tabStore.setActiveTab(path)
  baseHandleMenuSelect(undefined, path)
}

async function requestCloseTab(tab) {
  if (!isTabClosable(tab) || !await confirmDirtyTabs(tab, '关闭页面'))
    return
  if (tab.path === tabStore.activeTab || tab.key === tabStore.activeTab)
    authorizeDirtyNavigation(tab)
  tabStore.removeTab(tab.key || tab.path)
}

function handleTabDoubleClick(tab) {
  if (!tab.pinned)
    requestCloseTab(tab)
}

function handleTabAuxClick(event, tab) {
  if (event.button !== 1 || tab.pinned)
    return
  event.preventDefault()
  requestCloseTab(tab)
}

function handleTabReorder(tabs) {
  tabStore.reorderTabs(tabs)
}

function canMoveTab(event) {
  const draggedTab = event.draggedContext?.element
  const relatedTab = event.relatedContext?.element
  if (!draggedTab || !relatedTab)
    return true
  return Boolean(draggedTab.pinned) === Boolean(relatedTab.pinned)
}

function handleTabSortStart() {
  topTabRef.value?.classList.add('is-tab-sorting')
}

function handleTabSortEnd() {
  topTabRef.value?.classList.remove('is-tab-sorting')
  nextTick(() => {
    syncTabOverflowState()
  })
}

function resolveTabGroup(path = '') {
  if (path.startsWith('/system/'))
    return 'system'
  if (path.startsWith('/flow/') || path.startsWith('/workspace/'))
    return 'flow'
  if (path.startsWith('/app-center/') || path.startsWith('/ai/crud-page/'))
    return 'app'
  if (path.startsWith('/data/'))
    return 'data'
  if (path.startsWith('/generator/') || path.startsWith('/ai/'))
    return 'develop'
  if (path.startsWith('/message/'))
    return 'message'
  return 'default'
}

async function handleSearchPanelVisibleChange(show) {
  if (!show) {
    tabSearchKeyword.value = ''
    return
  }
  await nextTick()
  tabSearchInputRef.value?.focus()
}

async function handleSearchTabSelect(tab) {
  await handleItemClick(tab.path)
  searchPanelVisible.value = false
  await nextTick()
  scrollActiveTabIntoView()
}

async function switchRelativeTab(direction) {
  if (tabStore.tabs.length < 2)
    return
  const activeIndex = tabStore.tabs.findIndex(tab => tab.path === tabStore.activeTab || tab.key === tabStore.activeTab)
  const nextIndex = (activeIndex + direction + tabStore.tabs.length) % tabStore.tabs.length
  await handleItemClick(tabStore.tabs[nextIndex].path)
}

async function handleGlobalKeydown(event) {
  const key = event.key.toLowerCase()
  if ((event.ctrlKey || event.metaKey) && key === 'w') {
    const activeTab = getActiveTab()
    if (!isTabClosable(activeTab))
      return
    event.preventDefault()
    await requestCloseTab(activeTab)
    return
  }
  if (event.ctrlKey && event.key === 'Tab') {
    event.preventDefault()
    await switchRelativeTab(event.shiftKey ? -1 : 1)
    return
  }
  if (event.altKey && !event.ctrlKey && !event.metaKey && /^[1-9]$/.test(event.key)) {
    const targetTab = tabStore.tabs[Number(event.key) - 1]
    if (!targetTab)
      return
    event.preventDefault()
    await handleItemClick(targetTab.path)
  }
}

function handleTabDirtyEvent(event) {
  const detail = event.detail || {}
  tabStore.setTabDirty(detail.path || tabStore.activeTab, detail.dirty !== false, detail.message || '')
}

function handleBeforeUnload(event) {
  if (!tabStore.tabs.some(tab => tab.dirty))
    return
  event.preventDefault()
  event.returnValue = ''
}

function resolveTabScrollElement() {
  return tabScrollRef.value
}

function scrollActiveTabIntoView() {
  const activeTab = Array.from(topTabRef.value?.querySelectorAll('.top-tab-item') || [])
    .find(element => element.dataset.tabPath === tabStore.activeTab)
  activeTab?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
}

function syncTabOverflowState() {
  const scrollElement = resolveTabScrollElement()
  if (!scrollElement) {
    canScrollLeft.value = false
    canScrollRight.value = false
    return
  }

  canScrollLeft.value = scrollElement.scrollLeft > 1
  canScrollRight.value = scrollElement.scrollLeft + scrollElement.clientWidth < scrollElement.scrollWidth - 1
}

function bindTabScrollElement() {
  const scrollElement = resolveTabScrollElement()
  if (observedScrollElement !== scrollElement) {
    observedScrollElement?.removeEventListener('scroll', syncTabOverflowState)
    observedScrollElement = scrollElement
    observedScrollElement?.addEventListener('scroll', syncTabOverflowState, { passive: true })
  }

  tabResizeObserver?.disconnect()
  if (typeof ResizeObserver !== 'undefined') {
    tabResizeObserver = new ResizeObserver(syncTabOverflowState)
    if (topTabRef.value)
      tabResizeObserver.observe(topTabRef.value)
    if (scrollElement)
      tabResizeObserver.observe(scrollElement)
  }
  syncTabOverflowState()
}

function scrollTabs(direction) {
  const scrollElement = resolveTabScrollElement()
  if (!scrollElement)
    return
  scrollElement.scrollBy({
    left: direction * Math.max(180, scrollElement.clientWidth * 0.6),
    behavior: 'smooth',
  })
}

function handleTabWheel(event) {
  const scrollElement = resolveTabScrollElement()
  if (!scrollElement || scrollElement.scrollWidth <= scrollElement.clientWidth + 1)
    return

  const delta = Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY
  if (!delta)
    return

  scrollElement.scrollLeft += delta
  syncTabOverflowState()
  event.preventDefault()
  event.stopPropagation()
}

function handleTabMouseDown(event) {
  if (event.button !== 0 || event.target.closest('.top-tab-item, .top-tab-search-button, .top-tab-scroll-button'))
    return

  const scrollElement = resolveTabScrollElement()
  if (!scrollElement || scrollElement.scrollWidth <= scrollElement.clientWidth + 1)
    return

  tabDragState.active = true
  tabDragState.moved = false
  tabDragState.startX = event.clientX
  tabDragState.startScrollLeft = scrollElement.scrollLeft
  tabDragState.scrollElement = scrollElement
  tabDragState.previousUserSelect = document.body.style.userSelect
  document.addEventListener('mousemove', handleTabMouseMove, true)
  document.addEventListener('mouseup', handleTabMouseUp, true)
}

function handleTabMouseMove(event) {
  if (!tabDragState.active || !tabDragState.scrollElement)
    return

  const deltaX = event.clientX - tabDragState.startX
  if (!tabDragState.moved && Math.abs(deltaX) < 4)
    return

  if (!tabDragState.moved) {
    tabDragState.moved = true
    document.body.style.userSelect = 'none'
    topTabRef.value?.classList.add('is-tab-dragging')
  }
  tabDragState.scrollElement.scrollLeft = tabDragState.startScrollLeft - deltaX
  syncTabOverflowState()
  event.preventDefault()
}

function handleTabMouseUp() {
  if (!tabDragState.active)
    return

  tabDragState.active = false
  document.removeEventListener('mousemove', handleTabMouseMove, true)
  document.removeEventListener('mouseup', handleTabMouseUp, true)
  document.body.style.userSelect = tabDragState.previousUserSelect || ''
  topTabRef.value?.classList.remove('is-tab-dragging')
  tabDragState.scrollElement = null

  if (tabDragState.moved) {
    tabDragState.suppressClick = true
    window.setTimeout(() => {
      tabDragState.suppressClick = false
    }, 0)
  }
}

function handleTabClickCapture(event) {
  if (!tabDragState.suppressClick)
    return
  tabDragState.suppressClick = false
  event.preventDefault()
  event.stopImmediatePropagation()
}

onMounted(async () => {
  await nextTick()
  bindTabScrollElement()
  window.addEventListener('keydown', handleGlobalKeydown)
  window.addEventListener('beforeunload', handleBeforeUnload)
  window.addEventListener('forge:tab-dirty', handleTabDirtyEvent)
})

watch(
  () => [tabStore.tabs.length, tabStore.activeTab],
  async () => {
    await nextTick()
    bindTabScrollElement()
    scrollActiveTabIntoView()
    window.requestAnimationFrame(syncTabOverflowState)
  },
)

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', handleTabMouseMove, true)
  document.removeEventListener('mouseup', handleTabMouseUp, true)
  observedScrollElement?.removeEventListener('scroll', syncTabOverflowState)
  window.removeEventListener('keydown', handleGlobalKeydown)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('forge:tab-dirty', handleTabDirtyEvent)
  tabResizeObserver?.disconnect()
  if (tabDragState.active)
    document.body.style.userSelect = tabDragState.previousUserSelect || ''
})

function showContextMenu() {
  contextMenuOption.show = true
}
function hideContextMenu() {
  contextMenuOption.show = false
}
function setContextMenu(x, y, currentPath) {
  Object.assign(contextMenuOption, { x, y, currentPath })
}

// 右击菜单
async function handleContextMenu(e, tagItem) {
  const { clientX, clientY } = e
  hideContextMenu()
  setContextMenu(clientX, clientY, tagItem.path)
  await nextTick()
  showContextMenu()
}
</script>

<style scoped>
#top-tab {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  height: 38px;
  overflow: hidden;
  --forge-tab-height: 30px;
  --forge-tab-gap: 5px;
  --forge-tab-text: var(--chrome-tab-text, var(--text-secondary, #4e5969));
  --forge-tab-muted: var(--chrome-tab-text, var(--text-tertiary, #86909c));
  --forge-tab-accent: var(--primary-color, #4242f7);
  --forge-tab-active-bg: var(--chrome-tab-active-bg, color-mix(in srgb, var(--primary-color, #4242f7) 9%, transparent));
  --forge-tab-hover-bg: var(--chrome-tab-hover-bg, color-mix(in srgb, var(--text-primary, #1d2129) 5%, transparent));
}

#top-tab::before,
#top-tab::after {
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: 3;
  width: 30px;
  content: '';
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.18s ease;
}

#top-tab::before {
  left: 0;
  background: linear-gradient(90deg, var(--chrome-tab-surface, var(--bg-primary, #fff)) 12%, transparent);
  box-shadow: inset 12px 0 10px -12px rgb(15 23 42 / 40%);
}

#top-tab::after {
  right: 28px;
  background: linear-gradient(270deg, var(--chrome-tab-surface, var(--bg-primary, #fff)) 12%, transparent);
  box-shadow: inset -12px 0 10px -12px rgb(15 23 42 / 40%);
}

#top-tab.has-tab-overflow-start::before,
#top-tab.has-tab-overflow-end::after {
  opacity: 1;
}

#top-tab.has-tab-overflow-end::after {
  right: 54px;
}

.top-tab-scroll-viewport {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
  height: 38px;
  overflow-x: auto;
  overflow-y: hidden;
  cursor: grab;
  overscroll-behavior-x: contain;
  scrollbar-width: none;
  touch-action: pan-x;
  -webkit-overflow-scrolling: touch;
}

.top-tab-scroll-viewport::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.top-tab-list {
  display: inline-flex;
  width: max-content;
  min-width: 100%;
  height: 38px;
  align-items: center;
  gap: var(--forge-tab-gap);
  padding: 0 4px;
  box-sizing: border-box;
}

.top-tab-item {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  height: var(--forge-tab-height);
  min-height: var(--forge-tab-height);
  align-items: center;
  padding: 0 12px;
  border: 0;
  border-radius: 3px;
  outline: none;
  background: transparent;
  color: var(--forge-tab-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  line-height: var(--forge-tab-height);
  user-select: none;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.top-tab-item.is-pinned {
  padding-right: 8px;
  padding-left: 8px;
  background: color-mix(in srgb, var(--primary-color, #4242f7) 5%, transparent);
}

.top-tab-item.is-pinned .top-tab-label {
  max-width: 72px;
}

.top-tab-group-marker {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  margin-right: 7px;
  border-radius: 50%;
  background: #94a3b8;
}

.top-tab-item.is-group-system .top-tab-group-marker {
  background: #3b82f6;
}

.top-tab-item.is-group-flow .top-tab-group-marker {
  background: #8b5cf6;
}

.top-tab-item.is-group-app .top-tab-group-marker {
  background: #10b981;
}

.top-tab-item.is-group-data .top-tab-group-marker {
  background: #f59e0b;
}

.top-tab-item.is-group-develop .top-tab-group-marker {
  background: #06b6d4;
}

.top-tab-item.is-group-message .top-tab-group-marker {
  background: #ec4899;
}

.top-tab-page-icon {
  display: inline-flex;
  flex: 0 0 auto;
  margin-right: 6px;
}

.top-tab-item::after {
  position: absolute;
  right: 8px;
  bottom: 0;
  left: 8px;
  height: 2px;
  background: var(--chrome-tab-active-text, var(--primary-color, #4242f7));
  content: '';
  opacity: 0;
  transform: scaleX(0.35);
  transition:
    opacity 0.16s ease,
    transform 0.16s ease;
}

.top-tab-item:hover {
  background: var(--forge-tab-hover-bg);
  color: var(--chrome-tab-active-text, var(--text-primary, #1d2129));
}

.top-tab-item:focus-visible {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary-color, #4242f7) 44%, transparent);
}

.top-tab-item.is-active {
  background: var(--forge-tab-active-bg);
  color: var(--chrome-tab-active-text, var(--forge-tab-accent));
  font-weight: 600;
}

.top-tab-item.is-active::after {
  opacity: 1;
  transform: scaleX(1);
}

.top-tab-label {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-tab-dirty-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  margin-left: 7px;
  border-radius: 50%;
  background: var(--warning-color, #f59e0b);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--warning-color, #f59e0b) 14%, transparent);
}

.top-tab-close,
.top-tab-pin {
  display: inline-flex;
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
  padding: 0;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: var(--forge-tab-muted);
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.top-tab-close:hover,
.top-tab-pin:hover {
  background: var(--chrome-tab-hover-bg, color-mix(in srgb, var(--text-primary, #1d2129) 9%, transparent));
  color: var(--chrome-tab-text, var(--text-secondary, #4e5969));
}

.top-tab-close i,
.top-tab-pin i {
  font-size: 14px;
}

.top-tab-pin {
  color: var(--chrome-tab-text, var(--primary-color, #4242f7));
}

#top-tab.is-tab-dragging,
#top-tab.is-tab-dragging * {
  cursor: grabbing !important;
}

#top-tab.is-tab-sorting .top-tab-item {
  cursor: grabbing;
}

.top-tab-drag-ghost {
  opacity: 0.35;
}

.top-tab-drag-chosen,
.top-tab-dragging-item {
  background: var(--forge-tab-active-bg) !important;
  box-shadow: 0 4px 14px rgb(15 23 42 / 18%);
}

.top-tab-scroll-button {
  position: absolute;
  top: 7px;
  z-index: 4;
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid color-mix(in srgb, var(--border-light, #e5e7eb) 82%, transparent);
  border-radius: 4px;
  background: var(--chrome-tab-surface, color-mix(in srgb, var(--bg-primary, #fff) 94%, transparent));
  color: var(--chrome-tab-text, var(--text-secondary, #4e5969));
  cursor: pointer;
  box-shadow: 0 1px 4px rgb(15 23 42 / 14%);
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.top-tab-scroll-button:hover {
  border-color: color-mix(in srgb, var(--primary-color, #4242f7) 36%, var(--border-light, #e5e7eb));
  background: var(--chrome-tab-hover-bg, var(--bg-primary, #fff));
  color: var(--chrome-tab-active-text, var(--forge-tab-accent));
}

.top-tab-scroll-button.is-left {
  left: 2px;
}

.top-tab-scroll-button.is-right {
  position: static;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.top-tab-scroll-button i {
  font-size: 18px;
}

.top-tab-end-tools {
  position: relative;
  z-index: 5;
  display: inline-flex;
  height: 38px;
  flex: 0 0 auto;
  align-items: center;
  padding: 0 3px;
  background: var(--chrome-tab-surface, var(--bg-primary, #fff));
  gap: 2px;
}

.top-tab-search-button {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--chrome-tab-text, var(--text-secondary, #4e5969));
  cursor: pointer;
}

.top-tab-scroll-button.is-right:hover,
.top-tab-search-button:hover {
  border-color: transparent;
  background: var(--forge-tab-hover-bg);
  color: var(--chrome-tab-active-text, var(--forge-tab-accent));
  box-shadow: none;
}

.top-tab-search-button i {
  font-size: 18px;
}

.top-tab-search-panel {
  width: min(360px, calc(100vw - 32px));
  padding: 4px;
}

.top-tab-search-results {
  display: flex;
  max-height: 340px;
  flex-direction: column;
  margin-top: 8px;
  gap: 3px;
  overflow-y: auto;
}

.top-tab-search-result {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  padding: 8px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary, #4e5969);
  cursor: pointer;
  gap: 9px;
  text-align: left;
}

.top-tab-search-result:hover,
.top-tab-search-result.is-active {
  background: var(--forge-tab-active-bg);
  color: var(--primary-color, #4242f7);
}

.top-tab-search-result__content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.top-tab-search-result__content strong,
.top-tab-search-result__content small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-tab-search-result__content strong {
  font-size: 13px;
  font-weight: 600;
}

.top-tab-search-result__content small {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.top-tab-search-empty {
  padding: 28px 12px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  text-align: center;
}

.dark #top-tab {
  /* 内容区深色 Tab 用正文高亮，顶栏内仍优先使用导航表面的对比色。 */
  --forge-tab-accent: var(--text-primary);
  --forge-tab-active-bg: color-mix(in srgb, var(--primary-color, #6a7dff) 18%, transparent);
  --forge-tab-hover-bg: rgba(255, 255, 255, 0.06);
}
.dark .top-tab-search-result:is(:hover, .is-active) {
  color: var(--text-primary);
}
</style>
