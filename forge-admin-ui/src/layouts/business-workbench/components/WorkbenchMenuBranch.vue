<template>
  <ul class="workbench-menu-branch" :class="{ 'is-nested': depth > 0, 'is-deep': depth > 1 }" :style="{ '--menu-depth': depth }">
    <li v-for="item in items" :key="item.key">
      <template v-if="item.children.length">
        <button v-if="item.path" type="button" class="workbench-menu-group" :class="{ 'is-nested': depth > 0, 'is-deep': depth > 1 }" @click="navigate(item)">
          {{ item.label }}
        </button>
        <span v-else class="workbench-menu-group" :class="{ 'is-nested': depth > 0, 'is-deep': depth > 1 }">{{ item.label }}</span>
        <WorkbenchMenuBranch :items="item.children" :depth="depth + 1" />
      </template>
      <button v-else type="button" class="workbench-menu-link" :class="{ 'is-current': String(activeKey) === item.key, 'is-nested': depth > 0, 'is-deep': depth > 1 }" :aria-current="String(activeKey) === item.key ? 'page' : undefined" @click="navigate(item)">
        <IconRenderer v-if="depth === 0 && typeof item.icon === 'string' && item.icon" :icon="item.icon" :size="16" />
        <component :is="item.icon" v-else-if="depth === 0 && item.icon" />
        <span>{{ item.label }}</span>
        <em v-if="item.badgeText" class="menu-count" :class="`tone-${item.badgeTone || 'muted'}`">{{ item.badgeText }}</em>
      </button>
    </li>
  </ul>
</template>

<script setup>
import IconRenderer from '@/components/IconRenderer.vue'
import { useWorkbenchNavigation } from '../useWorkbenchNavigation'

defineProps({
  items: { type: Array, default: () => [] },
  depth: { type: Number, default: 0 },
})
const { activeKey, navigate } = useWorkbenchNavigation()
</script>
