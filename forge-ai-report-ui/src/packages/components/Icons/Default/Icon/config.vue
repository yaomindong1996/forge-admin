<template>
  <collapse-item name="属性" :expanded="true">
    <setting-item-box name="样式">
      <setting-item name="颜色">
        <n-color-picker size="small" :modes="['hex']" v-model:value="optionData.color"></n-color-picker>
      </setting-item>
      <setting-item name="尺寸">
        <n-input-number v-model:value="optionData.size" size="small" :min="0" placeholder="尺寸"></n-input-number>
      </setting-item>
    </setting-item-box>
    <setting-item-box name="快捷旋转">
      <setting-item name="也可使用通用的【变换】来旋转">
        <n-select v-model:value="optionData.rotate" size="small" :options="rotateMode"></n-select>
      </setting-item>
    </setting-item-box>
  </collapse-item>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { option } from './config'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

// 旋转方式
const rotateMode = [
  {
    value: 0,
    label: '0°'
  },
  {
    value: 90,
    label: '90°'
  },
  {
    value: 180,
    label: '180°'
  },
  {
    value: 270,
    label: '270°'
  }
]
</script>
