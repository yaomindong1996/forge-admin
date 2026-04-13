<template>
  <template v-if="optionData.point">
    <collapse-item name="实心点(图元)">
      <template #header>
        <n-switch v-model:value="optionData.point.visible" size="small"></n-switch>
      </template>

      <setting-item-box name="样式">
        <setting-item name="位置">
          <n-select v-model:value="optionData.point.style.symbolType" size="small" :options="styleConfig.symbolType" />
        </setting-item>

        <setting-item name="大小">
          <n-input-number v-model:value="optionData.point.style.size" :min="0" size="small" />
        </setting-item>
        <setting-item name="填充透明度">
          <n-input-number v-model:value="optionData.point.style.fillOpacity" :step="0.1" :min="0" size="small" />
        </setting-item>
        <setting-item name="边框宽度">
          <n-input-number v-model:value="optionData.point.style.lineWidth" :min="0" size="small" />
        </setting-item>
        <setting-item name="边框颜色">
          <n-color-picker v-model:value="optionData.point.style.stroke" size="small" />
        </setting-item>
        <setting-item name="边框透明度">
          <n-input-number v-model:value="optionData.point.style.strokeOpacity" :step="0.1" :min="0" size="small" />
        </setting-item>
        <setting-item name="偏移X">
          <n-input-number v-model:value="optionData.point.style.dx" :min="0" size="small" />
        </setting-item>
        <setting-item name="偏移Y">
          <n-input-number v-model:value="optionData.point.style.dy" :min="0" size="small" />
        </setting-item>
      </setting-item-box>
    </collapse-item>
  </template>
</template>

<script setup lang="ts">
import { PropType, toRefs } from 'vue'
import { styleConfig } from '@/packages/chartConfiguration/vcharts/index'
import FontStyle from './common/FontStyle.vue'
import { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'

defineProps({
  optionData: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  },
  positionOptions: {
    type: Array,
    required: false
  }
})
</script>
