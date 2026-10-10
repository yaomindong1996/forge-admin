<template>
  <svg
    class="forge-symbol" :data-symbol="symbol" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"
    :width="size" :height="size" aria-hidden="true" focusable="false"
  >
    <path v-for="(path, index) in forgeSymbols[symbol]" :key="index" :d="path" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { forgeSymbols, resolveForgeSymbol } from './forgeSymbols'

const props = defineProps({ name: { type: String, default: 'module' }, size: { type: [Number, String], default: 20 } })
const symbol = computed(() => resolveForgeSymbol(props.name))
</script>

<style scoped>
.forge-symbol {
  display: inline-block;
  flex: none;
  vertical-align: middle;
}
</style>
