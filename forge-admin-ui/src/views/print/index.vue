<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PrintCenterWorkspace from '@/components/print/center/PrintCenterWorkspace.vue'
import { printSourceFromQuery } from '@/components/print/management/printRouteContext'
import PrintTemplateList from '@/components/print/management/PrintTemplateList.vue'

const route = useRoute()
const source = computed(() => printSourceFromQuery(route.query))
const applicationId = computed(() => /^[1-9]\d*$/.test(String(route.query.applicationId || '')) ? String(route.query.applicationId) : null)
const legacyApplicationScope = computed(() => Boolean(applicationId.value))
</script>

<template>
  <!-- 低代码应用内嵌入口仍走模板列表；独立打印中心合并为单一工作台。 -->
  <PrintTemplateList
    v-if="legacyApplicationScope"
    :application-id="applicationId"
    :source="source"
  />
  <PrintCenterWorkspace v-else />
</template>
