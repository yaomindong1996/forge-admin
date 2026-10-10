<template>
  <span class="plugin-illustration" :style="artworkStyle" aria-hidden="true" />
</template>

<script setup>
import { computed } from 'vue'
import atlas from '@/assets/illustrations/plugins/plugin-atlas.png'
import { pluginPresentation } from '../pluginPresentation'

const props = defineProps({
  plugin: { type: Object, default: null },
  size: { type: Number, default: 88 },
})
const artworkStyle = computed(() => {
  const index = pluginPresentation(props.plugin).artwork
  return {
    width: `${props.size}px`,
    height: `${props.size}px`,
    backgroundImage: `url(${atlas})`,
    backgroundPosition: `${(index % 4) * 100 / 3}% ${Math.floor(index / 4) * 100 / 3}%`,
  }
})
</script>

<style scoped>
.plugin-illustration {
  display: inline-block;
  flex: none;
  background-repeat: no-repeat;
  background-size: 400% 400%;
}
</style>
