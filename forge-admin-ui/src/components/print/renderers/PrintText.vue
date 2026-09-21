<script setup>
import { printStyle } from './style'

defineProps({ node: { type: Object, required: true } })
</script>

<template>
  <div :style="{ ...printStyle(node.style), overflow: node.overflow === 'hidden' ? 'hidden' : undefined, alignItems: node.style?.textAlign === 'center' ? 'center' : node.style?.textAlign === 'right' ? 'flex-end' : 'stretch' }">
    <template v-if="node.lines">
      <div v-for="(line, index) in node.lines" :key="index" :style="{ height: `${node.lineHeightMm}mm`, whiteSpace: 'pre', width: '100%' }">
        {{ line || '\u200b' }}
      </div>
    </template>
    <template v-else>
      <span style="width: 100%">{{ node.text }}</span>
    </template>
  </div>
</template>
