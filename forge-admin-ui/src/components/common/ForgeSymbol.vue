<template>
  <svg
    class="forge-symbol" :class="{ 'forge-symbol--multicolor': multicolor }"
    :style="paletteStyle" :data-symbol="symbol" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"
    :width="size" :height="size" aria-hidden="true" focusable="false"
  >
    <path v-for="(path, index) in paths" :key="index" :d="path.d" :stroke="path.stroke" :fill="path.fill" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { forgeSymbolColoredPaths, forgeSymbolPaletteStyle } from './forgeSymbolColors'
import { forgeSymbols, resolveForgeSymbol } from './forgeSymbols'

const props = defineProps({
  name: { type: String, default: 'module' },
  size: { type: [Number, String], default: 20 },
  multicolor: Boolean,
})
const symbol = computed(() => resolveForgeSymbol(props.name))
const paletteStyle = computed(() => props.multicolor ? forgeSymbolPaletteStyle(symbol.value) : undefined)
const paths = computed(() => props.multicolor
  ? forgeSymbolColoredPaths(symbol.value)
  : forgeSymbols[symbol.value].map(d => ({ d })))
</script>

<style scoped>
.forge-symbol {
  display: inline-block;
  flex: none;
  vertical-align: middle;
}

.forge-symbol--multicolor {
  --forge-icon-primary: var(--forge-icon-main-light);
  --forge-icon-secondary: var(--forge-icon-accent-light);
  --forge-icon-primary-fill: color-mix(in srgb, var(--forge-icon-primary) 14%, transparent);
  --forge-icon-secondary-fill: color-mix(in srgb, var(--forge-icon-secondary) 14%, transparent);
}

.dark .forge-symbol--multicolor {
  --forge-icon-primary: var(--forge-icon-main-dark);
  --forge-icon-secondary: var(--forge-icon-accent-dark);
}
</style>
