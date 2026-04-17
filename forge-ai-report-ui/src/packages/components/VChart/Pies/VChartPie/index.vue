<template>
  <GoVChart ref="vChartRef" :option="chartConfig.option"> </GoVChart>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { GoVChart } from '@/components/GoVChart'
import { useChartDataFetch } from '@/hooks'
import config from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<config>,
    required: true
  }
})

const { vChartRef } = useChartDataFetch(props.chartConfig, useChartEditStore, (newData: any) => {
  props.chartConfig.option.dataset = newData
})
</script>
