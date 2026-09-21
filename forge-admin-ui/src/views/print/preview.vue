<script setup>
import { NEmpty, useThemeVars } from 'naive-ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { leavePrintPreview, printRecordFromQuery } from '@/components/print/management/printRouteContext'
import PrintTemplatePicker from '@/components/print/runtime/PrintTemplatePicker.vue'

const route = useRoute()
const router = useRouter()
const theme = useThemeVars()
const themeStyle = computed(() => ({ background: theme.value.bodyColor, color: theme.value.textColor1 }))
const record = computed(() => printRecordFromQuery(route.query))
function goBack() {
  leavePrintPreview({ router, record: record.value })
}
</script>

<template>
  <main class="print-runtime-page" :style="themeStyle">
    <PrintTemplatePicker v-if="record" :record="record" @back="goBack" />
    <div v-else class="runtime-empty">
      <NEmpty description="请从已保存单据的打印入口进入" />
    </div>
  </main>
</template>

<style scoped>
.print-runtime-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.runtime-empty {
  flex: 1;
  display: grid;
  place-items: center;
}
</style>
