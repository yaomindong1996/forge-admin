<script setup>
import PrintImage from './PrintImage.vue'

defineProps({ node: { type: Object, required: true } })
</script>

<template>
  <div class="print-code">
    <PrintImage :node="{ ...node, alt: `条码 ${node.text}` }" />
  </div>
</template>

<style scoped>
.print-code {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
