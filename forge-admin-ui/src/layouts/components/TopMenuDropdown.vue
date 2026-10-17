<template>
  <div class="forge-top-menu-branch">
    <div
      v-for="item in items"
      :key="item.key"
      class="forge-top-menu-branch-item"
      :class="{ 'has-children': hasChildren(item), 'is-active': isItemActive(item) }"
    >
      <button
        v-if="hasChildren(item)"
        class="forge-top-menu-dropdown-item forge-top-menu-dropdown-group"
        type="button"
        role="menuitem"
        :aria-current="isItemActive(item) ? 'page' : undefined"
        aria-haspopup="menu"
        @click="selectItem(item)"
      >
        <span v-if="item.icon" class="forge-top-menu-dropdown-icon">
          <IconRenderer v-if="typeof item.icon === 'string'" :icon="item.icon" :size="16" />
          <component :is="item.icon" v-else />
        </span>
        <span class="forge-top-menu-dropdown-label">{{ item.label }}</span>
        <span
          v-if="item.badgeText"
          class="forge-top-menu-badge"
          :class="`tone-${item.badgeTone || 'muted'}`"
        >{{ item.badgeText }}</span>
        <i class="forge-top-menu-dropdown-arrow i-material-symbols:chevron-right-rounded" aria-hidden="true" />
      </button>

      <button
        v-else
        class="forge-top-menu-dropdown-item"
        type="button"
        role="menuitem"
        :aria-current="isItemActive(item) ? 'page' : undefined"
        @click="selectItem(item)"
      >
        <span v-if="item.icon" class="forge-top-menu-dropdown-icon">
          <IconRenderer v-if="typeof item.icon === 'string'" :icon="item.icon" :size="16" />
          <component :is="item.icon" v-else />
        </span>
        <span class="forge-top-menu-dropdown-label">{{ item.label }}</span>
        <span
          v-if="item.badgeText"
          class="forge-top-menu-badge"
          :class="`tone-${item.badgeTone || 'muted'}`"
        >{{ item.badgeText }}</span>
      </button>

      <div v-if="hasChildren(item)" class="forge-top-menu-submenu" role="menu">
        <TopMenuDropdown
          :items="item.children"
          :active-key="activeKey"
          @select="selectItem"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import IconRenderer from '@/components/IconRenderer.vue'

defineOptions({ name: 'TopMenuDropdown' })

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  activeKey: {
    type: [String, Number],
    default: '',
  },
})

const emit = defineEmits(['select'])

function normalizeKey(key) {
  return key === undefined || key === null ? '' : String(key)
}

function hasChildren(item) {
  return Array.isArray(item?.children) && item.children.length > 0
}

function isItemActive(item) {
  const activeKey = normalizeKey(props.activeKey)
  if (!activeKey) {
    return false
  }
  if (normalizeKey(item.key) === activeKey) {
    return true
  }
  return hasChildren(item) && item.children.some(child => isItemActive(child))
}

function selectItem(item) {
  // 没有路径的目录只是分组入口，点击时由 hover/focus 展开，不触发无效路由跳转。
  if (!item?.path) {
    return
  }
  emit('select', item)
}
</script>

<style scoped>
.forge-top-menu-branch {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 100%;
}

.forge-top-menu-branch-item {
  position: relative;
  min-width: 188px;
}

.forge-top-menu-dropdown-item {
  width: 100%;
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 11px;
  border: 0;
  border-radius: 6px;
  color: var(--text-secondary);
  background: transparent;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  transition:
    background-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);
}

.forge-top-menu-dropdown-item:hover,
.forge-top-menu-branch-item.is-active > .forge-top-menu-dropdown-item {
  color: var(--side-menu-text-color-active);
  background: var(--side-menu-bg-color-active);
}

.forge-top-menu-dropdown-item:hover {
  transform: translateX(3px);
}

.forge-top-menu-dropdown-item:active {
  transform: translateX(1px) scale(0.985);
}

.forge-top-menu-dropdown-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.forge-top-menu-dropdown-arrow {
  width: 16px;
  height: 16px;
  margin-left: auto;
  flex-shrink: 0;
  color: var(--text-tertiary);
  transition:
    color var(--transition-fast),
    transform var(--transition-fast);
}

.forge-top-menu-branch-item:hover > .forge-top-menu-dropdown-item .forge-top-menu-dropdown-arrow,
.forge-top-menu-branch-item:focus-within > .forge-top-menu-dropdown-item .forge-top-menu-dropdown-arrow {
  color: var(--side-menu-text-color-active);
  transform: translateX(2px);
}

.forge-top-menu-dropdown-label {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.forge-top-menu-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  flex: 0 0 auto;
  margin-left: 4px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  color: #fff;
  background: var(--error-color, #d03050);
}

.forge-top-menu-badge.tone-muted {
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--text-secondary) 16%, transparent);
}

.forge-top-menu-dropdown-group .forge-top-menu-badge {
  margin-right: 2px;
}

.forge-top-menu-submenu {
  position: absolute;
  top: -7px;
  left: calc(100% + 6px);
  z-index: 1;
  display: none;
  min-width: 188px;
  max-width: 300px;
  padding: 6px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  box-shadow: var(--shadow-lg);
}

.forge-top-menu-branch-item:hover > .forge-top-menu-submenu,
.forge-top-menu-branch-item:focus-within > .forge-top-menu-submenu {
  display: block;
}

@media (max-width: 800px) {
  .forge-top-menu-branch-item {
    min-width: 170px;
  }

  .forge-top-menu-submenu {
    position: static;
    display: block;
    max-width: none;
    margin: 0 4px 4px 12px;
    padding: 2px 0 2px 8px;
    border: 0;
    border-left: 1px solid var(--border-light);
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
