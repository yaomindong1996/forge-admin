<template>
  <span class="plugin-illustration" :style="{ width: `${size}px`, height: `${size}px` }" aria-hidden="true">
    <ForgeSymbol :name="pluginPresentation(plugin).symbol" :size="Math.round(size * 0.63)" />
  </span>
</template>

<script setup>
import ForgeSymbol from '@/components/common/ForgeSymbol.vue'
import { pluginPresentation } from '../pluginPresentation'

defineProps({ plugin: { type: Object, default: null }, size: { type: Number, default: 40 } })
</script>

<style scoped>
.plugin-illustration {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-light));
  border-radius: 4px;
  background: color-mix(in srgb, var(--primary-color) 5%, var(--bg-primary));
  color: var(--primary-color);
  box-sizing: border-box;
}
</style>
