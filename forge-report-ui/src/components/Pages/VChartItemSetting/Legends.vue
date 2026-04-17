<template>
  <!-- todo 补充常用配置项 -->
  <div v-if="optionData.legends">
    <div v-for="(legendItem, index) in optionData.legends" :key="index">
      <collapse-item name="图例">
        <template #header>
          <n-switch v-model:value="legendItem.visible" size="small"></n-switch>
        </template>
        <setting-item-box name="布局">
          <setting-item name="位置">
            <n-select v-model:value="legendItem.orient" size="small" :options="legendsConfig.orient" />
          </setting-item>
          <setting-item name="对齐方式">
            <n-select v-model:value="legendItem.position" size="small" :options="legendsConfig.position" />
          </setting-item>
        </setting-item-box>
        <setting-item-box name="项配置">
          <FontStyle :style="toRefs(legendItem.item.label.style)"></FontStyle>
        </setting-item-box>
      </collapse-item>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType, toRefs } from 'vue'
import { legendsConfig } from '@/packages/chartConfiguration/vcharts/index'
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
