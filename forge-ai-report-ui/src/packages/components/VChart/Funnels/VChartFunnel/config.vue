<template>
  <!-- vCharts 全局设置 -->
  <VChartGlobalSetting :optionData="optionData"></VChartGlobalSetting>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { VChartGlobalSetting } from '@/components/Pages/VChartItemSetting'
import { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'

defineProps({
  optionData: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  }
})
</script>
