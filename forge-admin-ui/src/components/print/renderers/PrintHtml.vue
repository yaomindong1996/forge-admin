<script setup>
import { computed } from 'vue'
import { printStyle } from './style'

const props = defineProps({ node: { type: Object, required: true } })

/** Strip executable markup so HTML blocks stay display-only. */
function sanitizeHtml(raw) {
  const source = String(raw ?? '')
  return source
    .replace(/<\s*(script|iframe|object|embed|link|meta|base|form)\b[^>]{0,500}>[\s\S]*?<\s*\/\s*\1\s*>/gi, '')
    .replace(/<\s*(script|iframe|object|embed|link|meta|base|form)\b[^>]{0,200}>/gi, '')
    .replace(/\son[a-z]{1,32}\s*=\s*("[^"]{0,500}"|'[^']{0,500}'|[^\s>]{0,200})/gi, '')
    .replace(/(href|src)\s*=\s*("|')\s*javascript:[^"']{0,500}\2/gi, '$1="#"')
}

const html = computed(() => sanitizeHtml(props.node.html ?? props.node.text ?? ''))
</script>

<template>
  <div class="print-html" :style="printStyle(node.style)" v-html="html" />
</template>

<style scoped>
.print-html {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  overflow: hidden;
  word-break: break-word;
}
.print-html :deep(*) {
  max-width: 100%;
}
</style>
