<template>
  <TopMenuBar
    v-if="navigationStyle === 'top'"
    class="portal-system-top-menu"
    :items="topMenuItems"
    :active-key="currentPageId"
    dropdown
    aria-label="应用页面导航"
    @select="handleTopMenuSelect"
  />
  <nav v-else class="portal-navigation" :class="[`is-${navigationStyle}`, { collapsed }]" aria-label="应用页面导航">
    <PortalNavigationEntry
      v-for="navItem in tree"
      :key="navItem.id"
      :item="navItem"
      :current-page-id="currentPageId"
      :collapsed="collapsed && navigationStyle !== 'top'"
      :collapsed-group-ids="collapsedGroupIds"
      :navigation-style="navigationStyle"
      @select="emit('select', $event)"
      @toggle-group="toggleGroup"
    />
  </nav>
</template>

<script setup>
import { computed, defineComponent, h, ref } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import TopMenuBar from '@/layouts/components/TopMenuBar.vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  currentPageId: { type: String, default: '' },
  navigationStyle: { type: String, default: 'side' },
  collapsed: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])
const collapsedGroupIds = ref(new Set())
const tree = computed(() => buildTree(props.nodes))
const topMenuItems = computed(() => tree.value.map(toSystemMenuItem))

function toggleGroup(groupId) {
  const nextCollapsedGroupIds = new Set(collapsedGroupIds.value)
  const normalizedGroupId = String(groupId)

  if (nextCollapsedGroupIds.has(normalizedGroupId))
    nextCollapsedGroupIds.delete(normalizedGroupId)
  else
    nextCollapsedGroupIds.add(normalizedGroupId)

  collapsedGroupIds.value = nextCollapsedGroupIds
}

function toSystemMenuItem(item) {
  const children = (item.children || []).map(toSystemMenuItem)
  return {
    key: String(item.id),
    label: item.title,
    ...(item.icon ? { icon: item.icon } : {}),
    ...(item.type === 'page' ? { path: `#${item.id}` } : {}),
    ...(children.length ? { children } : {}),
  }
}

function handleTopMenuSelect(item) {
  if (!item?.key || item.children?.length)
    return
  emit('select', String(item.key))
}

const PortalNavigationEntry = defineComponent({
  name: 'PortalNavigationEntry',
  props: {
    item: { type: Object, required: true },
    currentPageId: { type: String, default: '' },
    collapsed: Boolean,
    collapsedGroupIds: { type: Set, default: () => new Set() },
    navigationStyle: { type: String, default: 'side' },
  },
  emits: ['select', 'toggleGroup'],
  setup(itemProps, { emit: itemEmit }) {
    return () => {
      const item = itemProps.item
      if (item.type === 'group') {
        const active = isNavigationItemActive(item, itemProps.currentPageId)
        const groupExpanded = itemProps.navigationStyle === 'top' || !itemProps.collapsedGroupIds.has(String(item.id))
        const titleContent = [
          item.icon ? h(IconRenderer, { icon: item.icon, size: 16 }) : null,
          (!itemProps.collapsed || itemProps.navigationStyle === 'top') ? h('span', item.title) : null,
          itemProps.navigationStyle === 'top' || !itemProps.collapsed
            ? h('span', { 'class': 'portal-nav-group-caret', 'aria-hidden': 'true' }, [h('i', { class: 'i-material-symbols:keyboard-arrow-down-rounded' })])
            : null,
        ]
        const title = itemProps.navigationStyle === 'top'
          ? h('button', {
              'type': 'button',
              'class': ['portal-nav-group-title', { active }],
              'aria-haspopup': 'menu',
            }, titleContent)
          : h('button', {
              'type': 'button',
              'class': ['portal-nav-group-title', { active, collapsed: !groupExpanded }],
              'aria-expanded': String(groupExpanded),
              'title': itemProps.collapsed ? item.title : undefined,
              'onClick': () => itemEmit('toggleGroup', item.id),
            }, titleContent)
        return h('section', { class: ['portal-nav-group', { 'has-children': item.children?.length, 'is-active': active, 'is-group-collapsed': !groupExpanded }] }, [
          title,
          groupExpanded
            ? h('div', { class: 'portal-nav-group-items', role: itemProps.navigationStyle === 'top' ? 'menu' : undefined }, (item.children || []).map(child => h(PortalNavigationEntry, {
                key: child.id,
                item: child,
                currentPageId: itemProps.currentPageId,
                collapsed: itemProps.collapsed,
                collapsedGroupIds: itemProps.collapsedGroupIds,
                navigationStyle: itemProps.navigationStyle,
                onSelect: value => itemEmit('select', value),
                onToggleGroup: value => itemEmit('toggleGroup', value),
              })))
            : null,
        ])
      }

      const active = String(item.id) === String(itemProps.currentPageId)
      return h('button', {
        'type': 'button',
        'class': ['portal-nav-item', { active }],
        'title': itemProps.collapsed ? item.title : undefined,
        'aria-current': active ? 'page' : undefined,
        'onClick': () => itemEmit('select', item.id),
      }, [
        item.icon
          ? h(IconRenderer, { icon: item.icon, size: 17 })
          : null,
        itemProps.collapsed ? null : h('span', item.title),
      ])
    }
  },
})

function isNavigationItemActive(item, currentPageId) {
  if (!item)
    return false
  if (String(item.id) === String(currentPageId))
    return true
  return Array.isArray(item.children) && item.children.some(child => isNavigationItemActive(child, currentPageId))
}

function buildTree(nodes) {
  const visible = (nodes || [])
    .map(normalizeNavigationNode)
    .filter(node => node && (node.navigationVisible ?? node.settings?.navigationVisible) !== false)
  const byParent = new Map()
  visible.forEach((node) => {
    const key = node.parentId == null || node.parentId === '' ? '__root__' : String(node.parentId)
    if (!byParent.has(key))
      byParent.set(key, [])
    byParent.get(key).push(node)
  })
  const visit = (parentKey, visited = new Set()) => (byParent.get(parentKey) || [])
    .sort((left, right) => {
      const systemOrder = Number(Boolean(right.systemView)) - Number(Boolean(left.systemView))
      if (systemOrder !== 0)
        return systemOrder
      return Number(left.sort || 0) - Number(right.sort || 0)
    })
    .filter(node => !visited.has(String(node.id)))
    .map((node) => {
      const nextVisited = new Set(visited).add(String(node.id))
      return { ...node, children: visit(String(node.id), nextVisited) }
    })
    .filter(node => node.type === 'page' || node.children.length)
  return visit('__root__')
}

function normalizeNavigationNode(node) {
  if (!node || typeof node !== 'object')
    return null
  const rawType = String(node.type || node.nodeType || node.kind || '').toLowerCase()
  const type = ['group', 'page-group', 'page_group', 'pagegroup', 'menu-group', 'menu_group', 'directory', 'folder'].includes(rawType) ? 'group' : 'page'
  const parentId = node.parentId ?? node.parentNodeId ?? node.parentID ?? node.settings?.parentId ?? null
  const icon = String(node.icon || '').trim()
  return {
    ...node,
    id: String(node.id || '').trim(),
    type,
    title: String(node.title || node.name || (type === 'group' ? '未命名页面组' : '未命名页面')).trim(),
    // 页面组允许不配置图标，但运行时仍需要一个稳定的目录视觉锚点，避免组标题和页面看起来没有层级关系。
    icon: type === 'group' ? (icon || 'i-material-symbols:folder-open-rounded') : icon,
    parentId: parentId == null || parentId === '' ? null : String(parentId),
  }
}
</script>

<style scoped>
.portal-navigation {
  display: flex;
  min-width: 0;
}

.portal-navigation.is-side,
.portal-navigation.is-collapsed {
  width: 232px;
  flex-direction: column;
  gap: 2px;
  padding: 12px 10px;
}

.portal-navigation.collapsed {
  width: 64px;
}

.portal-navigation.is-top {
  align-items: center;
  gap: 4px;
  overflow: visible;
  padding: 0 8px;
}

.portal-nav-group {
  display: block;
  min-width: 0;
}

.is-side .portal-nav-group,
.is-collapsed .portal-nav-group {
  display: block;
  margin-top: 6px;
}

.is-side > .portal-nav-group:first-child,
.is-collapsed > .portal-nav-group:first-child {
  margin-top: 0;
}

:deep(.portal-nav-group-title) {
  display: flex;
  height: 30px;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
  border-radius: 5px;
  color: var(--portal-text-muted, #8f959e);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.is-side :deep(.portal-nav-group-title),
.is-collapsed :deep(.portal-nav-group-title) {
  display: flex;
  width: 100%;
  height: 32px;
  align-items: center;
  padding: 0 10px;
  border: 0;
  background: color-mix(in srgb, var(--portal-text-muted, #8f959e) 7%, transparent);
  color: var(--portal-text, #4e5969);
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
}

.is-side :deep(.portal-nav-group-title:hover),
.is-collapsed :deep(.portal-nav-group-title:hover) {
  background: color-mix(in srgb, var(--portal-primary, #3370ff) 7%, transparent);
  color: var(--portal-primary, #3370ff);
}

.is-side :deep(.portal-nav-group-title:focus-visible),
.is-collapsed :deep(.portal-nav-group-title:focus-visible) {
  outline: 2px solid color-mix(in srgb, var(--portal-primary, #3370ff) 54%, transparent);
  outline-offset: 2px;
}

.is-side .portal-nav-group.is-active > :deep(.portal-nav-group-title),
.is-collapsed .portal-nav-group.is-active > :deep(.portal-nav-group-title) {
  background: color-mix(in srgb, var(--portal-primary, #3370ff) 9%, transparent);
  color: var(--portal-primary, #3370ff);
  font-weight: 600;
}

:deep(.portal-nav-group-items) {
  display: grid;
  gap: 2px;
  margin: 4px 0 4px 18px;
  padding-left: 12px;
  border-left: 1px solid color-mix(in srgb, var(--portal-text-muted, #8f959e) 30%, transparent);
}

.is-side .portal-nav-group.is-active > :deep(.portal-nav-group-items),
.is-collapsed .portal-nav-group.is-active > :deep(.portal-nav-group-items) {
  border-left-color: color-mix(in srgb, var(--portal-primary, #3370ff) 46%, transparent);
}

.is-side :deep(.portal-nav-group-items > .portal-nav-item),
.is-collapsed :deep(.portal-nav-group-items > .portal-nav-item) {
  position: relative;
}

.is-side :deep(.portal-nav-group-items > .portal-nav-item)::before,
.is-collapsed :deep(.portal-nav-group-items > .portal-nav-item)::before {
  position: absolute;
  top: 50%;
  left: -13px;
  width: 9px;
  border-top: 1px solid color-mix(in srgb, var(--portal-text-muted, #8f959e) 30%, transparent);
  content: '';
}

.is-side :deep(.portal-nav-group-items > .portal-nav-item.active)::before,
.is-collapsed :deep(.portal-nav-group-items > .portal-nav-item.active)::before {
  border-top-color: color-mix(in srgb, var(--portal-primary, #3370ff) 46%, transparent);
}

.portal-navigation.collapsed :deep(.portal-nav-group-title) {
  justify-content: center;
  padding: 0;
}

.portal-navigation.collapsed :deep(.portal-nav-group-items) {
  margin: 2px 0;
  padding-left: 0;
  border-left: 0;
}

.portal-navigation.collapsed :deep(.portal-nav-group-items > .portal-nav-item)::before {
  display: none;
}

.is-top .portal-nav-group {
  position: relative;
  display: block;
  margin-top: 0;
}

.is-top :deep(.portal-nav-group-title) {
  position: relative;
  display: inline-flex;
  width: auto;
  height: 38px;
  align-items: center;
  gap: 7px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--portal-text-muted, #4e5969);
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
}

.is-top :deep(.portal-nav-group-items) {
  position: absolute;
  z-index: 40;
  top: calc(100% + 4px);
  left: 0;
  display: none;
  min-width: 190px;
  max-width: 300px;
  margin: 0;
  padding: 6px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: var(--portal-surface, #fff);
  box-shadow: 0 10px 28px rgb(31 35 41 / 14%);
}

.is-top .portal-nav-group:hover > :deep(.portal-nav-group-items),
.is-top .portal-nav-group:focus-within > :deep(.portal-nav-group-items) {
  display: grid;
  gap: 2px;
}

.is-top :deep(.portal-nav-group-title:hover),
.is-top :deep(.portal-nav-group-title.active),
.is-top .portal-nav-group.is-active > :deep(.portal-nav-group-title) {
  color: var(--portal-primary, #3370ff);
}

.is-top :deep(.portal-nav-group-title.active)::after,
.is-top .portal-nav-group.is-active > :deep(.portal-nav-group-title)::after {
  position: absolute;
  right: 12px;
  bottom: 2px;
  left: 12px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
  content: '';
}

.is-top :deep(.portal-nav-group-items > .portal-nav-group) {
  width: 100%;
}

.is-top :deep(.portal-nav-group-items > .portal-nav-group > .portal-nav-group-title) {
  display: flex;
  width: 100%;
  min-height: 36px;
  height: 36px;
  justify-content: flex-start;
  padding: 0 11px;
  border-radius: 6px;
  color: var(--portal-text-muted, #4e5969);
  font-size: 13px;
  text-align: left;
}

.is-top :deep(.portal-nav-group-items > .portal-nav-group > .portal-nav-group-title)::after {
  display: none;
}

.is-top :deep(.portal-nav-group-items > .portal-nav-group > .portal-nav-group-title:hover),
.is-top :deep(.portal-nav-group-items > .portal-nav-group > .portal-nav-group-title.active) {
  background: color-mix(in srgb, var(--portal-primary, #3370ff) 10%, transparent);
}

.is-top :deep(.portal-nav-group-items > .portal-nav-group > .portal-nav-group-items) {
  top: -7px;
  left: calc(100% + 6px);
}

:deep(.portal-nav-group-caret) {
  display: inline-flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  margin-left: 1px;
  opacity: 0.72;
  font-size: 16px;
  transition: transform 0.16s ease;
}

.is-side :deep(.portal-nav-group-title .portal-nav-group-caret),
.is-collapsed :deep(.portal-nav-group-title .portal-nav-group-caret) {
  margin-left: auto;
}

.is-side .portal-nav-group.is-group-collapsed > :deep(.portal-nav-group-title .portal-nav-group-caret),
.is-collapsed .portal-nav-group.is-group-collapsed > :deep(.portal-nav-group-title .portal-nav-group-caret) {
  transform: rotate(-90deg);
}

.is-top .portal-nav-group:hover > :deep(.portal-nav-group-title .portal-nav-group-caret),
.is-top .portal-nav-group:focus-within > :deep(.portal-nav-group-title .portal-nav-group-caret) {
  transform: rotate(180deg);
}

:deep(.portal-nav-item) {
  display: flex;
  height: 36px;
  min-width: 0;
  align-items: center;
  gap: 9px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--portal-text-muted, #4e5969);
  font: inherit;
  padding: 0 10px;
  text-align: left;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.is-top :deep(.portal-nav-item) {
  height: 34px;
  padding: 0 10px;
}

.is-top :deep(.portal-nav-group-items .portal-nav-item) {
  width: 100%;
  height: 36px;
  padding: 0 11px;
  border-radius: 6px;
}

.collapsed :deep(.portal-nav-item) {
  width: 42px;
  justify-content: center;
  padding: 0;
}

:deep(.portal-nav-item:hover) {
  background: color-mix(in srgb, var(--portal-primary, #3370ff) 6%, transparent);
  color: var(--portal-primary, #3370ff);
}

:deep(.portal-nav-item:focus-visible) {
  outline: 2px solid color-mix(in srgb, var(--portal-primary, #3370ff) 54%, transparent);
  outline-offset: 2px;
}

:deep(.portal-nav-item.active) {
  background: color-mix(in srgb, var(--portal-primary, #3370ff) 10%, transparent);
  color: var(--portal-primary, #3370ff);
  font-weight: 500;
}

.is-top :deep(.portal-nav-item.active) {
  position: relative;
  background: transparent;
}

.is-top :deep(.portal-nav-item.active::after) {
  position: absolute;
  right: 12px;
  bottom: 2px;
  left: 12px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
  content: '';
}

.is-top :deep(.portal-nav-group-items .portal-nav-item.active) {
  background: color-mix(in srgb, var(--portal-primary, #3370ff) 10%, transparent);
}

.is-top :deep(.portal-nav-group-items .portal-nav-item.active)::after {
  display: none;
}

@media (max-width: 768px) {
  .portal-navigation.is-side,
  .portal-navigation.is-collapsed,
  .portal-navigation.collapsed {
    width: 100%;
    flex-direction: row;
    gap: 4px;
    overflow-x: auto;
    padding: 8px 12px;
  }

  .portal-navigation.is-side .portal-nav-group,
  .portal-navigation.is-collapsed .portal-nav-group {
    display: flex;
    align-items: center;
    gap: 2px;
    margin-top: 0;
  }

  .portal-navigation.is-side :deep(.portal-nav-group-title),
  .portal-navigation.is-collapsed :deep(.portal-nav-group-title) {
    height: 34px;
    flex: 0 0 auto;
    padding: 0 8px;
    border-radius: 6px;
    background: color-mix(in srgb, var(--portal-primary, #3370ff) 7%, transparent);
  }

  .portal-navigation.is-side :deep(.portal-nav-group-items),
  .portal-navigation.is-collapsed :deep(.portal-nav-group-items) {
    display: flex;
    margin-left: 0;
    padding-left: 0;
    border-left: 0;
  }

  .collapsed :deep(.portal-nav-item) {
    width: auto;
    justify-content: flex-start;
    padding: 0 12px;
  }
}
</style>
