<template>
  <div
    ref="wrapRef"
    class="forge-top-menu"
    :class="{ 'can-scroll-left': canScrollLeft, 'can-scroll-right': canScrollRight }"
  >
    <button
      v-show="canScrollLeft"
      class="forge-top-menu-scroll is-left"
      type="button"
      aria-label="向左查看更多菜单"
      @click="scrollMenu('left')"
    >
      <i class="i-material-symbols:chevron-left-rounded" />
    </button>

    <div ref="scrollRef" class="forge-top-menu-track" @scroll="updateScrollState">
      <div class="forge-top-menu-list">
        <div
          v-for="item in items"
          :key="item.key"
          class="forge-top-menu-node"
          :class="{
            'is-active': isItemActive(item),
            'has-dropdown': dropdown && hasChildren(item),
            'is-dropdown-open': isDropdownOpen(item),
          }"
          @mouseenter="openDropdown(item, $event.currentTarget)"
          @mouseleave="scheduleCloseDropdown"
          @focusin="openDropdown(item, $event.currentTarget)"
          @focusout="scheduleCloseDropdown"
        >
          <button
            class="forge-top-menu-item"
            type="button"
            :aria-current="isItemActive(item) ? 'page' : undefined"
            :aria-haspopup="dropdown && hasChildren(item) ? 'menu' : undefined"
            @click="handleItemClick(item)"
          >
            <span v-if="item.icon" class="forge-top-menu-icon">
              <IconRenderer v-if="typeof item.icon === 'string'" :icon="item.icon" :size="16" />
              <component :is="item.icon" v-else />
            </span>
            <span class="forge-top-menu-label">{{ item.label }}</span>
            <span v-if="dropdown && hasChildren(item)" class="forge-top-menu-caret">
              <i class="i-material-symbols:keyboard-arrow-down-rounded" />
            </span>
          </button>
        </div>
      </div>
    </div>

    <button
      v-show="canScrollRight"
      class="forge-top-menu-scroll is-right"
      type="button"
      aria-label="向右查看更多菜单"
      @click="scrollMenu('right')"
    >
      <i class="i-material-symbols:chevron-right-rounded" />
    </button>

    <div
      v-if="activeDropdownChildren.length"
      class="forge-top-menu-dropdown"
      :style="dropdownStyle"
      role="menu"
      @mouseenter="cancelCloseDropdown"
      @focusin="cancelCloseDropdown"
      @mouseleave="scheduleCloseDropdown"
    >
      <TopMenuDropdown
        :items="activeDropdownChildren"
        :active-key="activeKey"
        @select="handleItemClick"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import TopMenuDropdown from './TopMenuDropdown.vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  activeKey: {
    type: [String, Number],
    default: '',
  },
  dropdown: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select'])

const wrapRef = ref(null)
const scrollRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)
const activeDropdownKey = ref('')
const dropdownStyle = ref({})
let resizeObserver = null
let closeDropdownTimer = null

function normalizeKey(key) {
  return key === undefined || key === null ? '' : String(key)
}

function hasChildren(item) {
  return Array.isArray(item?.children) && item.children.length > 0
}

function isItemActive(item) {
  const activeKey = normalizeKey(props.activeKey)
  if (!activeKey)
    return false
  if (normalizeKey(item.key) === activeKey)
    return true
  return hasChildren(item) && item.children.some(child => isItemActive(child))
}

const activeDropdownChildren = computed(() => {
  if (!activeDropdownKey.value)
    return []
  const activeItem = props.items.find(item => normalizeKey(item.key) === activeDropdownKey.value)
  return activeItem?.children || []
})

function isDropdownOpen(item) {
  return activeDropdownKey.value === normalizeKey(item.key)
}

function updateDropdownPosition(targetEl) {
  if (!targetEl)
    return

  const rect = targetEl.getBoundingClientRect()
  const viewportWidth = window.innerWidth || document.documentElement.clientWidth
  const minWidth = 190
  const maxWidth = 320
  const left = Math.min(Math.max(8, rect.left), Math.max(8, viewportWidth - maxWidth - 8))
  dropdownStyle.value = {
    position: 'fixed',
    top: `${rect.bottom - 6}px`,
    left: `${left}px`,
    minWidth: `${Math.max(minWidth, Math.min(maxWidth, rect.width + 56))}px`,
  }
}

function openDropdown(item, targetEl) {
  cancelCloseDropdown()
  if (!props.dropdown || !hasChildren(item)) {
    activeDropdownKey.value = ''
    return
  }

  activeDropdownKey.value = normalizeKey(item.key)
  updateDropdownPosition(targetEl)
}

function cancelCloseDropdown() {
  if (!closeDropdownTimer)
    return
  clearTimeout(closeDropdownTimer)
  closeDropdownTimer = null
}

function scheduleCloseDropdown() {
  cancelCloseDropdown()
  closeDropdownTimer = window.setTimeout(() => {
    activeDropdownKey.value = ''
    closeDropdownTimer = null
  }, 120)
}

function handleItemClick(item) {
  activeDropdownKey.value = ''
  emit('select', item)
}

function updateScrollState() {
  const el = scrollRef.value
  if (!el)
    return

  activeDropdownKey.value = ''
  const maxScrollLeft = Math.max(0, el.scrollWidth - el.clientWidth)
  canScrollLeft.value = el.scrollLeft > 1
  canScrollRight.value = el.scrollLeft < maxScrollLeft - 1
}

function scrollMenu(direction) {
  const el = scrollRef.value
  if (!el)
    return

  const distance = Math.max(160, Math.floor(el.clientWidth * 0.65))
  el.scrollBy({
    left: direction === 'left' ? -distance : distance,
    behavior: 'smooth',
  })
}

onMounted(() => {
  nextTick(updateScrollState)
  resizeObserver = new ResizeObserver(updateScrollState)
  if (wrapRef.value)
    resizeObserver.observe(wrapRef.value)
  if (scrollRef.value)
    resizeObserver.observe(scrollRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  cancelCloseDropdown()
})

watch(() => props.items, () => nextTick(updateScrollState), { deep: true })
watch(() => props.activeKey, () => {
  activeDropdownKey.value = ''
})
</script>

<style scoped>
.forge-top-menu {
  position: relative;
  min-width: 0;
  width: 100%;
  height: 100%;
}

.forge-top-menu-track {
  display: flex;
  justify-content: flex-start;
  min-width: 0;
  width: 100%;
  height: 100%;
  overflow-x: auto;
  overflow-y: visible;
  scrollbar-width: none;
  scroll-behavior: smooth;
}

.forge-top-menu-track::-webkit-scrollbar {
  display: none;
}

.forge-top-menu-list {
  width: max-content;
  min-width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
}

.forge-top-menu-node {
  position: relative;
  flex: 0 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
}

.forge-top-menu-item {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  position: relative;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  color: var(--top-menu-text-color);
  background: var(--top-menu-bg-color);
  font-size: calc(var(--top-menu-font-size, 14px) * var(--font-scale, 1));
  font-weight: var(--top-menu-font-weight, 500);
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast);
}

.forge-top-menu-item:hover {
  color: var(--top-menu-text-color-hover);
  background: transparent;
}

.forge-top-menu-item:active {
  background: color-mix(in srgb, currentColor 8%, transparent);
}

.forge-top-menu-node.is-active > .forge-top-menu-item {
  color: var(--top-menu-text-color-active);
  background: transparent;
  font-weight: 600;
}

.forge-top-menu-node.is-active > .forge-top-menu-item::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: calc(100% - 18px);
  min-width: 34px;
  height: 2px;
  border-radius: var(--radius-full);
  background: var(--top-menu-active-bar-color, var(--primary-color));
  transform: translateX(-50%);
}

.forge-top-menu-icon,
.forge-top-menu-caret,
.forge-top-menu-dropdown-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: inherit;
}

.forge-top-menu-icon {
  width: 18px;
  height: 18px;
}

.forge-top-menu-icon :deep(.icon-renderer),
.forge-top-menu-icon :deep(.xicon),
.forge-top-menu-icon :deep(svg) {
  width: 16px;
  height: 16px;
  display: block;
}

.forge-top-menu-caret {
  width: 16px;
  height: 16px;
  opacity: 0.8;
  transition: transform var(--transition-fast);
}

.forge-top-menu-node:hover .forge-top-menu-caret,
.forge-top-menu-node.is-dropdown-open .forge-top-menu-caret {
  transform: rotate(180deg);
}

.forge-top-menu-dropdown {
  z-index: 3000;
  min-width: 190px;
  max-width: 320px;
  padding: 6px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  box-shadow: var(--shadow-lg);
  animation: forge-top-menu-dropdown-in var(--transition-fast) ease-out;
}

.forge-top-menu-scroll {
  position: absolute;
  top: 50%;
  z-index: 35;
  width: 28px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 6px;
  color: var(--top-menu-text-color, #fff);
  background: transparent;
  opacity: 0.62;
  cursor: pointer;
  transform: translateY(-50%);
  transition:
    color var(--transition-fast),
    background-color var(--transition-fast),
    opacity var(--transition-fast),
    transform var(--transition-fast);
}

.forge-top-menu-scroll i {
  width: 20px;
  height: 20px;
  font-size: 20px;
}

.forge-top-menu-scroll:hover {
  color: var(--top-menu-text-color-hover, var(--top-menu-text-color, #fff));
  background: color-mix(in srgb, var(--top-menu-text-color, #fff) 14%, transparent);
  opacity: 1;
}

.forge-top-menu-scroll:active {
  transform: translateY(-50%) scale(0.94);
}

.forge-top-menu-scroll:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--top-menu-text-color, #fff) 78%, transparent);
  outline-offset: 1px;
  opacity: 1;
}

.forge-top-menu-scroll.is-left {
  left: 0;
}

.forge-top-menu-scroll.is-right {
  right: 0;
}

.forge-top-menu.can-scroll-left::before,
.forge-top-menu.can-scroll-right::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: 30;
  width: 32px;
  pointer-events: none;
}

.forge-top-menu.can-scroll-left::before {
  left: 0;
  background: linear-gradient(90deg, var(--layout-header-bg-color) 0%, transparent 100%);
}

.forge-top-menu.can-scroll-right::after {
  right: 0;
  background: linear-gradient(270deg, var(--layout-header-bg-color) 0%, transparent 100%);
}

@keyframes forge-top-menu-dropdown-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
