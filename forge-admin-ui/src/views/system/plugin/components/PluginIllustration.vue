<template>
  <span
    class="plugin-illustration"
    :style="{ width: `${size}px`, height: `${size}px` }"
    aria-hidden="true"
  >
    <ForgeSymbol :name="pluginPresentation(plugin).symbol" :size="Math.round(size * 0.56)" />
  </span>
</template>

<script setup>
import ForgeSymbol from '@/components/common/ForgeSymbol.vue'
import { pluginPresentation } from '../pluginPresentation'

defineProps({ plugin: { type: Object, default: null }, size: { type: Number, default: 32 } })
</script>

<style scoped>
/* 统一浅底图标位，避免每卡随机彩色块 */
.plugin-illustration {
  display: grid;
  place-items: center;
  flex: none;
  box-sizing: border-box;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 2px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-secondary, #4e5969);
}
</style>
