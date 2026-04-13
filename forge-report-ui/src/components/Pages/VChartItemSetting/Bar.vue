<template>
  <template v-if="optionData.bar">
    <collapse-item name="柱体">
      <SettingItemBox name="样式">
        <setting-item v-if="'width' in optionData.bar.style" name="宽度">
          <n-input-number v-model:value="optionData.bar.style.width" size="small" :min="1"></n-input-number>
        </setting-item>
        <setting-item v-if="'height' in (optionData.bar.style as any)" name="高度">
          <n-input-number v-model:value="(optionData.bar.style as any).height" size="small" :min="1"></n-input-number>
        </setting-item>
        <setting-item name="圆角大小">
          <n-input-number v-model:value="optionData.bar.style.cornerRadius" size="small" :min="0"></n-input-number>
        </setting-item>
        <setting-item name="填充透明度">
          <n-input-number
            v-model:value="optionData.bar.style.fillOpacity"
            :step="0.1"
            size="small"
            :min="0"
            :max="1"
          ></n-input-number>
        </setting-item>
        <setting-item name="整体透明度">
          <n-input-number
            v-model:value="optionData.bar.style.opacity"
            :step="0.1"
            size="small"
            :min="0"
            :max="1"
          ></n-input-number>
        </setting-item>
        <setting-item name="纹理类型">
          <n-select v-model:value="optionData.bar.style.texture" :options="styleConfig.texture" size="small"></n-select>
        </setting-item>
      </SettingItemBox>
    </collapse-item>
  </template>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { styleConfig } from '@/packages/chartConfiguration/vcharts/index'

defineProps({
  optionData: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  }
})
</script>
