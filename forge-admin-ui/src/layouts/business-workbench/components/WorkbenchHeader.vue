<template>
  <header
    ref="header"
    class="business-workbench-header layout-chrome-header"
    :style="{ '--workbench-mega-intent-ms': `${INTENT_MS}ms` }"
    @keydown.esc="closeAndFocus"
    @mouseenter="cancelClose"
    @mouseleave="onHeaderLeave"
  >
    <div class="workbench-brand">
      <TheLogo />
      <TheTitle />
    </div>
    <nav class="business-mega-navigation" aria-label="平台主导航">
      <button v-if="overflowing" type="button" class="menu-scroll" aria-label="向左查看更多菜单" :disabled="arrivedState.left" @click="scrollMenus(-1)">
        <i class="i-lucide:chevron-left" />
      </button>
      <div ref="track" class="business-mega-triggers" @keydown.right.prevent="moveFocus($event, 1)" @keydown.left.prevent="moveFocus($event, -1)">
        <button
          v-for="item in menus" :key="item.key" type="button" :data-menu-key="item.key"
          :class="{
            'active': store.activeMenu ? store.activeMenu === item.key : activeRootKey === item.key,
            'is-open': store.activeMenu === item.key,
            'is-intent': intentMenuKey === item.key,
          }"
          :aria-expanded="item.children.length ? store.activeMenu === item.key : undefined"
          :aria-controls="item.children.length ? 'business-mega-panel' : undefined"
          :aria-current="activeRootKey === item.key ? 'true' : undefined"
          @click="activateMenuFromHeader(item)"
          @mouseenter="hoverMenu(item)"
          @mouseleave="leaveMenu(item)"
          @animationend="onIntentAnimationEnd($event, item)"
          @keydown.down.prevent="openAndFocus(item)"
        >
          <IconRenderer v-if="typeof item.icon === 'string' && item.icon" :icon="item.icon" :size="16" />
          <component :is="item.icon" v-else-if="item.icon" />
          <span>{{ item.label }}</span>
        </button>
        <span v-if="!menus.length" class="menu-empty">{{ permissionStore.menuDataLoaded ? '暂无可访问菜单' : '菜单加载中…' }}</span>
      </div>
      <button v-if="overflowing" type="button" class="menu-scroll" aria-label="向右查看更多菜单" :disabled="arrivedState.right" @click="scrollMenus(1)">
        <i class="i-lucide:chevron-right" />
      </button>
    </nav>
    <HeaderTools class="business-header-tools" show-appearance />
    <WorkbenchMegaPanel />
  </header>
</template>

<script setup>
import { onClickOutside, useResizeObserver, useScroll } from '@vueuse/core'
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import TheLogo from '@/components/common/TheLogo.vue'
import TheTitle from '@/components/common/TheTitle.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import HeaderTools from '@/layouts/components/HeaderTools.vue'
import { usePermissionStore } from '@/store'
import { useBusinessWorkbenchStore } from '@/stores/layout/businessWorkbenchStore'
import { useWorkbenchNavigation } from '../useWorkbenchNavigation'
import WorkbenchMegaPanel from './WorkbenchMegaPanel.vue'

/** 下划线从 0→100% 的时长，走完再展开面板（与 CSS animation 对齐） */
const INTENT_MS = 320
/** 面板已开时，一级菜单间切换保持跟手 */
const SWITCH_DELAY_MS = 50
/** 离开顶栏（含下拉面板）后稍后再关 */
const CLOSE_DELAY_MS = 220

const store = useBusinessWorkbenchStore()
const permissionStore = usePermissionStore()
const { menus, activeRootKey, activateMenu } = useWorkbenchNavigation()
const header = ref(null)
const track = ref(null)
const overflowing = ref(false)
const intentMenuKey = ref(null)
let switchTimer
let closeTimer
const { arrivedState, measure } = useScroll(track)
onClickOutside(header, () => {
  clearAllTimers()
  intentMenuKey.value = null
  store.closeMenus()
})

function measureTrack() {
  overflowing.value = Boolean(track.value && track.value.scrollWidth > track.value.clientWidth + 2)
  measure()
}
useResizeObserver(track, measureTrack)
watch(menus, () => nextTick(measureTrack))
watch(activeRootKey, () => nextTick(() => {
  const button = [...(track.value?.querySelectorAll('button') || [])].find(node => node.dataset.menuKey === activeRootKey.value)
  button?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
}))

function clearAllTimers() {
  window.clearTimeout(switchTimer)
  window.clearTimeout(closeTimer)
}

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function clearIntent() {
  intentMenuKey.value = null
}

function openMenu(item) {
  clearIntent()
  window.clearTimeout(switchTimer)
  store.activeMenu = item.children.length ? item.key : null
}

function hoverMenu(item) {
  if (!window.matchMedia('(hover: hover)').matches)
    return
  window.clearTimeout(closeTimer)
  window.clearTimeout(switchTimer)

  if (!item.children.length) {
    clearIntent()
    if (store.activeMenu) {
      switchTimer = window.setTimeout(() => {
        store.activeMenu = null
      }, SWITCH_DELAY_MS)
    }
    return
  }

  if (store.activeMenu === item.key) {
    clearIntent()
    return
  }

  // 面板已展开：快速切换，不再走完整进度条
  if (store.activeMenu) {
    clearIntent()
    switchTimer = window.setTimeout(() => {
      store.activeMenu = item.key
    }, SWITCH_DELAY_MS)
    return
  }

  // 面板未开：下划线进度走完再开；弱动画偏好则立刻开
  if (prefersReducedMotion()) {
    openMenu(item)
    return
  }
  intentMenuKey.value = item.key
}

function leaveMenu(item) {
  window.clearTimeout(switchTimer)
  if (intentMenuKey.value === item.key)
    clearIntent()
}

function onIntentAnimationEnd(event, item) {
  if (event.pseudoElement !== '::after')
    return
  if (intentMenuKey.value !== item.key)
    return
  if (!item.children.length)
    return
  openMenu(item)
}

function scheduleClose() {
  window.clearTimeout(switchTimer)
  clearIntent()
  if (!store.activeMenu)
    return
  window.clearTimeout(closeTimer)
  closeTimer = window.setTimeout(() => store.closeMenus(), CLOSE_DELAY_MS)
}

function cancelClose() {
  window.clearTimeout(closeTimer)
}

function onHeaderLeave() {
  scheduleClose()
}

function activateMenuFromHeader(item) {
  clearAllTimers()
  clearIntent()
  activateMenu(item)
}

async function openAndFocus(item) {
  if (!item.children.length)
    return
  clearAllTimers()
  openMenu(item)
  await nextTick()
  header.value?.querySelector('.mega-menu-search input')?.focus()
}

function closeAndFocus() {
  const key = store.activeMenu
  clearAllTimers()
  clearIntent()
  store.closeMenus()
  const button = [...(track.value?.querySelectorAll('button') || [])].find(node => node.dataset.menuKey === key)
  button?.focus()
}

function moveFocus(event, direction) {
  const buttons = [...(track.value?.querySelectorAll('button') || [])]
  const index = buttons.indexOf(event.target)
  if (index >= 0)
    buttons[(index + direction + buttons.length) % buttons.length]?.focus()
}

function scrollMenus(direction) {
  track.value?.scrollBy({ left: direction * track.value.clientWidth * 0.7, behavior: 'smooth' })
}

onBeforeUnmount(() => {
  clearAllTimers()
  clearIntent()
})
</script>
