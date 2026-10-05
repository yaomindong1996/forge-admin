<template>
  <button class="chrome-icon-button" type="button" title="操作指引" aria-label="操作指引" @click="start">
    <i class="i-lucide:book-open" />
    <span v-if="withLabel">操作指引</span>
  </button>
</template>

<script setup>
import { useAppStore } from '@/store'

defineProps({ withLabel: Boolean })
const emit = defineEmits(['start'])
const appStore = useAppStore()
function start() {
  // 指引由 App 持有；先关闭工具浮层，避免浮层卸载时连同指引一起消失。
  emit('start')
  appStore.guideOpen = true
}
</script>
