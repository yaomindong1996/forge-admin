<script setup>
import { printStyle } from './style'

defineProps({ node: { type: Object, required: true } })
</script>

<template>
  <div
    v-if="node.src"
    class="print-image"
    :style="{
      ...printStyle(node.style),
      width: '100%',
      height: '100%',
      overflow: 'hidden',
    }"
  >
    <img
      :src="node.src"
      :alt="node.alt || ''"
      :style="{ display: 'block', width: '100%', height: '100%', objectFit: node.style?.objectFit || 'contain' }"
    >
  </div>
</template>
