<template>
  <!-- Echarts 全局设置 -->
  <global-setting :optionData="optionData"></global-setting>
  <CollapseItem name="饼图配置" :expanded="true">
    <SettingItemBox name="类型">
      <SettingItem>
        <n-select v-model:value="optionData.type" size="small" :options="fontWeightOptions" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="动画" :alone="true">
      <SettingItem>
        <n-space>
          <n-switch v-model:value="optionData.isCarousel" size="small"></n-switch>
          <n-text>开启<n-text :depth="3">（将自动隐藏图例）</n-text></n-text>
        </n-space>
      </SettingItem>
      <SettingItem>
        <n-text :depth="3">无鼠标点击图例场景时，可强行打开图例</n-text>
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="图形">
      <setting-item name="内圈范围">
        <n-input v-model:value="optionData.series[0].radius[0]" size="small"></n-input>
      </setting-item>
      <setting-item name="外圈范围">
        <n-input v-model:value="optionData.series[0].radius[1]" size="small"></n-input>
      </setting-item>
      <setting-item name="X轴中心">
        <n-input v-model:value="optionData.series[0].center[0]" size="small"></n-input>
      </setting-item>
      <setting-item name="Y轴中心">
        <n-input v-model:value="optionData.series[0].center[1]" size="small"></n-input>
      </setting-item>
    </SettingItemBox>
    <SettingItemBox name="标签">
      <SettingItem>
        <n-space>
          <n-switch v-model:value="optionData.series[0].label.show" size="small"></n-switch>
          <n-text>展示标签</n-text>
        </n-space>
      </SettingItem>
      <setting-item>
        <n-space>
          <n-switch v-model:value="optionData.series[0].labelLine.show" size="small"></n-switch>
          <n-text>引导线</n-text>
        </n-space>
      </setting-item>
      <SettingItem name="位置">
        <n-select v-model:value="optionData.series[0].label.position" size="small" :options="labelConfig.position" />
      </SettingItem>
      <setting-item name="展示类型">
        <n-select v-model:value="optionData.series[0].label.formatter" size="small" :options="labelFormatterOptions" />
      </setting-item>
    </SettingItemBox>
    <setting-item-box name="字体">
      <setting-item name="大小">
        <n-input-number v-model:value="optionData.series[0].label.fontSize" size="small" :min="0"></n-input-number>
      </setting-item>
      <setting-item name="颜色" v-if="optionData.series[0].label.color">
        <n-color-picker size="small" :modes="['hex']" v-model:value="optionData.series[0].label.color"></n-color-picker>
      </setting-item>
      <SettingItem name="文字加粗" v-if="optionData.series[0].label.fontWeight">
        <n-select
          v-model:value="optionData.series[0].label.fontWeight"
          size="small"
          :options="labelConfig.fontWeight"
        />
      </SettingItem>
      <setting-item name="文字边框大小" v-if="optionData.series[0].label.textBorderWidth > -1">
        <n-input-number
          v-model:value="optionData.series[0].label.textBorderWidth"
          size="small"
          :min="0"
        ></n-input-number>
      </setting-item>
      <setting-item name="文字边框色" v-if="optionData.series[0].label.textBorderColor">
        <n-color-picker
          size="small"
          :modes="['hex']"
          v-model:value="optionData.series[0].label.textBorderColor"
        ></n-color-picker>
      </setting-item>
    </setting-item-box>
    <setting-item-box name="分段样式">
      <setting-item name="圆角大小">
        <n-input-number
          v-model:value="optionData.series[0].itemStyle.borderRadius"
          size="small"
          :min="0"
        ></n-input-number>
      </setting-item>
      <setting-item name="线条宽度">
        <n-input-number
          v-model:value="optionData.series[0].itemStyle.borderWidth"
          size="small"
          :min="0"
        ></n-input-number>
      </setting-item>
    </setting-item-box>
    <setting-item-box name="鼠标聚焦">
      <setting-item name="文字大小">
        <n-input-number
          v-model:value="optionData.series[0].emphasis.label.fontSize"
          size="small"
          :min="0"
        ></n-input-number>
      </setting-item>
      <SettingItem name="文字加粗" v-if="optionData.series[0].emphasis.label.fontWeight">
        <n-select
          v-model:value="optionData.series[0].emphasis.label.fontWeight"
          size="small"
          :options="labelConfig.fontWeight"
        />
      </SettingItem>
    </setting-item-box>
  </CollapseItem>
</template>

<script setup lang="ts">
import { PropType, watch } from 'vue'
import { GlobalThemeJsonType } from '@/settings/chartThemes/index'
import { GlobalSetting, CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { PieTypeObject, PieTypeEnum } from './config'
import { labelConfig } from '@/packages/chartConfiguration/echarts'

const props = defineProps({
  optionData: {
    type: Object as PropType<GlobalThemeJsonType>,
    required: true
  }
})
const fontWeightOptions = [
  {
    label: PieTypeEnum.NORMAL,
    value: PieTypeObject[PieTypeEnum.NORMAL]
  },
  {
    label: PieTypeEnum.RING,
    value: PieTypeObject[PieTypeEnum.RING]
  },
  {
    label: PieTypeEnum.ROSE,
    value: PieTypeObject[PieTypeEnum.ROSE]
  }
]

const labelFormatterOptions = [
  { label: '数据名', value: '{b}' },
  { label: '百分比', value: '{d}' },
  { label: '列名:百分比', value: '{b}:{d}%' }
]
</script>
