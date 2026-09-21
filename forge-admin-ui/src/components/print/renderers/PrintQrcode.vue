<script setup>
import PrintImage from './PrintImage.vue'

defineProps({ node: { type: Object, required: true } })
</script>

<template>
  <div
    class="print-code"
    :style="{
      display: 'flex',
      flexDirection: 'column',
      width: '100%',
      height: '100%',
      overflow: 'hidden',
      boxSizing: 'border-box',
    }"
  >
    <PrintImage
      :node="{ ...node, alt: '二维码', style: { ...node.style, height: node.showCodeText ? 'calc(100% - 12pt)' : '100%' } }"
      :style="{ flex: '1 1 auto', minHeight: 0, width: '100%' }"
    />
    <div
      v-if="node.showCodeText"
      :style="{
        flex: 'none',
        padding: '1px 2px',
        color: '#111',
        fontSize: '8pt',
        lineHeight: 1.2,
        textAlign: 'center',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      }"
    >
      {{ node.text }}
    </div>
  </div>
</template>
