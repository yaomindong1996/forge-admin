<template>
  <!-- 图例 -->
  <Legends :optionData="optionData"></Legends>
  <Tooltip :optionData="optionData"></Tooltip>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'
import Legends from './Legends.vue'
import Tooltip from './Tooltip.vue'
const props = defineProps({
  optionData: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  }
})
</script>
