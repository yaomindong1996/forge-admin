<template>
  <nav
    class="forge-side-menu"
    :class="{ 'forge-side-menu--collapsed': collapsed }"
    aria-label="主导航"
  >
    <div class="forge-side-menu__scroll">
      <template v-for="item in menuOptions" :key="item.key">
        <SideMenuNode
          :item="item"
          :level="0"
          :active-key="currentActiveKey"
          :expanded-keys="expandedKeys"
          :collapsed="collapsed"
          @select="handleSelect"
        />
      </template>
    </div>
  </nav>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useMenu } from '@/composables'
import { useAppStore } from '@/store'
import SideMenuNode from './SideMenuNode.vue'

const props = defineProps({
  options: {
    type: Array,
    default: undefined,
  },
  activeKeyOverride: {
    type: [String, Number],
    default: '',
  },
  collapsedOverride: { type: Boolean, default: undefined },
  expandAll: Boolean,
})
const emit = defineEmits(['select'])

const appStore = useAppStore()
const collapsed = computed(() => props.collapsedOverride ?? appStore.collapsed)

const { processedMenus, activeKey, handleMenuSelect } = useMenu()
const expandedKeys = ref([])
const menuOptions = computed(() => Array.isArray(props.options) ? props.options : processedMenus.value)
const currentActiveKey = computed(() => props.activeKeyOverride || activeKey.value)

function normalizeKey(key) {
  return key === undefined || key === null ? '' : String(key)
}

function hasChildren(item) {
  return Array.isArray(item?.children) && item.children.length > 0
}

function findAncestorKeys(items, targetKey, ancestors = []) {
  for (const item of items || []) {
    const itemKey = normalizeKey(item.key)
    if (itemKey === targetKey)
      return ancestors
    if (hasChildren(item)) {
      const found = findAncestorKeys(item.children, targetKey, [...ancestors, itemKey])
      if (found)
        return found
    }
  }
  return null
}

function collectParentKeys(items) {
  return items.flatMap(item => hasChildren(item) ? [normalizeKey(item.key), ...collectParentKeys(item.children)] : [])
}

watch(
  [menuOptions, currentActiveKey, () => props.expandAll],
  ([menus, key, expandAll]) => {
    const ancestors = findAncestorKeys(menus, normalizeKey(key)) || []
    if (expandAll) {
      expandedKeys.value = collectParentKeys(menus)
      return
    }
    const merged = new Set([...expandedKeys.value, ...ancestors])
    expandedKeys.value = Array.from(merged)
  },
  { immediate: true },
)

function toggleExpanded(key) {
  const normalizedKey = normalizeKey(key)
  if (!normalizedKey)
    return
  if (expandedKeys.value.includes(normalizedKey)) {
    expandedKeys.value = expandedKeys.value.filter(item => item !== normalizedKey)
    return
  }
  expandedKeys.value = [...expandedKeys.value, normalizedKey]
}

function handleSelect(item) {
  if (hasChildren(item)) {
    if (collapsed.value) {
      handleMenuSelect(item.key, item.path)
      return
    }
    toggleExpanded(item.key)
    return
  }
  handleMenuSelect(item.key, item.path)
  emit('select', item)
}
</script>

<style>
.forge-side-menu {
  height: 100%;
  padding: 8px 0;
  background: transparent;
  color: var(--side-menu-text-color);
}

.forge-side-menu__scroll {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 8px 10px;
}

.forge-side-menu-node + .forge-side-menu-node {
  margin-top: 2px;
}

.forge-side-menu-item {
  width: 100%;
  height: 38px;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  padding: 0 10px 0 calc(12px + var(--menu-level) * 20px);
  border: 0;
  border-radius: 8px;
  color: var(--side-menu-text-color);
  background: transparent;
  font-size: calc(var(--side-menu-font-size, 14px) * var(--font-scale, 1));
  font-weight: var(--side-menu-font-weight, 400);
  line-height: 1;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  transform: translateX(0);
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast),
    box-shadow var(--transition-fast),
    transform var(--transition-fast);
}

.forge-side-menu-item::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: currentColor;
  opacity: 0;
  pointer-events: none;
  transform: scaleX(0.6);
  transition:
    opacity var(--transition-fast),
    transform var(--transition-fast);
  transform-origin: left center;
}

.forge-side-menu-item:hover {
  color: var(--side-menu-text-color-hover);
  background: var(--side-menu-bg-color-hover);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--side-menu-text-color-hover) 8%, transparent);
  transform: translateX(1px);
}

.forge-side-menu-item:hover::after {
  opacity: 0.032;
  transform: scaleX(1);
}

.forge-side-menu-item:active {
  transform: translateX(0.5px) scale(0.992);
}

.forge-side-menu-item:active::after {
  opacity: 0.09;
}

.forge-side-menu-node.is-active > .forge-side-menu-item {
  color: var(--side-menu-text-color-active);
  background: var(--side-menu-bg-color-active);
  font-weight: 600;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--side-menu-text-color-active) 10%, transparent);
  transform: none;
}

.forge-side-menu-node.is-active > .forge-side-menu-item:hover {
  color: var(--side-menu-text-color-active);
  background: var(--side-menu-bg-color-active);
}

.forge-side-menu-node.is-child-active:not(.is-active) > .forge-side-menu-item:not(:hover) {
  color: var(--side-menu-parent-text-color-active);
  background: var(--side-menu-parent-bg-color-active);
  font-weight: 600;
}

.forge-side-menu-node.is-active > .forge-side-menu-item::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 50%;
  width: 3px;
  height: 18px;
  border-radius: var(--radius-full);
  background: var(--side-menu-text-color-active);
  transform: translateY(-50%);
}

.forge-side-menu-icon {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 22px;
  color: var(--side-menu-icon-color);
  font-size: 18px;
  transform: scale(1);
  transition:
    color var(--transition-fast),
    transform var(--transition-fast);
}

.forge-side-menu-item:hover .forge-side-menu-icon,
.forge-side-menu-node.is-child-active > .forge-side-menu-item .forge-side-menu-icon {
  color: var(--side-menu-text-color-hover);
}

.forge-side-menu-item:hover .forge-side-menu-icon {
  transform: scale(1.04);
}

.forge-side-menu-node.is-child-active:not(.is-active) > .forge-side-menu-item:not(:hover) .forge-side-menu-icon {
  color: var(--side-menu-parent-text-color-active);
}

.forge-side-menu-node.is-child-active:not(.is-active) > .forge-side-menu-item:not(:hover) .forge-side-menu-arrow {
  color: var(--side-menu-parent-text-color-active);
  opacity: 1;
}

.forge-side-menu-node.is-active > .forge-side-menu-item .forge-side-menu-icon {
  color: var(--side-menu-icon-color-active);
  transform: scale(1.03);
}

.forge-side-menu-icon .icon-renderer,
.forge-side-menu-icon .xicon,
.forge-side-menu-icon svg {
  width: 18px;
  height: 18px;
  display: block;
}

.forge-side-menu-label {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: inherit;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.forge-side-menu-arrow {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 22px;
  font-size: 18px;
  color: inherit;
  opacity: 0.72;
  transition:
    transform var(--transition-fast),
    opacity var(--transition-fast);
}

.forge-side-menu-node.is-expanded > .forge-side-menu-item .forge-side-menu-arrow {
  opacity: 1;
  transform: rotate(180deg);
}

.forge-side-menu-children {
  margin: 2px 0 4px;
}

.forge-side-menu--collapsed .forge-side-menu__scroll {
  padding: 0 8px 10px;
}

.forge-side-menu--collapsed .forge-side-menu-item {
  width: 40px;
  height: 40px;
  justify-content: center;
  margin: 2px auto;
  padding: 0;
}

.forge-side-menu--collapsed .forge-side-menu-item:hover,
.forge-side-menu--collapsed .forge-side-menu-node.is-active > .forge-side-menu-item {
  transform: scale(1.015);
}

.forge-side-menu--collapsed .forge-side-menu-item:active {
  transform: scale(0.985);
}

.forge-side-menu--collapsed .forge-side-menu-label,
.forge-side-menu--collapsed .forge-side-menu-arrow,
.forge-side-menu--collapsed .forge-side-menu-node.is-active > .forge-side-menu-item::before {
  display: none;
}

.forge-side-menu__scroll::-webkit-scrollbar {
  width: 4px;
}

.forge-side-menu__scroll::-webkit-scrollbar-track {
  background: transparent;
}

.forge-side-menu__scroll::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: var(--radius-full);
}

.forge-side-menu__scroll::-webkit-scrollbar-thumb:hover {
  background: var(--border-default);
}

.forge-side-menu-flyout {
  min-width: 190px;
  max-width: 280px;
  padding: 8px;
  border-radius: 8px;
  background: var(--side-menu-bg-color);
  box-shadow: var(--shadow-lg);
}

.forge-side-menu-popover-content,
.n-popover__content.forge-side-menu-popover-content {
  padding: 0 !important;
  border-radius: 8px !important;
  overflow: hidden !important;
  background: var(--side-menu-bg-color) !important;
}

.forge-side-menu-flyout-title {
  padding: 4px 8px 8px;
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 600;
}

.forge-side-menu-flyout-item {
  width: 100%;
  min-height: 34px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  color: var(--side-menu-text-color);
  background: transparent;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.forge-side-menu-flyout-item:hover {
  color: var(--side-menu-text-color-hover);
  background: var(--side-menu-bg-color-hover);
}

.forge-side-menu-flyout-item.is-active {
  color: var(--side-menu-text-color-active);
  background: var(--side-menu-bg-color-active);
  font-weight: 600;
}

.forge-side-menu-flyout-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 18px;
  color: inherit;
}

.forge-side-menu-flyout-icon .icon-renderer,
.forge-side-menu-flyout-icon .xicon,
.forge-side-menu-flyout-icon svg {
  width: 16px;
  height: 16px;
  display: block;
}

.forge-side-menu-flyout-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
