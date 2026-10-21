<template>
  <span
    class="plugin-illustration"
    :style="{ width: `${size}px`, height: `${size}px` }"
    aria-hidden="true"
  >
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
  display: grid;
  place-items: center;
  flex: none;
  border-radius: 4px;
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--plugin-wash, #dbeafe) 40%, #fff),
    var(--plugin-wash, #dbeafe)
  );
  color: var(--plugin-ink, #2563eb);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--plugin-ink, #2563eb) 12%, transparent);
  box-sizing: border-box;
}
</style>
