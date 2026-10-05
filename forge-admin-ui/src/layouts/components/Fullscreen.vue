<template>
  <button
    id="fullscreen"
    class="chrome-icon-button" type="button"
    :title="isFullscreen ? '退出全屏' : '进入全屏'"
    :aria-label="isFullscreen ? '退出全屏' : '进入全屏'" :aria-pressed="isFullscreen"
    @click="toggle"
  >
    <i :class="isFullscreen ? 'i-lucide:minimize' : 'i-lucide:maximize'" />
    <span v-if="withLabel">{{ isFullscreen ? '退出全屏' : '进入全屏' }}</span>
  </button>
</template>

<script setup>
import { useFullscreen } from '@vueuse/core'

defineProps({ withLabel: Boolean })
const { isFullscreen, toggle } = useFullscreen()
</script>
