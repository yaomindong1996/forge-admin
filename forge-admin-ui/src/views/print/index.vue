<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { printSourceFromQuery } from '@/components/print/management/printRouteContext'
import PrintTemplateList from '@/components/print/management/PrintTemplateList.vue'

const route = useRoute()
const source = computed(() => printSourceFromQuery(route.query))
const applicationId = computed(() => /^[1-9]\d*$/.test(String(route.query.applicationId || '')) ? String(route.query.applicationId) : null)
</script>

<template>
  <PrintTemplateList :application-id="applicationId" :source="source" />
</template>
