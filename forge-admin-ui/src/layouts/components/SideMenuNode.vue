<template>
  <div
    class="forge-side-menu-node"
    :class="[
      `forge-side-menu-node--level-${level}`,
      {
        'is-active': isSelfActive,
        'is-child-active': isChildActive,
        'is-expanded': isExpanded,
        'has-children': hasChildItems,
      },
    ]"
  >
    <NPopover
      v-if="hasChildItems && collapsed"
      v-model:show="flyoutVisible"
      trigger="click"
      placement="right-start"
      :show-arrow="false"
      :overlap="false"
      style="padding: 0"
      content-class="forge-side-menu-popover-content"
      content-style="padding: 0; border-radius: 8px; overflow: hidden;"
    >
      <template #trigger>
        <button
          class="forge-side-menu-item"
          type="button"
          :title="collapsedTitle"
          :style="itemStyle"
          :aria-current="isSelfActive ? 'page' : undefined"
          aria-haspopup="menu"
        >
          <span class="forge-side-menu-icon">
            <component :is="item.icon" v-if="item.icon" />
            <span
              v-if="badgeText && collapsed"
              class="forge-side-menu-badge is-corner"
              :class="`tone-${badgeTone}`"
            >{{ badgeText }}</span>
          </span>
          <span class="forge-side-menu-label">{{ item.label }}</span>
          <span
            v-if="badgeText && !collapsed"
            class="forge-side-menu-badge"
            :class="`tone-${badgeTone}`"
          >{{ badgeText }}</span>
          <span class="forge-side-menu-arrow">
            <i class="i-material-symbols:keyboard-arrow-down-rounded" />
          </span>
        </button>
      </template>

      <div class="forge-side-menu-flyout" role="menu">
        <div class="forge-side-menu-flyout-title">
          {{ item.label }}
        </div>
        <button
          v-for="child in flyoutItems"
          :key="child.key"
          class="forge-side-menu-flyout-item"
          :class="{ 'is-active': normalizeKey(child.key) === normalizeKey(activeKey) }"
          type="button"
          role="menuitem"
          @click="handleFlyoutSelect(child)"
        >
          <span v-if="child.icon" class="forge-side-menu-flyout-icon">
            <component :is="child.icon" />
          </span>
          <span class="forge-side-menu-flyout-label">{{ child.flyoutLabel || child.label }}</span>
          <span
            v-if="child.badgeText"
            class="forge-side-menu-badge"
            :class="`tone-${child.badgeTone || 'muted'}`"
          >{{ child.badgeText }}</span>
        </button>
      </div>
    </NPopover>

    <button
      v-else
      class="forge-side-menu-item"
      type="button"
      :title="collapsedTitle"
      :style="itemStyle"
      :aria-current="isSelfActive ? 'page' : undefined"
      :aria-expanded="hasChildItems ? String(isExpanded) : undefined"
      @click="$emit('select', item)"
    >
      <span class="forge-side-menu-icon">
        <component :is="item.icon" v-if="item.icon" />
        <span
          v-if="badgeText && collapsed"
          class="forge-side-menu-badge is-corner"
          :class="`tone-${badgeTone}`"
        >{{ badgeText }}</span>
      </span>
      <span class="forge-side-menu-label">{{ item.label }}</span>
      <span
        v-if="badgeText && !collapsed"
        class="forge-side-menu-badge"
        :class="`tone-${badgeTone}`"
      >{{ badgeText }}</span>
      <span v-if="hasChildItems" class="forge-side-menu-arrow">
        <i class="i-material-symbols:keyboard-arrow-down-rounded" />
      </span>
    </button>

    <div v-if="hasChildItems && !collapsed" v-show="isExpanded" class="forge-side-menu-children">
      <SideMenuNode
        v-for="child in children"
        :key="child.key"
        :item="child"
        :level="level + 1"
        :active-key="activeKey"
        :expanded-keys="expandedKeys"
        :collapsed="collapsed"
        @select="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { NPopover } from 'naive-ui'
import { computed, ref } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  level: {
    type: Number,
    required: true,
  },
  activeKey: {
    type: [String, Number],
    default: '',
  },
  expandedKeys: {
    type: Array,
    default: () => [],
  },
  collapsed: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select'])
const flyoutVisible = ref(false)

function normalizeKey(key) {
  return key === undefined || key === null ? '' : String(key)
}

const children = computed(() => props.item.children || [])
const itemKey = computed(() => normalizeKey(props.item.key))
const hasChildItems = computed(() => children.value.length > 0)
const isExpanded = computed(() => props.expandedKeys.includes(itemKey.value))
const isSelfActive = computed(() => itemKey.value === normalizeKey(props.activeKey))
const badgeText = computed(() => props.item.badgeText || '')
const badgeTone = computed(() => props.item.badgeTone || 'muted')
const collapsedTitle = computed(() => {
  if (!props.collapsed)
    return undefined
  return badgeText.value ? `${props.item.label}（${badgeText.value}）` : props.item.label
})
const itemStyle = computed(() => ({
  '--menu-level': props.level,
}))
const isChildActive = computed(() => {
  const visit = (items) => {
    return items.some((item) => {
      if (normalizeKey(item.key) === normalizeKey(props.activeKey))
        return true
      return Array.isArray(item.children) && visit(item.children)
    })
  }

  return hasChildItems.value && visit(children.value)
})

const flyoutItems = computed(() => collectFlyoutItems(children.value))

function collectFlyoutItems(items, parentLabels = []) {
  return (items || []).flatMap((child) => {
    const label = child.label || child.name || ''
    const childItems = Array.isArray(child.children) ? child.children : []
    const item = {
      ...child,
      flyoutLabel: parentLabels.length ? `${parentLabels.join(' / ')} / ${label}` : label,
    }

    if (childItems.length) {
      const nested = collectFlyoutItems(childItems, [...parentLabels, label])
      return child.path ? [item, ...nested] : nested
    }

    return [item]
  })
}

function handleFlyoutSelect(child) {
  flyoutVisible.value = false
  emit('select', child)
}
</script>
