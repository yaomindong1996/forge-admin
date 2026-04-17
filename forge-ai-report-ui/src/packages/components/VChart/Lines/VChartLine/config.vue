<template>
  <!-- vCharts 全局设置 -->
  <VChartGlobalSetting :optionData="optionData"></VChartGlobalSetting>
  <Axis :axis="optionData.xAxis"></Axis>
  <Axis :axis="optionData.yAxis"></Axis>
  <!-- 开启百分比 -->
  <CollapseItem name="百分比堆叠">
    <SettingItemBox name="配置" alone>
      <n-space>
        <span>开启百分比堆叠</span>
        <n-switch v-model:value="optionData.percent" size="small"></n-switch>
      </n-space>
    </SettingItemBox>
  </CollapseItem>
  <Line :optionData="optionData"></Line>
  <Label :optionData="optionData" :positionOptions="labelConfig.linePosition"></Label>
  <Point :optionData="optionData"></Point>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { VChartGlobalSetting, Axis, Label, Line, Point } from '@/components/Pages/VChartItemSetting'
import { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'
import { CollapseItem, SettingItemBox } from '@/components/Pages/ChartItemSetting'
import { labelConfig } from '@/packages/chartConfiguration/vcharts/index'

defineProps({
  optionData: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  }
})
</script>
