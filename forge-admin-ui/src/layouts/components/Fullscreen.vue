<template>
  <i
    id="fullscreen"
    class="mr-16 cursor-pointer"
    :class="isFullscreen ? 'ai-icon:minimize' : 'ai-icon:maximize'"
    @click="toggle"
  />
</template>

<script setup>
import { useFullscreen } from '@vueuse/core'

const { isFullscreen, toggle } = useFullscreen()
</script>
