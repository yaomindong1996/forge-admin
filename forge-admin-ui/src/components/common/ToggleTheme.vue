<template>
  <button
    id="toggleTheme"
    class="chrome-icon-button" type="button"
    :title="appStore.isDark ? '切换浅色模式' : '切换深色模式'"
    :aria-label="appStore.isDark ? '切换浅色模式' : '切换深色模式'"
    :aria-pressed="appStore.isDark" @click="appStore.toggleDark()"
  >
    <i :class="appStore.isDark ? 'i-lucide:moon' : 'i-lucide:sun'" />
    <span v-if="withLabel">{{ appStore.isDark ? '切换浅色模式' : '切换深色模式' }}</span>
  </button>
</template>

<script setup>
import { useAppStore } from '@/store'

defineProps({ withLabel: Boolean })
const appStore = useAppStore()
</script>
