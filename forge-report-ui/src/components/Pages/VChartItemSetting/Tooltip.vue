<template>
  <!-- todo 补充常用配置项 -->
  <div v-if="optionData.tooltip">
    <collapse-item name="提示框">
      <template #header>
        <n-switch v-model:value="optionData.tooltip.visible" size="small"></n-switch>
      </template>
      <setting-item-box name="框">
        <setting-item name="填充">
          <n-color-picker v-model:value="optionData.tooltip.style.panel.backgroundColor" size="small" />
        </setting-item>
        <setting-item name="瞄边">
          <n-color-picker v-model:value="optionData.tooltip.style.panel.border.color" size="small" />
        </setting-item>
        <setting-item name="粗细">
          <n-input-number v-model:value="optionData.tooltip.style.panel.border.width" :min="0" size="small" />
        </setting-item>
        <setting-item name="圆角">
          <n-input-number v-model:value="optionData.tooltip.style.panel.border.radius" :min="0" size="small" />
        </setting-item>
      </setting-item-box>
      <setting-item-box name="标题">
        <FontStyle :style="toRefs(optionData.tooltip.style.titleLabel)"></FontStyle>
      </setting-item-box>
      <setting-item-box name="名称">
        <FontStyle :style="toRefs(optionData.tooltip.style.keyLabel)"></FontStyle>
      </setting-item-box>
      <setting-item-box name="值">
        <FontStyle :style="toRefs(optionData.tooltip.style.valueLabel)"></FontStyle>
      </setting-item-box>
    </collapse-item>
  </div>
</template>

<script setup lang="ts">
import { PropType, toRefs } from 'vue'
import FontStyle from './common/FontStyle.vue'
import { vChartGlobalThemeJsonType } from '@/settings/vchartThemes/index'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'

defineProps({
  optionData: {
    type: Object as PropType<vChartGlobalThemeJsonType>,
    required: true
  }
})
</script>
