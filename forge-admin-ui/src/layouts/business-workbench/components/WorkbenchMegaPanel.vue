<template>
  <Transition name="mega-panel">
    <div
      v-if="expandedMenu" :id="panelId"
      class="business-mega-panel" :class="{ 'is-rail': variant === 'rail' }"
      :aria-label="`${expandedMenu.label}子菜单`" :data-menu-key="expandedMenu.key"
      :style="variant === 'rail' ? undefined : panelAnchorStyle"
    >
      <div ref="panelInner" class="business-mega-inner" @mousemove="trackPointer">
        <aside class="mega-panel-aside">
          <div class="mega-panel-heading">
            <span class="mega-panel-icon" aria-hidden="true">
              <IconRenderer v-if="typeof expandedMenu.icon === 'string' && expandedMenu.icon" :icon="expandedMenu.icon" :size="22" />
              <component :is="expandedMenu.icon" v-else-if="expandedMenu.icon" />
              <i v-else class="i-lucide:layout-grid" />
            </span>
            <h2>{{ expandedMenu.label }}</h2>
          </div>
          <p class="mega-panel-description">
            {{ menuDescription }}
          </p>

          <label class="mega-menu-search">
            <i class="i-lucide:search" aria-hidden="true" />
            <input v-model.trim="searchQuery" type="search" :placeholder="`搜索${expandedMenu.label}`" :aria-label="`搜索${expandedMenu.label}菜单`" autocomplete="off">
            <button v-if="searchQuery" type="button" aria-label="清空菜单搜索" @click="searchQuery = ''">
              <i class="i-lucide:x" />
            </button>
          </label>

          <nav
            v-if="filteredSections.length"
            class="mega-section-nav"
            :aria-label="`${expandedMenu.label}二级菜单`"
            @mouseleave="onSectionNavLeave"
          >
            <button
              v-for="section in filteredSections" :key="section.key" type="button"
              :class="{ 'is-active': activeSection?.key === section.key, 'is-current': currentSectionKey === section.key }"
              :aria-current="currentSectionKey === section.key ? 'page' : undefined"
              @click="handleSection(section)"
              @mouseenter="scheduleSectionPreview(section, $event)"
              @mouseleave="onSectionButtonLeave(section, $event)"
              @focus="commitSectionPreview(section)"
            >
              <span class="mega-section-nav__icon" aria-hidden="true">
                <IconRenderer v-if="typeof section.icon === 'string' && section.icon" :icon="section.icon" :size="15" />
                <component :is="section.icon" v-else-if="section.icon" />
                <i v-else class="i-lucide:folder" />
              </span>
              <span class="mega-section-nav__label">{{ section.title }}</span>
              <i v-if="section.hasChildren" class="mega-section-nav__arrow i-lucide:chevron-right" aria-hidden="true" />
            </button>
          </nav>

          <div v-if="filteredSections.length" class="mega-section-nav-summary">
            {{ filteredSections.length }} 个二级菜单 · {{ totalFeatureCount }} 个入口
          </div>
        </aside>

        <section
          class="mega-panel-main"
          @mouseenter="lockSectionPreview"
          @mouseleave="unlockSectionPreview"
        >
          <header class="mega-panel-toolbar">
            <div>
              <strong>{{ activeSection?.title || '全部功能' }}</strong>
              <span>{{ activeSection?.hasChildren ? `${activeFeatureCount} 个可访问入口` : '直接入口' }}</span>
            </div>
            <button class="mega-close" type="button" aria-label="关闭菜单" @click="store.closeMenus()">
              <i class="i-lucide:x" />
            </button>
          </header>

          <div v-if="activeSection" class="mega-links">
            <section v-if="activeSection.hasChildren" :key="activeSection.key" class="mega-section-content">
              <WorkbenchMenuBranch :items="activeSection.items" />
            </section>
            <section v-else :key="`direct-${activeSection.key}`" class="mega-direct-entry">
              <span class="mega-direct-entry__icon" aria-hidden="true">
                <IconRenderer v-if="typeof activeSection.icon === 'string' && activeSection.icon" :icon="activeSection.icon" :size="24" />
                <component :is="activeSection.icon" v-else-if="activeSection.icon" />
                <i v-else class="i-lucide:arrow-up-right" />
              </span>
              <strong>{{ activeSection.title }}</strong>
              <span>该功能可直接进入，无下级菜单。</span>
              <button type="button" @click="navigate(activeSection.entry)">
                进入功能
              </button>
            </section>
          </div>

          <div v-else class="mega-search-empty" role="status">
            <i class="i-lucide:search-x" aria-hidden="true" />
            <strong>没有找到相关菜单</strong>
            <span>请换一个关键词，或清空搜索查看全部功能。</span>
            <button type="button" @click="searchQuery = ''">
              清空搜索
            </button>
          </div>
        </section>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { useBusinessWorkbenchStore } from '@/stores/layout/businessWorkbenchStore'
import { countMenuLeaves, filterMegaSections } from '../menu-model'
import { useWorkbenchNavigation } from '../useWorkbenchNavigation'
import WorkbenchMenuBranch from './WorkbenchMenuBranch.vue'

const props = defineProps({
  variant: { type: String, default: 'dropdown' },
})
const store = useBusinessWorkbenchStore()
const panelId = computed(() => props.variant === 'rail' ? 'side-flyout-panel' : 'business-mega-panel')
const { activeKey, expandedMenu, navigate, sections, trail } = useWorkbenchNavigation()
/** 必须在同一个二级上停住才切三级；路过或斜向进入右侧都不切 */
const SECTION_INTENT_MS = 520

const panelInner = ref(null)
const searchQuery = ref('')
const selectedSectionKey = ref(null)
const panelAnchorStyle = ref({})
let sectionIntentTimer
let pendingSectionKey = null
let browsingFeatures = false
let lastPointer = { x: 0, y: 0 }
const filteredSections = computed(() => filterMegaSections(sections.value, searchQuery.value))
const menuDescription = computed(() => `集中查看和使用${expandedMenu.value?.label || '当前模块'}下的授权能力。`)
const currentSectionKey = computed(() => trail.value[1]?.key)
const activeSection = computed(() => {
  const visible = filteredSections.value
  return visible.find(section => section.hasChildren && section.key === selectedSectionKey.value)
    || visible.find(section => section.hasChildren && section.key === currentSectionKey.value)
    || visible.find(section => section.hasChildren)
    || visible.find(section => section.key === activeKey.value)
    || visible[0]
})
const activeFeatureCount = computed(() => activeSection.value?.hasChildren ? countMenuLeaves(activeSection.value.items) : 1)
const totalFeatureCount = computed(() => filteredSections.value.reduce((total, section) => total + (section.hasChildren ? countMenuLeaves(section.items) : 1), 0))

function cancelSectionIntent() {
  window.clearTimeout(sectionIntentTimer)
  sectionIntentTimer = undefined
  pendingSectionKey = null
}

function trackPointer(event) {
  lastPointer = { x: event.clientX, y: event.clientY }
}

function isInsideMain(node) {
  return Boolean(node && panelInner.value?.querySelector('.mega-panel-main')?.contains(node))
}

function isMovingTowardMain(event) {
  const main = panelInner.value?.querySelector('.mega-panel-main')
  if (!main)
    return false
  const rect = main.getBoundingClientRect()
  const dx = event.clientX - lastPointer.x
  const dy = event.clientY - lastPointer.y
  if (rect.left >= event.clientX - 8) {
    const aimingMain = event.clientY >= rect.top - 16 && event.clientY <= rect.bottom + 16
    return dx >= 1 && aimingMain && Math.abs(dx) >= Math.abs(dy) * 0.25
  }
  if (rect.top >= event.clientY - 8)
    return dy >= 1 && Math.abs(dy) >= Math.abs(dx) * 0.25
  return false
}

function commitSectionPreview(section) {
  cancelSectionIntent()
  if (section.hasChildren)
    selectedSectionKey.value = section.key
}

function scheduleSectionPreview(section, event) {
  if (!section.hasChildren)
    return
  if (!window.matchMedia('(hover: hover)').matches) {
    commitSectionPreview(section)
    return
  }
  if (activeSection.value?.key === section.key) {
    cancelSectionIntent()
    return
  }
  if (browsingFeatures || isMovingTowardMain(event)) {
    cancelSectionIntent()
    return
  }
  cancelSectionIntent()
  pendingSectionKey = section.key
  sectionIntentTimer = window.setTimeout(() => {
    if (pendingSectionKey !== section.key || browsingFeatures)
      return
    selectedSectionKey.value = section.key
    sectionIntentTimer = undefined
    pendingSectionKey = null
  }, SECTION_INTENT_MS)
}

function onSectionButtonLeave(section, event) {
  if (pendingSectionKey !== section.key)
    return
  if (isInsideMain(event.relatedTarget))
    lockSectionPreview()
  else
    cancelSectionIntent()
}

function onSectionNavLeave(event) {
  if (isInsideMain(event.relatedTarget))
    lockSectionPreview()
  else
    cancelSectionIntent()
}

function lockSectionPreview() {
  browsingFeatures = true
  cancelSectionIntent()
}

function unlockSectionPreview() {
  browsingFeatures = false
}

function handleSection(section) {
  browsingFeatures = false
  cancelSectionIntent()
  if (section.hasChildren)
    selectedSectionKey.value = section.key
  else
    navigate(section.entry)
}

/** 面板仍挂在触发菜单下方（非顶栏全宽）；内部布局用 container query 自适应 */
function syncPanelAnchor() {
  if (props.variant === 'rail' || !store.activeMenu || window.innerWidth <= 640) {
    panelAnchorStyle.value = {}
    return
  }
  const header = document.querySelector('.business-workbench-header')
  const buttons = [...(header?.querySelectorAll('.business-mega-triggers > button[aria-controls="business-mega-panel"]') || [])]
  const activeButton = buttons.find(button => button.dataset.menuKey === String(store.activeMenu))
  if (!header || !activeButton)
    return

  const headerRect = header.getBoundingClientRect()
  const activeRect = activeButton.getBoundingClientRect()
  const edgeGap = 12
  const left = Math.max(0, activeRect.left - headerRect.left)
  const safeRight = Math.min(headerRect.right, window.innerWidth) - edgeGap
  const available = Math.max(0, safeRight - (headerRect.left + left))
  const width = Math.min(1040, Math.max(280, available))

  panelAnchorStyle.value = {
    '--mega-panel-anchor-left': `${left}px`,
    '--mega-panel-anchor-width': `${width}px`,
    '--mega-panel-anchor-translate': '0',
  }
}

watch(() => store.activeMenu, () => {
  browsingFeatures = false
  cancelSectionIntent()
  searchQuery.value = ''
  selectedSectionKey.value = null
  syncPanelAnchor()
})
watch(searchQuery, () => {
  browsingFeatures = false
  cancelSectionIntent()
  selectedSectionKey.value = null
})
onMounted(() => {
  syncPanelAnchor()
  window.addEventListener('resize', syncPanelAnchor)
})
onBeforeUnmount(() => {
  cancelSectionIntent()
  window.removeEventListener('resize', syncPanelAnchor)
})
</script>
