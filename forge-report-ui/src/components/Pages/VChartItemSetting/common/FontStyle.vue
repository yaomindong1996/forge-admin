<template>
  <!-- todo 补充常用配置项 -->
  <template v-if="style">
    <setting-item name="颜色">
      <n-color-picker v-model:value="style.fill.value" size="small" />
    </setting-item>
    <setting-item name="大小">
      <n-input-number v-model:value="style.fontSize.value" :min="1" size="small" />
    </setting-item>
    <setting-item name="字体">
      <n-select v-model:value="style.fontFamily.value" :options="fontStyleConfig.fontFamily" size="small" />
    </setting-item>
    <setting-item name="字重">
      <n-select v-model:value="style.fontWeight.value" :options="fontStyleConfig.fontWeight" size="small" />
    </setting-item>
    <setting-item v-if="style?.dx" name="X轴偏移">
      <n-input-number v-model:value="style.dx.value" size="small" />
    </setting-item>
    <setting-item v-if="style?.dy" name="Y轴偏移">
      <n-input-number v-model:value="style.dy.value" size="small" />
    </setting-item>
    <setting-item v-if="style?.angle" name="旋转">
      <n-input-number v-model:value="style.angle.value" :step="0.1" :min="0" :max="360" size="small" />
    </setting-item>
  </template>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { fontStyleConfig } from '@/packages/chartConfiguration/vcharts/index'
import { FontType } from '@/settings/vchartThemes/index'
import { SettingItem } from '@/components/Pages/ChartItemSetting'

defineProps({
  style: {
    type: Object as PropType<any>,
    required: true
  }
})
</script>
