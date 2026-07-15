<template>
  <CrudPage
    :runtime-config="runtimeConfig"
    :sync-document-title="syncDocumentTitle"
  />
</template>

<script setup>
import CrudPage from '@/views/ai/crud-page.vue'

defineOptions({ name: 'LowcodeRuntimePage' })

defineProps({
  runtimeConfig: {
    type: Object,
    required: true,
  },
  syncDocumentTitle: {
    type: Boolean,
    default: false,
  },
})
</script>
